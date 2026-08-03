package com.wavvy.app.features.player.data.service

// Android core
import android.content.Intent
import android.os.Bundle
import androidx.annotation.OptIn
import androidx.core.net.toUri
// Media3
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.LoadControl
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.CommandButton
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
// Guava
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
// Project resources and components
import com.wavvy.app.R
import com.wavvy.app.features.auth.data.AuthRepositoryImpl
import com.wavvy.app.features.player.data.extractor.ExtractorHelper
import com.wavvy.app.features.player.models.toMediaItem
import com.wavvy.app.features.player.ui.components.QueueSong
// Coroutines
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

// Music execution playback controller
@OptIn(UnstableApi::class)
class MusicService : MediaSessionService() {

    private var player: ExoPlayer? = null
    private var mediaSession: MediaSession? = null

    // Scope structural controllers
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private lateinit var authRepository: AuthRepositoryImpl

    // State data models cache
    private var currentPlaylist: List<QueueSong> = emptyList()
    // Simulated internal state for heart toggle
    private var isCurrentTrackLiked = false

    // Pending-play control
    private var pendingPlayForMediaId: String? = null
    private var pendingUserPlay: Boolean = false
    private var autoPlayRequested: Boolean = false

    private val errorRetryCount = mutableMapOf<String, Int>()
    private val maxErrorRetries = 2

    companion object {
        const val EXTRA_AUTOPLAY = "EXTRA_AUTOPLAY"
        const val EXTRA_START_DURATION_MS = "EXTRA_START_DURATION_MS"
        const val EXTRA_SYNC_QUEUE = "EXTRA_SYNC_QUEUE"

        // In-process signal replacing LocalBroadcastManager (deprecated)
        private val _loadMoreQueueEvents = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
        val loadMoreQueueEvents: SharedFlow<Unit> = _loadMoreQueueEvents.asSharedFlow()
    }

    override fun onCreate() {
        super.onCreate()
        authRepository = AuthRepositoryImpl(applicationContext)

        // Custom notification provider decoration
        val notificationProvider = DefaultMediaNotificationProvider(applicationContext)
        notificationProvider.setSmallIcon(R.drawable.ic_app_logo_notification)
        setMediaNotificationProvider(notificationProvider)

        initializePlayer()
    }

    // Intercept incoming queue arrays sequence from user interaction states
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let {
            it.setExtrasClassLoader(QueueSong::class.java.classLoader)
            val playlist = androidx.core.content.IntentCompat.getParcelableArrayListExtra(it, "EXTRA_PLAYLIST", QueueSong::class.java)
            val startIndex = it.getIntExtra("EXTRA_START_INDEX", 0)
            val startPositionMs = it.getLongExtra("EXTRA_START_POSITION_MS", 0L)
            val startAudioUrl = it.getStringExtra("EXTRA_START_AUDIO_URL")
            val isAppend = it.getBooleanExtra("EXTRA_IS_APPEND", false)
            val isSyncQueue = it.getBooleanExtra(EXTRA_SYNC_QUEUE, false)
            val autoPlay = it.getBooleanExtra(EXTRA_AUTOPLAY, false)
            val startDurationVal = it.getLongExtra(EXTRA_START_DURATION_MS, -1L)
            val startDurationMs = if (startDurationVal > 0L) startDurationVal else null

            if (!playlist.isNullOrEmpty()) {
                if (isSyncQueue) {
                    syncQueueOrder(playlist)
                } else {
                    loadQueue(playlist, startIndex, startPositionMs, startAudioUrl, startDurationMs, isAppend, autoPlay)
                }
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    // Audio instance initialization pipeline
    private fun initializePlayer() {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .build()

        // Conservative lower buffer to improve startup latency
        val loadControl: LoadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                500,
                2000,
                250,
                500
            )
            .build()

        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setUserAgent("Wavvy/1.0")
            .setAllowCrossProtocolRedirects(true)

        serviceScope.launch {
            val sessionCookie = authRepository.getSessionToken()
            val customHeaders = mutableMapOf(
                "Accept" to "*/*",
                "Accept-Language" to "en-US,en;q=0.9",
                "Connection" to "keep-alive",
                "Origin" to "https://music.youtube.com",
                "Referer" to "https://music.youtube.com/"
            )
            if (!sessionCookie.isNullOrEmpty()) customHeaders["Cookie"] = sessionCookie
            httpDataSourceFactory.setDefaultRequestProperties(customHeaders)
        }

        val mediaSourceFactory = DefaultMediaSourceFactory(this)
            .setDataSourceFactory(httpDataSourceFactory)

        val playerInstance = ExoPlayer.Builder(this)
            .setLoadControl(loadControl)
            .setAudioAttributes(audioAttributes, true)
            .setMediaSourceFactory(mediaSourceFactory)
            .build()

        player = playerInstance

        // Creating session with layout command buttons attached
        mediaSession = MediaSession.Builder(this, playerInstance)
            .setCallback(CustomMediaSessionCallback())
            .build()

        // Listener: handle playback state changes and user-initiated blind play
        playerInstance.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                val currentIndex = playerInstance.currentMediaItemIndex
                val item = playerInstance.getMediaItemAtOrNull(currentIndex)
                val mediaId = item?.mediaId

                if (playbackState == Player.STATE_READY) {
                    if ((pendingUserPlay || autoPlayRequested) && (pendingPlayForMediaId == null || mediaId == pendingPlayForMediaId)) {
                        playerInstance.playWhenReady = true
                        pendingUserPlay = false
                        autoPlayRequested = false
                        pendingPlayForMediaId = null
                        errorRetryCount.remove(mediaId)
                        prefetchNextItem(currentIndex)
                    }
                }

                if (playbackState == Player.STATE_ENDED) {
                    val hasNext = playerInstance.hasNextMediaItem()
                    if (hasNext) {
                        playerInstance.seekToNextMediaItem()
                        playerInstance.prepare()
                        playerInstance.play()
                    }
                }
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                val newItem = mediaItem ?: return
                val newId = newItem.mediaId

                val currentIndex = playerInstance.currentMediaItemIndex
                val totalItems = playerInstance.mediaItemCount
                val remaining = totalItems - currentIndex

                if (remaining <= 1) {
                    broadcastLoadMoreNeeded()
                }

                pendingPlayForMediaId = newId
                errorRetryCount.remove(newId)

                // If item has a placeholder URI, resolve the direct stream URL
                val currentUri = newItem.localConfiguration?.uri?.toString() ?: ""
                if (!isDirectAudioUrl(currentUri)) {
                    serviceScope.launch {
                        val directAudioUrl = ExtractorHelper.extractAudioUrl(applicationContext, newId)
                        if (!directAudioUrl.isNullOrEmpty()) {
                            player?.let { exoPlayer ->
                                val idx = exoPlayer.currentMediaItemIndex
                                val itemAtIndex = exoPlayer.getMediaItemAtOrNull(idx)
                                if (itemAtIndex?.mediaId == newId) {
                                    val updatedItem = itemAtIndex.buildUpon().setUri(directAudioUrl).build()
                                    exoPlayer.replaceMediaItem(idx, updatedItem)
                                    exoPlayer.prepare()
                                }
                            }
                        }
                    }
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                val idx = playerInstance.currentMediaItemIndex
                val item = playerInstance.getMediaItemAtOrNull(idx)
                val mediaId = item?.mediaId ?: return

                val attempts = errorRetryCount.getOrDefault(mediaId, 0)
                if (attempts >= maxErrorRetries) {
                    errorRetryCount.remove(mediaId)
                    return
                }
                errorRetryCount[mediaId] = attempts + 1

                serviceScope.launch {
                    val freshUrl = ExtractorHelper.extractAudioUrl(applicationContext, mediaId)
                    if (!freshUrl.isNullOrEmpty()) {
                        val currentIdx = playerInstance.currentMediaItemIndex
                        val currentItem = playerInstance.getMediaItemAtOrNull(currentIdx)
                        if (currentItem?.mediaId == mediaId) {
                            val updatedItem = currentItem.buildUpon().setUri(freshUrl).build()
                            playerInstance.replaceMediaItem(currentIdx, updatedItem)
                            playerInstance.prepare()

                            pendingPlayForMediaId = mediaId
                            pendingUserPlay = true
                        }
                    }
                }
            }

            override fun onEvents(player: Player, events: Player.Events) {
                if (events.contains(Player.EVENT_REPEAT_MODE_CHANGED) ||
                    events.contains(Player.EVENT_SHUFFLE_MODE_ENABLED_CHANGED)) {
                    mediaSession?.let { updateNotificationLayout(it) }
                }
            }
        })

        updateNotificationLayout(mediaSession ?: return)
    }

    private fun broadcastLoadMoreNeeded() {
        serviceScope.launch {
            _loadMoreQueueEvents.emit(Unit)
        }
    }

    private fun Player.getMediaItemAtOrNull(index: Int): MediaItem? {
        return try {
            if (index in 0 until mediaItemCount) getMediaItemAt(index) else null
        } catch (_: Exception) {
            null
        }
    }

    // Reconciles item order/membership on the running ExoPlayer without touching playback state
    private fun syncQueueOrder(newPlaylist: List<QueueSong>) {
        val exoPlayer = player ?: return
        currentPlaylist = newPlaylist

        // Remove items no longer present in the new order
        for (i in exoPlayer.mediaItemCount - 1 downTo 0) {
            val id = exoPlayer.getMediaItemAtOrNull(i)?.mediaId
            if (id != null && newPlaylist.none { it.id == id }) {
                exoPlayer.removeMediaItem(i)
            }
        }

        // Move remaining items into their target position, one pass, left to right
        newPlaylist.forEachIndexed { targetIndex, song ->
            var currentPos = -1
            for (i in targetIndex until exoPlayer.mediaItemCount) {
                if (exoPlayer.getMediaItemAtOrNull(i)?.mediaId == song.id) {
                    currentPos = i
                    break
                }
            }
            if (currentPos != -1 && currentPos != targetIndex) {
                exoPlayer.moveMediaItem(currentPos, targetIndex)
            }
        }
    }

    private fun loadQueue(
        playlist: List<QueueSong>,
        startIndex: Int,
        startPositionMs: Long = 0L,
        startAudioUrl: String? = null,
        startDurationMs: Long? = null,
        isAppend: Boolean = false,
        autoPlay: Boolean = false
    ) {
        val exoPlayer = player ?: return

        if (isAppend && exoPlayer.mediaItemCount > 0 && currentPlaylist.isNotEmpty() && playlist.size > currentPlaylist.size) {
            val oldSize = currentPlaylist.size
            currentPlaylist = playlist
            val additionalSongs = playlist.subList(oldSize, playlist.size)
            val mediaItemsToAdd = additionalSongs.map { it.toMediaItem() }
            exoPlayer.addMediaItems(mediaItemsToAdd)
            preloadUpcomingItems(exoPlayer.currentMediaItemIndex)

            // Force playback transition if player has already ended
            if (exoPlayer.playbackState == Player.STATE_ENDED) {
                if (exoPlayer.hasNextMediaItem()) {
                    exoPlayer.seekToNextMediaItem()
                    exoPlayer.prepare()
                    exoPlayer.play()
                }
            }
            mediaSession?.let { updateNotificationLayout(it) }
            return
        }

        val currentMediaItem = exoPlayer.currentMediaItem
        val targetTrack = playlist.getOrNull(startIndex)

        // Zero interruption path
        if (currentMediaItem != null && targetTrack != null && currentMediaItem.mediaId == targetTrack.id) {
            currentPlaylist = playlist
            val isReady = exoPlayer.playbackState == Player.STATE_READY || exoPlayer.playbackState == Player.STATE_BUFFERING

            if (isReady) {
                // UI state sync trigger
                if (exoPlayer.playWhenReady) {
                    exoPlayer.playWhenReady = false
                    exoPlayer.playWhenReady = true
                } else if (autoPlay) {
                    exoPlayer.playWhenReady = true
                }
                return
            }
        }

        currentPlaylist = playlist
        serviceScope.launch {
            val mediaItems = playlist.mapIndexed { _, song -> song.toMediaItem() }
            if (mediaItems.isEmpty()) return@launch

            val startTrack = playlist.getOrNull(startIndex)
            val hasQueueDuration = (startTrack?.durationSeconds ?: 0L) > 0L

            val resolvedItems = if (startTrack != null) {
                val resolvedUrl = startAudioUrl ?: ExtractorHelper.extractAudioUrl(applicationContext, startTrack.id)
                if (!resolvedUrl.isNullOrEmpty()) {
                    mediaItems.mapIndexed { idx, item ->
                        if (idx == startIndex) {
                            val metaBuilder = item.mediaMetadata.buildUpon()
                            if (startTrack.imageUrl.isNotBlank()) metaBuilder.setArtworkUri(startTrack.imageUrl.toUri())
                            val attachedDurationMs = if (hasQueueDuration) startTrack.durationSeconds * 1000L else startDurationMs
                            item.buildUpon()
                                .setUri(resolvedUrl)
                                .setTag(attachedDurationMs)
                                .setMediaMetadata(metaBuilder.build())
                                .build()
                        } else item
                    }
                } else mediaItems
            } else mediaItems

            try {
                exoPlayer.setMediaItems(resolvedItems, startIndex, startPositionMs)
            } catch (_: Exception) {
                exoPlayer.stop()
                exoPlayer.clearMediaItems()
                exoPlayer.setMediaItems(resolvedItems)
                exoPlayer.seekTo(startIndex, startPositionMs)
            }

            // Pending playback state
            pendingPlayForMediaId = startTrack?.id
            pendingUserPlay = autoPlay
            autoPlayRequested = autoPlay

            if (autoPlay) {
                exoPlayer.playWhenReady = true
            }

            exoPlayer.prepare()
        }
    }

    // Check if a URI is already a direct audio stream (googlevideo.com CDN)
    private fun isDirectAudioUrl(uri: String): Boolean {
        return uri.startsWith("http") && (uri.contains("googlevideo.com") || uri.contains("rr") && uri.contains(".googlevideo"))
    }

    private fun preloadUpcomingItems(currentIndex: Int) {
        val toPreload = listOf(currentIndex + 1, currentIndex + 2, currentIndex + 3)
            .filter { it >= 0 && it < (player?.mediaItemCount ?: 0) }

        toPreload.forEach { idx ->
            val item = player?.getMediaItemAtOrNull(idx)
            if (item != null) {
                val videoId = item.mediaId
                val currentUri = item.localConfiguration?.uri?.toString() ?: ""
                // Only preload if not already a direct CDN stream
                if (!isDirectAudioUrl(currentUri)) {
                    serviceScope.launch {
                        val url = ExtractorHelper.extractAudioUrl(applicationContext, videoId)
                        if (!url.isNullOrEmpty()) {
                            player?.let { exo ->
                                if (idx < exo.mediaItemCount) {
                                    val updated = exo.getMediaItemAt(idx).buildUpon().setUri(url).build()
                                    exo.replaceMediaItem(idx, updated)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun prefetchNextItem(currentIndex: Int) {
        val nextIndex = currentIndex + 1
        val item = player?.getMediaItemAtOrNull(nextIndex) ?: return
        val videoId = item.mediaId
        val currentUri = item.localConfiguration?.uri?.toString() ?: ""
        if (isDirectAudioUrl(currentUri)) return // already resolved as direct CDN stream

        serviceScope.launch {
            val url = ExtractorHelper.extractAudioUrl(applicationContext, videoId)
            if (!url.isNullOrEmpty()) {
                player?.let { exo ->
                    if (nextIndex < exo.mediaItemCount) {
                        val current = exo.getMediaItemAtOrNull(nextIndex)
                        if (current?.mediaId == videoId) {
                            val updated = current.buildUpon().setUri(url).build()
                            exo.replaceMediaItem(nextIndex, updated)
                        }
                    }
                }
            }
        }
    }

    private fun updateNotificationLayout(session: MediaSession) {
        val playerInstance = session.player

        // Define explicit custom tokens
        val shuffleCommand = SessionCommand("CUSTOM_COMMAND_SHUFFLE", Bundle.EMPTY)
        val repeatCommand = SessionCommand("CUSTOM_COMMAND_REPEAT", Bundle.EMPTY)

        // Evaluate true state matching structures
        val isShuffle = playerInstance.shuffleModeEnabled
        val repeatMode = playerInstance.repeatMode

        // Resolve dynamic resource icons - using project drawables
        val shuffleIcon = if (isShuffle) R.drawable.ic_shuffle_active else R.drawable.ic_shuffle
        val repeatIcon = when (repeatMode) {
            Player.REPEAT_MODE_ONE -> R.drawable.ic_repeat_one_active
            Player.REPEAT_MODE_ALL -> R.drawable.ic_repeat_active
            else -> R.drawable.ic_repeat
        }

        val shuffleButton = CommandButton.Builder(CommandButton.ICON_UNDEFINED)
            .setSessionCommand(shuffleCommand)
            .setCustomIconResId(shuffleIcon)
            .setDisplayName("Shuffle")
            .build()

        // Use standard framework layout tokens for structural enforcement
        val previousButton = CommandButton.Builder(CommandButton.ICON_UNDEFINED)
            .setPlayerCommand(Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM)
            .setCustomIconResId(R.drawable.ic_previous)
            .setDisplayName("Previous")
            .build()

        val nextButton = CommandButton.Builder(CommandButton.ICON_UNDEFINED)
            .setPlayerCommand(Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM)
            .setCustomIconResId(R.drawable.ic_next)
            .setDisplayName("Next")
            .build()

        val repeatButton = CommandButton.Builder(CommandButton.ICON_UNDEFINED)
            .setSessionCommand(repeatCommand)
            .setCustomIconResId(repeatIcon)
            .setDisplayName("Repeat")
            .build()

        // Enforce a static sequence matrix array orders to prevent crashes
        session.setCustomLayout(listOf(shuffleButton, previousButton, nextButton, repeatButton))
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    override fun onTaskRemoved(rootIntent: Intent?) {
        val playerInstance = player
        if (playerInstance != null) {
            if (!playerInstance.playWhenReady || playerInstance.mediaItemCount == 0) stopSelf()
        }
    }

    // Lifecycle dismantling handling procedure
    override fun onDestroy() {
        serviceScope.cancel()
        mediaSession?.let { session ->
            session.player.release()
            session.release()
            mediaSession = null
        }
        player = null
        ExtractorHelper.clearCaches()
        super.onDestroy()
    }

    // Interactive custom and native feedback implementation
    private inner class CustomMediaSessionCallback : MediaSession.Callback {
        override fun onConnect(session: MediaSession, controller: MediaSession.ControllerInfo): MediaSession.ConnectionResult {
            val availableSessionCommands = MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS.buildUpon()
                .add(SessionCommand("CUSTOM_COMMAND_SHUFFLE", Bundle.EMPTY))
                .add(SessionCommand("CUSTOM_COMMAND_LIKE", Bundle.EMPTY))
                .add(SessionCommand("CUSTOM_COMMAND_REPEAT", Bundle.EMPTY))
                .build()

            // Restore transport matrix execution permissions to stabilize layouts positioning
            val availablePlayerCommands = MediaSession.ConnectionResult.DEFAULT_PLAYER_COMMANDS.buildUpon()
                .add(Player.COMMAND_SEEK_TO_NEXT)
                .add(Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM)
                .add(Player.COMMAND_SEEK_TO_PREVIOUS)
                .add(Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM)
                .add(Player.COMMAND_SET_SHUFFLE_MODE)
                .add(Player.COMMAND_SET_REPEAT_MODE)
                .add(Player.COMMAND_PLAY_PAUSE)
                .build()

            return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                .setAvailableSessionCommands(availableSessionCommands)
                .setAvailablePlayerCommands(availablePlayerCommands)
                .build()
        }

        @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
        override fun onPlaybackResumption(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo
        ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> {
            val playerInstance = mediaSession.player
            val count = playerInstance.mediaItemCount
            val index = playerInstance.currentMediaItemIndex
            val pos = playerInstance.currentPosition.coerceAtLeast(0L)
            return if (count > 0 && index in 0 until count) {
                val items = mutableListOf<MediaItem>()
                for (i in 0 until count) {
                    items.add(playerInstance.getMediaItemAt(i))
                }
                Futures.immediateFuture(
                    MediaSession.MediaItemsWithStartPosition(items, index, pos)
                )
            } else {
                Futures.immediateFuture(
                    MediaSession.MediaItemsWithStartPosition(emptyList(), 0, 0L)
                )
            }
        }

        override fun onAddMediaItems(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: MutableList<MediaItem>
        ): ListenableFuture<MutableList<MediaItem>> {
            return Futures.immediateFuture(mediaItems)
        }

        override fun onCustomCommand(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            customCommand: SessionCommand,
            args: Bundle
        ): ListenableFuture<SessionResult> {
            val playerInstance = session.player
            when (customCommand.customAction) {
                "CUSTOM_COMMAND_SHUFFLE" -> playerInstance.shuffleModeEnabled = !playerInstance.shuffleModeEnabled
                "CUSTOM_COMMAND_LIKE" -> {
                    isCurrentTrackLiked = !isCurrentTrackLiked
                    updateNotificationLayout(session)
                }
                "CUSTOM_COMMAND_REPEAT" -> {
                    val nextMode = when (playerInstance.repeatMode) {
                        Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
                        Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
                        else -> Player.REPEAT_MODE_OFF
                    }
                    playerInstance.repeatMode = nextMode
                }
            }
            return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
        }
    }
}

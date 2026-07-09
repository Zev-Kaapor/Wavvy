package com.lonewolf.wavvy.ui.player.service

// Android core components
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.annotation.OptIn
import android.util.Log
// Media3 core and player components
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
// Media3 networking and source structures
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.LoadControl
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
// Media3 sessions engine components
import androidx.media3.session.CommandButton
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
// Guava asynchronous execution tools
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
// Project business logic architecture
import com.lonewolf.wavvy.R
import com.lonewolf.wavvy.data.AuthRepositoryImpl
import com.lonewolf.wavvy.data.models.toMediaItem
import com.lonewolf.wavvy.ui.player.ExtractorHelper
import com.lonewolf.wavvy.ui.player.components.QueueSong
// Coroutines background async scope implementations
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
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

    private var pendingProbe: Boolean = false
    private val errorRetryCount = mutableMapOf<String, Int>()
    private val MAX_ERROR_RETRIES = 2

    companion object {
        const val EXTRA_AUTOPLAY = "EXTRA_AUTOPLAY"
        const val EXTRA_START_DURATION_MS = "EXTRA_START_DURATION_MS"
        const val EXTRA_SYNC_QUEUE = "EXTRA_SYNC_QUEUE"
    }

    override fun onCreate() {
        super.onCreate()
        authRepository = AuthRepositoryImpl(applicationContext)
        initializePlayer()
    }

    // Intercept incoming queue arrays sequence from user interaction states
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let {
            @Suppress("DEPRECATION")
            val playlist = it.getParcelableArrayListExtra<QueueSong>("EXTRA_PLAYLIST")
            val startIndex = it.getIntExtra("EXTRA_START_INDEX", 0)
            val startAudioUrl = it.getStringExtra("EXTRA_START_AUDIO_URL")
            val isAppend = it.getBooleanExtra("EXTRA_IS_APPEND", false)
            val isSyncQueue = it.getBooleanExtra(EXTRA_SYNC_QUEUE, false)
            val autoPlay = it.getBooleanExtra(EXTRA_AUTOPLAY, false)
            val startDurationVal = it.getLongExtra(EXTRA_START_DURATION_MS, -1L)
            val startDurationMs = if (startDurationVal > 0L) startDurationVal else null

            autoPlayRequested = autoPlay

            if (!playlist.isNullOrEmpty()) {
                if (isSyncQueue) {
                    syncQueueOrder(playlist)
                } else {
                    loadQueue(playlist, startIndex, startAudioUrl, startDurationMs, isAppend, autoPlay)
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
                Log.d("MusicService", "Player state -> $playbackState")
                if (playbackState == Player.STATE_READY) {
                    val currentIndex = playerInstance.currentMediaItemIndex
                    val item = playerInstance.getMediaItemAtOrNull(currentIndex)
                    val mediaId = item?.mediaId
                    val dur = playerInstance.duration
                    Log.d("MusicService", "STATE_READY mediaId=$mediaId duration=$dur pending=$pendingPlayForMediaId userReq=$pendingUserPlay autoPlay=$autoPlayRequested")

                    // If pending target matches and duration present, we can start playback (if requested)
                    if (pendingPlayForMediaId != null && mediaId == pendingPlayForMediaId && dur > 0) {
                        if (pendingUserPlay || autoPlayRequested) {
                            try { playerInstance.seekTo(0L) } catch (_: Exception) {}
                            playerInstance.playWhenReady = true
                            pendingUserPlay = false
                            autoPlayRequested = false
                            pendingPlayForMediaId = null
                            errorRetryCount.remove(mediaId)
                            prefetchNextItem(currentIndex)
                            Log.d("MusicService", "Started playback for $mediaId after ready+duration")
                        } else {
                            Log.d("MusicService", "Item ready with duration but no autoplay requested for $mediaId")
                        }
                    }
                }

                if (pendingProbe && (playbackState == Player.STATE_BUFFERING || playbackState == Player.STATE_READY)) {
                    val curDur = playerInstance.duration
                    if (curDur == C.TIME_UNSET || curDur <= 0) {
                        val idx = playerInstance.currentMediaItemIndex
                        val curMediaId = playerInstance.getMediaItemAtOrNull(idx)?.mediaId
                        if (curMediaId != null) {
                            Log.d("MusicService", "Running scheduled probe (playbackState=$playbackState) for mediaId=$curMediaId")
                            probeDurationAndMaybePlay(playerInstance, curMediaId, autoPlay = pendingUserPlay || autoPlayRequested)
                        } else {
                            pendingProbe = false
                        }
                    } else {
                        pendingProbe = false
                    }
                }
            }

            override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                // If user (controller) requested play but duration is unknown, intercept and run probing flow
                if (playWhenReady) {
                    val curDur = playerInstance.duration
                    val idx = playerInstance.currentMediaItemIndex
                    val curMediaId = playerInstance.getMediaItemAtOrNull(idx)?.mediaId
                    if ((curDur == C.TIME_UNSET || curDur <= 0) && curMediaId != null) {
                        Log.d("MusicService", "Detected playWhenReady=true but duration unknown (mediaId=$curMediaId). Intercepting to probe metadata.")
                        // stop playback immediately to avoid blind play
                        playerInstance.playWhenReady = false
                        pendingUserPlay = true
                        pendingPlayForMediaId = curMediaId
                        // launch probing routine
                        probeDurationAndMaybePlay(playerInstance, curMediaId, autoPlay = false)
                    }
                }
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                val newItem = mediaItem ?: return
                val newId = newItem.mediaId

                playerInstance.playWhenReady = false
                pendingPlayForMediaId = newId
                pendingUserPlay = true
                pendingProbe = false

                resolveStreamingUrlForComposition(newItem) {
                    Log.d("MusicService", "onMediaItemTransition: audio url resolved (or already valid) for $newId, probing duration")
                    probeDurationAndMaybePlay(playerInstance, newId, autoPlay = true)
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                val idx = playerInstance.currentMediaItemIndex
                val item = playerInstance.getMediaItemAtOrNull(idx)
                val mediaId = item?.mediaId
                Log.e("MusicService", "onPlayerError mediaId=$mediaId code=${error.errorCode} msg=${error.message}")

                if (mediaId == null) return

                val attempts = errorRetryCount.getOrDefault(mediaId, 0)
                if (attempts >= MAX_ERROR_RETRIES) {
                    Log.w("MusicService", "onPlayerError: max retries reached for $mediaId, giving up.")
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
                            pendingProbe = false
                            probeDurationAndMaybePlay(playerInstance, mediaId, autoPlay = true)
                            Log.d("MusicService", "onPlayerError: recovered with fresh url for $mediaId, prepare() called.")
                        }
                    } else {
                        Log.w("MusicService", "onPlayerError: extraction failed again for $mediaId.")
                    }
                }
            }

            override fun onEvents(player: Player, events: Player.Events) {
                if (events.contains(Player.EVENT_MEDIA_ITEM_TRANSITION) ||
                    events.contains(Player.EVENT_TIMELINE_CHANGED) ||
                    events.contains(Player.EVENT_REPEAT_MODE_CHANGED) ||
                    events.contains(Player.EVENT_SHUFFLE_MODE_ENABLED_CHANGED)) {
                    mediaSession?.let { updateNotificationLayout(it) }
                }
            }
        })

        updateNotificationLayout(mediaSession ?: return)
    }

    // Probing routine: try a tiny seek on the ExoPlayer to force metadata/duration discovery.
    private fun probeDurationAndMaybePlay(exoPlayer: ExoPlayer, mediaId: String, autoPlay: Boolean) {
        // Clear scheduled flag
        pendingProbe = false
        serviceScope.launch {
            val start = System.currentTimeMillis()
            val timeout = 12000L
            val poll = 80L

            try {
                try {
                    exoPlayer.seekTo(1L)
                } catch (_: Exception) {}

                // wait until duration becomes available or timeout
                while ((exoPlayer.duration == C.TIME_UNSET || exoPlayer.duration <= 0) && System.currentTimeMillis() - start < timeout) {
                    delay(poll)
                }

                // restore to 0 so playback starts from beginning
                try { exoPlayer.seekTo(0L) } catch (_: Exception) {}

                // if duration available and this was an autoPlay request or user requested play, start
                if (exoPlayer.duration != C.TIME_UNSET && exoPlayer.duration > 0) {
                    val shouldPlay = pendingUserPlay || autoPlayRequested || autoPlay
                    if (shouldPlay && pendingPlayForMediaId == mediaId) {
                        exoPlayer.playWhenReady = true
                        pendingUserPlay = false
                        pendingPlayForMediaId = null
                        autoPlayRequested = false
                        errorRetryCount.remove(mediaId)
                        prefetchNextItem(exoPlayer.currentMediaItemIndex)
                        Log.d("MusicService", "probeDuration: duration found, starting playback for $mediaId (duration=${exoPlayer.duration}).")
                    } else {
                        Log.d("MusicService", "probeDuration: duration found for $mediaId but no autoplay flag set.")
                    }
                } else {
                    Log.d("MusicService", "probeDuration: duration still unknown after probing for $mediaId.")
                    // leave paused — safer than blind play
                }
            } catch (e: Exception) {
                Log.w("MusicService", "probeDuration error: ${e.message}")
            }
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

        Log.d("MusicService", "syncQueueOrder: reconciled to ${newPlaylist.size} items, playback untouched.")
    }

    private fun loadQueue(
        playlist: List<QueueSong>,
        startIndex: Int,
        startAudioUrl: String? = null,
        startDurationMs: Long? = null,
        isAppend: Boolean = false,
        autoPlay: Boolean = false
    ) {
        val exoPlayer = player ?: return
        val startTimestamp = System.currentTimeMillis()
        Log.d("MusicService", "loadQueue start index=$startIndex append=$isAppend size=${playlist.size} autoPlay=$autoPlay startDuration=${startDurationMs ?: "null"}")

        if (isAppend && exoPlayer.mediaItemCount > 0 && currentPlaylist.isNotEmpty() && playlist.size > currentPlaylist.size) {
            val oldSize = currentPlaylist.size
            currentPlaylist = playlist
            val additionalSongs = playlist.subList(oldSize, playlist.size)
            val mediaItemsToAdd = additionalSongs.map { it.toMediaItem() }
            exoPlayer.addMediaItems(mediaItemsToAdd)
            Log.d("MusicService", "Appended ${mediaItemsToAdd.size} items")
            return
        }

        val currentMediaItem = exoPlayer.currentMediaItem
        val targetTrack = playlist.getOrNull(startIndex)
        if (currentMediaItem != null && targetTrack != null && currentMediaItem.mediaId == targetTrack.id) {
            currentPlaylist = playlist

            exoPlayer.playWhenReady = false

            serviceScope.launch {
                val mediaItems = playlist.map { it.toMediaItem() }
                val currentIndex = exoPlayer.currentMediaItemIndex
                if (mediaItems.isNotEmpty()) {
                    if (exoPlayer.mediaItemCount > currentIndex + 1) {
                        exoPlayer.removeMediaItems(currentIndex + 1, exoPlayer.mediaItemCount)
                    }
                    if (mediaItems.size > startIndex + 1) {
                        val upcomingItems = mediaItems.subList(startIndex + 1, mediaItems.size)
                        exoPlayer.addMediaItems(upcomingItems)
                    }
                    preloadUpcomingItems(currentIndex)
                }

                val hasArtwork = targetTrack.imageUrl.isNotBlank()
                val knownDuration = exoPlayer.duration

                if (hasArtwork) {
                    pendingPlayForMediaId = targetTrack.id

                    if (knownDuration != C.TIME_UNSET && knownDuration > 0L) {
                        try { exoPlayer.seekTo(currentIndex, 0L) } catch (_: Exception) {}
                        exoPlayer.playWhenReady = true
                        pendingPlayForMediaId = null
                        Log.d("MusicService", "Re-click: known duration — restarting and playing.")
                    } else {
                        pendingUserPlay = true
                        pendingProbe = true
                        probeDurationAndMaybePlay(exoPlayer, targetTrack.id, autoPlay = true)
                        Log.d("MusicService", "Re-click: duration missing — probing before play.")
                    }
                } else {
                    Log.d("MusicService", "Re-click: no artwork — waiting for manual play.")
                }
            }
            return
        }

        currentPlaylist = playlist
        serviceScope.launch {
            val mediaItems = playlist.mapIndexed { _, song -> song.toMediaItem() }
            if (mediaItems.isEmpty()) return@launch

            val startTrack = playlist.getOrNull(startIndex)

            val hasArtwork = startTrack?.imageUrl?.isNotBlank() ?: false
            val hasQueueDuration = (startTrack?.durationSeconds ?: 0L) > 0L

            val resolvedItems = if (startTrack != null) {
                val resolvedUrl = startAudioUrl ?: ExtractorHelper.extractAudioUrl(applicationContext, startTrack.id)
                if (!resolvedUrl.isNullOrEmpty()) {
                    mediaItems.mapIndexed { idx, item ->
                        if (idx == startIndex) {
                            val metaBuilder = item.mediaMetadata.buildUpon()
                            if (startTrack.imageUrl.isNotBlank()) metaBuilder.setArtworkUri(Uri.parse(startTrack.imageUrl))
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
                exoPlayer.playWhenReady = false
                exoPlayer.setMediaItems(resolvedItems, startIndex, 0L)
            } catch (e: Exception) {
                Log.w("MusicService", "setMediaItems failed, fallback clear: ${e.message}")
                exoPlayer.stop()
                exoPlayer.clearMediaItems()
                exoPlayer.setMediaItems(resolvedItems)
                exoPlayer.seekTo(startIndex, 0L)
            }

            // mark pending media and auto-play preference
            pendingPlayForMediaId = startTrack?.id
            pendingUserPlay = autoPlay
            autoPlayRequested = autoPlay
            exoPlayer.prepare()
            Log.d("MusicService", "setMediaItems + prepare() called. pendingPlay=$pendingPlayForMediaId autoPlay=$autoPlay (delta=${System.currentTimeMillis()-startTimestamp}ms) hasArtwork=$hasArtwork hasQueueDuration=$hasQueueDuration")

            if (hasArtwork) {
                if (hasQueueDuration) {
                    Log.d("MusicService", "Entry with duration present — autoplay will start at STATE_READY.")
                } else {
                    pendingProbe = true
                    Log.d("MusicService", "Duration missing — scheduled probe after prepare().")
                }
            } else {
                if (autoPlay && pendingPlayForMediaId != null && (startDurationMs == null || startDurationMs <= 0L)) {
                    pendingProbe = true
                    Log.d("MusicService", "Autoplay requested without known duration — scheduled probe after prepare().")
                }
            }
        }
    }

    // Asynchronous resolution layer linking NewPipe extractor pipelines on active track change bounds
    private fun resolveStreamingUrlForComposition(item: MediaItem, onResolved: (() -> Unit)? = null) {
        val videoId = item.mediaId
        val currentUri = item.localConfiguration?.uri?.toString() ?: ""

        // Skip if URI is already a valid audio stream
        if (currentUri.startsWith("http") && !currentUri.contains("youtube.com") && !currentUri.contains("music.youtube.com")) {
            onResolved?.invoke()
            return
        }

        serviceScope.launch {
            val directAudioUrl = ExtractorHelper.extractAudioUrl(applicationContext, videoId)
            if (!directAudioUrl.isNullOrEmpty()) {
                player?.let { exoPlayer ->
                    val currentIndex = exoPlayer.currentMediaItemIndex
                    val itemAtIndex = exoPlayer.getMediaItemAtOrNull(currentIndex)
                    if (itemAtIndex?.mediaId == videoId) {
                        val updatedItem = itemAtIndex.buildUpon().setUri(directAudioUrl).build()
                        exoPlayer.replaceMediaItem(currentIndex, updatedItem)
                    }
                }
            }
            onResolved?.invoke()
        }
    }

    private fun preloadUpcomingItems(currentIndex: Int) {
        val toPreload = listOf(currentIndex + 1, currentIndex + 2, currentIndex + 3)
            .filter { it >= 0 && it < (player?.mediaItemCount ?: 0) }

        toPreload.forEach { idx ->
            val item = player?.getMediaItemAtOrNull(idx)
            if (item != null) {
                val videoId = item.mediaId
                val currentUri = item.localConfiguration?.uri?.toString() ?: ""
                if (!currentUri.startsWith("http") || currentUri.contains("youtube.com")) {
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
        if (currentUri.startsWith("http") && !currentUri.contains("youtube.com")) return // already resolved

        serviceScope.launch {
            val url = ExtractorHelper.extractAudioUrl(applicationContext, videoId)
            if (!url.isNullOrEmpty()) {
                player?.let { exo ->
                    if (nextIndex < exo.mediaItemCount) {
                        val current = exo.getMediaItemAtOrNull(nextIndex)
                        if (current?.mediaId == videoId) {
                            val updated = current.buildUpon().setUri(url).build()
                            exo.replaceMediaItem(nextIndex, updated)
                            Log.d("MusicService", "prefetchNextItem: resolved next track $videoId ahead of time.")
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
        val likeCommand = SessionCommand("CUSTOM_COMMAND_LIKE", Bundle.EMPTY)
        val repeatCommand = SessionCommand("CUSTOM_COMMAND_REPEAT", Bundle.EMPTY)

        // Evaluate true state matching structures
        val isShuffle = playerInstance.shuffleModeEnabled
        val repeatMode = playerInstance.repeatMode
        val hasNext = playerInstance.hasNextMediaItem()

        // Resolve dynamic resource icons - using project drawables
        val shuffleIcon = if (isShuffle) R.drawable.ic_shuffle_active else R.drawable.ic_shuffle
        val repeatIcon = when (repeatMode) {
            Player.REPEAT_MODE_ONE -> R.drawable.ic_repeat_one_active
            Player.REPEAT_MODE_ALL -> R.drawable.ic_repeat_active
            else -> R.drawable.ic_repeat
        }

        val shuffleButton = CommandButton.Builder()
            .setSessionCommand(shuffleCommand)
            .setIconResId(shuffleIcon)
            .setDisplayName("Shuffle")
            .build()

        // Use standard framework layout tokens for structural enforcement
        val previousButton = CommandButton.Builder()
            .setPlayerCommand(Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM)
            .setIconResId(R.drawable.ic_previous)
            .setDisplayName("Previous")
            .build()

        // Leverage native slots to anchor position layout constraints
        val nextOrLikeButton = if (hasNext) {
            CommandButton.Builder()
                .setPlayerCommand(Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM)
                .setIconResId(R.drawable.ic_next)
                .setDisplayName("Next")
                .build()
        } else {
            CommandButton.Builder()
                .setSessionCommand(likeCommand)
                .setIconResId(R.drawable.ic_heart)
                .setDisplayName("Like")
                .build()
        }

        val repeatButton = CommandButton.Builder()
            .setSessionCommand(repeatCommand)
            .setIconResId(repeatIcon)
            .setDisplayName("Repeat")
            .build()

        // Enforce sequence matrix array orders
        session.setCustomLayout(listOf(shuffleButton, previousButton, nextOrLikeButton, repeatButton))
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
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        player = null
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
                .build()

            return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                .setAvailableSessionCommands(availableSessionCommands)
                .setAvailablePlayerCommands(availablePlayerCommands)
                .build()
        }

        // We no longer override onPlay signature to avoid API mismatch; we intercept user play via onPlayWhenReadyChanged listener above.
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

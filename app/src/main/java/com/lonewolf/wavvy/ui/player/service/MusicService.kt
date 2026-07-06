package com.lonewolf.wavvy.ui.player.service

// Android core components
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.annotation.OptIn
// Media3 core and player components
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
// Media3 networking and source structures
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
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

    // Lifecycle orchestration layer
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

            if (!playlist.isNullOrEmpty()) {
                loadQueue(playlist, startIndex)
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

        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setUserAgent("Mozilla/5.0 (Linux; Android 10; Quest 2) AppleWebKit/537.36 (KHTML, like Gecko) OculusBrowser/15.0.0.0.22 Chrome/89.0.4389.90 Mobile Safari/537.36")
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

            if (!sessionCookie.isNullOrEmpty()) {
                customHeaders["Cookie"] = sessionCookie
            }

            httpDataSourceFactory.setDefaultRequestProperties(customHeaders)
        }

        val mediaSourceFactory = DefaultMediaSourceFactory(this)
            .setDataSourceFactory(httpDataSourceFactory)

        val playerInstance = ExoPlayer.Builder(this)
            .setAudioAttributes(audioAttributes, true)
            .setMediaSourceFactory(mediaSourceFactory)
            .build()

        player = playerInstance

        // Creating session with layout command buttons attached
        mediaSession = MediaSession.Builder(this, playerInstance)
            .setCallback(CustomMediaSessionCallback())
            .build()

        // Sync local interface on player state events changes
        playerInstance.addListener(object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                super.onMediaItemTransition(mediaItem, reason)
                if (mediaItem != null) {
                    val currentUri = mediaItem.localConfiguration?.uri?.toString() ?: ""
                    val isResolved = currentUri.startsWith("http") && !currentUri.contains("youtube.com") && !currentUri.contains("music.youtube.com")

                    if (!isResolved) {
                        playerInstance.pause()
                    }

                    // Inject artwork uri from active track data cache
                    val currentIndex = playerInstance.currentMediaItemIndex
                    val queueSong = currentPlaylist.getOrNull(currentIndex)
                    if (queueSong != null && queueSong.imageUrl.isNotBlank()) {
                        val artworkItem = mediaItem.buildUpon()
                            .setMediaMetadata(
                                mediaItem.mediaMetadata.buildUpon()
                                    .setArtworkUri(Uri.parse(queueSong.imageUrl))
                                    .build()
                            )
                            .build()
                        playerInstance.replaceMediaItem(currentIndex, artworkItem)
                    }

                    // Lazy-resolve the streaming link structure before resuming playback
                    resolveStreamingUrlForComposition(mediaItem) {
                        // Resume playback after URL is resolved
                        playerInstance.playWhenReady = true

                        // Preload upcoming items in background
                        preloadUpcomingItems(playerInstance.currentMediaItemIndex)
                    }
                }
            }

            override fun onEvents(player: Player, events: Player.Events) {
                if (events.contains(Player.EVENT_SHUFFLE_MODE_ENABLED_CHANGED) ||
                    events.contains(Player.EVENT_REPEAT_MODE_CHANGED) ||
                    events.contains(Player.EVENT_MEDIA_ITEM_TRANSITION) ||
                    events.contains(Player.EVENT_TIMELINE_CHANGED)) {
                    if (events.contains(Player.EVENT_MEDIA_ITEM_TRANSITION)) {
                        isCurrentTrackLiked = false
                    }
                    mediaSession?.let { updateNotificationLayout(it) }
                }
            }
        })

        updateNotificationLayout(mediaSession ?: return)
    }

    // Allocation pipeline for mapping abstract metadata items before executing hardware decoders
    private fun loadQueue(playlist: List<QueueSong>, startIndex: Int) {
        val exoPlayer = player ?: return

        if (exoPlayer.mediaItemCount > 0 && currentPlaylist.isNotEmpty() && playlist.size > currentPlaylist.size) {
            val oldSize = currentPlaylist.size
            currentPlaylist = playlist
            val additionalSongs = playlist.subList(oldSize, playlist.size)

            val mediaItemsToAdd = additionalSongs.map { it.toMediaItem() }
            exoPlayer.addMediaItems(mediaItemsToAdd)
            return
        }

        val currentMediaItem = exoPlayer.currentMediaItem
        val targetTrack = playlist.getOrNull(startIndex)
        if (currentMediaItem != null && targetTrack != null && currentMediaItem.mediaId == targetTrack.id) {
            currentPlaylist = playlist
            serviceScope.launch {
                val mediaItems = playlist.map { it.toMediaItem() }
                if (mediaItems.isNotEmpty()) {
                    val currentIndex = exoPlayer.currentMediaItemIndex

                    if (exoPlayer.mediaItemCount > currentIndex + 1) {
                        exoPlayer.removeMediaItems(currentIndex + 1, exoPlayer.mediaItemCount)
                    }
                    if (mediaItems.size > startIndex + 1) {
                        val upcomingItems = mediaItems.subList(startIndex + 1, mediaItems.size)
                        exoPlayer.addMediaItems(upcomingItems)
                    }

                    preloadUpcomingItems(currentIndex)
                }
            }
            return
        }

        // Stop and clear player synchronously on the main thread before updating items
        exoPlayer.stop()
        exoPlayer.clearMediaItems()
        currentPlaylist = playlist

        serviceScope.launch {
            val mediaItems = playlist.mapIndexed { index, song ->
                song.toMediaItem()
            }

            if (mediaItems.isEmpty()) return@launch

            val startTrack = playlist.getOrNull(startIndex)
            val finalMediaItems = if (startTrack != null) {
                val directAudioUrl = ExtractorHelper.extractAudioUrl(applicationContext, startTrack.id)
                if (!directAudioUrl.isNullOrEmpty()) {
                    mediaItems.mapIndexed { index, item ->
                        if (index == startIndex) {
                            item.buildUpon()
                                .setUri(directAudioUrl)
                                .setMediaMetadata(
                                    item.mediaMetadata.buildUpon()
                                        .setArtworkUri(if (startTrack.imageUrl.isNotBlank()) Uri.parse(startTrack.imageUrl) else null)
                                        .build()
                                )
                                .build()
                        } else {
                            item
                        }
                    }
                } else {
                    mediaItems
                }
            } else {
                mediaItems
            }

            exoPlayer.setMediaItems(finalMediaItems)
            exoPlayer.seekTo(startIndex, 0L)
            exoPlayer.prepare()
            exoPlayer.playWhenReady = true
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

                    // Verify the item at current index matches the one we're resolving
                    val itemAtIndex = exoPlayer.getMediaItemAt(currentIndex)
                    if (itemAtIndex.mediaId == videoId) {
                        val updatedItem = itemAtIndex.buildUpon()
                            .setUri(directAudioUrl)
                            .build()

                        // Replace safely without losing queue context
                        exoPlayer.replaceMediaItem(currentIndex, updatedItem)
                    }
                }
            }

            // Notify when resolution is complete (whether successful or not)
            onResolved?.invoke()
        }
    }

    // Preload URLs for next N items in queue to avoid playback gaps
    private fun preloadUpcomingItems(currentIndex: Int) {
        val itemsToPreload = listOf(
            currentIndex + 1,
            currentIndex + 2,
            currentIndex + 3
        ).filter { it >= 0 && it < (player?.mediaItemCount ?: 0) }

        itemsToPreload.forEach { index ->
            val item = player?.getMediaItemAt(index)
            if (item != null) {
                val videoId = item.mediaId
                val currentUri = item.localConfiguration?.uri?.toString() ?: ""

                // Only preload if not already resolved
                if (!currentUri.startsWith("http") || currentUri.contains("youtube.com") || currentUri.contains("music.youtube.com")) {
                    serviceScope.launch {
                        val directAudioUrl = ExtractorHelper.extractAudioUrl(applicationContext, videoId)
                        if (!directAudioUrl.isNullOrEmpty()) {
                            player?.let { exoPlayer ->
                                if (index < exoPlayer.mediaItemCount) {
                                    val updatedItem = exoPlayer.getMediaItemAt(index).buildUpon()
                                        .setUri(directAudioUrl)
                                        .build()
                                    exoPlayer.replaceMediaItem(index, updatedItem)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Dynamic notification layout using internal framework drawable assets
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
                .setIconResId(if (isCurrentTrackLiked) R.drawable.ic_heart_active else R.drawable.ic_heart)
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

    // Active session provider interface
    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    // Task extraction sequence controller
    override fun onTaskRemoved(rootIntent: Intent?) {
        val playerInstance = player
        if (playerInstance != null) {
            if (!playerInstance.playWhenReady || playerInstance.mediaItemCount == 0) {
                stopSelf()
            }
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

        override fun onCustomCommand(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            customCommand: SessionCommand,
            args: Bundle
        ): ListenableFuture<SessionResult> {
            val playerInstance = session.player
            when (customCommand.customAction) {
                "CUSTOM_COMMAND_SHUFFLE" -> {
                    playerInstance.shuffleModeEnabled = !playerInstance.shuffleModeEnabled
                }
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

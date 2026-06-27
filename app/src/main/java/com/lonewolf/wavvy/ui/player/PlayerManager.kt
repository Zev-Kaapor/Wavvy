package com.lonewolf.wavvy.ui.player

// Android core components
import android.content.ComponentName
import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.annotation.OptIn
// Media3 core and player components
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
// Media3 sessions engine components
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
// Guava asynchronous execution tools
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
// Project background service components
import com.lonewolf.wavvy.ui.player.service.MusicService
// Coroutines state observation flows
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withTimeoutOrNull

// Media3 player controller
@OptIn(UnstableApi::class)
class PlayerManager(private val context: Context) {

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var mediaController: MediaController? = null

    // Signals when MediaController is ready
    private val controllerReady = CompletableDeferred<Unit>()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentMediaItem = MutableStateFlow<MediaItem?>(null)
    val currentMediaItem: StateFlow<MediaItem?> = _currentMediaItem.asStateFlow()

    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration.asStateFlow()

    private val _progress = MutableStateFlow(0f)
    val progress: StateFlow<Float> = _progress.asStateFlow()

    // Timestamp when playback started (perf test)
    private val _playbackStartedTimestamp = MutableStateFlow(0L)
    val playbackStartedTimestamp: StateFlow<Long> = _playbackStartedTimestamp.asStateFlow()

    // Progress sync handler
    private val handler = Handler(Looper.getMainLooper())
    private val updateProgressRunnable = object : Runnable {
        override fun run() {
            mediaController?.let { controller ->
                if (controller.isPlaying) {
                    val currentPos = controller.currentPosition
                    val totalDuration = controller.duration
                    if (totalDuration > 0) {
                        _progress.value = currentPos.toFloat() / totalDuration
                    }
                }
                handler.postDelayed(this, 1000)
            }
        }
    }

    init {
        initializeController()
    }

    // Setup MediaController connection
    private fun initializeController() {
        val sessionToken = SessionToken(context, ComponentName(context, MusicService::class.java))
        controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()

        controllerFuture?.addListener({
            try {
                mediaController = controllerFuture?.get()
                setupControllerListener()
                handler.post(updateProgressRunnable)
                controllerReady.complete(Unit)
            } catch (e: Exception) {
                e.printStackTrace()
                controllerReady.completeExceptionally(e)
            }
        }, MoreExecutors.directExecutor())
    }

    // Listen to player state changes
    private fun setupControllerListener() {
        mediaController?.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _isPlaying.value = isPlaying
                if (isPlaying) {
                    _playbackStartedTimestamp.value = System.currentTimeMillis()
                }
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                _currentMediaItem.value = mediaItem
                _duration.value = mediaController?.duration?.coerceAtLeast(0L) ?: 0L
                _progress.value = 0f
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    _duration.value = mediaController?.duration?.coerceAtLeast(0L) ?: 0L
                }
            }
        })

        mediaController?.let {
            _isPlaying.value = it.isPlaying
            _currentMediaItem.value = it.currentMediaItem
            _duration.value = it.duration.coerceAtLeast(0L)
        }
    }

    // Wait for controller readiness with timeout
    suspend fun awaitReady(timeoutMs: Long = 3000L): Boolean {
        if (mediaController != null) return true
        val result = withTimeoutOrNull(timeoutMs) { controllerReady.await() }
        return result != null
    }

    // Load and play track
    suspend fun playTrack(url: String, title: String, artist: String, imageUrl: String): Boolean {
        val isReady = awaitReady()
        if (!isReady) {
            android.util.Log.w("PlayerManager", "MediaController not ready in time.")
            return false
        }

        val mediaItem = MediaItem.Builder()
            .setMediaId(url)
            .setUri(url)
            .setMediaMetadata(
                androidx.media3.common.MediaMetadata.Builder()
                    .setTitle(title)
                    .setArtist(artist)
                    .setArtworkUri(android.net.Uri.parse(imageUrl))
                    .build()
            )
            .build()

        mediaController?.run {
            setMediaItem(mediaItem)
            prepare()
            play()
        }
        return true
    }

    // Pause playback immediately
    fun pause() {
        mediaController?.pause()
    }

    // Toggle play/pause
    fun playPause() {
        mediaController?.let {
            if (it.isPlaying) it.pause() else it.play()
        }
    }

    // Seek to position
    fun seekTo(positionMs: Long) {
        mediaController?.seekTo(positionMs)
        mediaController?.let {
            val total = it.duration
            if (total > 0) {
                _progress.value = positionMs.toFloat() / total
            }
        }
    }

    // Reset progress for new track
    fun resetProgress() {
        _progress.value = 0f
        _duration.value = 0L
    }

    // Cleanup resources
    fun release() {
        handler.removeCallbacks(updateProgressRunnable)
        controllerFuture?.let {
            MediaController.releaseFuture(it)
        }
        mediaController = null
    }
}

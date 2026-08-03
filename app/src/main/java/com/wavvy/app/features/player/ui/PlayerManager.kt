package com.wavvy.app.features.player.ui

// Android core architecture components
import android.content.ComponentName
import android.content.Context
// Media3
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
// Guava concurrency utilities
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
// Coroutines
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
// Project services
import com.wavvy.app.features.player.data.service.MusicService

class PlayerManager(private val context: Context) {

    // Controller readiness signal
    private val controllerReady = CompletableDeferred<Unit>()

    // Playback buffering state
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _playbackStartedTimestamp = MutableStateFlow(0L)
    val playbackStartedTimestamp: StateFlow<Long> = _playbackStartedTimestamp.asStateFlow()

    private val _currentMediaItem = MutableStateFlow<MediaItem?>(null)
    val currentMediaItem: StateFlow<MediaItem?> = _currentMediaItem.asStateFlow()

    private val _duration = MutableStateFlow(C.TIME_UNSET)
    val duration: StateFlow<Long> = _duration.asStateFlow()

    private val _progress = MutableStateFlow(0f)
    val progress: StateFlow<Float> = _progress.asStateFlow()

    private var mediaController: MediaController? = null

    // Reference kept so the controller connection can be released on cleanup
    private var controllerFuture: ListenableFuture<MediaController>? = null

    // Scope and job for the progress polling loop
    private val managerScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var progressJob: Job? = null

    init {
        initializeController()
    }

    // Build the session token and connect to MusicService's media session
    private fun initializeController() {
        val token = SessionToken(context, ComponentName(context, MusicService::class.java))
        val future = MediaController.Builder(context, token).buildAsync()
        controllerFuture = future
        future.addListener(
            { injectController(future.get()) },
            MoreExecutors.directExecutor()
        )
    }

    // Player state listener
    private fun setupControllerListener() {
        mediaController?.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _isPlaying.value = isPlaying
                if (isPlaying) {
                    _playbackStartedTimestamp.value = System.currentTimeMillis()
                    startProgressPolling()
                } else {
                    stopProgressPolling()
                }
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                val isSameTrack = mediaItem?.mediaId != null && mediaItem.mediaId == _currentMediaItem.value?.mediaId
                _currentMediaItem.value = mediaItem

                // Fallback duration handling
                val controllerDuration = mediaController?.duration ?: C.TIME_UNSET
                if (controllerDuration != C.TIME_UNSET && controllerDuration > 0L) {
                    _duration.value = controllerDuration
                } else {
                    val tagDur = mediaItem?.localConfiguration?.tag as? Long
                    _duration.value = tagDur ?: C.TIME_UNSET
                }

                if (!isSameTrack) {
                    _progress.value = 0f
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                _isLoading.value = playbackState == Player.STATE_BUFFERING
                if (playbackState == Player.STATE_READY) {
                    val controllerDuration = mediaController?.duration ?: C.TIME_UNSET
                    if (controllerDuration != C.TIME_UNSET && controllerDuration > 0L) {
                        _duration.value = controllerDuration
                    } else {
                        val extras = mediaController?.currentMediaItem?.mediaMetadata?.extras
                        val tagDur = extras?.getLong("CUSTOM_METADATA_KEY_DURATION_MS", -1L) ?: -1L
                        _duration.value = if (tagDur > 0L) tagDur else C.TIME_UNSET
                    }
                    // Resume polling if player was already playing when ready
                    if (mediaController?.isPlaying == true) startProgressPolling()
                }
            }
        })

        mediaController?.let {
            _isLoading.value = it.playbackState == Player.STATE_BUFFERING
            _isPlaying.value = it.isPlaying
            _currentMediaItem.value = it.currentMediaItem
            val ctrlDur = it.duration
            val extras = it.currentMediaItem?.mediaMetadata?.extras
            val backupDur = extras?.getLong("CUSTOM_METADATA_KEY_DURATION_MS", -1L) ?: -1L

            _duration.value = if (ctrlDur != C.TIME_UNSET && ctrlDur > 0L) {
                ctrlDur
            } else if (backupDur > 0L) {
                backupDur
            } else {
                C.TIME_UNSET
            }
        }
    }

    // Controller injection bridge
    fun injectController(controller: MediaController) {
        mediaController = controller
        setupControllerListener()
        controllerReady.complete(Unit)
    }

    // Controller state awaiter
    suspend fun awaitReady(): Boolean {
        controllerReady.await()
        val state = mediaController?.playbackState
        return state != null && state != Player.STATE_IDLE
    }

    // Playback controls
    fun playPause() {
        mediaController?.let {
            if (it.isPlaying) it.pause() else it.play()
        }
    }

    fun pause() {
        mediaController?.pause()
    }

    fun next() {
        mediaController?.seekToNextMediaItem()
    }

    fun previous() {
        mediaController?.seekToPreviousMediaItem()
    }

    fun seekTo(positionMs: Long) {
        mediaController?.seekTo(positionMs)
    }

    fun setRepeatMode(mode: Int) {
        mediaController?.repeatMode = when (mode) {
            1 -> Player.REPEAT_MODE_ALL
            2 -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
    }

    // Poll current playback position every 500ms and update _progress
    private fun startProgressPolling() {
        progressJob?.cancel()
        progressJob = managerScope.launch {
            while (true) {
                val controller = mediaController ?: break
                val duration = controller.duration
                val position = controller.currentPosition
                if (duration > 0L && duration != C.TIME_UNSET) {
                    _progress.value = (position.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
                    _duration.value = duration
                }
                delay(500)
            }
        }
    }

    private fun stopProgressPolling() {
        progressJob?.cancel()
        progressJob = null
    }

    fun resetProgress() {
        stopProgressPolling()
        _progress.value = 0f
    }

    // Lifecycle release handling
    fun release() {
        stopProgressPolling()
        managerScope.cancel()
        controllerFuture?.let { MediaController.releaseFuture(it) }
        mediaController = null
    }
}

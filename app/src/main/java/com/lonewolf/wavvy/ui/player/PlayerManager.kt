package com.lonewolf.wavvy.ui.player

// Android core components
import android.content.ComponentName
import android.content.Context
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

// Infrastructure setup components
@OptIn(UnstableApi::class)
class PlayerManager(private val context: Context) {

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var mediaController: MediaController? = null

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentMediaItem = MutableStateFlow<MediaItem?>(null)
    val currentMediaItem: StateFlow<MediaItem?> = _currentMediaItem.asStateFlow()

    init {
        initializeController()
    }

    // Direct initialization setup handlers
    private fun initializeController() {
        val sessionToken = SessionToken(context, ComponentName(context, MusicService::class.java))
        controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()

        controllerFuture?.addListener({
            try {
                mediaController = controllerFuture?.get()
                setupControllerListener()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, MoreExecutors.directExecutor())
    }

    // Direct interface player interaction control
    private fun setupControllerListener() {
        mediaController?.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _isPlaying.value = isPlaying
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                _currentMediaItem.value = mediaItem
            }
        })

        mediaController?.let {
            _isPlaying.value = it.isPlaying
            _currentMediaItem.value = it.currentMediaItem
        }
    }

    // Process player activation and track media loading
    fun playTrack(url: String, title: String, artist: String, imageUrl: String) {
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
    }

    // Direct interface player interaction control
    fun playPause() {
        mediaController?.let {
            if (it.isPlaying) it.pause() else it.play()
        }
    }

    // Direct interface player interaction control
    fun seekTo(positionMs: Long) {
        mediaController?.seekTo(positionMs)
    }

    // Infrastructure lifecycle breakdown handler
    fun release() {
        controllerFuture?.let {
            MediaController.releaseFuture(it)
        }
        mediaController = null
    }
}

package com.lonewolf.wavvy.ui.player.service

// Android core components
import android.content.Intent
import androidx.annotation.OptIn
// Media3 core and player components
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
// Media3 networking and source structures
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
// Media3 sessions engine components
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
// Project business logic architecture
import com.lonewolf.wavvy.data.AuthRepositoryImpl
// Coroutines background async scope implementations
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

// Music execution playback controller
class MusicService : MediaSessionService() {

    private var player: ExoPlayer? = null
    private var mediaSession: MediaSession? = null

    // Scope structural controllers
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private lateinit var authRepository: AuthRepositoryImpl

    // Lifecycle orchestration layer
    override fun onCreate() {
        super.onCreate()
        authRepository = AuthRepositoryImpl(applicationContext)
        initializePlayer()
    }

    // Audio instance initialization pipeline
    @OptIn(UnstableApi::class)
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

        mediaSession = MediaSession.Builder(this, playerInstance)
            .setCallback(CustomMediaSessionCallback())
            .build()
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
    @OptIn(UnstableApi::class)
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

    // Interactive custom feedback implementation
    private class CustomMediaSessionCallback : MediaSession.Callback
}

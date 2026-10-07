package com.wavvy.app.core.playback

// Android app and intents
import android.app.PendingIntent
import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
// Media3 player, data and session
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.HttpDataSource
import androidx.media3.datasource.ResolvingDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.extractor.ExtractorsFactory
import androidx.media3.extractor.mkv.MatroskaExtractor
import androidx.media3.extractor.mp4.FragmentedMp4Extractor
import androidx.media3.extractor.mp4.Mp4Extractor
import androidx.media3.session.CommandButton
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionError
import androidx.media3.session.SessionResult
// Concurrency
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
// Coroutines
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
// Networking
import okhttp3.OkHttpClient
import java.io.IOException
// Project resources
import com.wavvy.app.MainActivity
import com.wavvy.app.R
import com.wavvy.app.core.history.ListenTracker

// Plays the audio in the background with the media notification, the link of each song is found only when the player needs it, as Metrolist (GPL-3.0) does
@OptIn(UnstableApi::class)
class PlaybackService : MediaSessionService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val streamCache = StreamUrlCache()

    // Tries of each song after YouTube refused its link, and the try that is waiting
    private val retries = mutableMapOf<String, Int>()
    private var retryJob: Job? = null

    private lateinit var player: ExoPlayer
    private lateinit var listenTracker: ListenTracker
    private var mediaSession: MediaSession? = null

    override fun onCreate() {
        super.onCreate()
        StreamResolver.initialize(this)

        player = ExoPlayer.Builder(this)
            .setMediaSourceFactory(DefaultMediaSourceFactory(createDataSourceFactory(), extractorsFactory))
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                true
            )
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .build()
        player.addListener(playerListener)

        // A song goes to the history as soon as it plays long enough with sound, and the rest of its time is added when it ends
        listenTracker = ListenTracker(this, player, scope).also { player.addListener(it) }

        mediaSession = MediaSession.Builder(this, player)
            .setSessionActivity(openAppIntent())
            .setCallback(sessionCallback)
            .setMediaButtonPreferences(modeButtons())
            .build()
    }

    // Shuffle and repeat buttons of the notification, showing the mode that is on
    private fun modeButtons(): List<CommandButton> {
        val shuffle = CommandButton.Builder(if (player.shuffleModeEnabled) CommandButton.ICON_SHUFFLE_ON else CommandButton.ICON_SHUFFLE_OFF)
            .setDisplayName(getString(if (player.shuffleModeEnabled) R.string.playback_shuffle_off else R.string.playback_shuffle_on))
            .setSessionCommand(ToggleShuffle)
            .build()
        val repeat = CommandButton.Builder(
            when (player.repeatMode) {
                Player.REPEAT_MODE_ALL -> CommandButton.ICON_REPEAT_ALL
                Player.REPEAT_MODE_ONE -> CommandButton.ICON_REPEAT_ONE
                else -> CommandButton.ICON_REPEAT_OFF
            }
        )
            .setDisplayName(
                getString(
                    when (player.repeatMode) {
                        Player.REPEAT_MODE_ALL -> R.string.playback_repeat_all
                        Player.REPEAT_MODE_ONE -> R.string.playback_repeat_one
                        else -> R.string.playback_repeat_off
                    }
                )
            )
            .setSessionCommand(ToggleRepeat)
            .build()
        return listOf(shuffle, repeat)
    }

    // Lets the notification send the shuffle and repeat commands, and runs them
    private val sessionCallback = object : MediaSession.Callback {
        override fun onConnect(session: MediaSession, controller: MediaSession.ControllerInfo): MediaSession.ConnectionResult {
            val commands = MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS.buildUpon()
                .add(ToggleShuffle)
                .add(ToggleRepeat)
                .build()
            return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                .setAvailableSessionCommands(commands)
                .build()
        }

        override fun onCustomCommand(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            customCommand: SessionCommand,
            args: Bundle
        ): ListenableFuture<SessionResult> {
            when (customCommand.customAction) {
                ToggleShuffleAction -> player.shuffleModeEnabled = !player.shuffleModeEnabled
                ToggleRepeatAction -> player.repeatMode = when (player.repeatMode) {
                    Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
                    Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
                    else -> Player.REPEAT_MODE_OFF
                }
                else -> return Futures.immediateFuture(SessionResult(SessionError.ERROR_NOT_SUPPORTED))
            }
            return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    // Closing the app from the recent apps stops the service only when nothing is playing
    override fun onTaskRemoved(rootIntent: Intent?) {
        if (!player.playWhenReady || player.mediaItemCount == 0) stopSelf()
    }

    override fun onDestroy() {
        listenTracker.finish()
        scope.cancel()
        mediaSession?.run {
            player.release()
            release()
        }
        mediaSession = null
        super.onDestroy()
    }

    // Tapping the notification opens the app
    private fun openAppIntent(): PendingIntent =
        PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

    // Asks for the link of a song when the player opens it, reusing a link that is still valid
    private fun createDataSourceFactory(): DataSource.Factory {
        val upstream = DefaultDataSource.Factory(this, OkHttpDataSource.Factory(OkHttpClient()))

        return ResolvingDataSource.Factory(upstream) { dataSpec ->
            val mediaId = dataSpec.key ?: throw IOException("No media id")
            streamCache[mediaId]?.let { return@Factory dataSpec.withResolvedStream(it) }

            // The time each extraction takes shows in the log, to tell a slow opening from a repeated one
            val started = SystemClock.elapsedRealtime()
            Log.d(LogTag, "Resolving $mediaId")
            val stream = runBlocking(Dispatchers.IO) { StreamResolver.resolve(mediaId) }
                .getOrElse { error ->
                    Log.d(LogTag, "Failed $mediaId after ${SystemClock.elapsedRealtime() - started} ms: ${error.message}")
                    throw IOException("Could not resolve the stream", error)
                }
            Log.d(LogTag, "Resolved $mediaId by ${stream.clientName} in ${SystemClock.elapsedRealtime() - started} ms")
            streamCache.put(mediaId, stream)
            dataSpec.withResolvedStream(stream)
        }
    }

    // Recovers from links YouTube refused, at most a few times per song
    private val playerListener = object : Player.Listener {
        override fun onPlayerError(error: PlaybackException) {
            val mediaId = player.currentMediaItem?.mediaId ?: return
            val code = httpResponseCode(error)
            if (code != HttpForbidden && code != HttpGone) return

            val tries = retries[mediaId] ?: 0
            if (tries >= MaxRetriesPerSong) {
                retries.remove(mediaId)
                return
            }
            retries[mediaId] = tries + 1

            // The client that gave the refused link is left out, and the cipher may need a newer config
            streamCache[mediaId]?.let { StreamResolver.markClientFailed(mediaId, it.clientName) }
            streamCache.invalidate(mediaId)
            scope.launch {
                if (runCatching { StreamResolver.refreshAfterStreamRejection() }.getOrDefault(false)) {
                    StreamResolver.clearClientFailures()
                }
            }

            val position = player.currentPosition
            val index = player.currentMediaItemIndex
            retryJob?.cancel()
            retryJob = scope.launch {
                delay(RetryDelayMs)
                if (player.currentMediaItem?.mediaId != mediaId) return@launch
                player.seekTo(index, position)
                player.prepare()
            }
        }

        // A song that started playing gets its tries back
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            if (isPlaying) player.currentMediaItem?.mediaId?.let(retries::remove)
        }

        // The notification buttons follow the modes, whoever changed them
        override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
            mediaSession?.setMediaButtonPreferences(modeButtons())
        }

        override fun onRepeatModeChanged(repeatMode: Int) {
            mediaSession?.setMediaButtonPreferences(modeButtons())
        }
    }

    // HTTP status YouTube answered with, found in the causes of a player error
    private fun httpResponseCode(error: PlaybackException): Int? {
        var cause: Throwable? = error.cause
        while (cause != null) {
            if (cause is HttpDataSource.InvalidResponseCodeException) return cause.responseCode
            cause = cause.cause
        }
        return null
    }

    private companion object {
        const val HttpForbidden = 403
        const val HttpGone = 410
        const val MaxRetriesPerSong = 3
        const val RetryDelayMs = 1000L

        // Name the extraction times are logged under
        const val LogTag = "WavvyPlayback"

        // Commands of the shuffle and repeat buttons of the notification
        const val ToggleShuffleAction = "com.wavvy.app.TOGGLE_SHUFFLE"
        const val ToggleRepeatAction = "com.wavvy.app.TOGGLE_REPEAT"
        val ToggleShuffle = SessionCommand(ToggleShuffleAction, Bundle.EMPTY)
        val ToggleRepeat = SessionCommand(ToggleRepeatAction, Bundle.EMPTY)

        // YouTube serves audio as WebM or MP4, the other formats are left out of the app
        val extractorsFactory = ExtractorsFactory { arrayOf(MatroskaExtractor(), FragmentedMp4Extractor(), Mp4Extractor()) }
    }
}

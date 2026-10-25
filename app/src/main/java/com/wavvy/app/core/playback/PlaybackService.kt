package com.wavvy.app.core.playback

// Android app and intents
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
// Media3 player, data and session
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
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
import androidx.media3.extractor.DefaultExtractorsFactory
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
import com.wavvy.app.core.network.WavvyProxy
import com.wavvy.app.core.radio.isRadioTrack
import java.io.IOException
// Project resources
import com.wavvy.app.MainActivity
import com.wavvy.app.R
import com.wavvy.app.core.cast.CastBridge
import com.wavvy.app.core.device.isDeviceTrack
import com.wavvy.app.core.download.DownloadFirstDataSource
import com.wavvy.app.core.download.Downloads
import com.wavvy.app.core.history.ListenTracker
import com.wavvy.app.core.language.AppLanguage
import com.wavvy.app.features.settings.data.PrivacyPrefs
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import androidx.media3.common.Timeline
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.WeakHashMap
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import com.wavvy.app.core.designsystem.components.Toasts
import com.wavvy.app.features.settings.data.PlaybackSettings
import com.wavvy.app.features.settings.data.SleepSettings
import com.wavvy.app.features.settings.data.SettingsStore

// Plays the audio in the background with the media notification, the link of each song is found only when the player needs it, as Metrolist (GPL-3.0) does
@OptIn(UnstableApi::class)
class PlaybackService : MediaSessionService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val streamCache = StreamUrlCache()

    // Tries of each song after YouTube refused its link, and the try that is waiting
    private val retries = mutableMapOf<String, Int>()
    private var retryJob: Job? = null
    private var sleepJob: Job? = null

    private lateinit var player: ExoPlayer
    private lateinit var crossfade: CrossfadeEngine
    private lateinit var sponsorBlock: SponsorBlock
    private lateinit var listenTracker: ListenTracker
    private var castBridge: CastBridge? = null
    private var mediaSession: MediaSession? = null

    // What the user chose for the playback and the sleep timer, kept up to date while the service lives
    private var playbackSettings = PlaybackSettings()
    private var sleepSettings = SleepSettings()

    // The part of each player that brings its songs to the same loudness
    private val normalizers = WeakHashMap<ExoPlayer, VolumeNormalizer>()

    // The queue that comes back when the app opens, and the job that writes it a moment after the last change
    private lateinit var queueSaver: QueueSaver
    private var saveJob: Job? = null
    private var muteJob: Job? = null

    // True while the music is paused by the volume going to nothing, so it comes back when the volume does, and the Bluetooth devices that were already there
    private var isPausedByMute = false
    private val knownBluetooth = mutableSetOf<Int>()

    // True while the sleep timer lowers the volume, and the window of the schedule the timer already started in
    private var isFading = false
    private var lastAutoWindow: LocalDate? = null

    // The language the user picked, for the versions of Android that do not keep it themselves
    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(AppLanguage.wrap(base))
    }

    override fun onCreate() {
        super.onCreate()
        StreamResolver.initialize(this)
        PrivacyPrefs.follow(this)

        // The settings of the playback are known before the first player is made, so it starts as the user chose
        val store = SettingsStore(this)
        playbackSettings = runBlocking { store.playback.first() }
        StreamResolver.streamQuality = playbackSettings.quality.audio
        sleepSettings = runBlocking { store.sleep.first() }
        queueSaver = QueueSaver(this)

        player = createPlayer()
        player.addListener(playerListener)

        // The queue of the last time comes back paused, when it was asked
        if (playbackSettings.persistentQueue) queueSaver.restore(player)

        scope.launch {
            store.playback.collect {
                playbackSettings = it
                StreamResolver.streamQuality = it.quality.audio
                watchMute(it.pauseWhenMuted)
                normalizers.keys.forEach { each -> runCatching { each.skipSilenceEnabled = it.skipSilence } }
                applyNormalization()
            }
        }
        scope.launch { store.sleep.collect { sleepSettings = it } }
        scope.launch { Loudness.updates.collect { applyNormalization() } }

        // A song that is paused for a long time still comes back where it was, so the place in it is kept every little while
        scope.launch {
            while (true) {
                delay(SaveEveryMillis)
                if (player.isPlaying) saveQueue()
            }
        }

        registerBluetooth()

        // The crossfade follows what the user chose, and the session shows the player that sends the changes of song through it
        crossfade = CrossfadeEngine(player, scope, ::createPlayer, musicAttributes, ::swapPlayer, ::prefetchStream)
        player.addListener(crossfade)
        scope.launch { SettingsStore(this@PlaybackService).crossfade.collect { crossfade.settings = it } }

        // The sleep timer stops the music when its time comes, or at the end of the song when it was asked so
        scope.launch {
            SleepTimer.state.collect { state ->
                sleepJob?.cancel()
                if (isFading) {
                    isFading = false
                    player.volume = 1f
                }
                player.pauseAtEndOfMediaItems = state.atEndOfSong
                val endsAt = state.endsAtMs ?: return@collect

                sleepJob = scope.launch { runSleepTimer(endsAt) }
            }
        }

        // The parts of a song that the community marked are skipped as the user chose
        sponsorBlock = SponsorBlock(scope).also { it.attach(player) }
        scope.launch { SettingsStore(this@PlaybackService).sponsorBlock.collect { sponsorBlock.settings = it } }

        // A song goes to the history as soon as it plays long enough with sound, and the rest of its time is added when it ends
        listenTracker = ListenTracker(this, player, scope).also { player.addListener(it) }

        mediaSession = MediaSession.Builder(this, CrossfadePlayer(player, crossfade))
            .setSessionActivity(openAppIntent())
            .setCallback(sessionCallback)
            .setMediaButtonPreferences(modeButtons())
            .build()

        // A TV that is connected takes the sound, a phone without the Google services has no cast and goes on without it
        castBridge = runCatching {
            CastBridge(this, scope, { player }, { CrossfadePlayer(player, crossfade) }, { next -> mediaSession?.player = next })
        }.getOrNull()
    }

    // Shuffle and repeat buttons of the notification, showing the mode that is on
    private fun modeButtons(): List<CommandButton> {
        // A station has no queue to shuffle or repeat
        if (isRadioTrack(player.currentMediaItem?.mediaId)) return emptyList()

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

        // In a room, what a guest does on the notification, the lock screen or the headset goes to the host as a request, the app asks by itself before it gets here
        override fun onPlayerCommandRequest(session: MediaSession, controller: MediaSession.ControllerInfo, playerCommand: Int): Int {
            if (controller.connectionHints.getBoolean(AppControllerKey)) return SessionResult.RESULT_SUCCESS
            val gate = PlayerConnection.controlGate ?: return SessionResult.RESULT_SUCCESS

            val action = when (playerCommand) {
                Player.COMMAND_PLAY_PAUSE -> ControlAction.PlayPause(!player.playWhenReady)
                Player.COMMAND_SEEK_TO_NEXT, Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM -> ControlAction.Next
                Player.COMMAND_SEEK_TO_PREVIOUS, Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM -> ControlAction.Previous
                Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM, Player.COMMAND_SEEK_BACK, Player.COMMAND_SEEK_FORWARD, Player.COMMAND_SEEK_TO_MEDIA_ITEM,
                Player.COMMAND_SET_REPEAT_MODE, Player.COMMAND_SET_SHUFFLE_MODE -> ControlAction.Blocked
                else -> return SessionResult.RESULT_SUCCESS
            }
            return if (gate.intercept(action)) SessionError.ERROR_PERMISSION_DENIED else SessionResult.RESULT_SUCCESS
        }

        override fun onCustomCommand(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            customCommand: SessionCommand,
            args: Bundle
        ): ListenableFuture<SessionResult> {
            // The buttons of shuffle and repeat are only for the host in a room
            if (controller.connectionHints.getBoolean(AppControllerKey).not() && PlayerConnection.controlGate?.intercept(ControlAction.Blocked) == true) {
                return Futures.immediateFuture(SessionResult(SessionError.ERROR_PERMISSION_DENIED))
            }

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
    // The user can ask for it to stop even while a song plays
    // A TV that is connected is left, the sound would go on with nobody holding the app
    override fun onTaskRemoved(rootIntent: Intent?) {
        saveQueue(now = true)
        if (castBridge?.isCasting == true) {
            castBridge?.stopRemote()
            stopSelf()
        } else if (playbackSettings.stopOnTaskRemoved) {
            player.pause()
            stopSelf()
        } else if (!player.playWhenReady || player.mediaItemCount == 0) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        saveQueue(now = true)
        getSystemService(AudioManager::class.java)?.unregisterAudioDeviceCallback(bluetoothCallback)
        crossfade.release()
        sponsorBlock.release()
        castBridge?.release()
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

    // A player of the queue, the first one when the service starts and the ones that take its place when a crossfade swaps them
    private fun createPlayer(): ExoPlayer {
        // Every player has a part that brings the songs to the same loudness, which does nothing until the user turns it on
        val normalizer = VolumeNormalizer()
        val renderers = object : DefaultRenderersFactory(this) {
            override fun buildAudioSink(context: Context, enableFloatOutput: Boolean, enableAudioTrackPlaybackParams: Boolean): AudioSink =
                DefaultAudioSink.Builder(context)
                    .setEnableFloatOutput(enableFloatOutput)
                    .setEnableAudioTrackPlaybackParams(enableAudioTrackPlaybackParams)
                    .setAudioProcessorChain(DefaultAudioSink.DefaultAudioProcessorChain(normalizer))
                    .build()
        }

        return ExoPlayer.Builder(this, renderers)
            .setMediaSourceFactory(DefaultMediaSourceFactory(createDataSourceFactory(), extractorsFactory))
            .setAudioAttributes(musicAttributes, true)
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .build()
            .also {
                normalizers[it] = normalizer
                it.skipSilenceEnabled = playbackSettings.skipSilence
            }
    }

    // Sets the gain of each player to the one of the song it plays, or leaves it alone when the user did not ask or the loudness is not known
    private fun applyNormalization() {
        normalizers.forEach { (each, normalizer) ->
            val gain = if (playbackSettings.normalizeAudio) runCatching { Loudness.gainMb(each.currentMediaItem?.mediaId, playbackSettings.loudness.targetLufs) }.getOrNull() else null
            normalizer.setTargetGain(gain ?: 0)
            normalizer.enabled = gain != null
        }
    }

    // Writes the queue a moment after the last change, or at once when the app is about to go away
    private fun saveQueue(now: Boolean = false) {
        if (!playbackSettings.persistentQueue) return
        saveJob?.cancel()

        val text = queueSaver.snapshot(player)
        if (now) {
            queueSaver.write(text)
        } else {
            saveJob = scope.launch {
                delay(SaveDebounceMillis)
                withContext(Dispatchers.IO) { queueSaver.write(text) }
            }
        }
    }

    // The sleep timer ends the music, lowering the volume in the last minute when it was asked, and waiting for the end of the song when it was asked
    private suspend fun runSleepTimer(endsAt: Long) {
        val settings = sleepSettings
        val canFade = settings.fadeOut && !settings.finishSong
        val remaining = endsAt - SystemClock.elapsedRealtime()
        val window = minOf(SleepFadeWindowMillis, remaining.coerceAtLeast(0L))

        if (canFade) {
            delay((remaining - window).coerceAtLeast(0L))
            isFading = true
            while (true) {
                val left = endsAt - SystemClock.elapsedRealtime()
                if (left <= 0L) break
                player.volume = (left.toFloat() / window).coerceIn(0f, 1f)
                delay(SleepFadeStepMillis)
            }
        } else {
            delay(remaining.coerceAtLeast(0L))
        }

        if (settings.finishSong) {
            SleepTimer.startAtEndOfSong()
        } else {
            player.pause()
            player.volume = 1f
            isFading = false
            SleepTimer.cancel()
        }
    }

    // The timer starts by itself when the music plays inside the window the user chose, once for each window
    private fun startAutoSleepTimer() {
        val settings = sleepSettings
        if (!settings.auto || SleepTimer.state.value.isActive) return

        val window = settings.activeWindow(LocalDateTime.now()) ?: return
        if (window == lastAutoWindow) return

        lastAutoWindow = window
        SleepTimer.startIn(settings.minutes)
        Toasts.show(getString(R.string.settings_sleep_auto_started, settings.minutes))
    }

    // While the user asked for it, the volume of the music is looked at every little while, the player does not tell when it changes
    private fun watchMute(isOn: Boolean) {
        muteJob?.cancel()
        muteJob = null
        if (!isOn) return

        val audio = getSystemService(AudioManager::class.java) ?: return
        muteJob = scope.launch {
            var wasMuted = audio.isMusicSilent()
            while (true) {
                delay(MuteCheckMillis)
                val isMuted = audio.isMusicSilent()
                if (isMuted == wasMuted) continue
                wasMuted = isMuted

                // The music pauses when the volume goes down to nothing, and goes on when it comes back, if it was the volume that paused it
                if (isMuted && player.isPlaying) {
                    isPausedByMute = true
                    player.pause()
                } else if (!isMuted && isPausedByMute) {
                    isPausedByMute = false
                    player.play()
                }
            }
        }
    }

    private fun AudioManager.isMusicSilent(): Boolean = getStreamVolume(AudioManager.STREAM_MUSIC) == 0 || isStreamMute(AudioManager.STREAM_MUSIC)

    // A Bluetooth device that connects makes the music go on, the ones that were already connected do not count
    private val bluetoothCallback = object : AudioDeviceCallback() {
        override fun onAudioDevicesAdded(addedDevices: Array<AudioDeviceInfo>) {
            val fresh = addedDevices.filter { it.isBluetoothOutput() && knownBluetooth.add(it.id) }
            if (fresh.isNotEmpty() && playbackSettings.resumeOnBluetooth && player.mediaItemCount > 0 && !player.isPlaying) player.play()
        }

        override fun onAudioDevicesRemoved(removedDevices: Array<AudioDeviceInfo>) {
            removedDevices.forEach { knownBluetooth.remove(it.id) }
        }
    }

    private fun registerBluetooth() {
        val audio = getSystemService(AudioManager::class.java) ?: return
        audio.getDevices(AudioManager.GET_DEVICES_OUTPUTS).filter { it.isBluetoothOutput() }.forEach { knownBluetooth.add(it.id) }
        audio.registerAudioDeviceCallback(bluetoothCallback, null)
    }

    private fun AudioDeviceInfo.isBluetoothOutput(): Boolean =
        type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP || (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && (type == AudioDeviceInfo.TYPE_BLE_HEADSET || type == AudioDeviceInfo.TYPE_BLE_SPEAKER))

    // The new player takes the place of the old one in the service, the session and the history
    private fun swapPlayer(from: ExoPlayer, to: ExoPlayer) {
        from.removeListener(playerListener)
        from.removeListener(listenTracker)
        listenTracker.finish()

        player = to
        applyNormalization()
        to.pauseAtEndOfMediaItems = SleepTimer.state.value.atEndOfSong
        to.addListener(playerListener)
        listenTracker = ListenTracker(this, to, scope).also { to.addListener(it) }
        sponsorBlock.attach(to)
        // While a TV plays the session keeps the player of the TV
        if (castBridge?.isCasting != true) mediaSession?.player = CrossfadePlayer(to, crossfade)
        mediaSession?.setMediaButtonPreferences(modeButtons())
    }

    // Finds the link of the next song ahead of time, so it starts at once when the crossfade needs it
    private fun prefetchStream(item: MediaItem) {
        val mediaId = item.mediaId
        if (isDeviceTrack(mediaId) || streamCache[mediaId] != null) return

        scope.launch(Dispatchers.IO) {
            StreamResolver.resolve(mediaId).onSuccess { streamCache.put(mediaId, it) }
        }
    }

    // Asks for the link of a song when the player opens it, reusing a link that is still valid
    private fun createDataSourceFactory(): DataSource.Factory {
        val upstream = DefaultDataSource.Factory(this, OkHttpDataSource.Factory(WavvyProxy.client))

        val resolving = ResolvingDataSource.Factory(upstream) { dataSpec ->
            // A request with no key is a piece of the list of a station, which goes as it is
            val mediaId = dataSpec.key ?: return@Factory dataSpec

            // A file of the phone or a station plays from where it is, there is no link to look for
            if (isDeviceTrack(mediaId)) return@Factory dataSpec
            streamCache[mediaId]?.let { return@Factory dataSpec.withResolvedStream(it) }

            val stream = runBlocking(Dispatchers.IO) { StreamResolver.resolve(mediaId) }
                .getOrElse { error -> throw IOException("Could not resolve the stream", error) }
            streamCache.put(mediaId, stream)
            dataSpec.withResolvedStream(stream)
        }

        // The files of the downloads come first for a song that is all there, the player never writes what it streams there
        Downloads.initialize(this)
        return DownloadFirstDataSource.factory(Downloads.cache, resolving)
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

        // The music stopped at the end of the song as the timer asked, so the timer is done
        override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
            if (!playWhenReady && reason == Player.PLAY_WHEN_READY_CHANGE_REASON_END_OF_MEDIA_ITEM) SleepTimer.cancel()
            if (playWhenReady) isPausedByMute = false
        }

        // A song that started playing gets its tries back, and a pause is a good moment to keep the place in the queue
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            if (isPlaying) {
                player.currentMediaItem?.mediaId?.let(retries::remove)
                startAutoSleepTimer()
            } else {
                saveQueue()
            }
        }

        // A new song brings its own loudness, and the queue changes with it
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            applyNormalization()
            saveQueue()
            // The notification gets the buttons of a song or none at all for a station
            mediaSession?.setMediaButtonPreferences(modeButtons())
        }

        override fun onTimelineChanged(timeline: Timeline, reason: Int) {
            saveQueue()
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

        // The sleep timer lowers the volume during its last minute, a little at a time
        const val SleepFadeWindowMillis = 60_000L
        const val SleepFadeStepMillis = 250L

        // The queue is written this long after the last change, and every few seconds while a song plays
        const val SaveDebounceMillis = 1000L
        const val SaveEveryMillis = 15_000L

        // How often the volume is looked at for the pause when the sound is off
        const val MuteCheckMillis = 400L

        // What the players play, music
        val musicAttributes: AudioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .build()

        // Commands of the shuffle and repeat buttons of the notification
        const val ToggleShuffleAction = "com.wavvy.app.TOGGLE_SHUFFLE"
        const val ToggleRepeatAction = "com.wavvy.app.TOGGLE_REPEAT"
        val ToggleShuffle = SessionCommand(ToggleShuffleAction, Bundle.EMPTY)
        val ToggleRepeat = SessionCommand(ToggleRepeatAction, Bundle.EMPTY)

        // YouTube serves audio as WebM or MP4, which are tried first, and the files of the phone come in the other formats, such as MP3 and FLAC
        val extractorsFactory = ExtractorsFactory {
            arrayOf(MatroskaExtractor(), FragmentedMp4Extractor(), Mp4Extractor()) + DefaultExtractorsFactory().createExtractors()
        }
    }
}

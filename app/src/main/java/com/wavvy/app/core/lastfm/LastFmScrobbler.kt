package com.wavvy.app.core.lastfm

// Android context
import android.content.Context
// Coroutines and reactive flows
import java.io.IOException
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
// Project resources
import com.wavvy.app.core.playback.PlayableTrack
import com.wavvy.app.core.playback.PlayerConnection
import com.wavvy.app.core.radio.isRadioTrack
import com.wavvy.app.features.settings.data.PrivacyPrefs

// How often the time listened is counted
private const val TickMillis = 1_000L

// Last.fm counts a song when half of it was heard or four minutes of it, and takes only songs of at least half a minute
private const val CountAfterMillis = 240_000L
private const val MinSongMillis = 30_000L

// A song that goes back by this much in its time starts again, as it does when it repeats
private const val RestartDropMillis = 5_000L

// Listenings older than this are not taken by Last.fm any more
private const val MaxAgeSeconds = 14L * 24 * 60 * 60

// The word YouTube Music puts after the name of an artist whose page is made by it
private const val TopicSuffix = " - Topic"

// Sends to Last.fm what the user listens to, while they are logged in and have it on
object LastFmScrobbler {
    private val scope = MainScope()
    private val queueLock = Mutex()
    private var appContext: Context? = null

    // The song that plays, when it began in seconds, how long it was heard and whether it was already counted
    private var currentId: String? = null
    private var startedAt = 0L
    private var playedMs = 0L
    private var isCounted = false
    private var lastPosition = 0L

    // Follows the player, called once when the app opens
    fun start(context: Context) {
        if (appContext != null || !LastFmApi.isAvailable) return
        val app = context.applicationContext
        appContext = app

        scope.launch { PlayerConnection.track.collect { begin(it) } }
        scope.launch {
            while (true) {
                delay(TickMillis)
                if (PlayerConnection.isPlaying.value) tick()
            }
        }

        // What waited for the network goes when the login is there, and again each time the app opens
        scope.launch { LastFmStore(app).isConnected.distinctUntilChanged().filter { it }.collect { deliver(emptyList()) } }
    }

    // A new song begins, the count starts over and Last.fm is told what plays now
    private fun begin(track: PlayableTrack?) {
        currentId = track?.takeIf(::isCountable)?.id
        startedAt = System.currentTimeMillis() / MillisPerSecond
        playedMs = 0L
        isCounted = false
        lastPosition = PlayerConnection.positionMs
        if (currentId != null && track != null) announce(track)
    }

    // The time of the song that plays is counted, and the song is sent when it was heard enough
    private fun tick() {
        val track = PlayerConnection.track.value?.takeIf { it.id == currentId } ?: return
        val position = PlayerConnection.positionMs

        // The song came back to the start, so this is another listening of it
        if (isCounted && position < lastPosition - RestartDropMillis) {
            startedAt = System.currentTimeMillis() / MillisPerSecond
            playedMs = 0L
            isCounted = false
            announce(track)
        }
        lastPosition = position
        playedMs += TickMillis

        val needed = if (track.durationMs > 0) minOf(track.durationMs / 2, CountAfterMillis) else CountAfterMillis
        if (!isCounted && playedMs >= needed) {
            isCounted = true
            deliver(listOf(scrobbleOf(track, startedAt)))
        }
    }

    // Only music with a name and an artist counts, not a station, and not while the history is paused
    private fun isCountable(track: PlayableTrack): Boolean =
        !isRadioTrack(track.id) &&
            !PrivacyPrefs.pauseHistory &&
            track.title.isNotBlank() &&
            !track.artist.isNullOrBlank() &&
            (track.durationMs == 0L || track.durationMs >= MinSongMillis)

    private fun scrobbleOf(track: PlayableTrack, timestamp: Long) =
        Scrobble(track.artist.orEmpty().removeSuffix(TopicSuffix), track.title, null, (track.durationMs / MillisPerSecond).toInt(), timestamp)

    // Tells Last.fm what plays now, a failure of it is not worth a retry
    private fun announce(track: PlayableTrack) {
        val app = appContext ?: return
        scope.launch {
            val store = LastFmStore(app)
            val session = store.session.first() ?: return@launch
            val settings = store.settings.first()
            if (!settings.isEnabled || !settings.showNowPlaying) return@launch
            runCatching { LastFmApi.nowPlaying(session, scrobbleOf(track, startedAt)) }
        }
    }

    // Sends the songs with the ones that waited, and keeps what did not go for later
    private fun deliver(fresh: List<Scrobble>) {
        val app = appContext ?: return
        scope.launch {
            queueLock.withLock {
                val store = LastFmStore(app)
                val session = store.session.first()
                val settings = store.settings.first()

                // Without a login or with it off, nothing is sent and nothing is kept
                if (session == null || !settings.isEnabled) return@withLock

                val oldest = System.currentTimeMillis() / MillisPerSecond - MaxAgeSeconds
                val pending = (store.queue() + fresh).filter { it.timestamp > oldest }.toMutableList()
                while (pending.isNotEmpty()) {
                    val batch = pending.take(LastFmBatchSize)
                    try {
                        LastFmApi.scrobble(session, batch)
                        repeat(batch.size) { pending.removeAt(0) }
                    } catch (error: LastFmException) {
                        if (error.code == LastFmBadSession) {
                            store.clear()
                            return@withLock
                        }
                        // A refusal that will not change is dropped, the others wait for another time
                        if (error.isTemporary) break
                        repeat(batch.size) { pending.removeAt(0) }
                    } catch (error: IOException) {
                        break
                    }
                }
                store.saveQueue(pending)
            }
        }
    }

    private const val MillisPerSecond = 1_000L
}

package com.wavvy.app.features.recap.data

// Android context
import android.content.Context
import android.media.audiofx.LoudnessEnhancer
// Media player
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.ResolvingDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
// Coroutines
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import java.io.IOException
import kotlin.random.Random
// Project resources
import com.wavvy.app.core.network.WavvyProxy
import com.wavvy.app.core.playback.StreamResolver
import com.wavvy.app.core.playback.withResolvedStream
import com.wavvy.app.features.artist.data.ArtistRepository

// How long a song fades into the next one, and how often the volumes change while it does
private const val FadeMillis = 1000L
private const val FadeStepMillis = 30L

// The longest the fade waits for the new song to be ready to play, so a song that is late does not hold the music back for good
private const val ReadyWaitMillis = 5_000L

// Where a song of YouTube Music starts, since the beginning is rarely the best part, and the least that must be left after it
private const val ClipStartMillis = 70_000L
private const val ClipLeastMillis = 30_000L

// The friendly song that plays while the story tells things that do not give a song away, and the length of it from where a place to start is drawn, which leaves the end of the song out
private const val AmbientVideoId = "t5Q5FFWRxCE"
private const val AmbientDrawMillis = 29 * 60_000L

// How much louder the friendly song is made than the volume of the player allows, in millibels, since its recording is quiet next to the songs of the artists
private const val AmbientGainMillibels = 800

// The music of the story: a friendly song at the start, a piece of a song of the artist or of the song when the pages talk about them, always fading from one to the other
// Two players take turns, and the next song is made ready on the free one while the page before it is still on the screen, so the fade is heard when the page comes
class RecapAudio(private val context: Context) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    // What each player plays: a piece of a song starting at a fixed place, or the friendly song, which is the only one that is made louder
    private val isClip = BooleanArray(2)
    private val isAmbientTrack = BooleanArray(2)
    private val boosts = arrayOfNulls<LoudnessEnhancer>(2)
    private val players = List(2) { build(it) }
    private val levels = FloatArray(players.size)
    private var active = 0
    private var key: String? = null
    private var fade: Job? = null
    private var master = 1f

    // The song made ready on the free player, with the place it is on
    private var ready: Pair<String, Int>? = null

    // The most listened song of each artist, asked once
    private val topSongs = HashMap<String, String?>()

    // True while the friendly song is the one playing
    val isAmbient: Boolean get() = key?.startsWith(AmbientKey) == true

    // The friendly song, from a place that is new every time, it stays if it is already playing
    fun playAmbient() {
        if (isAmbient) return
        start(ambientKey(), clipOf(AmbientVideoId), drawStart(), isPiece = false, isAmbientSong = true)
    }

    // A piece of a song of YouTube Music
    fun playSong(videoId: String) = start("$SongKey$videoId", clipOf(videoId), ClipStartMillis, isPiece = true, isAmbientSong = false)

    // A piece of the most listened song of an artist, which is the first of its page, nothing changes when it cannot be found
    suspend fun playArtist(artistId: String) {
        playSong(topSongOf(artistId) ?: return)
    }

    // Gets the friendly song ready on the free player, still and silent, for the page that comes next
    suspend fun prepareAmbient() {
        if (isAmbient || ready?.first?.startsWith(AmbientKey) == true) return
        prepare(ambientKey(), clipOf(AmbientVideoId), drawStart(), isPiece = false, isAmbientSong = true)
    }

    // Gets a song of YouTube Music ready on the free player, still and silent, for the page that comes next
    suspend fun prepareSong(videoId: String) {
        val newKey = "$SongKey$videoId"
        if (newKey == key || newKey == ready?.first) return
        prepare(newKey, clipOf(videoId), ClipStartMillis, isPiece = true, isAmbientSong = false)
    }

    suspend fun prepareArtist(artistId: String) {
        prepareSong(topSongOf(artistId) ?: return)
    }

    // Silences everything or brings the sound back
    fun setMuted(isMuted: Boolean) {
        master = if (isMuted) 0f else 1f
        applyVolumes()
    }

    fun pause() = players.forEach { it.pause() }

    fun resume() {
        if (key != null) players[active].play()
    }

    fun release() {
        scope.cancel()
        boosts.forEach { it?.release() }
        players.forEach { it.release() }
    }

    private fun ambientKey(): String = "$AmbientKey${Random.nextInt()}"

    // A place to start the friendly song, drawn before it loads so it plays from there at once
    private fun drawStart(): Long = Random.nextLong(AmbientDrawMillis)

    private fun clipOf(videoId: String): MediaItem = MediaItem.Builder().setUri(videoId).setCustomCacheKey(videoId).build()

    private suspend fun topSongOf(artistId: String): String? =
        topSongs.getOrPut(artistId) { ArtistRepository(context).load(artistId).getOrNull()?.topSongs?.firstOrNull()?.id }

    private suspend fun prepare(newKey: String, item: MediaItem, startMs: Long, isPiece: Boolean, isAmbientSong: Boolean) {
        // The free player may still be fading out the song before, which is left to end
        fade?.join()
        val index = 1 - active
        load(index, item, startMs, isPiece, isAmbientSong, playNow = false)
        ready = newKey to index
    }

    // Makes the friendly song louder while it is the one on a player, and leaves everything else as it is
    private fun applyBoost(index: Int) {
        boosts[index]?.runCatching {
            setTargetGain(if (isAmbientTrack[index]) AmbientGainMillibels else 0)
            enabled = isAmbientTrack[index]
        }
    }

    // The effect that makes it louder is tied to the audio session of the player, which may change
    private fun newBoost(index: Int, sessionId: Int) {
        boosts[index]?.release()
        boosts[index] = runCatching { LoudnessEnhancer(sessionId) }.getOrNull()
        applyBoost(index)
    }

    private fun load(index: Int, item: MediaItem, startMs: Long, isPiece: Boolean, isAmbientSong: Boolean, playNow: Boolean) {
        isClip[index] = isPiece
        isAmbientTrack[index] = isAmbientSong
        applyBoost(index)
        players[index].apply {
            levels[index] = 0f
            applyVolumes()
            setMediaItem(item, startMs)
            repeatMode = Player.REPEAT_MODE_ONE
            playWhenReady = playNow
            prepare()
        }
    }

    // Brings the new song in while the one that plays goes out, both at the same time, once the new one is ready to be heard
    private fun start(newKey: String, item: MediaItem, startMs: Long, isPiece: Boolean, isAmbientSong: Boolean) {
        if (newKey == key) return
        key = newKey

        val outgoing = active
        val prepared = if (isAmbientSong) ready?.takeIf { it.first.startsWith(AmbientKey) } else ready?.takeIf { it.first == newKey }
        val incoming = prepared?.second ?: (1 - active)
        ready = null
        if (prepared == null) load(incoming, item, startMs, isPiece, isAmbientSong, playNow = true)
        active = incoming
        players[incoming].playWhenReady = true

        fade?.cancel()
        fade = scope.launch {
            // The old song goes on at its own volume until the new one has something to play
            withTimeoutOrNull(ReadyWaitMillis) {
                while (players[incoming].playbackState != Player.STATE_READY) delay(FadeStepMillis)
            }

            val from = levels[outgoing]
            val steps = (FadeMillis / FadeStepMillis).toInt()
            for (step in 1..steps) {
                val share = step.toFloat() / steps
                levels[incoming] = share
                levels[outgoing] = from * (1f - share)
                applyVolumes()
                delay(FadeStepMillis)
            }
            players[outgoing].pause()
        }
    }

    private fun applyVolumes() = players.forEachIndexed { index, player -> player.volume = levels[index] * master }

    // A player that brings the songs of YouTube Music by their links, as the player of the app does
    private fun build(index: Int): ExoPlayer {
        val upstream = DefaultDataSource.Factory(context, OkHttpDataSource.Factory(WavvyProxy.client))
        val resolving = ResolvingDataSource.Factory(upstream) { dataSpec ->
            val videoId = dataSpec.key ?: return@Factory dataSpec
            val stream = runBlocking(Dispatchers.IO) { StreamResolver.resolve(videoId) }
                .getOrElse { error -> throw IOException("Could not resolve the stream", error) }
            dataSpec.withResolvedStream(stream)
        }

        return ExoPlayer.Builder(context)
            .setMediaSourceFactory(DefaultMediaSourceFactory(resolving))
            .setAudioAttributes(AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_MUSIC).build(), false)
            .build()
            .also { player ->
                newBoost(index, player.audioSessionId)
                player.addListener(object : Player.Listener {
                    override fun onAudioSessionIdChanged(audioSessionId: Int) = newBoost(index, audioSessionId)

                    override fun onPlaybackStateChanged(state: Int) {
                        val duration = player.duration
                        if (state != Player.STATE_READY || duration == C.TIME_UNSET) return

                        // A song shorter than where the piece starts is taken from a third of its way instead
                        if (isClip[index] && player.currentPosition >= duration - ClipLeastMillis) player.seekTo(duration / 3)
                    }
                })
            }
    }

    private companion object {
        const val AmbientKey = "ambient:"
        const val SongKey = "song:"
    }
}

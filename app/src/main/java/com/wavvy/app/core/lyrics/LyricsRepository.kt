package com.wavvy.app.core.lyrics

// Android context and web storage
import android.content.Context
import android.webkit.CookieManager
// Coroutines
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
// Java utilities
import java.util.Locale
// Project resources
import com.wavvy.app.core.innertube.MusicOrigin
import com.wavvy.app.core.innertube.VisitorStore
import com.wavvy.app.core.innertube.YouTubeSession
import com.wavvy.app.core.history.PlayHistory
import com.wavvy.app.core.innertube.deviceLocale
import com.wavvy.app.core.playback.StreamResolver
import com.wavvy.app.features.auth.data.Entry
import com.wavvy.app.features.auth.data.EntryStore

// A word of a line timed word by word, with when it starts and ends
class LyricWord(
    val text: String,
    val startMs: Long,
    val endMs: Long
)

// One line of lyrics, with the moment it is sung when the lyrics are timed, and its words when they are timed one by one
class LyricLine(
    val timeMs: Long,
    val text: String,
    val words: List<LyricWord> = emptyList()
)

// Lyrics of a song, timed when every line has its moment, with the song the source says they belong to when it says
class SongLyrics(
    val lines: List<LyricLine>,
    val isSynced: Boolean,
    val title: String? = null,
    val artist: String? = null
) {
    // The same lyrics with the name of the song they were found for
    fun named(title: String?, artist: String?): SongLyrics = SongLyrics(lines, isSynced, title, artist)
}

// Text of lyrics as a library gave it, with the song it belongs to when the library says
internal class FoundText(
    val text: String,
    val title: String?,
    val artist: String?
)

// Lyrics one source brought for a manual search, and the song they are for
// The kind of the lyrics and the place of the source in the chosen order tell where it stands among the others, as the answers come one by one
class LyricsMatch(
    val source: LyricsSource,
    val lyrics: SongLyrics,
    val title: String,
    val artist: String,
    val quality: Int,
    val order: Int
)

// Finds the lyrics of a song in the sources the user chose, by default BetterLyrics, Paxsenix and LyricsPlus, then LrcLib and KuGou,
// then YouTube Music and the captions, and translates them, keeping both for the session
object LyricsRepository {
    // Timestamps of a line, minutes, seconds and the fraction of a second with two or three digits
    private val timestamp = Regex("""\[(\d{1,3}):(\d{2})(?:[.:](\d{1,3}))?]""")

    // Marks of the extended format, the time of a word and the voice that sings a line
    private val wordStamp = Regex("""<\d{1,3}:\d{2}(?:[.:]\d{1,3})?>""")
    private val voiceTag = Regex("""^\s*v\d+:\s*""")
    private val spaces = Regex("""\s+""")
    private const val MillisPerSecond = 1000L
    private const val SecondsPerMinute = 60L
    private const val CentisecondDigits = 2
    private const val MillisPerCentisecond = 10L
    private const val MinimumLines = 4

    // Kinds of lyrics from the best to the plainest, and how long the sources that are left may take once something was found
    private const val WordQuality = 3
    private const val TimedQuality = 2
    private const val PlainQuality = 1
    private const val UpgradeWaitMillis = 4_000L

    // Lyrics and translations already found, an empty value means the song has none
    private val lyricsCache = HashMap<String, SongLyrics?>()
    private val translationCache = HashMap<String, LyricsTranslation?>()

    // Lyrics the user picked by hand for a video, they come before any search
    private val chosenLyrics = HashMap<String, SongLyrics>()

    // Where the writes to the device happen, so choosing lyrics never waits for the database
    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Lyrics of a video, from the cache or from the first of the chosen sources that has them, in the chosen order
    suspend fun lyrics(
        context: Context,
        videoId: String,
        title: String,
        artist: String,
        durationSeconds: Int,
        sources: List<LyricsSource>,
        // Told with the first lyrics that come, so they can show at once while a better kind is still waited for
        onFirst: (SongLyrics) -> Unit = {}
    ): SongLyrics? {
        synchronized(chosenLyrics) { chosenLyrics[videoId]?.let { return it } }

        // The lyrics picked by hand in an earlier session, kept on the device, come before any search too
        withContext(Dispatchers.IO) { PlayHistory.chosenLyrics(context, videoId)?.let(::songLyricsFromJson) }?.let { saved ->
            synchronized(chosenLyrics) { chosenLyrics[videoId] = saved }
            return saved
        }

        val key = "$videoId:${sources.joinToString { it.name }}"
        synchronized(lyricsCache) { if (lyricsCache.containsKey(key)) return lyricsCache[key] }

        val lyrics = withContext(Dispatchers.IO) { best(context, sources, videoId, title, artist, durationSeconds, onFirst) }

        synchronized(lyricsCache) { lyricsCache[key] = lyrics }
        return lyrics
    }

    // How good lyrics are, word by word first, then timed, then plain
    private fun quality(lyrics: SongLyrics): Int = when {
        lyrics.lines.any { it.words.isNotEmpty() } -> WordQuality
        lyrics.isSynced -> TimedQuality
        else -> PlainQuality
    }

    // Asks every source at once and keeps the best lyrics, the order of the sources only breaks a tie
    // The answers are taken in the order they come, so a slow source never holds back the ones that already answered
    // The first lyrics are told as soon as they come, the search ends when the best kind is found, and once something is found the others only have a little while to do better
    private suspend fun best(
        context: Context,
        sources: List<LyricsSource>,
        videoId: String,
        title: String,
        artist: String,
        durationSeconds: Int,
        onFirst: (SongLyrics) -> Unit
    ): SongLyrics? = coroutineScope {
        val answers = Channel<Pair<Int, SongLyrics?>>(Channel.UNLIMITED)
        val pending = sources.mapIndexed { index, source ->
            launch { answers.send(index to fetch(context, source, videoId, title, artist, durationSeconds)) }
        }
        var best: SongLyrics? = null
        var bestIndex = sources.size
        var deadline = 0L
        var received = 0

        while (received < sources.size) {
            received++
            val (index, found) = if (best == null) {
                answers.receive()
            } else {
                withTimeoutOrNull((deadline - System.currentTimeMillis()).coerceAtLeast(0L)) { answers.receive() } ?: break
            }
            if (found == null) continue

            val current = best
            if (current == null) {
                deadline = System.currentTimeMillis() + UpgradeWaitMillis
                onFirst(found)
            }
            if (current == null || quality(found) > quality(current) || (quality(found) == quality(current) && index < bestIndex)) {
                best = found
                bestIndex = index
            }
            if (best != null && quality(best) == WordQuality) break
        }

        pending.forEach { it.cancel() }
        best
    }

    // Forgets the lyrics and translations of a video, so the next request searches again, the ones picked by hand included, even the saved ones
    // It waits for the saved ones to be gone, so the search that follows never reads them again
    suspend fun forget(context: Context, videoId: String) {
        withContext(Dispatchers.IO) { PlayHistory.deleteChosenLyrics(context, videoId) }
        synchronized(chosenLyrics) { chosenLyrics.remove(videoId) }
        synchronized(lyricsCache) { lyricsCache.keys.removeAll { it.startsWith("$videoId:") } }
        synchronized(translationCache) { translationCache.keys.removeAll { it.startsWith("$videoId:") } }
    }

    // Sources that look the song up by its video, so what they bring is always the song that plays
    private val byVideo = setOf(LyricsSource.YouTubeMusic, LyricsSource.YouTubeCaptions)

    // Searches every source at once with the title and artist the user typed, and gives what each one found as soon as it answers
    // The flow ends when the last source answered, a source that finds nothing gives nothing
    // A source that does not say which song it found shows the typed words, or the song that plays when it works by video
    fun search(
        context: Context,
        videoId: String,
        title: String,
        artist: String,
        playingTitle: String,
        playingArtist: String,
        durationSeconds: Int,
        sources: List<LyricsSource>
    ): Flow<LyricsMatch> = channelFlow {
        sources.forEachIndexed { index, source ->
            launch {
                fetch(context, source, videoId, title, artist, durationSeconds)?.let { lyrics ->
                    val fallback = if (source in byVideo) playingTitle to playingArtist else title to artist
                    send(LyricsMatch(source, lyrics, lyrics.title ?: fallback.first, lyrics.artist ?: fallback.second, quality(lyrics), index))
                }
            }
        }
    }.flowOn(Dispatchers.IO)

    // Keeps the lyrics the user picked for the video, on the device too, until the search is asked again, the translation of the older ones is dropped
    fun choose(context: Context, videoId: String, lyrics: SongLyrics) {
        ioScope.launch { PlayHistory.saveChosenLyrics(context, videoId, lyrics.toJson()) }
        synchronized(chosenLyrics) { chosenLyrics[videoId] = lyrics }
        synchronized(translationCache) { translationCache.keys.removeAll { it.startsWith("$videoId:") } }
    }

    // Lyrics of one source, a source counts only when it brings enough lines, a short answer is usually a header of another song
    private suspend fun fetch(
        context: Context,
        source: LyricsSource,
        videoId: String,
        title: String,
        artist: String,
        durationSeconds: Int
    ): SongLyrics? = when (source) {
        LyricsSource.BetterLyrics -> BetterLyrics.getLyrics(title, artist, durationSeconds).getOrNull()
        LyricsSource.LrcLib -> LrcLib.getLyrics(title, artist, durationSeconds).getOrNull()?.let { parseText(it.text).named(it.title, it.artist) }
        LyricsSource.KuGou -> KuGou.getLyrics(title, artist, durationSeconds).getOrNull()?.let { parseText(it.text).named(it.title, it.artist) }
        LyricsSource.YouTubeMusic -> YouTubeLyrics.getLyrics(currentSession(context), videoId).getOrNull()?.let(::parseText)
        LyricsSource.Paxsenix -> Paxsenix.getLyrics(title, artist, durationSeconds).getOrNull()
        LyricsSource.LyricsPlus -> LyricsPlus.getLyrics(title, artist, durationSeconds).getOrNull()
        LyricsSource.YouTubeCaptions -> StreamResolver.transcript(videoId).getOrNull()?.let(::parseText)
    }?.takeIf { it.lines.size >= MinimumLines }

    // Lyrics in the chosen language, the one of the device when none is chosen, empty when they are already in it or the translation failed
    suspend fun translation(videoId: String, lyrics: SongLyrics, targetLanguage: String): LyricsTranslation? {
        val target = targetLanguage.ifEmpty { translationLanguageOf(Locale.getDefault()) }
        // The lines are part of the key, lyrics of another source split the song in other lines and need their own translation
        val key = "$videoId:$target:${lyrics.lines.size}"
        synchronized(translationCache) { if (translationCache.containsKey(key)) return translationCache[key] }

        val translation = withContext(Dispatchers.IO) {
            LyricsTranslator.translate(lyrics.lines.map { it.text }, target).getOrNull()
        }?.takeIf { !it.sourceLanguage.startsWith(target.substringBefore('-')) }

        synchronized(translationCache) { translationCache[key] = translation }
        return translation
    }

    // Timed lyrics in the LRC format, the marks of each word of the extended format left out, or plain lines one after the other
    internal fun parseText(text: String): SongLyrics {
        val timed = text.lines().flatMap { line ->
            val stamps = timestamp.findAll(line).toList()
            if (stamps.isEmpty()) return@flatMap emptyList()
            val words = line.substring(stamps.last().range.last + 1).replace(wordStamp, "").replace(voiceTag, "").replace(spaces, " ").trim()
            stamps.map { LyricLine(timeOf(it), words) }
        }.filter { it.text.isNotBlank() }.sortedBy { it.timeMs }

        if (timed.isNotEmpty()) return SongLyrics(timed, isSynced = true)

        val plain = text.lines().map { it.trim() }.filter { it.isNotEmpty() }.mapIndexed { index, line -> LyricLine(index.toLong(), line) }
        return SongLyrics(plain, isSynced = false)
    }

    // Milliseconds of a timestamp, two digit fractions are hundredths and three digit ones are thousandths
    private fun timeOf(match: MatchResult): Long {
        val (minutes, seconds, fraction) = match.destructured
        val fractionMs = when {
            fraction.isEmpty() -> 0L
            fraction.length <= CentisecondDigits -> fraction.padEnd(CentisecondDigits, '0').toLong() * MillisPerCentisecond
            else -> fraction.toLong()
        }
        return (minutes.toLong() * SecondsPerMinute + seconds.toLong()) * MillisPerSecond + fractionMs
    }

    // The same identity the Home uses, with the account when the user signed in with Google
    private suspend fun currentSession(context: Context): YouTubeSession {
        val locale = deviceLocale()
        val cookies = if (EntryStore(context).entry.first() == Entry.Google) {
            withContext(Dispatchers.Main) { CookieManager.getInstance().getCookie(MusicOrigin) }
        } else {
            null
        }
        return YouTubeSession(cookies = cookies, visitorData = VisitorStore(context).get(locale), locale = locale)
    }
}

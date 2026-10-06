package com.wavvy.app.core.lyrics

// Ktor networking
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
// JSON parsing
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
// Coroutines
import kotlinx.coroutines.CancellationException

// Lyrics of the Binimum API and of the LyricsPlus servers, word by word when they have it, adapted from Metrolist (GPL-3.0)
internal object LyricsPlus {
    private const val BinimumUrl = "https://lyrics-api.binimum.org/"
    private const val WordTiming = "word"

    // Mirrors of LyricsPlus, the last one that answered is asked first
    private val servers = listOf(
        "https://lyricsplus.binimum.org",
        "https://lyricsplus.atomix.one",
        "https://lyricsplus.prjktla.my.id",
        "https://lyricsplus-seven.vercel.app"
    )

    @Volatile
    private var lastWorkingServer: String? = null

    // Word timed lyrics of Binimum first, then the ones of LyricsPlus, then whatever Binimum had
    suspend fun getLyrics(title: String, artist: String, durationSeconds: Int): Result<SongLyrics> = runCatching {
        if (title.isBlank() || artist.isBlank()) throw IllegalStateException("Missing title or artist")

        val binimum = runCatchingKeepCancel { fetchBinimum(title, artist, durationSeconds) }
        if (binimum != null && binimum.second) return@runCatching binimum.first

        val fromServers = fetchServers(title, artist, durationSeconds)
        when {
            fromServers != null && fromServers.lines.any { it.words.isNotEmpty() } -> fromServers
            binimum != null -> binimum.first
            fromServers != null -> fromServers
            else -> throw IllegalStateException("Lyrics unavailable")
        }
    }

    // Binimum gives a link to a TTML file, and whether its timing is word by word
    private suspend fun fetchBinimum(title: String, artist: String, durationSeconds: Int): Pair<SongLyrics, Boolean>? {
        val answer = lyricsJson.parseToJsonElement(
            lyricsHttp.get(BinimumUrl) {
                parameter("track", title)
                parameter("artist", artist)
                if (durationSeconds > 0) parameter("duration", durationSeconds)
            }.bodyAsText()
        ).jsonObject

        val result = answer["results"]?.jsonArray?.map { it.jsonObject }?.firstOrNull { !it.text("lyricsUrl").isNullOrBlank() } ?: return null
        val ttml = lyricsHttp.get(result.text("lyricsUrl").orEmpty()).bodyAsText()
        val lines = BetterLyrics.parse(ttml).takeIf { it.isNotEmpty() } ?: return null
        return SongLyrics(lines.sortedBy { it.timeMs }, isSynced = true) to result.text("timing_type").equals(WordTiming, ignoreCase = true)
    }

    // Asks each mirror until one has the lyrics
    private suspend fun fetchServers(title: String, artist: String, durationSeconds: Int): SongLyrics? {
        val ordered = lastWorkingServer?.let { last -> listOf(last) + servers.filter { it != last } } ?: servers
        for (server in ordered) {
            val lyrics = runCatchingKeepCancel {
                val body = lyricsHttp.get("$server/v2/lyrics/get") {
                    parameter("title", title)
                    parameter("artist", artist)
                    if (durationSeconds > 0) parameter("duration", durationSeconds)
                }.bodyAsText()
                convert(lyricsJson.parseToJsonElement(body).jsonObject)
            }
            if (lyrics != null) {
                lastWorkingServer = server
                return lyrics
            }
        }
        return null
    }

    // Lines of a LyricsPlus answer, the syllables of the main voice and of the background vocals as separate lines
    private fun convert(answer: JsonObject): SongLyrics? {
        val isWordTimed = answer.text("type").equals(WordTiming, ignoreCase = true)
        val lines = answer["lyrics"]?.jsonArray?.flatMap { element ->
            val line = element.jsonObject
            val syllables = line["syllabus"]?.jsonArray?.map { it.jsonObject }.orEmpty()
            val main = syllables.filter { it["isBackground"]?.jsonPrimitive?.booleanOrNull != true }
            val background = syllables.filter { it["isBackground"]?.jsonPrimitive?.booleanOrNull == true }
            val start = line["time"]?.jsonPrimitive?.longOrNull ?: 0L

            val mainLine = if (isWordTimed && main.isNotEmpty()) {
                wordsLine(main)
            } else {
                line.text("text")?.trim()?.takeIf { it.isNotEmpty() && (main.isNotEmpty() || background.isEmpty()) }?.let { LyricLine(start, it) }
            }
            val backgroundLine = background.takeIf { isWordTimed && it.isNotEmpty() }?.let(::wordsLine)
            listOfNotNull(mainLine, backgroundLine)
        }.orEmpty()

        return lines.takeIf { it.isNotEmpty() }?.let { SongLyrics(it.sortedBy { line -> line.timeMs }, isSynced = true) }
    }

    // A line built from timed syllables, the spaces come inside the syllables
    private fun wordsLine(syllables: List<JsonObject>): LyricLine? {
        val words = syllables.mapNotNull { syllable ->
            val text = syllable.text("text")?.trim()?.takeIf { it.isNotEmpty() } ?: return@mapNotNull null
            val start = syllable["time"]?.jsonPrimitive?.longOrNull ?: 0L
            val duration = syllable["duration"]?.jsonPrimitive?.longOrNull ?: 0L
            LyricWord(text, start, start + duration)
        }
        val text = syllables.joinToString("") { it.text("text").orEmpty() }.trim()
        return text.takeIf { it.isNotEmpty() && words.isNotEmpty() }?.let { LyricLine(words.first().startMs, it, words) }
    }

    // Runs a request, keeping a cancellation and turning any other failure into nothing
    private suspend fun <T> runCatchingKeepCancel(block: suspend () -> T?): T? =
        try {
            block()
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            null
        }

    // Text of a field, empty when it is missing or null
    private fun JsonObject.text(key: String): String? = this[key]?.jsonPrimitive?.contentOrNull
}

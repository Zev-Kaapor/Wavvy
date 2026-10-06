package com.wavvy.app.core.lyrics

// Ktor networking
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.http.encodeURLParameter
// JSON parsing
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
// Encoding and math
import kotlin.io.encoding.Base64
import kotlin.math.abs
import kotlin.math.min

// Lyrics library of KuGou, strong on Asian songs, adapted from Metrolist (GPL-3.0), which took it from ViMusic
internal object KuGou {
    private const val SongSearchUrl = "https://mobileservice.kugou.com/api/v3/search/song"
    private const val LyricsSearchUrl = "https://lyrics.kugou.com/search"
    private const val LyricsDownloadUrl = "https://lyrics.kugou.com/download"

    // Songs asked per search, lines checked at each end for credits, and seconds the length may differ
    private const val PageSize = 8
    private const val HeadCutLimit = 30
    private const val DurationTolerance = 8
    private const val MillisPerSecond = 1000

    // Only timed lines are kept, and the ones with credits such as the composer are cut from the ends
    private val acceptedLine = Regex("""\[(\d\d):(\d\d)\.(\d{2,3})].*""")
    private val creditLine = Regex(""".+].+[:：].+""")

    // Lyrics a search can give, as its id and access key, with the song it is for when the answer says
    private data class Candidate(val id: Long, val accessKey: String, val title: String?, val artist: String?)

    // Timed lyrics of the song that matches the length
    suspend fun getLyrics(title: String, artist: String, durationSeconds: Int): Result<FoundText> = runCatching {
        val keyword = "${normalizeTitle(title)} - ${normalizeArtist(artist)}"
        val candidate = findCandidate(keyword, durationSeconds) ?: throw IllegalStateException("No lyrics candidate")
        FoundText(download(candidate.id, candidate.accessKey), candidate.title, candidate.artist)
    }

    // Lyrics found through the songs of the search, or through the lyrics search itself
    private suspend fun findCandidate(keyword: String, durationSeconds: Int): Candidate? {
        val songs = lyricsJson.parseToJsonElement(
            lyricsHttp.get(SongSearchUrl) {
                parameter("version", 9108)
                parameter("plat", 0)
                parameter("pagesize", PageSize)
                parameter("showtype", 0)
                url.encodedParameters.append("keyword", keyword.encodeURLParameter(spaceToPlus = false))
            }.bodyAsText()
        ).jsonObject["data"]?.jsonObject?.get("info")?.jsonArray.orEmpty()

        for (song in songs) {
            val info = song.jsonObject
            val songDuration = info["duration"]?.jsonPrimitive?.intOrNull ?: continue
            val hash = info.text("hash") ?: continue
            if (durationSeconds > 0 && abs(songDuration - durationSeconds) > DurationTolerance) continue
            searchLyrics { parameter("hash", hash) }.firstOrNull()
                ?.let { return it.copy(title = it.title ?: info.text("songname"), artist = it.artist ?: info.text("singername")) }
        }

        return searchLyrics {
            if (durationSeconds > 0) parameter("duration", durationSeconds * MillisPerSecond)
            url.encodedParameters.append("keyword", keyword.encodeURLParameter(spaceToPlus = false))
        }.firstOrNull()
    }

    // Lyrics search, each candidate with its id and access key
    private suspend fun searchLyrics(extra: HttpRequestBuilder.() -> Unit): List<Candidate> {
        val text = lyricsHttp.get(LyricsSearchUrl) {
            parameter("ver", 1)
            parameter("man", "yes")
            parameter("client", "pc")
            extra()
        }.bodyAsText()

        return lyricsJson.parseToJsonElement(text).jsonObject["candidates"]?.jsonArray.orEmpty().mapNotNull { element ->
            val candidate = element.jsonObject
            val id = candidate["id"]?.jsonPrimitive?.longOrNull ?: return@mapNotNull null
            val key = candidate.text("accesskey") ?: return@mapNotNull null
            Candidate(id, key, candidate.text("song"), candidate.text("singer"))
        }
    }

    // Lyrics of a candidate, decoded and without the credits
    private suspend fun download(id: Long, accessKey: String): String {
        val text = lyricsHttp.get(LyricsDownloadUrl) {
            parameter("fmt", "lrc")
            parameter("charset", "utf8")
            parameter("client", "pc")
            parameter("ver", 1)
            parameter("id", id)
            parameter("accesskey", accessKey)
        }.bodyAsText()
        val content = lyricsJson.parseToJsonElement(text).jsonObject.text("content") ?: throw IllegalStateException("Empty lyrics")
        return Base64.Default.decode(content).decodeToString().normalize()
    }

    // Keeps the timed lines and cuts the credits at the start and at the end
    private fun String.normalize(): String {
        val lines = lines().filter { it.matches(acceptedLine) }

        var headCut = 0
        for (i in min(HeadCutLimit, lines.lastIndex) downTo 0) {
            if (lines[i].matches(creditLine)) {
                headCut = i + 1
                break
            }
        }

        var tailCut = 0
        for (i in min(lines.size - HeadCutLimit, lines.lastIndex) downTo 0) {
            if (lines[lines.lastIndex - i].matches(creditLine)) {
                tailCut = i + 1
                break
            }
        }

        return lines.drop(headCut).dropLast(tailCut).joinToString("\n")
    }

    // Title without the parts in brackets
    private fun normalizeTitle(title: String): String =
        listOf("\\(.*\\)", "（.*）", "「.*」", "『.*』", "<.*>", "《.*》", "〈.*〉", "＜.*＞")
            .fold(title) { cleaned, pattern -> cleaned.replace(pattern.toRegex(), "") }

    // Artists joined the way KuGou writes them
    private fun normalizeArtist(artist: String): String =
        artist.replace(", ", "、").replace(" & ", "、").replace(".", "").replace("和", "、")
            .replace("\\(.*\\)".toRegex(), "").replace("（.*）".toRegex(), "")

    // Text of a field, empty when it is missing or null
    private fun JsonObject.text(key: String): String? = this[key]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() }
}

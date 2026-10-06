package com.wavvy.app.core.lyrics

// Ktor networking
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
// JSON parsing
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
// Coroutines
import kotlinx.coroutines.CancellationException
// Java utilities
import java.util.Locale

// Verified lyrics of Jewish music, matched by the YouTube video itself, Zemer points to the public sites that have them, adapted from Metrolist (GPL-3.0)
internal object Zemer {
    private const val ResolveUrl = "https://search.zemer.io/lyrics/resolve"
    private const val LrcLibTrackUrl = "https://lrclib.net/api/get/"
    private const val ZingUrl = "https://jewishmusic.fm:8443/graphql"
    private const val MinimumLines = 4
    private const val SecondsPerMinute = 60
    private const val EndToleranceSeconds = 2
    private const val OrderTolerance = 0.01

    // How good each kind of source usually is, lower first
    private val sourceRank = mapOf(
        "jkaraoke" to 0,
        "lrclib" to 1, "jyrics" to 1, "shironet" to 1, "zingmusic" to 1, "tab4u" to 1, "zemirotdb" to 1,
        "booklet" to 2, "manual" to 2,
        "canonical" to 3, "community" to 3
    )
    private const val UnknownRank = 9

    // Pieces of HTML, the section labels and the credits that are not lyrics
    private val numericEntity = Regex("""&#(\d+);""")
    private val tag = Regex("""<[^>]+>""")
    private val lineBreak = Regex("""<br\s*/?>""", RegexOption.IGNORE_CASE)
    private val spaces = Regex("""[ \t ]+""")
    private val jyricsCut = Regex("""<ul[^>]*class="[^"]*related-list|<h\d[^>]*>\s*Other Songs from""", RegexOption.IGNORE_CASE)
    private val jyricsScript = Regex("""<script[\s\S]*?</script>|<style[\s\S]*?</style>|<!--[\s\S]*?-->""")
    private val jyricsNavigation = Regex("""^(Print|SHARE|Added by|admin)$""", RegexOption.IGNORE_CASE)
    private val sectionLabel = Regex(
        """^\(?\s*(verse|chorus|bridge|intro|outro|pre-?chorus|hook|refrain|interlude|פזמון|בית|גשר|מעבר|סיום|פתיחה)\s*[\d\w]*\s*:?\s*\)?$""",
        RegexOption.IGNORE_CASE
    )
    private val credit = Regex(
        """^\(?\s*(composed|arranged|written|lyrics|words|music|produced|recorded|later recorded|originally|from the album|album)\b[^\n]*\bby\b|^\(?\s*(composed|arranged|recorded)\b|^(מילים|לחן|עיבוד|הפקה)\s*:""",
        RegexOption.IGNORE_CASE
    )
    private val hebrewLetter = Regex("""[א-ת]""")

    // A source Zemer points to
    private class Source(
        val type: String,
        val url: String?,
        val songId: Long?,
        val feedUrl: String?,
        val trackId: Long?,
        val plain: String?,
        val syncedLrc: String?,
        val synced: Boolean
    )

    // The first source with enough lines, timed ones first
    suspend fun getLyrics(videoId: String): Result<String> = runCatching {
        val sources = resolve(videoId)
        for (source in sources.sortedWith(compareBy({ !it.synced }, { sourceRank[it.type] ?: UnknownRank }))) {
            val body = quietly { fetchBody(source) }
            if (body != null && body.lineSequence().count { it.isNotBlank() } >= MinimumLines) return@runCatching body
        }
        throw IllegalStateException("No Zemer source yielded a body")
    }

    // Sources Zemer knows for a video, none when it has nothing
    private suspend fun resolve(videoId: String): List<Source> {
        val body = lyricsHttp.get(ResolveUrl) {
            parameter("videoId", videoId)
            header("Accept", "application/json")
        }.bodyAsText()
        return lyricsJson.parseToJsonElement(body).jsonObject["sources"]?.jsonArray.orEmpty().map { element ->
            val source = element.jsonObject
            Source(
                type = source.text("type").orEmpty(),
                url = source.text("url"),
                songId = source["songId"]?.jsonPrimitive?.longOrNull,
                feedUrl = source.text("feedUrl"),
                trackId = source["trackId"]?.jsonPrimitive?.longOrNull,
                plain = source.text("plain"),
                syncedLrc = source.text("syncedLrc"),
                synced = source["synced"]?.jsonPrimitive?.booleanOrNull == true
            )
        }
    }

    // Text of a source, read the way its site writes it
    private suspend fun fetchBody(source: Source): String? = when (source.type) {
        "jkaraoke" -> source.feedUrl?.let { fetchText(it) }?.let { page -> source.songId?.let { jkaraokeLrc(page, it) } }
        "lrclib" -> source.trackId?.let { fetchText("$LrcLibTrackUrl$it") }?.let(::lrclibBody)
        "jyrics" -> source.url?.let { fetchText(it) }?.let(::parseJyrics)
        "shironet" -> source.url?.let { fetchText(it) }?.let(::parseShironet)
        "zingmusic" -> source.trackId?.let { zingLyricsHtml(it) }?.let(::zingToPlain)
        "tab4u" -> source.url?.let { fetchText(it) }?.let(::parseTab4u)
        "zemirotdb" -> source.url?.let { fetchText(it) }?.let(::parseZemirotDb)
        "booklet", "manual", "canonical", "community" -> source.syncedLrc ?: source.plain
        else -> null
    }

    // Page of a source as text
    private suspend fun fetchText(url: String): String =
        lyricsHttp.get(url) { header("Accept", "text/html,application/json") }.bodyAsText()

    // Karaoke feed with the start of each line, kept only when the lines are in order and inside the song
    private fun jkaraokeLrc(page: String, songId: Long): String? {
        val song = lyricsJson.parseToJsonElement(page).jsonObject["data"]?.jsonArray
            ?.map { it.jsonObject }?.firstOrNull { it["id"]?.jsonPrimitive?.longOrNull == songId } ?: return null
        val lines = song["lyrics"]?.jsonArray?.map { it.jsonObject }.orEmpty().mapNotNull { line ->
            val start = line["start"]?.jsonPrimitive?.doubleOrNull ?: return@mapNotNull null
            val text = line.text("text") ?: return@mapNotNull null
            start to text
        }
        if (lines.size < MinimumLines) return null
        if ((1 until lines.size).any { lines[it].first + OrderTolerance < lines[it - 1].first }) return null
        val duration = song["duration"]?.jsonPrimitive?.intOrNull
        if (lines.first().first < 0 || (duration != null && duration > 0 && lines.last().first > duration + EndToleranceSeconds)) return null
        return lines.joinToString("\n") { (start, text) -> "[${lrcTime(start)}]${text.trim()}" }
    }

    // Seconds as an LRC timestamp
    private fun lrcTime(seconds: Double): String {
        val minutes = (seconds / SecondsPerMinute).toInt()
        return String.format(Locale.US, "%02d:%05.2f", minutes, seconds - minutes * SecondsPerMinute)
    }

    // Timed or plain lyrics of a song of LrcLib, none for instrumentals
    private fun lrclibBody(body: String): String? {
        val track = lyricsJson.parseToJsonElement(body).jsonObject
        if (track["instrumental"]?.jsonPrimitive?.booleanOrNull == true) return null
        return track.text("syncedLyrics") ?: track.text("plainLyrics")
    }

    // Hebrew or English lyrics of a track of jewishmusic.fm
    private suspend fun zingLyricsHtml(trackId: Long): String? {
        val body = lyricsHttp.post(ZingUrl) {
            header("Content-Type", "application/json")
            setBody("""{"query":"{ track(where:{id:$trackId}){ heLyrics enLyrics } }"}""")
        }.bodyAsText()
        val track = lyricsJson.parseToJsonElement(body).jsonObject["data"]?.jsonObject?.get("track")?.jsonObject ?: return null
        return track.text("heLyrics") ?: track.text("enLyrics")
    }

    // HTML entities written back as characters
    private fun unescape(text: String): String = text
        .replace("&#8217;", "'").replace("&rsquo;", "'")
        .replace("&#8211;", "–").replace("&ndash;", "–")
        .replace("&amp;", "&").replace("&quot;", "\"").replace("&#039;", "'").replace("&nbsp;", " ")
        .replace(numericEntity) { match -> match.groupValues[1].toInt().toChar().toString() }

    // Lines without extra spaces and without empty lines at the ends or repeated
    private fun tidy(lines: List<String>): String {
        val out = ArrayList<String>()
        for (raw in lines) {
            val line = raw.replace(spaces, " ").trim()
            if (line.isNotEmpty() || (out.isNotEmpty() && out.last().isNotEmpty())) out.add(line)
        }
        while (out.isNotEmpty() && out.last().isEmpty()) out.removeAt(out.lastIndex)
        while (out.isNotEmpty() && out.first().isEmpty()) out.removeAt(0)
        return out.joinToString("\n")
    }

    // Lyrics of a page of jyrics.com
    private fun parseJyrics(html: String): String? {
        var article = Regex("""<article[\s\S]*?</article>""").find(html)?.value ?: html
        jyricsCut.find(article)?.let { article = article.substring(0, it.range.first) }
        val text = unescape(
            article.replace(jyricsScript, "").replace(lineBreak, "\n")
                .replace(Regex("""</p>\s*""", RegexOption.IGNORE_CASE), "\n\n")
                .replace(Regex("""</(div|h\d|li)>\s*""", RegexOption.IGNORE_CASE), "\n")
                .replace(tag, "")
        )
        val lines = text.split("\n").map { it.replace(spaces, " ").trim() }
        val start = lines.indexOfFirst { it.equals("LYRIC", ignoreCase = true) }
        val body = lines.subList(if (start >= 0) start + 1 else 0, lines.size).filterNot { jyricsNavigation.matches(it) }.toMutableList()
        while (body.isNotEmpty() && body.first().isEmpty()) body.removeAt(0)
        if (body.isNotEmpty()) body.removeAt(0)
        return tidy(body.filterNot { sectionLabel.matches(it) || credit.containsMatchIn(it) }).ifBlank { null }
    }

    // Lyrics of a page of shironet
    private fun parseShironet(html: String): String? {
        val span = Regex("""<span[^>]*class="artist_lyrics_text"[^>]*>([\s\S]*?)</span>""").find(html)?.groupValues?.get(1) ?: return null
        val text = unescape(span.replace(lineBreak, "\n").replace(tag, ""))
        return tidy(text.split("\n").filterNot { sectionLabel.matches(it.trim()) }).ifBlank { null }
    }

    // Lyrics of jewishmusic.fm without the chord and section lines
    private fun zingToPlain(html: String): String? {
        val text = unescape(
            html.replace(lineBreak, "\n")
                .replace(Regex("""</(p|div|pre|li|h\d)>""", RegexOption.IGNORE_CASE), "\n")
                .replace(tag, "").replace("&lt;", "<").replace("&gt;", ">")
        )
        val chordLine = Regex("""^\[[^]]*]$""")
        val sectionLine = Regex("""^\((?:verse|chorus|bridge|intro|outro|פזמון|בית|מעבר|גשר)\b[^)]*\)$""", RegexOption.IGNORE_CASE)
        return tidy(text.split("\n").filterNot { chordLine.matches(it.trim()) || sectionLine.matches(it.trim()) }).ifBlank { null }
    }

    // Hebrew lines of a page of tab4u
    private fun parseTab4u(html: String): String? {
        val lines = Regex("""<td class="song[^"]*"[^>]*>([\s\S]*?)</td>""").findAll(html).mapNotNull { match ->
            unescape(match.groupValues[1].replace(tag, "")).replace(spaces, " ").trim().takeIf { it.isNotEmpty() && hebrewLetter.containsMatchIn(it) }
        }.toList()
        return tidy(lines).ifBlank { null }
    }

    // Hebrew lyrics of a page of zemirotdb
    private fun parseZemirotDb(html: String): String? {
        val block = Regex("""<div id=['"]hebrew['"][^>]*>([\s\S]*?)</div>""").find(html)?.groupValues?.get(1) ?: return null
        val text = unescape(block.replace(lineBreak, "\n").replace(Regex("""</p>\s*""", RegexOption.IGNORE_CASE), "\n\n").replace(tag, ""))
        return tidy(text.split("\n")).ifBlank { null }
    }

    // Runs a request, keeping a cancellation and turning any other failure into nothing
    private suspend fun <T> quietly(block: suspend () -> T?): T? =
        try {
            block()
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            null
        }

    // Text of a field, empty when it is missing, null or blank
    private fun JsonObject.text(key: String): String? = this[key]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() }
}

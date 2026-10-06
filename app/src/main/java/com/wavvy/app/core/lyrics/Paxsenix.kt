package com.wavvy.app.core.lyrics

// Ktor networking
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
// JSON parsing
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
// Coroutines
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
// Math
import kotlin.math.abs

// Lyrics of Apple Music through the Paxsenix API, the song is found in the catalog of Apple Music first, adapted from Metrolist (GPL-3.0)
internal object Paxsenix {
    private const val LyricsUrl = "https://lyrics.paxsenix.org/apple-music/lyrics"
    private const val AppleSearchUrl = "https://amp-api.music.apple.com/v1/catalog/us/search"
    private const val AppleWebUrl = "https://beta.music.apple.com"
    private const val AppleOrigin = "https://music.apple.com"
    private const val BrowserAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:95.0) Gecko/20100101 Firefox/95.0"
    private const val HttpUnauthorized = 401
    private const val SyllableTiming = "Syllable"

    // Songs tried per search, and points of the match by length, title and artist
    private const val Candidates = 10
    private const val MillisPerSecond = 1000
    private const val CloseGapMs = 2000
    private const val NearGapMs = 5000
    private const val FarGapMs = 10000
    private const val CloseScore = 100.0
    private const val NearScore = 50.0
    private const val FarScore = 10.0
    private const val WrongLengthScore = -50.0
    private const val SameTitleScore = 80.0
    private const val SimilarTitleScore = 40.0
    private const val WrongMixScore = -60.0
    private const val WrongRemixScore = -40.0
    private const val SameArtistScore = 50.0
    private const val SimilarArtistScore = 25.0
    private const val ShortWordLength = 2

    // Parts of a title that are not part of the name of the song, and what separates the first artist from the others
    private val titleCleanupPatterns = listOf(
        Regex("""\s*\(.*?(official|video|audio|lyrics|lyric|visualizer|hd|hq|4k|remaster|remix|live|acoustic|version|edit|extended|radio|clean|explicit).*?\)""", RegexOption.IGNORE_CASE),
        Regex("""\s*\[.*?(official|video|audio|lyrics|lyric|visualizer|hd|hq|4k|remaster|remix|live|acoustic|version|edit|extended|radio|clean|explicit).*?]""", RegexOption.IGNORE_CASE),
        Regex("""\s*【.*?】"""),
        Regex("""\s*\|.*$"""),
        Regex("""\s*-\s*(official|video|audio|lyrics|lyric|visualizer).*$""", RegexOption.IGNORE_CASE),
        Regex("""\s*\(feat\..*?\)""", RegexOption.IGNORE_CASE),
        Regex("""\s*\(ft\..*?\)""", RegexOption.IGNORE_CASE),
        Regex("""\s*feat\..*$""", RegexOption.IGNORE_CASE),
        Regex("""\s*ft\..*$""", RegexOption.IGNORE_CASE),
        Regex("""\s*\([^)]*\d{4}[^)]*\)""", RegexOption.IGNORE_CASE)
    )
    private val artistSeparators = listOf(" & ", " and ", ", ", " x ", " X ", " feat. ", " feat ", " ft. ", " ft ", " featuring ", " with ")
    private val bracketPattern = Regex("""\s*\(.*?\)|\s*\[.*?]""")
    private val indexScriptPattern = Regex("""/assets/index~[^/]+\.js""")
    private val tokenPattern = Regex("""eyJ[A-Za-z0-9\-_=]+\.[A-Za-z0-9\-_=]+\.[A-Za-z0-9\-_=]+""")

    // Song of the catalog of Apple Music
    private class AppleSong(val id: String, val title: String, val artist: String, val durationMs: Long?)

    // Public token of the Apple Music web player, read from its script and kept until it stops working
    private val tokenLock = Mutex()
    private var token: String? = null

    // Best lyrics among the matching songs, word timed ones end the search
    suspend fun getLyrics(title: String, artist: String, durationSeconds: Int): Result<SongLyrics> = runCatching {
        val cleanedTitle = cleanTitle(title)
        val cleanedArtist = cleanArtist(artist)

        val candidates = listOf("$cleanedTitle $cleanedArtist", cleanedTitle).firstNotNullOfOrNull { query ->
            score(search(query), title, artist, durationSeconds).takeIf { it.isNotEmpty() }
        } ?: throw IllegalStateException("No tracks found on Paxsenix")

        var best: SongLyrics? = null
        var bestQuality = 0
        for (song in candidates) {
            val lyrics = runCatchingKeepCancel { fetchLyrics(song.id) } ?: continue
            val quality = when {
                lyrics.lines.any { it.words.isNotEmpty() } -> 3
                lyrics.isSynced -> 2
                else -> 1
            }
            if (quality > bestQuality) {
                bestQuality = quality
                best = lyrics.named(song.title, song.artist)
            }
            if (bestQuality == 3) break
        }
        best ?: throw IllegalStateException("No lyrics available from Paxsenix")
    }

    // Lyrics of a song of Apple Music, from the TTML, the extended LRC, the plain text or the timed syllables
    private suspend fun fetchLyrics(id: String): SongLyrics? {
        val answer = lyricsJson.parseToJsonElement(lyricsHttp.get(LyricsUrl) { parameter("id", id) }.bodyAsText()).jsonObject

        answer.text("ttmlContent")?.let { ttml ->
            runCatching { BetterLyrics.parse(ttml) }.getOrNull()?.takeIf { it.isNotEmpty() }?.let { return SongLyrics(it.sortedBy { line -> line.timeMs }, isSynced = true) }
        }
        (answer.text("elrcMultiPerson") ?: answer.text("elrc") ?: answer.text("plain"))?.let { return LyricsRepository.parseText(it) }

        val content = answer["content"]?.jsonArray?.map { it.jsonObject }.orEmpty()
        if (content.isEmpty()) return null

        if (answer.text("type") != SyllableTiming) {
            val plain = content.mapNotNull { line -> line.words().joinToString(" ") { it.text }.takeIf { it.isNotBlank() } }
            return SongLyrics(plain.mapIndexed { index, text -> LyricLine(index.toLong(), text) }, isSynced = false)
        }

        val lines = content.mapNotNull { line ->
            val words = line.words()
            val text = words.joinToString(" ") { it.text }
            text.takeIf { it.isNotBlank() }?.let { LyricLine(line["timestamp"]?.jsonPrimitive?.longOrNull ?: 0L, it, words) }
        }
        return SongLyrics(lines.sortedBy { it.timeMs }, isSynced = true)
    }

    // Timed words of a line of the content array
    private fun JsonObject.words(): List<LyricWord> =
        this["text"]?.jsonArray?.mapNotNull { element ->
            val word = element.jsonObject
            val text = word.text("text")?.trim()?.takeIf { it.isNotEmpty() } ?: return@mapNotNull null
            LyricWord(text, word["timestamp"]?.jsonPrimitive?.longOrNull ?: 0L, word["endtime"]?.jsonPrimitive?.longOrNull ?: 0L)
        }.orEmpty()

    // Songs of the catalog of Apple Music, asking for a new token once when the old one is refused
    private suspend fun search(query: String): List<AppleSong> =
        try {
            searchWith(appleToken(), query)
        } catch (error: CancellationException) {
            throw error
        } catch (error: ClientRequestException) {
            if (error.response.status.value != HttpUnauthorized) return emptyList()
            tokenLock.withLock { token = null }
            runCatchingKeepCancel { searchWith(appleToken(), query) }.orEmpty()
        } catch (error: Exception) {
            emptyList()
        }

    // One search on the catalog, songs with their details
    private suspend fun searchWith(token: String, query: String): List<AppleSong> {
        val body = lyricsHttp.get(AppleSearchUrl) {
            parameter("term", query)
            parameter("types", "songs")
            parameter("limit", 25)
            parameter("l", "en-US")
            parameter("platform", "web")
            parameter("format[resources]", "map")
            parameter("include[songs]", "artists")
            header("Authorization", "Bearer $token")
            header("Origin", AppleOrigin)
            header("Referer", "$AppleOrigin/")
            header("User-Agent", BrowserAgent)
            header("Accept", "application/json")
        }.bodyAsText()

        val answer = lyricsJson.parseToJsonElement(body).jsonObject
        val details = answer["resources"]?.jsonObject?.get("songs")?.jsonObject
        return answer["results"]?.jsonObject?.get("songs")?.jsonObject?.get("data")?.jsonArray.orEmpty().mapNotNull { element ->
            val id = element.jsonObject.text("id") ?: return@mapNotNull null
            val attributes = details?.get(id)?.jsonObject?.get("attributes")?.jsonObject ?: return@mapNotNull null
            AppleSong(
                id = id,
                title = attributes.text("name").orEmpty(),
                artist = attributes.text("artistName").orEmpty(),
                durationMs = attributes["durationInMillis"]?.jsonPrimitive?.longOrNull
            )
        }
    }

    // Token of the web player, found in the main script of its page
    private suspend fun appleToken(): String = tokenLock.withLock {
        token?.let { return it }
        val page = lyricsHttp.get(AppleWebUrl).bodyAsText()
        val script = indexScriptPattern.find(page)?.value ?: throw IllegalStateException("Could not find the Apple Music script")
        val found = tokenPattern.find(lyricsHttp.get("$AppleWebUrl$script").bodyAsText())?.value
            ?: throw IllegalStateException("Could not find the Apple Music token")
        token = found
        found
    }

    // Songs ranked by how close their length, title and artist are, without the ones that clearly do not match
    private fun score(songs: List<AppleSong>, title: String, artist: String, durationSeconds: Int): List<AppleSong> {
        val durationMs = durationSeconds.toLong() * MillisPerSecond
        val targetTitle = title.replace(bracketPattern, "").lowercase().trim()
        val targetArtist = cleanArtist(artist).lowercase()
        val wantsMix = title.contains("mixed", ignoreCase = true)
        val wantsRemix = title.contains("remix", ignoreCase = true)

        return songs.map { song ->
            var points = 0.0
            song.durationMs?.let { length ->
                val gap = abs(length - durationMs)
                points += when {
                    gap <= CloseGapMs -> CloseScore
                    gap <= NearGapMs -> NearScore
                    gap <= FarGapMs -> FarScore
                    else -> WrongLengthScore
                }
            }

            val songTitle = song.title.replace(bracketPattern, "").lowercase().trim()
            points += when {
                songTitle == targetTitle -> SameTitleScore
                songTitle.contains(targetTitle) || targetTitle.contains(songTitle) -> SimilarTitleScore
                else -> 0.0
            }
            if (song.title.contains("mixed", ignoreCase = true) && !wantsMix) points += WrongMixScore
            if (song.title.contains("remix", ignoreCase = true) && !wantsRemix) points += WrongRemixScore

            val songArtist = song.artist.lowercase()
            points += when {
                songArtist.contains(targetArtist) -> SameArtistScore
                targetArtist.split(Regex("\\s+")).filter { it.length > ShortWordLength }.any { songArtist.contains(it) } -> SimilarArtistScore
                else -> 0.0
            }
            song to points
        }.sortedByDescending { it.second }.filter { it.second > 0 }.take(Candidates).map { it.first }
    }

    // Title without the extra words of videos and featured artists
    private fun cleanTitle(title: String): String =
        titleCleanupPatterns.fold(title.trim()) { cleaned, pattern -> cleaned.replace(pattern, "") }.trim()

    // First artist of the list
    private fun cleanArtist(artist: String): String {
        val cleaned = artist.trim()
        val separator = artistSeparators.firstOrNull { cleaned.contains(it, ignoreCase = true) } ?: return cleaned
        return cleaned.split(separator, ignoreCase = true, limit = 2)[0].trim()
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

    // Text of a field, empty when it is missing, null or blank
    private fun JsonObject.text(key: String): String? = this[key]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() }
}

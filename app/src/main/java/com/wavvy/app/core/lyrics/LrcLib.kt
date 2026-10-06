package com.wavvy.app.core.lyrics

// Ktor networking
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
// JSON parsing
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
// Math
import kotlin.math.abs

// Song found on LrcLib, with the timed and the plain lyrics it has
private class LrcLibTrack(
    val trackName: String,
    val artistName: String,
    val duration: Double,
    val plainLyrics: String?,
    val syncedLyrics: String?
)

// Open lyrics library lrclib.net, adapted from Metrolist (GPL-3.0)
internal object LrcLib {
    private const val SearchUrl = "https://lrclib.net/api/search"

    // Seconds a song on LrcLib may differ from the playing one, and how alike the names must be when the length is unknown
    private const val DurationTolerance = 5
    private const val MinimumSimilarity = 0.6
    private const val ContainsSimilarity = 0.8
    private const val SyncedBonus = 0.1

    // Parts of a title that are not part of the name of the song
    private val titleCleanupPatterns = listOf(
        Regex("""\s*\(.*?(official|video|audio|lyrics|lyric|visualizer|hd|hq|4k|remaster|remix|live|acoustic|version|edit|extended|radio|clean|explicit).*?\)""", RegexOption.IGNORE_CASE),
        Regex("""\s*\[.*?(official|video|audio|lyrics|lyric|visualizer|hd|hq|4k|remaster|remix|live|acoustic|version|edit|extended|radio|clean|explicit).*?]""", RegexOption.IGNORE_CASE),
        Regex("""\s*【.*?】"""),
        Regex("""\s*\|.*$"""),
        Regex("""\s*-\s*(official|video|audio|lyrics|lyric|visualizer).*$""", RegexOption.IGNORE_CASE),
        Regex("""\s*\(feat\..*?\)""", RegexOption.IGNORE_CASE),
        Regex("""\s*\(ft\..*?\)""", RegexOption.IGNORE_CASE),
        Regex("""\s*feat\..*$""", RegexOption.IGNORE_CASE),
        Regex("""\s*ft\..*$""", RegexOption.IGNORE_CASE)
    )

    // What separates the first artist from the others
    private val artistSeparators = listOf(" & ", " and ", ", ", " x ", " X ", " feat. ", " feat ", " ft. ", " ft ", " featuring ", " with ")

    // Timed lyrics when there are, plain ones otherwise, of the song that best matches the length
    suspend fun getLyrics(title: String, artist: String, durationSeconds: Int): Result<FoundText> = runCatching {
        val tracks = search(title, artist)
        val match = if (durationSeconds <= 0) {
            bestByName(tracks, cleanTitle(title), cleanArtist(artist))
        } else {
            bestByDuration(tracks, durationSeconds)
        }
        match?.let { track -> (track.syncedLyrics ?: track.plainLyrics)?.let { FoundText(it, track.trackName, track.artistName) } }
            ?: throw IllegalStateException("Lyrics unavailable")
    }

    // Tries from the most precise search to the loosest one until one finds lyrics
    private suspend fun search(title: String, artist: String): List<LrcLibTrack> {
        val cleanedTitle = cleanTitle(title)
        val cleanedArtist = cleanArtist(artist)
        val attempts = listOfNotNull(
            mapOf("track_name" to cleanedTitle, "artist_name" to cleanedArtist),
            mapOf("track_name" to cleanedTitle),
            mapOf("q" to "$cleanedArtist $cleanedTitle"),
            mapOf("q" to cleanedTitle),
            mapOf("track_name" to title.trim(), "artist_name" to artist.trim()).takeIf { cleanedTitle != title.trim() }
        )

        for (parameters in attempts) {
            val found = query(parameters).filter { it.syncedLyrics != null || it.plainLyrics != null }
            if (found.isNotEmpty()) return found
        }
        return emptyList()
    }

    // One search on LrcLib, empty when it fails
    private suspend fun query(parameters: Map<String, String>): List<LrcLibTrack> = runCatching {
        val text = lyricsHttp.get(SearchUrl) { parameters.forEach { (key, value) -> parameter(key, value) } }.bodyAsText()
        lyricsJson.parseToJsonElement(text).jsonArray.map { element ->
            val track = element.jsonObject
            LrcLibTrack(
                trackName = track.text("trackName").orEmpty(),
                artistName = track.text("artistName").orEmpty(),
                duration = track["duration"]?.jsonPrimitive?.doubleOrNull ?: 0.0,
                plainLyrics = track.text("plainLyrics"),
                syncedLyrics = track.text("syncedLyrics")
            )
        }
    }.getOrDefault(emptyList())

    // Song closest in length, timed lyrics first, within the tolerance
    private fun bestByDuration(tracks: List<LrcLibTrack>, durationSeconds: Int): LrcLibTrack? {
        fun gap(track: LrcLibTrack) = abs(track.duration.toInt() - durationSeconds)
        return tracks.filter { it.syncedLyrics != null }.minByOrNull(::gap)?.takeIf { gap(it) <= DurationTolerance }
            ?: tracks.minByOrNull(::gap)?.takeIf { gap(it) <= DurationTolerance }
    }

    // Song with the most alike title and artist, when the length is unknown
    private fun bestByName(tracks: List<LrcLibTrack>, title: String, artist: String): LrcLibTrack? {
        fun score(track: LrcLibTrack) =
            (similarity(title.lowercase(), track.trackName.lowercase()) + similarity(artist.lowercase(), track.artistName.lowercase())) / 2.0
        return tracks.maxByOrNull { score(it) + if (it.syncedLyrics != null) SyncedBonus else 0.0 }
            ?.takeIf { score(it) > MinimumSimilarity }
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

    // How alike two names are, from zero to one
    private fun similarity(first: String, second: String): Double {
        val a = first.trim()
        val b = second.trim()
        if (a == b) return 1.0
        if (a.isEmpty() || b.isEmpty()) return 0.0
        val contains = if (a.contains(b) || b.contains(a)) ContainsSimilarity else 0.0
        return maxOf(contains, 1.0 - levenshtein(a, b).toDouble() / maxOf(a.length, b.length))
    }

    // Edits needed to turn one text into the other
    private fun levenshtein(first: String, second: String): Int {
        val matrix = Array(first.length + 1) { IntArray(second.length + 1) }
        for (i in 0..first.length) matrix[i][0] = i
        for (j in 0..second.length) matrix[0][j] = j
        for (i in 1..first.length) {
            for (j in 1..second.length) {
                val cost = if (first[i - 1] == second[j - 1]) 0 else 1
                matrix[i][j] = minOf(matrix[i - 1][j] + 1, matrix[i][j - 1] + 1, matrix[i - 1][j - 1] + cost)
            }
        }
        return matrix[first.length][second.length]
    }

    // Text of a field, empty when it is missing or null
    private fun JsonObject.text(key: String): String? = this[key]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() }
}

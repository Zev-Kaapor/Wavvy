package com.wavvy.app.features.player.data.extractor

// JSON parsing
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
// Network client
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

// Search result before matching against a chart entry
data class InnerTubeSearchResult(
    val videoId: String,
    val title: String,
    val artist: String?
)

// Resolves a text query into a videoId using the YT Music search endpoint
object InnerTubeSearchClient {

    private val gson = Gson()
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    // Songs-only search filter param, same one used elsewhere in the app
    private const val SONGS_FILTER_PARAM = "EgWKAQIIAWoQEAMQBBAJEAoQCRAKEAYQAQ=="

    private fun buildSearchPayload(query: String): String {
        val root = JsonObject()

        val context = JsonObject()
        val clientObj = JsonObject()
        clientObj.addProperty("clientName", "WEB_REMIX")
        clientObj.addProperty("clientVersion", "1.20240501.01.00")
        clientObj.addProperty("hl", "pt-BR")
        clientObj.addProperty("gl", "BR")
        context.add("client", clientObj)
        root.add("context", context)

        root.addProperty("query", query)
        root.addProperty("params", SONGS_FILTER_PARAM)

        return gson.toJson(root)
    }

    // Search and return the raw ordered list of song results
    fun search(query: String): List<InnerTubeSearchResult> {
        try {
            val url = "https://music.youtube.com/youtubei/v1/search?prettyPrint=false"
            val jsonBody = buildSearchPayload(query)
            val mediaType = "application/json; charset=utf-8".toMediaType()
            val body = jsonBody.toRequestBody(mediaType)

            val request = Request.Builder()
                .url(url)
                .post(body)
                .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .addHeader("Content-Type", "application/json")
                .addHeader("X-Origin", "https://music.youtube.com")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return emptyList()
                val responseBody = response.body.string()
                val rootObj = JsonParser.parseString(responseBody).asJsonObject
                return parseSearchResults(rootObj)
            }
        } catch (_: Exception) {
            return emptyList()
        }
    }

    // Resolve a chart entry into a videoId, preferring an artist name match
    fun resolveVideoId(title: String, artist: String?): String? {
        val query = if (artist.isNullOrBlank()) title else "$artist $title"
        val results = search(query)
        if (results.isEmpty()) return null

        if (!artist.isNullOrBlank()) {
            val normalizedArtist = artist.lowercase().trim()
            val matched = results.firstOrNull { result ->
                result.artist?.lowercase()?.contains(normalizedArtist) == true ||
                        normalizedArtist.contains(result.artist?.lowercase().orEmpty())
            }
            if (matched != null) return matched.videoId
        }

        return results.first().videoId
    }

    private fun parseSearchResults(rootObj: JsonObject): List<InnerTubeSearchResult> {
        val results = mutableListOf<InnerTubeSearchResult>()

        val contents = rootObj
            .getAsJsonObject("contents")
            ?.getAsJsonObject("tabbedSearchResultsRenderer")
            ?.getAsJsonArray("tabs")
            ?.get(0)?.asJsonObject
            ?.getAsJsonObject("tabRenderer")
            ?.getAsJsonObject("content")
            ?.getAsJsonObject("sectionListRenderer")
            ?.getAsJsonArray("contents") ?: return emptyList()

        for (i in 0 until contents.size()) {
            val section = contents.get(i).asJsonObject
            val musicShelf = section.getAsJsonObject("musicShelfRenderer") ?: continue
            val items = musicShelf.getAsJsonArray("contents") ?: continue

            for (j in 0 until items.size()) {
                val itemObj = items.get(j).asJsonObject
                val responsiveItem = itemObj.getAsJsonObject("musicResponsiveListItemRenderer") ?: continue
                parseResponsiveItem(responsiveItem)?.let { results.add(it) }
            }
        }

        return results
    }

    private fun parseResponsiveItem(item: JsonObject): InnerTubeSearchResult? {
        val videoId = item.getAsJsonObject("playlistItemData")
            ?.get("videoId")?.asString
            ?: item.getAsJsonArray("flexColumns")
                ?.get(0)?.asJsonObject
                ?.getAsJsonObject("musicResponsiveListItemFlexColumnRenderer")
                ?.getAsJsonObject("text")
                ?.getAsJsonArray("runs")
                ?.get(0)?.asJsonObject
                ?.getAsJsonObject("navigationEndpoint")
                ?.getAsJsonObject("watchEndpoint")
                ?.get("videoId")?.asString

        if (videoId.isNullOrBlank()) return null

        val flexColumns = item.getAsJsonArray("flexColumns") ?: return null
        if (flexColumns.size() < 1) return null

        val titleRuns = flexColumns.get(0).asJsonObject
            .getAsJsonObject("musicResponsiveListItemFlexColumnRenderer")
            ?.getAsJsonObject("text")
            ?.getAsJsonArray("runs")
        val title = titleRuns?.get(0)?.asJsonObject?.get("text")?.asString ?: return null

        var artist: String? = null
        if (flexColumns.size() >= 2) {
            val subtitleRuns = flexColumns.get(1).asJsonObject
                .getAsJsonObject("musicResponsiveListItemFlexColumnRenderer")
                ?.getAsJsonObject("text")
                ?.getAsJsonArray("runs")

            artist = subtitleRuns?.get(0)?.asJsonObject?.get("text")?.asString
        }

        return InnerTubeSearchResult(
            videoId = videoId,
            title = title,
            artist = artist
        )
    }
}

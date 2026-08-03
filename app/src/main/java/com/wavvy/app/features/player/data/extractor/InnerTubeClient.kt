package com.wavvy.app.features.player.data.extractor

// JSON parsing
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
// Network client
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
// Domain models
import com.wavvy.app.features.player.ui.components.QueueSong
// Utilities
import java.util.concurrent.TimeUnit

// Helper data models
private data class Run(
    val text: String,
    val browseId: String? = null
)

// Extension functions
private fun List<Run>.splitBySeparator(): List<List<Run>> {
    val res = mutableListOf<List<Run>>()
    var tmp = mutableListOf<Run>()
    forEach { run ->
        if (run.text.trim() == "•") {
            res.add(tmp)
            tmp = mutableListOf()
        } else {
            tmp.add(run)
        }
    }
    res.add(tmp)
    return res
}

private fun List<Run>.oddElements() = filterIndexed { index, _ -> index % 2 == 0 }

// YouTube InnerTube API client
object InnerTubeClient {

    private val gson = Gson()
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    private var cachedVisitorData: String? = null

    fun getOrFetchVisitorData(authCookie: String? = null): String? {
        if (!cachedVisitorData.isNullOrBlank()) return cachedVisitorData
        try {
            val url = "https://music.youtube.com/youtubei/v1/browse?prettyPrint=false"
            val root = JsonObject()
            val context = JsonObject()
            val clientObj = JsonObject()
            clientObj.addProperty("clientName", "WEB_REMIX")
            clientObj.addProperty("clientVersion", "1.20240501.01.00")
            clientObj.addProperty("hl", "pt-BR")
            clientObj.addProperty("gl", "BR")
            context.add("client", clientObj)
            root.add("context", context)
            root.addProperty("browseId", "FEmusic_home")

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val body = gson.toJson(root).toRequestBody(mediaType)

            val requestBuilder = Request.Builder()
                .url(url)
                .post(body)
                .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36 Music")
                .addHeader("Content-Type", "application/json")

            if (!authCookie.isNullOrBlank()) {
                requestBuilder.addHeader("Cookie", authCookie)
            }

            client.newCall(requestBuilder.build()).execute().use { response ->
                if (!response.isSuccessful) return null
                val responseBody = response.body.string()
                val rootObj = JsonParser.parseString(responseBody).asJsonObject
                val visitorData = rootObj.getAsJsonObject("responseContext")
                    ?.get("visitorData")?.asString
                if (!visitorData.isNullOrBlank()) {
                    cachedVisitorData = visitorData
                }
                return visitorData
            }
        } catch (_: Exception) {
            return null
        }
    }

    fun fetchStreamUrl(videoId: String, authCookie: String? = null): String? {
        try {
            val url = "https://www.youtube.com/youtubei/v1/player?prettyPrint=false"
            val visitorData = getOrFetchVisitorData(authCookie)

            val root = JsonObject()
            val context = JsonObject()
            val clientObj = JsonObject()
            clientObj.addProperty("clientName", "ANDROID_VR")
            clientObj.addProperty("clientVersion", "1.50.31")
            clientObj.addProperty("deviceMake", "Oculus")
            clientObj.addProperty("deviceModel", "Quest 2")
            clientObj.addProperty("osName", "Android")
            clientObj.addProperty("osVersion", "12")
            clientObj.addProperty("androidSdkVersion", 32)
            clientObj.addProperty("hl", "pt-BR")
            clientObj.addProperty("gl", "BR")
            if (!visitorData.isNullOrBlank()) {
                clientObj.addProperty("visitorData", visitorData)
            }

            context.add("client", clientObj)
            root.add("context", context)
            root.addProperty("videoId", videoId)

            val jsonBody = gson.toJson(root)
            val mediaType = "application/json; charset=utf-8".toMediaType()
            val body = jsonBody.toRequestBody(mediaType)

            val requestBuilder = Request.Builder()
                .url(url)
                .post(body)
                .addHeader("User-Agent", "com.google.android.apps.youtube.vr/1.50.31 (Linux; U; Android 12; en_US; Oculus Quest 2)")
                .addHeader("Content-Type", "application/json")
                .addHeader("X-Goog-Api-Format-Version", "2")

            if (!visitorData.isNullOrBlank()) {
                requestBuilder.addHeader("X-Goog-Visitor-Id", visitorData)
            }
            if (!authCookie.isNullOrBlank()) {
                requestBuilder.addHeader("Cookie", authCookie)
            }

            client.newCall(requestBuilder.build()).execute().use { response ->
                if (!response.isSuccessful) {
                    android.util.Log.e("InnerTubeClient", "fetchStreamUrl HTTP error ${response.code} for $videoId")
                    return null
                }
                val responseBody = response.body.string()
                val rootObj = JsonParser.parseString(responseBody).asJsonObject

                // Log playability status for diagnostics
                val playabilityStatus = rootObj.getAsJsonObject("playabilityStatus")
                val status = playabilityStatus?.get("status")?.asString ?: "UNKNOWN"
                val reason = playabilityStatus?.get("reason")?.asString ?: ""
                android.util.Log.d("InnerTubeClient", "fetchStreamUrl videoId=$videoId status=$status reason=$reason")

                if (status != "OK") {
                    android.util.Log.e("InnerTubeClient", "fetchStreamUrl: non-OK status '$status' for $videoId: $reason")
                    return null
                }

                val streamingData = rootObj.getAsJsonObject("streamingData") ?: run {
                    android.util.Log.e("InnerTubeClient", "fetchStreamUrl: no streamingData for $videoId")
                    return null
                }
                val adaptiveFormats = streamingData.getAsJsonArray("adaptiveFormats") ?: JsonArray()

                var bestUrl: String? = null
                var highestBitrate = -1

                for (i in 0 until adaptiveFormats.size()) {
                    val formatObj = adaptiveFormats.get(i).asJsonObject
                    val mimeType = formatObj.get("mimeType")?.asString ?: ""
                    if (mimeType.contains("audio")) {
                        val streamUrl = formatObj.get("url")?.asString
                        val bitrate = formatObj.get("bitrate")?.asInt ?: 0
                        if (!streamUrl.isNullOrEmpty() && bitrate > highestBitrate) {
                            highestBitrate = bitrate
                            bestUrl = streamUrl
                        }
                    }
                }

                if (bestUrl != null) {
                    android.util.Log.d("InnerTubeClient", "fetchStreamUrl: resolved direct stream bitrate=$highestBitrate for $videoId")
                    return bestUrl
                } else {
                    android.util.Log.w("InnerTubeClient", "fetchStreamUrl: no direct audio URL found via InnerTube for $videoId, falling back to NewPipeExtractor")
                    return NewPipeHelper.extractStreamUrl(videoId)
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("InnerTubeClient", "fetchStreamUrl InnerTube exception for $videoId: ${e.message}, attempting NewPipeExtractor fallback")
            return NewPipeHelper.extractStreamUrl(videoId)
        }
    }

    private fun buildNextPayload(
        videoId: String? = null,
        playlistId: String? = null,
        continuation: String? = null
    ): String {
        val root = JsonObject()

        val context = JsonObject()
        val clientObj = JsonObject()
        clientObj.addProperty("clientName", "WEB_REMIX")
        clientObj.addProperty("clientVersion", "1.20240501.01.00")
        clientObj.addProperty("hl", "pt-BR")
        clientObj.addProperty("gl", "BR")
        clientObj.addProperty("musicExtensionVersion", "v1")
        context.add("client", clientObj)
        root.add("context", context)

        if (!videoId.isNullOrEmpty()) {
            root.addProperty("videoId", videoId)
        }
        if (!playlistId.isNullOrEmpty()) {
            root.addProperty("playlistId", playlistId)
        }
        if (!continuation.isNullOrEmpty()) {
            root.addProperty("continuation", continuation)
        }

        return gson.toJson(root)
    }

    fun fetchNextQueue(
        videoId: String,
        continuation: String? = null,
        authCookie: String? = null,
        excludeVideoId: String? = null
    ): Pair<List<QueueSong>, String?> {
        try {
            val url = "https://music.youtube.com/youtubei/v1/next?prettyPrint=false"

            val jsonBody = buildNextPayload(
                videoId = if (continuation == null) videoId else null,
                playlistId = if (continuation == null) "RDAMVM$videoId" else null,
                continuation = continuation
            )
            val mediaType = "application/json; charset=utf-8".toMediaType()
            val body = jsonBody.toRequestBody(mediaType)

            val requestBuilder = Request.Builder()
                .url(url)
                .post(body)
                .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36 Music")
                .addHeader("Content-Type", "application/json")
                .addHeader("X-Origin", "https://music.youtube.com")

            if (!authCookie.isNullOrBlank()) {
                requestBuilder.addHeader("Cookie", authCookie)
            }

            client.newCall(requestBuilder.build()).execute().use { response ->
                if (!response.isSuccessful) return Pair(emptyList(), null)
                val responseBody = response.body.string()

                val rootObj = JsonParser.parseString(responseBody).asJsonObject

                var playlistPanel = rootObj.getAsJsonObject("continuationContents")
                    ?.getAsJsonObject("playlistPanelContinuation")

                if (playlistPanel == null) {
                    playlistPanel = rootObj
                        .getAsJsonObject("contents")
                        ?.getAsJsonObject("singleColumnMusicWatchNextResultsRenderer")
                        ?.getAsJsonObject("tabbedRenderer")
                        ?.getAsJsonObject("watchNextTabbedResultsRenderer")
                        ?.getAsJsonArray("tabs")
                        ?.get(0)?.asJsonObject
                        ?.getAsJsonObject("tabRenderer")
                        ?.getAsJsonObject("content")
                        ?.getAsJsonObject("musicQueueRenderer")
                        ?.getAsJsonObject("content")
                        ?.getAsJsonObject("playlistPanelRenderer")
                }

                if (playlistPanel == null) return Pair(emptyList(), null)

                val rawItems = playlistPanel.getAsJsonArray("contents") ?: JsonArray()
                val continuations = playlistPanel.getAsJsonArray("continuations")

                val nextToken = continuations?.get(0)?.asJsonObject
                    ?.getAsJsonObject("nextContinuationData")
                    ?.get("continuation")?.asString

                val songs = mutableListOf<QueueSong>()
                for (i in 0 until rawItems.size()) {
                    val itemObj = rawItems.get(i).asJsonObject
                    val videoRenderer = itemObj.getAsJsonObject("playlistPanelVideoRenderer")
                    if (videoRenderer != null) {
                        val trackId = videoRenderer.get("videoId")?.asString
                        if (trackId != null && trackId != excludeVideoId) {
                            parseVideoRenderer(videoRenderer)?.let { songs.add(it) }
                        }
                    }
                }

                return Pair(songs, nextToken)
            }
        } catch (_: Exception) {
            return Pair(emptyList(), null)
        }
    }

    private fun parseVideoRenderer(renderer: JsonObject): QueueSong? {
        val trackId = renderer.get("videoId")?.asString ?: return null

        val title = renderer.getAsJsonObject("title")
            ?.getAsJsonArray("runs")
            ?.get(0)?.asJsonObject
            ?.get("text")?.asString ?: "Unknown"

        val durationText = renderer.getAsJsonObject("lengthText")
            ?.getAsJsonArray("runs")
            ?.get(0)?.asJsonObject
            ?.get("text")?.asString ?: "0:00"
        val durationSeconds = parseTimeToSeconds(durationText)

        val longBylineRuns = mutableListOf<Run>()
        val bylineArray = renderer.getAsJsonObject("longBylineText")
            ?.getAsJsonArray("runs")

        if (bylineArray != null) {
            for (i in 0 until bylineArray.size()) {
                val run = bylineArray.get(i).asJsonObject
                val text = run.get("text")?.asString ?: ""
                val browseId = run.getAsJsonObject("navigationEndpoint")
                    ?.getAsJsonObject("browseEndpoint")
                    ?.get("browseId")?.asString

                longBylineRuns.add(Run(text, browseId))
            }
        }

        val byLineSections = longBylineRuns.splitBySeparator()

        val artistsList = byLineSections
            .firstOrNull()
            ?.oddElements()
            ?.filter { it.text.isNotBlank() }
            ?.map { it.text }
            ?: emptyList()

        val artistsMerged = if (artistsList.isEmpty()) "Unknown Artist" else artistsList.joinToString(", ")

        val navEndpoint = renderer.getAsJsonObject("navigationEndpoint")
        val watchEndpoint = navEndpoint?.getAsJsonObject("watchEndpoint")
        val videoType = watchEndpoint?.get("musicVideoType")?.asString
        val isVideo = videoType != null && videoType != "MUSIC_VIDEO_TYPE_ATV"

        var imgUrl = ""
        val thumbnails = renderer.getAsJsonObject("thumbnail")
            ?.getAsJsonArray("thumbnails")
        if (thumbnails != null && thumbnails.size() > 0) {
            imgUrl = thumbnails.get(thumbnails.size() - 1).asJsonObject
                .get("url")?.asString ?: ""
        }

        return QueueSong(
            id = trackId,
            title = title,
            artist = artistsMerged,
            imageUrl = imgUrl,
            durationSeconds = durationSeconds,
            isVideoSong = isVideo,
            isEpisode = false,
            isPodcast = false
        )
    }

    private fun parseTimeToSeconds(timeStr: String): Long {
        return try {
            val parts = timeStr.split(":").map { it.toLong() }
            when (parts.size) {
                2 -> parts[0] * 60 + parts[1]
                3 -> parts[0] * 3600 + parts[1] * 60 + parts[2]
                else -> 0L
            }
        } catch (_: Exception) {
            0L
        }
    }
}

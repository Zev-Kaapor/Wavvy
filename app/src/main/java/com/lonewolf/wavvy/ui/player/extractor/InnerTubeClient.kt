package com.lonewolf.wavvy.ui.player.extractor

import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.lonewolf.wavvy.ui.player.components.QueueSong
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

private data class Run(
    val text: String,
    val browseId: String? = null
)

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

object InnerTubeClient {

    private val gson = Gson()
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

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
                val responseBody = response.body?.string() ?: return Pair(emptyList(), null)

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
        } catch (e: Exception) {
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

        val album = byLineSections
            .getOrNull(1)
            ?.firstOrNull()
            ?.text
            ?.takeIf { it.isNotBlank() }
            ?: ""

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
        } catch (e: Exception) {
            0L
        }
    }
}

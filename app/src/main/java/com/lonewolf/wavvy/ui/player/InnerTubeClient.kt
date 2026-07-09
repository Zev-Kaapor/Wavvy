package com.lonewolf.wavvy.ui.player

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

object InnerTubeClient {

    private val gson = Gson()
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    // Base JSON contexts payload structures
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

    // Call innerTube endpoint sequence returning mapped structures
    fun fetchNextQueue(
        videoId: String,
        continuation: String? = null,
        authCookie: String? = null
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
                        parseVideoRenderer(videoRenderer)?.let { songs.add(it) }
                    }
                }

                return Pair(songs, nextToken)
            }
        } catch (_: Exception) {
            return Pair(emptyList(), null)
        }
    }

    // Safely transform InnerTube structural elements into data tracks
    private fun parseVideoRenderer(renderer: JsonObject): QueueSong? {
        val trackId = renderer.get("videoId")?.asString ?: return null

        val titleObj = renderer.getAsJsonObject("title")
        val title = titleObj?.getAsJsonArray("runs")?.get(0)?.asJsonObject?.get("text")?.asString ?: "Unknown Track"

        val lengthObj = renderer.getAsJsonObject("lengthText")
        val durationText = lengthObj?.getAsJsonArray("runs")?.get(0)?.asJsonObject?.get("text")?.asString ?: "0:00"
        val durationSeconds = parseTimeToSeconds(durationText)

        val artistList = mutableListOf<String>()
        val bylineObj = renderer.getAsJsonObject("longBylineText")
        val bylineRuns = bylineObj?.getAsJsonArray("runs")
        if (bylineRuns != null) {
            for (i in 0 until bylineRuns.size()) {
                val run = bylineRuns.get(i).asJsonObject
                val text = run.get("text")?.asString
                if (text != null && text.trim() != "•" && text.trim() != "&" && text.trim() != ",") {
                    artistList.add(text)
                }
            }
        }
        val artistsMerged = if (artistList.isEmpty()) "Unknown Artist" else artistList.joinToString(", ")

        val navEndpoint = renderer.getAsJsonObject("navigationEndpoint")
        val watchEndpoint = navEndpoint?.getAsJsonObject("watchEndpoint")
        val videoType = watchEndpoint?.get("musicVideoType")?.asString
        val isVideo = videoType != null && videoType != "MUSIC_VIDEO_TYPE_ATV"

        var imgUrl = ""
        val thumbObj = renderer.getAsJsonObject("thumbnail")
        val thumbnails = thumbObj?.getAsJsonArray("thumbnails")
        if (thumbnails != null && thumbnails.size() > 0) {
            imgUrl = thumbnails.get(thumbnails.size() - 1).asJsonObject.get("url").asString
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

    // Convert structural track timestamp representation into total seconds metric
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

package com.lonewolf.wavvy.ui.player.extractor

// Android core
import android.content.Context
// Third-party extractors (YoutubeDL wrappers)
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
// Project models
import com.lonewolf.wavvy.ui.player.components.QueueSong
// JSON parser
import org.json.JSONObject

// YoutubeDL extractor integration
object YoutubeDlExtractor {

    @Volatile
    var isFallbackReady = false
        private set

    @Volatile
    private var isFallbackInitializing = false

    // Fallback extractor init
    fun initFallback(context: Context) {
        if (isFallbackReady || isFallbackInitializing) return
        isFallbackInitializing = true
        try {
            YoutubeDL.getInstance().init(context)
            isFallbackReady = true
        } catch (_: Exception) {
            // ignore
        } finally {
            isFallbackInitializing = false
        }
    }

    // Fallback extraction via youtube-dl wrapper
    fun extractUrl(context: Context, videoId: String): String? {
        if (!isFallbackReady) {
            initFallback(context)
        }

        return try {
            val request = YoutubeDLRequest("https://music.youtube.com/watch?v=$videoId")
            request.addOption("-f", "bestaudio")
            request.addOption("-g")
            request.addOption("--no-check-certificates")
            request.addOption("--no-playlist")
            request.addOption("--no-warnings")

            val result = YoutubeDL.getInstance().execute(request)
            val output = result.out.trim()

            if (output.startsWith("http")) {
                android.util.Log.d("WavvyExtractor", "Fallback URL captured: $output")
                output
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    // Fallback queue dump chain
    fun fetchQueue(context: Context, videoId: String, limit: Int): List<QueueSong> {
        if (!isFallbackReady) {
            initFallback(context)
        }

        try {
            val targetMixUrl = "https://music.youtube.com/watch?v=${videoId}&list=RDAMVM${videoId}"
            val fallbackQueue = mutableListOf<QueueSong>()
            val scanWindow = limit + 30

            val request = YoutubeDLRequest(targetMixUrl).apply {
                addOption("--dump-json")
                addOption("--no-warnings")
                addOption("--playlist-items", "1-$scanWindow")
                addOption("--match-filter", "!is_live")
            }

            val result = YoutubeDL.getInstance().execute(request)
            val jsonLines = result.out.split("\n")

            for (line in jsonLines) {
                val trimmedLine = line.trim()
                if (trimmedLine.isBlank()) continue
                try {
                    val entry = JSONObject(trimmedLine)
                    val trackId = entry.optString("id", "")
                    val duration = entry.optLong("duration", 0L)

                    val categories = entry.optJSONArray("categories")
                    var isMusicCategory = false
                    if (categories != null) {
                        for (i in 0 until categories.length()) {
                            if (categories.optString(i).equals("Music", ignoreCase = true)) {
                                isMusicCategory = true
                                break
                            }
                        }
                    }

                    val videoType = entry.optString("videoType", "")
                    val isVideo = videoType.isNotBlank() && videoType != "MUSIC_VIDEO_TYPE_ATV"

                    val rawUploader = entry.optString("uploader", "").lowercase()
                    val isPodcastOrEpisode = !isMusicCategory || rawUploader.contains("podcast")

                    if (trackId.isNotBlank() && trackId != videoId) {
                        val isAlreadyAdded = fallbackQueue.any { it.id == trackId }
                        if (!isAlreadyAdded) {
                            val thumbnailsArray = entry.optJSONArray("thumbnails")
                            val extractedThumb = if (thumbnailsArray != null && thumbnailsArray.length() > 0) {
                                thumbnailsArray.optJSONObject(thumbnailsArray.length() - 1)?.optString("url", "") ?: ""
                            } else {
                                entry.optString("thumbnail", "")
                            }

                            fallbackQueue.add(
                                QueueSong(
                                    id = trackId,
                                    title = entry.optString("title", "Unknown Track"),
                                    artist = entry.optString("uploader", entry.optString("artist", "")),
                                    imageUrl = extractedThumb,
                                    durationSeconds = duration,
                                    isVideoSong = isVideo,
                                    isEpisode = isPodcastOrEpisode,
                                    isPodcast = isPodcastOrEpisode
                                )
                            )
                        }
                    }
                } catch (_: Exception) {
                    // Skip malformed entries safely
                }
            }

            if (fallbackQueue.isNotEmpty()) {
                val filtered = fallbackQueue
                    .filterVideoSongs(disableVideos = true)
                    .filterShortsAndPodcasts()
                    .filter { it.durationSeconds < 600L }

                return filtered.take(limit)
            }
        } catch (_: Exception) {
            // ignore
        }
        return emptyList()
    }
}

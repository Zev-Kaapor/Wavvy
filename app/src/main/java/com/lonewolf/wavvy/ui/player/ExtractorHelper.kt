package com.lonewolf.wavvy.ui.player

// Android core
import android.content.Context
// Third-party extractors (NewPipe / YoutubeDL wrappers)
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.stream.StreamInfo
import org.schabi.newpipe.extractor.stream.StreamInfoItem
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
// Project models
import com.lonewolf.wavvy.ui.player.components.QueueSong
// Coroutines
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Collections
import java.util.Locale
// JSON parser
import org.json.JSONObject

// Content filters
fun List<QueueSong>.filterVideoSongs(disableVideos: Boolean = false) =
    if (disableVideos) filterNot { it.isVideoSong } else this

fun List<QueueSong>.filterShortsAndPodcasts() =
    filterNot { it.isEpisode || it.isPodcast || it.id.startsWith("SS") }

// Queue list cache with TTL
private object QueueCache {
    private const val TTL_MS = 10 * 60 * 1000L
    private data class Entry(val queue: List<QueueSong>, val timestamp: Long)

    private val cache = Collections.synchronizedMap(mutableMapOf<String, Entry>())

    fun get(videoId: String): List<QueueSong>? {
        val entry = cache[videoId] ?: return null
        if (System.currentTimeMillis() - entry.timestamp > TTL_MS) {
            cache.remove(videoId)
            return null
        }
        return entry.queue
    }

    fun put(videoId: String, queue: List<QueueSong>) {
        cache[videoId] = Entry(queue, System.currentTimeMillis())
    }

    fun clear() {
        cache.clear()
    }
}

// Stream link decoder
object ExtractorHelper {

    @Volatile
    private var isInitializing = false

    @Volatile
    private var isReady = false

    @Volatile
    private var isFallbackReady = false

    @Volatile
    private var isFallbackInitializing = false

    private val backgroundScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Primary extractor init
    suspend fun initExtractor(): Unit = withContext(Dispatchers.IO) {
        if (isReady || isInitializing) return@withContext
        isInitializing = true
        try {
            NewPipeDownloader.init()
            NewPipe.init(NewPipeDownloader.getInstance())
            isReady = true
        } catch (_: Exception) {
            // ignore init errors here
        } finally {
            isInitializing = false
        }
    }

    // Fallback extractor init
    private suspend fun initFallbackExtractor(context: Context): Unit = withContext(Dispatchers.IO) {
        if (isFallbackReady || isFallbackInitializing) return@withContext
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

    // Public prefetch API
    fun prefetchAudioUrl(context: Context, videoId: String) {
        // launch background extraction
        backgroundScope.launch {
            try {
                val url = extractAudioUrl(context, videoId)
                if (!url.isNullOrEmpty()) {
                    android.util.Log.d("ExtractorHelper", "Prefetch resolved for $videoId")
                }
            } catch (_: Exception) {
                // ignore prefetch errors silently
            }
        }
    }

    // Public extract API
    suspend fun extractAudioUrl(context: Context, videoId: String): String? = withContext(Dispatchers.IO) {
        if (!isReady) {
            initExtractor()
        }

        val primaryResult = tryNewPipeExtraction(videoId)
        if (primaryResult != null) {
            return@withContext primaryResult
        }

        tryYoutubeDlExtraction(context, videoId)
    }

    // Try NewPipe extraction
    private fun tryNewPipeExtraction(videoId: String): String? {
        return try {
            val url = "https://music.youtube.com/watch?v=$videoId"
            val streamInfo = StreamInfo.getInfo(org.schabi.newpipe.extractor.ServiceList.YouTube, url)

            // Prefer audioStreams if present
            val audioStreams = try {
                streamInfo.audioStreams
            } catch (_: Throwable) {
                null
            }

            if (!audioStreams.isNullOrEmpty()) {
                val best = audioStreams.maxByOrNull { it.averageBitrate }
                best?.url
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    // Fallback extraction via youtube-dl wrapper
    private suspend fun tryYoutubeDlExtraction(context: Context, videoId: String): String? {
        if (!isFallbackReady) {
            initFallbackExtractor(context)
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

    // Fetch paginated chunk sequence
    suspend fun fetchMoreSongs(
        context: Context,
        videoId: String,
        offset: Int,
        limit: Int = 50
    ): List<QueueSong> = withContext(Dispatchers.IO) {
        val cachedQueue = QueueCache.get(videoId)

        if (cachedQueue != null) {
            val startIndex = offset.coerceAtMost(cachedQueue.size)
            val endIndex = (offset + limit).coerceAtMost(cachedQueue.size)
            if (startIndex < cachedQueue.size) {
                return@withContext cachedQueue.subList(startIndex, endIndex)
            }
            return@withContext emptyList()
        }

        val fullQueue = fetchUpNextQueue(context, videoId, limit)
        if (fullQueue.isNotEmpty()) {
            QueueCache.put(videoId, fullQueue)
            val startIndex = offset.coerceAtMost(fullQueue.size)
            val endIndex = (offset + limit).coerceAtMost(fullQueue.size)
            if (startIndex < fullQueue.size) {
                return@withContext fullQueue.subList(startIndex, endIndex)
            }
        }

        emptyList()
    }

    // Existing queue extraction
    suspend fun fetchUpNextQueue(context: Context, videoId: String, limit: Int = 50): List<QueueSong> = withContext(Dispatchers.IO) {
        // InnerTube integration
        try {
            val (innertubeSongs, _) = InnerTubeClient.fetchNextQueue(videoId)
            if (innertubeSongs.isNotEmpty()) {
                val filtered = innertubeSongs
                    .filterVideoSongs(disableVideos = true)
                    .filterShortsAndPodcasts()
                    .filter { it.durationSeconds < 600L }

                if (filtered.isNotEmpty()) {
                    return@withContext filtered.take(limit)
                }
            }
        } catch (_: Exception) {
            android.util.Log.e("WavvyExtractor", "InnerTube extraction failed, bypassing to NewPipe")
        }

        if (!isReady) {
            initExtractor()
        }

        try {
            val url = "https://music.youtube.com/watch?v=$videoId"
            val streamInfo = StreamInfo.getInfo(org.schabi.newpipe.extractor.ServiceList.YouTube, url)
            val relatedItems = streamInfo.relatedItems ?: emptyList()

            val primaryQueue = relatedItems.filterIsInstance<StreamInfoItem>()
                .mapNotNull { item: StreamInfoItem ->
                    val extractedId = item.url?.substringAfter("v=", "") ?: ""
                    if (extractedId.isBlank()) return@mapNotNull null

                    val duration = item.duration
                    val realThumb = item.thumbnails.maxByOrNull { it.height }?.url ?: ""

                    QueueSong(
                        id = extractedId,
                        title = item.name ?: "",
                        artist = item.uploaderName ?: "",
                        imageUrl = realThumb,
                        durationSeconds = duration,
                        isVideoSong = false,
                        isEpisode = false,
                        isPodcast = false
                    )
                }

            if (primaryQueue.isNotEmpty()) {
                val filtered = primaryQueue
                    .filterVideoSongs(disableVideos = true)
                    .filterShortsAndPodcasts()
                    .filter { it.durationSeconds < 600L }

                return@withContext filtered.take(limit)
            }
        } catch (_: Exception) {
            android.util.Log.e("WavvyExtractor", "NewPipe queue extraction failed")
        }

        if (!isFallbackReady) {
            initFallbackExtractor(context)
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

                return@withContext filtered.take(limit)
            }
        } catch (_: Exception) {
            // ignore
        }

        emptyList()
    }

    // Format raw duration seconds into timestamp string
    fun formatDuration(seconds: Long): String {
        val hrs = seconds / 3600
        val mins = (seconds % 3600) / 60
        val secs = seconds % 60
        return if (hrs > 0) {
            String.format(Locale.US, "%02d:%02d:%02d", hrs, mins, secs)
        } else {
            String.format(Locale.US, "%02d:%02d", mins, secs)
        }
    }

    // Clear runtime data maps structures
    fun clearCaches() {
        QueueCache.clear()
    }
}

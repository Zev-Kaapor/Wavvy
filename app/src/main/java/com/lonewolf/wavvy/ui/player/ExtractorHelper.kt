package com.lonewolf.wavvy.ui.player

// Android core
import android.content.Context
// Third-party extractors
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.stream.StreamInfo
import org.schabi.newpipe.extractor.stream.StreamInfoItem
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import com.lonewolf.wavvy.ui.player.components.QueueSong
// Coroutines
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Collections
// JSON parser infrastructure
import org.json.JSONObject

// Stream URL cache with TTL
private object StreamUrlCache {
    private const val TTL_MS = 4 * 60 * 1000L
    private data class Entry(val url: String, val timestamp: Long)

    private val cache = Collections.synchronizedMap(mutableMapOf<String, Entry>())

    fun get(videoId: String): String? {
        val entry = cache[videoId] ?: return null
        if (System.currentTimeMillis() - entry.timestamp > TTL_MS) {
            cache.remove(videoId)
            return null
        }
        return entry.url
    }

    fun put(videoId: String, url: String) {
        cache[videoId] = Entry(url, System.currentTimeMillis())
    }

    fun invalidate(videoId: String) {
        cache.remove(videoId)
    }

    fun clear() {
        cache.clear()
    }
}

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

    // Primary extractor init
    suspend fun initExtractor(): Unit = withContext(Dispatchers.IO) {
        if (isReady || isInitializing) return@withContext
        isInitializing = true
        try {
            NewPipeDownloader.init()
            NewPipe.init(NewPipeDownloader.getInstance())
            isReady = true
        } catch (e: Exception) {
            e.printStackTrace()
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
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            isFallbackInitializing = false
        }
    }

    // Fetch paginated chunk sequence out of generated up next tracks queue
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

        val fullQueue = fetchUpNextQueue(context, videoId)
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

    // Fetch up next queue items with fallback mechanics enabled
    suspend fun fetchUpNextQueue(context: Context, videoId: String): List<QueueSong> = withContext(Dispatchers.IO) {
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
                    val realThumb = item.thumbnails?.maxByOrNull { it.height }?.url ?: ""
                    QueueSong(
                        id = extractedId,
                        title = item.name ?: "",
                        artist = item.uploaderName ?: "",
                        imageUrl = realThumb,
                        durationSeconds = 0L
                    )
                }

            if (primaryQueue.isNotEmpty()) {
                return@withContext primaryQueue
            }
        } catch (e: Exception) {
            android.util.Log.e("WavvyExtractor", "NewPipe queue extraction failed: ${e.message}")
        }

        if (!isFallbackReady) {
            initFallbackExtractor(context)
        }

        try {
            val targetMixUrl = "https://music.youtube.com/watch?v=${videoId}&list=RDAMVM${videoId}"
            val request = YoutubeDLRequest(targetMixUrl).apply {
                addOption("--dump-json")
                addOption("--no-warnings")
                addOption("--playlist-items", "1-20")
            }

            val result = YoutubeDL.getInstance().execute(request)
            val jsonLines = result.out.split("\n")
            val fallbackQueue = mutableListOf<QueueSong>()

            for (line in jsonLines) {
                val trimmedLine = line.trim()
                if (trimmedLine.isBlank()) continue
                try {
                    val entry = JSONObject(trimmedLine)
                    val trackId = entry.optString("id", "")
                    if (trackId.isNotBlank() && trackId != videoId) {
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
                                durationSeconds = entry.optLong("duration", 0L)
                            )
                        )
                    }
                } catch (jsonEx: Exception) {
                    // Skip malformed entries safely
                }
            }

            if (fallbackQueue.isNotEmpty()) {
                return@withContext fallbackQueue
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        emptyList()
    }

    // Resolve audio stream URL
    suspend fun extractAudioUrl(
        context: Context,
        videoId: String,
        forceRefresh: Boolean = false
    ): String? = withContext(Dispatchers.IO) {

        if (!forceRefresh) {
            StreamUrlCache.get(videoId)?.let { cachedUrl ->
                android.util.Log.d("WavvyExtractor", "Cache hit for $videoId")
                return@withContext cachedUrl
            }
        } else {
            StreamUrlCache.invalidate(videoId)
        }

        if (!isReady) {
            initExtractor()
        }

        val primaryResult = tryNewPipeExtraction(videoId)
        if (primaryResult != null) {
            StreamUrlCache.put(videoId, primaryResult)
            return@withContext primaryResult
        }

        val fallbackResult = tryYoutubeDlExtraction(context, videoId)
        if (fallbackResult != null) {
            StreamUrlCache.put(videoId, fallbackResult)
        }
        fallbackResult
    }

    // Primary extraction via NewPipe
    private fun tryNewPipeExtraction(videoId: String): String? {
        return try {
            val url = "https://www.youtube.com/watch?v=$videoId"
            val streamInfo = StreamInfo.getInfo(url)

            val audioStream = streamInfo.audioStreams
                .filter { it.format?.mimeType?.contains("audio") == true }
                .maxByOrNull { it.averageBitrate }

            audioStream?.url
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // Fallback extraction via yt-dlp
    private suspend fun tryYoutubeDlExtraction(context: Context, videoId: String): String? {
        if (!isFallbackReady) {
            initFallbackExtractor(context)
        }

        return try {
            val request = YoutubeDLRequest("https://www.youtube.com/watch?v=$videoId")
            request.addOption("-f", "ba")
            request.addOption("-g")
            request.addOption("--no-check-certificates")
            request.addOption("--no-playlist")
            request.addOption("--no-warnings")
            request.addOption("--extractor-args", "youtube:player_client=android_vr")

            val streamInfo = YoutubeDL.getInstance().execute(request)
            val output = streamInfo.out.trim()

            if (output.startsWith("http")) {
                android.util.Log.d("WavvyExtractor", "Fallback URL captured: $output")
                output
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // Clear runtime data maps structures
    fun clearCaches() {
        QueueCache.clear()
        StreamUrlCache.clear()
    }
}

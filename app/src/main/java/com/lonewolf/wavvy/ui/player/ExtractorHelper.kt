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
import java.util.LinkedHashMap
// JSON parser
import org.json.JSONObject

// Track cache entry (audio url + timestamp)
private data class TrackCacheEntry(val audioUrl: String, val timestamp: Long)

// LRU + TTL track cache
private object TrackCache {
    private const val CACHE_CAPACITY = 3 // keep last 3 recently used tracks
    private const val CACHE_TTL_MS = 5 * 60 * 1000L // 5 minutes TTL

    // LinkedHashMap with accessOrder=true to implement LRU
    private val map = Collections.synchronizedMap(object : LinkedHashMap<String, TrackCacheEntry>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, TrackCacheEntry>?): Boolean {
            return this.size > CACHE_CAPACITY
        }
    })

    fun get(videoId: String): String? {
        val entry = map[videoId] ?: return null
        if (System.currentTimeMillis() - entry.timestamp > CACHE_TTL_MS) {
            map.remove(videoId)
            return null
        }
        return entry.audioUrl
    }

    fun put(videoId: String, audioUrl: String) {
        map[videoId] = TrackCacheEntry(audioUrl, System.currentTimeMillis())
    }

    fun invalidate(videoId: String) {
        map.remove(videoId)
    }

    fun clear() {
        map.clear()
    }

    // Expose for debugging
    fun snapshotKeys(): List<String> = synchronized(map) { map.keys.toList() }

    // New helper: check whether a cached entry exists and is still fresh (within TTL)
    fun isFresh(videoId: String, ttlMs: Long = CACHE_TTL_MS): Boolean {
        val entry = synchronized(map) { map[videoId] } ?: return false
        return System.currentTimeMillis() - entry.timestamp <= ttlMs
    }
}

// Queue list cache with TTL (unchanged)
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

    // Fallback extractor init (used by youtube-dl fallback)
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

    // Public prefetch API: start extracting and cache result (non-blocking)
    fun prefetchAudioUrl(context: Context, videoId: String) {
        // if cache hit, nothing to do
        if (TrackCache.get(videoId) != null) return

        // launch background extraction and cache
        backgroundScope.launch {
            try {
                val url = extractAudioUrl(context, videoId)
                if (!url.isNullOrEmpty()) {
                    android.util.Log.d("ExtractorHelper", "Prefetch cached for $videoId")
                }
            } catch (_: Exception) {
                // ignore prefetch errors silently
            }
        }
    }

    // Public helper: check if audio url is cached and still fresh (within TTL)
    fun isAudioCachedAndFresh(videoId: String): Boolean {
        return TrackCache.isFresh(videoId)
    }

    // Public extract API (uses TrackCache first)
    suspend fun extractAudioUrl(context: Context, videoId: String, forceRefresh: Boolean = false): String? = withContext(Dispatchers.IO) {
        if (!forceRefresh) {
            TrackCache.get(videoId)?.let { cachedUrl ->
                android.util.Log.d("WavvyExtractor", "TrackCache hit for $videoId")
                return@withContext cachedUrl
            }
        } else {
            TrackCache.invalidate(videoId)
        }

        if (!isReady) {
            initExtractor()
        }

        val primaryResult = tryNewPipeExtraction(videoId)
        if (primaryResult != null) {
            TrackCache.put(videoId, primaryResult)
            return@withContext primaryResult
        }

        val fallbackResult = tryYoutubeDlExtraction(context, videoId)
        if (fallbackResult != null) {
            TrackCache.put(videoId, fallbackResult)
        }
        fallbackResult
    }

    // Expose quick read-only helper
    fun getCachedAudioUrl(videoId: String): String? = TrackCache.get(videoId)

    // Try NewPipe extraction (best-effort)
    private fun tryNewPipeExtraction(videoId: String): String? {
        return try {
            val url = "https://www.youtube.com/watch?v=$videoId"
            val streamInfo = StreamInfo.getInfo(org.schabi.newpipe.extractor.ServiceList.YouTube, url)

            // Prefer audioStreams if present
            val audioStreams = try {
                streamInfo.audioStreams
            } catch (_: Throwable) {
                null
            }

            if (!audioStreams.isNullOrEmpty()) {
                val best = audioStreams.maxByOrNull { it.averageBitrate ?: 0 }
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
            val request = YoutubeDLRequest("https://www.youtube.com/watch?v=$videoId")
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

    // Existing queue extraction (uses NewPipe primary and youtube-dl fallback)
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
        } catch (_: Exception) {
            android.util.Log.e("WavvyExtractor", "NewPipe queue extraction failed")
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
                } catch (_: Exception) {
                    // Skip malformed entries safely
                }
            }

            if (fallbackQueue.isNotEmpty()) {
                return@withContext fallbackQueue
            }
        } catch (_: Exception) {
            // ignore
        }

        emptyList()
    }

    // Clear runtime data maps structures
    fun clearCaches() {
        QueueCache.clear()
        TrackCache.clear()
    }

    // Debug helper: snapshot of cached keys
    fun debugCachedTrackKeys(): List<String> = TrackCache.snapshotKeys()
}

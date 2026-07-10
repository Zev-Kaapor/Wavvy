package com.lonewolf.wavvy.ui.player.extractor

// Android core
import android.content.Context
// Project models
import com.lonewolf.wavvy.ui.player.components.QueueSong
// Coroutines
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Collections

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

    private val backgroundScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Primary extractor init
    suspend fun initExtractor() {
        NewPipeExtractor.init()
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
        if (!NewPipeExtractor.isReady) {
            initExtractor()
        }

        val primaryResult = NewPipeExtractor.extractUrl(videoId)
        if (primaryResult != null) {
            return@withContext primaryResult
        }

        YoutubeDlExtractor.extractUrl(context, videoId)
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
        // Safe adjusted target count preventing overflow (Current track + Recommendations = Total Custom Limit)
        val targetRecommendationsLimit = (limit - 1).coerceAtLeast(1)

        // InnerTube integration
        val innerTubeList = InnerTubeExtractor.fetchQueue(videoId, targetRecommendationsLimit)
        if (innerTubeList.isNotEmpty()) {
            return@withContext innerTubeList
        }

        // NewPipe fallback sequence
        val newPipeList = NewPipeExtractor.fetchQueue(videoId, targetRecommendationsLimit)
        if (newPipeList.isNotEmpty()) {
            return@withContext newPipeList
        }

        // YoutubeDL deepest fallback chain
        return@withContext YoutubeDlExtractor.fetchQueue(context, videoId, targetRecommendationsLimit)
    }

    // Clear runtime data maps structures
    fun clearCaches() {
        QueueCache.clear()
    }
}

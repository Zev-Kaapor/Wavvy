package com.lonewolf.wavvy.ui.player

// Android core
import android.content.Context
// Third-party extractors
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.stream.StreamInfo
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
// Coroutines
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Collections

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

        android.util.Log.w("WavvyExtractor", "NewPipeExtractor failed for $videoId, falling back to yt-dlp")

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
}

package com.lonewolf.wavvy.ui.player

// Android core contexts
import android.content.Context
// Third-party extraction tools
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
// Coroutines execution and context wrappers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// Stream link decoding engine
object ExtractorHelper {

    @Volatile
    private var isInitializing = false

    @Volatile
    private var isReady = false

    // Native binary initialization engine
    suspend fun initExtractor(context: Context): Unit = withContext(Dispatchers.IO) {
        if (isReady || isInitializing) return@withContext
        isInitializing = true
        try {
            YoutubeDL.getInstance().init(context)
            isReady = true
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            isInitializing = false
        }
    }

    // Direct stream download URL resolution pipeline
    suspend fun extractAudioUrl(context: Context, videoId: String): String? = withContext(Dispatchers.IO) {
        if (!isReady) {
            initExtractor(context)
        }

        try {
            val request = YoutubeDLRequest("https://www.youtube.com/watch?v=$videoId")
            request.addOption("-f", "ba")
            request.addOption("-g")
            request.addOption("--no-check-certificates")
            request.addOption("--no-playlist")
            request.addOption("--no-warnings")

            // Target infrastructure identification properties
            request.addOption("--extractor-args", "youtube:player_client=android_vr")

            val streamInfo = YoutubeDL.getInstance().execute(request)
            val output = streamInfo.out.trim()

            if (output.startsWith("http")) {
                android.util.Log.d("WavvyExtractor", "URL capturada: $output")
                return@withContext output
            }
            null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}

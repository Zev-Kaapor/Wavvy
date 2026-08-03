package com.wavvy.app.features.player.data.extractor

// Android log utilities
import android.util.Log
// OkHttp networking
import okhttp3.OkHttpClient
import okhttp3.Request as OkRequest
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MediaType.Companion.toMediaTypeOrNull
// NewPipe Extractor
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Request as NpRequest
import org.schabi.newpipe.extractor.downloader.Response as NpResponse
import org.schabi.newpipe.extractor.exceptions.ReCaptchaException
import org.schabi.newpipe.extractor.services.youtube.YoutubeJavaScriptPlayerManager
// Java IO
import java.io.IOException
import java.util.concurrent.TimeUnit

// OkHttp-backed downloader for NewPipe Extractor
private class WavvyNewPipeDownloader(private val client: OkHttpClient) : Downloader() {

    @Throws(IOException::class, ReCaptchaException::class)
    override fun execute(request: NpRequest): NpResponse {
        val requestBuilder = OkRequest.Builder()
            .url(request.url())
            .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")

        request.headers()?.forEach { (key, values) ->
            values.forEach { value -> requestBuilder.addHeader(key, value) }
        }

        val httpMethod = request.httpMethod()
        val requestBody = request.dataToSend()

        val okBody = when {
            requestBody != null -> requestBody.toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
            httpMethod == "POST" -> ByteArray(0).toRequestBody(null)
            else -> null
        }

        when (httpMethod) {
            "GET" -> requestBuilder.get()
            "POST" -> requestBuilder.post(okBody!!)
            "DELETE" -> requestBuilder.delete(okBody)
            else -> requestBuilder.get()
        }

        val response = client.newCall(requestBuilder.build()).execute()
        return NpResponse(
            response.code,
            response.message,
            response.headers.toMultimap(),
            response.body.string(),
            response.request.url.toString()
        )
    }
}

// NewPipe Extractor singleton initializer and stream resolver
object NewPipeHelper {

    private var isInitialized = false

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    // Initialize NewPipe with OkHttp downloader — call once at app startup
    fun init() {
        if (isInitialized) return
        try {
            NewPipe.init(WavvyNewPipeDownloader(okHttpClient))
            isInitialized = true
            Log.d("NewPipeHelper", "NewPipe Extractor initialized")
        } catch (e: Exception) {
            Log.e("NewPipeHelper", "Failed to initialize NewPipe Extractor: ${e.message}")
        }
    }

    // Extract direct audio stream URL using NewPipe's StreamExtractor (handles signature decryption & throttling)
    fun extractStreamUrl(videoId: String): String? {
        return try {
            init()
            val watchUrl = "https://www.youtube.com/watch?v=$videoId"
            val extractor = org.schabi.newpipe.extractor.ServiceList.YouTube.getStreamExtractor(watchUrl)
            extractor.fetchPage()

            val audioStreams = extractor.audioStreams
            if (!audioStreams.isNullOrEmpty()) {
                // Get highest average bitrate audio stream
                val bestAudio = audioStreams.maxByOrNull { it.averageBitrate }
                val resolvedUrl = bestAudio?.content
                if (!resolvedUrl.isNullOrEmpty()) {
                    Log.d("NewPipeHelper", "StreamExtractor resolved audio stream bitrate=${bestAudio.averageBitrate} for $videoId")
                    return resolvedUrl
                }
            }
            null
        } catch (e: Exception) {
            Log.e("NewPipeHelper", "extractStreamUrl error for $videoId: ${e.message}")
            null
        }
    }
}

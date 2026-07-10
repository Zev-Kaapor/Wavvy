package com.lonewolf.wavvy.ui.player.extractor

import okhttp3.OkHttpClient
import okhttp3.RequestBody
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Request
import org.schabi.newpipe.extractor.downloader.Response
import org.schabi.newpipe.extractor.exceptions.ReCaptchaException
import java.io.IOException
import java.util.concurrent.TimeUnit

// HTTP downloader for NewPipe extractor
class NewPipeDownloader private constructor(
    builder: OkHttpClient.Builder
) : Downloader() {

    companion object {
        private const val USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:140.0) Gecko/20100101 Firefox/140.0"

        @Volatile
        private var instance: NewPipeDownloader? = null

        // Initialize singleton instance
        fun init(builder: OkHttpClient.Builder? = null): NewPipeDownloader {
            val created = NewPipeDownloader(builder ?: OkHttpClient.Builder())
            instance = created
            return created
        }

        // Get singleton instance
        fun getInstance(): NewPipeDownloader =
            instance ?: throw IllegalStateException(
                "NewPipeDownloader not initialized. Call init() first."
            )
    }

    // HTTP client with timeout config
    private val client = builder
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val cookies = mutableMapOf<String, String>()

    // Cookie management
    fun setCookie(key: String, value: String) {
        cookies[key] = value
    }

    fun removeCookie(key: String) {
        cookies.remove(key)
    }

    private fun getCookieHeader(): String =
        cookies.values.joinToString("; ")

    // Execute HTTP request
    @Throws(IOException::class, ReCaptchaException::class)
    override fun execute(request: Request): Response {
        val httpMethod = request.httpMethod()
        val url = request.url()
        val headers = request.headers()
        val dataToSend = request.dataToSend()

        val requestBody: RequestBody? = dataToSend?.let { RequestBody.create(null, it) }

        val builder = okhttp3.Request.Builder()
            .method(httpMethod, requestBody)
            .url(url)
            .addHeader("User-Agent", USER_AGENT)

        val cookieHeader = getCookieHeader()
        if (cookieHeader.isNotEmpty()) {
            builder.addHeader("Cookie", cookieHeader)
        }

        headers.forEach { (key, values) ->
            builder.removeHeader(key)
            values.forEach { value ->
                builder.addHeader(key, value)
            }
        }

        client.newCall(builder.build()).execute().use { response ->
            if (response.code == 429) {
                throw ReCaptchaException("reCaptcha Challenge requested", url)
            }

            val body = response.body?.use { it.string() }
            val latestUrl = response.request.url.toString()

            return Response(
                response.code,
                response.message,
                response.headers.toMultimap(),
                body,
                latestUrl
            )
        }
    }
}
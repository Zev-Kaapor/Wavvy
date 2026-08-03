package com.wavvy.app.core.data.remote.kworb

// Network client
import okhttp3.OkHttpClient
import okhttp3.Request
// Time utilities
import java.util.concurrent.TimeUnit

// Raw HTML fetcher for kworb pages
object KworbApi {

    private const val BASE_URL = "https://kworb.net/youtube"

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    // Fetch raw HTML from a kworb path, returns null on failure
    private fun fetchHtml(url: String): String? {
        return try {
            val request = Request.Builder()
                .url(url)
                .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                response.body.string()
            }
        } catch (_: Exception) {
            null
        }
    }

    // Global trending music page
    fun fetchTrendingMusicHtml(): String? =
        fetchHtml("$BASE_URL/trending_music.html")

    // Country weekly insights page
    fun fetchCountryWeeklyHtml(countryCode: String): String? =
        fetchHtml("$BASE_URL/insights/$countryCode.html")

    // Country daily insights page
    fun fetchCountryDailyHtml(countryCode: String): String? =
        fetchHtml("$BASE_URL/insights/${countryCode}_daily.html")
}

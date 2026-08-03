package com.wavvy.app.core.data.remote.kworb

// Enums defining chart types and scopes
enum class KworbChartScope {
    GLOBAL_TRENDING_MUSIC,
    COUNTRY
}

// Chart time window
enum class KworbChartPeriod {
    DAILY,
    WEEKLY
}

// Single chart item model
data class KworbChartEntry(
    val position: Int,
    val title: String,
    val artist: String?,
    val videoId: String? = null
)

// Supported country codes for insights
object KworbCountries {
    val SUPPORTED_CODES: List<String> = listOf(
        "us", "gb", "br", "mx", "ar", "es", "pt", "fr", "de", "it",
        "jp", "kr", "in", "id", "ph", "tr", "ca", "au", "nl", "pl"
    )
}

// YouTube thumbnail URL builder
fun youtubeThumbnailUrl(videoId: String): String =
    "https://i.ytimg.com/vi/$videoId/hqdefault.jpg"

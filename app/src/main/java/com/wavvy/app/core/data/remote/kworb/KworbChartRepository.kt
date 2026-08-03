package com.wavvy.app.core.data.remote.kworb

// Coroutines threading and dispatchers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// Cached HTML entry
private data class CachedHtml(
    val html: String,
    val fetchedAt: Long
)

// Fetches, parses and caches kworb chart data
class KworbChartRepository {

    private companion object {
        const val TRENDING_TTL_MS = 4 * 60 * 60 * 1000L
        const val COUNTRY_TTL_MS = 12 * 60 * 60 * 1000L
    }

    private val htmlCache = mutableMapOf<String, CachedHtml>()

    // Global trending music chart
    suspend fun getTrendingMusic(): List<KworbChartEntry> = withContext(Dispatchers.IO) {
        val html = getCachedOrFetch("trending_music", TRENDING_TTL_MS) {
            KworbApi.fetchTrendingMusicHtml()
        } ?: return@withContext emptyList()

        KworbHtmlParser.parseTrendingMusic(html)
    }

    // Country chart, daily or weekly
    suspend fun getCountryChart(
        countryCode: String,
        period: KworbChartPeriod
    ): List<KworbChartEntry> = withContext(Dispatchers.IO) {
        val cacheKey = "country_${countryCode}_${period.name}"

        val html = getCachedOrFetch(cacheKey, COUNTRY_TTL_MS) {
            when (period) {
                KworbChartPeriod.DAILY -> KworbApi.fetchCountryDailyHtml(countryCode)
                KworbChartPeriod.WEEKLY -> KworbApi.fetchCountryWeeklyHtml(countryCode)
            }
        } ?: return@withContext emptyList()

        when (period) {
            KworbChartPeriod.DAILY -> KworbHtmlParser.parseCountryDaily(html)
            KworbChartPeriod.WEEKLY -> KworbHtmlParser.parseCountryWeekly(html)
        }
    }

    // Return cached html if still valid, otherwise fetch and store
    private fun getCachedOrFetch(
        key: String,
        ttlMs: Long,
        fetch: () -> String?
    ): String? {
        val cached = htmlCache[key]
        val isValid = cached != null && System.currentTimeMillis() - cached.fetchedAt < ttlMs

        if (isValid) return cached.html

        val fresh = fetch() ?: return cached?.html
        htmlCache[key] = CachedHtml(fresh, System.currentTimeMillis())
        return fresh
    }
}

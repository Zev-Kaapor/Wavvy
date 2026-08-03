package com.wavvy.app.features.home.data

// Storage and preferences
import com.wavvy.app.core.data.local.ChartResolutionCache
import com.wavvy.app.core.data.local.SettingsStorage
import com.wavvy.app.core.data.remote.kworb.KworbChartEntry
import com.wavvy.app.core.data.remote.kworb.KworbChartPeriod
import com.wavvy.app.core.data.remote.kworb.KworbChartRepository
import com.wavvy.app.core.data.remote.kworb.KworbChartScope
import com.wavvy.app.core.data.remote.kworb.youtubeThumbnailUrl
// Auth and search
import com.wavvy.app.features.auth.data.AuthRepository
import com.wavvy.app.features.player.data.extractor.InnerTubeSearchClient
// Domain models
import com.wavvy.app.features.home.models.KworbChartConfig
import com.wavvy.app.features.home.models.QuickPick
import com.wavvy.app.features.home.models.QuickPicksSource
// Coroutines
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

// Decides which source feeds the quick picks section and normalizes the result
class QuickPicksRepository(
    private val authRepository: AuthRepository,
    private val recentHistoryManager: RecentHistoryManager,
    private val kworbChartRepository: KworbChartRepository,
    private val chartResolutionCache: ChartResolutionCache,
    private val settingsStorage: SettingsStorage
) {

    private companion object {
        const val MAX_QUICK_PICKS = 20
    }

    // Fetch picks based on the persisted source preference
    suspend fun fetchQuickPicks(): List<QuickPick> = withContext(Dispatchers.IO) {
        val isLoggedIn = authRepository.fetchAuthenticatedAccountDetails() != null
        val source = getPersistedSource(isLoggedIn)

        when (source) {
            QuickPicksSource.YTMUSIC_API -> authRepository.fetchQuickPicks()
            QuickPicksSource.RECENT_HISTORY -> fetchFromRecentHistory()
            QuickPicksSource.KWORB_CHART -> fetchFromKworbChart()
        }
    }

    // Available sources depend on login state
    fun availableSources(isLoggedIn: Boolean): List<QuickPicksSource> {
        return if (isLoggedIn) {
            listOf(QuickPicksSource.YTMUSIC_API, QuickPicksSource.RECENT_HISTORY, QuickPicksSource.KWORB_CHART)
        } else {
            listOf(QuickPicksSource.KWORB_CHART, QuickPicksSource.RECENT_HISTORY)
        }
    }

    // Persisted source preference, falls back to a sane default per login state
    private fun getPersistedSource(isLoggedIn: Boolean): QuickPicksSource {
        val defaultSource = if (isLoggedIn) QuickPicksSource.YTMUSIC_API else QuickPicksSource.KWORB_CHART
        val stored = settingsStorage.getString(SettingsStorage.KEY_QUICK_PICKS_SOURCE, defaultSource.name)
        val resolved = runCatching { QuickPicksSource.valueOf(stored) }.getOrDefault(defaultSource)

        // Guest without history yet, or a logged-out user stuck on a login-only source
        return if (resolved in availableSources(isLoggedIn)) resolved else defaultSource
    }

    // Save the user's chosen source
    fun saveSource(source: QuickPicksSource) {
        settingsStorage.saveString(SettingsStorage.KEY_QUICK_PICKS_SOURCE, source.name)
    }

    // Save the kworb chart config
    fun saveKworbConfig(config: KworbChartConfig) {
        settingsStorage.saveString(SettingsStorage.KEY_KWORB_SCOPE, config.scope.name)
        settingsStorage.saveString(SettingsStorage.KEY_KWORB_COUNTRY, config.countryCode)
        settingsStorage.saveString(SettingsStorage.KEY_KWORB_PERIOD, config.period.name)
    }

    // Read the persisted kworb chart config
    fun getKworbConfig(): KworbChartConfig {
        val scope = runCatching {
            KworbChartScope.valueOf(settingsStorage.getString(SettingsStorage.KEY_KWORB_SCOPE, KworbChartScope.GLOBAL_TRENDING_MUSIC.name))
        }.getOrDefault(KworbChartScope.GLOBAL_TRENDING_MUSIC)

        val period = runCatching {
            KworbChartPeriod.valueOf(settingsStorage.getString(SettingsStorage.KEY_KWORB_PERIOD, KworbChartPeriod.DAILY.name))
        }.getOrDefault(KworbChartPeriod.DAILY)

        val countryCode = settingsStorage.getString(SettingsStorage.KEY_KWORB_COUNTRY, "us")

        return KworbChartConfig(scope = scope, countryCode = countryCode, period = period)
    }

    // Map recent history into the common quick pick shape
    private suspend fun fetchFromRecentHistory(): List<QuickPick> {
        val tracks = recentHistoryManager.recentTracks.first()
        return tracks.map { track ->
            QuickPick(
                videoId = track.id,
                title = track.title,
                artist = track.artist,
                artists = listOf(track.artist),
                thumbnailUrl = track.imageUrl.ifBlank { null }
            )
        }
    }

    // Fetch the configured kworb chart and resolve each entry into a playable video id
    private suspend fun fetchFromKworbChart(): List<QuickPick> {
        val config = getKworbConfig()

        val entries = when (config.scope) {
            KworbChartScope.GLOBAL_TRENDING_MUSIC -> kworbChartRepository.getTrendingMusic()
            KworbChartScope.COUNTRY -> kworbChartRepository.getCountryChart(config.countryCode, config.period)
        }

        // Charts return far more entries than needed, cap before resolving to avoid wasted lookups
        val sampled = entries.shuffled().take(MAX_QUICK_PICKS)

        return sampled.mapNotNull { entry -> resolveEntry(entry) }
    }

    // Resolve a chart entry into a QuickPick, using the direct id when available
    private suspend fun resolveEntry(entry: KworbChartEntry): QuickPick? {
        val videoId = entry.videoId ?: resolveViaSearch(entry) ?: return null

        return QuickPick(
            videoId = videoId,
            title = entry.title,
            artist = entry.artist.orEmpty(),
            artists = listOfNotNull(entry.artist),
            thumbnailUrl = youtubeThumbnailUrl(videoId)
        )
    }

    // Resolve a video id via cache first, then a live search as a last resort
    private suspend fun resolveViaSearch(entry: KworbChartEntry): String? {
        val cacheKey = chartResolutionCache.buildKey(entry.artist, entry.title)

        return chartResolutionCache.get(cacheKey)
            ?: InnerTubeSearchClient.resolveVideoId(entry.title, entry.artist)?.also {
                chartResolutionCache.put(cacheKey, it)
            }
    }
}

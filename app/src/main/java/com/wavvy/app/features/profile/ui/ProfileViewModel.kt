package com.wavvy.app.features.profile.ui

// Android application and view model
import android.app.Application
import android.net.Uri
import android.webkit.CookieManager
import androidx.annotation.StringRes
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
// Time
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import java.time.temporal.WeekFields
import java.util.Locale
// Coroutines and reactive flows
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
// Network
import okhttp3.Request
import org.json.JSONObject
// Project resources
import com.wavvy.app.R
import com.wavvy.app.core.history.ListenStats
import com.wavvy.app.core.history.PlayHistory
import com.wavvy.app.core.network.WavvyProxy
import com.wavvy.app.core.innertube.MusicOrigin
import com.wavvy.app.features.auth.data.AccountClient
import com.wavvy.app.features.auth.data.AccountSession
import com.wavvy.app.features.auth.data.AccountStore
import com.wavvy.app.features.home.data.HomeItem
import com.wavvy.app.features.search.data.SearchRepository

// The periods the numbers of the profile can be counted in, with none chosen everything that was listened to counts
enum class ProfilePeriod(@StringRes val titleRes: Int) {
    Today(R.string.history_filter_today),
    Week(R.string.history_filter_week),
    Month(R.string.history_filter_month),
    Semester(R.string.history_filter_semester),
    Year(R.string.history_filter_year);

    // The day the period starts, the week starts on the first day of the week of the place of the user
    private fun first(today: LocalDate): LocalDate = when (this) {
        Today -> today
        Week -> today.with(TemporalAdjusters.previousOrSame(WeekFields.of(Locale.getDefault()).firstDayOfWeek))
        Month -> today.withDayOfMonth(1)
        Semester -> LocalDate.of(today.year, if (today.monthValue <= SemesterLastMonth) 1 else SemesterLastMonth + 1, 1)
        Year -> LocalDate.of(today.year, 1, 1)
    }

    // The moment the period starts, and the moment the period before it starts
    fun since(zone: ZoneId, today: LocalDate = LocalDate.now(zone)): Long = first(today).atStartOfDay(zone).toInstant().toEpochMilli()

    fun previousSince(zone: ZoneId, today: LocalDate = LocalDate.now(zone)): Long {
        val start = first(today)
        val before = when (this) {
            Today -> start.minusDays(1)
            Week -> start.minusWeeks(1)
            Month -> start.minusMonths(1)
            Semester -> start.minusMonths(SemesterMonths)
            Year -> start.minusYears(1)
        }
        return before.atStartOfDay(zone).toInstant().toEpochMilli()
    }
}

// Everything the profile draws that is asked of the history and of YouTube Music
data class ProfileUiState(
    val period: ProfilePeriod? = null,
    val stats: ListenStats? = null,
    val topSongs: List<RankedSong> = emptyList(),
    val topArtists: List<TopArtist> = emptyList(),
    val artistCount: Int = 0,
    val playlists: List<HomeItem> = emptyList(),
    val bannerUrl: String? = null,
    val channelId: String? = null,
    val about: String? = null,
    val isRefreshing: Boolean = false,
    // True until the numbers of the period are in
    val isLoading: Boolean = true
)

// The last month of the first semester of the year, how many months it has, and how many artists and songs the profile shows
private const val SemesterLastMonth = 6
private const val SemesterMonths = 6L
private const val TopArtistsCount = 5
private const val TopSongsCount = 3

// Where the channel page is read, how the app shows itself to it, the mark of the banner in the page and how far after it is read
private const val ChannelAddress = "https://www.youtube.com/"
private const val BrowserAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:140.0) Gecko/20100101 Firefox/140.0"
private const val ConsentCookie = "CONSENT=YES+1; SOCS=CAI"
private const val BannerMarker = "\"imageBannerViewModel\":{"
private const val BannerScan = 4000
private const val BannerMinWidth = 1500

// How long a banner that was found is kept before it is looked for again, a week
private const val BannerRefreshMs = 7L * 24 * 60 * 60 * 1000

// Loads the numbers of the profile when it opens, and again when the period changes
class ProfileViewModel(application: Application) : AndroidViewModel(application) {
    private val context = application
    private val search = SearchRepository(application)
    private val mutableState = MutableStateFlow(ProfileUiState())
    val state: StateFlow<ProfileUiState> = mutableState.asStateFlow()

    private var statsJob: Job? = null

    init {
        loadStats()
        loadPlaylists()
        loadBanner()
    }

    // A tap on the period that is on turns it off, which counts everything again
    fun selectPeriod(period: ProfilePeriod) {
        mutableState.update { it.copy(period = if (it.period == period) null else period) }
        loadStats()
    }

    // The profile pulled down by the finger, everything is asked again with the mark on until the answers come
    fun refresh() {
        if (mutableState.value.isRefreshing) return
        mutableState.update { it.copy(isRefreshing = true) }
        viewModelScope.launch {
            listOf(loadStats(), loadPlaylists(), loadBanner(isForced = true)).joinAll()
            mutableState.update { it.copy(isRefreshing = false) }
        }
    }

    // The user came back from editing the channel, so the name, the photo, the banner and the words are asked again
    fun syncChannel() {
        viewModelScope.launch {
            val cookies = withContext(Dispatchers.Main) { CookieManager.getInstance().getCookie(MusicOrigin) }
            cookies?.takeIf { it.isNotBlank() }?.let { AccountClient.fetchAccount(it).getOrNull() }?.let { AccountSession.apply(context, it) }
            loadBanner(isForced = true)
        }
    }

    // The banner of the channel, the saved one shows at once and it is looked for again when it is old or when the user asked
    private fun loadBanner(isForced: Boolean = false): Job =
        viewModelScope.launch {
            val store = AccountStore(context)
            val saved = store.banner.first()
            mutableState.update { it.copy(bannerUrl = saved.url, channelId = saved.channelId, about = saved.about) }

            val handle = store.details.first()?.handle ?: return@launch
            if (!isForced && saved.channelId != null && saved.about != null && System.currentTimeMillis() - saved.fetchedAt < BannerRefreshMs) return@launch

            // A failed look keeps the saved banner and tries again the next time
            val found = withContext(Dispatchers.IO) { runCatching { ChannelBanner.find(handle) } }.getOrNull() ?: return@launch
            store.saveBanner(found.url, found.channelId, found.about, System.currentTimeMillis())
            mutableState.update { it.copy(bannerUrl = found.url, channelId = found.channelId, about = found.about) }
        }

    // The time, the songs and the artists of the period, the pictures of the artists come after, as they are found
    private fun loadStats(): Job {
        statsJob?.cancel()
        val job = viewModelScope.launch {
            mutableState.update { it.copy(isLoading = true) }
            val period = mutableState.value.period
            val since = period?.since(ZoneId.systemDefault()) ?: 0L
            val stats = withContext(Dispatchers.IO) { PlayHistory.listenStats(context, since) }
            val songs = ProfileRanking.songs(context, period, TopSongsCount)
            val artists = ProfileRanking.artists(context, period)

            val top = artists.take(TopArtistsCount)
            mutableState.update { it.copy(stats = stats, topSongs = songs, topArtists = top, artistCount = artists.size, isLoading = false) }
            val found = ProfileRanking.withPhotos(search, top)
            mutableState.update { it.copy(topArtists = found) }
        }
        statsJob = job
        return job
    }

    // The playlists of the account, a guest has none
    private fun loadPlaylists(): Job =
        viewModelScope.launch { mutableState.update { it.copy(playlists = ProfileRanking.playlists(context)) } }
}

// The banner of a channel read from its public page, the address is null when the channel has none
private data class BannerResult(val url: String?, val channelId: String?, val about: String?)

private object ChannelBanner {
    // The sizes in the page go from small to big, the first one that is wide enough is used
    private val ChannelId = Regex("""rel="canonical" href="https://www\.youtube\.com/channel/([\w-]+)"""")
    private val Sources = Regex(""""url":"([^"]+)","width":(\d+)""")
    private val About = Regex(""""channelMetadataRenderer":\{"title":"(?:[^"\\]|\\.)*","description":"((?:[^"\\]|\\.)*)"""")

    fun find(handle: String): BannerResult? {
        val request = Request.Builder()
            .url("$ChannelAddress${Uri.encode(handle, "@")}")
            .header("User-Agent", BrowserAgent)
            .header("Accept-Language", "en-US,en;q=0.9")
            .header("Cookie", ConsentCookie)
            .build()

        val html = WavvyProxy.client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return null
            response.body.string()
        }

        // The id of the channel is in the canonical address of the page
        val channelId = ChannelId.find(html)?.groupValues?.get(1)

        // The words of the description are a text of the page with its escapes, empty when the channel has none
        val about = About.find(html)?.groupValues?.get(1)?.let { runCatching { JSONObject("{\"v\":\"$it\"}").getString("v") }.getOrNull() }.orEmpty().trim()

        // Only the part after the banner is read, so the pictures of the channel are not taken for it
        val start = html.indexOf(BannerMarker)
        if (start < 0) return BannerResult(null, channelId, about)
        val sources = Sources.findAll(html.substring(start, minOf(html.length, start + BannerScan))).map { it.groupValues[1] to it.groupValues[2].toInt() }.toList()
        val best = sources.firstOrNull { it.second >= BannerMinWidth } ?: sources.maxByOrNull { it.second }
        return BannerResult(best?.first?.replace("\\u0026", "&")?.replace("\\/", "/"), channelId, about)
    }
}

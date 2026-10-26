package com.wavvy.app.features.profile.ui

// Android context
import androidx.core.content.edit
import android.content.Context
// Time
import java.time.LocalDate
import java.time.ZoneId
import java.util.concurrent.ConcurrentHashMap
// Coroutines
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.json.JSONArray
// Project resources
import com.wavvy.app.core.history.ArtistTotal
import com.wavvy.app.core.history.PlayHistory
import com.wavvy.app.core.history.TopSong
import com.wavvy.app.features.home.data.HomeItem
import com.wavvy.app.features.home.data.HomeItemKind
import com.wavvy.app.features.home.data.PlaylistPagePrefix
import com.wavvy.app.features.library.data.LibraryRepository
import com.wavvy.app.features.library.data.LibrarySource
import com.wavvy.app.features.search.data.SearchCategory
import com.wavvy.app.features.search.data.SearchRepository

// An artist listened to, with the time and the times, how it moved in the ranking, its picture and the page that opens it once they are found
data class TopArtist(
    val name: String,
    val totalMs: Long,
    val plays: Int,
    val cover: String?,
    val trend: String? = null,
    val id: String? = null,
    val photoUrl: String? = null,
    // True once the picture of the artist was looked for, so the cover of a song is only used for an artist that has no picture of its own
    val isLookedUp: Boolean = false
) {
    // The picture shown, the one of the artist, or the cover of a song once it is known the artist has none, nothing while it is still looked for
    val picture: String? get() = photoUrl ?: cover.takeIf { isLookedUp }
}

// A song listened to, with how it moved in the ranking
data class RankedSong(val top: TopSong, val trend: String)

// The names the arrows of a ranking have, the same the charts of YouTube Music use
private const val TrendUp = "ARROW_CHART_UP"
private const val TrendDown = "ARROW_CHART_DOWN"
private const val TrendNeutral = "ARROW_CHART_NEUTRAL"

// How many songs and artists of the period before are ranked to compare with, and how many pictures are asked at once
private const val BeforeLimit = 200
private const val PhotoBatch = 6

// Where the playlists of the account are kept on the phone
private const val PlaylistsPreferences = "profile_playlists"
private const val PlaylistsKey = "playlists"

// The lists of the account that YouTube Music makes by itself, the start of the id of the Shorts and how many playlists are kept
private val AutomaticLists = setOf("LM", "SE", "WL")
private const val ShortsPrefix = "SS"
private const val MaxPlaylists = 50

// Where the name of the first artist ends in the text of the artist of a song
private val ArtistSplit = Regex("""\s*(,|\sfeat\.?\s|\sft\.?\s)\s*""", RegexOption.IGNORE_CASE)

// A period, from where it starts, and the period before it to compare with
data class RankingWindow(val since: Long, val previousSince: Long, val previousUntil: Long)

// The period and the one before it, with none chosen everything counts and is compared with what it was before the last session of listening
fun rankingWindow(period: ProfilePeriod?, sessionStart: Long?, zone: ZoneId = ZoneId.systemDefault(), today: LocalDate = LocalDate.now(zone)): RankingWindow =
    if (period == null) {
        RankingWindow(since = 0L, previousSince = 0L, previousUntil = sessionStart ?: 0L)
    } else {
        RankingWindow(since = period.since(zone, today), previousSince = period.previousSince(zone, today), previousUntil = period.since(zone, today))
    }

// The rankings of what was listened to, each place with how it moved since the period before
object ProfileRanking {
    suspend fun songs(context: Context, period: ProfilePeriod?, limit: Int): List<RankedSong> = withContext(Dispatchers.IO) {
        val window = rankingWindow(period, PlayHistory.lastSessionStart(context))
        val current = PlayHistory.topSongs(context, window.since, Long.MAX_VALUE, limit)
        val before = PlayHistory.topSongs(context, window.previousSince, window.previousUntil, BeforeLimit).map { it.song.id }
        current.mapIndexed { index, top -> RankedSong(top, trend(index, top.song.id, before)) }
    }

    // All the artists of the period, the first artist of a song takes the time of all of them
    suspend fun artists(context: Context, period: ProfilePeriod?): List<TopArtist> = withContext(Dispatchers.IO) {
        val window = rankingWindow(period, PlayHistory.lastSessionStart(context))
        val current = group(PlayHistory.artistTotals(context, window.since, Long.MAX_VALUE))
        val before = group(PlayHistory.artistTotals(context, window.previousSince, window.previousUntil)).take(BeforeLimit).map { it.name }
        current.mapIndexed { index, artist -> artist.copy(trend = trend(index, artist.name, before)) }
    }

    // The pictures and the pages already found, kept on the phone, so they are there when the profile opens and no cover of a song shows in their place
    // They show even when old, the ones that are a day old are looked at again after, so a picture an artist changed comes in
    fun withKnownPhotos(context: Context, artists: List<TopArtist>): List<TopArtist> =
        artists.map { artist -> ArtistDirectory.known(context, artist.name)?.let { match -> artist.copy(id = match.id, photoUrl = match.thumbnailUrl, isLookedUp = !ArtistDirectory.isOld(context, artist.name)) } ?: artist }

    // Finds the picture and the page of each artist, a few at a time, and keeps what it found so the next time is at once
    // With refresh it looks again even when what is kept is new, which is for the few artists of the profile and not for a long list
    suspend fun withPhotos(context: Context, search: SearchRepository, artists: List<TopArtist>, refresh: Boolean = false): List<TopArtist> = withContext(Dispatchers.IO) {
        artists.chunked(PhotoBatch).flatMap { batch ->
            batch.map { artist ->
                async {
                    ArtistDirectory.find(context, search, artist.name, refresh)?.let { match -> artist.copy(id = match.id, photoUrl = match.thumbnailUrl, isLookedUp = true) }
                        ?: artist.copy(isLookedUp = true)
                }
            }.awaitAll()
        }
    }

    // The playlists of the account, without the lists that YouTube Music makes by itself, a guest has none
    suspend fun playlists(context: Context): List<HomeItem> =
        LibraryRepository(context).load(LibrarySource.Playlists).getOrNull()?.items.orEmpty().filter { item ->
            val id = item.id.removePrefix(PlaylistPagePrefix)
            item.kind == HomeItemKind.Playlist && id !in AutomaticLists && !id.startsWith(ShortsPrefix)
        }.take(MaxPlaylists)

    // The playlists of the account kept on the phone, so they show at once when the profile opens, none for a guest
    fun cachedPlaylists(context: Context): List<HomeItem> {
        val saved = context.applicationContext.getSharedPreferences(PlaylistsPreferences, Context.MODE_PRIVATE).getString(PlaylistsKey, null) ?: return emptyList()
        val array = runCatching { JSONArray(saved) }.getOrNull() ?: return emptyList()

        return (0 until array.length()).mapNotNull { index ->
            val json = array.optJSONObject(index) ?: return@mapNotNull null
            HomeItem(
                kind = HomeItemKind.Playlist,
                id = json.optString("id").ifEmpty { return@mapNotNull null },
                title = json.optString("title"),
                thumbnailUrl = json.optString("cover").ifEmpty { null },
                author = json.optString("author").ifEmpty { null },
                countText = json.optString("count").ifEmpty { null }
            )
        }
    }

    // Keeps the playlists, or forgets them when the account is gone
    fun savePlaylists(context: Context, playlists: List<HomeItem>) {
        val array = JSONArray()
        playlists.forEach { item ->
            array.put(JSONObject().put("id", item.id).put("title", item.title).put("cover", item.thumbnailUrl.orEmpty()).put("author", item.author.orEmpty()).put("count", item.countText.orEmpty()))
        }
        context.applicationContext.getSharedPreferences(PlaylistsPreferences, Context.MODE_PRIVATE).edit { putString(PlaylistsKey, array.toString()) }
    }

    private fun group(totals: List<ArtistTotal>): List<TopArtist> =
        totals
            .groupBy { it.artist.split(ArtistSplit).first().trim() }
            .filterKeys { it.isNotEmpty() }
            .map { (name, list) -> TopArtist(name, list.sumOf { it.total }, list.sumOf { it.plays }, list.maxByOrNull { it.total }?.cover) }
            .sortedWith(compareByDescending<TopArtist> { it.plays }.thenByDescending { it.totalMs })

    // Up when it rose or is new, down when it fell, and a dot when it stayed or there is nothing before to compare with
    private fun trend(index: Int, key: String, before: List<String>): String {
        if (before.isEmpty()) return TrendNeutral
        val old = before.indexOf(key)
        return when {
            old < 0 || index < old -> TrendUp
            index > old -> TrendDown
            else -> TrendNeutral
        }
    }
}

// The pages of the artists found by their names, kept on the phone and looked at again when they are a day old, an artist that was not found is not looked for again while the app runs
private object ArtistDirectory {
    private val loaded = ConcurrentHashMap<String, HomeItem>()
    private val missing = ConcurrentHashMap.newKeySet<String>()
    private val checked = ConcurrentHashMap.newKeySet<String>()

    // Where they are kept, and how long they are good for
    private const val PreferencesName = "artist_directory"
    private const val RefreshMs = 24L * 60 * 60 * 1000

    // What is already known of an artist, in memory or kept on the phone, even when it is old
    fun known(context: Context, name: String): HomeItem? {
        loaded[name]?.let { return it }

        val saved = context.applicationContext.getSharedPreferences(PreferencesName, Context.MODE_PRIVATE).getString(name, null) ?: return null
        val json = runCatching { JSONObject(saved) }.getOrNull() ?: return null

        return HomeItem(kind = HomeItemKind.Artist, id = json.optString("id"), title = name, thumbnailUrl = json.optString("photo").ifEmpty { null })
            .also { loaded[name] = it }
    }

    // True when what is kept of an artist is a day old or more, or when it was not looked at since the app opened
    fun isOld(context: Context, name: String): Boolean {
        if (name in checked) return false

        val saved = context.applicationContext.getSharedPreferences(PreferencesName, Context.MODE_PRIVATE).getString(name, null) ?: return true
        val at = runCatching { JSONObject(saved).optLong("at") }.getOrDefault(0L)
        return System.currentTimeMillis() - at > RefreshMs
    }

    suspend fun find(context: Context, search: SearchRepository, name: String, refresh: Boolean = false): HomeItem? {
        if (!refresh && !isOld(context, name)) known(context, name)?.let { return it }
        if (name in missing) return null

        val match = search.search(name, SearchCategory.Artists, keepsHistory = false).getOrNull()?.items
            ?.filter { it.kind == HomeItemKind.Artist }
            ?.let { artists -> artists.firstOrNull { it.title.equals(name, ignoreCase = true) } ?: artists.firstOrNull() }

        if (match == null) {
            missing += name
            // What is kept stays when the look failed, and is looked at again the next time
            return known(context, name)
        } else {
            checked += name
            loaded[name] = match
            context.applicationContext.getSharedPreferences(PreferencesName, Context.MODE_PRIVATE).edit {
                putString(name, JSONObject().put("id", match.id).put("photo", match.thumbnailUrl.orEmpty()).put("at", System.currentTimeMillis()).toString())
            }
        }
        return match
    }
}

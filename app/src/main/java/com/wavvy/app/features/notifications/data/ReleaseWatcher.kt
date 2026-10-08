package com.wavvy.app.features.notifications.data

// Android context
import android.content.Context
import androidx.core.content.edit
// Coroutines
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
// Time
import java.util.Calendar
// Project resources
import com.wavvy.app.core.history.PlayHistory
import com.wavvy.app.core.history.ReleaseEntity
import com.wavvy.app.core.innertube.InnerTubeClient
import com.wavvy.app.core.innertube.YouTubeSession
import com.wavvy.app.core.innertube.currentSession
import com.wavvy.app.core.innertube.findObjects
import com.wavvy.app.core.innertube.objectAt
import com.wavvy.app.core.innertube.stringAt
import com.wavvy.app.features.artist.data.ArtistParser
import com.wavvy.app.features.home.data.HomeItem
import com.wavvy.app.features.home.data.HomeItemKind
import com.wavvy.app.features.home.data.HomeParser

// The page of YouTube Music that lists the artists the account follows
private const val FollowedArtistsBrowseId = "FEmusic_library_corpus_artists"

// Pages of that list that are read at most, and artists looked at at the same time
private const val MaxPages = 5
private const val ParallelArtists = 4

// Where the time of the last check is kept, and how long to wait before checking again
private const val PreferencesName = "release_watcher"
private const val LastCheckKey = "last_check"
private const val CheckIntervalMillis = 11 * 60 * 60 * 1000L

// Prefix of the id of the page of an artist
private const val ArtistPrefix = "UC"
private val YearPattern = Regex("\\b(20\\d\\d)\\b")

// An artist the account follows
private class FollowedArtist(val id: String, val name: String, val photo: String?)

// Looks at the artists the account follows for releases that were not there before, so the app can tell the user, adapted from the idea of the Activity page of YouTube Music
object ReleaseWatcher {
    private val running = Mutex()

    // Checks the artists when it has been a while since the last time, or at once when forced, and gives back the new releases, a guest follows no one
    // A little under the twelve hours of the background work, so the work is never held back by a check that happened a moment before
    suspend fun check(context: Context, force: Boolean = false): List<ReleaseEntity> {
        if (!running.tryLock()) return emptyList()

        try {
            val app = context.applicationContext
            val preferences = app.getSharedPreferences(PreferencesName, Context.MODE_PRIVATE)
            val now = System.currentTimeMillis()
            if (!force && now - preferences.getLong(LastCheckKey, 0L) < CheckIntervalMillis) return emptyList()

            val session = currentSession(app)
            if (session.cookies.isNullOrBlank()) return emptyList()
            val artists = followedArtists(session).getOrNull()?.takeIf { it.isNotEmpty() } ?: return emptyList()

            // A few at a time, so a long list does not open too many connections at once
            val gate = Semaphore(ParallelArtists)
            val news = coroutineScope {
                artists.map { artist ->
                    async(Dispatchers.IO) { gate.withPermit { look(app, session, artist, now) } }
                }.awaitAll().flatten()
            }

            preferences.edit { putLong(LastCheckKey, now) }
            return news
        } finally {
            running.unlock()
        }
    }

    // The artists of the library of the account, the list comes in pages
    private suspend fun followedArtists(session: YouTubeSession): Result<List<FollowedArtist>> = runCatching {
        val artists = LinkedHashMap<String, FollowedArtist>()
        var response = InnerTubeClient.browse(session, browseId = FollowedArtistsBrowseId).getOrThrow()

        repeat(MaxPages) {
            response.findObjects("musicResponsiveListItemRenderer").forEach { row ->
                val id = row.stringAt("navigationEndpoint", "browseEndpoint", "browseId")?.takeIf { it.startsWith(ArtistPrefix) } ?: return@forEach
                val name = row.stringAt("flexColumns", 0, "musicResponsiveListItemFlexColumnRenderer", "text", "runs", 0, "text") ?: return@forEach
                artists.putIfAbsent(id, FollowedArtist(id, name, HomeParser.coverOf(row.objectAt("thumbnail"))))
            }

            val next = response.findObjects("nextContinuationData").firstNotNullOfOrNull { it.stringAt("continuation") } ?: return@runCatching artists.values.toList()
            response = InnerTubeClient.browse(session, continuation = next).getOrThrow()
        }
        artists.values.toList()
    }

    // The albums and singles on the page of an artist are written down, the new ones are found by the database and given back
    private suspend fun look(context: Context, session: YouTubeSession, artist: FollowedArtist, now: Long): List<ReleaseEntity> {
        val response = InnerTubeClient.browse(session, browseId = artist.id).getOrNull() ?: return emptyList()
        val year = Calendar.getInstance().get(Calendar.YEAR)
        val page = ArtistParser.parse(response, year) ?: return emptyList()

        val releases = page.sections.flatMap { it.items }
            .filter { it.kind == HomeItemKind.Album }
            .distinctBy { it.id }
            .map { it.toRelease(artist, now, year) }

        return PlayHistory.saveReleases(context, artist.id, releases, now)
    }

    // Only a release of this year is news, and one of the year before while it is January so a release of the last days of December is not lost
    // YouTube pages old releases in and out and gives them new ids, so an old one that shows up late must not be announced
    private fun isNewYear(year: Int?, currentYear: Int): Boolean {
        if (year == null) return false
        return year == currentYear || (Calendar.getInstance().get(Calendar.MONTH) == Calendar.JANUARY && year == currentYear - 1)
    }

    // A release as the database keeps it
    private fun HomeItem.toRelease(artist: FollowedArtist, now: Long, currentYear: Int): ReleaseEntity {
        val year = lineText?.let { YearPattern.find(it)?.groupValues?.get(1)?.toIntOrNull() }

        return ReleaseEntity(
            id = id,
            artistId = artist.id,
            artistName = artist.name,
            artistPhoto = artist.photo,
            title = title,
            coverUrl = thumbnailUrl,
            kind = typeText,
            seenAt = now,
            isAnnounced = isNewYear(year, currentYear),
            isRead = false
        )
    }
}

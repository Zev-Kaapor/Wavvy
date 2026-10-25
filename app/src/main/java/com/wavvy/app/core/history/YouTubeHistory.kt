package com.wavvy.app.core.history

// Android context
import android.content.Context
// Time, JSON and collections
import java.time.LocalDate
import java.time.ZoneId
import java.util.concurrent.ConcurrentHashMap
import org.json.JSONObject
// Project resources
import com.wavvy.app.R
import com.wavvy.app.core.innertube.InnerTubeClient
import com.wavvy.app.core.innertube.YouTubeSession
import com.wavvy.app.core.innertube.arrayAt
import com.wavvy.app.core.innertube.currentSession
import com.wavvy.app.core.innertube.objectAt
import com.wavvy.app.core.innertube.objects
import com.wavvy.app.core.innertube.resize
import com.wavvy.app.core.innertube.stringAt
import com.wavvy.app.core.playback.StreamResolver
import com.wavvy.app.features.settings.data.PrivacyPrefs

// How long a listening must be to have its place kept, ten minutes, shorter ones are songs and start over
private const val EpisodeMinimumMillis = 10 * 60 * 1000L
private const val MillisPerSecond = 1000L

// The page of the history of the account, the icon of the option that takes a song out of it, and the size the covers are kept at
private const val HistoryPage = "FEmusic_history"
private const val RemoveIcon = "REMOVE_FROM_HISTORY"
private const val ArtworkSize = 544

// How far apart two songs are placed when there is no song of the device to place them between, about the length of a song
private const val PlacedStepMs = 3 * 60 * 1000L

// The seconds in a minute, to read the length of a song as the page writes it
private const val SecondsPerMinute = 60L

// A song of the history of the account, with the name of the part of the page it is in and the token that takes it out
private data class AccountEntry(val song: SongEntity, val part: String?, val removeToken: String?)

// Tells YouTube Music what was listened to here, so it shows in the history of the account, and brings what was listened to elsewhere, nothing is sent or read for a guest
object YouTubeHistory {
    // The tokens that take the songs out of the history of the account, from the last time it was read
    private val removeTokens = ConcurrentHashMap<String, String>()

    // Registers a listening of the video with the address that came with its stream, a failure is ignored since the history of the device already has it
    suspend fun report(context: Context, videoId: String) {
        val session = currentSession(context)
        if (session.cookies.isNullOrBlank()) return

        val address = StreamResolver.accountTrackingOf(videoId)?.playbackUrl ?: return
        InnerTubeClient.registerPlayback(session, address)
    }

    // Tells where a long listening stands, only for what is long enough to be an episode, which is what resumes where it stopped
    suspend fun reportPosition(context: Context, videoId: String, positionMs: Long, lengthMs: Long) {
        // A length that is not known yet counts as long when the place is already far, since a song is never listened to that far in one go
        val isLong = if (lengthMs > 0L) lengthMs >= EpisodeMinimumMillis else positionMs >= EpisodeMinimumMillis
        if (!isLong || positionMs <= 0L) return

        val session = currentSession(context)
        if (session.cookies.isNullOrBlank()) return

        val address = StreamResolver.accountTrackingOf(videoId)?.watchtimeUrl ?: return
        InnerTubeClient.reportWatchTime(session, address, positionMs / MillisPerSecond, maxOf(lengthMs, positionMs) / MillisPerSecond)
    }

    // Brings the songs listened to elsewhere in the account into the history of the device, placed among the ones listened to here
    // Gives false when the page cannot be read, a guest has nothing to bring and a paused history or one not sent to the account brings nothing either
    suspend fun pull(context: Context): Boolean {
        if (PrivacyPrefs.pauseHistory || !PrivacyPrefs.syncToAccount) return true

        val session = currentSession(context)
        if (session.cookies.isNullOrBlank()) return true

        val entries = read(session) ?: return false
        val known = PlayHistory.playedTimes(context).associate { it.id to it.lastPlayedAt }
        val found = place(entries, known, context.getString(R.string.history_today), context.getString(R.string.history_yesterday))
        if (found.isNotEmpty()) PlayHistory.addFound(context, found)
        return true
    }

    // Takes a song out of the history of the account too, the page is read again when its token is not known yet
    suspend fun remove(context: Context, videoId: String): Boolean {
        val session = currentSession(context)
        if (session.cookies.isNullOrBlank()) return false

        val token = removeTokens[videoId] ?: run {
            read(session)
            removeTokens[videoId]
        } ?: return false

        return InnerTubeClient.feedback(session, listOf(token)).isSuccess.also { if (it) removeTokens.remove(videoId) }
    }

    // The songs of the page, the most recent first, and the tokens that take them out
    private suspend fun read(session: YouTubeSession): List<AccountEntry>? {
        val page = InnerTubeClient.browse(session, browseId = HistoryPage).getOrNull() ?: return null
        val entries = page.arrayAt("contents", "singleColumnBrowseResultsRenderer", "tabs", 0, "tabRenderer", "content", "sectionListRenderer", "contents").objects()
            .mapNotNull { it.objectAt("musicShelfRenderer") }
            .flatMap { shelf ->
                val part = shelf.stringAt("title", "runs", 0, "text")
                shelf.arrayAt("contents").objects().mapNotNull { item -> item.objectAt("musicResponsiveListItemRenderer")?.let { entryOf(it, part) } }
            }

        removeTokens.clear()
        entries.forEach { entry -> entry.removeToken?.let { removeTokens[entry.song.id] = it } }
        return entries
    }

    // A row of the page as a song, its name, its artists, its length, its cover and the token of the option that takes it out
    private fun entryOf(row: JSONObject, part: String?): AccountEntry? {
        val id = row.stringAt("playlistItemData", "videoId") ?: return null
        val title = runsText(row, "flexColumns", 0, "musicResponsiveListItemFlexColumnRenderer", "text", "runs") ?: return null
        val artist = runsText(row, "flexColumns", 1, "musicResponsiveListItemFlexColumnRenderer", "text", "runs")
        val length = runsText(row, "fixedColumns", 0, "musicResponsiveListItemFixedColumnRenderer", "text", "runs")?.let(::lengthMs) ?: 0L
        val artwork = row.arrayAt("thumbnail", "musicThumbnailRenderer", "thumbnail", "thumbnails").objects().lastOrNull()
            ?.optString("url")?.takeIf { it.isNotBlank() }?.resize(ArtworkSize, ArtworkSize)
        val token = row.arrayAt("menu", "menuRenderer", "items").objects()
            .mapNotNull { it.objectAt("menuServiceItemRenderer") }
            .firstOrNull { it.stringAt("icon", "iconType") == RemoveIcon }
            ?.stringAt("serviceEndpoint", "feedbackEndpoint", "feedbackToken")

        val song = SongEntity(id = id, title = title, artist = artist, artworkUrl = artwork, durationMs = length, totalPlayTimeMs = 0L, lastPlayedAt = 0L, isEstimated = true)
        return AccountEntry(song, part, token)
    }

    // The words of the runs at the end of the path joined, empty when there are none
    private fun runsText(row: JSONObject, vararg path: Any): String? =
        row.arrayAt(*path).objects().joinToString("") { it.optString("text") }.takeIf { it.isNotBlank() }

    // The length of a song written as minutes and seconds, or hours, minutes and seconds, empty when it cannot be read
    private fun lengthMs(text: String): Long? =
        text.split(":").fold(0L) { total, part -> total * SecondsPerMinute + (part.trim().toLongOrNull() ?: return null) } * MillisPerSecond

    // Gives a time to each song that is not on the device, between the songs of the device around it in the page, since the page is in the order they were played
    // The part of the page keeps the guess in its day when it is today or yesterday, and before yesterday when it is an older part
    private fun place(entries: List<AccountEntry>, known: Map<String, Long>, today: String, yesterday: String): List<SongEntity> {
        val zone = ZoneId.systemDefault()
        val now = System.currentTimeMillis()
        val date = LocalDate.now(zone)
        val startOfToday = date.atStartOfDay(zone).toInstant().toEpochMilli()
        val startOfYesterday = date.minusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()

        // A song played again here after the page put it lower would break the order, so each anchor is never later than the one above it
        var ceiling = Long.MAX_VALUE
        val anchors = entries.map { entry ->
            known[entry.song.id]?.let { time -> minOf(time, ceiling).also { ceiling = it } }
        }

        return entries.mapIndexedNotNull { index, entry ->
            if (anchors[index] != null) return@mapIndexedNotNull null

            val newer = (index - 1 downTo 0).firstOrNull { anchors[it] != null }
            val older = (index + 1 until entries.size).firstOrNull { anchors[it] != null }
            val guess = when {
                newer != null && older != null -> {
                    val top = anchors[newer]!!
                    val bottom = anchors[older]!!
                    top - (top - bottom) * (index - newer) / (older - newer)
                }
                newer != null -> anchors[newer]!! - (index - newer) * PlacedStepMs
                older != null -> minOf(now, anchors[older]!! + (older - index) * PlacedStepMs)
                else -> now - index * PlacedStepMs
            }

            val time = when {
                entry.part.equals(today, ignoreCase = true) -> guess.coerceIn(startOfToday, now)
                entry.part.equals(yesterday, ignoreCase = true) -> guess.coerceIn(startOfYesterday, startOfToday - 1)
                else -> minOf(guess, startOfYesterday - 1)
            }
            entry.song.copy(lastPlayedAt = time)
        }
    }
}

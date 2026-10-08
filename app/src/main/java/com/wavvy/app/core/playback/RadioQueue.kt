package com.wavvy.app.core.playback

// Android context and web storage
import android.content.Context
import android.webkit.CookieManager
// Collections
import java.util.concurrent.ConcurrentHashMap
// Coroutines
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
// JSON parsing
import org.json.JSONObject
// Project resources
import com.wavvy.app.core.innertube.InnerTubeClient
import com.wavvy.app.core.innertube.MusicOrigin
import com.wavvy.app.core.innertube.VisitorStore
import com.wavvy.app.core.innertube.YouTubeSession
import com.wavvy.app.core.innertube.arrayAt
import com.wavvy.app.core.innertube.deviceLocale
import com.wavvy.app.core.innertube.objectAt
import com.wavvy.app.core.innertube.objects
import com.wavvy.app.core.innertube.stringAt
import com.wavvy.app.features.auth.data.Entry
import com.wavvy.app.features.auth.data.EntryStore

// Prefix of the endless radio YouTube Music builds from a song
private const val RadioPrefix = "RDAMVM"

// Kinds of a queue item that are videos, official and uploaded by users, the others are songs and episodes
private val VideoTypes = setOf("MUSIC_VIDEO_TYPE_OMV", "MUSIC_VIDEO_TYPE_UGC")

// Units of a duration such as 3:45 or 1:02:30
private const val SecondsPerMinute = 60L
private const val SecondsPerHour = 3600L
private const val MillisPerSecond = 1000L

// Prefix of the id of the page of an album
private const val AlbumPagePrefix = "MPRE"

// Pages a song leads to, the ids of its artists and of its album when it has one
class TrackLinks(
    val artistIds: List<String>,
    val albumId: String?
)

// One page of a radio, with what is needed to ask the next one
class RadioPage(
    val tracks: List<PlayableTrack>,
    val playlistId: String,
    val continuation: String?
)

// The radio of a song, the queue YouTube Music plays after it, adapted from Metrolist (GPL-3.0)
internal object RadioQueue {
    // First page of the radio of a song, which starts with the song itself
    suspend fun start(context: Context, videoId: String): Result<RadioPage> =
        page(context, videoId, RadioPrefix + videoId, continuation = null)

    // The page after the last one asked
    suspend fun more(context: Context, videoId: String, playlistId: String, continuation: String): Result<RadioPage> =
        page(context, videoId, playlistId, continuation)

    // Pages already worked out, so a second tap on the same song does not ask again
    private val linksCache = ConcurrentHashMap<String, TrackLinks>()

    // The pages of a song when they are already known, so a tap can open them at once
    fun cachedLinks(videoId: String): TrackLinks? = linksCache[videoId]

    // The artists and the album of a song, read from its own entry at the start of its radio
    suspend fun links(context: Context, videoId: String): Result<TrackLinks> {
        linksCache[videoId]?.let { return Result.success(it) }

        return InnerTubeClient.next(currentSession(context), videoId, RadioPrefix + videoId).mapCatching { response ->
            val renderer = response.objectAt(
                "contents", "singleColumnMusicWatchNextResultsRenderer", "tabbedRenderer",
                "watchNextTabbedResultsRenderer", "tabs", 0, "tabRenderer", "content",
                "musicQueueRenderer", "content", "playlistPanelRenderer"
            )?.arrayAt("contents").objects()
                ?.mapNotNull { it.optJSONObject("playlistPanelVideoRenderer") }
                ?.firstOrNull { it.stringAt("videoId") == videoId }
                ?: throw IllegalStateException("Song not in the answer")

            val ids = renderer.arrayAt("longBylineText", "runs").objects()
                .mapNotNull { it.stringAt("navigationEndpoint", "browseEndpoint", "browseId") }

            TrackLinks(
                artistIds = ids.filterNot { it.startsWith(AlbumPagePrefix) }.distinct(),
                albumId = ids.firstOrNull { it.startsWith(AlbumPagePrefix) }
            ).also { linksCache[videoId] = it }
        }
    }

    // Asks a page of the queue and reads its songs
    private suspend fun page(context: Context, videoId: String, playlistId: String, continuation: String?): Result<RadioPage> =
        InnerTubeClient.next(currentSession(context), videoId, playlistId, continuation).mapCatching { response ->
            val panel = response.objectAt("continuationContents", "playlistPanelContinuation")
                ?: response.objectAt(
                    "contents", "singleColumnMusicWatchNextResultsRenderer", "tabbedRenderer",
                    "watchNextTabbedResultsRenderer", "tabs", 0, "tabRenderer", "content",
                    "musicQueueRenderer", "content", "playlistPanelRenderer"
                )
                ?: throw IllegalStateException("No queue in the answer")

            RadioPage(
                // Videos are left out of the queue, except the one that was asked for
                tracks = panel.arrayAt("contents").objects()
                    .mapNotNull { it.optJSONObject("playlistPanelVideoRenderer")?.let(::trackOf) }
                    .filter { !it.isVideo || it.id == videoId },
                playlistId = panel.stringAt("playlistId") ?: playlistId,
                continuation = panel.arrayAt("continuations").objects().firstNotNullOfOrNull { next ->
                    next.stringAt("nextRadioContinuationData", "continuation")
                        ?: next.stringAt("nextContinuationData", "continuation")
                }
            )
        }

    // A song of the queue, with its largest picture
    private fun trackOf(renderer: JSONObject): PlayableTrack? {
        val id = renderer.stringAt("videoId") ?: return null
        val title = renderer.stringAt("title", "runs", 0, "text") ?: return null

        return PlayableTrack(
            id = id,
            title = title,
            artist = renderer.arrayAt("shortBylineText", "runs").objects()
                .joinToString("") { it.optString("text") }
                .ifBlank { null },
            artworkUrl = renderer.arrayAt("thumbnail", "thumbnails").objects().lastOrNull()?.stringAt("url"),
            durationMs = renderer.stringAt("lengthText", "runs", 0, "text")?.let(::parseTime) ?: 0L,
            isVideo = renderer.stringAt(
                "navigationEndpoint", "watchEndpoint", "watchEndpointMusicSupportedConfigs",
                "watchEndpointMusicConfig", "musicVideoType"
            ) in VideoTypes
        )
    }

    // A duration such as 3:45 or 1:02:30 in milliseconds, empty when the text is not one
    private fun parseTime(text: String): Long? {
        val parts = text.trim().split(':').map { it.toLongOrNull() ?: return null }

        val seconds = when (parts.size) {
            2 -> parts[0] * SecondsPerMinute + parts[1]
            3 -> parts[0] * SecondsPerHour + parts[1] * SecondsPerMinute + parts[2]
            else -> return null
        }
        return seconds * MillisPerSecond
    }

    // The same identity the Home uses, with the account when the user signed in with Google
    private suspend fun currentSession(context: Context): YouTubeSession {
        val locale = deviceLocale()
        val cookies = if (EntryStore(context).entry.first() == Entry.Google) {
            withContext(Dispatchers.Main) { CookieManager.getInstance().getCookie(MusicOrigin) }
        } else {
            null
        }
        return YouTubeSession(cookies = cookies, visitorData = VisitorStore(context).get(locale), locale = locale)
    }
}

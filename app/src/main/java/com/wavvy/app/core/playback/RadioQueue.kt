package com.wavvy.app.core.playback

// Android context and web storage
import android.content.Context
import android.webkit.CookieManager
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
                tracks = panel.arrayAt("contents").objects()
                    .mapNotNull { it.optJSONObject("playlistPanelVideoRenderer")?.let(::trackOf) },
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

package com.wavvy.app.core.playback

// Android context and web storage
import android.content.Context
import android.webkit.CookieManager
// Collections
import java.util.concurrent.ConcurrentHashMap
// Coroutines
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
// JSON parsing
import org.json.JSONObject
// Project resources
import com.wavvy.app.core.innertube.InnerTubeClient
import com.wavvy.app.features.home.data.HomeParser
import com.wavvy.app.features.home.data.VideoTypes
import com.wavvy.app.features.settings.data.ContentPrefs
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

// Units of a duration such as 3:45 or 1:02:30
private const val SecondsPerMinute = 60L
private const val SecondsPerHour = 3600L
private const val MillisPerSecond = 1000L

// The mark that ends the artists in the line under a song, and the words and signs that join them in the languages of the app
private const val BylineDivider = "\u2022"
private val ArtistJoiners = setOf("", ",", "&", "e", "and", "y", "et", "und", "feat.", "ft.", "com", "with")

// How many more times a radio is asked when it comes back empty or fails, and how long it waits before each one
private const val RadioRetries = 2
private const val RadioRetryMillis = 600L

// Prefix of the id of the page of an album
private const val AlbumPagePrefix = "MPRE"

// The kind of a video that someone uploaded, not an artist, its line names the channel
private const val UploadType = "MUSIC_VIDEO_TYPE_UGC"

// Pages a song leads to, the ids of its artists and of its album when it has one, with the name of the album
class TrackLinks(
    // The artists in the order the song shows them, each with its name and its id, empty when the artist has no page
    val artists: List<Pair<String, String>>,
    val albumId: String?,
    val albumName: String? = null,
    // The name of the song as YouTube Music shows it
    val title: String? = null,
    // True for a video someone uploaded, whose artists are the channel and not the ones of the song
    val isUpload: Boolean = false
) {}

// One page of a radio, with what is needed to ask the next one
class RadioPage(
    val tracks: List<PlayableTrack>,
    val playlistId: String,
    val continuation: String?
)

// The radio of a song, the queue YouTube Music plays after it, adapted from Metrolist (GPL-3.0)
internal object RadioQueue {
    // First page of the radio of a song, which starts with the song itself
    // YouTube sometimes answers with a radio that has only the song itself, or fails, so it is asked again a few times, as Metrolist (GPL-3.0) does
    suspend fun start(context: Context, videoId: String): Result<RadioPage> {
        var result = page(context, videoId, RadioPrefix + videoId, continuation = null)
        repeat(RadioRetries) {
            if (result.getOrNull()?.let { it.tracks.size > 1 } == true) return result
            delay(RadioRetryMillis)
            result = page(context, videoId, RadioPrefix + videoId, continuation = null)
        }
        return result
    }

    // The page after the last one asked
    suspend fun more(context: Context, videoId: String, playlistId: String, continuation: String): Result<RadioPage> {
        var result = page(context, videoId, playlistId, continuation)
        repeat(RadioRetries) {
            if (result.isSuccess) return result
            delay(RadioRetryMillis)
            result = page(context, videoId, playlistId, continuation)
        }
        return result
    }

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
                ?.mapNotNull { it.optJSONObject("playlistPanelVideoRenderer") ?: it.objectAt("playlistPanelVideoWrapperRenderer", "primaryRenderer", "playlistPanelVideoRenderer") }
                ?.firstOrNull { it.stringAt("videoId") == videoId }
                ?: throw IllegalStateException("Song not in the answer")

            val runs = renderer.arrayAt("longBylineText", "runs").objects()
            val albumRun = runs.firstOrNull { run ->
                run.stringAt("navigationEndpoint", "browseEndpoint", "browseId")?.startsWith(AlbumPagePrefix) == true
            }
            val albumId = albumRun?.stringAt("navigationEndpoint", "browseEndpoint", "browseId")

            // The artists come before the album, the words that join them are not artists, and one without a page keeps an empty id
            val artists = runs
                .takeWhile { run ->
                    run.optString("text").trim() != BylineDivider &&
                        run.stringAt("navigationEndpoint", "browseEndpoint", "browseId")?.startsWith(AlbumPagePrefix) != true
                }
                .mapNotNull { run ->
                    val text = run.optString("text")
                    val id = run.stringAt("navigationEndpoint", "browseEndpoint", "browseId")
                    if (id == null && text.trim().lowercase() in ArtistJoiners) null else text.trim() to id.orEmpty()
                }
                .filter { it.first.isNotEmpty() }
                .distinctBy { it.first }

            TrackLinks(
                artists = artists,
                albumId = albumId,
                albumName = albumRun?.stringAt("text")?.trim(),
                title = renderer.stringAt("title", "runs", 0, "text")?.trim(),
                isUpload = renderer.stringAt(
                    "navigationEndpoint", "watchEndpoint", "watchEndpointMusicSupportedConfigs",
                    "watchEndpointMusicConfig", "musicVideoType"
                ) == UploadType
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

            // A signed in answer wraps most rows with the other version of the same recording, so a video is swapped for its song when there is one
            val rows = panel.arrayAt("contents").objects().mapNotNull { row ->
                val wrapper = row.optJSONObject("playlistPanelVideoWrapperRenderer")
                val primary = (row.optJSONObject("playlistPanelVideoRenderer")
                    ?: wrapper?.objectAt("primaryRenderer", "playlistPanelVideoRenderer"))?.let(::trackOf) ?: return@mapNotNull null
                val song = wrapper?.arrayAt("counterpart").objects()
                    .mapNotNull { it.objectAt("counterpartRenderer", "playlistPanelVideoRenderer")?.let(::trackOf) }
                    .firstOrNull { !it.isVideo }
                if (primary.isVideo && primary.id != videoId && song != null) song else primary
            }.filter { !ContentPrefs.hideExplicit || !it.isExplicit || it.id == videoId }

            // The radio plays songs, the video the user chose stays, and a page with no song at all is kept as it came
            val songs = rows.filter { !it.isVideo || it.id == videoId }

            RadioPage(
                tracks = if (songs.any { it.id != videoId }) songs else rows,
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
            ) in VideoTypes,
            isExplicit = HomeParser.hasExplicitBadge(renderer.arrayAt("badges"))
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

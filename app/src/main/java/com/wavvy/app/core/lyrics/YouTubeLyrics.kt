package com.wavvy.app.core.lyrics

// JSON parsing
import org.json.JSONObject
// Project resources
import com.wavvy.app.core.innertube.InnerTubeClient
import com.wavvy.app.core.innertube.YouTubeSession
import com.wavvy.app.core.innertube.arrayAt
import com.wavvy.app.core.innertube.objects
import com.wavvy.app.core.innertube.stringAt

// Plain lyrics YouTube Music shows in the lyrics tab of the player, the last source because they have no timing
internal object YouTubeLyrics {
    private const val LyricsPageType = "MUSIC_PAGE_TYPE_TRACK_LYRICS"

    // Opens the page of the video, finds the lyrics tab and reads its text
    suspend fun getLyrics(session: YouTubeSession, videoId: String): Result<String> = runCatching {
        val next = InnerTubeClient.next(session, videoId).getOrThrow()
        val browseId = lyricsBrowseId(next) ?: throw IllegalStateException("No lyrics tab")
        val page = InnerTubeClient.browse(session, browseId = browseId).getOrThrow()
        lyricsText(page) ?: throw IllegalStateException("Lyrics unavailable")
    }

    // Id of the page behind the lyrics tab
    private fun lyricsBrowseId(next: JSONObject): String? =
        next.arrayAt(
            "contents", "singleColumnMusicWatchNextResultsRenderer", "tabbedRenderer",
            "watchNextTabbedResultsRenderer", "tabs"
        ).objects()
            .mapNotNull { it.optJSONObject("tabRenderer")?.optJSONObject("endpoint")?.optJSONObject("browseEndpoint") }
            .firstOrNull {
                it.stringAt("browseEndpointContextSupportedConfigs", "browseEndpointContextMusicConfig", "pageType") == LyricsPageType
            }
            ?.optString("browseId")
            ?.takeIf { it.isNotEmpty() }

    // Text of the description shelf of the lyrics page
    private fun lyricsText(page: JSONObject): String? =
        page.arrayAt("contents", "sectionListRenderer", "contents").objects()
            .firstNotNullOfOrNull { it.optJSONObject("musicDescriptionShelfRenderer") }
            ?.arrayAt("description", "runs").objects()
            .joinToString("") { it.optString("text") }
            .takeIf { it.isNotBlank() }
}

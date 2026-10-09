package com.wavvy.app.features.podcast.data

// JSON helpers of the YouTube Music answers
import com.wavvy.app.core.innertube.arrayAt
import com.wavvy.app.core.innertube.objectAt
import com.wavvy.app.core.innertube.objects
import com.wavvy.app.core.innertube.stringAt
// JSON
import org.json.JSONObject
// Project resources
import com.wavvy.app.features.home.data.HomeParser

// Turns the page of an episode of YouTube Music into its details
object EpisodeParser {
    fun parse(response: JSONObject): EpisodePage? {
        val columns = response.objectAt("contents", "twoColumnBrowseResultsRenderer") ?: return null
        val header = columns.arrayAt("tabs").objects().firstOrNull()
            ?.arrayAt("tabRenderer", "content", "sectionListRenderer", "contents").objects().firstOrNull()
            ?.objectAt("musicResponsiveHeaderRenderer") ?: return null

        val play = header.arrayAt("buttons").objects().firstNotNullOfOrNull { it.objectAt("musicPlayButtonRenderer") }
        val subtitle = header.arrayAt("subtitle", "runs").objects().map { it.optString("text").trim() }
            .filter { it.isNotEmpty() && it != Separator }
        val progress = header.objectAt("progress", "musicPlaybackProgressRenderer")

        return EpisodePage(
            videoId = play?.stringAt("playNavigationEndpoint", "watchEndpoint", "videoId") ?: return null,
            title = header.stringAt("title", "runs", 0, "text") ?: return null,
            coverUrl = HomeParser.coverOf(header.objectAt("thumbnail")),
            showName = header.stringAt("straplineTextOne", "runs", 0, "text"),
            showId = header.stringAt("straplineTextOne", "runs", 0, "navigationEndpoint", "browseEndpoint", "browseId"),
            views = subtitle.getOrNull(0),
            age = subtitle.getOrNull(1),
            durationText = progress?.arrayAt("durationText", "runs").objects().lastOrNull()?.optString("text")?.trim()?.takeIf { it.isNotEmpty() },
            progressPercent = progress?.optInt("playbackProgressPercentage") ?: 0,
            description = columns.objectAt("secondaryContents", "sectionListRenderer")?.arrayAt("contents").objects()
                .firstNotNullOfOrNull { it.objectAt("musicDescriptionShelfRenderer") }
                ?.arrayAt("description", "runs").objects()
                .map { run ->
                    val endpoint = run.objectAt("navigationEndpoint")
                    EpisodeTextPart(
                        text = run.optString("text"),
                        isLink = endpoint != null,
                        url = endpoint?.stringAt("urlEndpoint", "url")
                    )
                }
        )
    }

    // The sign YouTube Music puts between the views and the age
    private const val Separator = "•"
}

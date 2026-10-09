package com.wavvy.app.features.podcast.data

// JSON helpers of the YouTube Music answers
import com.wavvy.app.core.innertube.arrayAt
import com.wavvy.app.core.innertube.objectAt
import com.wavvy.app.core.innertube.objects
import com.wavvy.app.core.innertube.stringAt
// JSON
import org.json.JSONArray
import org.json.JSONObject
// Project resources
import com.wavvy.app.features.home.data.HomeParser

// Turns the page of a podcast of YouTube Music into the details, the buttons and the episodes
object PodcastParser {
    fun parse(response: JSONObject): PodcastPage? {
        val columns = response.objectAt("contents", "twoColumnBrowseResultsRenderer") ?: return null
        val header = columns.arrayAt("tabs").objects().firstOrNull()
            ?.arrayAt("tabRenderer", "content", "sectionListRenderer", "contents").objects().firstOrNull()
            ?.objectAt("musicResponsiveHeaderRenderer") ?: return null

        val list = columns.objectAt("secondaryContents", "sectionListRenderer")
        val shelf = list?.arrayAt("contents").objects().firstNotNullOfOrNull { it.objectAt("musicShelfRenderer") }

        val save = header.arrayAt("buttons").objects().firstNotNullOfOrNull { it.objectAt("toggleButtonRenderer") }

        return PodcastPage(
            title = header.stringAt("title", "runs", 0, "text") ?: return null,
            author = header.stringAt("straplineTextOne", "runs", 0, "text"),
            authorPhoto = HomeParser.coverOf(header.objectAt("straplineThumbnail")),
            coverUrl = HomeParser.coverOf(header.objectAt("thumbnail")),
            description = header.arrayAt("description", "musicDescriptionShelfRenderer", "description", "runs").objects()
                .joinToString("") { it.optString("text") }.trim().takeIf { it.isNotEmpty() },
            saveLabel = save?.stringAt("defaultText", "runs", 0, "text"),
            chips = chipsOf(list?.arrayAt("header", "chipCloudRenderer", "chips")),
            episodes = episodesOf(shelf?.arrayAt("contents"), continuationOf(shelf))
        )
    }

    // The episodes that come after a filter or an order was chosen, or the next ones of a list
    fun parseEpisodes(response: JSONObject): PodcastEpisodes {
        val shelf = response.objectAt("continuationContents", "musicShelfContinuation")
        return episodesOf(shelf?.arrayAt("contents"), continuationOf(shelf))
    }

    // The episodes of a shelf, a list that has none carries a message instead
    private fun episodesOf(contents: JSONArray?, continuation: String?): PodcastEpisodes {
        val rows = contents.objects()

        return PodcastEpisodes(
            episodes = rows.mapNotNull { it.objectAt("musicMultiRowListItemRenderer")?.let(HomeParser::parseEpisodeRow) },
            continuation = continuation,
            message = rows.firstNotNullOfOrNull { it.arrayAt("messageRenderer", "text", "runs") }?.objects()
                ?.joinToString("") { it.optString("text") }?.trim()?.takeIf { it.isNotEmpty() }
        )
    }

    // The token that asks the next episodes
    private fun continuationOf(shelf: JSONObject?): String? =
        shelf?.arrayAt("continuations").objects().firstNotNullOfOrNull { it.stringAt("nextContinuationData", "continuation") }

    // The buttons of the row, the first one orders and the others filter
    private fun chipsOf(chips: JSONArray?): List<PodcastChip> =
        chips.objects().mapNotNull { chip ->
            val renderer = chip.objectAt("chipCloudChipRenderer") ?: return@mapNotNull null
            val title = renderer.stringAt("text", "simpleText") ?: renderer.stringAt("text", "runs", 0, "text") ?: return@mapNotNull null
            val endpoint = renderer.objectAt("navigationEndpoint")

            val sorts = endpoint?.arrayAt("openPopupAction", "popup", "menuPopupRenderer", "items").objects().mapNotNull { item ->
                val option = item.objectAt("menuNavigationItemRenderer") ?: return@mapNotNull null
                PodcastSort(
                    title = option.stringAt("text", "simpleText") ?: return@mapNotNull null,
                    token = reloadTokenOf(option.objectAt("navigationEndpoint")) ?: return@mapNotNull null,
                    isSelected = option.has("icon")
                )
            }

            PodcastChip(title = title, token = reloadTokenOf(endpoint), sorts = sorts)
        }

    // The token a button carries to ask the list again under it
    private fun reloadTokenOf(endpoint: JSONObject?): String? =
        endpoint?.stringAt("browseSectionListReloadEndpoint", "continuation", "reloadContinuationData", "continuation")
}

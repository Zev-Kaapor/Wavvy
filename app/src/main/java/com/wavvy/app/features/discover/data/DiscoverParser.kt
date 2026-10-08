package com.wavvy.app.features.discover.data

// JSON helpers of the YouTube Music answers
import com.wavvy.app.core.innertube.arrayAt
import com.wavvy.app.core.innertube.objectAt
import com.wavvy.app.core.innertube.objects
import com.wavvy.app.core.innertube.stringAt
// JSON
import org.json.JSONArray
import org.json.JSONObject
// Project resources
import com.wavvy.app.features.home.data.HomeLink
import com.wavvy.app.features.home.data.HomeParser

// What marks a card as wide, a video or an episode instead of a cover
private const val WideAspect = "RECTANGLE_16_9"

// Turns the Explore page of YouTube Music into the buttons and shelves of the Discover tab
object DiscoverParser {
    fun parse(response: JSONObject): DiscoverPage? {
        val blocks = response.arrayAt("contents", "singleColumnBrowseResultsRenderer", "tabs").objects().firstOrNull()
            ?.arrayAt("tabRenderer", "content", "sectionListRenderer", "contents").objects() ?: return null

        val shortcuts = blocks.firstNotNullOfOrNull { it.objectAt("gridRenderer") }
            ?.arrayAt("items").objects()
            ?.mapNotNull { it.objectAt("musicNavigationButtonRenderer")?.let(::shortcutOf) }
            .orEmpty()

        val shelves = blocks.mapNotNull { block -> block.objectAt("musicCarouselShelfRenderer")?.let(::blockOf) }
        if (shortcuts.isEmpty() && shelves.isEmpty()) return null

        return DiscoverPage(shortcuts = shortcuts, blocks = shelves)
    }

    // A big button of the top
    private fun shortcutOf(button: JSONObject): DiscoverShortcut? =
        DiscoverShortcut(
            title = button.stringAt("buttonText", "runs", 0, "text") ?: return null,
            icon = button.stringAt("iconStyle", "icon", "iconType"),
            browseId = button.stringAt("clickCommand", "browseEndpoint", "browseId") ?: return null
        )

    // A shelf as the kind of block its cards ask for
    private fun blockOf(shelf: JSONObject): DiscoverBlock? {
        val contents = shelf.arrayAt("contents").objects()
        val first = contents.firstOrNull() ?: return null

        // Moods and genres are buttons, they are not cards of the Home
        if (first.has("musicNavigationButtonRenderer")) return moodsOf(shelf, contents)

        val section = HomeParser.parseSections(JSONArray().put(JSONObject().put("musicCarouselShelfRenderer", shelf))).firstOrNull() ?: return null

        return when {
            first.has("musicResponsiveListItemRenderer") -> DiscoverBlock.Ranked(section)
            first.has("musicMultiRowListItemRenderer") -> DiscoverBlock.Wide(section)
            first.stringAt("musicTwoRowItemRenderer", "aspectRatio")?.contains(WideAspect) == true -> DiscoverBlock.Wide(section)
            else -> DiscoverBlock.Covers(section)
        }
    }

    // The moods and genres of a shelf, each with its color and the page it opens
    private fun moodsOf(shelf: JSONObject, contents: List<JSONObject>): DiscoverBlock? {
        val header = shelf.objectAt("header", "musicCarouselShelfBasicHeaderRenderer")
        val more = header?.objectAt("moreContentButton", "buttonRenderer", "navigationEndpoint", "browseEndpoint")

        val moods = contents.mapNotNull { item ->
            val button = item.objectAt("musicNavigationButtonRenderer") ?: return@mapNotNull null
            val browse = button.objectAt("clickCommand", "browseEndpoint") ?: return@mapNotNull null

            DiscoverMood(
                title = button.stringAt("buttonText", "runs", 0, "text") ?: return@mapNotNull null,
                color = button.objectAt("solid")?.optLong("leftStripeColor") ?: 0L,
                browseId = browse.stringAt("browseId") ?: return@mapNotNull null,
                params = browse.stringAt("params")
            )
        }
        if (moods.isEmpty()) return null

        return DiscoverBlock.Moods(
            title = header?.stringAt("title", "runs", 0, "text") ?: return null,
            link = more?.stringAt("browseId")?.let { HomeLink(browseId = it, params = more.stringAt("params"), isArtist = false) },
            moods = moods
        )
    }
}

package com.wavvy.app.features.discover.data

// JSON helpers of the YouTube Music answers
import com.wavvy.app.core.innertube.arrayAt
import com.wavvy.app.core.innertube.objectAt
import com.wavvy.app.core.innertube.objects
import com.wavvy.app.core.innertube.stringAt
// JSON
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLDecoder
import java.util.Base64
// Project resources
import com.wavvy.app.features.home.data.HomeItem
import com.wavvy.app.features.home.data.HomeItemKind
import com.wavvy.app.features.home.data.HomeLink
import com.wavvy.app.features.home.data.HomeSection
import com.wavvy.app.features.home.data.PageArtist
import com.wavvy.app.features.home.data.HomeParser

// What marks a card as wide, a video or an episode instead of a cover
private const val WideAspect = "RECTANGLE_16_9"

// The style of the title of a shelf on the pages that come from the Explore tab, which asks for two rows of covers
private const val DisplayTwo = "DISPLAY_TWO"
private const val TwoRows = 2

// A shelf with fewer cards than this stays in one row, as the three charts of videos do
private const val MinTwoRowCards = 4

// How the code of a country sits in the key of its line
private val CountryKey = Regex("country_menu_\\d+([A-Z]{2})")

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

    // A page opened from the Explore tab, such as the new releases, with its title and its blocks
    fun parseExplorePage(response: JSONObject): ExplorePage? {
        val title = response.stringAt("header", "musicHeaderRenderer", "title", "runs", 0, "text") ?: return null
        val contents = response.arrayAt("contents", "singleColumnBrowseResultsRenderer", "tabs").objects().firstOrNull()
            ?.arrayAt("tabRenderer", "content", "sectionListRenderer", "contents").objects() ?: return null

        val blocks = contents.mapNotNull { block ->
            block.objectAt("gridRenderer")?.let(::gridOf) ?: block.objectAt("musicCarouselShelfRenderer")?.let(::blockOf)
        }
        if (blocks.isEmpty()) return null

        // A page that is only a grid is a list of everything, a grid before shelves is the featured covers
        val only = blocks.singleOrNull() as? DiscoverBlock.Featured
        val list = if (only != null) listOf(DiscoverBlock.Grid(only.items, only.isWide)) else blocks
        return ExplorePage(title = title, blocks = list, countries = countriesOf(contents))
    }

    // The button that picks the country of the charts, with the countries of its list and the code each one is asked with
    private fun countriesOf(contents: List<JSONObject>): ChartCountries? {
        val button = contents.firstNotNullOfOrNull { it.objectAt("musicShelfRenderer") }?.arrayAt("subheaders").objects()
            .firstNotNullOfOrNull { it.objectAt("musicSideAlignedItemRenderer") }?.arrayAt("startItems").objects()
            .firstNotNullOfOrNull { it.objectAt("musicSortFilterButtonRenderer") } ?: return null
        val menu = button.objectAt("menu", "musicMultiSelectMenuRenderer") ?: return null

        val options = mutableListOf<ChartCountry?>()
        for (option in menu.arrayAt("options").objects()) {
            val item = option.objectAt("musicMultiSelectMenuItemRenderer")
            if (item == null) {
                // The line between the first countries and the rest has nothing in it
                if (option.has("musicMenuItemDividerRenderer")) options += null
                continue
            }

            options += ChartCountry(
                name = item.stringAt("title", "runs", 0, "text") ?: continue,
                code = countryCodeOf(item.stringAt("formItemEntityKey")) ?: continue,
                // The country that is already chosen has no command to choose it
                isSelected = !item.has("selectedCommand")
            )
        }
        if (options.isEmpty()) return null

        return ChartCountries(
            selected = button.stringAt("title", "runs", 0, "text") ?: return null,
            title = menu.stringAt("title", "musicMenuTitleRenderer", "primaryText", "runs", 0, "text").orEmpty(),
            options = options
        )
    }

    // The two letters of the country are written in the key of its line, after the name of the menu and a number
    private fun countryCodeOf(key: String?): String? {
        val bytes = runCatching { Base64.getDecoder().decode(URLDecoder.decode(key ?: return null, "UTF-8")) }.getOrNull() ?: return null
        return CountryKey.find(String(bytes, Charsets.ISO_8859_1))?.groupValues?.get(1)
    }

    // The cards of a grid, they are the featured ones when shelves follow
    private fun gridOf(grid: JSONObject): DiscoverBlock? {
        // A grid of buttons is the moods and genres, with the title of its group
        val buttons = grid.arrayAt("items").objects().mapNotNull(::moodOf)
        if (buttons.isNotEmpty()) {
            return DiscoverBlock.MoodGrid(grid.stringAt("header", "gridHeaderRenderer", "title", "runs", 0, "text").orEmpty(), buttons)
        }

        val entries = grid.arrayAt("items").objects()

        // A list of episodes is made of rows and every card of it is wide
        val episodes = entries.mapNotNull { it.objectAt("musicMultiRowListItemRenderer")?.let(HomeParser::parseEpisodeRow) }
        if (episodes.isNotEmpty()) return DiscoverBlock.Featured(episodes, isWide = true)

        val rows = entries.mapNotNull { it.objectAt("musicTwoRowItemRenderer") }
        val items = rows.mapNotNull { HomeParser.parseTwoRow(it) }

        return items.takeIf { it.isNotEmpty() }?.let { DiscoverBlock.Featured(it, isWide = rows.firstOrNull()?.stringAt("aspectRatio")?.contains(WideAspect) == true) }
    }

    // A big button of the top
    private fun shortcutOf(button: JSONObject): DiscoverShortcut? =
        DiscoverShortcut(
            title = button.stringAt("buttonText", "runs", 0, "text") ?: return null,
            icon = button.stringAt("iconStyle", "icon", "iconType"),
            browseId = button.stringAt("clickCommand", "browseEndpoint", "browseId") ?: return null
        )

    // A shelf as the kind of block its cards ask for
    internal fun blockOf(shelf: JSONObject): DiscoverBlock? {
        val contents = shelf.arrayAt("contents").objects()
        val first = contents.firstOrNull() ?: return null

        // Moods and genres are buttons, they are not cards of the Home
        if (first.has("musicNavigationButtonRenderer")) return moodsOf(shelf, contents)

        // A ranked list of artists, such as the top artists of the charts, is read here since the Home has no such rows
        if (first.has("musicResponsiveListItemRenderer") && isArtistRow(first.objectAt("musicResponsiveListItemRenderer"))) return artistsOf(shelf, contents)

        val section = HomeParser.parseSections(JSONArray().put(JSONObject().put("musicCarouselShelfRenderer", shelf))).firstOrNull() ?: return null

        return when {
            first.has("musicResponsiveListItemRenderer") -> DiscoverBlock.Ranked(section)
            first.has("musicMultiRowListItemRenderer") -> DiscoverBlock.Wide(section)
            first.stringAt("musicTwoRowItemRenderer", "aspectRatio")?.contains(WideAspect) == true -> DiscoverBlock.Wide(section)
            // The pages that come from the Explore tab put covers in two rows, the Explore tab itself keeps them in one
            else -> DiscoverBlock.Covers(section, rows = if (shelf.stringAt("header", "musicCarouselShelfBasicHeaderRenderer", "headerStyle")?.endsWith(DisplayTwo) == true && section.items.size >= MinTwoRowCards) TwoRows else 1)
        }
    }

    // True for a row that opens the page of an artist
    private fun isArtistRow(row: JSONObject?): Boolean {
        val browse = row?.objectAt("navigationEndpoint", "browseEndpoint") ?: return false
        return HomeParser.pageTypeOf(browse) == PageArtist
    }

    // The artists of a ranked shelf, each with its photo, how many subscribe to it and how it moved in the chart
    private fun artistsOf(shelf: JSONObject, contents: List<JSONObject>): DiscoverBlock? {
        val items = contents.mapNotNull { content ->
            val row = content.objectAt("musicResponsiveListItemRenderer") ?: return@mapNotNull null
            HomeItem(
                kind = HomeItemKind.Artist,
                id = row.stringAt("navigationEndpoint", "browseEndpoint", "browseId") ?: return@mapNotNull null,
                title = row.stringAt("flexColumns", 0, "musicResponsiveListItemFlexColumnRenderer", "text", "runs", 0, "text") ?: return@mapNotNull null,
                thumbnailUrl = HomeParser.coverOf(row.objectAt("thumbnail")),
                countText = row.stringAt("flexColumns", 1, "musicResponsiveListItemFlexColumnRenderer", "text", "runs", 0, "text"),
                trend = row.stringAt("customIndexColumn", "musicCustomIndexColumnRenderer", "icon", "iconType")
            )
        }
        if (items.isEmpty()) return null

        return DiscoverBlock.Ranked(
            HomeSection(
                title = shelf.stringAt("header", "musicCarouselShelfBasicHeaderRenderer", "title", "runs", 0, "text") ?: return null,
                label = null,
                thumbnailUrl = null,
                link = null,
                items = items
            )
        )
    }

    // A mood or a genre, with its color and the page it opens
    private fun moodOf(item: JSONObject): DiscoverMood? {
        val button = item.objectAt("musicNavigationButtonRenderer") ?: return null
        val browse = button.objectAt("clickCommand", "browseEndpoint") ?: return null

        return DiscoverMood(
            title = button.stringAt("buttonText", "runs", 0, "text") ?: return null,
            color = button.objectAt("solid")?.optLong("leftStripeColor") ?: 0L,
            browseId = browse.stringAt("browseId") ?: return null,
            params = browse.stringAt("params")
        )
    }

    // The moods and genres of a shelf, each with its color and the page it opens
    private fun moodsOf(shelf: JSONObject, contents: List<JSONObject>): DiscoverBlock? {
        val header = shelf.objectAt("header", "musicCarouselShelfBasicHeaderRenderer")
        val more = header?.objectAt("moreContentButton", "buttonRenderer", "navigationEndpoint", "browseEndpoint")

        val moods = contents.mapNotNull(::moodOf)
        if (moods.isEmpty()) return null

        return DiscoverBlock.Moods(
            title = header?.stringAt("title", "runs", 0, "text") ?: return null,
            link = more?.stringAt("browseId")?.let { HomeLink(browseId = it, params = more.stringAt("params"), isArtist = false) },
            moods = moods
        )
    }
}

package com.wavvy.app.features.discover.data

// Project resources
import com.wavvy.app.features.home.data.HomeItem
import com.wavvy.app.features.home.data.HomeLink
import com.wavvy.app.features.home.data.HomeSection

// One of the big buttons on top of the page, such as the new releases or the charts, the icon is the name YouTube Music gives it
data class DiscoverShortcut(
    val title: String,
    val icon: String?,
    val browseId: String
)

// A mood or a genre, with the color of the stripe on its side
data class DiscoverMood(
    val title: String,
    val color: Long,
    val browseId: String,
    val params: String?
)

// What a shelf of the page holds decides how it is drawn, covers in a row, wide cards, a ranked list of songs or moods and genres
sealed interface DiscoverBlock {
    // Covers in a row, or in two rows that slide together on the pages YouTube Music draws in that style
    data class Covers(val section: HomeSection, val rows: Int = 1) : DiscoverBlock

    // The first covers of a page, two by two with no title and no sliding
    data class Featured(val items: List<HomeItem>, val isWide: Boolean = false) : DiscoverBlock

    // The moods and genres of a page of their own, a title and the buttons in two columns
    data class MoodGrid(val title: String, val moods: List<DiscoverMood>) : DiscoverBlock

    // Everything of a list page, as covers in two columns or as wide cards in one
    data class Grid(val items: List<HomeItem>, val isWide: Boolean) : DiscoverBlock
    data class Wide(val section: HomeSection) : DiscoverBlock
    data class Ranked(val section: HomeSection) : DiscoverBlock
    data class Moods(val title: String, val link: HomeLink?, val moods: List<DiscoverMood>) : DiscoverBlock
}

// The page that YouTube Music calls Explore, the big buttons and the shelves in the order they come
data class DiscoverPage(
    val shortcuts: List<DiscoverShortcut>,
    val blocks: List<DiscoverBlock>
)

// A page that opens from the Explore tab, with the name it shows on top and what it holds in the order it comes
data class ExplorePage(
    val title: String,
    val blocks: List<DiscoverBlock>,
    // The country of the charts and the ones that can be chosen, only the charts have it
    val countries: ChartCountries? = null
)

// The choice of a country on top of the charts, the name on its button, the title of its list and the countries in the order YouTube Music sends them
// A null among the options is the line that separates the first ones from the rest
data class ChartCountries(
    val selected: String,
    val title: String,
    val options: List<ChartCountry?>
)

// A country of the charts, its code is what the page is asked with
data class ChartCountry(
    val name: String,
    val code: String,
    val isSelected: Boolean
)

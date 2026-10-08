package com.wavvy.app.features.discover.data

// Project resources
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
    data class Covers(val section: HomeSection) : DiscoverBlock
    data class Wide(val section: HomeSection) : DiscoverBlock
    data class Ranked(val section: HomeSection) : DiscoverBlock
    data class Moods(val title: String, val link: HomeLink?, val moods: List<DiscoverMood>) : DiscoverBlock
}

// The page that YouTube Music calls Explore, the big buttons and the shelves in the order they come
data class DiscoverPage(
    val shortcuts: List<DiscoverShortcut>,
    val blocks: List<DiscoverBlock>
)

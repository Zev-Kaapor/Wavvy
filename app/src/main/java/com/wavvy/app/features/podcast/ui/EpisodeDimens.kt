package com.wavvy.app.features.podcast.ui

// UI styling and utilities
import androidx.compose.ui.unit.dp

// Sizes of the page of an episode as YouTube Music draws it, in the scale of the page of a podcast
object EpisodeDimens {
    // How far the title in the bar waits, so it comes in when the name of the episode is mostly gone
    val BarScrollThreshold = 160.dp

    // The picture with the length under it, and the name under the length
    val PictureTop = 24.dp
    val PictureWidth = 168.dp
    val PictureCorner = 6.dp
    val LengthTop = 12.dp
    val TitleTop = 12.dp

    // The play button in the middle of the buttons
    val PlaySize = 56.dp
    val PlayIcon = 28.dp

    // The numbers, their room above and under, their height and corners
    val NumbersTop = 16.dp
    val NumbersBottom = 16.dp
    val TileHeight = 52.dp
    val TileCorner = 12.dp
}

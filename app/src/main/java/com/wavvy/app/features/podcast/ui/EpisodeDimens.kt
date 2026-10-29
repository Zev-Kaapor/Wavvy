package com.wavvy.app.features.podcast.ui

// UI styling and utilities
import androidx.compose.ui.unit.dp

// Sizes of the page of an episode as YouTube Music draws it, in the scale of the page of a podcast
object EpisodeDimens {
    // How far the title in the bar waits, so it comes in when the name of the episode is mostly gone
    val BarScrollThreshold = 160.dp

    // The picture, the widest it gets and its corners, and the room above the name
    val PictureTop = 16.dp
    val PictureMaxWidth = 360.dp
    val PictureCorner = 8.dp

    // The picture lying down, smaller and at the end of the page
    val LandscapePictureWidth = 240.dp
    val TitleTop = 16.dp

    // The line of who makes the podcast, its corners, and the bar of how much was heard
    val ByCorner = 8.dp
    val ProgressTop = 12.dp
    val ProgressWidth = 160.dp
    val ProgressHeight = 4.dp

    // The room inside the pill that has the time that is left, how often it ticks and how often the page is asked again
    val CountdownPaddingX = 24.dp
    const val CountdownTickMillis = 1000L
    const val RefreshMillis = 30_000L

    // The description, how many lines show before the word that opens the rest, and the room above that word
    const val DescriptionLines = 5
    val MoreTop = 4.dp

    // The other episodes of the podcast, how many show and the room above their title
    const val MoreEpisodes = 5
    val SectionTop = 8.dp

    // How much of the width the line of the numbers takes in the placeholder
    const val SkeletonNumbersFraction = 0.7f
}

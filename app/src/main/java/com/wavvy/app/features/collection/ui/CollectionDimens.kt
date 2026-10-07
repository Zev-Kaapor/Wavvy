package com.wavvy.app.features.collection.ui

// UI styling and utilities
import androidx.compose.ui.unit.dp

// Sizes and limits of the page of an album or of a playlist, the rows of songs are the ones of the Home
object CollectionDimens {
    // Cover of the page, how much of the width it takes at most, its largest size and its corners
    const val CoverWidthFraction = 0.55f
    val CoverMaxSize = 280.dp
    val CoverCorner = 8.dp
    val MenuCoverCorner = 4.dp

    // Blurred cover behind the top of the page, how much of the height it covers and how dark its top is, so the arrow can be seen
    const val BackdropFraction = 0.65f
    const val BackdropTopShade = 0.35f

    // Where the name of the shelf starts on the top bar, after the arrow
    val TopTitleStart = 56.dp

    // Photo of who made it on top of the page, and the space between it and the name
    val TopAvatar = 24.dp
    val TopAvatarGap = 8.dp

    // Space between the cover, the texts and the buttons
    val HeaderGap = 16.dp
    val TextGap = 4.dp

    // Round buttons under the title, the small ones and the play button in the middle, their icons and the space between them
    val ActionSize = 52.dp
    val ActionIcon = 24.dp
    val PlaySize = 72.dp
    val PlayIcon = 36.dp
    val ActionGap = 16.dp

    // Lines of the description before it is opened
    const val DescriptionLines = 3

    // Room around the line with the number of songs at the end of the list
    val FooterPadding = 24.dp

    // Placeholder of the header, the lines of text under the cover and their widths
    val SkeletonLine = 14.dp
    const val SkeletonTitleFraction = 0.5f
    const val SkeletonSubtitleFraction = 0.3f

    // Spinner at the end of a long playlist that is asking for more
    val MoreSpinner = 28.dp
    val MorePadding = 16.dp

    // How many rows from the end the next songs are asked for, and how many pages are asked at most to play a whole playlist
    const val LoadMoreThreshold = 6
    const val MaxPages = 10
}

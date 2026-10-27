package com.wavvy.app.features.collection.ui

// UI styling and utilities
import androidx.compose.ui.unit.dp

// Sizes and limits of the page of an album or of a playlist, the rows of songs are the ones of the Home
object CollectionDimens {
    // How far under the status bar the mark of the pulled page shows, which is under the bar of the page
    val RefreshTop = 56.dp

    // Cover of the page, how much of the width it takes at most, its largest size and its corners
    const val CoverWidthFraction = 0.55f
    val CoverMaxSize = 280.dp
    val CoverCorner = 8.dp

    // Blurred cover behind the top of the page, how much of the height it covers and how dark its top is, so the arrow can be seen
    // The color holds until this share of it and then fades into the page
    const val BackdropFraction = 0.65f
    const val BackdropTopShade = 0.15f
    const val BackdropHold = 0.4f

    // On the light theme the cover is a soft tint over the page and nothing dark is laid over it, so the arrow and the texts stay readable
    const val BackdropLightAlpha = 0.4f

    // How far the page scrolls, as a share of the height of the screen, before the color of the cover has faded into the page
    const val BackdropFadeFraction = 0.45f

    // Room under the last row, so the mini player and the bar never cover it
    val ListBottom = 24.dp

    // Where the name of the shelf starts on the top bar, after the arrow
    val TopTitleStart = 56.dp

    // Photo of who made it on top of the page, and the space between it and the name
    val TopAvatar = 20.dp
    val TopAvatarGap = 8.dp

    // The photos of the artists of an album stack, each one over the half of the one before, how many show at most and the ring that parts them
    const val StackMax = 3
    val StackRing = 2.dp

    // The gap between "By" and the photos of who made the page
    val ByGap = 4.dp

    // Lying down the cover is small and stands at the end, with the texts and the buttons at the start, and the space between them
    val LandscapeCover = 150.dp
    val LandscapeGap = 24.dp

    // Space between the cover, the texts and the buttons
    val HeaderGap = 16.dp
    val TextGap = 4.dp

    // Round buttons under the title, the small ones and the play button in the middle, their icons and the space between them
    val ActionSize = 48.dp
    val ActionIcon = 24.dp
    val PlaySize = 56.dp
    val PlayIcon = 32.dp
    val ActionGap = 16.dp
    val ProgressStroke = 2.dp

    // Space under the round buttons, before the songs
    val ActionsBottom = 8.dp

    // Lines of the description before it is opened
    const val DescriptionLines = 3

    // How many songs a page needs before the filters and the search show, a few songs have nothing to filter
    const val FilterMinSongs = 8

    // Placeholder of the header, the lines of text under the cover and their widths
    val SkeletonLine = 14.dp
    const val SkeletonTitleFraction = 0.5f

    const val SkeletonDetailsFraction = 0.45f
    val SkeletonBy = 24.dp
    val SkeletonTitle = 26.dp
    val SkeletonOwner = 120.dp

    // Spinner at the end of a long playlist that is asking for more
    val MoreSpinner = 28.dp
    val MorePadding = 16.dp

    // How many rows from the end the next songs are asked for, and how many pages are asked at most to play a whole playlist
    const val LoadMoreThreshold = 6
    const val MaxPages = 10
}

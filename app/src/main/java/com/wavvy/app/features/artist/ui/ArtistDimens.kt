package com.wavvy.app.features.artist.ui

// UI styling and utilities
import androidx.compose.ui.unit.dp

// Sizes and limits of the page of an artist, the shelves are the ones of the Home
object ArtistDimens {
    // Height of the picture on top as a share of the width, and how much of its bottom fades into the background
    const val HeroHeightFraction = 0.9f
    const val HeroFadeFraction = 0.6f

    // How dark the top of the picture is, so the arrow can be seen over any picture, and where that shade ends
    const val HeroTopShade = 0.5f
    const val HeroTopShadeEnd = 0.3f

    // Size in pixels asked for the picture, the share of the width of the hero keeps it from being stretched
    const val HeroRequestWidth = 1080
    const val HeroRequestHeight = 972

    // Round buttons of the page, the mix and the play button, their icons and the space between them
    val MixSize = 40.dp
    val MixIcon = 22.dp
    val PlaySize = 52.dp
    val PlayIcon = 28.dp
    val ActionGap = 12.dp

    // Subscribe button, its height, the space inside it and between its words and the count
    val SubscribeHeight = 40.dp
    val SubscribePadding = 16.dp
    val SubscribeGap = 6.dp

    // Card of the release the artist is promoting, its cover, corners and the space around it
    val FeaturedCover = 44.dp
    val FeaturedCorner = 10.dp
    val FeaturedPadding = 6.dp
    val FeaturedGap = 10.dp

    // Top bar that gets a background when the page scrolls, how far the page has to scroll for it and how long the change takes
    val BarScrollThreshold = 200.dp
    const val BarFadeMillis = 200

    // Button that translates the description and its icon
    val TranslateButton = 40.dp
    val TranslateIcon = 20.dp

    // Words that show the rest of the description, and the room around them
    val ShowMorePadding = 4.dp

    // Lines of the description before it is opened, and the space around it under the name
    const val DescriptionLines = 3
    val DescriptionGap = 4.dp

    // Room around the block at the end of the page, between its lines and between its groups, the width of the names of its lines and the space between chips
    val AboutPadding = 16.dp
    val AboutGap = 6.dp
    val AboutGroupGap = 12.dp
    val AboutLabelWidth = 104.dp

    // Chips of the genres and of the links, the space between them, their corners, border and the space inside them
    val ChipGap = 6.dp
    val ChipCorner = 8.dp
    val ChipBorder = 1.dp
    val ChipPaddingHorizontal = 10.dp
    val ChipPaddingVertical = 4.dp

    // Room kept between the end of the page and the mini player
    val MiniPlayerClearance = 30.dp

    // Placeholder of the page while it loads, the name and the listeners over the picture, the lines of the description,
    // the subscribe button, and the rows of songs, with the share of the width each one takes
    val SkeletonName = 36.dp
    val SkeletonLine = 14.dp
    const val SkeletonNameFraction = 0.55f
    const val SkeletonListenersFraction = 0.35f
    val SkeletonDescriptionLines = listOf(1f, 0.95f, 0.6f)
    val SkeletonSubscribeWidth = 130.dp
    const val SkeletonSongRows = 4
    const val SkeletonRowTitleFraction = 0.6f
    const val SkeletonRowSubtitleFraction = 0.35f
}

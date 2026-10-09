package com.wavvy.app.features.discover.ui

// UI styling and utilities
import androidx.compose.ui.unit.dp

// Sizes of the Discover tab as the Explore page of YouTube Music has them, measured on the screen of a phone
object DiscoverDimens {
    // The space at the sides of the page and between the cards of a row, the same all over the page
    val Side = 16.dp
    val Gap = 16.dp

    // The big buttons of the top, their height, corners, the room inside and their icon
    val ShortcutHeight = 87.dp
    val ShortcutCorner = 16.dp
    val ShortcutPadding = 16.dp
    val ShortcutIcon = 20.dp

    // Room kept between the end of the page and the mini player
    val MiniPlayerClearance = 30.dp

    // Title of a shelf and the room around it, and its arrow
    val TitleVertical = 14.dp
    val TitleArrow = 28.dp

    // Covers in a row, two whole ones and a bit of the next, so what is left of the width is shared after the sides and the gaps
    const val CoverColumns = 2
    const val CoverGapsAroundCards = 4
    val CoverCorner = 4.dp
    val CardTextGap = 8.dp

    // Moods and genres, how many rows, their height, corners and the stripe on their side
    const val MoodRows = 3
    val MoodHeight = 48.dp
    val MoodCorner = 12.dp
    val MoodStripe = 5.dp

    // Wide cards of videos and episodes, what is taken from the width for the side, the gap and the bit of the next card, and the corners
    val WideRemainder = 48.dp
    val WideCorner = 6.dp

    // Covers in two rows, the room under a cover for its title and its line, and the room between the rows and between the blocks of a page
    val CoverTextHeight = 46.dp
    val CoverRowGap = 32.dp
    val BlockGap = 24.dp

    // The length of an episode on the end of its picture, its distance from the corner, the room around the words, its corners and how dark its back is
    val LengthInset = 8.dp
    val LengthPaddingX = 6.dp
    val LengthPaddingY = 2.dp
    val LengthCorner = 4.dp
    const val LengthAlpha = 0.6f

    // The play mark on the cover of a playlist, its size, its distance from the corner and how dark its back is
    val PlayBadge = 22.dp
    val PlayBadgeInset = 8.dp
    val PlayBadgeIcon = 14.dp
    const val PlayBadgeAlpha = 0.55f

    // The bar of a page, its height, the room under the big title and the distance from the top of the page to the title
    val BarHeight = 56.dp
    // The first line of the title keeps its place as the lines sit closer, so the room above it grows by half of what they lost
    val PageTitleTop = 5.dp
    val PageTitleBottom = 33.dp

    // The room under the title when what follows is a title of a group, which has room of its own, and the room between two groups of buttons
    val PageTitleBottomBeforeGroup = 12.dp
    val MoodGroupGap = 38.dp

    // How wide the big title may be before its words go to the next line, as it is on the page of the moods and genres
    val PageTitleMaxWidth = 174.dp
    val BarScrollThreshold = 56.dp
    const val BarFadeMillis = 200

    // The room a page keeps for its spinner or its message while it loads, and the room between the rows of a list of covers
    val LoadingHeight = 360.dp
    val GridRowGap = 24.dp

    // The button that picks the country, its least width, its corners, and the rows of the list of countries with the room of the check
    val CountryButtonMinWidth = 188.dp
    val CountryButtonHeight = 36.dp
    val CountryButtonCorner = 18.dp
    val CountryChevron = 24.dp
    val CountryRowHeight = 48.dp
    val CountryCheckWidth = 47.dp
    val TrendMark = 8.dp

    // Ranked list, how many songs make a column, the height of a row, the width of the rank and of the picture, and the corners of the picture
    const val RankedRows = 4
    val RankedRowHeight = 72.dp
    val RankWidth = 52.dp
    val RankedPicture = 56.dp
    val RankedCorner = 4.dp
}

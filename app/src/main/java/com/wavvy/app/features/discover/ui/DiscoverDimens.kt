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

    // Ranked list, how many songs make a column, the height of a row, the width of the rank and of the picture, and the corners of the picture
    const val RankedRows = 4
    val RankedRowHeight = 72.dp
    val RankWidth = 52.dp
    val RankedPicture = 56.dp
    val RankedCorner = 4.dp
}

package com.wavvy.app.features.history.ui

// UI styling and utilities
import androidx.compose.ui.unit.dp

// Sizes of the history, the rows of songs as the other lists of the app have them
object HistoryDimens {
    // The room at the sides and the name of a day
    val Side = 16.dp
    val DayVertical = 12.dp

    // The filters under the bar, as the buttons of the top of the library are
    val ChipHeight = 31.dp
    val ChipCorner = 8.dp
    val ChipGap = 8.dp
    val ChipPaddingX = 14.dp
    val ChipsVertical = 10.dp

    // How many songs of a day show before the arrow that shows all of them
    const val PageSize = 10
    val MoreHeight = 48.dp

    // A row, its height, its cover, its corners and the room between the cover and the words
    val RowHeight = 64.dp
    val Cover = 48.dp
    val CoverCorner = 4.dp
    val Gap = 16.dp
}

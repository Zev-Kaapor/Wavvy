package com.wavvy.app.features.library.ui

// UI styling and utilities
import androidx.compose.ui.unit.dp

// Sizes of the library as YouTube Music draws it, measured on the screen of a phone, in the scale of the other pages
object LibraryDimens {
    // The room at the sides, the head and the buttons under it
    val Side = 16.dp
    val HeaderTop = 8.dp
    val ChipHeight = 31.dp
    val ChipCorner = 8.dp
    val ChipGap = 8.dp
    val ChipPaddingX = 14.dp
    val ChipsVertical = 10.dp
    val ClearWidth = 44.dp
    val ClearIcon = 20.dp

    // How far under the status bar the mark of the pulled list shows, which is under the header and the buttons
    val RefreshTop = 120.dp

    // How long the search takes to open and to close
    const val SearchMillis = 250

    // The line of the order and of how the list is drawn
    val SortHeight = 40.dp
    val SortArrow = 22.dp
    val ViewIcon = 24.dp

    // A row of the list, the cover, the room between its parts and the three dots
    val RowHeight = 71.dp
    val RowCover = 54.dp
    val RowCoverCorner = 4.dp
    val RowGap = 16.dp

    // A cover of the grid, two by two, and the room between them
    const val GridColumns = 2
    val GridGap = 16.dp
    val GridCover = 176.dp
    val GridTextTop = 8.dp
    val GridRowGap = 20.dp

    // The button that makes something new, its height, the icon and the room from the edges
    val ActionHeight = 47.dp
    val ActionIcon = 24.dp
    val ActionPaddingX = 18.dp
    val ActionEnd = 24.dp
    val ActionBottom = 24.dp
    val ActionIconGap = 12.dp
    const val ActionMillis = 200

    // What shows when a list is empty, the icon, the room around the words and the button under them
    val EmptyIcon = 40.dp
    val EmptyGap = 16.dp
    val EmptyButtonHeight = 47.dp
    val EmptyButtonPaddingX = 28.dp

    // The loading rows and the room kept under the list for the mini player and the button
    val SkeletonRows = 6
    val ListBottom = 96.dp

    // How near the end of the list the next page is asked
    const val LoadMoreDistance = 6
}

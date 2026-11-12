package com.wavvy.app.features.playlist.ui

// UI styling and utilities
import androidx.compose.ui.unit.dp

// Sizes of the sheets that save to a playlist and that make a new one
object PlaylistDimens {
    // A playlist of the list, its cover and the room between its parts
    val RowCover = 48.dp
    val RowCoverCorner = 4.dp
    val RowGap = 16.dp
    val RowBadge = 12.dp
    val RowBadgePadding = 3.dp
    val RowBadgeInset = 2.dp
    val RowBadgeCorner = 4.dp
    const val RowBadgeAlpha = 0.55f
    const val SkeletonRows = 6
    const val ParallelReads = 3
    const val BusyAlpha = 0.6f
    val CountLine = 20.dp
    val CountPlaceholderWidth = 72.dp
    const val SearchDelayMillis = 400L

    // The lines of the description
    const val DescriptionLines = 4

    // The cover in the shape of YouTube Music, its corners, the dashed square that shows the cut of the app and the line under them
    val CoverCorner = 16.dp
    val CoverSquareCorner = 8.dp
    val CoverStroke = 2.dp
    val CoverDash = 6.dp
    val CoverNoteInset = 8.dp
    val CoverNotePaddingX = 8.dp
    val CoverNotePaddingY = 4.dp
    val CoverCamera = 48.dp
    const val CoverCameraAlpha = 0.55f

    // The words that say why the list is not there
    val MessageVertical = 24.dp
}

package com.wavvy.app.features.playlist.ui

// UI styling and utilities
import androidx.compose.ui.unit.dp

// Sizes of the sheets that save to a playlist and that make a new one
object PlaylistDimens {
    // A playlist of the list, its cover, the room between its parts and the most the list grows before it scrolls
    val RowHeight = 64.dp
    val RowCover = 48.dp
    val RowCoverCorner = 4.dp
    val RowGap = 16.dp
    val RowBadge = 12.dp
    val RowBadgePadding = 3.dp
    val RowBadgeInset = 2.dp
    val RowBadgeCorner = 4.dp
    const val RowBadgeAlpha = 0.55f
    val ListMaxHeight = 420.dp
    const val SkeletonRows = 6
    const val ParallelReads = 3
    const val BusyAlpha = 0.6f
    val CountLine = 20.dp
    val CountPlaceholderWidth = 72.dp
    const val SearchDelayMillis = 400L

    // The button that makes a new playlist, above the list
    val NewHeight = 40.dp
    val NewPaddingX = 16.dp
    val NewIconGap = 8.dp
    val NewVertical = 12.dp

    // The field of the name, the lines of who can find it and the button that makes it
    val FieldVertical = 8.dp
    const val DescriptionLines = 4
    val PrivacyRowHeight = 56.dp
    val CreateHeight = 44.dp
    val CreatePaddingX = 24.dp
    val CreateTop = 16.dp

    // The cover in the shape of YouTube Music, its corners, the dashed square that shows the cut of the app and the line under them
    val CoverCorner = 12.dp
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

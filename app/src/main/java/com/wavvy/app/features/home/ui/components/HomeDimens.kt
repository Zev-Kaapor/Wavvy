package com.wavvy.app.features.home.ui.components

// UI styling and utilities
import androidx.compose.ui.unit.dp

// Sizes of the Home header, as in the old Wavvy
object HomeDimens {
    // Height of the isologo, its width follows the proportion of the image
    val LogoHeight = 24.dp

    // Round profile button
    val ProfileButton = 40.dp

    // Bell icon without a background
    val BellIcon = 26.dp

    // Filter placeholders, widths vary so they look like real chips
    val FilterHeight = 32.dp
    val FilterPlaceholderWidths = listOf(72.dp, 64.dp, 96.dp, 80.dp, 72.dp)

    // Shelf placeholders, how many shelves and covers they have, enough to fill wide screens
    const val SkeletonShelves = 3
    const val SkeletonCovers = 8

    // Title bar of a shelf, and the two text lines under each cover
    val ShelfTitleWidth = 180.dp
    val ShelfTitleHeight = 24.dp
    val CoverLineHeight = 14.dp
    const val CoverTitleFraction = 0.8f
    const val CoverSubtitleFraction = 0.5f
}

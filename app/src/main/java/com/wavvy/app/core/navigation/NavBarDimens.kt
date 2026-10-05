package com.wavvy.app.core.navigation

// UI styling and utilities
import androidx.compose.ui.unit.dp

// Sizes of the navigation bar and of the side rail used in landscape
object NavBarDimens {
    // Least height of the bar without the system bars, as in the old Wavvy, it grows with the system font size
    val Height = 85.dp

    // Widest the group of items gets, so the icons stay together on wide screens
    val GroupMaxWidth = 340.dp

    // Size of the icon of an item when selected
    val IconSize = 24.dp

    // Width of the area with the icons, and of the whole rail when there is no camera cutout
    val RailIconAreaWidth = 80.dp
    val RailDefaultWidth = 92.dp

    // How much of the cutout the rail does not add to its width, tuned by eye on the old Wavvy
    val RailCameraReduction = 16.dp

    // Height of each item and the space between them
    val RailItemHeight = 56.dp
    val RailItemSpacing = 8.dp

    // No inset
    val NoInset = 0.dp
}

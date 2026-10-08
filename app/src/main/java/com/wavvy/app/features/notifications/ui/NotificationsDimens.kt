package com.wavvy.app.features.notifications.ui

// UI styling and utilities
import androidx.compose.ui.unit.dp

// Sizes of the screen of notifications as the Activity page of YouTube Music has them
object NotificationsDimens {
    // Round photo of the artist on the left, and the cover of the release on the right with its corners
    val Avatar = 56.dp
    val Cover = 62.dp
    val CoverCorner = 4.dp

    // Room around a row and between its parts
    val RowVertical = 12.dp
    val Gap = 16.dp

    // The three dots at the end of a row
    val More = 32.dp
}

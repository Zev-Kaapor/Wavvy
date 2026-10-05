package com.wavvy.app.features.home.ui.components

// UI styling and utilities
import androidx.compose.ui.unit.dp

// Sizes of the Home header, as in the old Wavvy
object HomeDimens {
    // Height of the isologo, its width follows the proportion of the image
    val LogoHeight = 24.dp

    // Round profile button and the icon inside it
    val ProfileButton = 40.dp
    val ProfileIcon = 20.dp

    // Gradient ring around the profile button and the gap between the ring and the circle
    val ProfileRing = 2.dp
    val ProfileRingGap = 2.dp

    // How opaque the profile circle is over the background
    const val ProfileContainerAlpha = 0.7f

    // Bell icon without a background
    val BellIcon = 26.dp

    // Filter placeholders, widths vary so they look like real chips
    val FilterHeight = 32.dp
    val FilterPlaceholderWidths = listOf(72.dp, 64.dp, 96.dp, 80.dp, 72.dp)
}

package com.wavvy.app.features.profile.ui

// UI styling and utilities
import androidx.compose.ui.unit.dp

// Sizes of the profile avatar and of the profile menu
object ProfileDimens {
    // Gradient ring around the avatar, the gap between the ring and the photo, and the icon as a share of the avatar
    val AvatarRing = 2.dp
    val AvatarRingGap = 2.dp
    const val AvatarIconFraction = 0.5f

    // How opaque the circle behind the photo is
    const val AvatarContainerAlpha = 0.7f

    // Avatar in the header of the menu
    val MenuAvatar = 56.dp

    // Widest the menu gets, so it does not stretch on wide screens
    val MenuMaxWidth = 560.dp

    // Small bar at the top of the menu that shows it can be dragged, and how far it is dragged to close
    val HandleWidth = 32.dp
    val HandleHeight = 4.dp
    val DismissDistance = 96.dp

    // How dark the screen gets behind the menu
    const val ScrimAlpha = 0.5f

    // Icon of the action at the end of the header, and of each item of the list with its height
    val ActionIcon = 24.dp
    val ItemIcon = 24.dp
    val ItemHeight = 56.dp
}

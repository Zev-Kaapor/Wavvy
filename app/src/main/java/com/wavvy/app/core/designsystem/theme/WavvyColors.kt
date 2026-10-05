package com.wavvy.app.core.designsystem.theme

// Compose state and runtime
import androidx.compose.runtime.Immutable
// UI styling and utilities
import androidx.compose.ui.graphics.Color

// Colors that depend on the theme but are not part of the Material color scheme
@Immutable
data class WavvyColors(
    // Text and icons over images and gradients
    val onMedia: Color,
    // Dark fade under text over images
    val mediaScrim: Color,
    // Filled accent controls, darker than the accent so the white text stays readable
    val accentFill: Color,
    val onAccentFill: Color,
    // Surface for bars and sheets that sit above the background
    val elevated: Color,
    // Background of the bottom navigation bar and of the side rail
    val navBar: Color,
    // Color the loading placeholders pulse with
    val skeletonPulse: Color,
    // Filter chips and the icon of an unselected navigation item
    val chipContainer: Color,
    val navUnselected: Color,
    // Icon and name of the selected navigation item
    val navSelected: Color,
    // Soft glow at the top of full screens, blending from one side to the other
    val glowStart: Color,
    val glowEnd: Color,
    // Gradient ring around the profile button, blue to violet to red
    val ringStart: Color,
    val ringMiddle: Color,
    val ringEnd: Color
)

// Extra colors of the dark theme
val DarkWavvyColors = WavvyColors(
    onMedia = OnFilled,
    mediaScrim = Color(0xCC000000),
    accentFill = Color(0xFF0070E0),
    onAccentFill = OnFilled,
    elevated = DarkContainerHigh,
    navBar = Color(0xFF0C0C12),
    skeletonPulse = Color(0xFF2E2E2E),
    chipContainer = Color(0x0FF2F2F2),
    navUnselected = Color(0xFF676D75),
    navSelected = Color(0xFF539DF3),
    glowStart = Color(0x380081FF),
    glowEnd = Color(0x388D44FF),
    ringStart = DarkAccent,
    ringMiddle = Color(0xFF9B72CB),
    ringEnd = Color(0xFFD96570)
)

// Extra colors of the light theme
val LightWavvyColors = WavvyColors(
    onMedia = OnFilled,
    mediaScrim = Color(0x99000000),
    accentFill = LightAccent,
    onAccentFill = OnFilled,
    elevated = Color(0xFFE4E4E4),
    navBar = Color(0xFFFFFFFF),
    skeletonPulse = Color(0xFFCDCDCD),
    chipContainer = Color(0x0F121212),
    navUnselected = Color(0xFF484C52),
    navSelected = Color(0xFF539DF3),
    glowStart = Color(0x240560D6),
    glowEnd = Color(0x248D44FF),
    ringStart = LightAccent,
    ringMiddle = Color(0xFF9B72CB),
    ringEnd = Color(0xFFD96570)
)

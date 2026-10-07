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
    // Top bar of the Google sign in page, over the cutout and with the back button
    val loginBar: Color,
    // Background of the bottom navigation bar and of the side rail
    val navBar: Color,
    // Round play button over a cover, and the dark that fades over the covers of the speed dial
    val playButton: Color,
    val tileScrim: Color,
    // Ring of the song progress around the cover of the mini player, its track, and the cover shown while there is no picture
    val playerRing: Color,
    val playerRingTrack: Color,
    val coverPlaceholder: Color,
    // Cyan of the expanded player, the red of a liked song, and the shadow under text over pictures
    val playerAccent: Color,
    val playerLiked: Color,
    val textShadow: Color,
    // Camera that marks the cover of a video
    val videoBadge: Color,
    // Push pin that marks what is pinned to the speed dial
    val pinBadge: Color,
    // Loading placeholders and the light that sweeps over them
    val skeleton: Color,
    val skeletonHighlight: Color,
    // Icon of an unselected navigation item
    val navUnselected: Color,
    // Icon and name of the selected navigation item
    val navSelected: Color,
    // Soft glow at the top of full screens, blending from one side to the other
    val glowStart: Color,
    val glowEnd: Color,
    // Brand gradient from blue to violet to red, on the profile ring
    val gradientStart: Color,
    val gradientMiddle: Color,
    val gradientEnd: Color,
    // Main buttons, blue fading to a darker blue
    val buttonStart: Color,
    val buttonEnd: Color
)

// Extra colors of the dark theme
val DarkWavvyColors = WavvyColors(
    onMedia = OnFilled,
    mediaScrim = Color(0xCC000000),
    accentFill = Color(0xFF0070E0),
    onAccentFill = OnFilled,
    elevated = DarkContainerHigh,
    playButton = Color(0x99000000),
    tileScrim = Color(0xFF000000),
    playerRing = Color(0xFF00B2FF),
    playerRingTrack = Color(0x26FAFAFC),
    coverPlaceholder = Color(0xFF0C0C12),
    playerAccent = Color(0xFF00B2FF),
    playerLiked = Color(0xFFFF2D55),
    textShadow = Color(0xFF000000),
    videoBadge = Color(0xFFFF0000),
    pinBadge = Color(0xFFFF0000),
    loginBar = Color(0xFF0E0E0E),
    navBar = Color(0xFF0C0C12),
    skeleton = Color(0xFF2E2E2E),
    skeletonHighlight = Color(0xFF4A4A4A),
    navUnselected = Color(0xFF676D75),
    navSelected = Color(0xFF539DF3),
    glowStart = Color(0x380081FF),
    glowEnd = Color(0x388D44FF),
    gradientStart = DarkAccent,
    gradientMiddle = Color(0xFF9B72CB),
    gradientEnd = Color(0xFFD96570),
    buttonStart = DarkAccent,
    buttonEnd = Color(0xFF004FB3)
)

// Extra colors of the light theme
val LightWavvyColors = WavvyColors(
    onMedia = OnFilled,
    mediaScrim = Color(0x99000000),
    accentFill = LightAccent,
    onAccentFill = OnFilled,
    elevated = Color(0xFFE4E4E4),
    playButton = Color(0x99000000),
    tileScrim = Color(0xFF000000),
    playerRing = Color(0xFF000000),
    playerRingTrack = Color(0x1A000000),
    coverPlaceholder = Color(0xFF0C0C12),
    playerAccent = Color(0xFF0088CC),
    playerLiked = Color(0xFFFF2D55),
    textShadow = Color(0xFF000000),
    videoBadge = Color(0xFFFF0000),
    pinBadge = Color(0xFFFF0000),
    loginBar = Color(0xFFFFFFFF),
    navBar = Color(0xFFFFFFFF),
    skeleton = Color(0xFFCDCDCD),
    skeletonHighlight = Color(0xFFF2F2F2),
    navUnselected = Color(0xFF484C52),
    navSelected = Color(0xFF539DF3),
    glowStart = Color(0x240560D6),
    glowEnd = Color(0x248D44FF),
    gradientStart = LightAccent,
    gradientMiddle = Color(0xFF9B72CB),
    gradientEnd = Color(0xFFD96570),
    buttonStart = LightAccent,
    buttonEnd = Color(0xFF03448F)
)

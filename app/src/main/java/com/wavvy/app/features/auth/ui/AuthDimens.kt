package com.wavvy.app.features.auth.ui

// UI styling and utilities
import androidx.compose.ui.unit.dp

// Sizes of the welcome and login screens
object AuthDimens {
    // Share of the screen height the photo covers in portrait, and where its fade into the background starts in landscape
    const val PhotoHeightFraction = 0.75f
    const val PhotoFadeStart = 0.35f

    // Where the fade of the photo starts and ends in portrait, as a share of the screen height, ending at the top of the isologo
    const val PortraitFadeStart = 0.3f
    const val PortraitFadeEnd = 0.74f

    // Points of the fade in portrait, they ease it in and out so it does not show where it starts
    const val PhotoFadeSteps = 5

    // Vertical position the photo is cropped around, so the face stays in view in landscape
    const val WelcomePhotoFocusBias = -0.25f

    // Height of the isologo on the welcome
    val LogoHeight = 24.dp

    // Round back button
    val BackButton = 40.dp

    // White badge with the Google logo inside the sign in button, and the logo inside the badge
    val GoogleBadge = 32.dp
    val GoogleLogo = 18.dp

    // Icon of the continue without an account button, kept in the same area as the Google badge
    val GuestIcon = 20.dp

    // Widest the texts and buttons get, so they do not stretch on wide screens
    val ContentMaxWidth = 320.dp
}

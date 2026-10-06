package com.wavvy.app.features.profile.ui

// Compose state and graphics
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.ImageBitmap

// Who is using the app, the photo and the names are empty for a guest
data class Profile(
    val photo: ImageBitmap?,
    val name: String?,
    val handle: String?,
    val isSignedIn: Boolean
)

// Profile of the person using the app
val LocalProfile = staticCompositionLocalOf { Profile(photo = null, name = null, handle = null, isSignedIn = false) }

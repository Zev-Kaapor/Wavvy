package com.wavvy.app.features.auth.ui

// Compose state and graphics
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.ImageBitmap

// Profile photo of the signed in account, empty when there is none
val LocalProfilePhoto = staticCompositionLocalOf<ImageBitmap?> { null }

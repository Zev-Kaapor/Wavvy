package com.wavvy.app.core.navigation

// Compose graphics and resources
import androidx.annotation.StringRes
import androidx.compose.ui.graphics.vector.ImageVector
// Project resources
import com.wavvy.app.R
import com.wavvy.app.core.designsystem.icons.WavvyIcons

// Main destinations shown in the navigation bar, in this order
enum class MainTab(
    val route: String,
    val icon: ImageVector,
    @StringRes val labelRes: Int
) {
    HOME("home", WavvyIcons.Home, R.string.nav_home),
    EXPLORE("explore", WavvyIcons.Explore, R.string.nav_explore),
    DISCOVER("discover", WavvyIcons.Discover, R.string.nav_discover),
    LIBRARY("library", WavvyIcons.Library, R.string.nav_library)
}

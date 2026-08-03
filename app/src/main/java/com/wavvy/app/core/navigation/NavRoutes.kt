package com.wavvy.app.core.navigation

// Navigation routes defined as constants
object NavRoutes {
    const val SPLASH = "splash"
    const val AUTH = "auth"
    const val HOME = "home"
    const val SEARCH = "search"
    const val DISCOVER = "discover"
    const val LIBRARY = "library"
    const val SETTINGS = "settings"
}

// Startup tab preference mapped to its navigation route
enum class DefaultTab(val route: String) {
    HOME(NavRoutes.HOME),
    SEARCH(NavRoutes.SEARCH),
    DISCOVER(NavRoutes.DISCOVER),
    LIBRARY(NavRoutes.LIBRARY)
}

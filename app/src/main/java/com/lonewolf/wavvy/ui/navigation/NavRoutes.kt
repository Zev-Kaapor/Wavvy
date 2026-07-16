package com.lonewolf.wavvy.ui.navigation

// Navigation routes defined as constants
object NavRoutes {
    const val HOME = "home"
    const val SEARCH = "search"
    const val LIBRARY = "library"
    const val SETTINGS = "settings"
}

// Startup tab preference mapped to its navigation route
enum class DefaultTab(val route: String) {
    HOME(NavRoutes.HOME),
    SEARCH(NavRoutes.SEARCH),
    LIBRARY(NavRoutes.LIBRARY)
}

package com.wavvy.app.core.designsystem.theme

// Durations of the animations of the app, in milliseconds
object WavvyMotion {
    // Navigation icon growing when selected, and how small it is when it is not
    const val IconScaleMillis = 150
    const val UnselectedIconScale = 0.875f

    // Fade when switching between the main tabs
    const val TabSwitchMillis = 200

    // Fade when moving between the welcome, the login and the app
    const val ScreenFadeMillis = 300

    // Colors changing between the light and dark themes
    const val ThemeMillis = 400

    // Loading placeholders, one light sweep and the wait before the next one
    const val SkeletonSweepMillis = 900
    const val SkeletonPauseMillis = 300
}

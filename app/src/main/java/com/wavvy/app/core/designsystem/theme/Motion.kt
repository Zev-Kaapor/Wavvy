package com.wavvy.app.core.designsystem.theme

// Durations of the animations of the app, in milliseconds
object WavvyMotion {
    // Navigation icon growing when selected, and how small it is when it is not
    const val IconScaleMillis = 150
    const val UnselectedIconScale = 0.875f

    // Fade when switching between the main tabs
    const val TabSwitchMillis = 200

    // How far a screen slides when it comes in or goes out, as a fraction of its width
    const val PageSlideDivisor = 8

    // Fade when moving between the welcome, the login and the app
    const val ScreenFadeMillis = 300

    // Curiosity of the welcome, how long it stays and the fade into the next one
    const val WelcomeFactMillis = 15000
    const val WelcomeFactFadeMillis = 400

    // A part inside a screen opening or closing, such as the fields of a password
    const val ExpandMillis = 250

    // The bars of the pill that listens for a song, one wave of them, and how fast they follow the sound
    const val ListeningWaveMillis = 1400
    const val ListeningLevelMillis = 90

    // The pill of the search by sound, a change of its phase and its coming in and going out
    const val SoundPhaseMillis = 350
    const val SoundShowMillis = 400

    // The light at the bottom, how slow its waves go and how softly they follow the sound, and how long a search that found nothing stays before it goes
    const val SoundGlowWaveMillis = 4000
    const val SoundGlowLevelMillis = 250
    const val SoundFailedHoldMillis = 3000

    // Sheet sliding in from the bottom and the screen dimming behind it
    const val SheetMillis = 300

    // Colors changing between the light and dark themes
    const val ThemeMillis = 400

    // Loading placeholders, one light sweep and the wait before the next one
    const val SkeletonSweepMillis = 900
    const val SkeletonPauseMillis = 300
}

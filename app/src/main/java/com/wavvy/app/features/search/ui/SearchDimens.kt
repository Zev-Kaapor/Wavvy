package com.wavvy.app.features.search.ui

// UI styling and utilities
import androidx.compose.ui.unit.dp

// Sizes and times of the search, the field and the history as in the old Wavvy, the rows of results as the ones of the Home
object SearchDimens {
    // Field of the search, its height and the icons at its end
    val FieldHeight = 52.dp
    val FieldIcon = 22.dp
    val ClearIcon = 20.dp
    val FieldIconEnd = 4.dp

    // The icon of the field changes with a short slide
    const val FieldIconMillis = 150

    // The round button that searches by sound, its size, its icon and the room between it and the field
    val SoundButton = 52.dp
    val SoundIcon = 32.dp
    val SoundButtonGap = 8.dp

    // The bars of the sound, how tall each one is against the middle one, how far apart their waves are, how high they rise in each phase and how much the sound counts
    val SoundBarEnvelope = listOf(0.55f, 0.8f, 1f, 0.8f, 0.55f)
    const val SoundBarPhaseStep = 0.9f
    const val SoundBarFindingAmplitude = 0.35f
    const val SoundBarIdleAmplitude = 0.3f
    const val SoundBarWaveShare = 0.7f
    const val SoundLevelGain = 10f

    // How tall the light at the bottom is, and the window over the other apps when it has to be a screen
    val SoundOverlayHeight = 220.dp

    // The pill with what is happening, its size, the room around it, its icon and its bars, and the spinner and the button that closes it
    val SoundPillHeight = 56.dp
    val SoundPillSide = 16.dp
    val SoundPillMax = 440.dp
    val SoundPillStart = 20.dp
    val SoundPillEnd = 4.dp
    val SoundPillGap = 12.dp
    val SoundPillIcon = 28.dp
    val SoundPillBarWidth = 3.dp
    val SoundPillBarGap = 3.dp
    val SoundPillBarMin = 5.dp
    val SoundPillBarMax = 22.dp
    val SoundPillSpinner = 20.dp
    val SoundSpinnerStroke = 3.dp
    val SoundPillClose = 20.dp
    const val SoundPillAlpha = 0.96f

    // The light of the bottom edge, how strong the veil is, and the waves that move over it, their height, their lift, their length and how strong each one is
    const val SoundGlowAlpha = 0.35f
    const val SoundGlowFaintAlpha = 0.12f
    val SoundGlowWave = 22.dp
    const val SoundGlowWaveBase = 0.35f
    val SoundGlowLift = 28.dp
    val SoundGlowStep = 10.dp
    val SoundGlowWaveCycles = listOf(1.2f, 1.7f, 0.9f)
    val SoundGlowWaves = listOf(0.45f, 0.35f, 0.3f)

    // Row of the history and of the words under the field, the room around it, its icons and the buttons at its end
    val RowCorner = 12.dp
    val RowPadding = 4.dp
    val RowIcon = 18.dp
    val RowGap = 16.dp
    val RowAction = 32.dp
    val RowActionIcon = 16.dp
    const val RowIconAlpha = 0.7f
    const val RowActionAlpha = 0.5f

    // Top of the history, the room of its title and its button
    val HeaderPaddingVertical = 4.dp
    val SuggestedTop = 16.dp

    // Message of a search with nothing to show, its icon and the room around it
    val EmptyIcon = 56.dp
    val EmptyPadding = 32.dp
    val EmptyGap = 16.dp
    const val EmptyIconAlpha = 0.4f

    // Chips of the filters, the space above them and below
    val ChipsTop = 4.dp
    val ChipsBottom = 8.dp

    // Spinner at the end of a list that is asking for more
    val MoreSpinner = 28.dp
    val MorePadding = 16.dp

    // How many rows of each kind the search of everything shows before the title that opens the whole list
    const val SectionPreviewRows = 4

    // Placeholders of the results, how many rows, and the lines of text of each
    const val SkeletonRows = 8
    const val SkeletonTitleFraction = 0.6f
    const val SkeletonSubtitleFraction = 0.35f
    val SkeletonLine = 12.dp

    // How many searches are kept and shown, how long the field waits after a letter before asking for words, and how many rows from the end the next page is asked for
    const val HistoryLimit = 50

    // How many searches the suggestions under the history hold at most
    const val SuggestedLimit = 8

    // How many of the artists listened to the most are looked at when the suggestions are made
    const val SuggestedTopArtists = 20
    const val SuggestionDelayMillis = 150L
    const val LoadMoreThreshold = 4
}

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

    // The first letters asked to the account to bring the searches that start with them, how many are asked at once and how long the answer is kept
    const val HistoryProbes = "abcdefghijklmnopqrstuvwxyz0123456789"
    const val HistoryProbeParallel = 6
    const val HistoryProbeKeepMillis = 60_000L

    // How many searches are kept and shown, how long the field waits after a letter before asking for words, and how many rows from the end the next page is asked for
    const val HistoryLimit = 50
    const val SuggestionDelayMillis = 150L
    const val LoadMoreThreshold = 4
}

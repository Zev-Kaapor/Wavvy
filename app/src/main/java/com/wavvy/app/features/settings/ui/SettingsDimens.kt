package com.wavvy.app.features.settings.ui

// UI styling and utilities
import androidx.compose.ui.unit.dp

// Sizes of the settings, the groups in cards with rows of an icon, two lines of words and an arrow
object SettingsDimens {
    // The room at the sides and between the groups
    val Side = 16.dp
    val GroupGap = 20.dp
    val ListTop = 8.dp

    // The name of a group over its card
    val GroupTitleStart = 8.dp
    val GroupTitleBottom = 8.dp

    // The card of a group and its corners
    val CardCorner = 20.dp

    // A row of a card, its height, the room at its sides, its icon, the room between the icon and the words and the arrow at its end
    val RowHeight = 68.dp
    val RowVertical = 12.dp
    val RowPadding = 16.dp
    val RowIcon = 24.dp
    val RowIconGap = 16.dp
    val RowArrow = 20.dp

    // The dot that tells the state of a row, the room before it, and how faint a row that cannot be used is
    val StatusDot = 10.dp
    val StatusDotGap = 8.dp
    const val DisabledAlpha = 0.38f

    // The room between the lines of what changed and between the buttons of the updates, and the thin ring of the spinner
    val NotesGap = 8.dp
    val NotesBetween = 16.dp
    val ButtonGap = 12.dp
    val ProgressStroke = 2.dp
    val NotesDetailTop = 4.dp

    // The boxes of the backup, their corners, the room inside them, between their parts and over their buttons
    val DialogCorner = 28.dp
    val DialogPadding = 24.dp
    val DialogGap = 12.dp
    val DialogButtonsTop = 12.dp

    // The box of the proxy, its corners, its room inside, between its parts and over its buttons
    val ProxyDialogCorner = 28.dp
    val ProxyDialogPadding = 24.dp
    val ProxyFieldGap = 12.dp
    val ProxyButtonsTop = 12.dp

    // The logo of a service at the start of a row and the room after it
    val LogoSize = 32.dp
    val LogoGap = 12.dp

    // The picture of each project on the screen of the thanks
    val ThanksIcon = 40.dp

    // The photo of the author on the page about the app
    val AuthorPhoto = 56.dp

    // The line between two rows of a card, which starts where the words start
    val DividerStart = RowPadding + RowIcon + RowIconGap
}

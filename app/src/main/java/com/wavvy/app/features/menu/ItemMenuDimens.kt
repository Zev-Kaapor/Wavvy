package com.wavvy.app.features.menu

// UI styling and utilities
import androidx.compose.ui.unit.dp

// Sizes of the menus of the cards, as the menus of YouTube Music have them
object ItemMenuDimens {
    // Room at the sides and under the last line of a menu
    val Side = 24.dp
    val Bottom = 16.dp

    // The head of the menu with the name and the close button
    val HeaderTop = 16.dp
    val HeaderBottom = 8.dp
    val CloseEnd = 12.dp

    // How many lines the name and the line under it of an episode can have before they are cut
    const val EpisodeTitleLines = 2
    const val EpisodeLineLines = 2

    // The buttons side by side, their height, corners, icon, the room between them and the room of their name
    val TileHeight = 62.dp
    val TileCorner = 12.dp
    val TileIcon = 24.dp
    val TileGap = 16.dp
    val TilesVertical = 16.dp
    val TileLabelTop = 8.dp
    val TileLabelWidth = 104.dp
    const val DisabledAlpha = 0.4f

    // The lines under the buttons, their height, icon and the room between the icon and the words
    val RowHeight = 56.dp
    val RowIcon = 24.dp
    val RowTextStart = 24.dp

    // The bar that stands for the words of a line that is still loading
    val PlaceholderWidth = 140.dp
    val PlaceholderHeight = 16.dp
    val PlaceholderCorner = 4.dp
}

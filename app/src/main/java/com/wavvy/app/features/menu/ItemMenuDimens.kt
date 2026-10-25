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

    // Menu of a song as the old Wavvy drew it, the cover on top, its corners and the room after it
    val HeaderCover = 64.dp
    val HeaderCoverCorner = 14.dp
    val HeaderGap = 16.dp
    val HeaderVertical = 6.dp

    // Every option is a rounded card, with the room at the sides of the menu, between the cards and between the groups
    val CardCorner = 16.dp
    val CardsSide = 16.dp
    val CardGap = 8.dp
    val CardGroupGap = 14.dp

    // The three cards of the top, side by side, their height, icon and the room above their name
    val CardTileHeight = 84.dp
    val CardTileGap = 10.dp
    val CardTileIcon = 24.dp
    val CardTileLabelTop = 6.dp
    val CardTilePadding = 8.dp

    // The cards of the list, their room inside, icon and the room before the words
    val CardRowHorizontal = 16.dp
    val CardRowVertical = 12.dp
    val CardRowIcon = 22.dp
    val CardRowTextStart = 16.dp
    val CardRowTagGap = 8.dp

    // Lying down, the share of the width the song takes on the start, and the room under the options
    const val LandscapeHeadWeight = 0.5f
    val LandscapeBottom = 16.dp
    val LandscapeIndicatorGap = 8.dp

    // The sheet of the credits, the room under it, between the names, and the cards that stand for the groups while they load
    val CreditsBottom = 32.dp
    val CreditsTitleStart = 4.dp
    val CreditsNameGap = 2.dp
    val CreditsSkeletonHeight = 64.dp
    const val CreditsSkeletonCards = 3

    // The bar that stands for the words of a line that is still loading
    val PlaceholderWidth = 140.dp
    val PlaceholderHeight = 16.dp
    val PlaceholderCorner = 4.dp
}

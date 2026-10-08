package com.wavvy.app.features.home.ui.components

// UI styling and utilities
import androidx.compose.ui.unit.dp

// Sizes of the Home, the header as in the old Wavvy and the content as in Metrolist (GPL-3.0)
object HomeDimens {
    // Height of the isologo, its width follows the proportion of the image
    val LogoHeight = 24.dp

    // Icon of the connection at the top end of the isologo, the space between them and how faint it is while the connection is fine
    val ConnectionIcon = 12.dp
    val ConnectionGap = 4.dp
    const val ConnectionAlpha = 0.6f

    // Round profile button
    val ProfileButton = 40.dp

    // Bell icon without a background
    val BellIcon = 26.dp

    // Filter placeholders, widths vary so they look like real chips
    val FilterHeight = 32.dp
    val FilterPlaceholderWidths = listOf(72.dp, 64.dp, 96.dp, 80.dp, 72.dp)

    // Filters, the space before the first one and after each, and their corners
    val FilterStart = 12.dp
    val FilterSpacing = 8.dp
    val FilterCorner = 16.dp

    // Widest the message of the Home gets, and how many shelves from the end the next page is asked for
    val MessageMaxWidth = 320.dp
    const val LoadMoreThreshold = 3

    // Height of the message of an empty Home inside the list
    val EmptyHeight = 240.dp

    // Title of a shelf, the space around it and between its parts, and the photo next to it
    val TitlePadding = 12.dp
    val TitleSpacing = 12.dp
    val TitlePhoto = 48.dp

    // Play all button next to the title of a shelf of songs
    val PlayAllHeight = 24.dp
    val PlayAllCorner = 12.dp
    val PlayAllBorder = 1.dp
    val PlayAllPaddingHorizontal = 12.dp
    val PlayAllPaddingVertical = 2.dp
    const val PlayAllBorderAlpha = 0.5f

    // Card of a cover, the height of the cover, the space around the card and above its text
    val GridCover = 128.dp
    val GridPadding = 12.dp
    val GridTextGap = 6.dp

    // Corners of the covers
    val CoverCorner = 3.dp

    // Row of a list, its height, the space at its sides, around its cover and around its text, and the cover
    val ListHeight = 64.dp
    val ListPadding = 8.dp
    val ListCoverPadding = 6.dp
    val ListTextPadding = 6.dp
    val ListCover = 48.dp

    // Width of the place of a song in a ranked list
    val RankWidth = 32.dp

    // Shelf of only songs, rows in each column, share of the width a column takes and the width two columns need
    const val SongRows = 4
    const val SongColumnWideFraction = 0.475f
    const val SongColumnNarrowFraction = 0.9f
    val SongColumnMinWidth = 320.dp

    // Round play button over a cover, its icon, the icon of the album button and its space from the corner
    val PlayButton = 36.dp
    val PlayIcon = 20.dp
    val AlbumPlayIcon = 24.dp
    val AlbumPlayInset = 8.dp

    // Mark of explicit lyrics before the line of a card, and the space after it
    val Badge = 18.dp
    val BadgeEnd = 2.dp

    // Speed dial, the width a tile aims for, the room kept at the sides, how many columns a page has at least and when it has more rows
    val SpeedDialTile = 160.dp
    val SpeedDialMargin = 32.dp
    const val SpeedDialMinColumns = 3
    const val SpeedDialWideColumns = 6
    const val SpeedDialMidColumns = 4
    const val SpeedDialMaxItems = 27

    // Speed dial pages, the space at their sides and between them, and the room around a tile
    val SpeedDialPagePadding = 16.dp
    val SpeedDialPageSpacing = 16.dp
    val SpeedDialTilePadding = 4.dp

    // Title of a tile, its room from the edge, the arrow of a tile that opens a page, and the size the picture of a tile is asked in
    val SpeedDialTitlePadding = 8.dp
    val SpeedDialArrow = 20.dp
    const val SpeedDialRequestSize = 200

    // Dark over a tile from top to bottom
    const val SpeedDialScrimTop = 0.4f
    const val SpeedDialScrimMiddle = 0.6f
    const val SpeedDialScrimBottom = 0.9f

    // Dots that tell which page is showing, their row, their size and room, and how faint the other pages are
    val SpeedDialIndicatorHeight = 24.dp
    val SpeedDialDot = 8.dp
    val SpeedDialDotPadding = 4.dp
    const val SpeedDialDotAlpha = 0.5f

    // Tile that picks something at random, its dots and how far they sit from the middle
    val RandomDot = 14.dp
    val RandomDotOffset = 24.dp

    // Songs of the history the Home shows standing and lying, the old Wavvy showed few so the row stays a glance
    const val RecentPortraitItems = 5
    const val RecentMaxItems = 10

    // Songs listened to the longest that open the speed dial, how many and in how many days, as Metrolist (GPL-3.0) takes them from two weeks
    const val KeepListeningMaxItems = 10
    const val KeepListeningDays = 14

    // Forgotten favorites, how many days without listening make a song forgotten and how many are shown
    const val ForgottenDays = 30
    const val ForgottenMaxItems = 20

    // Quick picks, how many songs are shown and how many of them come from the forgotten favorites, as Metrolist (GPL-3.0)
    const val QuickPicksMaxItems = 20
    const val QuickPicksForgottenItems = 8

    // A card of the history fades in and out while the others slide, and its content fades in after the cover arrives
    const val RecentFadeMillis = 300
    const val RecentContentFadeMillis = 1200

    // Size in pixels the pictures of the covers are asked in
    const val CoverRequestSize = 544

    // Camera of a video and pin of a pinned song on the top corners of a cover, their size, their room and how far from the corner
    val CoverBadgeIcon = 10.dp
    val CoverBadgePadding = 3.dp
    val CoverBadgeInset = 4.dp

    // Shelf placeholders, how many shelves and covers they have, enough to fill wide screens
    const val SkeletonShelves = 3
    const val SkeletonCovers = 8

    // Title bar of a shelf, and the two text lines under each cover
    val ShelfTitleWidth = 180.dp
    val ShelfTitleHeight = 24.dp
    val CoverLineHeight = 14.dp
    const val CoverTitleFraction = 0.8f
    const val CoverSubtitleFraction = 0.5f
}

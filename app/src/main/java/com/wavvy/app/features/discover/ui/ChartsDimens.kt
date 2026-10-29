package com.wavvy.app.features.discover.ui

// UI styling and utilities
import androidx.compose.ui.unit.dp

// Sizes of the page of the charts, drawn for what it holds, the charts as cards, the first three artists as a podium and the others as a list
object ChartsDimens {
    // The room at the sides and between the parts of the page
    val Side = 16.dp
    val SectionGap = 24.dp

    // From this width the charts come in columns and the list of the ranks in two, so a wide screen shows more and not bigger
    val WideBreakpoint = 600.dp
    const val WideChartColumns = 3
    const val WideRankColumns = 2
    val ColumnGap = 12.dp

    // A chart as a card, its cover, corners, the room inside and between its parts, its arrow and the room between two cards
    val ChartCover = 64.dp
    val ChartCorner = 14.dp
    val ChartCoverCorner = 8.dp
    val ChartPadding = 10.dp
    val ChartGap = 12.dp
    val ChartArrow = 28.dp
    val ChartCardGap = 10.dp

    // The podium, how wide it gets, the photo of the first and of the other two, the ring of the brand around the first, the mark with the place, the menu and the room around the names
    val PodiumFirst = 104.dp
    val PodiumOther = 76.dp
    val PodiumRing = 3.dp
    val PodiumRingGap = 3.dp
    val PodiumBadge = 24.dp
    val PodiumBadgeBorder = 2.dp
    val PodiumMaxWidth = 420.dp
    val PodiumMenu = 32.dp
    val PodiumMenuIcon = 18.dp
    val PodiumTrendSlot = 12.dp
    val PodiumNameGap = 10.dp
    val PodiumTextSide = 4.dp
    val PodiumTrend = 8.dp

    // A row of the list, its height, the room of the place, the picture, its corners and the room around the texts
    val RowHeight = 64.dp
    val RowRank = 44.dp
    val RowPicture = 48.dp
    val RowCorner = 6.dp
    val RowTrend = 8.dp

    // The button of the country in the bar, its height, corners, the room inside and its arrow
    val PillHeight = 32.dp
    val PillCorner = 16.dp
    val PillPadding = 12.dp
    val PillChevron = 18.dp
    val PillGap = 4.dp
    val BarEnd = 12.dp

    // Placeholder of the page, the lines of a chart and of a row, and the width of the title of a section
    val SkeletonLine = 14.dp
    val SkeletonTitleWidth = 140.dp
    const val SkeletonNameFraction = 0.6f
    const val SkeletonLineFraction = 0.4f
    const val SkeletonCharts = 3
    const val SkeletonRows = 4
}

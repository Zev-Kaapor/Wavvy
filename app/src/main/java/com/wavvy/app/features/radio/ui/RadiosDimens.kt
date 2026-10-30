package com.wavvy.app.features.radio.ui

// UI styling and utilities
import androidx.compose.ui.unit.dp

// Sizes of the list of the radios
object RadiosDimens {
    // The logo of a station and its corners, and the room above and below a row
    val Logo = 56.dp
    val LogoCorner = 8.dp
    val RowVertical = 8.dp

    // The room around the title of a group of places
    val SectionVertical = 12.dp

    // The spinner that shows a station is opening and the one at the end of the list
    val Spinner = 22.dp
    val SpinnerStroke = 2.dp
    val FooterVertical = 16.dp

    // The station that plays now, its corners, the room inside it, its logo, the button that pauses it and the dot that says it is live
    val NowCorner = 16.dp
    val NowPadding = 12.dp
    val NowLogo = 64.dp
    val NowButton = 44.dp
    val NowButtonIcon = 24.dp
    val LiveDot = 8.dp
    val LiveGap = 6.dp
    val FavoriteMark = 16.dp

    // A station as a card, its width, which is the one of its logo, its corners, the room above its name and the mark of the one that plays
    val CardWidth = 104.dp
    val CardCorner = 12.dp
    val CardTextGap = 8.dp
    val CardBadge = 24.dp
    val CardBadgeInset = 6.dp
    val CardBadgeIcon = 14.dp

    // The big cards of the places, their height, corners, the room inside them and their icon
    val PlaceHeight = 92.dp
    val PlaceCorner = 16.dp
    val PlacePadding = 16.dp
    val PlaceIcon = 24.dp

    // The room of a list while it is asked, how many rows of stations and of places, how much of the width the lines take, their heights and the place of the menu
    const val SkeletonRows = 9
    const val SkeletonPlaceRows = 6
    const val SkeletonNameFraction = 0.6f
    const val SkeletonPlaceFraction = 0.45f
    const val SkeletonOnAirFraction = 0.3f
    val SkeletonNameHeight = 16.dp
    val SkeletonLineHeight = 12.dp
    val SkeletonCountWidth = 80.dp
    val SkeletonMenu = 24.dp
    val SkeletonMenuGap = 12.dp

    // The room around the line that says how many stations a list has
    val CountVertical = 8.dp

    // The room under each part of the page, what the search finds as buttons, how many, how wide and the room around them
    val SectionBottom = 8.dp
    const val MaxMatches = 12
    val MatchWidth = 160.dp
    val MatchesVertical = 8.dp

    // Room kept between the end of the list and the mini player
    val MiniPlayerClearance = 30.dp

    // How many stations are read at most while something is typed in a list, so a huge list is not read whole
    const val FilterScanLimit = 660

    // How many stations before the end of the list the next page is asked
    const val LoadMoreMargin = 6
}

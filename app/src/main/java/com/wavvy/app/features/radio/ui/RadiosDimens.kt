package com.wavvy.app.features.radio.ui

// UI styling and utilities
import androidx.compose.ui.unit.dp

// Sizes of the list of the radios
object RadiosDimens {
    // The logo of a station and its corners, and the room above and below a row
    val Logo = 56.dp
    val LogoCorner = 8.dp
    val RowVertical = 8.dp

    // The buttons of the filters and the room around them, the title of a group of places, and a place with its arrow
    val ChipsGap = 8.dp
    val ChipsVertical = 4.dp
    val SectionVertical = 12.dp
    val EntryHeight = 52.dp
    val EntryArrow = 24.dp

    // The spinner that shows a station is opening and the one at the end of the list
    val Spinner = 22.dp
    val SpinnerStroke = 2.dp
    val FooterVertical = 16.dp

    // Room kept between the end of the list and the mini player
    val MiniPlayerClearance = 30.dp

    // How many stations are read at most while something is typed in a list, so a huge list is not read whole
    const val FilterScanLimit = 660

    // How many stations before the end of the list the next page is asked
    const val LoadMoreMargin = 6
}

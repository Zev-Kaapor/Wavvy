package com.wavvy.app.features.home.ui.components

// UI styling and utilities
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Text of the Home content, the line heights of Metrolist (GPL-3.0) with sizes and weights reduced so Poppins looks like its Roboto
object HomeType {
    // Small line above the title of a shelf
    val SectionLabel = TextStyle(fontSize = 12.5.sp, lineHeight = 20.sp, fontWeight = FontWeight.Normal, letterSpacing = 0.1.sp)

    // Title of a shelf
    val SectionTitle = TextStyle(fontSize = 20.sp, lineHeight = 28.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.sp)

    // Play all button
    val PlayAll = TextStyle(fontSize = 10.sp, lineHeight = 16.sp, fontWeight = FontWeight.Normal, letterSpacing = 0.5.sp)

    // Title and line under a card of a cover
    val GridTitle = TextStyle(fontSize = 14.5.sp, lineHeight = 24.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.5.sp)
    val GridSubtitle = TextStyle(fontSize = 12.5.sp, lineHeight = 20.sp, fontWeight = FontWeight.Normal, letterSpacing = 0.25.sp)

    // Title and line of a row of a list
    val ListTitle = TextStyle(fontSize = 12.5.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.25.sp)
    // Place of a song in a ranked list
    val Rank = TextStyle(fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight.Light, letterSpacing = 0.sp)

    val ListSubtitle = TextStyle(fontSize = 11.sp, lineHeight = 16.sp, fontWeight = FontWeight.Normal, letterSpacing = 0.4.sp)

    // Title over a tile of the speed dial
    val SpeedDialTitle = TextStyle(fontSize = 13.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.1.sp)

    // Name of a filter
    val Filter = TextStyle(fontSize = 12.5.sp, lineHeight = 20.sp, fontWeight = FontWeight.Normal, letterSpacing = 0.1.sp)
}

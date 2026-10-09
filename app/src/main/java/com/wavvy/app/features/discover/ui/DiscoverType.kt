package com.wavvy.app.features.discover.ui

// UI text styles
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Text styles of the Discover tab, white titles and plain lines under them as in YouTube Music
object DiscoverType {
    // Name of a big button
    val Shortcut = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.sp)

    // Title of a shelf
    val Shelf = TextStyle(fontSize = 21.sp, lineHeight = 28.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.sp)

    // Title of a card, and the line under it
    val CardTitle = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.sp)
    val CardLine = TextStyle(fontSize = 13.sp, lineHeight = 18.sp, fontWeight = FontWeight.Normal, letterSpacing = 0.sp)

    // Name of a mood or of a genre
    val Mood = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.sp)

    // The big title of a page and the title in its bar
    val PageTitle = TextStyle(fontSize = 34.sp, lineHeight = 30.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.sp)
    val BarTitle = TextStyle(fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.sp)

    // Name of the country on its button and in its list
    val Country = TextStyle(fontSize = 16.sp, lineHeight = 24.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.sp)
    val CountryOption = TextStyle(fontSize = 18.sp, lineHeight = 26.sp, fontWeight = FontWeight.Normal, letterSpacing = 0.sp)

    // The length of an episode on its picture
    val Length = TextStyle(fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.sp)

    // Place of a song in the ranked list
    val Rank = TextStyle(fontSize = 28.sp, lineHeight = 34.sp, fontWeight = FontWeight.Light, letterSpacing = 0.sp)
}

package com.wavvy.app.features.artist.ui

// UI text styles
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Text styles of the page of an artist, the shelves use the ones of the Home
object ArtistType {
    // Name of the artist over the picture
    val Name = TextStyle(fontSize = 36.sp, lineHeight = 44.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.sp)

    // Listeners of the month and the views
    val Detail = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Normal, letterSpacing = 0.1.sp)

    // Name of the artist on the top bar once the page scrolled
    val Bar = TextStyle(fontSize = 20.sp, lineHeight = 28.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.sp)

    // Title of the about block
    val About = TextStyle(fontSize = 22.sp, lineHeight = 30.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.sp)
}

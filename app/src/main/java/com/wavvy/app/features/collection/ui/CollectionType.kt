package com.wavvy.app.features.collection.ui

// UI text styles
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Text styles of the page of an album or of a playlist, the rows of songs use the ones of the Home
object CollectionType {
    // Title under the cover
    val Title = TextStyle(fontSize = 26.sp, lineHeight = 34.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.sp)

    // Name of who made it, on top of the page
    val Owner = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.1.sp)

    // Kind and year under the name, and the line with the number of songs at the end of the list
    val Detail = TextStyle(fontSize = 13.sp, lineHeight = 18.sp, fontWeight = FontWeight.Normal, letterSpacing = 0.1.sp)
}

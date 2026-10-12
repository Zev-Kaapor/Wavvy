package com.wavvy.app.features.settings.ui

// UI text styles
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Text styles of the settings
object SettingsType {
    // The name of a group over its card
    val GroupTitle = TextStyle(fontSize = 13.sp, lineHeight = 18.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.sp)

    // A row, its name and the line under it
    val RowTitle = TextStyle(fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.sp)
    val RowSubtitle = TextStyle(fontSize = 13.sp, lineHeight = 18.sp, fontWeight = FontWeight.Normal, letterSpacing = 0.sp)
}

package com.wavvy.app.features.notifications.ui

// UI text styles
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Text styles of the screen of notifications
object NotificationsType {
    // Name of a group, such as the new ones and the ones before
    val Group = TextStyle(fontSize = 16.sp, lineHeight = 24.sp, fontWeight = FontWeight.Normal, letterSpacing = 0.sp)

    // The message of a notification and its time
    val Message = TextStyle(fontSize = 15.sp, lineHeight = 22.sp, fontWeight = FontWeight.Normal, letterSpacing = 0.sp)
    val Time = TextStyle(fontSize = 13.sp, lineHeight = 18.sp, fontWeight = FontWeight.Normal, letterSpacing = 0.sp)
}

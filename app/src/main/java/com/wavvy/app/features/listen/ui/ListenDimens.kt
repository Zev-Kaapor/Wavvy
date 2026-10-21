package com.wavvy.app.features.listen.ui

// UI styling and utilities
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Sizes of the boxes of listening together
object ListenDimens {
    // The corners of a box, the room inside it and between its parts
    val DialogCorner = 28.dp
    val DialogPadding = 24.dp
    val DialogGap = 12.dp

    // The room between the two choices of the first step, closer than the other parts
    val ChoiceGap = 4.dp

    // The outline of the buttons that are not the main one, in the color of the text and softer than it
    val ButtonBorder = 1.dp
    const val ButtonBorderAlpha = 0.4f

    // The room between a name and the mark of the host after it
    val HostGap = 6.dp

    // The room between the letters of the code of a room, so it is easy to read out loud
    val CodeSpacing = 4.sp
}

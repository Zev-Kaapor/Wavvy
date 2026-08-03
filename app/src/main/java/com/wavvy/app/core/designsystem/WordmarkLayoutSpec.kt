package com.wavvy.app.core.designsystem

// UI measurement units
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// Wordmark positioning and layout specifications
object WordmarkLayoutSpec {
    val CONTENT_PADDING = 24.dp
    val BOTTOM_ACTIONS_HEIGHT = 152.dp
    val WORDMARK_HEIGHT = 40.dp
    const val VERTICAL_BIAS = -0.3f

    // Docked offset calculation
    fun dockedOffset(screenHeight: Dp): Dp {
        val titleBoxHeight = screenHeight - (CONTENT_PADDING * 2) - BOTTOM_ACTIONS_HEIGHT
        val biasFactor = (1f + VERTICAL_BIAS) / 2f
        val offsetWithinBox = (titleBoxHeight - WORDMARK_HEIGHT) * biasFactor
        return CONTENT_PADDING + offsetWithinBox
    }
}

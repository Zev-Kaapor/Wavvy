package com.wavvy.app.core.designsystem.components

// Compose layouts and foundations
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
// Compose state and runtime
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
// UI utilities
import androidx.compose.ui.Modifier

// Clickable without the touch ripple, as the cards and buttons of the old Wavvy
@Composable
fun Modifier.clickableNoIndication(onClick: () -> Unit): Modifier {
    val interactionSource = remember { MutableInteractionSource() }

    return clickable(
        interactionSource = interactionSource,
        indication = null,
        onClick = onClick
    )
}

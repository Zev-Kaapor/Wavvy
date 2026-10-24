package com.wavvy.app.core.designsystem.components

// Material 3 components
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
// Compose state and runtime
import androidx.compose.runtime.Composable
// UI styling and utilities
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color

// A little smaller than the switch of the system, not so small that its outline gets thin
private const val SwitchScale = 0.9f

// The switch of the app, when it is off only its outline shows, in a color that stands out on the dark theme and on the light one
@Composable
fun WavvySwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        enabled = enabled,
        colors = SwitchDefaults.colors(
            uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
            uncheckedTrackColor = Color.Transparent,
            uncheckedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        modifier = modifier.scale(SwitchScale)
    )
}

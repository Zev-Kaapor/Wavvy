package com.wavvy.app.features.cast.ui

// Compose layouts and foundations
import androidx.compose.foundation.layout.requiredSize
// Material 3 components
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
// Compose state and runtime
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
// UI styling and utilities
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
// Project resources
import com.wavvy.app.R
import com.wavvy.app.core.cast.CastController
import com.wavvy.app.core.designsystem.icons.WavvyIcons

// The button that opens the list of devices, filled with the waves when the app is connected to one, nothing on a phone that cannot cast
@Composable
fun CastButton(tint: Color, buttonSize: Dp, iconSize: Dp, modifier: Modifier = Modifier) {
    val isAvailable by CastController.isAvailable.collectAsState()
    val connectedName by CastController.connectedName.collectAsState()
    if (!isAvailable) return

    IconButton(onClick = CastSheet::show, modifier = modifier.requiredSize(buttonSize)) {
        Icon(
            imageVector = if (connectedName != null) WavvyIcons.CastConnected else WavvyIcons.Cast,
            contentDescription = stringResource(R.string.cast_title),
            modifier = Modifier.requiredSize(iconSize),
            tint = tint
        )
    }
}

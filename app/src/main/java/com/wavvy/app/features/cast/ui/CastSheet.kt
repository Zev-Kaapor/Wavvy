package com.wavvy.app.features.cast.ui

// Compose layouts and foundations
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
// Material 3 components
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
// Compose state and runtime
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
// UI styling and utilities
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
// Project resources
import com.wavvy.app.R
import com.wavvy.app.core.cast.CastController
import com.wavvy.app.core.cast.CastDevice
import com.wavvy.app.core.designsystem.components.LocalSheetClose
import com.wavvy.app.core.designsystem.components.OverlaySheet
import com.wavvy.app.core.designsystem.components.WavvySheet
import com.wavvy.app.core.designsystem.icons.WavvyIcons
import com.wavvy.app.features.menu.ItemMenuDimens
import com.wavvy.app.features.playlist.ui.SheetHeader
import com.wavvy.app.features.playlist.ui.SheetMessage

// The sheet that lists the devices the sound can go to, held outside the screens so it can cover the whole window
object CastSheet {
    fun show() {
        OverlaySheet.show { CastSheetContent(onDismiss = OverlaySheet::dismiss) }
    }
}

// The devices found while the sheet is open, the one that is connected comes first with the way to leave it
@Composable
private fun CastSheetContent(onDismiss: () -> Unit) {
    val devices by CastController.devices.collectAsState()
    val connectedName by CastController.connectedName.collectAsState()

    // The search runs only while the sheet is on the screen
    DisposableEffect(Unit) {
        CastController.startSearch()
        onDispose { CastController.stopSearch() }
    }

    WavvySheet(onDismiss = onDismiss) {
        val closeSheet = LocalSheetClose.current

        Column(modifier = Modifier.verticalScroll(rememberScrollState()).padding(bottom = ItemMenuDimens.Bottom)) {
            SheetHeader(title = stringResource(R.string.cast_title), action = {
                IconButton(onClick = CastController::restartSearch) {
                    Icon(imageVector = WavvyIcons.Refresh, contentDescription = stringResource(R.string.cast_search_again), tint = MaterialTheme.colorScheme.onSurface)
                }
            })

            connectedName?.let { name ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = ItemMenuDimens.RowHeight)
                        .clickable {
                            CastController.disconnect()
                            closeSheet()
                        }
                        .padding(horizontal = ItemMenuDimens.Side)
                ) {
                    Box(modifier = Modifier.width(ItemMenuDimens.RowIcon + ItemMenuDimens.RowTextStart)) {
                        Icon(imageVector = WavvyIcons.CastConnected, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = stringResource(R.string.cast_connected, name), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                        Text(text = stringResource(R.string.cast_disconnect), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            val others = devices.filterNot { it.isConnected }
            if (others.isEmpty() && connectedName == null) {
                SheetMessage(stringResource(R.string.cast_none))
            }
            others.forEach { device ->
                DeviceRow(device = device, onClick = {
                    CastController.connect(device)
                    closeSheet()
                })
            }
        }
    }
}

// A device of the list, its name and what it says it is
@Composable
private fun DeviceRow(device: CastDevice, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = ItemMenuDimens.RowHeight)
            .clickable(onClick = onClick)
            .padding(horizontal = ItemMenuDimens.Side)
    ) {
        Box(modifier = Modifier.width(ItemMenuDimens.RowIcon + ItemMenuDimens.RowTextStart)) {
            Icon(imageVector = WavvyIcons.Cast, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface)
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = device.name,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            device.description?.takeIf { it.isNotBlank() }?.let { description ->
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

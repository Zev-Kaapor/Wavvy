package com.wavvy.app.features.cast.ui

// Material 3 components
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
// Compose state and runtime
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
// UI utilities
import androidx.compose.ui.res.stringResource
// Project resources
import com.wavvy.app.R
import com.wavvy.app.core.cast.CastController
import com.wavvy.app.core.designsystem.components.LocalSheetClose
import com.wavvy.app.core.designsystem.components.OverlaySheet
import com.wavvy.app.core.designsystem.components.WavvySheet
import com.wavvy.app.core.designsystem.icons.WavvyIcons
import com.wavvy.app.features.menu.CardMenuHead
import com.wavvy.app.features.menu.CardMenuLayout
import com.wavvy.app.features.menu.MenuCard
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

        // The title with the search again at its end, the connected device and then the others as cards, as the other menus have them
        CardMenuLayout(
            head = {
                CardMenuHead(
                    title = stringResource(R.string.cast_title),
                    hasCover = false,
                    trailing = {
                        IconButton(onClick = CastController::restartSearch) {
                            Icon(imageVector = WavvyIcons.Refresh, contentDescription = stringResource(R.string.cast_search_again), tint = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                )
            },
            options = {
                connectedName?.let { name ->
                    MenuCard(WavvyIcons.CastConnected, stringResource(R.string.cast_connected, name), subtitle = stringResource(R.string.cast_disconnect)) {
                        CastController.disconnect()
                        closeSheet()
                    }
                }

                val others = devices.filterNot { it.isConnected }
                if (others.isEmpty() && connectedName == null) {
                    SheetMessage(stringResource(R.string.cast_none))
                }
                others.forEach { device ->
                    MenuCard(WavvyIcons.Cast, device.name, subtitle = device.description?.takeIf { it.isNotBlank() }) {
                        CastController.connect(device)
                        closeSheet()
                    }
                }
            }
        )
    }
}

package com.lonewolf.wavvy.ui.settings.sections

// Compose foundation and layout
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
// Project resources
import com.lonewolf.wavvy.R
import com.lonewolf.wavvy.data.SettingsStorage
import com.lonewolf.wavvy.ui.settings.components.SettingsGroupCard
import com.lonewolf.wavvy.ui.settings.components.SettingsToggleRow

// Playback engine preferences subscreen layout
@Composable
fun PlayerSubScreen(
    queueLimit: Int,
    onQueueLimitChange: (Int) -> Unit,
    isPlayerActive: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    // Initialise persistence driver
    val storage = remember { SettingsStorage(context) }

    // Read wifi-only preference
    var wifiOnly by remember {
        mutableStateOf(storage.getBoolean(SettingsStorage.KEY_DOWNLOAD_WIFI_ONLY, false))
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        SettingsGroupCard(title = stringResource(R.string.setting_group_playback)) {
            SettingsToggleRow(
                title = stringResource(R.string.setting_download_wifi_only),
                subtitle = stringResource(R.string.setting_download_wifi_only_desc),
                icon = Icons.Rounded.Wifi,
                checked = wifiOnly,
                onCheckedChange = { newValue ->
                    wifiOnly = newValue
                    storage.saveBoolean(SettingsStorage.KEY_DOWNLOAD_WIFI_ONLY, newValue)
                },
                showDivider = false
            )
        }

        Spacer(modifier = Modifier.height(if (isPlayerActive) 110.dp else 16.dp))
    }
}

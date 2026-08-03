package com.wavvy.app.features.settings.ui.sections

// Compose foundation and layout
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
// Material 3 icons
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
// Project resources
import com.wavvy.app.R
import com.wavvy.app.core.data.local.SettingsStorage
import com.wavvy.app.features.settings.ui.components.SettingsGroupCard
import com.wavvy.app.features.settings.ui.components.SettingsToggleRow

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

    // Initialize persistence driver
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

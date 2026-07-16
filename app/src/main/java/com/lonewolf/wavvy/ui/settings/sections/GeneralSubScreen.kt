package com.lonewolf.wavvy.ui.settings.sections

// Compose foundation and layout
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
// Material 3 icons
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.Troubleshoot
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

// App general preferences subscreen layout
@Composable
fun GeneralSubScreen(
    isPlayerActive: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    // Initialize persistence driver
    val storage = remember { SettingsStorage(context) }

    // Read initial values from storage
    var startOnBoot by remember {
        mutableStateOf(storage.getBoolean("pref_start_on_boot", false))
    }
    var sendTelemetry by remember {
        mutableStateOf(storage.getBoolean("pref_send_telemetry", true))
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        SettingsGroupCard(title = stringResource(R.string.setting_group_app)) {
            SettingsToggleRow(
                title = "Start on Boot",
                subtitle = "Launch Wavvy automatically when the device starts up",
                icon = Icons.Rounded.PowerSettingsNew,
                checked = startOnBoot,
                onCheckedChange = { newValue ->
                    startOnBoot = newValue
                    storage.saveBoolean("pref_start_on_boot", newValue)
                },
                showDivider = true
            )
            SettingsToggleRow(
                title = "Send Anonymous Analytics",
                subtitle = "Help us improve Wavvy by sharing performance reports",
                icon = Icons.Rounded.Troubleshoot,
                checked = sendTelemetry,
                onCheckedChange = { newValue ->
                    sendTelemetry = newValue
                    storage.saveBoolean("pref_send_telemetry", newValue)
                },
                showDivider = false
            )
        }

        Spacer(modifier = Modifier.height(if (isPlayerActive) 110.dp else 16.dp))
    }
}

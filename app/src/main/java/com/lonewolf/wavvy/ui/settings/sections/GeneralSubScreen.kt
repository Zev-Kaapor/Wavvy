package com.lonewolf.wavvy.ui.settings.sections

// Compose foundation and layout
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
// Material 3 icons
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.ClearAll
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
// Project resources
import com.lonewolf.wavvy.R
import com.lonewolf.wavvy.ui.settings.components.SettingsGroupCard
import com.lonewolf.wavvy.ui.settings.components.SettingsInteractiveRow
import com.lonewolf.wavvy.ui.settings.components.SettingsToggleRow

// General preferences subscreen layout
@Composable
fun GeneralSubScreen(
    isPlayerActive: Boolean,
    modifier: Modifier = Modifier
) {
    val internalScrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(internalScrollState)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        SettingsGroupCard(title = stringResource(R.string.setting_subgroup_localization)) {
            SettingsInteractiveRow(
                title = stringResource(R.string.setting_app_language),
                subtitle = stringResource(R.string.setting_app_language_default),
                icon = Icons.Rounded.Language,
                showDivider = false
            )
        }

        SettingsGroupCard(title = stringResource(R.string.setting_subgroup_system)) {
            SettingsToggleRow(
                title = stringResource(R.string.setting_keep_screen_on),
                subtitle = stringResource(R.string.setting_keep_screen_on_desc),
                icon = Icons.Rounded.LightMode,
                showDivider = true
            )
            SettingsToggleRow(
                title = stringResource(R.string.setting_stop_on_recents_remove),
                subtitle = stringResource(R.string.setting_stop_on_recents_remove_desc),
                icon = Icons.Rounded.ClearAll,
                showDivider = true
            )
            SettingsToggleRow(
                title = stringResource(R.string.setting_resume_on_bluetooth),
                subtitle = stringResource(R.string.setting_resume_on_bluetooth_desc),
                icon = Icons.Rounded.Bluetooth,
                showDivider = false
            )
        }

        Spacer(modifier = Modifier.height(if (isPlayerActive) 110.dp else 16.dp))
    }
}

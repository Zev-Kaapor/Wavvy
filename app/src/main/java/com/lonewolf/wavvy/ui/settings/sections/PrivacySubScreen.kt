package com.lonewolf.wavvy.ui.settings.sections

// Compose foundation and layout
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
// Material 3 and icons
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ManageSearch
import androidx.compose.material.icons.rounded.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
// Project resources
import com.lonewolf.wavvy.R
import com.lonewolf.wavvy.ui.settings.components.SettingsActionRow
import com.lonewolf.wavvy.ui.settings.components.SettingsGroupCard
import com.lonewolf.wavvy.ui.settings.components.SettingsToggleRow

// Privacy and history management subscreen layout
@Composable
fun PrivacySubScreen(
    onClearPlaybackHistory: () -> Unit,
    onClearSearchHistory: () -> Unit,
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
        SettingsGroupCard(title = stringResource(R.string.setting_subgroup_playback_history)) {
            SettingsToggleRow(
                title = stringResource(R.string.setting_pause_playback_history),
                subtitle = stringResource(R.string.setting_pause_playback_history_desc),
                icon = Icons.Rounded.HistoryToggleOff,
                showDivider = true
            )
            SettingsActionRow(
                title = stringResource(R.string.setting_clear_playback_history),
                subtitle = stringResource(R.string.setting_clear_playback_history_desc),
                icon = Icons.Rounded.DeleteSweep,
                showDivider = false,
                onClick = onClearPlaybackHistory
            )
        }

        SettingsGroupCard(title = stringResource(R.string.setting_subgroup_search_history)) {
            SettingsToggleRow(
                title = stringResource(R.string.setting_pause_search_history),
                subtitle = stringResource(R.string.setting_pause_search_history_desc),
                icon = Icons.AutoMirrored.Rounded.ManageSearch,
                showDivider = true
            )
            SettingsActionRow(
                title = stringResource(R.string.setting_clear_search_history),
                subtitle = stringResource(R.string.setting_clear_search_history_desc),
                icon = Icons.Rounded.HistoryEdu,
                showDivider = false,
                onClick = onClearSearchHistory
            )
        }

        SettingsGroupCard(title = stringResource(R.string.setting_subgroup_security)) {
            SettingsToggleRow(
                title = stringResource(R.string.setting_disable_screenshots),
                subtitle = stringResource(R.string.setting_disable_screenshots_desc),
                icon = Icons.Rounded.NoPhotography,
                showDivider = false
            )
        }

        Spacer(modifier = Modifier.height(if (isPlayerActive) 110.dp else 16.dp))
    }
}

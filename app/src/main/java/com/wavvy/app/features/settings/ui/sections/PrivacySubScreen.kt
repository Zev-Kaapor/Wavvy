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
import androidx.compose.material.icons.automirrored.rounded.ManageSearch
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.HistoryEdu
import androidx.compose.material.icons.rounded.HistoryToggleOff
import androidx.compose.material.icons.rounded.NoPhotography
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
import com.wavvy.app.features.settings.ui.components.SettingsActionRow
import com.wavvy.app.features.settings.ui.components.SettingsGroupCard
import com.wavvy.app.features.settings.ui.components.SettingsToggleRow

// Privacy and history management subscreen layout
@Composable
fun PrivacySubScreen(
    onClearPlaybackHistory: () -> Unit,
    onClearSearchHistory: () -> Unit,
    isPlayerActive: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val internalScrollState = rememberScrollState()

    // Initialize persistence driver
    val storage = remember { SettingsStorage(context) }

    // Read initial values from storage
    var pausePlaybackHistory by remember {
        mutableStateOf(storage.getBoolean("pref_pause_playback_history", false))
    }
    var pauseSearchHistory by remember {
        mutableStateOf(storage.getBoolean("pref_pause_search_history", false))
    }
    var disableScreenshots by remember {
        mutableStateOf(storage.getBoolean("pref_disable_screenshots", false))
    }

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
                checked = pausePlaybackHistory,
                onCheckedChange = { newValue ->
                    pausePlaybackHistory = newValue
                    storage.saveBoolean("pref_pause_playback_history", newValue)
                },
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
                checked = pauseSearchHistory,
                onCheckedChange = { newValue ->
                    pauseSearchHistory = newValue
                    storage.saveBoolean("pref_pause_search_history", newValue)
                },
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
                checked = disableScreenshots,
                onCheckedChange = { newValue ->
                    disableScreenshots = newValue
                    storage.saveBoolean("pref_disable_screenshots", newValue)
                },
                showDivider = false
            )
        }

        Spacer(modifier = Modifier.height(if (isPlayerActive) 110.dp else 16.dp))
    }
}

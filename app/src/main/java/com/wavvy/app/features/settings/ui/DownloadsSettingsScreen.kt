package com.wavvy.app.features.settings.ui

// Android file sizes
import android.text.format.Formatter
// Compose state and runtime
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.remember
// UI styling and utilities
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
// Coroutines
import kotlinx.coroutines.launch
// Project resources
import com.wavvy.app.R
import com.wavvy.app.core.designsystem.components.OverlaySheet
import com.wavvy.app.core.download.DownloadPhase
import com.wavvy.app.core.download.Downloads
import com.wavvy.app.features.playlist.ui.SortChoice
import com.wavvy.app.features.playlist.ui.SortChoiceSheet
import com.wavvy.app.features.settings.data.DownloadLimit
import com.wavvy.app.features.settings.data.AudioLevel
import com.wavvy.app.features.settings.data.DownloadSettings
import com.wavvy.app.features.settings.data.SettingsStore
import com.wavvy.app.features.settings.ui.components.SettingsChoiceRow
import com.wavvy.app.features.settings.ui.components.SettingsDivider
import com.wavvy.app.features.settings.ui.components.SettingsGroup
import com.wavvy.app.features.settings.ui.components.SettingsInfoRow
import com.wavvy.app.features.settings.ui.components.SettingsPage
import com.wavvy.app.features.settings.ui.components.SettingsSwitchRow

// The settings of the downloads, only on Wi-Fi, how good the audio is, how much space they can take and how much they take now
@Composable
fun DownloadsSettingsScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val store = remember { SettingsStore(context) }
    val settings by store.downloads.collectAsState(initial = DownloadSettings())
    val downloads by Downloads.items.collectAsState()
    val used = Formatter.formatShortFileSize(context, downloads.values.filter { it.phase == DownloadPhase.Completed }.sumOf { it.bytes })

    // The choices of a setting open in a sheet, the one in use has the check
    val qualityTitle = stringResource(R.string.settings_downloads_quality)
    val limitTitle = stringResource(R.string.settings_downloads_limit)
    val qualityChoices = AudioLevel.entries.filter { it != AudioLevel.Automatic }.map { quality ->
        SortChoice(stringResource(quality.titleRes), quality == settings.quality, quality == DownloadSettings().quality, stringResource(quality.detailRes)) { scope.launch { store.setDownloadQuality(quality) } }
    }
    val limitChoices = DownloadLimit.entries.map { limit ->
        SortChoice(stringResource(limit.titleRes), limit == settings.limit, limit == DownloadSettings().limit) { scope.launch { store.setDownloadLimit(limit) } }
    }

    SettingsPage(title = stringResource(R.string.settings_downloads), onBack = onBack, modifier = modifier) {
        SettingsGroup(title = stringResource(R.string.settings_group_network)) {
            SettingsSwitchRow(
                title = stringResource(R.string.settings_downloads_wifi),
                subtitle = stringResource(R.string.settings_downloads_wifi_subtitle),
                checked = settings.wifiOnly,
                onCheckedChange = { scope.launch { store.setDownloadWifiOnly(it) } }
            )
        }

        SettingsGroup(title = stringResource(R.string.settings_group_quality)) {
            SettingsChoiceRow(
                title = qualityTitle,
                value = stringResource(settings.quality.titleRes),
                onClick = { OverlaySheet.show { SortChoiceSheet(title = qualityTitle, choices = qualityChoices, onDismiss = OverlaySheet::dismiss) } }
            )
            SettingsDivider()
            SettingsChoiceRow(
                title = limitTitle,
                value = stringResource(settings.limit.titleRes),
                onClick = { OverlaySheet.show { SortChoiceSheet(title = limitTitle, choices = limitChoices, onDismiss = OverlaySheet::dismiss) } }
            )
            SettingsDivider()
            SettingsInfoRow(title = stringResource(R.string.settings_downloads_used), value = used)
        }
    }
}

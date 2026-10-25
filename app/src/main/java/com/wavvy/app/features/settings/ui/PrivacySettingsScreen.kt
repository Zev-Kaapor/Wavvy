package com.wavvy.app.features.settings.ui

// Compose state and runtime
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
// UI styling and utilities
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
// Coroutines
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
// Project resources
import com.wavvy.app.R
import com.wavvy.app.core.designsystem.components.ConfirmDialog
import com.wavvy.app.core.designsystem.components.Toasts
import com.wavvy.app.core.history.PlayHistory
import com.wavvy.app.core.navigation.ItemNavigator
import com.wavvy.app.features.profile.ui.LocalProfile
import com.wavvy.app.features.settings.data.PrivacySettings
import com.wavvy.app.features.settings.data.SettingsStore
import com.wavvy.app.features.settings.ui.components.SettingsActionRow
import com.wavvy.app.features.settings.ui.components.SettingsDivider
import com.wavvy.app.features.settings.ui.components.SettingsGroup
import com.wavvy.app.features.settings.ui.components.SettingsPage
import com.wavvy.app.features.settings.ui.components.SettingsSwitchRow

// The privacy, whether what is listened to is kept and sent to the account, the history itself and the ways to clear what was kept
@Composable
fun PrivacySettingsScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val store = remember { SettingsStore(context) }
    val settings by store.privacy.collectAsState(initial = PrivacySettings())
    var isClearingHistory by remember { mutableStateOf(false) }
    var isClearingSearches by remember { mutableStateOf(false) }
    val cleared = stringResource(R.string.settings_storage_cleared)

    // The history that goes to the account and comes from it cannot be cleared at once, YouTube takes the songs out one by one
    val isSynced = LocalProfile.current.isSignedIn && settings.syncToAccount && !settings.pauseHistory

    // The searches of the account are the ones of the history while signed in, so they are taken out one by one too
    val isSearchSynced = LocalProfile.current.isSignedIn && !settings.pauseSearchHistory

    if (isClearingHistory) {
        ConfirmDialog(
            title = stringResource(R.string.history_clear_title),
            text = stringResource(R.string.history_clear_text),
            confirmLabel = stringResource(R.string.history_clear_confirm),
            cancelLabel = stringResource(R.string.playlist_cancel),
            onConfirm = {
                isClearingHistory = false
                scope.launch(Dispatchers.IO) { PlayHistory.clearHistory(context) }
                Toasts.show(cleared)
            },
            onDismiss = { isClearingHistory = false }
        )
    }
    if (isClearingSearches) {
        ConfirmDialog(
            title = stringResource(R.string.settings_privacy_clear_searches_title),
            text = stringResource(R.string.settings_privacy_clear_searches_text),
            confirmLabel = stringResource(R.string.history_clear_confirm),
            cancelLabel = stringResource(R.string.playlist_cancel),
            onConfirm = {
                isClearingSearches = false
                scope.launch(Dispatchers.IO) { PlayHistory.clearSearches(context) }
                Toasts.show(cleared)
            },
            onDismiss = { isClearingSearches = false }
        )
    }

    SettingsPage(title = stringResource(R.string.settings_privacy), onBack = onBack, modifier = modifier) {
        SettingsGroup(title = stringResource(R.string.settings_group_sync)) {
            SettingsSwitchRow(
                title = stringResource(R.string.settings_privacy_sync),
                subtitle = stringResource(R.string.settings_privacy_sync_subtitle),
                checked = settings.syncToAccount,
                onCheckedChange = { scope.launch { store.setSyncToAccount(it) } }
            )
            SettingsDivider()
            SettingsSwitchRow(
                title = stringResource(R.string.settings_privacy_pause),
                subtitle = stringResource(R.string.settings_privacy_pause_subtitle),
                checked = settings.pauseHistory,
                onCheckedChange = { scope.launch { store.setPauseHistory(it) } }
            )
            SettingsDivider()
            SettingsSwitchRow(
                title = stringResource(R.string.settings_privacy_pause_searches),
                subtitle = stringResource(R.string.settings_privacy_pause_searches_subtitle),
                checked = settings.pauseSearchHistory,
                onCheckedChange = { scope.launch { store.setPauseSearchHistory(it) } }
            )
        }

        SettingsGroup(title = stringResource(R.string.settings_group_screen)) {
            SettingsSwitchRow(
                title = stringResource(R.string.settings_privacy_screenshots),
                subtitle = stringResource(R.string.settings_privacy_screenshots_subtitle),
                checked = settings.blockScreenshots,
                onCheckedChange = { scope.launch { store.setBlockScreenshots(it) } }
            )
        }

        SettingsGroup(title = stringResource(R.string.settings_group_history)) {
            SettingsActionRow(
                title = stringResource(R.string.history_title),
                subtitle = stringResource(R.string.settings_privacy_history_subtitle),
                onClick = { ItemNavigator.openHistory() }
            )
            SettingsDivider()
            SettingsActionRow(
                title = stringResource(R.string.history_clear_title),
                subtitle = stringResource(if (isSynced) R.string.settings_privacy_clear_history_synced else R.string.settings_privacy_clear_history_subtitle),
                isEnabled = !isSynced,
                onClick = { isClearingHistory = true }
            )
            SettingsDivider()
            SettingsActionRow(
                title = stringResource(R.string.settings_privacy_clear_searches),
                subtitle = stringResource(if (isSearchSynced) R.string.settings_privacy_clear_searches_synced else R.string.settings_privacy_clear_searches_subtitle),
                isEnabled = !isSearchSynced,
                onClick = { isClearingSearches = true }
            )
        }
    }
}

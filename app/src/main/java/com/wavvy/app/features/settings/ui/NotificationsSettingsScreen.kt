package com.wavvy.app.features.settings.ui

// Android settings of the system
import android.content.Intent
import android.provider.Settings
// Compose state and runtime
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
// UI styling and utilities
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
// Coroutines
import kotlinx.coroutines.launch
// Project resources
import com.wavvy.app.R
import com.wavvy.app.core.designsystem.components.OverlaySheet
import com.wavvy.app.features.playlist.ui.SortChoice
import com.wavvy.app.features.playlist.ui.SortChoiceSheet
import com.wavvy.app.features.settings.data.NotificationSettings
import com.wavvy.app.features.settings.data.ReleaseInterval
import com.wavvy.app.features.settings.data.SettingsStore
import com.wavvy.app.features.settings.ui.components.SettingsActionRow
import com.wavvy.app.features.settings.ui.components.SettingsCard
import com.wavvy.app.features.settings.ui.components.SettingsChoiceRow
import com.wavvy.app.features.settings.ui.components.SettingsDivider
import com.wavvy.app.features.settings.ui.components.SettingsPage
import com.wavvy.app.features.settings.ui.components.SettingsSwitchRow

// The settings of the notifications, whether the new releases of the artists followed are looked for, how often, and the door to the permissions of the system
@Composable
fun NotificationsSettingsScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val store = remember { SettingsStore(context) }
    val settings by store.notifications.collectAsState(initial = NotificationSettings())

    val intervalTitle = stringResource(R.string.settings_notifications_interval)
    val intervalChoices = ReleaseInterval.entries.map { interval ->
        SortChoice(stringResource(interval.titleRes), interval == settings.interval) { scope.launch { store.setReleaseInterval(interval) } }
    }

    SettingsPage(title = stringResource(R.string.settings_notifications), onBack = onBack, modifier = modifier) {
        SettingsCard {
            SettingsSwitchRow(
                title = stringResource(R.string.settings_notifications_releases),
                subtitle = stringResource(R.string.settings_notifications_releases_subtitle),
                checked = settings.enabled,
                onCheckedChange = { scope.launch { store.setReleasesEnabled(it) } }
            )
            SettingsDivider()
            SettingsChoiceRow(
                title = intervalTitle,
                value = stringResource(settings.interval.titleRes),
                onClick = { OverlaySheet.show { SortChoiceSheet(title = intervalTitle, choices = intervalChoices, onDismiss = OverlaySheet::dismiss) } }
            )
        }

        SettingsCard {
            SettingsActionRow(
                title = stringResource(R.string.settings_notifications_system),
                subtitle = stringResource(R.string.settings_notifications_system_subtitle),
                onClick = {
                    context.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName))
                }
            )
        }
    }
}

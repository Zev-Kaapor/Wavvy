package com.wavvy.app.features.settings.ui

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
import com.wavvy.app.core.language.AppLanguage
import com.wavvy.app.features.playlist.ui.SortChoice
import com.wavvy.app.features.playlist.ui.SortChoiceSheet
import com.wavvy.app.features.settings.ui.components.SettingsChoiceRow
import com.wavvy.app.features.settings.data.ContentSettings
import com.wavvy.app.features.settings.data.SettingsStore
import com.wavvy.app.features.settings.ui.components.SettingsCard
import com.wavvy.app.features.settings.ui.components.SettingsPage
import com.wavvy.app.features.settings.ui.components.SettingsSwitchRow

// The settings of the content, for now whether videos show in the Home and in the queue
@Composable
fun ContentSettingsScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val store = remember { SettingsStore(context) }
    val settings by store.content.collectAsState(initial = ContentSettings())

    // The language of the app, the one of the device or one the user picks, which the system keeps and the screen restarts with
    val languageTitle = stringResource(R.string.settings_language)
    val language = AppLanguage.current(context)
    val languageChoices = AppLanguage.entries.map { option ->
        SortChoice(option.titleRes?.let { stringResource(it) } ?: option.nativeName.orEmpty(), option == language) { AppLanguage.apply(context, option) }
    }

    SettingsPage(title = stringResource(R.string.settings_content), onBack = onBack, modifier = modifier) {
        SettingsCard {
            SettingsChoiceRow(
                title = languageTitle,
                value = language.titleRes?.let { stringResource(it) } ?: language.nativeName.orEmpty(),
                onClick = { OverlaySheet.show { SortChoiceSheet(title = languageTitle, choices = languageChoices, onDismiss = OverlaySheet::dismiss) } }
            )
        }

        SettingsCard {
            SettingsSwitchRow(
                title = stringResource(R.string.settings_content_videos),
                subtitle = stringResource(R.string.settings_content_videos_subtitle),
                checked = settings.showVideos,
                onCheckedChange = { scope.launch { store.setShowVideos(it) } }
            )
        }
    }
}

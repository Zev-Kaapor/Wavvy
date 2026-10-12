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
import com.wavvy.app.core.designsystem.theme.ThemeMode
import com.wavvy.app.features.playlist.ui.SortChoice
import com.wavvy.app.features.playlist.ui.SortChoiceSheet
import com.wavvy.app.features.settings.data.SettingsStore
import com.wavvy.app.features.settings.ui.components.SettingsCard
import com.wavvy.app.features.settings.ui.components.SettingsChoiceRow
import com.wavvy.app.features.settings.ui.components.SettingsPage

// The settings of the looks, for now the theme, the one of the device or always light or always dark
@Composable
fun AppearanceSettingsScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val store = remember { SettingsStore(context) }
    val mode by store.themeMode.collectAsState(initial = ThemeMode.SYSTEM)

    val themeTitle = stringResource(R.string.settings_appearance_theme)
    val choices = ThemeMode.entries.map { option ->
        SortChoice(stringResource(option.titleRes()), option == mode) { scope.launch { store.setThemeMode(option) } }
    }

    SettingsPage(title = stringResource(R.string.settings_appearance), onBack = onBack, modifier = modifier) {
        SettingsCard {
            SettingsChoiceRow(
                title = themeTitle,
                value = stringResource(mode.titleRes()),
                onClick = { OverlaySheet.show { SortChoiceSheet(title = themeTitle, choices = choices, onDismiss = OverlaySheet::dismiss) } }
            )
        }
    }
}

// The words that name each theme
private fun ThemeMode.titleRes(): Int = when (this) {
    ThemeMode.SYSTEM -> R.string.settings_appearance_theme_system
    ThemeMode.LIGHT -> R.string.settings_appearance_theme_light
    ThemeMode.DARK -> R.string.settings_appearance_theme_dark
}

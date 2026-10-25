package com.wavvy.app.features.settings.ui

// Compose layouts and foundations
import androidx.compose.foundation.layout.padding
// Material 3 components
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import com.wavvy.app.core.navigation.MainTab
import com.wavvy.app.features.playlist.ui.SortChoice
import com.wavvy.app.features.playlist.ui.SortChoiceSheet
import com.wavvy.app.features.settings.data.LibrarySettings
import com.wavvy.app.features.settings.data.SettingsStore
import com.wavvy.app.features.settings.ui.components.SettingsChoiceRow
import com.wavvy.app.features.settings.ui.components.SettingsDivider
import com.wavvy.app.features.settings.ui.components.SettingsGroup
import com.wavvy.app.features.settings.ui.components.SettingsSwitchRow
import com.wavvy.app.features.settings.ui.components.SettingsPage

// The settings of the looks, the theme, the one of the device or always light or always dark, and the lists of the library that are hidden
@Composable
fun AppearanceSettingsScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val store = remember { SettingsStore(context) }
    val mode by store.themeMode.collectAsState(initial = ThemeMode.SYSTEM)
    val library by store.library.collectAsState(initial = LibrarySettings())
    val defaultTab by store.defaultTab.collectAsState(initial = null)
    val tab = MainTab.entries.firstOrNull { it.name == defaultTab } ?: MainTab.HOME

    val tabTitle = stringResource(R.string.settings_appearance_default_tab)
    val tabChoices = MainTab.entries.map { option ->
        SortChoice(stringResource(option.labelRes), option == tab, option == MainTab.HOME) { scope.launch { store.setDefaultTab(option.name) } }
    }

    val themeTitle = stringResource(R.string.settings_appearance_theme)
    val choices = ThemeMode.entries.map { option ->
        SortChoice(stringResource(option.titleRes()), option == mode, option == ThemeMode.SYSTEM) { scope.launch { store.setThemeMode(option) } }
    }

    SettingsPage(title = stringResource(R.string.settings_appearance), onBack = onBack, modifier = modifier) {
        SettingsGroup(title = stringResource(R.string.settings_group_general)) {
            SettingsChoiceRow(
                title = themeTitle,
                value = stringResource(mode.titleRes()),
                onClick = { OverlaySheet.show { SortChoiceSheet(title = themeTitle, choices = choices, onDismiss = OverlaySheet::dismiss) } }
            )
            SettingsDivider()
            SettingsChoiceRow(
                title = tabTitle,
                value = stringResource(tab.labelRes),
                onClick = { OverlaySheet.show { SortChoiceSheet(title = tabTitle, choices = tabChoices, onDismiss = OverlaySheet::dismiss) } }
            )
        }

        // The lists that YouTube Music makes by itself, the user chooses which ones the library shows
        SettingsGroup(title = stringResource(R.string.settings_appearance_library)) {
            SettingsSwitchRow(
                title = stringResource(R.string.settings_appearance_hide_liked),
                subtitle = stringResource(R.string.settings_appearance_hide_liked_subtitle),
                checked = library.hideLiked,
                onCheckedChange = { scope.launch { store.setHideLiked(it) } }
            )
            SettingsDivider()
            SettingsSwitchRow(
                title = stringResource(R.string.settings_appearance_hide_episodes),
                subtitle = stringResource(R.string.settings_appearance_hide_episodes_subtitle),
                checked = library.hideEpisodes,
                onCheckedChange = { scope.launch { store.setHideEpisodes(it) } }
            )
            SettingsDivider()
            SettingsSwitchRow(
                title = stringResource(R.string.settings_appearance_hide_watch_later),
                subtitle = stringResource(R.string.settings_appearance_hide_watch_later_subtitle),
                checked = library.hideWatchLater,
                onCheckedChange = { scope.launch { store.setHideWatchLater(it) } }
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

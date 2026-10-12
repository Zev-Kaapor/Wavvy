package com.wavvy.app.features.settings.ui

// Compose state and runtime
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
// UI styling and utilities
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
// Names of the countries
import java.util.Locale
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
import com.wavvy.app.features.settings.ui.components.SettingsDivider
import com.wavvy.app.features.settings.ui.components.SettingsPage
import com.wavvy.app.features.settings.ui.components.SettingsSwitchRow

// The settings of the content, the language and the country, whether videos show in the Home and in the queue, explicit songs and Shorts
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

    // The country the content comes from, the one of the device or one the user picks, in the names of the language of the app
    val countryTitle = stringResource(R.string.settings_content_country)
    val deviceCountry = stringResource(R.string.settings_content_country_device)
    val appLocale = LocalConfiguration.current.locales[0]
    val countries = remember(appLocale) {
        Locale.getISOCountries().map { it to Locale.Builder().setRegion(it).build().getDisplayCountry(appLocale) }.sortedBy { it.second.lowercase() }
    }
    val countryChoices = listOf(SortChoice(deviceCountry, settings.country == null) { scope.launch { store.setCountry(null) } }) +
        countries.map { (code, name) -> SortChoice(name, code == settings.country) { scope.launch { store.setCountry(code) } } }

    SettingsPage(title = stringResource(R.string.settings_content), onBack = onBack, modifier = modifier) {
        SettingsCard {
            SettingsChoiceRow(
                title = languageTitle,
                value = language.titleRes?.let { stringResource(it) } ?: language.nativeName.orEmpty(),
                onClick = { OverlaySheet.show { SortChoiceSheet(title = languageTitle, choices = languageChoices, onDismiss = OverlaySheet::dismiss) } }
            )
            SettingsDivider()
            SettingsChoiceRow(
                title = countryTitle,
                value = countries.firstOrNull { it.first == settings.country }?.second ?: deviceCountry,
                onClick = { OverlaySheet.show { SortChoiceSheet(title = countryTitle, choices = countryChoices, onDismiss = OverlaySheet::dismiss) } }
            )
        }

        SettingsCard {
            SettingsSwitchRow(
                title = stringResource(R.string.settings_content_videos),
                subtitle = stringResource(R.string.settings_content_videos_subtitle),
                checked = settings.showVideos,
                onCheckedChange = { scope.launch { store.setShowVideos(it) } }
            )
            SettingsDivider()
            SettingsSwitchRow(
                title = stringResource(R.string.settings_content_explicit),
                subtitle = stringResource(R.string.settings_content_explicit_subtitle),
                checked = settings.hideExplicit,
                onCheckedChange = { scope.launch { store.setHideExplicit(it) } }
            )
            SettingsDivider()
            SettingsSwitchRow(
                title = stringResource(R.string.settings_content_shorts),
                subtitle = stringResource(R.string.settings_content_shorts_subtitle),
                checked = settings.hideShorts,
                onCheckedChange = { scope.launch { store.setHideShorts(it) } }
            )
        }
    }
}

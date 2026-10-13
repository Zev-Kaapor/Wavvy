package com.wavvy.app.features.settings.ui

// Compose state and runtime
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
// UI styling and utilities
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import com.wavvy.app.core.designsystem.components.Toasts
import com.wavvy.app.core.innertube.InnerTubeClient
import com.wavvy.app.core.innertube.currentSession
import com.wavvy.app.core.language.AppLanguage
import com.wavvy.app.features.playlist.ui.SortChoice
import com.wavvy.app.features.playlist.ui.SortChoiceSheet
import com.wavvy.app.features.settings.ui.components.SettingsChoiceRow
import com.wavvy.app.features.settings.data.ContentSettings
import com.wavvy.app.features.settings.data.ProxySettings
import com.wavvy.app.features.settings.data.SettingsStore
import com.wavvy.app.features.settings.ui.components.SettingsCard
import com.wavvy.app.features.settings.ui.components.SettingsDivider
import com.wavvy.app.features.settings.ui.components.SettingsPage
import com.wavvy.app.features.settings.ui.components.SettingsSwitchRow

// The switch of the account that asks YouTube Music not to play music videos, which the server follows when it answers
private const val DontPlayVideosId = "312"

// The settings of the content, the language and the country, the proxy, whether videos show in the Home and in the queue, explicit songs and Shorts
@Composable
fun ContentSettingsScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val syncFailed = stringResource(R.string.settings_content_videos_sync_failed)
    val store = remember { SettingsStore(context) }
    val settings by store.content.collectAsState(initial = ContentSettings())
    val proxy by store.proxy.collectAsState(initial = ProxySettings())
    val mirrorSave by store.mirrorSave.collectAsState(initial = false)
    var isSettingProxy by rememberSaveable { mutableStateOf(false) }

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

    if (isSettingProxy) {
        ProxyDialog(
            initial = proxy,
            onSave = {
                isSettingProxy = false
                scope.launch { store.setProxy(it.copy(enabled = true)) }
            },
            onDismiss = { isSettingProxy = false }
        )
    }

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
                onCheckedChange = { value ->
                    scope.launch {
                        // The account is told first, so the Home that follows the change already gets the songs
                        val session = currentSession(context)
                        if (session.cookies != null) {
                            InnerTubeClient.setAccountSetting(session, DontPlayVideosId, !value)
                                .onFailure { Toasts.show(syncFailed, subtitle = it.message, isError = true) }
                        }
                        store.setShowVideos(value)
                    }
                }
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

        // How several songs are saved to a playlist
        Text(
            text = stringResource(R.string.settings_playlists_group),
            style = MaterialTheme.typography.labelLarge.merge(SettingsType.GroupTitle),
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.padding(start = SettingsDimens.GroupTitleStart)
        )
        SettingsCard {
            SettingsSwitchRow(
                title = stringResource(R.string.settings_mirror_save),
                subtitle = stringResource(R.string.settings_mirror_save_subtitle),
                checked = mirrorSave,
                onCheckedChange = { scope.launch { store.setMirrorSave(it) } }
            )
        }

        // The proxy, for places where YouTube Music is blocked, a switch with no address set asks for it first
        Text(
            text = stringResource(R.string.settings_proxy_group),
            style = MaterialTheme.typography.labelLarge.merge(SettingsType.GroupTitle),
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.padding(start = SettingsDimens.GroupTitleStart)
        )
        SettingsCard {
            SettingsSwitchRow(
                title = stringResource(R.string.settings_proxy_enable),
                subtitle = stringResource(R.string.settings_proxy_enable_subtitle),
                checked = proxy.enabled,
                onCheckedChange = {
                    if (it && proxy.address.isEmpty()) isSettingProxy = true else scope.launch { store.setProxyEnabled(it) }
                }
            )
            if (proxy.enabled) {
                SettingsDivider()
                SettingsChoiceRow(
                    title = stringResource(R.string.settings_proxy_configure),
                    value = "${proxy.kind.label} \u2022 ${proxy.address}",
                    onClick = { isSettingProxy = true }
                )
            }
        }
    }
}

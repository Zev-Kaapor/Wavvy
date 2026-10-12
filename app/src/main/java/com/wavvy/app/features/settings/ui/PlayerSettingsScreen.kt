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
import com.wavvy.app.core.playback.SponsorCategory
import com.wavvy.app.features.playlist.ui.SortChoice
import com.wavvy.app.features.playlist.ui.SortChoiceSheet
import com.wavvy.app.features.settings.data.CrossfadeDurations
import com.wavvy.app.features.settings.data.CrossfadeSettings
import com.wavvy.app.features.settings.data.SettingsStore
import com.wavvy.app.features.settings.data.SponsorBlockSettings
import com.wavvy.app.features.settings.ui.components.SettingsCard
import com.wavvy.app.features.settings.ui.components.SettingsChoiceRow
import com.wavvy.app.features.settings.ui.components.SettingsDivider
import com.wavvy.app.features.settings.ui.components.SettingsPage
import com.wavvy.app.features.settings.ui.components.SettingsSwitchRow

// Lengths under a second are said in milliseconds, the others in seconds
private const val MillisPerSecond = 1000

// The settings of the player, the crossfade that blends a song into the next one and the SponsorBlock that skips the marked parts
@Composable
fun PlayerSettingsScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val store = remember { SettingsStore(context) }
    val settings by store.crossfade.collectAsState(initial = CrossfadeSettings())
    val sponsor by store.sponsorBlock.collectAsState(initial = SponsorBlockSettings())

    val durationTitle = stringResource(R.string.settings_crossfade_duration)
    val durationChoices = CrossfadeDurations.map { duration ->
        SortChoice(durationLabel(duration), duration == settings.durationMs) { scope.launch { store.setCrossfadeDuration(duration) } }
    }

    SettingsPage(title = stringResource(R.string.settings_player), onBack = onBack, modifier = modifier) {
        Text(
            text = stringResource(R.string.settings_crossfade),
            style = MaterialTheme.typography.labelLarge.merge(SettingsType.GroupTitle),
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.padding(start = SettingsDimens.GroupTitleStart)
        )
        SettingsCard {
            SettingsSwitchRow(
                title = stringResource(R.string.settings_crossfade_enable),
                subtitle = stringResource(R.string.settings_crossfade_enable_subtitle),
                checked = settings.enabled,
                onCheckedChange = { scope.launch { store.setCrossfadeEnabled(it) } }
            )
            if (settings.enabled) {
                SettingsDivider()
                SettingsChoiceRow(
                    title = durationTitle,
                    value = durationLabel(settings.durationMs),
                    onClick = { OverlaySheet.show { SortChoiceSheet(title = durationTitle, choices = durationChoices, onDismiss = OverlaySheet::dismiss) } }
                )
                SettingsDivider()
                SettingsSwitchRow(
                    title = stringResource(R.string.settings_crossfade_on_skip),
                    subtitle = stringResource(R.string.settings_crossfade_on_skip_subtitle),
                    checked = settings.onSkip,
                    onCheckedChange = { scope.launch { store.setCrossfadeOnSkip(it) } }
                )
                SettingsDivider()
                SettingsSwitchRow(
                    title = stringResource(R.string.settings_crossfade_on_auto),
                    subtitle = stringResource(R.string.settings_crossfade_on_auto_subtitle),
                    checked = settings.onAutoAdvance,
                    onCheckedChange = { scope.launch { store.setCrossfadeOnAutoAdvance(it) } }
                )
            }
        }

        // What the crossfade does not do
        Text(
            text = stringResource(R.string.settings_crossfade_note),
            style = MaterialTheme.typography.bodyMedium.merge(SettingsType.RowSubtitle),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = SettingsDimens.GroupTitleStart)
        )

        // The parts that the community of SponsorBlock marked, skipped as the user chose
        Text(
            text = stringResource(R.string.settings_sponsorblock),
            style = MaterialTheme.typography.labelLarge.merge(SettingsType.GroupTitle),
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.padding(start = SettingsDimens.GroupTitleStart)
        )
        SettingsCard {
            SettingsSwitchRow(
                title = stringResource(R.string.settings_sponsorblock_enable),
                subtitle = stringResource(R.string.settings_sponsorblock_enable_subtitle),
                checked = sponsor.enabled,
                onCheckedChange = { scope.launch { store.setSponsorBlockEnabled(it) } }
            )
            if (sponsor.enabled) {
                SponsorCategory.entries.forEach { category ->
                    SettingsDivider()
                    SettingsSwitchRow(
                        title = stringResource(category.titleRes),
                        subtitle = null,
                        checked = category in sponsor.categories,
                        onCheckedChange = { scope.launch { store.setSponsorCategory(category, it) } }
                    )
                }
            }
        }
        Text(
            text = stringResource(R.string.settings_sponsorblock_note),
            style = MaterialTheme.typography.bodyMedium.merge(SettingsType.RowSubtitle),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = SettingsDimens.GroupTitleStart)
        )
    }
}

// The length in the words of the language, milliseconds under a second and seconds from there
@Composable
private fun durationLabel(durationMs: Int): String =
    if (durationMs < MillisPerSecond) stringResource(R.string.settings_crossfade_ms, durationMs) else stringResource(R.string.settings_crossfade_s, durationMs / MillisPerSecond)

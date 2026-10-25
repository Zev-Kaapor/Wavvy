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
import kotlinx.coroutines.launch
// Project resources
import com.wavvy.app.R
import com.wavvy.app.core.playback.SponsorCategory
import com.wavvy.app.features.settings.data.SettingsStore
import com.wavvy.app.features.settings.data.SponsorBlockSettings
import com.wavvy.app.features.settings.ui.components.SettingsDivider
import com.wavvy.app.features.settings.ui.components.SettingsGroup
import com.wavvy.app.features.settings.ui.components.SettingsPage
import com.wavvy.app.features.settings.ui.components.SettingsSwitchRow

// The name of this screen in the settings
const val SponsorBlockSection = "SponsorBlock"

// What the line under each kind of part says it is
private val SponsorCategory.subtitleRes: Int
    get() = when (this) {
        SponsorCategory.NonMusic -> R.string.settings_sponsorblock_non_music_subtitle
        SponsorCategory.Sponsor -> R.string.settings_sponsorblock_sponsor_subtitle
        SponsorCategory.SelfPromo -> R.string.settings_sponsorblock_self_promo_subtitle
        SponsorCategory.Interaction -> R.string.settings_sponsorblock_interaction_subtitle
        SponsorCategory.Intro -> R.string.settings_sponsorblock_intro_subtitle
        SponsorCategory.Outro -> R.string.settings_sponsorblock_outro_subtitle
        SponsorCategory.Preview -> R.string.settings_sponsorblock_preview_subtitle
    }

// The kinds of part that SponsorBlock skips, each with a line that says what it is
@Composable
fun SponsorBlockScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val store = remember { SettingsStore(context) }
    val sponsor by store.sponsorBlock.collectAsState(initial = SponsorBlockSettings())

    SettingsPage(title = stringResource(R.string.settings_sponsorblock_categories), onBack = onBack, modifier = modifier) {
        SettingsGroup(title = stringResource(R.string.settings_sponsorblock_categories_group)) {
            SponsorCategory.entries.forEachIndexed { index, category ->
                if (index > 0) SettingsDivider()
                SettingsSwitchRow(
                    title = stringResource(category.titleRes),
                    subtitle = stringResource(category.subtitleRes),
                    checked = category in sponsor.categories,
                    onCheckedChange = { scope.launch { store.setSponsorCategory(category, it) } }
                )
            }
        }
    }
}

package com.wavvy.app.features.settings.ui.sections.content

// Compose foundation and layout
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
// Material 3 icons
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
// Project resources
import com.wavvy.app.R
import com.wavvy.app.features.home.models.QuickPicksSource
import com.wavvy.app.features.settings.ui.SettingsSection
import com.wavvy.app.features.settings.ui.SettingsUiState
import com.wavvy.app.features.settings.ui.components.SettingsGroupCard
import com.wavvy.app.features.settings.ui.components.SettingsInteractiveRow

// Content and feeds filtering subscreen layout
@Composable
fun ContentSubScreen(
    uiState: SettingsUiState,
    onNavigateToSection: (SettingsSection) -> Unit,
    isPlayerActive: Boolean,
    modifier: Modifier = Modifier
) {
    val internalScrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(internalScrollState)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        SettingsGroupCard(title = stringResource(R.string.setting_subgroup_filters)) {
            SettingsInteractiveRow(
                title = stringResource(R.string.setting_content_language),
                subtitle = stringResource(R.string.setting_content_language_global),
                icon = Icons.Rounded.Translate,
                showDivider = true,
                onClick = {}
            )
            SettingsInteractiveRow(
                title = stringResource(R.string.setting_content_country),
                subtitle = stringResource(R.string.setting_content_country_br),
                icon = Icons.Rounded.Place,
                showDivider = true,
                onClick = {}
            )
            SettingsInteractiveRow(
                title = stringResource(R.string.setting_section_quick_picks),
                subtitle = when (uiState.quickPicksSource) {
                    QuickPicksSource.YTMUSIC_API if uiState.isLoggedIn -> stringResource(R.string.setting_section_quick_picks_subtitle)
                    QuickPicksSource.KWORB_CHART -> "Kworb (${uiState.kworbChartConfig.countryCode.uppercase()})"
                    else -> stringResource(R.string.setting_section_quick_picks_subtitle)
                },
                icon = Icons.Rounded.AutoAwesome,
                showDivider = false,
                onClick = { onNavigateToSection(SettingsSection.QUICK_PICKS) }
            )
        }

        Spacer(modifier = Modifier.height(if (isPlayerActive) 110.dp else 16.dp))
    }
}

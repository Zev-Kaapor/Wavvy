package com.lonewolf.wavvy.ui.settings.sections

// Compose foundation and layout
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
// Material 3 icons
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Home
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
// Project resources
import com.lonewolf.wavvy.R
import com.lonewolf.wavvy.ui.settings.components.SettingsGroupCard
import com.lonewolf.wavvy.ui.settings.components.SettingsInteractiveRow

// Visual theme and layout navigation subscreen layout
@Composable
fun AppearanceSubScreen(
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
        SettingsGroupCard(title = stringResource(R.string.setting_subgroup_theme)) {
            SettingsInteractiveRow(
                title = stringResource(R.string.setting_theme),
                subtitle = stringResource(R.string.setting_theme_dark_amoled),
                icon = Icons.Rounded.DarkMode,
                showDivider = false
            )
        }

        SettingsGroupCard(title = stringResource(R.string.setting_subgroup_navigation)) {
            SettingsInteractiveRow(
                title = stringResource(R.string.setting_default_tab),
                subtitle = stringResource(R.string.setting_default_tab_home),
                icon = Icons.Rounded.Home,
                showDivider = false
            )
        }

        Spacer(modifier = Modifier.height(if (isPlayerActive) 110.dp else 16.dp))
    }
}

package com.lonewolf.wavvy.ui.settings.sections

// Compose foundation and layout
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
// Material 3 icons
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.NewReleases
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
// Project resources
import com.lonewolf.wavvy.R
import com.lonewolf.wavvy.ui.settings.components.SettingsGroupCard
import com.lonewolf.wavvy.ui.settings.components.SettingsItemRow

// Application build metadata and diagnostics subscreen layout
@Composable
fun AboutSubScreen(
    appVersion: String,
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
        SettingsGroupCard(title = stringResource(R.string.setting_subgroup_about_app)) {
            SettingsItemRow(
                title = stringResource(R.string.setting_app_version),
                subtitle = appVersion,
                icon = Icons.Rounded.Info,
                showDivider = true,
                onClick = {}
            )
            SettingsItemRow(
                title = stringResource(R.string.setting_changelog),
                subtitle = stringResource(R.string.setting_changelog_desc),
                icon = Icons.Rounded.NewReleases,
                showDivider = false,
                onClick = {}
            )
        }

        Spacer(modifier = Modifier.height(if (isPlayerActive) 110.dp else 16.dp))
    }
}

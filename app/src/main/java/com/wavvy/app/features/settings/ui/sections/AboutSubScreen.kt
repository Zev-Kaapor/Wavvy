package com.wavvy.app.features.settings.ui.sections

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
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.NewReleases
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
// Project resources
import com.wavvy.app.R
import com.wavvy.app.features.settings.ui.components.SettingsGroupCard
import com.wavvy.app.features.settings.ui.components.SettingsItemRow

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

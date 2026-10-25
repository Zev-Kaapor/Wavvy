package com.wavvy.app.features.settings.ui

// Android intents
import android.content.Intent
import androidx.core.net.toUri
// Compose layouts and foundations
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
// Material 3 components
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
// Compose state and runtime
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
// UI styling and utilities
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
// Image loading
import coil3.compose.AsyncImage
// Project resources
import com.wavvy.app.R
import com.wavvy.app.core.navigation.ItemNavigator
import com.wavvy.app.features.settings.ui.components.SettingsActionRow
import com.wavvy.app.features.settings.ui.components.SettingsDivider
import com.wavvy.app.features.settings.ui.components.SettingsGroup
import com.wavvy.app.features.settings.ui.components.SettingsInfoRow
import com.wavvy.app.features.settings.ui.components.SettingsNavigateRow
import com.wavvy.app.features.settings.ui.components.SettingsPage

// About the app, its version, who made it, the license, the pages of the project and the thanks to what inspired it
@Composable
fun AboutSettingsScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val resources = LocalResources.current
    val version = remember { context.packageManager.getPackageInfo(context.packageName, 0).versionName.orEmpty() }
    val open: (Int) -> Unit = { addressRes -> context.startActivity(Intent(Intent.ACTION_VIEW, resources.getString(addressRes).toUri())) }

    SettingsPage(title = stringResource(R.string.settings_about), onBack = onBack, modifier = modifier) {
        SettingsGroup(title = stringResource(R.string.settings_group_the_app)) {
            // The photo and the name of the author, a tap opens their page
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { open(R.string.settings_about_author_address) }
                    .padding(SettingsDimens.RowPadding)
            ) {
                AsyncImage(
                    model = stringResource(R.string.settings_about_author_photo),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(SettingsDimens.AuthorPhoto)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                )
                Column(modifier = Modifier.padding(start = SettingsDimens.RowIconGap)) {
                    Text(
                        text = stringResource(R.string.settings_about_author_label),
                        style = MaterialTheme.typography.bodyMedium.merge(SettingsType.RowSubtitle),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = stringResource(R.string.settings_about_author_name),
                        style = MaterialTheme.typography.bodyLarge.merge(SettingsType.RowTitle),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            SettingsDivider()
            SettingsInfoRow(title = stringResource(R.string.settings_about_version), value = version)
        }

        SettingsGroup(title = stringResource(R.string.settings_group_project)) {
            SettingsActionRow(
                title = stringResource(R.string.settings_about_source),
                subtitle = stringResource(R.string.settings_about_source_subtitle),
                onClick = { open(R.string.settings_about_source_address) }
            )
            SettingsDivider()
            SettingsNavigateRow(
                title = stringResource(R.string.settings_about_releases),
                subtitle = stringResource(R.string.settings_about_releases_subtitle),
                onClick = { ItemNavigator.openSettingsSection(UpdatesSection) }
            )
            SettingsDivider()
            SettingsActionRow(
                title = stringResource(R.string.settings_about_license),
                subtitle = stringResource(R.string.settings_about_license_subtitle),
                onClick = { open(R.string.settings_about_license_address) }
            )
        }

        SettingsGroup(title = stringResource(R.string.settings_about_thanks)) {
            SettingsNavigateRow(
                title = stringResource(R.string.settings_about_thanks_special),
                subtitle = stringResource(R.string.settings_about_thanks_special_subtitle),
                onClick = { ItemNavigator.openSettingsSection(ThanksSection) }
            )
        }
    }
}

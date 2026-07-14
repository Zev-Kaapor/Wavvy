package com.lonewolf.wavvy.ui.settings

// Compose foundation and layout
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.verticalScroll
// Material 3 and icons
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
// Project resources
import com.lonewolf.wavvy.R
import com.lonewolf.wavvy.ui.settings.components.SettingsGroupCard
import com.lonewolf.wavvy.ui.settings.components.SettingsItemRow

// Main category list for settings home screen layout
@Composable
fun MainSettingsList(
    scrollState: ScrollState,
    isPlayerActive: Boolean,
    onNavigateToSection: (SettingsSection) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        SettingsGroupCard(title = stringResource(R.string.setting_group_app)) {
            SettingsItemRow(
                title = stringResource(R.string.setting_section_general),
                subtitle = stringResource(R.string.setting_section_general_subtitle),
                icon = Icons.Rounded.Settings,
                showDivider = true,
                onClick = { onNavigateToSection(SettingsSection.GENERAL) }
            )
            SettingsItemRow(
                title = stringResource(R.string.setting_section_appearance),
                subtitle = stringResource(R.string.setting_section_appearance_subtitle),
                icon = Icons.Rounded.Palette,
                showDivider = false,
                onClick = { onNavigateToSection(SettingsSection.APPEARANCE) }
            )
        }

        SettingsGroupCard(title = stringResource(R.string.setting_group_playback)) {
            SettingsItemRow(
                title = stringResource(R.string.setting_section_player),
                subtitle = stringResource(R.string.setting_section_player_subtitle),
                icon = Icons.Rounded.MusicNote,
                showDivider = true,
                onClick = { onNavigateToSection(SettingsSection.PLAYER) }
            )
            SettingsItemRow(
                title = stringResource(R.string.setting_section_content),
                subtitle = stringResource(R.string.setting_section_content_subtitle),
                icon = Icons.Rounded.Tune,
                showDivider = false,
                onClick = { onNavigateToSection(SettingsSection.CONTENT) }
            )
        }

        SettingsGroupCard(title = stringResource(R.string.setting_group_data_security)) {
            SettingsItemRow(
                title = stringResource(R.string.setting_section_privacy),
                subtitle = stringResource(R.string.setting_section_privacy_subtitle),
                icon = Icons.Rounded.Security,
                showDivider = true,
                onClick = { onNavigateToSection(SettingsSection.PRIVACY) }
            )
            SettingsItemRow(
                title = stringResource(R.string.setting_section_storage),
                subtitle = stringResource(R.string.setting_section_storage_subtitle),
                icon = Icons.Rounded.Storage,
                showDivider = true,
                onClick = { onNavigateToSection(SettingsSection.STORAGE) }
            )
            SettingsItemRow(
                title = stringResource(R.string.setting_section_backup),
                subtitle = stringResource(R.string.setting_section_backup_subtitle),
                icon = Icons.Rounded.CloudUpload,
                showDivider = false,
                onClick = { onNavigateToSection(SettingsSection.BACKUP) }
            )
        }

        SettingsGroupCard(title = stringResource(R.string.setting_group_more)) {
            SettingsItemRow(
                title = stringResource(R.string.setting_section_links),
                subtitle = stringResource(R.string.setting_section_links_subtitle),
                icon = Icons.Rounded.Link,
                showDivider = true,
                onClick = { onNavigateToSection(SettingsSection.LINKS) }
            )
            SettingsItemRow(
                title = stringResource(R.string.setting_section_about),
                subtitle = stringResource(R.string.setting_section_about_subtitle),
                icon = Icons.Rounded.Info,
                showDivider = false,
                onClick = { onNavigateToSection(SettingsSection.ABOUT) }
            )
        }

        Spacer(modifier = Modifier.height(if (isPlayerActive) 110.dp else 40.dp))
    }
}

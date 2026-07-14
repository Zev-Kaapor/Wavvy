package com.lonewolf.wavvy.ui.settings.sections

// Compose foundation and layout
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
// Material 3 icons
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.RestorePage
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
// Project resources
import com.lonewolf.wavvy.R
import com.lonewolf.wavvy.ui.settings.components.SettingsActionRow
import com.lonewolf.wavvy.ui.settings.components.SettingsGroupCard

// Local backup and migration utilities subscreen layout
@Composable
fun BackupSubScreen(
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
        SettingsGroupCard(title = stringResource(R.string.setting_subgroup_backup_management)) {
            SettingsActionRow(title = stringResource(R.string.setting_backup_create), subtitle = stringResource(R.string.setting_backup_create_desc), icon = Icons.Rounded.Backup, showDivider = true, onClick = {})
            SettingsActionRow(title = stringResource(R.string.setting_backup_restore), subtitle = stringResource(R.string.setting_backup_restore_desc), icon = Icons.Rounded.RestorePage, showDivider = false, onClick = {})
        }

        SettingsGroupCard(title = stringResource(R.string.setting_subgroup_external_import)) {
            SettingsActionRow(title = stringResource(R.string.setting_import_playlist), subtitle = stringResource(R.string.setting_import_playlist_desc), icon = Icons.Rounded.FileDownload, showDivider = false, onClick = {})
        }

        Spacer(modifier = Modifier.height(if (isPlayerActive) 110.dp else 16.dp))
    }
}

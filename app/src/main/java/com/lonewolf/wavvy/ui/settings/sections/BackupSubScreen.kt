package com.lonewolf.wavvy.ui.settings.sections

// Activity Result Contracts
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
// Compose foundation and layout
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
// Material 3 icons
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.FileUpload
import androidx.compose.material.icons.rounded.RestorePage
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
// Project resources
import com.lonewolf.wavvy.R
import com.lonewolf.wavvy.data.SettingsBackupHelper
import com.lonewolf.wavvy.ui.common.components.ToastData
import com.lonewolf.wavvy.ui.settings.components.SettingsActionRow
import com.lonewolf.wavvy.ui.settings.components.SettingsGroupCard
import com.lonewolf.wavvy.ui.theme.MusicStateColors

// Local backup and migration utilities subscreen layout
@Composable
fun BackupSubScreen(
    onShowToast: (ToastData) -> Unit,
    isPlayerActive: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val internalScrollState = rememberScrollState()

    // Theme color mappings
    val successColor = MusicStateColors.downloaded

    // String resources resolved for success notifications
    val successExportTitle = stringResource(R.string.setting_backup_export_success)
    val successExportDesc = stringResource(R.string.setting_backup_export_success_desc)
    val successImportTitle = stringResource(R.string.setting_backup_import_success)
    val successImportDesc = stringResource(R.string.setting_backup_import_success_desc)

    // String resources resolved for error notifications
    val errorExportTitle = stringResource(R.string.setting_backup_export_error)
    val errorExportDesc = stringResource(R.string.setting_backup_export_error_desc)
    val errorImportTitle = stringResource(R.string.setting_backup_import_error)
    val errorImportDesc = stringResource(R.string.setting_backup_import_error_desc)

    // Activity launcher for creating a backup file (Export)
    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            val jsonConfig = SettingsBackupHelper.exportSettingsToJson(context)
            val isSuccess = if (jsonConfig != null) {
                SettingsBackupHelper.writeTextToUri(context, uri, jsonConfig)
            } else {
                false
            }

            if (isSuccess) {
                onShowToast(
                    ToastData(
                        message = successExportTitle,
                        subtitle = successExportDesc
                    )
                )
            } else {
                onShowToast(
                    ToastData(
                        message = errorExportTitle,
                        subtitle = errorExportDesc
                    )
                )
            }
        }
    }

    // Activity launcher for selecting an existing backup file (Import)
    val openDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            val jsonConfig = SettingsBackupHelper.readTextFromUri(context, uri)
            val isSuccess = if (jsonConfig != null) {
                SettingsBackupHelper.importSettingsFromJson(context, jsonConfig)
            } else {
                false
            }

            if (isSuccess) {
                onShowToast(
                    ToastData(
                        message = successImportTitle,
                        subtitle = successImportDesc
                    )
                )
            } else {
                onShowToast(
                    ToastData(
                        message = errorImportTitle,
                        subtitle = errorImportDesc
                    )
                )
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(internalScrollState)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        SettingsGroupCard(title = stringResource(R.string.setting_subgroup_backup_management)) {
            SettingsActionRow(
                title = stringResource(R.string.setting_backup_create),
                subtitle = stringResource(R.string.setting_backup_create_desc),
                icon = Icons.Rounded.Backup,
                tint = successColor,
                showDivider = true,
                onClick = {
                    val fileName = SettingsBackupHelper.generateBackupFileName()
                    createDocumentLauncher.launch(fileName)
                }
            )
            SettingsActionRow(
                title = stringResource(R.string.setting_backup_restore),
                subtitle = stringResource(R.string.setting_backup_restore_desc),
                icon = Icons.Rounded.RestorePage,
                showDivider = false,
                onClick = {
                    openDocumentLauncher.launch(arrayOf("application/json", "application/octet-stream", "*/*"))
                }
            )
        }

        SettingsGroupCard(title = stringResource(R.string.setting_subgroup_external_import)) {
            SettingsActionRow(
                title = stringResource(R.string.setting_export_playlist),
                subtitle = stringResource(R.string.setting_export_playlist_desc),
                icon = Icons.Rounded.FileUpload,
                tint = successColor,
                showDivider = true,
                onClick = {}
            )
            SettingsActionRow(
                title = stringResource(R.string.setting_import_playlist),
                subtitle = stringResource(R.string.setting_import_playlist_desc),
                icon = Icons.Rounded.FileDownload,
                showDivider = false,
                onClick = {}
            )
        }

        Spacer(modifier = Modifier.height(if (isPlayerActive) 110.dp else 16.dp))
    }
}

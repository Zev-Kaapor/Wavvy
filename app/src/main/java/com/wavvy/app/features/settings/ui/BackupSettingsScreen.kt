package com.wavvy.app.features.settings.ui

// Compose state and runtime
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
// UI styling and utilities
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
// Project resources
import com.wavvy.app.R
import com.wavvy.app.core.drive.DriveState
import com.wavvy.app.core.drive.DriveStatus
import com.wavvy.app.core.drive.DriveStore
import com.wavvy.app.core.navigation.ItemNavigator
import com.wavvy.app.features.settings.ui.components.SettingsCard
import com.wavvy.app.features.settings.ui.components.SettingsNavigateRow
import com.wavvy.app.features.settings.ui.components.SettingsPage
import com.wavvy.app.features.settings.ui.components.StatusDot
import com.wavvy.app.features.settings.ui.components.StatusKind

// The ways to keep a backup, the Drive of the user and the file made by hand, the file is out of reach while the Drive takes care of the backup
@Composable
fun BackupSettingsScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val status by DriveState.status.collectAsState()
    val isConnected by remember { DriveStore(context).isConnected }.collectAsState(initial = false)

    LaunchedEffect(Unit) { DriveState.refresh(context) }

    SettingsPage(title = stringResource(R.string.settings_backup), onBack = onBack, modifier = modifier) {
        SettingsCard {
            SettingsNavigateRow(
                title = stringResource(R.string.backup_drive),
                subtitle = stringResource(R.string.backup_drive_subtitle),
                titleAccessory = {
                    StatusDot(
                        when {
                            status == DriveStatus.Failed -> StatusKind.Error
                            isConnected -> StatusKind.On
                            else -> StatusKind.Off
                        }
                    )
                },
                onClick = { ItemNavigator.openSettingsSection(BackupDriveSection) }
            )
        }
        SettingsCard {
            SettingsNavigateRow(
                title = stringResource(R.string.backup_manual),
                subtitle = stringResource(if (isConnected) R.string.backup_manual_by_drive else R.string.backup_manual_subtitle),
                isEnabled = !isConnected,
                onClick = { ItemNavigator.openSettingsSection(BackupManualSection) }
            )
        }
    }
}

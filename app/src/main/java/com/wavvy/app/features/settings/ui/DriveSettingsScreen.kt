package com.wavvy.app.features.settings.ui

// Android text and messages
import android.text.format.Formatter
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
// Compose state and runtime
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
// UI styling and utilities
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
// Time and coroutines
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
// Project resources
import com.wavvy.app.R
import com.wavvy.app.core.backup.BackupCrypto
import com.wavvy.app.core.backup.BackupData
import com.wavvy.app.core.backup.BackupFailure
import com.wavvy.app.core.backup.BackupSection
import com.wavvy.app.core.designsystem.components.ConfirmDialog
import com.wavvy.app.core.designsystem.components.OverlaySheet
import com.wavvy.app.core.designsystem.components.Toasts
import com.wavvy.app.core.drive.DriveAccess
import com.wavvy.app.core.drive.DriveAuth
import com.wavvy.app.core.drive.DriveClient
import com.wavvy.app.core.drive.DriveInterval
import com.wavvy.app.core.drive.DriveState
import com.wavvy.app.core.drive.DriveStatus
import com.wavvy.app.core.drive.DriveStore
import com.wavvy.app.features.playlist.ui.SortChoice
import com.wavvy.app.features.playlist.ui.SortChoiceSheet
import com.wavvy.app.features.settings.ui.components.SettingsActionRow
import com.wavvy.app.features.settings.ui.components.SettingsChoiceRow
import com.wavvy.app.features.settings.ui.components.SettingsDivider
import com.wavvy.app.features.settings.ui.components.SettingsGroup
import com.wavvy.app.features.settings.ui.components.SettingsPage
import com.wavvy.app.features.settings.ui.components.SettingsSwitchRow

// The name of the screen of the Drive in the settings route
const val BackupDriveSection = "BackupDrive"

// The Drive of the user, where the backup is kept in a private folder of the app, connected once and looked at when the screen opens
@Composable
fun DriveSettingsScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val driveStore = remember { DriveStore(context) }
    val status by DriveState.status.collectAsState()
    val isConnected by driveStore.isConnected.collectAsState(initial = false)
    val isAuto by driveStore.autoSave.collectAsState(initial = false)
    val isLocked by driveStore.isLocked.collectAsState(initial = false)
    val autoHours by driveStore.autoHours.collectAsState(initial = DriveInterval.SixHours.hours)
    var step by remember { mutableStateOf<BackupStep>(BackupStep.None) }

    // A phone that was just connected is offered the backup that is in the Drive, once it is known, when it never met one
    var justConnected by remember { mutableStateOf(false) }
    var isOffering by remember { mutableStateOf(false) }
    val synced by driveStore.syncedAt.collectAsState(initial = -1L)

    // A file made and locked that waits for the user to agree to replace a backup this phone has not seen
    var waiting by remember { mutableStateOf<Pair<ByteArray, Boolean>?>(null) }

    val saved = stringResource(R.string.backup_drive_saved)
    val nothing = stringResource(R.string.backup_drive_nothing)
    val restored = stringResource(R.string.backup_restored)
    val failed = stringResource(R.string.backup_error)
    val wrongPassword = stringResource(R.string.backup_error_password)
    val tooNew = stringResource(R.string.backup_error_too_new)
    val notBackup = stringResource(R.string.backup_error_not_a_backup)

    LaunchedEffect(Unit) { DriveState.refresh(context) }
    LaunchedEffect(status) {
        if (!justConnected) return@LaunchedEffect
        when (status) {
            is DriveStatus.Found -> {
                justConnected = false
                if (synced == 0L) isOffering = true
            }
            DriveStatus.Empty, DriveStatus.Failed -> justConnected = false
            else -> Unit
        }
    }

    // The screen of Google where the user agrees to keep the backup in the private folder of the app
    val consentLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        val token = DriveAuth.tokenFrom(context, result.data)
        if (token == null) {
            DriveState.fail()
        } else {
            justConnected = true
            scope.launch {
                driveStore.setConnected(true)
                driveStore.setAutoSave(true)
                DriveState.use(token)
            }
        }
    }

    // Asks for the key, and either goes on with it or sends the user to the screen of Google
    val connect: () -> Unit = {
        scope.launch {
            runCatching { DriveAuth.authorize(context) }
                .onSuccess { access ->
                    when (access) {
                        is DriveAccess.Ready -> {
                            justConnected = true
                            driveStore.setConnected(true)
                            driveStore.setAutoSave(true)
                            DriveState.use(access.token)
                        }
                        is DriveAccess.NeedsConsent -> consentLauncher.launch(IntentSenderRequest.Builder(access.request.intentSender).build())
                    }
                }
                .onFailure { DriveState.fail() }
        }
    }

    // Says why the Drive or a file did not work
    val showFailure: (Throwable) -> Unit = {
        step = BackupStep.None
        Toasts.show(
            when (it) {
                BackupFailure.WrongPassword -> wrongPassword
                BackupFailure.TooNew -> tooNew
                BackupFailure.NotABackup -> notBackup
                else -> failed
            },
            isError = true
        )
    }

    // The key to the Drive for one action, null when it needs the user again, which the screen tells
    suspend fun freshToken(): String? =
        (runCatching { DriveAuth.authorize(context) }.getOrNull() as? DriveAccess.Ready)?.token

    // Sends the file to the Drive in place of the one that is there, and remembers that this phone has seen it
    val upload: (ByteArray, Boolean) -> Unit = { bytes, hasPassword ->
        step = BackupStep.Busy
        scope.launch {
            val result = runCatching {
                val token = freshToken() ?: error("No key")
                val current = (DriveState.status.value as? DriveStatus.Found)?.backup ?: DriveClient.find(token)
                val sent = DriveClient.save(token, bytes, current?.id)
                driveStore.setSyncedAt(System.currentTimeMillis())
                driveStore.setLastHash(null)
                driveStore.setLocked(hasPassword)
                DriveState.use(token)
                sent
            }
            step = BackupStep.None
            if (result.isSuccess) Toasts.show(saved) else {
                DriveState.fail()
                Toasts.show(failed, isError = true)
            }
        }
    }

    // Makes the file of what was chosen, and sends it unless there is nothing to save or the Drive has a backup this phone has not seen
    val save: (Set<BackupSection>, CharArray?) -> Unit = { sections, typed ->
        step = BackupStep.Busy
        scope.launch {
            val hasPassword = typed != null
            val made = runCatching {
                val payload = withContext(Dispatchers.IO) { BackupData.collect(context, sections) }
                if (BackupData.available(payload).values.all { it == 0 }) return@runCatching null
                withContext(Dispatchers.Default) { BackupCrypto.encrypt(payload.toString().toByteArray(Charsets.UTF_8), typed) }
            }
            typed?.fill(' ')

            val bytes = made.getOrNull()
            when {
                made.isFailure -> showFailure(made.exceptionOrNull() ?: BackupFailure.NotABackup)
                bytes == null -> {
                    step = BackupStep.None
                    Toasts.show(nothing, isError = true)
                }
                else -> {
                    val remote = (status as? DriveStatus.Found)?.backup
                    val seen = driveStore.syncedAt.first()
                    if (remote != null && remote.modifiedAt.toEpochMilli() > seen) {
                        step = BackupStep.None
                        waiting = bytes to hasPassword
                    } else {
                        upload(bytes, hasPassword)
                    }
                }
            }
        }
    }

    // Opens the file with its password, or without one when it has no lock, and goes on to the choice of what to put back
    val openFile: (ByteArray, CharArray?) -> Unit = { file, typed ->
        step = BackupStep.Busy
        scope.launch {
            val result = runCatching { readBackup(file, typed) }
            typed?.fill(' ')
            result
                .onSuccess { payload ->
                    val found = BackupData.available(payload)
                    if (found.isEmpty()) showFailure(BackupFailure.NotABackup) else step = BackupStep.Choose(payload, found)
                }
                .onFailure(showFailure)
        }
    }

    // Brings the backup down from the Drive, and asks its password only when it has a lock
    val restore: () -> Unit = {
        val remote = (status as? DriveStatus.Found)?.backup
        if (remote != null) {
            step = BackupStep.Busy
            scope.launch {
                runCatching {
                    val token = freshToken() ?: error("No key")
                    DriveClient.read(token, remote.id)
                }
                    .onSuccess { file -> if (BackupCrypto.isLocked(file)) step = BackupStep.Password(file) else openFile(file, null) }
                    .onFailure { showFailure(it) }
            }
        }
    }

    when (val current = step) {
        BackupStep.Export -> ExportDialog(onExport = save, onDismiss = { step = BackupStep.None })
        is BackupStep.Password -> PasswordDialog(onOpen = { typed -> openFile(current.file, typed) }, onDismiss = { step = BackupStep.None })
        is BackupStep.Choose -> ChooseDialog(
            found = current.found,
            onRestore = { sections ->
                step = BackupStep.Busy
                scope.launch {
                    val isRestored = runCatching { BackupData.restore(context, current.payload, sections) }.isSuccess
                    if (isRestored) driveStore.setSyncedAt(System.currentTimeMillis())
                    step = BackupStep.None
                    if (isRestored) Toasts.show(restored) else Toasts.show(failed, isError = true)
                }
            },
            onDismiss = { step = BackupStep.None }
        )
        BackupStep.Busy -> BusyDialog()
        BackupStep.None -> Unit
    }

    waiting?.let { (bytes, hasPassword) ->
        ConfirmDialog(
            title = stringResource(R.string.backup_drive_replace_title),
            text = stringResource(R.string.backup_drive_replace_text),
            confirmLabel = stringResource(R.string.backup_drive_replace),
            cancelLabel = stringResource(R.string.playlist_cancel),
            onConfirm = {
                waiting = null
                upload(bytes, hasPassword)
            },
            onDismiss = { waiting = null }
        )
    }

    if (isOffering) {
        val remote = (status as? DriveStatus.Found)?.backup
        ConfirmDialog(
            title = stringResource(R.string.backup_drive_offer_title),
            text = stringResource(
                R.string.backup_drive_offer_text,
                remote?.let { DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT).withZone(ZoneId.systemDefault()).format(it.modifiedAt) }.orEmpty()
            ),
            confirmLabel = stringResource(R.string.backup_drive_offer_restore),
            cancelLabel = stringResource(R.string.backup_drive_offer_later),
            onConfirm = {
                isOffering = false
                restore()
            },
            onDismiss = { isOffering = false }
        )
    }

    val intervalTitle = stringResource(R.string.backup_drive_interval)
    val intervalChoices = DriveInterval.entries.map { interval ->
        SortChoice(stringResource(interval.titleRes), interval.hours == autoHours, interval == DriveInterval.SixHours) { scope.launch { driveStore.setAutoHours(interval.hours) } }
    }

    SettingsPage(title = stringResource(R.string.backup_drive), onBack = onBack, modifier = modifier) {
        SettingsGroup(title = stringResource(R.string.settings_group_connection)) {
            SettingsActionRow(
                title = stringResource(if (isConnected) R.string.backup_drive_status else R.string.backup_drive_connect),
                subtitle = when (val current = status) {
                    DriveStatus.Disconnected -> stringResource(R.string.backup_drive_subtitle)
                    DriveStatus.Checking -> stringResource(R.string.backup_drive_checking)
                    DriveStatus.Empty -> stringResource(R.string.backup_drive_empty)
                    DriveStatus.Failed -> stringResource(R.string.backup_drive_failed)
                    is DriveStatus.Found -> stringResource(
                        R.string.backup_drive_found,
                        DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT).withZone(ZoneId.systemDefault()).format(current.backup.modifiedAt),
                        Formatter.formatShortFileSize(context, current.backup.sizeBytes)
                    )
                },
                onClick = { if (isConnected) scope.launch { DriveState.refresh(context) } else connect() }
            )
        }

        if (isConnected) {
            SettingsGroup(title = stringResource(R.string.settings_group_backup)) {
                SettingsActionRow(
                    title = stringResource(R.string.backup_drive_save),
                    subtitle = stringResource(R.string.backup_drive_save_subtitle),
                    onClick = { if (status != DriveStatus.Checking && status != DriveStatus.Failed) step = BackupStep.Export }
                )
                SettingsDivider()
                SettingsActionRow(
                    title = stringResource(R.string.backup_drive_restore),
                    subtitle = stringResource(R.string.backup_drive_restore_subtitle),
                    onClick = restore
                )
            }
            SettingsGroup(title = stringResource(R.string.settings_group_automatic)) {
                SettingsSwitchRow(
                    title = stringResource(R.string.backup_drive_auto),
                    subtitle = stringResource(if (isLocked) R.string.backup_drive_auto_locked else R.string.backup_drive_auto_subtitle),
                    checked = isAuto && !isLocked,
                    onCheckedChange = { value -> if (!isLocked) scope.launch { driveStore.setAutoSave(value) } }
                )
                if (isAuto && !isLocked) {
                    SettingsDivider()
                    SettingsChoiceRow(
                        title = intervalTitle,
                        value = stringResource(DriveInterval.entries.firstOrNull { it.hours == autoHours }?.titleRes ?: DriveInterval.SixHours.titleRes),
                        onClick = { OverlaySheet.show { SortChoiceSheet(title = intervalTitle, choices = intervalChoices, onDismiss = OverlaySheet::dismiss) } }
                    )
                }
            }
            SettingsGroup(title = stringResource(R.string.settings_group_account)) {
                SettingsActionRow(
                    title = stringResource(R.string.backup_drive_disconnect),
                    subtitle = stringResource(R.string.backup_drive_disconnect_subtitle),
                    onClick = {
                        DriveState.disconnect()
                        scope.launch {
                            driveStore.setConnected(false)
                            driveStore.setAutoSave(false)
                        }
                    }
                )
            }
        }
    }
}

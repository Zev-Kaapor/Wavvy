package com.wavvy.app.features.settings.ui

// Android dates, files and messages
import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
// Android configuration
import android.content.res.Configuration
// Compose layouts and foundations
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
// Material 3 components
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
// Compose state and runtime
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
// UI styling and utilities
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
// Coroutines and files
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
// Project resources
import com.wavvy.app.R
import com.wavvy.app.core.backup.BackupCrypto
import com.wavvy.app.core.backup.BackupData
import com.wavvy.app.core.backup.BackupFailure
import com.wavvy.app.core.backup.BackupInbox
import com.wavvy.app.core.backup.BackupSection
import com.wavvy.app.core.designsystem.components.LocalSheetClose
import com.wavvy.app.core.designsystem.components.OverlaySheet
import com.wavvy.app.core.designsystem.components.ScrollIndicatorWidth
import com.wavvy.app.core.designsystem.components.Toasts
import com.wavvy.app.core.designsystem.components.WavvySheet
import com.wavvy.app.core.designsystem.components.WavvySwitch
import com.wavvy.app.core.designsystem.components.scrollIndicator
import com.wavvy.app.core.designsystem.icons.WavvyIcons
import com.wavvy.app.core.designsystem.theme.WavvyMotion
import com.wavvy.app.core.designsystem.theme.WavvyTheme
import com.wavvy.app.features.menu.ItemMenuDimens
import com.wavvy.app.features.settings.ui.components.SettingsActionRow
import com.wavvy.app.features.settings.ui.components.SettingsGroup
import com.wavvy.app.features.settings.ui.components.SettingsPage

// The name of the screen of the manual backup in the settings route
const val BackupManualSection = "BackupManual"

// The name of a backup file, the letters of backup, the day and an end that only this app makes and reads
private const val FileExtension = ".wvy"
private const val FilePrefix = "bkp"

// The day in the name, two digits each for the year, the month and the day, such as 261012
private val FileDate: DateTimeFormatter = DateTimeFormatter.ofPattern("yyMMdd")
private const val FileMime = "application/octet-stream"

// The fewest letters a password of a backup can have, and the turn that flips the arrow when the password opens
private const val MinPasswordLength = 8
private const val ArrowFlip = 180f

// How long the sheet waits for what opened to finish opening before it slides down to it
private val ScrollDelayMillis = WavvyMotion.ExpandMillis.toLong()

// Where the backup is in its steps, nothing, asking what and with which password to export, asking the password of a file, choosing what to restore from it, or working
internal sealed interface BackupStep {
    data object None : BackupStep
    data object Export : BackupStep
    data class Password(val file: ByteArray) : BackupStep
    data class Choose(val payload: JSONObject, val found: Map<BackupSection, Int>) : BackupStep
    data object Busy : BackupStep
}

// The backup of what is only on this phone, the settings, the history and the like, in a file with a password that only this app opens
// What is kept in the account of YouTube Music, as the playlists and the likes, is not here, it comes back when the user signs in
@Composable
fun ManualBackupScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var step by remember { mutableStateOf<BackupStep>(BackupStep.None) }

    // What was asked to export waits here while the user chooses where the file goes, without a password the file has no lock
    var pending by remember { mutableStateOf<Pair<Set<BackupSection>, CharArray?>?>(null) }

    val saved = stringResource(R.string.backup_saved)
    val restored = stringResource(R.string.backup_restored)
    val failed = stringResource(R.string.backup_error)
    val wrongPassword = stringResource(R.string.backup_error_password)
    val tooNew = stringResource(R.string.backup_error_too_new)
    val notBackup = stringResource(R.string.backup_error_not_a_backup)

    // Says why a file did not open
    val showFailure: (Throwable) -> Unit = {
        step = BackupStep.None
        Toasts.show(
            when (it) {
                BackupFailure.WrongPassword -> wrongPassword
                BackupFailure.TooNew -> tooNew
                else -> notBackup
            },
            isError = true
        )
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

    // Reads the file that was picked or opened from another app, and asks for the password only when it has a lock
    val startImport: (Uri) -> Unit = { uri ->
        step = BackupStep.Busy
        scope.launch {
            val read = runCatching {
                val file = withContext(Dispatchers.IO) { context.contentResolver.openInputStream(uri)?.use { it.readBytes() } } ?: throw BackupFailure.NotABackup
                file to BackupCrypto.isLocked(file)
            }
            read
                .onSuccess { (file, isLocked) -> if (isLocked) step = BackupStep.Password(file) else openFile(file, null) }
                .onFailure(showFailure)
        }
    }

    // A file opened from another app goes straight in
    val incoming by BackupInbox.pending.collectAsState()
    LaunchedEffect(incoming) {
        incoming?.let {
            BackupInbox.clear()
            startImport(it)
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(FileMime)) { uri ->
        val request = pending
        pending = null
        if (uri == null || request == null) {
            step = BackupStep.None
            return@rememberLauncherForActivityResult
        }

        step = BackupStep.Busy
        scope.launch {
            val isSaved = runCatching { writeBackup(context, uri, request.first, request.second) }.isSuccess
            request.second?.fill(' ')
            step = BackupStep.None
            if (isSaved) Toasts.show(saved) else Toasts.show(failed, isError = true)
        }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) step = BackupStep.None else startImport(uri)
    }

    when (val current = step) {
        BackupStep.Export -> ExportDialog(
            onExport = { sections, typed ->
                pending = sections to typed
                exportLauncher.launch(FilePrefix + LocalDate.now().format(FileDate) + FileExtension)
            },
            onDismiss = { step = BackupStep.None }
        )
        is BackupStep.Password -> PasswordDialog(
            onOpen = { typed -> openFile(current.file, typed) },
            onDismiss = { step = BackupStep.None }
        )
        is BackupStep.Choose -> ChooseDialog(
            found = current.found,
            onRestore = { sections ->
                step = BackupStep.Busy
                scope.launch {
                    val isRestored = runCatching { BackupData.restore(context, current.payload, sections) }.isSuccess
                    step = BackupStep.None
                    if (isRestored) Toasts.show(restored) else Toasts.show(failed, isError = true)
                }
            },
            onDismiss = { step = BackupStep.None }
        )
        BackupStep.Busy -> BusyDialog()
        BackupStep.None -> Unit
    }

    SettingsPage(title = stringResource(R.string.backup_manual), onBack = onBack, modifier = modifier) {
        SettingsGroup(title = stringResource(R.string.settings_group_export)) {
            SettingsActionRow(
                title = stringResource(R.string.backup_export),
                subtitle = stringResource(R.string.backup_export_subtitle),
                onClick = { step = BackupStep.Export }
            )
        }
        SettingsGroup(title = stringResource(R.string.settings_group_import)) {
            SettingsActionRow(
                title = stringResource(R.string.backup_import),
                subtitle = stringResource(R.string.backup_import_subtitle),
                onClick = { importLauncher.launch(arrayOf("*/*")) }
            )
        }
    }
}

// Makes the file, locks it with the password when there is one, and writes it where the user chose
private suspend fun writeBackup(context: Context, uri: Uri, sections: Set<BackupSection>, password: CharArray?) =
    withContext(Dispatchers.IO) {
        val plain = BackupData.collect(context, sections).toString().toByteArray(Charsets.UTF_8)
        val made = withContext(Dispatchers.Default) { BackupCrypto.encrypt(plain, password) }
        context.contentResolver.openOutputStream(uri, "wt")?.use { it.write(made) } ?: error("No place to write")
    }

// Opens the bytes of a file with the password, which a file without a lock does not need
internal suspend fun readBackup(file: ByteArray, password: CharArray?): JSONObject =
    withContext(Dispatchers.Default) { JSONObject(String(BackupCrypto.decrypt(file, password), Charsets.UTF_8)) }

// The sheet that asks what goes in the backup, and the password if the user wants one, twice so a mistake does not lock it for good
@Composable
internal fun ExportDialog(onExport: (Set<BackupSection>, CharArray?) -> Unit, onDismiss: () -> Unit) {
    SheetWhileShown {
        var chosen by remember { mutableStateOf(BackupSection.entries.toSet()) }
        var isPasswordOpen by remember { mutableStateOf(false) }
        var first by remember { mutableStateOf("") }
        var second by remember { mutableStateOf("") }
        var error by remember { mutableStateOf<Int?>(null) }
        val arrowTurn by animateFloatAsState(if (isPasswordOpen) ArrowFlip else 0f, tween(WavvyMotion.ExpandMillis), label = "PasswordArrow")

        BackupSheet(
            title = stringResource(R.string.backup_export),
            description = null,
            confirmLabel = stringResource(R.string.backup_export_confirm),
            scrollToEnd = isPasswordOpen,
            scrollKey = error,
            isConfirmEnabled = chosen.isNotEmpty(),
            onConfirm = {
                when {
                    // No password means no lock, anything typed has to be good and the same twice
                    first.isEmpty() && second.isEmpty() -> onExport(chosen, null)
                    first.length < MinPasswordLength -> {
                        isPasswordOpen = true
                        error = R.string.backup_password_short
                    }
                    first != second -> {
                        isPasswordOpen = true
                        error = R.string.backup_password_different
                    }
                    else -> onExport(chosen, first.toCharArray())
                }
            },
            onDismiss = onDismiss
        ) {
            BackupCard {
                SectionRows(counts = BackupSection.entries.associateWith { null }, chosen = chosen, onChange = { chosen = it })
            }

            // The password is optional, a tap on its name opens the two fields, and under the name it says whether the file will have a lock
            BackupCard {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isPasswordOpen = !isPasswordOpen }
                        .padding(horizontal = ItemMenuDimens.CardRowHorizontal, vertical = ItemMenuDimens.CardRowVertical)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.backup_password_title),
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = stringResource(if (first.isEmpty()) R.string.backup_password_hint_none else R.string.backup_password_hint_set),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(
                        imageVector = WavvyIcons.ChevronDown,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.rotate(arrowTurn)
                    )
                }
                AnimatedVisibility(
                    visible = isPasswordOpen,
                    enter = expandVertically(tween(WavvyMotion.ExpandMillis)) + fadeIn(tween(WavvyMotion.ExpandMillis)),
                    exit = shrinkVertically(tween(WavvyMotion.ExpandMillis)) + fadeOut(tween(WavvyMotion.ExpandMillis))
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(ItemMenuDimens.CardGap),
                        modifier = Modifier.padding(start = ItemMenuDimens.CardRowHorizontal, end = ItemMenuDimens.CardRowHorizontal, bottom = ItemMenuDimens.CardRowVertical)
                    ) {
                        PasswordField(value = first, onValueChange = { first = it; error = null }, placeholder = stringResource(R.string.backup_password_optional))
                        PasswordField(
                            value = second,
                            onValueChange = { second = it; error = null },
                            placeholder = stringResource(R.string.backup_password_again_optional),
                            error = error?.let { stringResource(it) }
                        )
                    }
                }
            }
        }
    }
}

// The sheet that asks the password of the file that was picked
@Composable
internal fun PasswordDialog(onOpen: (CharArray) -> Unit, onDismiss: () -> Unit) {
    SheetWhileShown {
        var typed by remember { mutableStateOf("") }

        BackupSheet(
            title = stringResource(R.string.backup_import),
            description = null,
            confirmLabel = stringResource(R.string.backup_open),
            isConfirmEnabled = typed.isNotEmpty(),
            onConfirm = { onOpen(typed.toCharArray()) },
            onDismiss = onDismiss
        ) {
            PasswordField(value = typed, onValueChange = { typed = it }, placeholder = stringResource(R.string.backup_password_title))
        }
    }
}

// The sheet that shows what the file has and asks what to put back, which replaces what is on the phone
@Composable
internal fun ChooseDialog(found: Map<BackupSection, Int>, onRestore: (Set<BackupSection>) -> Unit, onDismiss: () -> Unit) {
    SheetWhileShown {
        var chosen by remember { mutableStateOf(found.keys) }

        BackupSheet(
            title = stringResource(R.string.backup_restore),
            description = stringResource(R.string.backup_restore_warning),
            confirmLabel = stringResource(R.string.backup_restore_confirm),
            isConfirmEnabled = chosen.isNotEmpty(),
            onConfirm = { onRestore(chosen) },
            onDismiss = onDismiss
        ) {
            BackupCard {
                SectionRows(counts = found, chosen = chosen, onChange = { chosen = it })
            }
        }
    }
}

// The box that waits while the file is made or opened, the key takes a moment to be made on purpose
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun BusyDialog() {
    BasicAlertDialog(onDismissRequest = {}) {
        Surface(shape = RoundedCornerShape(SettingsDimens.DialogCorner), color = MaterialTheme.colorScheme.surfaceContainerHigh) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(SettingsDimens.DialogGap),
                modifier = Modifier.padding(SettingsDimens.DialogPadding)
            ) {
                CircularProgressIndicator(strokeWidth = SettingsDimens.ProgressStroke)
                Text(text = stringResource(R.string.backup_working), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}

// Shows a sheet over the bars while the composable is on the screen and takes it away when it leaves
@Composable
private fun SheetWhileShown(content: @Composable () -> Unit) {
    DisposableEffect(Unit) {
        OverlaySheet.show(content)
        onDispose { OverlaySheet.dismiss() }
    }
}

// The frame of the sheets of the backup, the title and the buttons on the start and the parts on the end when the phone lies down
@Composable
private fun BackupSheet(
    title: String,
    description: String?,
    confirmLabel: String,
    isConfirmEnabled: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    scrollToEnd: Boolean = false,
    scrollKey: Any? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    WavvySheet(onDismiss = onDismiss) {
        val closeSheet = LocalSheetClose.current
        val scroll = rememberScrollState()

        // When something opens at the end of the sheet, such as the fields of the password, the sheet slides down to show it
        LaunchedEffect(scrollToEnd, scrollKey) {
            if (scrollToEnd) {
                delay(ScrollDelayMillis)
                scroll.animateScrollTo(scroll.maxValue)
            }
        }

        val head: @Composable () -> Unit = {
            Column(modifier = Modifier.padding(horizontal = ItemMenuDimens.CreditsTitleStart, vertical = ItemMenuDimens.HeaderVertical)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                description?.let {
                    Text(text = it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        // The two buttons of the same width, the one that goes on stands out as in the other questions
        val buttons: @Composable () -> Unit = {
            Row(horizontalArrangement = Arrangement.spacedBy(ItemMenuDimens.CardGap), modifier = Modifier.fillMaxWidth()) {
                FilledTonalButton(onClick = closeSheet, modifier = Modifier.weight(1f)) {
                    Text(text = stringResource(R.string.playlist_cancel), maxLines = 1)
                }
                Button(
                    onClick = onConfirm,
                    enabled = isConfirmEnabled,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.onBackground, contentColor = MaterialTheme.colorScheme.background),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(text = confirmLabel, maxLines = 1)
                }
            }
        }

        if (LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(ItemMenuDimens.CardGroupGap),
                modifier = Modifier.padding(start = ItemMenuDimens.CardsSide, end = ItemMenuDimens.CardsSide, bottom = ItemMenuDimens.LandscapeBottom)
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(ItemMenuDimens.HeaderVertical),
                    modifier = Modifier.weight(ItemMenuDimens.LandscapeHeadWeight).align(Alignment.CenterVertically)
                ) {
                    head()
                    buttons()
                }
                Column(
                    verticalArrangement = Arrangement.spacedBy(ItemMenuDimens.CardGap),
                    modifier = Modifier
                        .weight(1f - ItemMenuDimens.LandscapeHeadWeight)
                        .scrollIndicator(scroll)
                        .verticalScroll(scroll)
                        .padding(end = ScrollIndicatorWidth + ItemMenuDimens.LandscapeIndicatorGap)
                        .imePadding(),
                    content = content
                )
            }
        } else {
            Column(
                verticalArrangement = Arrangement.spacedBy(ItemMenuDimens.CardGap),
                modifier = Modifier
                    .scrollIndicator(scroll)
                    .verticalScroll(scroll)
                    .padding(start = ItemMenuDimens.CardsSide, end = ItemMenuDimens.CardsSide, bottom = ItemMenuDimens.Bottom)
                    .imePadding()
            ) {
                head()
                content()
                Spacer(modifier = Modifier.height(ItemMenuDimens.CardGap))
                buttons()
            }
        }
    }
}

// A card that holds the parts of a sheet, as the cards of the menus
@Composable
private fun BackupCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(ItemMenuDimens.CardCorner))
            .background(WavvyTheme.colors.menuCard),
        content = content
    )
}

// The parts of the backup, each with a switch, and how many things it has when the file says
@Composable
private fun SectionRows(
    counts: Map<BackupSection, Int?>,
    chosen: Set<BackupSection>,
    onChange: (Set<BackupSection>) -> Unit
) {
    counts.entries.forEachIndexed { index, (section, count) ->
        val isChosen = section in chosen
        if (index > 0) {
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = ItemMenuDimens.CardRowHorizontal),
                thickness = WavvyTheme.dimens.hairline,
                color = MaterialTheme.colorScheme.outlineVariant
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onChange(if (isChosen) chosen - section else chosen + section) }
                .padding(horizontal = ItemMenuDimens.CardRowHorizontal, vertical = ItemMenuDimens.CardRowVertical)
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = ItemMenuDimens.CardRowTextStart)) {
                Text(
                    text = stringResource(section.titleRes),
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (count == null) stringResource(section.subtitleRes) else stringResource(R.string.backup_items, count),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            WavvySwitch(checked = isChosen, onCheckedChange = { onChange(if (it) chosen + section else chosen - section) })
        }
    }
}

// A field for a password, its letters are hidden
@Composable
private fun PasswordField(value: String, onValueChange: (String) -> Unit, placeholder: String, error: String? = null) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(text = placeholder) },
        singleLine = true,
        isError = error != null,
        supportingText = error?.let { { Text(text = it) } },
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent),
        modifier = Modifier.fillMaxWidth()
    )
}

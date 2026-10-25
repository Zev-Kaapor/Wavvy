package com.wavvy.app.features.settings.ui

// Android file sizes and intents
import android.content.Intent
import android.text.format.Formatter
import androidx.core.net.toUri
// Compose layouts and foundations
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
// Material 3 components
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
// Compose state and runtime
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
// UI styling and utilities
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
// Time and coroutines
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlinx.coroutines.launch
// Project resources
import com.wavvy.app.R
import com.wavvy.app.core.designsystem.components.Toasts
import com.wavvy.app.core.designsystem.icons.WavvyIcons
import com.wavvy.app.core.lyrics.LyricsTranslator
import com.wavvy.app.features.updates.data.UpdateNote
import com.wavvy.app.core.designsystem.components.OverlaySheet
import com.wavvy.app.features.playlist.ui.SortChoice
import com.wavvy.app.features.playlist.ui.SortChoiceSheet
import com.wavvy.app.features.settings.data.SettingsStore
import com.wavvy.app.features.settings.data.UpdateInterval
import com.wavvy.app.features.settings.data.UpdateSettings
import com.wavvy.app.features.settings.ui.components.SettingsActionRow
import com.wavvy.app.features.settings.ui.components.SettingsCard
import com.wavvy.app.features.settings.ui.components.SettingsChoiceRow
import com.wavvy.app.features.settings.ui.components.SettingsDivider
import com.wavvy.app.features.settings.ui.components.SettingsGroup
import com.wavvy.app.features.settings.ui.components.SettingsPage
import com.wavvy.app.features.settings.ui.components.SettingsSwitchRow
import com.wavvy.app.features.updates.data.UpdateInfo
import com.wavvy.app.features.updates.data.UpdateState
import com.wavvy.app.features.updates.data.Updates

// The name of the screen of the updates in the route of the settings
const val UpdatesSection = "Updates"

// The updates, the version that is installed, what the check found with the buttons that download and install it, what changed in it and how the app looks for them
@Composable
fun UpdatesSettingsScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val store = remember { SettingsStore(context) }
    val settings by store.updates.collectAsState(initial = UpdateSettings())
    val state by Updates.state.collectAsState()

    // The question of the app opening is answered by being here, and the first time the screen looks for a newer version
    LaunchedEffect(Unit) {
        Updates.clearPrompt()
        if (state is UpdateState.Idle) Updates.check(context)
    }

    val intervalTitle = stringResource(R.string.updates_interval)
    val intervalChoices = UpdateInterval.entries.map { interval ->
        SortChoice(stringResource(interval.titleRes), interval == settings.interval, interval == UpdateSettings().interval) { scope.launch { store.setUpdateInterval(interval) } }
    }
    val info = when (val current = state) {
        is UpdateState.Available -> current.info
        is UpdateState.UpToDate -> current.info
        is UpdateState.Downloading -> current.info
        is UpdateState.Ready -> current.info
        is UpdateState.Installing -> current.info
        is UpdateState.NeedsPermission -> current.info
        is UpdateState.Failed -> current.info
        else -> null
    }

    SettingsPage(title = stringResource(R.string.updates_title), onBack = onBack, modifier = modifier) {
        SettingsGroup(title = stringResource(R.string.settings_group_status)) {
            StatusBlock(state = state, installed = Updates.installedVersion(context))
        }

        // What changed in the last release, in the language of the app when the user asks, each change opens to its description when the user touches it
        if (info != null && info.notes.isNotEmpty()) {
            var translated by remember(info.version) { mutableStateOf<List<UpdateNote>?>(null) }
            var isTranslating by remember(info.version) { mutableStateOf(false) }
            val notes = translated ?: info.notes
            val language = LocalConfiguration.current.locales[0].language
            val translateFailed = stringResource(R.string.updates_translate_error)

            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(start = SettingsDimens.GroupTitleStart)) {
                Text(
                    text = stringResource(R.string.updates_notes, info.version),
                    style = MaterialTheme.typography.labelLarge.merge(SettingsType.GroupTitle),
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.weight(1f)
                )
                // The notes are written in English, so the button is there only for another language
                if (language != EnglishLanguage) {
                    IconButton(
                        enabled = !isTranslating,
                        onClick = {
                            if (translated != null) {
                                translated = null
                            } else {
                                isTranslating = true
                                scope.launch {
                                    translateNotes(info.notes, language)
                                        .onSuccess { translated = it }
                                        .onFailure { Toasts.show(translateFailed, isError = true) }
                                    isTranslating = false
                                }
                            }
                        }
                    ) {
                        if (isTranslating) {
                            CircularProgressIndicator(modifier = Modifier.size(SettingsDimens.RowIcon), strokeWidth = SettingsDimens.ProgressStroke)
                        } else {
                            Icon(
                                imageVector = WavvyIcons.Translate,
                                contentDescription = stringResource(if (translated != null) R.string.updates_original else R.string.updates_translate),
                                tint = if (translated != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            SettingsCard {
                Column(modifier = Modifier.padding(SettingsDimens.RowPadding), verticalArrangement = Arrangement.spacedBy(SettingsDimens.NotesBetween)) {
                    notes.forEachIndexed { index, note -> NoteItem(note = note, key = "${info.version}:$index") }
                }
                SettingsDivider()
                SettingsActionRow(
                    title = stringResource(R.string.updates_release_page),
                    subtitle = null,
                    onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, info.pageUrl.toUri())) }
                )
            }
        }

        SettingsGroup(title = stringResource(R.string.settings_group_checking)) {
            SettingsSwitchRow(
                title = stringResource(R.string.updates_pause),
                subtitle = stringResource(R.string.updates_pause_subtitle),
                checked = settings.paused,
                onCheckedChange = { scope.launch { store.setUpdatesPaused(it) } }
            )
            SettingsDivider()
            SettingsChoiceRow(
                title = intervalTitle,
                value = stringResource(settings.interval.titleRes),
                onClick = { OverlaySheet.show { SortChoiceSheet(title = intervalTitle, choices = intervalChoices, onDismiss = OverlaySheet::dismiss) } }
            )
        }
    }
}

// The top of the screen, what the updater is doing in words and the buttons of what can be done now
@Composable
private fun StatusBlock(state: UpdateState, installed: String) {
    val context = LocalContext.current
    val dateFormat = remember { DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM) }

    // The words of the status, the line under them and the buttons, by what the updater is doing
    val title: String
    var subtitle: String? = null
    when (state) {
        UpdateState.Idle, UpdateState.Checking -> title = stringResource(R.string.updates_checking)
        is UpdateState.UpToDate -> {
            title = stringResource(R.string.updates_up_to_date)
            subtitle = stringResource(R.string.updates_installed, installed)
        }
        is UpdateState.Available -> {
            title = stringResource(R.string.updates_available, state.info.version)
            subtitle = detailsOf(state.info, dateFormat) { Formatter.formatShortFileSize(context, it) }
        }
        is UpdateState.Downloading -> {
            title = stringResource(R.string.updates_downloading, state.info.version)
            subtitle = stringResource(R.string.updates_progress, state.percent, Formatter.formatShortFileSize(context, state.bytes), Formatter.formatShortFileSize(context, state.total))
        }
        is UpdateState.Ready -> {
            title = stringResource(R.string.updates_ready, state.info.version)
            subtitle = stringResource(R.string.updates_ready_subtitle)
        }
        is UpdateState.Installing -> title = stringResource(R.string.updates_installing)
        is UpdateState.NeedsPermission -> {
            title = stringResource(R.string.updates_permission)
            subtitle = stringResource(R.string.updates_permission_subtitle)
        }
        is UpdateState.Failed -> title = stringResource(state.messageRes)
    }

    Column(modifier = Modifier.fillMaxWidth().padding(SettingsDimens.RowPadding), verticalArrangement = Arrangement.spacedBy(SettingsDimens.NotesGap)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (state is UpdateState.Checking || state is UpdateState.Installing) {
                CircularProgressIndicator(modifier = Modifier.size(SettingsDimens.RowIcon), strokeWidth = SettingsDimens.ProgressStroke)
                Text(text = title, style = MaterialTheme.typography.bodyLarge.merge(SettingsType.RowTitle), color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(start = SettingsDimens.RowIconGap))
            } else {
                Text(text = title, style = MaterialTheme.typography.bodyLarge.merge(SettingsType.RowTitle), color = MaterialTheme.colorScheme.onSurface)
            }
        }
        subtitle?.let {
            Text(text = it, style = MaterialTheme.typography.bodyMedium.merge(SettingsType.RowSubtitle), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (state is UpdateState.Downloading) {
            LinearProgressIndicator(progress = { state.percent / PercentTotal }, modifier = Modifier.fillMaxWidth())
        }

        // The button that stands out is the one that goes forward, the other one is the way back
        Row(horizontalArrangement = Arrangement.spacedBy(SettingsDimens.ButtonGap), modifier = Modifier.fillMaxWidth()) {
            when (state) {
                is UpdateState.Available -> PrimaryButton(stringResource(R.string.updates_download), Modifier.weight(1f)) { Updates.download(context) }
                is UpdateState.Downloading -> SecondaryButton(stringResource(R.string.playlist_cancel), Modifier.weight(1f)) { Updates.cancelDownload() }
                is UpdateState.Ready -> PrimaryButton(stringResource(R.string.updates_install), Modifier.weight(1f)) { Updates.install(context) }
                is UpdateState.NeedsPermission -> PrimaryButton(stringResource(R.string.updates_install), Modifier.weight(1f)) { Updates.install(context) }
                is UpdateState.Failed -> PrimaryButton(stringResource(R.string.updates_try_again), Modifier.weight(1f)) {
                    if (state.info != null) Updates.download(context) else Updates.check(context)
                }
                is UpdateState.UpToDate -> SecondaryButton(stringResource(R.string.updates_check), Modifier.weight(1f)) { Updates.check(context) }
                else -> Unit
            }
        }
    }
}

// One change of the release, its name, and under it the first words of its description in gray with the word that opens it, or the whole description with the word that closes it
@Composable
private fun NoteItem(note: UpdateNote, key: String) {
    var isOpen by remember(key) { mutableStateOf(false) }
    var isCut by remember(key, note.details) { mutableStateOf(false) }
    val style = MaterialTheme.typography.bodyMedium.merge(SettingsType.RowSubtitle)
    val toggle = @Composable { label: Int ->
        Text(text = stringResource(label), style = style.copy(fontWeight = FontWeight.SemiBold), color = MaterialTheme.colorScheme.primary)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (note.details != null && (isCut || isOpen)) Modifier.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { isOpen = !isOpen } else Modifier)
    ) {
        Text(
            text = note.title,
            style = MaterialTheme.typography.bodyMedium.merge(SettingsType.RowTitle),
            color = MaterialTheme.colorScheme.onSurface
        )
        if (note.details != null) {
            if (isOpen) {
                Text(text = note.details, style = style, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = SettingsDimens.NotesDetailTop))
                toggle(R.string.updates_note_less)
            } else {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = SettingsDimens.NotesDetailTop)) {
                    Text(
                        text = note.details,
                        style = style,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        onTextLayout = { isCut = it.hasVisualOverflow },
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (isCut) {
                        Text(text = " ", style = style)
                        toggle(R.string.updates_note_more)
                    }
                }
            }
        }
    }
}

// The notes in another language, the titles and the descriptions go in one request and come back in the same order
private suspend fun translateNotes(notes: List<UpdateNote>, language: String): Result<List<UpdateNote>> {
    val lines = notes.flatMap { listOfNotNull(it.title, it.details) }

    return LyricsTranslator.translate(lines, language).map { translation ->
        var next = 0
        notes.map { note ->
            val title = translation.lines[next++]
            UpdateNote(title, note.details?.let { translation.lines[next++] })
        }
    }
}

// The size and the day of a release as the line under its name
private fun detailsOf(info: UpdateInfo, dateFormat: DateTimeFormatter, size: (Long) -> String): String? =
    listOfNotNull(
        info.asset?.sizeBytes?.takeIf { it > 0 }?.let(size),
        info.publishedAt?.let { runCatching { dateFormat.format(Instant.parse(it).atZone(ZoneId.systemDefault())) }.getOrNull() }
    ).joinToString(" • ").ifEmpty { null }

// The button that goes forward, light on the dark theme and dark on the light one, as the play button
@Composable
private fun PrimaryButton(text: String, modifier: Modifier, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.onBackground, contentColor = MaterialTheme.colorScheme.background),
        modifier = modifier
    ) { Text(text = text, maxLines = 1) }
}

// The button of a way that is not the main one
@Composable
private fun SecondaryButton(text: String, modifier: Modifier, onClick: () -> Unit) {
    FilledTonalButton(onClick = onClick, modifier = modifier) { Text(text = text, maxLines = 1) }
}

// What the percent of the progress is counted against, and the language the notes are written in
private const val PercentTotal = 100f
private const val EnglishLanguage = "en"

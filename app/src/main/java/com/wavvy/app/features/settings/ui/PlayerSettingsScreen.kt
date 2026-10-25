package com.wavvy.app.features.settings.ui

// Compose layouts and foundations
import android.app.TimePickerDialog
import android.content.Context
import android.text.format.DateFormat
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
// Material 3 components
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
// Compose state and runtime
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
// UI styling and utilities
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
// Time
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.util.Locale
// Coroutines
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
// Project resources
import com.wavvy.app.R
import com.wavvy.app.core.designsystem.components.OverlaySheet
import com.wavvy.app.core.navigation.ItemNavigator
import com.wavvy.app.features.playlist.ui.SortChoice
import com.wavvy.app.features.playlist.ui.SortChoiceSheet
import com.wavvy.app.features.settings.data.CrossfadeDurations
import com.wavvy.app.features.settings.data.CrossfadeSettings
import com.wavvy.app.features.settings.data.LoudnessLevel
import com.wavvy.app.features.settings.data.AudioLevel
import com.wavvy.app.features.settings.data.PlaybackSettings
import com.wavvy.app.features.settings.data.SleepAutoMinutes
import com.wavvy.app.features.settings.data.SleepRepeat
import com.wavvy.app.features.settings.data.SleepSettings
import com.wavvy.app.features.settings.data.SettingsStore
import com.wavvy.app.features.settings.data.SponsorBlockSettings
import com.wavvy.app.features.settings.ui.components.SettingsCard
import com.wavvy.app.features.settings.ui.components.SettingsChoiceRow
import com.wavvy.app.features.settings.ui.components.SettingsDivider
import com.wavvy.app.features.settings.ui.components.SettingsGroup
import com.wavvy.app.features.settings.ui.components.SettingsNavigateRow
import com.wavvy.app.features.settings.ui.components.SettingsPage
import com.wavvy.app.features.settings.ui.components.SettingsSwitchRow

// How long the screen waits after coming back before it slides to the end, so the page is laid out first
private const val ReturnScrollMillis = 250L

// Lengths under a second are said in milliseconds, the others in seconds
private const val MillisPerSecond = 1000

// The days in a week and the minutes in an hour
private const val DaysInWeek = 7
private const val MinutesPerHour = 60

// The settings of the player, the crossfade that blends a song into the next one and the SponsorBlock that skips the marked parts
@Composable
fun PlayerSettingsScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val store = remember { SettingsStore(context) }
    val settings by store.crossfade.collectAsState(initial = CrossfadeSettings())
    val sponsor by store.sponsorBlock.collectAsState(initial = SponsorBlockSettings())
    val playback by store.playback.collectAsState(initial = PlaybackSettings())
    val sleep by store.sleep.collectAsState(initial = SleepSettings())
    var isPickingDays by remember { mutableStateOf(false) }

    // Turning SponsorBlock on opens the choice of what to skip, and coming back the screen slides down to the end so the new row is seen
    val scroll = rememberScrollState()
    var goToEnd by rememberSaveable { mutableStateOf(false) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && goToEnd) {
                goToEnd = false
                scope.launch {
                    delay(ReturnScrollMillis)
                    scroll.animateScrollTo(scroll.maxValue)
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    if (isPickingDays) {
        DaysDialog(
            selected = sleep.customDays,
            onChange = { scope.launch { store.setSleep(sleep.copy(customDays = it)) } },
            onDismiss = { isPickingDays = false }
        )
    }

    val qualityTitle = stringResource(R.string.settings_audio_quality)
    val qualityChoices = AudioLevel.entries.map { level ->
        SortChoice(stringResource(level.titleRes), level == playback.quality, level == PlaybackSettings().quality, stringResource(level.detailRes)) { scope.launch { store.setPlayback(playback.copy(quality = level)) } }
    }
    val loudnessTitle = stringResource(R.string.settings_loudness)
    val loudnessChoices = LoudnessLevel.entries.map { level ->
        SortChoice(stringResource(level.titleRes), level == playback.loudness, level == PlaybackSettings().loudness) { scope.launch { store.setPlayback(playback.copy(loudness = level)) } }
    }
    val minutesTitle = stringResource(R.string.settings_sleep_minutes)
    val minutesChoices = SleepAutoMinutes.map { minutes ->
        SortChoice(stringResource(R.string.settings_sleep_minutes_value, minutes), minutes == sleep.minutes, minutes == SleepSettings().minutes) { scope.launch { store.setSleep(sleep.copy(minutes = minutes)) } }
    }
    val repeatTitle = stringResource(R.string.settings_sleep_repeat)
    val repeatChoices = SleepRepeat.entries.map { option ->
        SortChoice(stringResource(option.titleRes), option == sleep.repeat, option == SleepSettings().repeat) {
            scope.launch { store.setSleep(sleep.copy(repeat = option)) }
            if (option == SleepRepeat.Custom) isPickingDays = true
        }
    }

    val durationTitle = stringResource(R.string.settings_crossfade_duration)
    val durationChoices = CrossfadeDurations.map { duration ->
        SortChoice(durationLabel(duration), duration == settings.durationMs, duration == CrossfadeSettings().durationMs) { scope.launch { store.setCrossfadeDuration(duration) } }
    }

    SettingsPage(title = stringResource(R.string.settings_player), onBack = onBack, modifier = modifier, scrollState = scroll) {
        // How the music plays and what the player does around it
        SettingsGroup(title = stringResource(R.string.settings_playback_group)) {
            SettingsChoiceRow(
                title = qualityTitle,
                value = stringResource(playback.quality.titleRes),
                onClick = { OverlaySheet.show { SortChoiceSheet(title = qualityTitle, choices = qualityChoices, onDismiss = OverlaySheet::dismiss) } }
            )
            SettingsDivider()
            SettingsSwitchRow(
                title = stringResource(R.string.settings_skip_silence),
                subtitle = stringResource(R.string.settings_skip_silence_subtitle),
                checked = playback.skipSilence,
                onCheckedChange = { scope.launch { store.setPlayback(playback.copy(skipSilence = it)) } }
            )
            SettingsDivider()
            SettingsSwitchRow(
                title = stringResource(R.string.settings_normalize),
                subtitle = stringResource(R.string.settings_normalize_subtitle),
                checked = playback.normalizeAudio,
                onCheckedChange = { scope.launch { store.setPlayback(playback.copy(normalizeAudio = it)) } }
            )
            if (playback.normalizeAudio) {
                SettingsDivider()
                SettingsChoiceRow(
                    title = loudnessTitle,
                    value = stringResource(playback.loudness.titleRes),
                    onClick = { OverlaySheet.show { SortChoiceSheet(title = loudnessTitle, choices = loudnessChoices, onDismiss = OverlaySheet::dismiss) } }
                )
            }
            SettingsDivider()
            SettingsSwitchRow(
                title = stringResource(R.string.settings_persistent_queue),
                subtitle = stringResource(R.string.settings_persistent_queue_subtitle),
                checked = playback.persistentQueue,
                onCheckedChange = { scope.launch { store.setPlayback(playback.copy(persistentQueue = it)) } }
            )
            SettingsDivider()
            SettingsSwitchRow(
                title = stringResource(R.string.settings_stop_on_removed),
                subtitle = stringResource(R.string.settings_stop_on_removed_subtitle),
                checked = playback.stopOnTaskRemoved,
                onCheckedChange = { scope.launch { store.setPlayback(playback.copy(stopOnTaskRemoved = it)) } }
            )
            SettingsDivider()
            SettingsSwitchRow(
                title = stringResource(R.string.settings_pause_muted),
                subtitle = stringResource(R.string.settings_pause_muted_subtitle),
                checked = playback.pauseWhenMuted,
                onCheckedChange = { scope.launch { store.setPlayback(playback.copy(pauseWhenMuted = it)) } }
            )
            SettingsDivider()
            SettingsSwitchRow(
                title = stringResource(R.string.settings_resume_bluetooth),
                subtitle = stringResource(R.string.settings_resume_bluetooth_subtitle),
                checked = playback.resumeOnBluetooth,
                onCheckedChange = { scope.launch { store.setPlayback(playback.copy(resumeOnBluetooth = it)) } }
            )
            SettingsDivider()
            SettingsSwitchRow(
                title = stringResource(R.string.settings_keep_screen_on),
                subtitle = stringResource(R.string.settings_keep_screen_on_subtitle),
                checked = playback.keepScreenOn,
                onCheckedChange = { scope.launch { store.setPlayback(playback.copy(keepScreenOn = it)) } }
            )
        }

        SettingsGroup(title = stringResource(R.string.settings_crossfade)) {
            SettingsSwitchRow(
                title = stringResource(R.string.settings_crossfade_enable),
                subtitle = stringResource(R.string.settings_crossfade_enable_subtitle),
                checked = settings.enabled,
                onCheckedChange = { scope.launch { store.setCrossfadeEnabled(it) } }
            )
            if (settings.enabled) {
                SettingsDivider()
                SettingsChoiceRow(
                    title = durationTitle,
                    value = durationLabel(settings.durationMs),
                    onClick = { OverlaySheet.show { SortChoiceSheet(title = durationTitle, choices = durationChoices, onDismiss = OverlaySheet::dismiss) } }
                )
                SettingsDivider()
                SettingsSwitchRow(
                    title = stringResource(R.string.settings_crossfade_on_skip),
                    subtitle = stringResource(R.string.settings_crossfade_on_skip_subtitle),
                    checked = settings.onSkip,
                    onCheckedChange = { scope.launch { store.setCrossfadeOnSkip(it) } }
                )
                SettingsDivider()
                SettingsSwitchRow(
                    title = stringResource(R.string.settings_crossfade_on_auto),
                    subtitle = stringResource(R.string.settings_crossfade_on_auto_subtitle),
                    checked = settings.onAutoAdvance,
                    onCheckedChange = { scope.launch { store.setCrossfadeOnAutoAdvance(it) } }
                )
            }
        }

        // How the sleep timer ends and when it starts by itself
        SettingsGroup(title = stringResource(R.string.settings_sleep_group)) {
            SettingsSwitchRow(
                title = stringResource(R.string.settings_sleep_fade),
                subtitle = stringResource(R.string.settings_sleep_fade_subtitle),
                checked = sleep.fadeOut,
                onCheckedChange = { scope.launch { store.setSleep(sleep.copy(fadeOut = it)) } }
            )
            SettingsDivider()
            SettingsSwitchRow(
                title = stringResource(R.string.settings_sleep_finish),
                subtitle = stringResource(R.string.settings_sleep_finish_subtitle),
                checked = sleep.finishSong,
                onCheckedChange = { scope.launch { store.setSleep(sleep.copy(finishSong = it)) } }
            )
            SettingsDivider()
            SettingsSwitchRow(
                title = stringResource(R.string.settings_sleep_auto),
                subtitle = stringResource(R.string.settings_sleep_auto_subtitle),
                checked = sleep.auto,
                onCheckedChange = { scope.launch { store.setSleep(sleep.copy(auto = it)) } }
            )
            if (sleep.auto) {
                SettingsDivider()
                SettingsChoiceRow(
                    title = minutesTitle,
                    value = stringResource(R.string.settings_sleep_minutes_value, sleep.minutes),
                    onClick = { OverlaySheet.show { SortChoiceSheet(title = minutesTitle, choices = minutesChoices, onDismiss = OverlaySheet::dismiss) } }
                )
                SettingsDivider()
                SettingsChoiceRow(
                    title = repeatTitle,
                    value = stringResource(sleep.repeat.titleRes),
                    onClick = { OverlaySheet.show { SortChoiceSheet(title = repeatTitle, choices = repeatChoices, onDismiss = OverlaySheet::dismiss) } }
                )
                if (sleep.repeat == SleepRepeat.Custom) {
                    SettingsDivider()
                    SettingsChoiceRow(
                        title = stringResource(R.string.settings_sleep_days),
                        value = sleep.customDays.sorted().joinToString { dayName(it) },
                        onClick = { isPickingDays = true }
                    )
                }
                SettingsDivider()
                SettingsChoiceRow(
                    title = stringResource(R.string.settings_sleep_start),
                    value = timeText(sleep.startMinute),
                    onClick = { pickTime(context, sleep.startMinute) { scope.launch { store.setSleep(sleep.copy(startMinute = it)) } } }
                )
                SettingsDivider()
                SettingsChoiceRow(
                    title = stringResource(R.string.settings_sleep_end),
                    value = timeText(sleep.endMinute),
                    onClick = { pickTime(context, sleep.endMinute) { scope.launch { store.setSleep(sleep.copy(endMinute = it)) } } }
                )
            }
        }

        // The parts that the community of SponsorBlock marked, skipped as the user chose
        SettingsGroup(title = stringResource(R.string.settings_sponsorblock)) {
            SettingsSwitchRow(
                title = stringResource(R.string.settings_sponsorblock_enable),
                subtitle = stringResource(R.string.settings_sponsorblock_enable_subtitle),
                checked = sponsor.enabled,
                onCheckedChange = {
                    scope.launch { store.setSponsorBlockEnabled(it) }
                    // Turning it on goes straight to the choice of what to skip, so it is not missed
                    if (it) {
                        goToEnd = true
                        ItemNavigator.openSettingsSection(SponsorBlockSection)
                    }
                }
            )
            if (sponsor.enabled) {
                SettingsDivider()
                SettingsNavigateRow(
                    title = stringResource(R.string.settings_sponsorblock_categories),
                    subtitle = pluralStringResource(R.plurals.settings_sponsorblock_selected, sponsor.categories.size, sponsor.categories.size),
                    onClick = { ItemNavigator.openSettingsSection(SponsorBlockSection) }
                )
            }
        }
    }
}

// The box where the user picks the days a schedule runs on, at least one stays on
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DaysDialog(selected: Set<Int>, onChange: (Set<Int>) -> Unit, onDismiss: () -> Unit) {
    BasicAlertDialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(SettingsDimens.DialogCorner), color = MaterialTheme.colorScheme.surfaceContainerHigh) {
            Column(modifier = Modifier.padding(vertical = SettingsDimens.DialogPadding)) {
                (1..DaysInWeek).forEach { day ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val next = if (day in selected) selected - day else selected + day
                                if (next.isNotEmpty()) onChange(next)
                            }
                            .padding(horizontal = SettingsDimens.DialogPadding)
                    ) {
                        Checkbox(checked = day in selected, onCheckedChange = null)
                        Text(text = dayName(day, isFull = true), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(start = SettingsDimens.RowIconGap, top = SettingsDimens.RowVertical, bottom = SettingsDimens.RowVertical))
                    }
                }
                TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End).padding(end = SettingsDimens.DialogPadding)) {
                    Text(text = stringResource(R.string.settings_days_ok))
                }
            }
        }
    }
}

// The name of a day, 1 for Monday to 7 for Sunday, in the language of the user
private fun dayName(day: Int, isFull: Boolean = false): String =
    DayOfWeek.of(day).getDisplayName(if (isFull) TextStyle.FULL else TextStyle.SHORT, Locale.getDefault())

// A time of the day, counted in minutes after midnight, written the way the phone writes the hour
private fun timeText(minutes: Int): String =
    LocalTime.of(minutes / MinutesPerHour, minutes % MinutesPerHour).format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT))

// Opens the clock of the system to pick a time of the day
private fun pickTime(context: Context, minutes: Int, onPicked: (Int) -> Unit) {
    TimePickerDialog(context, { _, hour, minute -> onPicked(hour * MinutesPerHour + minute) }, minutes / MinutesPerHour, minutes % MinutesPerHour, DateFormat.is24HourFormat(context)).show()
}

// The length in the words of the language, milliseconds under a second and seconds from there
@Composable
private fun durationLabel(durationMs: Int): String =
    if (durationMs < MillisPerSecond) stringResource(R.string.settings_crossfade_ms, durationMs) else stringResource(R.string.settings_crossfade_s, durationMs / MillisPerSecond)

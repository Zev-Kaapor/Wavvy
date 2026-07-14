package com.lonewolf.wavvy.ui.settings.sections

// Compose foundation and layout
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
// Material 3 and icons
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeMute
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.rounded.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
// Project resources
import com.lonewolf.wavvy.R
import com.lonewolf.wavvy.ui.settings.components.SettingsGroupCard
import com.lonewolf.wavvy.ui.settings.components.SettingsInteractiveRow
import com.lonewolf.wavvy.ui.settings.components.SettingsToggleRow

// Playback and audio engine engine subscreen layout
@Composable
fun PlayerSubScreen(
    queueLimit: Int,
    onQueueLimitChange: (Int) -> Unit,
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
        SettingsGroupCard(title = stringResource(R.string.setting_subgroup_audio_engine)) {
            SettingsInteractiveRow(title = stringResource(R.string.setting_audio_quality), subtitle = stringResource(R.string.setting_audio_quality_high), icon = Icons.Rounded.HighQuality, showDivider = true)
            SettingsToggleRow(title = stringResource(R.string.setting_audio_fade), subtitle = stringResource(R.string.setting_audio_fade_desc), icon = Icons.Rounded.Waves, showDivider = true)
            SettingsInteractiveRow(title = stringResource(R.string.setting_audio_fade_duration), subtitle = stringResource(R.string.setting_audio_fade_duration_default), icon = Icons.Rounded.Timer, showDivider = true)
            SettingsToggleRow(title = stringResource(R.string.setting_audio_normalization), subtitle = stringResource(R.string.setting_audio_normalization_desc), icon = Icons.Rounded.Equalizer, showDivider = false)
        }

        SettingsGroupCard(title = stringResource(R.string.setting_subgroup_silence)) {
            SettingsToggleRow(title = stringResource(R.string.setting_skip_silence), subtitle = stringResource(R.string.setting_skip_silence_desc), icon = Icons.AutoMirrored.Rounded.VolumeMute, showDivider = true)
            SettingsToggleRow(title = stringResource(R.string.setting_auto_skip_silence), subtitle = stringResource(R.string.setting_auto_skip_silence_desc), icon = Icons.Rounded.AutoMode, showDivider = true)
            SettingsToggleRow(title = stringResource(R.string.setting_pause_on_muted), subtitle = stringResource(R.string.setting_pause_on_muted_desc), icon = Icons.AutoMirrored.Rounded.VolumeOff, showDivider = false)
        }

        SettingsGroupCard(title = stringResource(R.string.setting_subgroup_queue)) {
            SettingsInteractiveRow(
                title = stringResource(R.string.setting_quick_choices),
                subtitle = "${stringResource(R.string.setting_quick_choices_desc)} $queueLimit",
                icon = Icons.Rounded.FlashOn,
                showDivider = false,
                onClick = { onQueueLimitChange(queueLimit + 25) }
            )
        }

        SettingsGroupCard(title = stringResource(R.string.setting_subgroup_utility)) {
            SettingsToggleRow(title = stringResource(R.string.setting_sleep_timer), subtitle = stringResource(R.string.setting_sleep_timer_desc), icon = Icons.Rounded.Snooze, showDivider = false)
        }

        Spacer(modifier = Modifier.height(if (isPlayerActive) 110.dp else 16.dp))
    }
}

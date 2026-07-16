package com.lonewolf.wavvy.ui.settings.sections

// Compose foundation and layout
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
// Material 3 and icons
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ManageSearch
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
// Coroutines
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
// Project resources
import com.lonewolf.wavvy.R
import com.lonewolf.wavvy.ui.common.components.CustomToast
import com.lonewolf.wavvy.ui.settings.components.SettingsActionRow
import com.lonewolf.wavvy.ui.settings.components.SettingsGroupCard
import com.lonewolf.wavvy.ui.settings.components.SettingsToggleRow
import kotlin.time.Duration.Companion.milliseconds

// Privacy and history management subscreen layout
@Composable
fun PrivacySubScreen(
    onClearPlaybackHistory: suspend () -> Unit,
    onClearSearchHistory: suspend () -> Unit,
    isPlayerActive: Boolean,
    modifier: Modifier = Modifier
) {
    val internalScrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()

    // Toast controllers
    var toastMessage by remember { mutableStateOf<String?>(null) }
    var toastSubtitle by remember { mutableStateOf<String?>(null) }
    var showToast by remember { mutableStateOf(false) }
    var toastId by remember { mutableIntStateOf(0) }
    var toastJob by remember { mutableStateOf<Job?>(null) }

    // Success state resource strings
    val playbackSuccessMessage = stringResource(R.string.setting_clear_playback_history_success)
    val playbackSuccessSubtitle = stringResource(R.string.setting_clear_playback_history_success_desc)
    val searchSuccessMessage = stringResource(R.string.setting_clear_search_history_success)
    val searchSuccessSubtitle = stringResource(R.string.setting_clear_search_history_success_desc)

    fun triggerToast(message: String, subtitle: String) {
        toastJob?.cancel()
        toastJob = coroutineScope.launch {
            if (showToast) {
                showToast = false
                delay(220.milliseconds)
            }
            toastMessage = message
            toastSubtitle = subtitle
            toastId++
            showToast = true
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(internalScrollState)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            SettingsGroupCard(title = stringResource(R.string.setting_subgroup_playback_history)) {
                SettingsToggleRow(
                    title = stringResource(R.string.setting_pause_playback_history),
                    subtitle = stringResource(R.string.setting_pause_playback_history_desc),
                    icon = Icons.Rounded.HistoryToggleOff,
                    showDivider = true
                )
                SettingsActionRow(
                    title = stringResource(R.string.setting_clear_playback_history),
                    subtitle = stringResource(R.string.setting_clear_playback_history_desc),
                    icon = Icons.Rounded.DeleteSweep,
                    showDivider = false,
                    onClick = {
                        coroutineScope.launch {
                            onClearPlaybackHistory()
                            triggerToast(playbackSuccessMessage, playbackSuccessSubtitle)
                        }
                    }
                )
            }

            SettingsGroupCard(title = stringResource(R.string.setting_subgroup_search_history)) {
                SettingsToggleRow(
                    title = stringResource(R.string.setting_pause_search_history),
                    subtitle = stringResource(R.string.setting_pause_search_history_desc),
                    icon = Icons.AutoMirrored.Rounded.ManageSearch,
                    showDivider = true
                )
                SettingsActionRow(
                    title = stringResource(R.string.setting_clear_search_history),
                    subtitle = stringResource(R.string.setting_clear_search_history_desc),
                    icon = Icons.Rounded.HistoryEdu,
                    showDivider = false,
                    onClick = {
                        coroutineScope.launch {
                            onClearSearchHistory()
                            triggerToast(searchSuccessMessage, searchSuccessSubtitle)
                        }
                    }
                )
            }

            SettingsGroupCard(title = stringResource(R.string.setting_subgroup_security)) {
                SettingsToggleRow(
                    title = stringResource(R.string.setting_disable_screenshots),
                    subtitle = stringResource(R.string.setting_disable_screenshots_desc),
                    icon = Icons.Rounded.NoPhotography,
                    showDivider = false
                )
            }

            Spacer(modifier = Modifier.height(if (isPlayerActive) 110.dp else 16.dp))
        }

        // Atmospheric background gradient
        AnimatedVisibility(
            visible = showToast,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .zIndex(9f)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (isPlayerActive) 230.dp else 140.dp)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                                MaterialTheme.colorScheme.surface
                            )
                        )
                    )
            )
        }

        // Floating toast layout
        AnimatedVisibility(
            visible = showToast,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = if (isPlayerActive) 96.dp else 24.dp)
                .zIndex(10f)
        ) {
            if (toastMessage != null) {
                key(toastId) {
                    CustomToast(
                        message = toastMessage!!,
                        subtitle = toastSubtitle,
                        durationMillis = 3000,
                        onDismiss = { showToast = false }
                    )
                }
            }
        }
    }
}

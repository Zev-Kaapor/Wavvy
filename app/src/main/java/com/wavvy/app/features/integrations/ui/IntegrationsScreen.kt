package com.wavvy.app.features.integrations.ui

// Compose state and runtime
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
// UI styling and utilities
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import kotlinx.coroutines.launch
// Project resources
import com.wavvy.app.R
import com.wavvy.app.core.designsystem.components.ConfirmDialog
import com.wavvy.app.core.discord.DiscordAuth
import com.wavvy.app.core.discord.DiscordStore
import com.wavvy.app.features.settings.ui.components.SettingsGroup
import com.wavvy.app.features.settings.ui.components.SettingsNavigateRow
import com.wavvy.app.features.settings.ui.components.SettingsPage
import com.wavvy.app.features.settings.ui.components.StatusDot
import com.wavvy.app.features.settings.ui.components.StatusKind

// The services the user can link to the app, each one off until the user turns it on and says it understands what it does
@Composable
fun IntegrationsScreen(onBack: () -> Unit, onDiscord: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val store = remember { DiscordStore(context) }
    val isConnected by store.isConnected.collectAsState(initial = false)
    var isWarning by rememberSaveable { mutableStateOf(false) }

    // The browser wakes up while the warning is read
    LaunchedEffect(isWarning) { if (isWarning) DiscordAuth.warmUp(context) }

    // The risk is said before the login opens, and the user has to say yes to it
    if (isWarning) {
        ConfirmDialog(
            title = stringResource(R.string.integrations_discord_warning_title),
            text = stringResource(R.string.integrations_discord_warning_text),
            confirmLabel = stringResource(R.string.integrations_discord_warning_confirm),
            cancelLabel = stringResource(R.string.playlist_cancel),
            textAlign = TextAlign.Start,
            onConfirm = {
                isWarning = false
                scope.launch { DiscordAuth.start(context) }
            },
            onDismiss = {
                isWarning = false
                DiscordAuth.coolDown(context)
            }
        )
    }

    SettingsPage(title = stringResource(R.string.profile_integrations), onBack = onBack, modifier = modifier) {
        SettingsGroup(title = stringResource(R.string.integrations_group_activity)) {
            // The dot tells the state, green when it is linked and gray when it is not
            SettingsNavigateRow(
                title = stringResource(R.string.integrations_discord),
                subtitle = stringResource(R.string.integrations_discord_subtitle),
                titleAccessory = { StatusDot(if (isConnected) StatusKind.On else StatusKind.Off) },
                logo = R.drawable.logo_discord,
                onClick = if (isConnected) onDiscord else ({ isWarning = true })
            )
        }
    }
}

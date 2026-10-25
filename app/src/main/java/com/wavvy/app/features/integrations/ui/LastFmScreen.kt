package com.wavvy.app.features.integrations.ui

// Compose layouts and foundations
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
// Material 3 components
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
// Compose state and runtime
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
// UI styling and utilities
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import kotlinx.coroutines.launch
// Project resources
import com.wavvy.app.R
import com.wavvy.app.core.designsystem.components.Toasts
import com.wavvy.app.core.lastfm.LastFmApi
import com.wavvy.app.core.lastfm.LastFmException
import com.wavvy.app.core.lastfm.LastFmSettings
import com.wavvy.app.core.lastfm.LastFmStore
import com.wavvy.app.features.settings.ui.SettingsDimens
import com.wavvy.app.features.settings.ui.components.SettingsActionRow
import com.wavvy.app.features.settings.ui.components.SettingsDivider
import com.wavvy.app.features.settings.ui.components.SettingsGroup
import com.wavvy.app.features.settings.ui.components.SettingsPage
import com.wavvy.app.features.settings.ui.components.SettingsSwitchRow

// What is sent to Last.fm and the way out of the account
@Composable
fun LastFmScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val store = remember { LastFmStore(context) }
    val settings by store.settings.collectAsState(initial = LastFmSettings())
    val session by store.session.collectAsState(initial = null)

    fun change(next: (LastFmSettings) -> LastFmSettings) {
        scope.launch { store.update(next) }
    }

    SettingsPage(title = stringResource(R.string.integrations_lastfm), onBack = onBack, modifier = modifier) {
        SettingsGroup(title = stringResource(R.string.integrations_group_scrobbling)) {
            SettingsSwitchRow(
                title = stringResource(R.string.integrations_lastfm_scrobble),
                subtitle = stringResource(R.string.integrations_lastfm_scrobble_subtitle),
                checked = settings.isEnabled,
                onCheckedChange = { value -> change { it.copy(isEnabled = value) } }
            )
            SettingsDivider()
            SettingsSwitchRow(
                title = stringResource(R.string.integrations_lastfm_now_playing),
                subtitle = stringResource(R.string.integrations_lastfm_now_playing_subtitle),
                checked = settings.showNowPlaying,
                onCheckedChange = { value -> change { it.copy(showNowPlaying = value) } }
            )
        }

        SettingsGroup(title = stringResource(R.string.settings_group_account)) {
            SettingsActionRow(
                title = stringResource(R.string.integrations_lastfm_disconnect),
                subtitle = stringResource(R.string.integrations_lastfm_disconnect_subtitle, session?.name.orEmpty()),
                onClick = {
                    scope.launch { store.clear() }
                    onBack()
                }
            )
        }
    }
}

// The box where the user logs in to Last.fm, a wrong name or password keeps it open and says so
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LastFmLoginDialog(onDone: () -> Unit, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var username by rememberSaveable { mutableStateOf("") }
    // The password is not kept in the state that survives a rotation
    var password by remember { mutableStateOf("") }
    var errorRes by rememberSaveable { mutableStateOf<Int?>(null) }
    var isBusy by rememberSaveable { mutableStateOf(false) }
    val done = stringResource(R.string.integrations_lastfm_done)

    fun connect() {
        if (isBusy || username.isBlank() || password.isEmpty()) return
        isBusy = true
        errorRes = null
        scope.launch {
            runCatching { LastFmApi.login(username.trim(), password) }
                .onSuccess {
                    LastFmStore(context).saveSession(it)
                    Toasts.show(done)
                    onDone()
                }
                .onFailure {
                    errorRes = if (it is LastFmException) R.string.integrations_lastfm_wrong else R.string.integrations_lastfm_offline
                    isBusy = false
                }
        }
    }

    BasicAlertDialog(onDismissRequest = { if (!isBusy) onDismiss() }) {
        Surface(shape = RoundedCornerShape(SettingsDimens.ProxyDialogCorner), color = MaterialTheme.colorScheme.surfaceContainerHigh) {
            Column(verticalArrangement = Arrangement.spacedBy(SettingsDimens.ProxyFieldGap), modifier = Modifier.padding(SettingsDimens.ProxyDialogPadding)) {
                Text(text = stringResource(R.string.integrations_lastfm_login_title), style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface)
                Text(
                    text = stringResource(R.string.integrations_lastfm_login_text),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                TextField(
                    value = username,
                    onValueChange = {
                        username = it
                        errorRes = null
                    },
                    placeholder = { Text(text = stringResource(R.string.integrations_lastfm_username)) },
                    singleLine = true,
                    isError = errorRes != null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent),
                    modifier = Modifier.fillMaxWidth()
                )
                TextField(
                    value = password,
                    onValueChange = {
                        password = it
                        errorRes = null
                    },
                    placeholder = { Text(text = stringResource(R.string.integrations_lastfm_password)) },
                    singleLine = true,
                    isError = errorRes != null,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    supportingText = errorRes?.let { res -> ({ Text(text = stringResource(res)) }) },
                    colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(horizontalArrangement = Arrangement.spacedBy(SettingsDimens.ProxyFieldGap), modifier = Modifier.fillMaxWidth().padding(top = SettingsDimens.ProxyButtonsTop)) {
                    FilledTonalButton(onClick = onDismiss, enabled = !isBusy, modifier = Modifier.weight(1f)) {
                        Text(text = stringResource(R.string.playlist_cancel), maxLines = 1)
                    }
                    Button(
                        onClick = ::connect,
                        enabled = !isBusy && username.isNotBlank() && password.isNotEmpty(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.onBackground,
                            contentColor = MaterialTheme.colorScheme.background
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(text = stringResource(R.string.integrations_lastfm_connect), maxLines = 1)
                    }
                }
            }
        }
    }
}

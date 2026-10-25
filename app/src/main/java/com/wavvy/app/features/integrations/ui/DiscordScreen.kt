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
import kotlinx.coroutines.launch
// Project resources
import com.wavvy.app.R
import com.wavvy.app.core.designsystem.components.OverlaySheet
import com.wavvy.app.core.discord.DiscordActivityKind
import com.wavvy.app.core.discord.DiscordAuth
import com.wavvy.app.core.discord.DiscordButton
import com.wavvy.app.core.discord.DiscordSettings
import com.wavvy.app.core.discord.DiscordStatus
import com.wavvy.app.core.discord.DiscordStore
import com.wavvy.app.features.playlist.ui.SortChoice
import com.wavvy.app.features.playlist.ui.SortChoiceSheet
import com.wavvy.app.features.settings.ui.SettingsDimens
import com.wavvy.app.features.settings.ui.components.SettingsActionRow
import com.wavvy.app.features.settings.ui.components.SettingsChoiceRow
import com.wavvy.app.features.settings.ui.components.SettingsDivider
import com.wavvy.app.features.settings.ui.components.SettingsGroup
import com.wavvy.app.features.settings.ui.components.SettingsPage
import com.wavvy.app.features.settings.ui.components.SettingsSwitchRow

// A text the user is changing, what it is called, what it holds and what to do with the new one
private class TextEdit(val title: String, val initial: String, val isUrl: Boolean, val onSave: (String) -> Unit)

// What the activity of Discord shows and how, and the way out of the account
@Composable
fun DiscordScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val store = remember { DiscordStore(context) }
    val settings by store.settings.collectAsState(initial = DiscordSettings())
    var edit by remember { mutableStateOf<TextEdit?>(null) }
    val defaultText = stringResource(R.string.integrations_discord_default)

    fun change(next: (DiscordSettings) -> DiscordSettings) {
        scope.launch { store.update(next) }
    }

    // A text row, it says the default when the text is empty and opens the box to change it
    @Composable
    fun TextRow(title: String, value: String, isUrl: Boolean = false, onSave: (String) -> Unit) {
        SettingsChoiceRow(title = title, value = value.ifBlank { defaultText }, onClick = { edit = TextEdit(title, value, isUrl, onSave) })
    }

    edit?.let { current ->
        DiscordTextDialog(title = current.title, initial = current.initial, isUrl = current.isUrl, onSave = { current.onSave(it); edit = null }, onDismiss = { edit = null })
    }

    val statusTitle = stringResource(R.string.integrations_discord_status)
    val statusChoices = DiscordStatus.entries.map { status ->
        SortChoice(stringResource(status.titleRes), status == settings.status, status == DiscordSettings().status) { change { it.copy(status = status) } }
    }
    val kindTitle = stringResource(R.string.integrations_discord_kind)
    val kindChoices = DiscordActivityKind.entries.map { kind ->
        SortChoice(stringResource(kind.titleRes), kind == settings.kind, kind == DiscordSettings().kind) { change { it.copy(kind = kind) } }
    }

    SettingsPage(title = stringResource(R.string.integrations_discord), onBack = onBack, modifier = modifier) {
        SettingsGroup(title = stringResource(R.string.integrations_group_activity)) {
            SettingsSwitchRow(
                title = stringResource(R.string.integrations_discord_presence),
                subtitle = stringResource(R.string.integrations_discord_presence_subtitle),
                checked = settings.isEnabled,
                onCheckedChange = { value -> change { it.copy(isEnabled = value) } }
            )
            SettingsDivider()
            SettingsChoiceRow(
                title = statusTitle,
                value = stringResource(settings.status.titleRes),
                onClick = { OverlaySheet.show { SortChoiceSheet(title = statusTitle, choices = statusChoices, onDismiss = OverlaySheet::dismiss) } }
            )
            SettingsDivider()
            SettingsChoiceRow(
                title = kindTitle,
                value = stringResource(settings.kind.titleRes),
                onClick = { OverlaySheet.show { SortChoiceSheet(title = kindTitle, choices = kindChoices, onDismiss = OverlaySheet::dismiss) } }
            )
        }

        SettingsGroup(title = stringResource(R.string.integrations_group_texts)) {
            TextRow(stringResource(R.string.integrations_discord_name), settings.name) { value -> change { it.copy(name = value) } }
            SettingsDivider()
            TextRow(stringResource(R.string.integrations_discord_details), settings.details) { value -> change { it.copy(details = value) } }
            SettingsDivider()
            TextRow(stringResource(R.string.integrations_discord_state), settings.state) { value -> change { it.copy(state = value) } }
            SettingsDivider()
            SettingsSwitchRow(
                title = stringResource(R.string.integrations_discord_cover),
                subtitle = stringResource(R.string.integrations_discord_cover_subtitle),
                checked = settings.showCover,
                onCheckedChange = { value -> change { it.copy(showCover = value) } }
            )
        }

        ButtonCard(
            number = 1,
            button = settings.button1,
            textRow = { title, value, isUrl, onSave -> TextRow(title, value, isUrl, onSave) },
            onChange = { next -> change { it.copy(button1 = next(it.button1)) } }
        )
        ButtonCard(
            number = 2,
            button = settings.button2,
            textRow = { title, value, isUrl, onSave -> TextRow(title, value, isUrl, onSave) },
            onChange = { next -> change { it.copy(button2 = next(it.button2)) } }
        )

        SettingsGroup(title = stringResource(R.string.settings_group_account)) {
            SettingsActionRow(
                title = stringResource(R.string.integrations_discord_disconnect),
                subtitle = stringResource(R.string.integrations_discord_disconnect_subtitle),
                onClick = {
                    scope.launch { DiscordAuth.signOut(context) }
                    onBack()
                }
            )
        }
    }
}

// One button under the activity, whether it shows and its label and address
@Composable
private fun ButtonCard(
    number: Int,
    button: DiscordButton,
    textRow: @Composable (String, String, Boolean, (String) -> Unit) -> Unit,
    onChange: ((DiscordButton) -> DiscordButton) -> Unit
) {
    SettingsGroup(title = stringResource(R.string.integrations_discord_button, number)) {
        SettingsSwitchRow(
            title = stringResource(R.string.integrations_discord_button_show),
            subtitle = stringResource(R.string.integrations_discord_button_subtitle),
            checked = button.isEnabled,
            onCheckedChange = { value -> onChange { it.copy(isEnabled = value) } }
        )
        if (button.isEnabled) {
            SettingsDivider()
            textRow(stringResource(R.string.integrations_discord_button_label), button.label, false) { value -> onChange { it.copy(label = value) } }
            SettingsDivider()
            textRow(stringResource(R.string.integrations_discord_button_url), button.url, true) { value -> onChange { it.copy(url = value) } }
        }
    }
}

// The box where a text is changed, an empty text goes back to the default and a wrong address keeps it open
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DiscordTextDialog(title: String, initial: String, isUrl: Boolean, onSave: (String) -> Unit, onDismiss: () -> Unit) {
    var text by rememberSaveable { mutableStateOf(initial) }
    var isWrong by rememberSaveable { mutableStateOf(false) }

    BasicAlertDialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(SettingsDimens.ProxyDialogCorner), color = MaterialTheme.colorScheme.surfaceContainerHigh) {
            Column(verticalArrangement = Arrangement.spacedBy(SettingsDimens.ProxyFieldGap), modifier = Modifier.padding(SettingsDimens.ProxyDialogPadding)) {
                Text(text = title, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface)
                Text(
                    text = stringResource(R.string.integrations_discord_text_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                TextField(
                    value = text,
                    onValueChange = {
                        text = it
                        isWrong = false
                    },
                    placeholder = { Text(text = stringResource(R.string.integrations_discord_default)) },
                    singleLine = true,
                    isError = isWrong,
                    supportingText = if (isWrong) ({ Text(text = stringResource(R.string.integrations_discord_url_error)) }) else null,
                    keyboardOptions = KeyboardOptions(keyboardType = if (isUrl) KeyboardType.Uri else KeyboardType.Text),
                    colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(horizontalArrangement = Arrangement.spacedBy(SettingsDimens.ProxyFieldGap), modifier = Modifier.fillMaxWidth().padding(top = SettingsDimens.ProxyButtonsTop)) {
                    FilledTonalButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text(text = stringResource(R.string.playlist_cancel), maxLines = 1)
                    }
                    Button(
                        onClick = {
                            val value = text.trim()
                            if (isUrl && value.isNotEmpty() && !value.startsWith("http://") && !value.startsWith("https://")) isWrong = true else onSave(value)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.onBackground,
                            contentColor = MaterialTheme.colorScheme.background
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(text = stringResource(R.string.settings_proxy_save), maxLines = 1)
                    }
                }
            }
        }
    }
}

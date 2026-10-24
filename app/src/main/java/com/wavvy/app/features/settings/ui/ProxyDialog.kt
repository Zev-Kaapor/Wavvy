package com.wavvy.app.features.settings.ui

// Compose layouts and foundations
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
// Material 3 components
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
// Compose state and runtime
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
// UI styling and utilities
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
// Project resources
import com.wavvy.app.core.designsystem.components.WavvySwitch
import com.wavvy.app.R
import com.wavvy.app.core.network.WavvyProxy
import com.wavvy.app.features.settings.data.ProxyKind
import com.wavvy.app.features.settings.data.ProxySettings

// The box where the proxy is set, its kind, its address and, when it asks, the login, a bad address keeps it open and says what is wrong
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProxyDialog(initial: ProxySettings, onSave: (ProxySettings) -> Unit, onDismiss: () -> Unit) {
    var kind by rememberSaveable { mutableStateOf(initial.kind) }
    var address by rememberSaveable { mutableStateOf(initial.address) }
    var hasLogin by rememberSaveable { mutableStateOf(initial.username.isNotEmpty() || initial.password.isNotEmpty()) }
    var username by rememberSaveable { mutableStateOf(initial.username) }
    var password by rememberSaveable { mutableStateOf(initial.password) }
    var isAddressWrong by rememberSaveable { mutableStateOf(false) }

    BasicAlertDialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(SettingsDimens.ProxyDialogCorner),
            color = MaterialTheme.colorScheme.surfaceContainerHigh
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(SettingsDimens.ProxyFieldGap),
                modifier = Modifier.verticalScroll(rememberScrollState()).padding(SettingsDimens.ProxyDialogPadding)
            ) {
                Text(
                    text = stringResource(R.string.settings_proxy_title),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Row(horizontalArrangement = Arrangement.spacedBy(SettingsDimens.ProxyFieldGap)) {
                    ProxyKind.entries.forEach { option ->
                        FilterChip(selected = kind == option, onClick = { kind = option }, label = { Text(text = option.label) })
                    }
                }

                ProxyField(
                    value = address,
                    onValueChange = {
                        address = it
                        isAddressWrong = false
                    },
                    placeholder = stringResource(R.string.settings_proxy_address_hint),
                    keyboardType = KeyboardType.Uri,
                    error = if (isAddressWrong) stringResource(R.string.settings_proxy_address_error) else null
                )

                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = stringResource(R.string.settings_proxy_login),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    WavvySwitch(checked = hasLogin, onCheckedChange = { hasLogin = it })
                }
                if (hasLogin) {
                    ProxyField(value = username, onValueChange = { username = it }, placeholder = stringResource(R.string.settings_proxy_username))
                    ProxyField(
                        value = password,
                        onValueChange = { password = it },
                        placeholder = stringResource(R.string.settings_proxy_password),
                        keyboardType = KeyboardType.Password,
                        isSecret = true
                    )
                }

                // The two buttons have the same width, the one that goes on stands out as in the other questions
                Row(horizontalArrangement = Arrangement.spacedBy(SettingsDimens.ProxyFieldGap), modifier = Modifier.fillMaxWidth().padding(top = SettingsDimens.ProxyButtonsTop)) {
                    FilledTonalButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text(text = stringResource(R.string.playlist_cancel), maxLines = 1)
                    }
                    Button(
                        onClick = {
                            if (WavvyProxy.parseAddress(address) == null) {
                                isAddressWrong = true
                            } else {
                                onSave(
                                    initial.copy(
                                        kind = kind,
                                        address = address.trim(),
                                        username = if (hasLogin) username else "",
                                        password = if (hasLogin) password else ""
                                    )
                                )
                            }
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

// A field of one line of the box, with its words inside while it is empty and the words of what is wrong under it
@Composable
private fun ProxyField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    isSecret: Boolean = false,
    error: String? = null
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(text = placeholder) },
        singleLine = true,
        isError = error != null,
        supportingText = error?.let { { Text(text = it) } },
        visualTransformation = if (isSecret) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent
        ),
        modifier = Modifier.fillMaxWidth()
    )
}

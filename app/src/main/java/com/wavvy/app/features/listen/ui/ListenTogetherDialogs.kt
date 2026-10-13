package com.wavvy.app.features.listen.ui

// Android context and intents
import android.content.Context
import android.content.Intent
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
// Compose state and runtime
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
// UI styling and utilities
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import kotlinx.coroutines.launch
// Project resources
import com.wavvy.app.R
import com.wavvy.app.core.designsystem.components.Toasts
import com.wavvy.app.core.designsystem.icons.WavvyIcons
import com.wavvy.app.core.designsystem.theme.WavvyTheme
import com.wavvy.app.core.listen.ListenCodeLength
import com.wavvy.app.core.listen.ListenError
import com.wavvy.app.core.listen.ListenStatus
import com.wavvy.app.core.listen.ListenTogether
import com.wavvy.app.core.listen.RoomState
import com.wavvy.app.features.profile.ui.LocalProfile

// The longest a name can be, the same the server keeps
private const val MaxNameLength = 24
private const val MaxRoomNameLength = 40

// Where the user is in the box, choosing what to do, making a room or typing the code of one
private enum class ListenStep { Choose, Create, Join }

// The boxes of listening together, the room the user is in when there is one, or the way to make a room or enter one
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListenTogetherDialogs(onDismiss: () -> Unit) {
    val room by ListenTogether.room.collectAsState()
    val current = room

    BasicAlertDialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(ListenDimens.DialogCorner), color = MaterialTheme.colorScheme.surfaceContainerHigh) {
            Column(verticalArrangement = Arrangement.spacedBy(ListenDimens.DialogGap), modifier = Modifier.padding(ListenDimens.DialogPadding)) {
                if (current == null) Entry(onDismiss = onDismiss) else InsideRoom(room = current, onDismiss = onDismiss)
            }
        }
    }
}

// The choice between making a room and entering one, and the forms of each
@Composable
private fun Entry(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val profile = LocalProfile.current
    var step by rememberSaveable { mutableStateOf(ListenStep.Choose) }
    var displayName by rememberSaveable { mutableStateOf("") }
    var roomName by rememberSaveable { mutableStateOf("") }
    var code by rememberSaveable { mutableStateOf("") }
    var isWorking by remember { mutableStateOf(false) }

    // A signed in user already has a name, a guest has to give one
    val knownName = if (profile.isSignedIn) profile.name.orEmpty().take(MaxNameLength) else ""
    val finalName = knownName.ifEmpty { displayName.trim() }

    Text(text = stringResource(R.string.listen_title), style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface)

    when (step) {
        ListenStep.Choose -> {
            Text(text = stringResource(R.string.listen_intro), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Button(onClick = { step = ListenStep.Create }, colors = mainButtonColors(), modifier = Modifier.fillMaxWidth()) {
                Text(text = stringResource(R.string.listen_create), maxLines = 1)
            }
            FilledTonalButton(onClick = { step = ListenStep.Join }, modifier = Modifier.fillMaxWidth()) {
                Text(text = stringResource(R.string.listen_join), maxLines = 1)
            }
        }

        ListenStep.Create -> {
            if (knownName.isEmpty()) NameField(value = displayName, onChange = { displayName = it.take(MaxNameLength) }, label = stringResource(R.string.listen_your_name))
            NameField(value = roomName, onChange = { roomName = it.take(MaxRoomNameLength) }, label = stringResource(R.string.listen_room_name))
            FormButtons(
                confirm = stringResource(R.string.listen_create_confirm),
                isEnabled = finalName.isNotEmpty() && !isWorking,
                onConfirm = {
                    isWorking = true
                    scope.launch {
                        ListenTogether.create(context, roomName.trim().ifEmpty { null }, finalName).onFailure { Toasts.show(failureText(context, it), isError = true) }
                        isWorking = false
                    }
                },
                onBack = { step = ListenStep.Choose }
            )
        }

        ListenStep.Join -> {
            TextField(
                value = code,
                onValueChange = { code = it.uppercase().filter { letter -> letter.isLetterOrDigit() }.take(ListenCodeLength) },
                label = { Text(text = stringResource(R.string.listen_code)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                colors = fieldColors(),
                modifier = Modifier.fillMaxWidth()
            )
            if (knownName.isEmpty()) NameField(value = displayName, onChange = { displayName = it.take(MaxNameLength) }, label = stringResource(R.string.listen_your_name))
            FormButtons(
                confirm = stringResource(R.string.listen_join_confirm),
                isEnabled = finalName.isNotEmpty() && code.length == ListenCodeLength,
                onConfirm = { ListenTogether.join(context, code, finalName).onFailure { Toasts.show(failureText(context, it), isError = true) } },
                onBack = { step = ListenStep.Choose }
            )
        }
    }

    if (step == ListenStep.Choose) {
        FilledTonalButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
            Text(text = stringResource(R.string.listen_close), maxLines = 1)
        }
    }
}

// The room the user is in, with its code to share, who is in it and the way out
@Composable
private fun InsideRoom(room: RoomState, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val isOver = room.status == ListenStatus.Closed || room.status == ListenStatus.Failed

    Text(
        text = room.name ?: stringResource(R.string.listen_room),
        style = MaterialTheme.typography.headlineSmall,
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )

    // What the connection is doing, or why the room is over
    Text(
        text = when (room.status) {
            ListenStatus.Connecting -> stringResource(R.string.listen_connecting)
            ListenStatus.Reconnecting -> stringResource(R.string.listen_reconnecting)
            ListenStatus.Connected -> stringResource(if (room.isHost) R.string.listen_host_hint else R.string.listen_guest_hint)
            ListenStatus.Closed, ListenStatus.Failed -> reasonText(room.reason)
        },
        style = MaterialTheme.typography.bodyMedium,
        color = if (room.status == ListenStatus.Failed) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
    )

    if (!isOver) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text(
                text = room.code,
                style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold, letterSpacing = ListenDimens.CodeSpacing),
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Start,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = { shareCode(context, room) }) {
                Icon(imageVector = WavvyIcons.ShareArrow, contentDescription = stringResource(R.string.listen_share), tint = MaterialTheme.colorScheme.onSurface)
            }
        }

        Text(text = stringResource(R.string.listen_people, room.people.size), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)

        // The host stays at the top, marked in gold, and the others keep the order they came in
        room.people.sortedByDescending { it.isHost }.forEach { person ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = person.name,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (person.isHost) {
                    Text(
                        text = stringResource(R.string.listen_host_label),
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = WavvyTheme.colors.host,
                        maxLines = 1,
                        modifier = Modifier.padding(start = ListenDimens.HostGap)
                    )
                }
            }
        }
    }

    // The host can take the requests of the guests without being asked each time
    if (room.isHost && !isOver) {
        val isAuto by ListenTogether.autoAccept.collectAsState()
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text(
                text = stringResource(R.string.listen_auto_accept),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            Switch(checked = isAuto, onCheckedChange = ListenTogether::setAutoAccept)
        }
    }

    Row(horizontalArrangement = Arrangement.spacedBy(ListenDimens.DialogGap), modifier = Modifier.fillMaxWidth().padding(top = ListenDimens.DialogGap)) {
        FilledTonalButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
            Text(text = stringResource(R.string.listen_close), maxLines = 1)
        }
        Button(
            onClick = {
                ListenTogether.leave()
                onDismiss()
            },
            colors = mainButtonColors(),
            modifier = Modifier.weight(1f)
        ) {
            Text(text = stringResource(if (room.isHost && !isOver) R.string.listen_end else R.string.listen_leave), maxLines = 1)
        }
    }
}

// A line to type a name in
@Composable
private fun NameField(value: String, onChange: (String) -> Unit, label: String) {
    TextField(
        value = value,
        onValueChange = onChange,
        label = { Text(text = label) },
        singleLine = true,
        colors = fieldColors(),
        modifier = Modifier.fillMaxWidth()
    )
}

// The buttons that close a form, going back and doing what it asks
@Composable
private fun FormButtons(confirm: String, isEnabled: Boolean, onConfirm: () -> Unit, onBack: () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(ListenDimens.DialogGap), modifier = Modifier.fillMaxWidth().padding(top = ListenDimens.DialogGap)) {
        FilledTonalButton(onClick = onBack, modifier = Modifier.weight(1f)) {
            Text(text = stringResource(R.string.listen_back), maxLines = 1)
        }
        Button(onClick = onConfirm, enabled = isEnabled, colors = mainButtonColors(), modifier = Modifier.weight(1f)) {
            Text(text = confirm, maxLines = 1)
        }
    }
}

@Composable
private fun fieldColors() = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent)

@Composable
private fun mainButtonColors() = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.onBackground, contentColor = MaterialTheme.colorScheme.background)

// Why a room could not be made, as words
private fun failureText(context: Context, error: Throwable): String =
    context.getString(
        when ((error as? ListenError)?.reason) {
            "name_required" -> R.string.listen_error_name
            "room_not_found" -> R.string.listen_error_not_found
            "room_full" -> R.string.listen_error_full
            else -> R.string.listen_error_unavailable
        }
    )

// Why a room is over, as words
@Composable
private fun reasonText(reason: String?): String =
    stringResource(
        when (reason) {
            "host_left" -> R.string.listen_over_host_left
            "expired" -> R.string.listen_over_expired
            "replaced" -> R.string.listen_over_replaced
            "room_not_found" -> R.string.listen_error_not_found
            "room_full" -> R.string.listen_error_full
            "name_required" -> R.string.listen_error_name
            else -> R.string.listen_over_lost
        }
    )

// Opens the sharing sheet of the system with the code of the room
private fun shareCode(context: Context, room: RoomState) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, context.getString(R.string.listen_share_text, room.code))
    }
    context.startActivity(Intent.createChooser(intent, context.getString(R.string.player_share_via)))
}

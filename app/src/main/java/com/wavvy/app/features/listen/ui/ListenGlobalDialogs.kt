package com.wavvy.app.features.listen.ui

// Compose layouts and foundations
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
// Material 3 components
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
// Compose state and runtime
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
// UI styling and utilities
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.delay
// Project resources
import com.wavvy.app.R
import com.wavvy.app.core.listen.GuestRequest
import com.wavvy.app.core.listen.ListenRequestTimeoutMillis
import com.wavvy.app.core.listen.ListenTogether

// The boxes of listening together that can come up on any screen, what a guest asked the host and how the app works for a guest
@Composable
fun ListenGlobalDialogs() {
    val notice by ListenTogether.notice.collectAsState()
    val requests by ListenTogether.requests.collectAsState()

    if (notice) NoticeDialog(onDismiss = ListenTogether::dismissNotice)
    requests.firstOrNull()?.let { RequestDialog(it) }
}

// What a guest asked, the host accepts or not, and no answer in time counts as a no, a tap outside or the back button do nothing
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RequestDialog(request: GuestRequest) {
    LaunchedEffect(request.requestId) {
        delay(ListenRequestTimeoutMillis)
        ListenTogether.decide(request, accepted = false)
    }

    BasicAlertDialog(onDismissRequest = {}, properties = AnswerDialogProperties) {
        Surface(shape = RoundedCornerShape(ListenDimens.DialogCorner), color = MaterialTheme.colorScheme.surfaceContainerHigh) {
            Column(verticalArrangement = Arrangement.spacedBy(ListenDimens.DialogGap), modifier = Modifier.padding(ListenDimens.DialogPadding)) {
                Text(text = stringResource(R.string.listen_request_title, request.fromName), style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface)
                Text(text = requestText(request), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(ListenDimens.DialogGap), modifier = Modifier.fillMaxWidth().padding(top = ListenDimens.DialogGap)) {
                    FilledTonalButton(onClick = { ListenTogether.decide(request, accepted = false) }, modifier = Modifier.weight(1f)) {
                        Text(text = stringResource(R.string.listen_request_decline), maxLines = 1)
                    }
                    Button(
                        onClick = { ListenTogether.decide(request, accepted = true) },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.onBackground, contentColor = MaterialTheme.colorScheme.background),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(text = stringResource(R.string.listen_request_accept), maxLines = 1)
                    }
                }
            }
        }
    }
}

@Composable
private fun requestText(request: GuestRequest): String {
    val title = request.track?.title.orEmpty()
    return when (request.action) {
        "play" -> stringResource(R.string.listen_request_play)
        "pause" -> stringResource(R.string.listen_request_pause)
        "next" -> stringResource(R.string.listen_request_next)
        "previous" -> stringResource(R.string.listen_request_previous)
        "seek" -> stringResource(R.string.listen_request_seek)
        "playTrack" -> stringResource(R.string.listen_request_play_track, title)
        "enqueue" -> if (request.tracks.size > 1) stringResource(R.string.listen_request_enqueue_many, request.tracks.size) else stringResource(R.string.listen_request_enqueue, request.tracks.firstOrNull()?.title.orEmpty())
        else -> if (request.tracks.size > 1) stringResource(R.string.listen_request_play_next_many, request.tracks.size) else stringResource(R.string.listen_request_play_next, request.tracks.firstOrNull()?.title.orEmpty())
    }
}

// The boxes that wait for a button, which neither a tap outside nor the back button closes
private val AnswerDialogProperties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)

// Tells a guest, once, how the app works while the room is on, only the button closes it
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NoticeDialog(onDismiss: () -> Unit) {
    BasicAlertDialog(onDismissRequest = {}, properties = AnswerDialogProperties) {
        Surface(shape = RoundedCornerShape(ListenDimens.DialogCorner), color = MaterialTheme.colorScheme.surfaceContainerHigh) {
            Column(verticalArrangement = Arrangement.spacedBy(ListenDimens.DialogGap), modifier = Modifier.padding(ListenDimens.DialogPadding)) {
                Text(text = stringResource(R.string.listen_notice_title), style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface)
                Text(text = stringResource(R.string.listen_notice_text), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.onBackground, contentColor = MaterialTheme.colorScheme.background),
                    modifier = Modifier.fillMaxWidth().padding(top = ListenDimens.DialogGap)
                ) {
                    Text(text = stringResource(R.string.listen_notice_ok), maxLines = 1)
                }
            }
        }
    }
}

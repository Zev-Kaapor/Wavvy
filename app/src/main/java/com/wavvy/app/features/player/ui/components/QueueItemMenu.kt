package com.wavvy.app.features.player.ui.components

// Compose layouts and foundations
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
// Material 3 components
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
// Compose state and runtime
import androidx.compose.runtime.Composable
// UI styling and utilities
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
// Project resources
import com.wavvy.app.R
import com.wavvy.app.core.designsystem.components.WavvySheet
import com.wavvy.app.core.designsystem.icons.WavvyIcons
import com.wavvy.app.core.designsystem.theme.DarkColors
import com.wavvy.app.core.playback.QueueEntry

// Options of a song of the queue, only what works for now, play next, remove and share
@Composable
fun QueueItemMenu(
    entry: QueueEntry,
    canPlayNext: Boolean,
    canRemove: Boolean,
    onPlayNext: () -> Unit,
    onRemove: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val track = entry.track

    DarkColors {
        WavvySheet(onDismiss = onDismiss) {
            Column(modifier = Modifier.padding(bottom = PlayerDimens.OptionsBottom)) {
                // The song the options are for
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = PlayerDimens.OptionsSide, vertical = PlayerDimens.OptionsRowGap)
                ) {
                    TrackCover(track = track, size = PlayerDimens.QueueMenuCover)
                    Column(modifier = Modifier.weight(1f).padding(start = PlayerDimens.QueueTextSide)) {
                        Text(
                            text = track.title,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            maxLines = 1,
                            modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE)
                        )
                        track.artist?.let { artist ->
                            Text(
                                text = artist,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE)
                            )
                        }
                    }
                }

                if (canPlayNext) {
                    MenuAction(WavvyIcons.PlaylistPlay, stringResource(R.string.queue_play_next)) {
                        onPlayNext()
                        onDismiss()
                    }
                }
                MenuAction(WavvyIcons.Share, stringResource(R.string.player_share)) {
                    shareSong(context, track.id)
                    onDismiss()
                }
                if (canRemove) {
                    MenuAction(WavvyIcons.Delete, stringResource(R.string.queue_remove)) {
                        onRemove()
                        onDismiss()
                    }
                }
            }
        }
    }
}

// A row of the menu with its icon and its name
@Composable
private fun MenuAction(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .height(PlayerDimens.OptionsRowHeight)
            .clickable(onClick = onClick)
            .padding(horizontal = PlayerDimens.OptionsSide)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(PlayerDimens.ToolbarIcon)
        )
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(start = PlayerDimens.QueueTextSide)
        )
    }
}

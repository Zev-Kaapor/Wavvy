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
import com.wavvy.app.core.device.isDeviceTrack
import com.wavvy.app.core.designsystem.components.LocalSheetClose
import com.wavvy.app.core.designsystem.components.WavvySheet
import com.wavvy.app.core.designsystem.icons.WavvyIcons
import com.wavvy.app.core.designsystem.theme.DarkColors
import com.wavvy.app.core.playback.QueueEntry
import com.wavvy.app.features.menu.CardMenuHead
import com.wavvy.app.features.menu.CardMenuLayout
import com.wavvy.app.features.menu.MenuCard
import com.wavvy.app.features.playlist.ui.SaveToPlaylist

// Options of a song of the queue, only what works for now, play next, save to a playlist, remove and share
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
            val closeSheet = LocalSheetClose.current

            CardMenuLayout(
                head = { CardMenuHead(title = track.title, subtitle = track.artist, coverUrl = track.artworkUrl) },
                options = {
                    if (canPlayNext) {
                        MenuCard(WavvyIcons.PlaylistPlay, stringResource(R.string.queue_play_next)) {
                            onPlayNext()
                            closeSheet()
                        }
                    }
                    // A file of the phone is not in YouTube Music, so it has no playlist of the account and no page to share
                    if (!isDeviceTrack(track.id)) {
                        MenuCard(WavvyIcons.PlaylistAdd, stringResource(R.string.playlist_save)) {
                            closeSheet()
                            SaveToPlaylist.show(listOf(track.id))
                        }
                        MenuCard(WavvyIcons.Share, stringResource(R.string.player_share)) {
                            shareSong(context, track.id)
                            closeSheet()
                        }
                    }
                    if (canRemove) {
                        MenuCard(WavvyIcons.Delete, stringResource(R.string.queue_remove)) {
                            onRemove()
                            closeSheet()
                        }
                    }
                }
            )
        }
    }
}

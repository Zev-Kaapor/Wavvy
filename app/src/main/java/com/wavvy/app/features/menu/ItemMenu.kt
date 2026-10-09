package com.wavvy.app.features.menu

// Android and Compose layouts
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
// Material 3 components
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
// Compose state and runtime
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
// UI utilities
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
// Coroutines and reactive flows
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
// Project resources
import com.wavvy.app.R
import com.wavvy.app.core.designsystem.components.WavvySheet
import com.wavvy.app.core.designsystem.icons.WavvyIcons
import com.wavvy.app.core.history.PlayHistory
import com.wavvy.app.core.playback.PlayerConnection
import com.wavvy.app.features.home.data.HomeItem
import com.wavvy.app.features.home.data.HomeItemKind
import com.wavvy.app.features.home.ui.toPlayableTrack
import com.wavvy.app.features.player.ui.components.MenuAction
import com.wavvy.app.features.player.ui.components.PlayerDimens
import com.wavvy.app.features.player.ui.components.TrackCover
import com.wavvy.app.features.player.ui.components.shareSong

// The song whose menu is open, held outside the screens so any card can open it and the sheet can cover the whole window
object ItemMenu {
    private val mutableItem = MutableStateFlow<HomeItem?>(null)
    val item: StateFlow<HomeItem?> = mutableItem.asStateFlow()

    // Opens the menu of a song, of an episode or of an artist, the other cards have no menu yet
    fun show(item: HomeItem) {
        if (item.kind == HomeItemKind.Song || item.kind == HomeItemKind.Episode || item.kind == HomeItemKind.Artist) mutableItem.value = item
    }

    fun dismiss() {
        mutableItem.value = null
    }
}

// Menu of a song of the Home, drawn over the whole screen, play next, add to the queue, share and pin to the speed dial
@Composable
fun ItemMenuHost() {
    val item by ItemMenu.item.collectAsState()
    val current = item ?: return

    // An artist has its own menu
    if (current.kind == HomeItemKind.Artist) {
        ArtistMenu(current)
        return
    }
    val track = current.toPlayableTrack() ?: return

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val pinnedIds by remember { PlayHistory.pinned(context).map { songs -> songs.map { it.id }.toSet() } }.collectAsState(initial = emptySet())
    val isPinned = track.id in pinnedIds

    WavvySheet(onDismiss = ItemMenu::dismiss) {
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

            MenuAction(WavvyIcons.PlaylistPlay, stringResource(R.string.queue_play_next)) {
                PlayerConnection.playNextTrack(context, track)
                ItemMenu.dismiss()
            }
            MenuAction(WavvyIcons.QueueMusic, stringResource(R.string.menu_add_to_queue)) {
                PlayerConnection.addTrackToQueue(context, track)
                ItemMenu.dismiss()
            }
            MenuAction(WavvyIcons.Share, stringResource(R.string.player_share)) {
                shareSong(context, track.id)
                ItemMenu.dismiss()
            }
            MenuAction(WavvyIcons.Pin, stringResource(if (isPinned) R.string.menu_unpin else R.string.menu_pin)) {
                scope.launch(Dispatchers.IO) {
                    if (isPinned) {
                        PlayHistory.unpin(context, track.id)
                    } else {
                        PlayHistory.pin(context, track.id, track.title, track.artist, track.artworkUrl, track.durationMs)
                    }
                }
                ItemMenu.dismiss()
            }
        }
    }
}

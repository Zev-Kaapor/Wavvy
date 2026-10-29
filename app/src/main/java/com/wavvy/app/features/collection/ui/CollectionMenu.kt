package com.wavvy.app.features.collection.ui

// Compose layouts and foundations
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
// Material 3 components
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
// Compose state and runtime
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
// UI styling and utilities
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
// Coroutines and reactive flows
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
// Project resources
import com.wavvy.app.R
import com.wavvy.app.core.designsystem.components.LocalSheetClose
import com.wavvy.app.core.designsystem.components.WavvySheet
import com.wavvy.app.core.designsystem.icons.WavvyIcons
import com.wavvy.app.core.designsystem.theme.WavvyTheme
import com.wavvy.app.core.history.PlayHistory
import com.wavvy.app.features.menu.CardMenuHead
import com.wavvy.app.features.menu.CardMenuLayout
import com.wavvy.app.features.menu.CardMenuTiles
import com.wavvy.app.features.menu.MenuCard
import com.wavvy.app.features.menu.MenuCardTile
import com.wavvy.app.features.menu.MenuReveal

// What the menu of an album or of a playlist shows and does, the actions are made by the screen that opens it
data class CollectionMenuData(
    val id: String,
    // The kind of the item as the speed dial keeps it, Album or Playlist
    val kindName: String,
    val title: String,
    val subtitle: String?,
    val coverUrl: String?,
    val onShuffle: () -> Unit,
    val onPlayNext: () -> Unit,
    val onAddToQueue: () -> Unit,
    val onSaveToPlaylist: () -> Unit,
    val onShare: () -> Unit,
    // The way the songs are in order now and what opens the choices, only for a page that has them
    val sortTitle: String? = null,
    val onSort: (() -> Unit)? = null,
    // Only a playlist the account made can be changed or deleted
    val onEdit: (() -> Unit)? = null,
    val onDelete: (() -> Unit)? = null,
    // Opens the page in the mode that marks several songs
    val onSelect: (() -> Unit)? = null,
    // Saves the page to the library or takes it out, none when the page has no such button
    val onToggleLibrary: (() -> Unit)? = null,
    val isInLibrary: Boolean = false,
    // Downloads every song, or takes them out when they are here or on their way, with the words of the line
    val onDownload: (() -> Unit)? = null,
    val downloadLabel: String = "",
    val isDownloadRemoval: Boolean = false,
    // Plays the radio of the first song, and opens the page of the artist of an album
    val onMix: (() -> Unit)? = null,
    val onGoToArtist: (() -> Unit)? = null,
    // True on the page of a playlist, which has the share among its buttons, so the menu has the mix in its place
    val hasShareButton: Boolean = false
)

// The menu that is open, held outside the screens so the sheet can cover the whole window
object CollectionMenu {
    private val mutableData = MutableStateFlow<CollectionMenuData?>(null)
    val data: StateFlow<CollectionMenuData?> = mutableData.asStateFlow()

    fun show(data: CollectionMenuData) {
        mutableData.value = data
    }

    fun dismiss() {
        mutableData.value = null
    }
}

// Menu of an album or of a playlist, drawn over the whole screen as cards, its cover and name on top with the three most used options under them and the others below
@Composable
fun CollectionMenuHost() {
    val data by CollectionMenu.data.collectAsState()
    val current = data ?: return

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val pinnedIds by remember { PlayHistory.pinned(context).map { pinned -> pinned.map { it.id }.toSet() } }.collectAsState(initial = emptySet())
    val isPinned = current.id in pinnedIds

    // The mix is a tile when the share is already a button of the page, and a card under the others when it is not
    val showsMixTile = current.hasShareButton && current.onMix != null

    WavvySheet(onDismiss = CollectionMenu::dismiss) {
        // Every action slides the menu out and then runs
        val closeSheet = LocalSheetClose.current
        val run: (() -> Unit) -> () -> Unit = { action -> { closeSheet(); action() } }

        CardMenuLayout(
            head = {
                CardMenuHead(title = current.title, subtitle = current.subtitle, coverUrl = current.coverUrl)
                CardMenuTiles {
                    MenuCardTile(WavvyIcons.PlaylistPlay, stringResource(R.string.queue_play_next), Modifier.weight(1f), onClick = run(current.onPlayNext))
                    MenuCardTile(WavvyIcons.PlaylistAdd, stringResource(R.string.playlist_save), Modifier.weight(1f), onClick = run(current.onSaveToPlaylist))
                    if (showsMixTile) {
                        MenuCardTile(WavvyIcons.Mix, stringResource(R.string.artist_mix), Modifier.weight(1f), onClick = run(current.onMix ?: {}))
                    } else {
                        MenuCardTile(WavvyIcons.Share, stringResource(R.string.player_share), Modifier.weight(1f), onClick = run(current.onShare))
                    }
                }
            },
            options = {
                MenuCard(WavvyIcons.Shuffle, stringResource(R.string.collection_shuffle), onClick = run(current.onShuffle))
                current.onSort?.let { onSort ->
                    val title = stringResource(R.string.collection_sort_by)
                    MenuCard(WavvyIcons.Sort, title, subtitle = current.sortTitle, onClick = run(onSort))
                }
                if (!showsMixTile) current.onMix?.let { MenuCard(WavvyIcons.Mix, stringResource(R.string.artist_mix), onClick = run(it)) }
                MenuCard(WavvyIcons.QueueMusic, stringResource(R.string.menu_add_to_queue), onClick = run(current.onAddToQueue))
                current.onSelect?.let { MenuCard(WavvyIcons.Checklist, stringResource(R.string.collection_select), onClick = run(it)) }
                current.onGoToArtist?.let { MenuCard(WavvyIcons.Person, stringResource(R.string.menu_go_to_artist), onClick = run(it)) }
                // The page brings the save to the library, so on the menu of a card its option comes in when the page does
                current.onToggleLibrary?.let {
                    MenuReveal {
                        MenuCard(
                            if (current.isInLibrary) WavvyIcons.Delete else WavvyIcons.Bookmark,
                            stringResource(if (current.isInLibrary) R.string.collection_library_remove else R.string.collection_library_save),
                            onClick = run(it)
                        )
                    }
                }
                current.onDownload?.let { MenuCard(if (current.isDownloadRemoval) WavvyIcons.Delete else WavvyIcons.Download, current.downloadLabel, onClick = run(it)) }
                current.onEdit?.let { MenuCard(WavvyIcons.Edit, stringResource(R.string.playlist_edit), onClick = run(it)) }
                current.onDelete?.let { MenuCard(WavvyIcons.Delete, stringResource(R.string.playlist_delete), onClick = run(it)) }
                MenuCard(WavvyIcons.Pin, stringResource(if (isPinned) R.string.menu_unpin else R.string.menu_pin)) {
                    scope.launch(Dispatchers.IO) {
                        if (isPinned) {
                            PlayHistory.unpin(context, current.id)
                        } else {
                            PlayHistory.pin(context, current.id, current.title, null, current.coverUrl, durationMs = 0L, kind = current.kindName)
                        }
                    }
                    closeSheet()
                }
            }
        )
    }
}

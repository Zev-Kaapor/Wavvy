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
import com.wavvy.app.core.designsystem.components.WavvySheet
import com.wavvy.app.core.designsystem.icons.WavvyIcons
import com.wavvy.app.core.designsystem.theme.WavvyTheme
import com.wavvy.app.core.history.PlayHistory
import com.wavvy.app.features.menu.ItemMenuDimens
import com.wavvy.app.features.player.ui.components.MenuAction

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
    val onDelete: (() -> Unit)? = null
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

// Menu of an album or of a playlist, drawn over the whole screen, its name on top with the close button, the two big buttons and the actions under them
@Composable
fun CollectionMenuHost() {
    val data by CollectionMenu.data.collectAsState()
    val current = data ?: return

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val pinnedIds by remember { PlayHistory.pinned(context).map { pinned -> pinned.map { it.id }.toSet() } }.collectAsState(initial = emptySet())
    val isPinned = current.id in pinnedIds

    // Every action closes the menu after it runs
    val run: (() -> Unit) -> () -> Unit = { action -> { CollectionMenu.dismiss(); action() } }

    WavvySheet(onDismiss = CollectionMenu::dismiss) {
        Column(modifier = Modifier.padding(bottom = ItemMenuDimens.Bottom)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = ItemMenuDimens.Side, end = ItemMenuDimens.CloseEnd, top = ItemMenuDimens.HeaderTop, bottom = ItemMenuDimens.HeaderBottom)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = current.title,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE)
                    )
                    current.subtitle?.let { subtitle ->
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE)
                        )
                    }
                }
                IconButton(onClick = CollectionMenu::dismiss) {
                    Icon(
                        imageVector = WavvyIcons.Close,
                        contentDescription = stringResource(R.string.cd_close),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            Row(
                horizontalArrangement = Arrangement.spacedBy(ItemMenuDimens.TileGap),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = ItemMenuDimens.Side, vertical = ItemMenuDimens.TilesVertical)
            ) {
                MenuTile(WavvyIcons.PlaylistPlay, stringResource(R.string.queue_play_next), run(current.onPlayNext), Modifier.weight(1f))
                MenuTile(WavvyIcons.Share, stringResource(R.string.player_share), run(current.onShare), Modifier.weight(1f))
            }

            MenuAction(WavvyIcons.Shuffle, stringResource(R.string.collection_shuffle), run(current.onShuffle))
            current.onSort?.let { onSort ->
                val title = stringResource(R.string.collection_sort_by)
                MenuAction(WavvyIcons.Sort, current.sortTitle?.let { "$title: ${it.lowercase()}" } ?: title, run(onSort))
            }
            MenuAction(WavvyIcons.QueueMusic, stringResource(R.string.menu_add_to_queue), run(current.onAddToQueue))
            MenuAction(WavvyIcons.PlaylistAdd, stringResource(R.string.playlist_save), run(current.onSaveToPlaylist))
            current.onEdit?.let { MenuAction(WavvyIcons.Edit, stringResource(R.string.playlist_edit), run(it)) }
            current.onDelete?.let { MenuAction(WavvyIcons.Delete, stringResource(R.string.playlist_delete), run(it)) }
            MenuAction(WavvyIcons.Pin, stringResource(if (isPinned) R.string.menu_unpin else R.string.menu_pin)) {
                scope.launch(Dispatchers.IO) {
                    if (isPinned) {
                        PlayHistory.unpin(context, current.id)
                    } else {
                        PlayHistory.pin(context, current.id, current.title, null, current.coverUrl, durationMs = 0L, kind = current.kindName)
                    }
                }
                CollectionMenu.dismiss()
            }
        }
    }
}

// A big button with the icon and the words under it
@Composable
private fun MenuTile(icon: ImageVector, text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxWidth()
                .height(ItemMenuDimens.TileHeight)
                .clip(RoundedCornerShape(ItemMenuDimens.TileCorner))
                .background(WavvyTheme.colors.chip)
                .clickable(onClick = onClick)
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface)
        }
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = ItemMenuDimens.TileLabelTop)
        )
    }
}

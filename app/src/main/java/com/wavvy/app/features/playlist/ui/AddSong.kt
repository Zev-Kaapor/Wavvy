package com.wavvy.app.features.playlist.ui

// Compose layouts and foundations
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
// Material 3 components
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
// Compose state and runtime
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
// UI styling and utilities
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
// Coroutines and reactive flows
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
// Image loading
import coil3.compose.AsyncImage
// Project resources
import com.wavvy.app.R
import com.wavvy.app.core.designsystem.components.WavvySheet
import com.wavvy.app.core.designsystem.icons.WavvyIcons
import com.wavvy.app.core.designsystem.theme.WavvyTheme
import com.wavvy.app.features.home.data.HomeItem
import com.wavvy.app.features.home.data.HomeItemKind
import com.wavvy.app.features.home.ui.components.itemSubtitle
import com.wavvy.app.features.menu.ItemMenuDimens
import com.wavvy.app.features.playlist.data.PlaylistRepository
import com.wavvy.app.features.search.data.SearchCategory
import com.wavvy.app.features.search.data.SearchRepository

// The playlist that is being given a song, held outside the screens so the sheet can cover the whole window
object AddSong {
    private val mutablePlaylistId = MutableStateFlow<String?>(null)
    val playlistId: StateFlow<String?> = mutablePlaylistId.asStateFlow()

    fun show(playlistId: String) {
        mutablePlaylistId.value = playlistId
    }

    fun dismiss() {
        mutablePlaylistId.value = null
    }
}

// The sheet that looks for songs and puts the one that is tapped in the playlist, it stays open so more of them can be added
@Composable
fun AddSongHost() {
    val playlistId by AddSong.playlistId.collectAsState()
    val current = playlistId ?: return

    val context = LocalContext.current
    val resources = LocalResources.current
    val scope = rememberCoroutineScope()
    val playlists = remember { PlaylistRepository(context) }
    val search = remember { SearchRepository(context) }
    var query by remember(current) { mutableStateOf("") }
    var results by remember(current) { mutableStateOf<List<HomeItem>>(emptyList()) }
    var isSearching by remember(current) { mutableStateOf(false) }
    var added by remember(current) { mutableStateOf<Set<String>>(emptySet()) }

    // Searches a little after the typing stops, so each letter does not ask again
    LaunchedEffect(query) {
        if (query.isBlank()) {
            results = emptyList()
            isSearching = false
            return@LaunchedEffect
        }
        isSearching = true
        delay(PlaylistDimens.SearchDelayMillis)
        search.search(query.trim(), SearchCategory.Songs).onSuccess { page ->
            results = page.items.filter { it.kind == HomeItemKind.Song }
        }
        isSearching = false
    }

    WavvySheet(onDismiss = AddSong::dismiss) {
        Column(modifier = Modifier.imePadding().padding(bottom = ItemMenuDimens.Bottom)) {
            SheetHeader(title = stringResource(R.string.playlist_add_song), onClose = AddSong::dismiss)

            TextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text(text = stringResource(R.string.playlist_add_song_hint), maxLines = 1) },
                leadingIcon = { Icon(imageVector = WavvyIcons.Search, contentDescription = null) },
                singleLine = true,
                shape = CircleShape,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = WavvyTheme.colors.chip,
                    unfocusedContainerColor = WavvyTheme.colors.chip,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                modifier = Modifier.fillMaxWidth().padding(horizontal = ItemMenuDimens.Side, vertical = PlaylistDimens.NewVertical)
            )

            // Room for the results is kept, so the sheet does not grow and shrink as the answers come
            LazyColumn(modifier = Modifier.heightIn(min = PlaylistDimens.ListMaxHeight, max = PlaylistDimens.ListMaxHeight)) {
                items(results, key = { it.id }) { song ->
                    val isAdded = song.id in added
                    SongRow(
                        song = song,
                        isAdded = isAdded,
                        onClick = {
                            if (!isAdded) {
                                added = added + song.id
                                scope.launch {
                                    playlists.add(current, listOf(song.id)).onFailure {
                                        added = added - song.id
                                        toast(context, resources.getString(R.string.playlist_action_error))
                                    }
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

// A song found, with its cover, its name and line, and the plus that becomes a check once it is in the playlist
@Composable
private fun SongRow(song: HomeItem, isAdded: Boolean, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .height(PlaylistDimens.RowHeight)
            .clickable(onClick = onClick)
            .padding(horizontal = ItemMenuDimens.Side)
    ) {
        AsyncImage(
            model = song.thumbnailUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(PlaylistDimens.RowCover)
                .clip(RoundedCornerShape(PlaylistDimens.RowCoverCorner))
                .background(WavvyTheme.colors.chip)
        )
        Column(modifier = Modifier.weight(1f).padding(horizontal = PlaylistDimens.RowGap)) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = itemSubtitle(song).orEmpty(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Box(modifier = Modifier.size(PlaylistDimens.RowCover / 2)) {
            Icon(
                imageVector = if (isAdded) WavvyIcons.Check else WavvyIcons.Add,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.fillMaxWidth().height(PlaylistDimens.RowCover / 2)
            )
        }
    }
}

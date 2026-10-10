package com.wavvy.app.features.playlist.ui

// Android widgets
import android.content.Context
import android.widget.Toast
// Compose layouts and foundations
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
// Material 3 components
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
// Coroutines and reactive flows
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
// Image loading
import coil3.compose.AsyncImage
// Project resources
import com.wavvy.app.R
import com.wavvy.app.core.designsystem.components.SkeletonHost
import com.wavvy.app.core.designsystem.components.LocalSheetClose
import com.wavvy.app.core.designsystem.components.WavvySheet
import com.wavvy.app.core.designsystem.components.skeleton
import com.wavvy.app.core.designsystem.icons.WavvyIcons
import com.wavvy.app.core.designsystem.theme.WavvyTheme
import com.wavvy.app.features.menu.ItemMenuDimens
import com.wavvy.app.features.playlist.data.PlaylistOption
import com.wavvy.app.features.playlist.data.PlaylistRepository

// The songs that are being saved, held outside the screens so the sheet can cover the whole window from any of them
object SaveToPlaylist {
    private val mutableVideoIds = MutableStateFlow<List<String>?>(null)
    val videoIds: StateFlow<List<String>?> = mutableVideoIds.asStateFlow()

    fun show(videoIds: List<String>) {
        if (videoIds.isNotEmpty()) mutableVideoIds.value = videoIds
    }

    fun dismiss() {
        mutableVideoIds.value = null
    }
}

// The playlists of the account to save the songs to, a check on the ones that have them already takes them out, and the button of a new one
@Composable
fun SaveToPlaylistHost() {
    val videoIds by SaveToPlaylist.videoIds.collectAsState()
    val current = videoIds ?: return

    val context = LocalContext.current
    val resources = LocalResources.current
    val scope = rememberCoroutineScope()
    val repository = remember { PlaylistRepository(context) }
    var isSignedIn by remember(current) { mutableStateOf(true) }
    var options by remember(current) { mutableStateOf<List<PlaylistOption>?>(null) }
    var isFailed by remember(current) { mutableStateOf(false) }
    var isBusy by remember(current) { mutableStateOf(false) }

    LaunchedEffect(current) {
        isSignedIn = repository.isSignedIn()
        if (isSignedIn) repository.options(current).onSuccess { options = it }.onFailure { isFailed = true }
    }

    // Reads what each playlist has, a few at a time, and puts the check on the ones that have every song
    val isLoaded = options != null
    LaunchedEffect(current, isLoaded) {
        val list = options ?: return@LaunchedEffect
        val gate = Semaphore(PlaylistDimens.ParallelReads)

        coroutineScope {
            list.forEach { option ->
                launch {
                    gate.withPermit {
                        val songs = repository.songsOf(option.id) ?: return@withPermit
                        val count = songs.values.sumOf { places -> maxOf(places.size, 1) }
                        options = options?.map {
                            if (it.id == option.id) it.copy(hasSongs = it.hasSongs || current.all { id -> id in songs }, songCount = count) else it
                        }
                    }
                }
            }
        }
    }

    // Puts the songs in the playlist, or takes them out when it has them, and closes once it is done
    val onToggle: (PlaylistOption) -> Unit = { option ->
        if (!isBusy) {
            isBusy = true
            scope.launch {
                val result = if (option.hasSongs) repository.remove(option.id, current) else repository.add(option.id, current)
                result
                    .onSuccess {
                        val message = if (option.hasSongs) R.string.playlist_removed_from else R.string.playlist_saved_to
                        toast(context, resources.getString(message, option.title))
                        // The sheet stays so the songs can go to more playlists, only the check of this one changes
                        options = options?.map { if (it.id == option.id) it.copy(hasSongs = !option.hasSongs) else it }
                        isBusy = false
                    }
                    .onFailure {
                        toast(context, resources.getString(R.string.playlist_action_error))
                        isBusy = false
                    }
            }
        }
    }

    WavvySheet(onDismiss = SaveToPlaylist::dismiss) {
        val closeSheet = LocalSheetClose.current

        Column(modifier = Modifier.padding(bottom = ItemMenuDimens.Bottom)) {
            SheetHeader(title = stringResource(R.string.playlist_save))

            when {
                !isSignedIn -> SheetMessage(stringResource(R.string.playlist_sign_in))
                isFailed -> SheetMessage(stringResource(R.string.playlist_load_error))
                else -> {
                    NewPlaylistButton(onClick = {
                        closeSheet()
                        NewPlaylist.show(current)
                    })

                    // The placeholder has the same rows as the list, so the one takes the place of the other without a jump
                    val list = options
                    if (list == null) {
                        OptionsSkeleton()
                    } else {
                        LazyColumn(modifier = Modifier.heightIn(max = PlaylistDimens.ListMaxHeight).alpha(if (isBusy) PlaylistDimens.BusyAlpha else 1f)) {
                            items(list, key = { it.id }) { option -> OptionRow(option = option, onClick = { onToggle(option) }) }
                        }
                    }
                }
            }
        }
    }
}

// The name of the sheet with the button that closes it, and the line under them
@Composable
internal fun SheetHeader(title: String) {
    val closeSheet = LocalSheetClose.current

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = ItemMenuDimens.Side, end = ItemMenuDimens.CloseEnd, top = ItemMenuDimens.HeaderTop, bottom = ItemMenuDimens.HeaderBottom)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            modifier = Modifier.weight(1f)
        )
        IconButton(onClick = closeSheet) {
            Icon(imageVector = WavvyIcons.Close, contentDescription = stringResource(R.string.cd_close), tint = MaterialTheme.colorScheme.onSurface)
        }
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
}

// Words in place of the list, for a guest or a failure
@Composable
internal fun SheetMessage(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = ItemMenuDimens.Side, vertical = PlaylistDimens.MessageVertical)
    )
}

// The round button with the plus that opens the sheet of a new playlist
@Composable
private fun NewPlaylistButton(onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .padding(horizontal = ItemMenuDimens.Side, vertical = PlaylistDimens.NewVertical)
            .height(PlaylistDimens.NewHeight)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.onSurface)
            .clickable(onClick = onClick)
            .padding(horizontal = PlaylistDimens.NewPaddingX)
    ) {
        Icon(imageVector = WavvyIcons.Add, contentDescription = null, tint = MaterialTheme.colorScheme.surface)
        Text(
            text = stringResource(R.string.library_new_playlist),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.padding(start = PlaylistDimens.NewIconGap)
        )
    }
}

// A playlist with its cover, its name and line, and the check when it has the songs
@Composable
private fun OptionRow(option: PlaylistOption, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .height(PlaylistDimens.RowHeight)
            .clickable(onClick = onClick)
            .padding(horizontal = ItemMenuDimens.Side)
    ) {
        // The check sits on the corner of the cover of a playlist that has the songs already
        Box(modifier = Modifier.size(PlaylistDimens.RowCover)) {
            AsyncImage(
                model = option.coverUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(PlaylistDimens.RowCoverCorner))
                    .background(WavvyTheme.colors.chip)
            )
            // On a dark corner, as the pin of the speed dial is, so it stays readable over any cover, and a placeholder of it until the playlist is read
            if (option.hasSongs) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(PlaylistDimens.RowBadgeInset)
                        .clip(RoundedCornerShape(PlaylistDimens.RowBadgeCorner))
                        .background(WavvyTheme.colors.tileScrim.copy(alpha = PlaylistDimens.RowBadgeAlpha))
                        .padding(PlaylistDimens.RowBadgePadding)
                ) {
                    Image(
                        painter = painterResource(R.drawable.playlist_check),
                        contentDescription = null,
                        modifier = Modifier.size(PlaylistDimens.RowBadge)
                    )
                }
            } else if (option.songCount == null) {
                Spacer(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(PlaylistDimens.RowBadgeInset)
                        .size(PlaylistDimens.RowBadge + PlaylistDimens.RowBadgePadding * 2)
                        .skeleton(RoundedCornerShape(PlaylistDimens.RowBadgeCorner))
                )
            }
        }
        Column(modifier = Modifier.weight(1f).padding(horizontal = PlaylistDimens.RowGap)) {
            Text(
                text = option.title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            // The number of songs once the playlist is read, a placeholder of the line until then
            if (option.songCount != null) {
                Text(
                    text = pluralStringResource(R.plurals.collection_song_count, option.songCount, option.songCount),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            } else {
                Box(modifier = Modifier.height(PlaylistDimens.CountLine), contentAlignment = Alignment.CenterStart) {
                    Spacer(
                        modifier = Modifier
                            .width(PlaylistDimens.CountPlaceholderWidth)
                            .height(ItemMenuDimens.PlaceholderHeight)
                            .skeleton(RoundedCornerShape(ItemMenuDimens.PlaceholderCorner))
                    )
                }
            }
        }
    }
}

// The room of the playlists while they are asked
@Composable
private fun OptionsSkeleton() {
    SkeletonHost(modifier = Modifier.fillMaxWidth()) {
        repeat(PlaylistDimens.SkeletonRows) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().height(PlaylistDimens.RowHeight).padding(horizontal = ItemMenuDimens.Side)
            ) {
                Spacer(modifier = Modifier.size(PlaylistDimens.RowCover).skeleton(RoundedCornerShape(PlaylistDimens.RowCoverCorner)))
                Spacer(
                    modifier = Modifier
                        .padding(start = PlaylistDimens.RowGap)
                        .width(ItemMenuDimens.PlaceholderWidth)
                        .height(ItemMenuDimens.PlaceholderHeight)
                        .skeleton(RoundedCornerShape(ItemMenuDimens.PlaceholderCorner))
                )
            }
        }
    }
}

internal fun toast(context: Context, text: String) {
    Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
}

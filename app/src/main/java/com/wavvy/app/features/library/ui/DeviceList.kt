package com.wavvy.app.features.library.ui

// Android settings, the media of the phone and the lifecycle
import android.app.Activity
import android.content.Intent
import android.database.ContentObserver
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
// Compose layouts and foundations
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
// Material 3 components
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
// Compose state and runtime
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
// UI styling and utilities
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
// Coroutines
import kotlinx.coroutines.launch
// Project resources
import com.wavvy.app.R
import com.wavvy.app.core.designsystem.components.ConfirmDialog
import com.wavvy.app.core.designsystem.components.LocalSheetClose
import com.wavvy.app.core.designsystem.components.OverlaySheet
import com.wavvy.app.core.designsystem.components.SelectionBar
import com.wavvy.app.core.designsystem.components.SelectionBarSpace
import com.wavvy.app.core.designsystem.components.SelectionHeader
import com.wavvy.app.core.designsystem.components.Toasts
import com.wavvy.app.core.designsystem.components.WavvySheet
import com.wavvy.app.core.designsystem.icons.WavvyIcons
import com.wavvy.app.core.device.DeleteResult
import com.wavvy.app.core.device.DeviceMusic
import com.wavvy.app.core.device.DeviceSong
import com.wavvy.app.core.playback.PlayableTrack
import com.wavvy.app.core.playback.PlayerConnection
import com.wavvy.app.features.home.data.HomeItem
import com.wavvy.app.features.home.data.HomeItemKind
import com.wavvy.app.features.player.ui.LocalMiniPlayerInset
import com.wavvy.app.features.menu.CardMenuHead
import com.wavvy.app.features.menu.CardMenuLayout
import com.wavvy.app.features.menu.MenuCard
import com.wavvy.app.features.player.ui.components.formatTime

// The mark between the pieces of the line under a song
private const val LineSeparator = " • "
private const val MillisPerSecond = 1000L
private const val PathSeparator = '/'

// How the music of the phone is shown, all the songs, the artists with their songs, or the folders with theirs
private enum class DeviceView(val titleRes: Int) {
    Songs(R.string.library_songs),
    Artists(R.string.library_artists),
    Folders(R.string.library_device_folders)
}

// Songs that go together, an artist or a folder, with what is shown for them
private class DeviceGroup(val key: String, val title: String, val line: String?, val songs: List<DeviceSong>)

// What the user asked about, a song or a group of them, which the menu is open for and which can be deleted
private class DeviceTarget(val title: String, val songs: List<DeviceSong>, val isGroup: Boolean)

// The music that is on the phone, by song, by artist or by folder, with the order and the search of the library, and the delete of the files
// It is asked with the permission of the user, without it the list says why it is needed and asks for it, and after a refusal it sends the user to the settings of the app
@Composable
internal fun DeviceList(
    query: String?,
    sort: LibrarySort,
    onPlay: (List<HomeItem>, Int, Boolean?) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isAllowed by remember { mutableStateOf(DeviceMusic.hasPermission(context)) }
    var wasRefused by remember { mutableStateOf(false) }
    var view by rememberSaveable { mutableStateOf(DeviceView.Songs) }
    var openKey by rememberSaveable { mutableStateOf<String?>(null) }
    var deleting by remember { mutableStateOf<DeviceTarget?>(null) }

    // Songs being marked, on or off and the ids of the ones marked, back leaves the mode before it leaves the list
    var isSelecting by remember { mutableStateOf(false) }
    var selectedIds by remember { mutableStateOf(emptySet<String>()) }
    val leaveSelection = {
        isSelecting = false
        selectedIds = emptySet()
    }
    BackHandler(enabled = isSelecting, onBack = leaveSelection)

    // Counts the times the list must be read again, when the files of the phone change and when the user comes back to the app
    var version by remember { mutableIntStateOf(0) }
    val request = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        isAllowed = granted
        wasRefused = !granted
    }

    // The files that are being deleted, and the ones that are left when the system has to ask the user about each one
    var toDelete by remember { mutableStateOf<List<String>>(emptyList()) }
    var remaining by remember { mutableStateOf<List<String>>(emptyList()) }
    val deleted = stringResource(R.string.library_device_deleted)
    val deleteFailed = stringResource(R.string.library_device_delete_error)

    // The files are gone, so they leave the queue and the list is read again
    val finishDelete: () -> Unit = {
        PlayerConnection.removeAll(toDelete.toSet())
        toDelete = emptyList()
        remaining = emptyList()
        version++
        Toasts.show(deleted)
    }
    val cancelDelete: () -> Unit = {
        toDelete = emptyList()
        remaining = emptyList()
    }

    // Deletes the files that are left, Android 10 asks the user about one file and goes on to the next
    lateinit var deleteFrom: (List<String>) -> Unit
    val question = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        if (result.resultCode != Activity.RESULT_OK) cancelDelete()
        else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) finishDelete()
        else deleteFrom(remaining)
    }
    deleteFrom = { ids ->
        scope.launch {
            when (val result = DeviceMusic.delete(context, ids)) {
                DeleteResult.Done -> finishDelete()
                is DeleteResult.Ask -> {
                    remaining = result.remaining
                    question.launch(IntentSenderRequest.Builder(result.question).build())
                }
                DeleteResult.Failed -> {
                    cancelDelete()
                    Toasts.show(deleteFailed, isError = true)
                }
            }
        }
    }
    val startDelete: (List<DeviceSong>) -> Unit = { songs ->
        toDelete = songs.map { it.id }
        val system = DeviceMusic.deleteQuestion(context, toDelete)
        if (system != null) question.launch(IntentSenderRequest.Builder(system).build()) else deleteFrom(toDelete)
    }

    DisposableEffect(isAllowed) {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                version++
            }
        }
        if (isAllowed) context.contentResolver.registerContentObserver(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, true, observer)

        // The permission may have been given in the settings of the app, so it is looked at again when the user comes back
        val owner = context as? LifecycleOwner
        val lifecycleObserver = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isAllowed = DeviceMusic.hasPermission(context)
                version++
            }
        }
        owner?.lifecycle?.addObserver(lifecycleObserver)

        onDispose {
            context.contentResolver.unregisterContentObserver(observer)
            owner?.lifecycle?.removeObserver(lifecycleObserver)
        }
    }

    if (!isAllowed) {
        PermissionMessage(
            wasRefused = wasRefused,
            onAllow = {
                if (wasRefused) {
                    context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null)))
                } else {
                    request.launch(DeviceMusic.permission)
                }
            },
            modifier = modifier
        )
        return
    }

    val songs by produceState<List<DeviceSong>?>(initialValue = null, isAllowed, version) { value = DeviceMusic.songs(context) }
    val all = songs
    if (all == null) {
        RowsSkeleton(modifier = modifier)
        return
    }

    val words = query?.trim().orEmpty()
    val unknownArtist = stringResource(R.string.library_device_unknown_artist)

    // The songs in the order of the library, and only the ones the search finds
    val names = remember { nameOrder() }
    val ordered = remember(all, sort) {
        when (sort) {
            LibrarySort.AToZ -> all.sortedWith(compareBy(names) { it.title })
            LibrarySort.ZToA -> all.sortedWith(compareByDescending(names) { it.title })
            LibrarySort.Largest -> all.sortedByDescending { it.sizeBytes }
            LibrarySort.Smallest -> all.sortedBy { it.sizeBytes }
            LibrarySort.Oldest -> all.sortedBy { it.addedAt }
            else -> all.sortedByDescending { it.addedAt }
        }
    }.filter { words.isEmpty() || it.title.contains(words, ignoreCase = true) || it.artist.orEmpty().contains(words, ignoreCase = true) }

    // The artists and the folders, each with its songs, in the order of the library too
    fun groupsOf(key: (DeviceSong) -> String, title: (String) -> String, line: (String) -> String?): List<DeviceGroup> =
        ordered.groupBy(key).map { (name, list) -> DeviceGroup(name, title(name), line(name), list) }.let { groups ->
            when (sort) {
                LibrarySort.AToZ -> groups.sortedWith(compareBy(names) { it.title })
                LibrarySort.ZToA -> groups.sortedWith(compareByDescending(names) { it.title })
                LibrarySort.Largest -> groups.sortedByDescending { group -> group.songs.sumOf { it.sizeBytes } }
                LibrarySort.Smallest -> groups.sortedBy { group -> group.songs.sumOf { it.sizeBytes } }
                LibrarySort.Oldest -> groups.sortedBy { group -> group.songs.maxOf { it.addedAt } }
                else -> groups.sortedByDescending { group -> group.songs.maxOf { it.addedAt } }
            }
        }

    val groups = when (view) {
        DeviceView.Songs -> emptyList()
        DeviceView.Artists -> groupsOf({ it.artist ?: unknownArtist }, { it }, { null })
        DeviceView.Folders -> groupsOf({ it.folder }, { it.substringAfterLast(PathSeparator).ifBlank { PathSeparator.toString() } }, { it })
    }
    val open = groups.firstOrNull { it.key == openKey }

    // The songs on the screen, all of them, or the ones of the artist or of the folder that is open
    val shown = if (view == DeviceView.Songs) ordered else open?.songs.orEmpty()
    val rows = shown.map { song -> song.toItem() }

    // The menu of a song or of a group rises over the whole screen, as the other sheets do, and not inside the list
    val openMenu: (DeviceTarget) -> Unit = { target ->
        OverlaySheet.show {
            DeviceMenu(
                target = target,
                onPlay = { onPlay(target.songs.map { it.toItem() }, 0, null) },
                onShuffle = { onPlay(target.songs.map { it.toItem() }, 0, true) },
                onPlayNext = { target.songs.forEach { PlayerConnection.playNextTrack(context, it.toTrack()) } },
                onAddToQueue = { target.songs.forEach { PlayerConnection.addTrackToQueue(context, it.toTrack()) } },
                onDelete = { deleting = target },
                onDismiss = OverlaySheet::dismiss
            )
        }
    }

    // The question before the delete
    deleting?.let { target ->
        ConfirmDialog(
            title = stringResource(R.string.library_device_delete_title),
            text = if (target.isGroup) {
                pluralStringResource(R.plurals.library_device_delete_text_group, target.songs.size, target.songs.size)
            } else {
                stringResource(R.string.library_device_delete_text, target.title)
            },
            confirmLabel = stringResource(R.string.playlist_delete_confirm),
            cancelLabel = stringResource(R.string.playlist_cancel),
            onConfirm = {
                deleting = null
                startDelete(target.songs)
            },
            onDismiss = { deleting = null }
        )
    }

    // What the marked songs are, from what is on the screen
    val marked = shown.filter { it.id in selectedIds }

    Box(modifier = modifier) {
    Column(modifier = Modifier.fillMaxSize()) {
        if (isSelecting) {
            SelectionHeader(
                count = selectedIds.size,
                allSelected = selectedIds.size == shown.size,
                onExit = leaveSelection,
                onToggleAll = { selectedIds = if (selectedIds.size == shown.size) emptySet() else shown.mapTo(HashSet()) { it.id } }
            )
        } else {
            // The three ways to see the music of the phone, and the button that marks songs when some are on the screen
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(LibraryDimens.ChipGap),
                verticalAlignment = Alignment.CenterVertically,
                contentPadding = PaddingValues(horizontal = LibraryDimens.Side, vertical = LibraryDimens.ChipsVertical)
            ) {
                items(DeviceView.entries) { entry ->
                    Chip(text = stringResource(entry.titleRes), isOn = entry == view) {
                        view = entry
                        openKey = null
                    }
                }
                if (shown.isNotEmpty() && (view == DeviceView.Songs || open != null)) {
                    item(key = "select") {
                        Icon(
                            imageVector = WavvyIcons.Checklist,
                            contentDescription = stringResource(R.string.collection_select),
                            tint = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.padding(start = LibraryDimens.ChipGap).size(LibraryDimens.ViewIcon).clickable { isSelecting = true }
                        )
                    }
                }
            }
        }

        when {
            ordered.isEmpty() -> EmptyList(
                icon = if (words.isEmpty()) WavvyIcons.MusicNote else WavvyIcons.Search,
                text = stringResource(if (words.isEmpty()) R.string.library_device_empty else R.string.library_no_results),
                onFindMusic = null,
                modifier = Modifier.weight(1f)
            )

            // The artists or the folders, a tap opens the one that was touched
            view != DeviceView.Songs && open == null -> LazyColumn(contentPadding = PaddingValues(bottom = LocalMiniPlayerInset.current + LibraryDimens.ListBottom), modifier = Modifier.weight(1f)) {
                items(groups, key = { it.key }) { group ->
                    val count = pluralStringResource(R.plurals.library_device_count, group.songs.size, group.songs.size)
                    LibraryRow(
                        item = HomeItem(
                            kind = if (view == DeviceView.Artists) HomeItemKind.Artist else HomeItemKind.Playlist,
                            id = group.key,
                            title = group.title,
                            thumbnailUrl = group.songs.first().artworkUrl,
                            lineText = listOfNotNull(count, group.line).joinToString(LineSeparator)
                        ),
                        onClick = { openKey = group.key },
                        onMenu = { openMenu(DeviceTarget(group.title, group.songs, isGroup = true)) }
                    )
                }
            }

            // The songs, all of them or the ones of the artist or of the folder that is open, with the arrow back over them
            else -> LazyColumn(contentPadding = PaddingValues(bottom = LocalMiniPlayerInset.current + LibraryDimens.ListBottom + if (isSelecting) SelectionBarSpace else 0.dp), modifier = Modifier.weight(1f)) {
                if (open != null) {
                    item(key = "open") {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(start = LibraryDimens.GridTextTop)) {
                            IconButton(onClick = { openKey = null }) {
                                Icon(imageVector = WavvyIcons.Back, contentDescription = stringResource(R.string.cd_back), tint = MaterialTheme.colorScheme.onBackground)
                            }
                            Text(
                                text = open.title,
                                style = MaterialTheme.typography.bodyLarge.merge(LibraryType.ItemTitle),
                                color = MaterialTheme.colorScheme.onBackground,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = { openMenu(DeviceTarget(open.title, open.songs, isGroup = true)) }) {
                                Icon(imageVector = WavvyIcons.MoreVertical, contentDescription = stringResource(R.string.cd_more), tint = MaterialTheme.colorScheme.onBackground)
                            }
                        }
                    }
                }
                items(rows.size, key = { rows[it].id }) { index ->
                    LibraryRow(
                        item = rows[index],
                        onClick = {
                            if (isSelecting) selectedIds = if (rows[index].id in selectedIds) selectedIds - rows[index].id else selectedIds + rows[index].id
                            else onPlay(rows, index, null)
                        },
                        onMenu = { openMenu(DeviceTarget(rows[index].title, shown.filter { it.id == rows[index].id }, isGroup = false)) },
                        isSelected = if (isSelecting) rows[index].id in selectedIds else null
                    )
                }
            }
        }
    }

    // What can be done with the marked songs, over the mini player
    if (isSelecting) {
        SelectionBar(
            hasSelection = marked.isNotEmpty(),
            onPlayNext = {
                marked.forEach { PlayerConnection.playNextTrack(context, it.toTrack()) }
                leaveSelection()
            },
            onAddToQueue = {
                marked.forEach { PlayerConnection.addTrackToQueue(context, it.toTrack()) }
                leaveSelection()
            },
            onDeleteFiles = {
                // The question of the delete of a group asks for the files of the marked songs
                deleting = DeviceTarget("", marked, isGroup = true)
                leaveSelection()
            },
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = LocalMiniPlayerInset.current)
        )
    }
    }
}

// A song of the phone as a card of the lists, with its artist and its length under its name
private fun DeviceSong.toItem(): HomeItem =
    HomeItem(
        kind = HomeItemKind.Song,
        id = id,
        title = title,
        thumbnailUrl = artworkUrl,
        artists = listOfNotNull(artist),
        durationSeconds = (durationMs / MillisPerSecond).toInt().takeIf { it > 0 },
        lineText = listOfNotNull(artist, formatTime(durationMs / MillisPerSecond)).joinToString(LineSeparator)
    )

// A song of the phone as a track for the queue
private fun DeviceSong.toTrack(): PlayableTrack = PlayableTrack(id, title, artist, artworkUrl, durationMs)

// The menu of a song or of a group, a song plays next, goes to the queue or is deleted, a group is played, shuffled or deleted
@Composable
private fun DeviceMenu(
    target: DeviceTarget,
    onPlay: () -> Unit,
    onShuffle: () -> Unit,
    onPlayNext: () -> Unit,
    onAddToQueue: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    WavvySheet(onDismiss = onDismiss) {
        val closeSheet = LocalSheetClose.current

        CardMenuLayout(
            head = { CardMenuHead(title = target.title, hasCover = false) },
            options = {
                if (target.isGroup) {
                    MenuCard(WavvyIcons.PlayArrow, stringResource(R.string.artist_play)) {
                        onPlay()
                        closeSheet()
                    }
                    MenuCard(WavvyIcons.Shuffle, stringResource(R.string.collection_shuffle)) {
                        onShuffle()
                        closeSheet()
                    }
                }
                MenuCard(WavvyIcons.PlaylistPlay, stringResource(R.string.queue_play_next)) {
                    onPlayNext()
                    closeSheet()
                }
                MenuCard(WavvyIcons.QueueMusic, stringResource(R.string.menu_add_to_queue)) {
                    onAddToQueue()
                    closeSheet()
                }
                MenuCard(WavvyIcons.Delete, stringResource(R.string.library_device_delete)) {
                    onDelete()
                    closeSheet()
                }
            }
        )
    }
}

// Says why the music of the phone needs the permission, with the button that asks for it, or that opens the settings after a refusal
@Composable
private fun PermissionMessage(wasRefused: Boolean, onAllow: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier.fillMaxWidth().padding(horizontal = LibraryDimens.Side)
    ) {
        Icon(
            imageVector = WavvyIcons.MusicNote,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.size(LibraryDimens.EmptyIcon)
        )
        Text(
            text = stringResource(if (wasRefused) R.string.library_device_refused else R.string.library_device_permission),
            style = MaterialTheme.typography.bodyLarge.merge(LibraryType.Empty),
            color = MaterialTheme.colorScheme.secondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(vertical = LibraryDimens.EmptyGap)
        )
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .height(LibraryDimens.EmptyButtonHeight)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.onBackground)
                .clickable(onClick = onAllow)
                .padding(horizontal = LibraryDimens.EmptyButtonPaddingX)
        ) {
            Text(
                text = stringResource(if (wasRefused) R.string.library_device_settings else R.string.library_device_allow),
                style = MaterialTheme.typography.bodyLarge.merge(LibraryType.Action),
                color = MaterialTheme.colorScheme.background
            )
        }
    }
}

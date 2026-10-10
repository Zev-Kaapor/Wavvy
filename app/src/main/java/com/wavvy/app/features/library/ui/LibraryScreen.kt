package com.wavvy.app.features.library.ui

// Android clipboard
// Compose layouts and foundations
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
// Material 3 components
import android.widget.Toast
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.pullToRefresh
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.TextButton
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
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
// UI styling and utilities
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
// Images and lifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
// Coroutines and random
import kotlin.random.Random
import kotlinx.coroutines.launch
// Project resources
import com.wavvy.app.R
import com.wavvy.app.core.designsystem.components.OverlaySheet
import com.wavvy.app.core.designsystem.components.SkeletonHost
import com.wavvy.app.core.designsystem.components.WavvySheet
import com.wavvy.app.core.designsystem.components.skeleton
import com.wavvy.app.core.designsystem.icons.WavvyIcons
import com.wavvy.app.core.designsystem.theme.WavvyTheme
import com.wavvy.app.features.home.data.HomeItem
import com.wavvy.app.features.home.data.HomeItemKind
import com.wavvy.app.features.home.ui.components.HomeMessage
import com.wavvy.app.features.home.ui.rememberItemPlayer
import com.wavvy.app.features.home.ui.rememberListPlayer
import com.wavvy.app.features.home.ui.toPlayableTrack
import com.wavvy.app.features.collection.ui.shareLink
import com.wavvy.app.core.playback.PlayerConnection
import com.wavvy.app.features.library.data.LibrarySource
import com.wavvy.app.features.menu.ItemMenu
import com.wavvy.app.features.podcast.ui.PodcastMenu
import com.wavvy.app.features.collection.ui.CollectionMenu
import com.wavvy.app.features.collection.ui.CollectionMenuData
import com.wavvy.app.features.playlist.ui.EditPlaylist
import com.wavvy.app.features.playlist.ui.EditPlaylistData
import com.wavvy.app.features.playlist.ui.NewPlaylist
import com.wavvy.app.features.playlist.ui.SaveToPlaylist
import com.wavvy.app.features.podcast.ui.PodcastSearchField
import com.wavvy.app.features.player.ui.LocalMiniPlayerInset
import com.wavvy.app.features.profile.ui.LocalProfile
import com.wavvy.app.features.profile.ui.components.ProfileAvatar

// The library, the buttons that choose what is listed, the order and how it is drawn, and the list of what the account saved
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    onProfileClick: () -> Unit,
    onFindMusic: () -> Unit,
    onFindPodcasts: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LibraryViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()
    val onItemClick = rememberItemPlayer()
    val playList = rememberListPlayer()
    val context = LocalContext.current
    val resources = LocalResources.current
    val profileName = LocalProfile.current.name

    // The playlist waiting for the yes before it is deleted
    var deleting by remember { mutableStateOf<HomeItem?>(null) }
    deleting?.let { item ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text(text = stringResource(R.string.playlist_delete_title)) },
            text = { Text(text = stringResource(R.string.playlist_delete_text, item.title)) },
            confirmButton = {
                TextButton(onClick = {
                    deleting = null
                    viewModel.delete(item) { isDone ->
                        val message = if (isDone) resources.getString(R.string.playlist_deleted, item.title) else resources.getString(R.string.playlist_action_error)
                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                    }
                }) { Text(text = stringResource(R.string.playlist_delete_confirm)) }
            },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text(text = stringResource(R.string.playlist_cancel)) } }
        )
    }

    // An album or a playlist has the menu of its page, the other items have their own
    val openMenu: (HomeItem) -> Unit = { item ->
        if (item.kind == HomeItemKind.Album || item.kind == HomeItemKind.Playlist) {
            CollectionMenu.show(
                CollectionMenuData(
                    id = item.id,
                    kindName = item.kind.name,
                    title = item.title,
                    // The line under the name without the kind that comes first in it
                    subtitle = lineOf(item)?.split(" \u2022 ")?.let { parts -> if (parts.size > 1) parts.drop(1) else parts }?.joinToString(" \u2022 "),
                    coverUrl = item.thumbnailUrl,
                    onShuffle = { viewModel.withTracks(item) { songs -> playList(songs, if (songs.isEmpty()) 0 else Random.nextInt(songs.size), true) } },
                    onPlayNext = { viewModel.withTracks(item) { songs -> PlayerConnection.enqueueAll(context, songs.mapNotNull { it.toPlayableTrack() }, afterCurrent = true) } },
                    onAddToQueue = { viewModel.withTracks(item) { songs -> PlayerConnection.enqueueAll(context, songs.mapNotNull { it.toPlayableTrack() }, afterCurrent = false) } },
                    onShare = { shareLink(context, viewModel.shareUrl(item)) },
                    onSaveToPlaylist = { viewModel.withTracks(item) { songs -> SaveToPlaylist.show(songs.map { it.id }) } },
                    onEdit = if (isOwned(item, profileName)) ({ EditPlaylist.show(EditPlaylistData(id = item.id, title = item.title, description = null, privacy = null, coverUrl = item.thumbnailUrl)) }) else null,
                    onDelete = if (isOwned(item, profileName)) ({ deleting = item }) else null
                )
            )
        } else if (item.kind == HomeItemKind.Podcast) {
            OverlaySheet.show {
                PodcastMenu(
                    id = item.id,
                    title = item.title,
                    author = item.author ?: item.lineText,
                    coverUrl = item.thumbnailUrl,
                    onSearch = null,
                    onDismiss = OverlaySheet::dismiss
                )
            }
        } else {
            ItemMenu.show(item)
        }
    }
    val listState = rememberLazyListState()

    // The next page of the list is asked when its end comes near
    LaunchedEffect(listState, state.items.size) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0 }
            .collect { last -> if (last >= listState.layoutInfo.totalItemsCount - LibraryDimens.LoadMoreDistance) viewModel.loadMore() }
    }

    // The button that makes something new loses its words while the list goes down and gets them back when it goes up
    var isActionWide by remember { mutableStateOf(true) }
    val refreshState = rememberPullToRefreshState()
    val scrollDirection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (available.y < -ActionScrollThreshold) isActionWide = false else if (available.y > ActionScrollThreshold) isActionWide = true
                return Offset.Zero
            }
        }
    }
    LaunchedEffect(state.source, state.group) { isActionWide = true }

    Box(modifier = modifier.fillMaxSize().nestedScroll(scrollDirection)) {
        Column(modifier = Modifier.fillMaxSize()) {
            // The search grows from the icon to the whole width while the header fades, and goes back the same way
            val isSearchOpen = state.query != null
            val searchProgress by animateFloatAsState(
                targetValue = if (isSearchOpen) 1f else 0f,
                animationSpec = tween(LibraryDimens.SearchMillis),
                label = "LibrarySearch"
            )
            Box {
                LibraryHeader(
                    onProfileClick = if (isSearchOpen) ({}) else onProfileClick,
                    onSearchClick = if (isSearchOpen) ({}) else viewModel::startSearch,
                    modifier = Modifier.alpha(1f - searchProgress)
                )
                if (searchProgress > 0f) {
                    // Under the status bar and the cutout, as the header is
                    Box(modifier = Modifier.windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top))) {
                        PodcastSearchField(
                            query = state.query.orEmpty(),
                            onQueryChange = viewModel::setQuery,
                            onClose = viewModel::stopSearch,
                            hint = stringResource(R.string.library_search_hint),
                            progress = searchProgress
                        )
                    }
                }
            }

            LibraryChips(
                group = state.group,
                source = state.source,
                onGroupClick = viewModel::selectGroup,
                onSourceClick = viewModel::selectSource,
                onClear = viewModel::clear
            )

            if (state.group != LibraryGroup.Downloads) {
                // The orders of YouTube Music when the list tells them, with the two by the name that are made here
                val aToZ = stringResource(LibrarySort.AToZ.titleRes)
                val zToA = stringResource(LibrarySort.ZToA.titleRes)
                val isLocalSort = state.sort == LibrarySort.AToZ || state.sort == LibrarySort.ZToA
                val sortRows = if (state.remoteSorts.isEmpty()) {
                    state.sorts.map { sort -> SortRow(stringResource(sort.titleRes), sort == state.sort) { viewModel.selectSort(sort) } }
                } else {
                    state.remoteSorts.map { option -> SortRow(option.title, !isLocalSort && option.title == state.remoteSelected) { viewModel.selectRemoteSort(option) } } +
                        listOf(LibrarySort.AToZ to aToZ, LibrarySort.ZToA to zToA)
                            .filter { (_, title) -> state.remoteSorts.none { it.title.equals(title, ignoreCase = true) } }
                            .map { (sort, title) -> SortRow(title, sort == state.sort) { viewModel.selectSort(sort) } }
                }
                SortLine(
                    title = if (state.remoteSorts.isEmpty() || isLocalSort) stringResource(state.sort.titleRes) else state.remoteSelected.orEmpty(),
                    isGrid = state.isGrid,
                    onSortClick = {
                        OverlaySheet.show {
                            SortSheet(
                                rows = sortRows,
                                onSelect = { row ->
                                    OverlaySheet.dismiss()
                                    row.onSelect()
                                },
                                onDismiss = OverlaySheet::dismiss
                            )
                        }
                    },
                    onViewClick = viewModel::toggleGrid
                )
            }

            val shown = state.shown
            when {
                state.group == LibraryGroup.Downloads -> EmptyList(
                    icon = WavvyIcons.Download,
                    text = stringResource(R.string.library_downloads_soon),
                    onFindMusic = null,
                    modifier = Modifier.weight(1f)
                )

                state.status == LibraryStatus.Loading -> RowsSkeleton(modifier = Modifier.weight(1f))

                state.status == LibraryStatus.Error -> HomeMessage(
                    text = stringResource(R.string.library_error),
                    modifier = Modifier.weight(1f),
                    actionLabel = stringResource(R.string.home_retry),
                    onAction = { viewModel.load() }
                )

                shown.isEmpty() && state.query?.isNotBlank() == true -> EmptyList(
                    icon = WavvyIcons.Search,
                    text = stringResource(R.string.library_no_results),
                    onFindMusic = null,
                    modifier = Modifier.weight(1f)
                )

                shown.isEmpty() -> EmptyList(
                    icon = emptyIconOf(state.source),
                    text = stringResource(emptyTextOf(state.source)),
                    onFindMusic = onFindMusic,
                    modifier = Modifier.weight(1f)
                )

                state.isGrid -> LazyVerticalGrid(
                    columns = GridCells.Fixed(LibraryDimens.GridColumns),
                    modifier = Modifier
                        .weight(1f)
                        .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)),
                    contentPadding = PaddingValues(
                        start = LibraryDimens.Side,
                        end = LibraryDimens.Side,
                        top = LibraryDimens.GridTextTop,
                        bottom = LocalMiniPlayerInset.current + LibraryDimens.ListBottom
                    ),
                    horizontalArrangement = Arrangement.spacedBy(LibraryDimens.GridGap),
                    verticalArrangement = Arrangement.spacedBy(LibraryDimens.GridRowGap)
                ) {
                    gridItems(shown, key = { it.id }) { item -> LibraryCover(item = item, onClick = { onItemClick(item) }) }
                }

                else -> LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .pullToRefresh(state = refreshState, isRefreshing = state.isRefreshing, onRefresh = viewModel::pullRefresh)
                        .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)),
                    contentPadding = PaddingValues(bottom = LocalMiniPlayerInset.current + LibraryDimens.ListBottom)
                ) {
                    items(shown, key = { it.id }) { item -> LibraryRow(item = item, onClick = { onItemClick(item) }, onMenu = { openMenu(item) }) }
                }
            }
        }

        // The mark that shows while the list is pulled, under the header and the buttons
        PullToRefreshDefaults.Indicator(
            state = refreshState,
            isRefreshing = state.isRefreshing,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top))
                .padding(top = LibraryDimens.RefreshTop)
        )

        // The button that makes something new, only in its place for now
        if (state.group != LibraryGroup.Downloads && (state.source == LibrarySource.Recent || state.source == LibrarySource.Playlists || state.source == LibrarySource.Podcasts)) {
            NewButton(
                text = stringResource(if (state.source == LibrarySource.Podcasts) R.string.library_add_podcast else R.string.library_new_playlist),
                isWide = isActionWide,
                onClick = if (state.source == LibrarySource.Podcasts) onFindPodcasts else ({ NewPlaylist.show() }),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = LibraryDimens.ActionEnd, bottom = LocalMiniPlayerInset.current + LibraryDimens.ActionBottom)
            )
        }
    }
}

// The name of the screen on the left, and on the right the history, which is only in its place for now, the search and the photo of the account
@Composable
private fun LibraryHeader(onProfileClick: () -> Unit, onSearchClick: () -> Unit, modifier: Modifier = Modifier) {
    val profile = LocalProfile.current

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))
            .padding(horizontal = LibraryDimens.Side, vertical = LibraryDimens.HeaderTop)
    ) {
        Text(
            text = stringResource(R.string.nav_library),
            style = MaterialTheme.typography.headlineSmall.merge(LibraryType.Title),
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(1f)
        )

        IconButton(onClick = { }) {
            Icon(imageVector = WavvyIcons.History, contentDescription = null, tint = MaterialTheme.colorScheme.onBackground)
        }
        IconButton(onClick = onSearchClick) {
            Icon(imageVector = WavvyIcons.Search, contentDescription = stringResource(R.string.library_search_hint), tint = MaterialTheme.colorScheme.onBackground)
        }
        ProfileAvatar(
            photo = profile.photo,
            size = LibraryDimens.ViewIcon + LibraryDimens.GridTextTop,
            modifier = Modifier.clickable(onClick = onProfileClick)
        )
    }
}

// The buttons of the top, with none on they are all there, with one on only it stays, with a cross before it that turns it off, and the lists of the same button come after it
@Composable
private fun LibraryChips(
    group: LibraryGroup?,
    source: LibrarySource,
    onGroupClick: (LibraryGroup) -> Unit,
    onSourceClick: (LibrarySource) -> Unit,
    onClear: () -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(LibraryDimens.ChipGap),
        contentPadding = PaddingValues(horizontal = LibraryDimens.Side, vertical = LibraryDimens.ChipsVertical),
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
    ) {
        if (group == null) {
            items(LibraryGroup.entries) { entry -> Chip(text = stringResource(entry.titleRes), isOn = false) { onGroupClick(entry) } }
        } else {
            item(key = "clear") {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(width = LibraryDimens.ClearWidth, height = LibraryDimens.ChipHeight)
                        .clip(RoundedCornerShape(LibraryDimens.ChipCorner))
                        .background(MaterialTheme.colorScheme.onBackground)
                        .clickable(onClick = onClear)
                ) {
                    Icon(
                        imageVector = WavvyIcons.Close,
                        contentDescription = stringResource(R.string.cd_close),
                        tint = MaterialTheme.colorScheme.background,
                        modifier = Modifier.size(LibraryDimens.ClearIcon)
                    )
                }
            }

            // A button with more than one list shows them all, the one in use is on, a button with one shows only itself
            if (group.sources.size > 1) {
                items(group.sources.size) { index ->
                    Chip(text = stringResource(group.sourceTitles[index]), isOn = group.sources[index] == source) { onSourceClick(group.sources[index]) }
                }
            } else {
                item(key = "group") { Chip(text = stringResource(group.titleRes), isOn = true) { } }
            }
        }
    }
}

// A button of the top, light when it is on
@Composable
private fun Chip(text: String, isOn: Boolean, onClick: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .height(LibraryDimens.ChipHeight)
            .clip(RoundedCornerShape(LibraryDimens.ChipCorner))
            .background(if (isOn) MaterialTheme.colorScheme.onBackground else WavvyTheme.colors.chip)
            .clickable(onClick = onClick)
            .padding(horizontal = LibraryDimens.ChipPaddingX)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium.merge(LibraryType.Chip),
            color = if (isOn) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.onBackground,
            maxLines = 1
        )
    }
}

// The order in use with the arrow that changes it, and the button that changes between rows and covers
@Composable
private fun SortLine(title: String, isGrid: Boolean, onSortClick: () -> Unit, onViewClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .height(LibraryDimens.SortHeight)
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
            .padding(start = LibraryDimens.Side, end = LibraryDimens.Side)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f).clickable(onClick = onSortClick)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.merge(LibraryType.Sort),
                color = MaterialTheme.colorScheme.secondary,
                maxLines = 1
            )
            Icon(
                imageVector = WavvyIcons.ChevronDown,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(LibraryDimens.SortArrow)
            )
        }

        Icon(
            imageVector = if (isGrid) WavvyIcons.ViewList else WavvyIcons.GridView,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier
                .size(LibraryDimens.ViewIcon)
                .clickable(onClick = onViewClick)
        )
    }
}

// One way to put the list in order, the words, if it is the one in use and what it does when it is chosen
private class SortRow(val title: String, val isSelected: Boolean, val onSelect: () -> Unit)

// The ways to put the list in order, the one in use has a check
@Composable
private fun SortSheet(rows: List<SortRow>, onSelect: (SortRow) -> Unit, onDismiss: () -> Unit) {
    WavvySheet(onDismiss = onDismiss) {
        Column(modifier = Modifier.verticalScroll(rememberScrollState()).padding(bottom = WavvyTheme.dimens.spaceMedium)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = LibraryDimens.Side, end = WavvyTheme.dimens.spaceSmall)
            ) {
                Text(
                    text = stringResource(R.string.library_sort_by),
                    style = MaterialTheme.typography.bodyLarge.merge(LibraryType.Action),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = WavvyIcons.Close,
                        contentDescription = stringResource(R.string.cd_close),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            rows.forEach { row ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(LibraryDimens.RowHeight - LibraryDimens.GridTextTop * 2)
                        .clickable { onSelect(row) }
                        .padding(horizontal = LibraryDimens.Side)
                ) {
                    Box(modifier = Modifier.width(LibraryDimens.ClearWidth + LibraryDimens.ChipGap)) {
                        if (row.isSelected) {
                            Icon(imageVector = WavvyIcons.Check, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                    Text(text = row.title, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                }
            }
        }
    }
}

// An item of the list as a row, the cover on the left, round for an artist, its name and the line under it, and the three dots
@Composable
private fun LibraryRow(item: HomeItem, onClick: () -> Unit, onMenu: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .height(LibraryDimens.RowHeight)
            .clickable(onClick = onClick)
            .padding(start = LibraryDimens.Side)
    ) {
        Cover(item = item, modifier = Modifier.size(LibraryDimens.RowCover))

        Column(modifier = Modifier.weight(1f).padding(horizontal = LibraryDimens.RowGap)) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.bodyLarge.merge(LibraryType.ItemTitle),
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            lineOf(item)?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium.merge(LibraryType.ItemLine),
                    color = MaterialTheme.colorScheme.secondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        IconButton(onClick = onMenu) {
            Icon(
                imageVector = WavvyIcons.MoreVertical,
                contentDescription = stringResource(R.string.cd_more),
                tint = MaterialTheme.colorScheme.onBackground
            )
        }
    }
}

// An item of the list as a big cover with its name and the line under it, an artist is round with the words in the middle
@Composable
private fun LibraryCover(item: HomeItem, onClick: () -> Unit) {
    val isArtist = item.kind == HomeItemKind.Artist

    Column(
        horizontalAlignment = if (isArtist) Alignment.CenterHorizontally else Alignment.Start,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
    ) {
        Cover(item = item, modifier = Modifier.fillMaxWidth().aspectRatio(1f))

        Text(
            text = item.title,
            style = MaterialTheme.typography.bodyLarge.merge(LibraryType.ItemTitle),
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = if (isArtist) TextAlign.Center else TextAlign.Start,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = LibraryDimens.GridTextTop)
        )
        lineOf(item)?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodyMedium.merge(LibraryType.ItemLine),
                color = MaterialTheme.colorScheme.secondary,
                textAlign = if (isArtist) TextAlign.Center else TextAlign.Start,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// The picture of an item, round for an artist
@Composable
private fun Cover(item: HomeItem, modifier: Modifier) {
    AsyncImage(
        model = item.thumbnailUrl,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = modifier
            .clip(if (item.kind == HomeItemKind.Artist) CircleShape else RoundedCornerShape(LibraryDimens.RowCoverCorner))
            .background(MaterialTheme.colorScheme.surfaceContainer)
    )
}

// The line under the name as YouTube Music writes it
private fun lineOf(item: HomeItem): String? = item.lineText ?: item.countText

// A playlist the account made names the account in the line under it
private fun isOwned(item: HomeItem, profileName: String?): Boolean =
    item.kind == HomeItemKind.Playlist && !profileName.isNullOrBlank() &&
        (item.author == profileName || lineOf(item)?.contains(profileName) == true)

// The room of the rows while the list is asked, a cover and two lines in the places of the real ones
@Composable
private fun RowsSkeleton(modifier: Modifier = Modifier) {
    SkeletonHost(modifier = modifier.fillMaxWidth()) {
        repeat(LibraryDimens.SkeletonRows) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(LibraryDimens.RowHeight)
                    .padding(horizontal = LibraryDimens.Side)
            ) {
                Box(modifier = Modifier.size(LibraryDimens.RowCover).skeleton(RoundedCornerShape(LibraryDimens.RowCoverCorner)))
                Column(modifier = Modifier.weight(1f).padding(start = LibraryDimens.RowGap)) {
                    Box(modifier = Modifier.fillMaxWidth(SkeletonTitleFraction).height(LibraryDimens.GridTextTop * 2).skeleton(MaterialTheme.shapes.extraSmall))
                    Spacer(modifier = Modifier.height(LibraryDimens.GridTextTop))
                    Box(modifier = Modifier.fillMaxWidth(SkeletonLineFraction).height(LibraryDimens.GridTextTop * 2).skeleton(MaterialTheme.shapes.extraSmall))
                }
            }
        }
    }
}

// How much of the width the lines of the placeholder take
private const val SkeletonTitleFraction = 0.7f
private const val SkeletonLineFraction = 0.4f

// What shows when a list has nothing, an icon, what will appear there and the button that goes to look for music
@Composable
private fun EmptyList(icon: ImageVector, text: String, onFindMusic: (() -> Unit)?, modifier: Modifier = Modifier) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier.fillMaxWidth().padding(horizontal = LibraryDimens.Side)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.size(LibraryDimens.EmptyIcon)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge.merge(LibraryType.Empty),
            color = MaterialTheme.colorScheme.secondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(vertical = LibraryDimens.EmptyGap)
        )
        if (onFindMusic != null) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .height(LibraryDimens.EmptyButtonHeight)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onBackground)
                    .clickable(onClick = onFindMusic)
                    .padding(horizontal = LibraryDimens.EmptyButtonPaddingX)
            ) {
                Text(
                    text = stringResource(R.string.library_find_music),
                    style = MaterialTheme.typography.bodyLarge.merge(LibraryType.Action),
                    color = MaterialTheme.colorScheme.background
                )
            }
        }

    }
}

// The button that makes something new, white, with a plus before its words, which only stands in its place for now
@Composable
private fun NewButton(text: String, isWide: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    // Round with only the plus when it is not wide, so the room around the plus follows the words going in and out
    val sidePadding by animateDpAsState(
        targetValue = if (isWide) LibraryDimens.ActionPaddingX else (LibraryDimens.ActionHeight - LibraryDimens.ActionIcon) / 2,
        animationSpec = tween(LibraryDimens.ActionMillis),
        label = "NewButtonPadding"
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .height(LibraryDimens.ActionHeight)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.onBackground)
            .clickable(onClick = onClick)
            .padding(horizontal = sidePadding)
    ) {
        Icon(
            imageVector = WavvyIcons.Add,
            contentDescription = text,
            tint = MaterialTheme.colorScheme.background,
            modifier = Modifier.size(LibraryDimens.ActionIcon)
        )
        AnimatedVisibility(
            visible = isWide,
            enter = expandHorizontally(tween(LibraryDimens.ActionMillis)) + fadeIn(tween(LibraryDimens.ActionMillis)),
            exit = shrinkHorizontally(tween(LibraryDimens.ActionMillis)) + fadeOut(tween(LibraryDimens.ActionMillis))
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge.merge(LibraryType.Action),
                color = MaterialTheme.colorScheme.background,
                maxLines = 1,
                softWrap = false,
                modifier = Modifier.padding(start = LibraryDimens.ActionIconGap)
            )
        }
    }
}

// How far a drag counts to change the button that makes something new
private const val ActionScrollThreshold = 4f

// The icon and the words of an empty list, by the list
private fun emptyIconOf(source: LibrarySource): ImageVector = when (source) {
    LibrarySource.Albums -> WavvyIcons.Album
    LibrarySource.Artists, LibrarySource.Subscriptions, LibrarySource.Channels -> WavvyIcons.Person
    LibrarySource.Podcasts -> WavvyIcons.Podcasts
    else -> WavvyIcons.MusicNote
}

private fun emptyTextOf(source: LibrarySource): Int = when (source) {
    LibrarySource.Songs -> R.string.library_empty_songs
    LibrarySource.Albums -> R.string.library_empty_albums
    LibrarySource.Artists -> R.string.library_empty_artists
    LibrarySource.Subscriptions -> R.string.library_empty_subscriptions
    LibrarySource.Playlists -> R.string.library_empty_playlists
    LibrarySource.Podcasts -> R.string.library_empty_podcasts
    LibrarySource.Channels -> R.string.library_empty_channels
    LibrarySource.Recent -> R.string.library_empty_recent
}

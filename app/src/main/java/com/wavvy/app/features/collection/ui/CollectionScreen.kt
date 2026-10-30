package com.wavvy.app.features.collection.ui

// Compose animation
import android.content.res.Configuration
// Back button of the system
import androidx.activity.compose.BackHandler
// Compose layouts and foundations
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
// Material 3 components
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
// Compose state and runtime
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
// UI styling and utilities
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.pullToRefresh
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
// Lifecycle, image loading and sharing
import android.content.Context
import android.content.Intent
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import coil3.request.transformations
// Project resources
import com.wavvy.app.core.designsystem.components.rememberBackdropFade
import com.wavvy.app.R
import com.wavvy.app.core.designsystem.components.SelectionBar
import com.wavvy.app.core.designsystem.components.SelectionBarSpace
import com.wavvy.app.core.designsystem.components.SelectionHeader
import com.wavvy.app.core.designsystem.components.SkeletonHost
import com.wavvy.app.core.designsystem.components.skeleton
import com.wavvy.app.core.designsystem.icons.WavvyIcons
import com.wavvy.app.core.designsystem.theme.WavvyMotion
import com.wavvy.app.core.designsystem.theme.WavvyTheme
import com.wavvy.app.core.innertube.resize
import com.wavvy.app.core.navigation.ItemNavigator
import com.wavvy.app.core.playback.PlayerConnection
import com.wavvy.app.features.artist.data.ArtistRepository
import com.wavvy.app.features.menu.LocalEditablePlaylist
import com.wavvy.app.features.playlist.data.PlaylistActions
import com.wavvy.app.features.playlist.data.PlaylistChanges
import com.wavvy.app.features.playlist.data.PlaylistEvent
import com.wavvy.app.features.playlist.data.PlaylistPrivacy
import com.wavvy.app.features.playlist.data.PlaylistRepository
import kotlinx.coroutines.launch
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import com.wavvy.app.features.playlist.ui.AddSong
import com.wavvy.app.features.playlist.ui.EditPlaylist
import com.wavvy.app.features.playlist.ui.EditPlaylistData
import com.wavvy.app.features.playlist.ui.SaveToPlaylist
import com.wavvy.app.features.playlist.ui.SortChoice
import com.wavvy.app.features.playlist.ui.SortChoiceSheet
import com.wavvy.app.core.designsystem.components.ConfirmDialog
import com.wavvy.app.core.designsystem.components.OverlaySheet
import com.wavvy.app.features.collection.data.CollectionKind
import com.wavvy.app.features.collection.data.CollectionPage
import com.wavvy.app.features.comments.ui.CommentsSheet
import com.wavvy.app.features.home.ui.components.HomeDimens
import com.wavvy.app.features.home.ui.components.HomeListItem
import com.wavvy.app.features.home.ui.components.HomeMessage
import com.wavvy.app.features.home.ui.components.HomeShelf
import com.wavvy.app.features.home.ui.rememberItemPlayer
import com.wavvy.app.features.home.ui.rememberListPlayer
import com.wavvy.app.features.home.ui.toPlayableTrack
import com.wavvy.app.core.download.DownloadArt
import com.wavvy.app.core.download.DownloadFolder
import com.wavvy.app.core.download.DownloadPhase
import com.wavvy.app.core.download.LocalDownloadFolder
import com.wavvy.app.core.download.Downloads
import com.wavvy.app.features.player.ui.LocalMiniPlayerInset
import com.wavvy.app.features.player.ui.components.BackdropBlur
import com.wavvy.app.features.player.ui.components.ArtistPickerSheet
import com.wavvy.app.features.player.ui.components.PlayerDimens
import com.wavvy.app.features.search.ui.components.SearchSkeleton
import com.wavvy.app.core.designsystem.components.Toasts

// Page of an album or of a playlist, tinted by its cover, with the cover, the title, the round buttons that play it and under them its songs
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectionScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CollectionViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val resources = LocalResources.current
    val playList = rememberListPlayer()
    val onItemClick = rememberItemPlayer()
    val listState = rememberLazyListState()
    val backdropFade = rememberBackdropFade(listState) { listState.layoutInfo.viewportSize.height * CollectionDimens.BackdropFadeFraction }

    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    val refreshState = rememberPullToRefreshState()
    val page = state.page

    // Songs being marked, on or off and the ids of the ones marked, back leaves the mode before it leaves the page
    var isSelecting by remember { mutableStateOf(false) }
    var selectedIds by remember { mutableStateOf(emptySet<String>()) }
    val leaveSelection = {
        isSelecting = false
        selectedIds = emptySet()
    }
    BackHandler(enabled = isSelecting, onBack = leaveSelection)
    var isRemovingMarked by remember { mutableStateOf(false) }

    // The cover blurred once into a small picture, the same backdrop the open player has
    val backdrop = remember(page?.thumbnailUrl) {
        page?.thumbnailUrl?.let { url ->
            ImageRequest.Builder(context)
                .data(url.resize(PlayerDimens.BackdropRequestSize, PlayerDimens.BackdropRequestSize))
                .size(PlayerDimens.BackdropRequestSize)
                .transformations(BackdropBlur(PlayerDimens.BackdropBlurRadius, PlayerDimens.BackdropBlurPasses))
                .crossfade(WavvyMotion.ScreenFadeMillis)
                .build()
        }
    }
    val pageColor = MaterialTheme.colorScheme.background
    val isLight = pageColor.luminance() > 0.5f

    // True when the last rows on the screen are close to the end of the list
    val nearEnd by remember {
        derivedStateOf {
            val info = listState.layoutInfo
            val last = info.visibleItemsInfo.lastOrNull()?.index ?: 0
            last >= info.totalItemsCount - CollectionDimens.LoadMoreThreshold
        }
    }
    LaunchedEffect(nearEnd, state.continuation, state.tracks.size) {
        if (nearEnd) viewModel.loadMore()
    }

    // The yes asked before the playlist of the page is deleted, the page closes once it is gone
    var isDeleting by remember { mutableStateOf(false) }
    if (isDeleting && page != null) {
        ConfirmDialog(
            title = stringResource(R.string.playlist_delete_title),
            text = stringResource(R.string.playlist_delete_text, page.title),
            confirmLabel = stringResource(R.string.playlist_delete_confirm),
            cancelLabel = stringResource(R.string.playlist_cancel),
            onConfirm = {
                isDeleting = false
                viewModel.delete { isDone ->
                    val message = if (isDone) resources.getString(R.string.playlist_deleted, page.title) else resources.getString(R.string.playlist_action_error)
                    Toasts.show(message, isError = !isDone)
                    if (isDone) onBack()
                }
            },
            onDismiss = { isDeleting = false }
        )
    }

    // How much of the page is downloaded, the button and the menu tell it and start it
    val downloads by Downloads.items.collectAsState()

    // What narrows the songs of the page, they are all that is drawn, marked and played from the list while it is on
    var filters by remember { mutableStateOf(CollectionFilters()) }
    val songs = remember(state.shown, filters, downloads) {
        filters.apply(state.shown) { id -> downloads[id]?.phase == DownloadPhase.Completed }
    }
    val songIds = state.tracks.map { it.id }
    val doneCount = songIds.count { downloads[it]?.phase == DownloadPhase.Completed }
    val runningCount = songIds.count { downloads[it]?.phase == DownloadPhase.Queued || downloads[it]?.phase == DownloadPhase.Downloading }
    val isAllDone = songIds.isNotEmpty() && doneCount == songIds.size

    // A page with songs that are downloaded keeps its cover too, so the page opens whole without the internet
    LaunchedEffect(page?.thumbnailUrl, doneCount > 0) {
        if (doneCount > 0) DownloadArt.ensure(context, page?.thumbnailUrl)
    }
    val downloadProgress = if (runningCount > 0 && songIds.isNotEmpty()) doneCount.toFloat() / songIds.size else null
    val startDownload = {
        val folder = state.page?.let { DownloadFolder(viewModel.id, it.title, it.thumbnailUrl) }
        viewModel.withAllTracks { songs -> Downloads.enqueueAll(context, songs.mapNotNull { it.toPlayableTrack() }, folder) }
    }
    val downloadLabel = when {
        isAllDone -> stringResource(R.string.download_remove_all)
        runningCount > 0 -> stringResource(R.string.download_cancel_all, doneCount, songIds.size)
        else -> stringResource(R.string.download_do)
    }

    // The question asked before the downloads of the page are taken out, the answer that takes them is the button that stands out
    var isRemovingDownloads by remember { mutableStateOf(false) }
    if (isRemovingDownloads && page != null) {
        ConfirmDialog(
            title = stringResource(R.string.download_folder_remove_title),
            text = stringResource(R.string.download_collection_remove_text, page.title),
            confirmLabel = stringResource(R.string.download_remove_confirm),
            cancelLabel = stringResource(R.string.playlist_cancel),
            onConfirm = {
                isRemovingDownloads = false
                viewModel.withAllTracks { songs -> Downloads.removeAll(context, songs.map { song -> song.id }) }
            },
            onDismiss = { isRemovingDownloads = false }
        )
    }

    // Saves the page to the library or takes it out, the menu and the button of an album do it
    val toggleLibrary = {
        viewModel.toggleLibrary { isSaved, isDone ->
            val message = if (!isDone) R.string.playlist_action_error else if (isSaved) R.string.collection_library_saved else R.string.collection_library_removed
            Toasts.show(resources.getString(message), isError = !isDone)
        }
    }

    // An album has the save and the comments among its buttons, as YouTube Music
    val isAlbum = viewModel.kind == CollectionKind.Album

    // The comments of the album, put aside while the page of who wrote one is open and opened again when the page comes back
    val openComments = {
        OverlaySheet.show {
            CommentsSheet(albumId = viewModel.id, onDismiss = OverlaySheet::dismiss, onAuthorOpened = { viewModel.isCommentsParked = true })
        }
    }
    LaunchedEffect(Unit) {
        if (viewModel.isCommentsParked) {
            viewModel.isCommentsParked = false
            openComments()
        }
    }

    // The menu of the page, its actions work on every song of it
    val sortSheetTitle = stringResource(R.string.collection_sort_sheet)
    val songCount = pluralStringResource(R.plurals.collection_song_count, state.tracks.size, state.tracks.size)
    val openMenu = {
        page?.let {
            CollectionMenu.show(
                CollectionMenuData(
                    id = viewModel.id,
                    kindName = it.kind.name,
                    title = it.title,
                    subtitle = listOfNotNull(it.owner, songCount).joinToString(" • "),
                    coverUrl = it.thumbnailUrl?.resize(HomeDimens.CoverRequestSize, HomeDimens.CoverRequestSize),
                    onShuffle = { viewModel.play(shuffle = true, onPlay = playList) },
                    onSelect = if (state.tracks.isEmpty()) null else ({ isSelecting = true }),
                    onMix = { viewModel.withAllTracks { songs -> songs.firstNotNullOfOrNull { song -> song.toPlayableTrack() }?.let { track -> PlayerConnection.play(context, track) } } },
                    onGoToArtist = it.owners.takeIf { owners -> owners.isNotEmpty() }?.let { owners ->
                        {
                            if (owners.size == 1) {
                                ItemNavigator.openArtist(owners.first().second)
                            } else {
                                OverlaySheet.show {
                                    ArtistPickerSheet(
                                        artists = owners,
                                        onArtistClick = { id ->
                                            OverlaySheet.dismiss()
                                            ItemNavigator.openArtist(id)
                                        },
                                        onDismiss = OverlaySheet::dismiss
                                    )
                                }
                            }
                        }
                    },
                    hasShareButton = it.kind != CollectionKind.Album,
                    onPlayNext = { viewModel.withAllTracks { songs -> PlayerConnection.enqueueAll(context, songs.mapNotNull { it.toPlayableTrack() }, afterCurrent = true) } },
                    onAddToQueue = { viewModel.withAllTracks { songs -> PlayerConnection.enqueueAll(context, songs.mapNotNull { it.toPlayableTrack() }, afterCurrent = false) } },
                    onSaveToPlaylist = { viewModel.withAllTracks { songs -> SaveToPlaylist.show(songs.map { it.id }) } },
                    onShare = { shareLink(context, viewModel.shareUrl) },
                    sortTitle = viewModel.sortTitle,
                    onSort = if (it.kind != CollectionKind.Playlist) null else ({
                        // The orders of YouTube Music when the page tells them, the ones made here when it does not
                        val choices = if (it.sortOptions.isEmpty()) {
                            CollectionOrder.entries.map { order ->
                                SortChoice(resources.getString(order.titleRes), order == state.order) { viewModel.selectOrder(order) }
                            }
                        } else {
                            it.sortOptions.map { option ->
                                SortChoice(option.title, option.title == viewModel.sortTitle) { viewModel.selectSort(option) }
                            }
                        }
                        OverlaySheet.show {
                            SortChoiceSheet(
                                title = sortSheetTitle,
                                choices = choices,
                                onDismiss = OverlaySheet::dismiss
                            )
                        }
                    }),
                    onEdit = viewModel.editablePlaylistId?.takeIf { _ -> it.isEditable }?.let { id ->
                        {
                            EditPlaylist.show(
                                EditPlaylistData(
                                    id = id,
                                    title = it.title,
                                    description = it.description,
                                    privacy = PlaylistPrivacy.entries.firstOrNull { privacy -> privacy.status == it.privacy },
                                    coverUrl = it.thumbnailUrl
                                )
                            )
                        }
                    },
                    onDelete = viewModel.editablePlaylistId?.takeIf { _ -> it.isEditable }?.let { { isDeleting = true } }
                )
            )
        }
        Unit
    }

    // Lying down the search row goes to the top as the field opens, so the keyboard never hides it
    LaunchedEffect(filters.query != null) {
        if (isLandscape && filters.query != null) listState.animateScrollToItem(if (page?.isEditable == true && page.kind == CollectionKind.Playlist) 2 else 1)
    }

    // Solid, so the screen under it does not show through while the page slides
    Box(modifier = modifier.fillMaxSize().background(pageColor)) {
        // The color of the cover fades into the page as the list goes down and comes back as it goes up
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(CollectionDimens.BackdropFraction)
                .align(Alignment.TopStart)
                .graphicsLayer {
                    alpha = backdropFade()
                }
        ) {
            if (backdrop != null) {
                AsyncImage(
                    model = backdrop,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().alpha(if (isLight) CollectionDimens.BackdropLightAlpha else 1f)
                )
            }

            // A little dark on top, only on the dark theme, and the bottom of the blur fades into the page
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0f to if (isLight) Color.Transparent else Color.Black.copy(alpha = CollectionDimens.BackdropTopShade),
                            CollectionDimens.BackdropHold to Color.Transparent,
                            1f to pageColor
                        )
                    )
            )
        }

        // The mark that shows while the page is pulled, under the status bar and the bar of the page
        PullToRefreshDefaults.Indicator(
            state = refreshState,
            isRefreshing = state.isRefreshing,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top))
                .padding(top = CollectionDimens.RefreshTop)
        )

        Column(modifier = Modifier.fillMaxSize()) {
            if (isSelecting) {
                SelectionHeader(
                    count = selectedIds.size,
                    allSelected = selectedIds.size == songs.size,
                    onExit = leaveSelection,
                    onToggleAll = { selectedIds = if (selectedIds.size == songs.size) emptySet() else songs.mapTo(HashSet()) { it.id } }
                )
            } else {
                CollectionTopBar(pageTitle = viewModel.pageTitle, onBack = onBack)
            }

            when {
                state.status == CollectionStatus.Loading -> CollectionSkeleton()

                state.status == CollectionStatus.Error || page == null -> HomeMessage(
                    text = stringResource(R.string.collection_error),
                    modifier = Modifier.weight(1f),
                    actionLabel = stringResource(R.string.home_retry),
                    onAction = viewModel::load
                )

                else -> LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .pullToRefresh(state = refreshState, isRefreshing = state.isRefreshing, onRefresh = viewModel::pullRefresh)
                        .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
                        .then(if (isLandscape) Modifier.imePadding() else Modifier),
                    contentPadding = PaddingValues(bottom = LocalMiniPlayerInset.current + CollectionDimens.ListBottom + if (isSelecting) SelectionBarSpace else 0.dp)
                ) {
                    item(key = "header") {
                        CollectionHeader(
                            page = page,
                            showTitle = viewModel.pageTitle.isEmpty(),
                            isEnabled = state.tracks.isNotEmpty() && !state.isPreparing,
                            isOwned = page.isEditable && page.kind == CollectionKind.Playlist,
                            onShuffle = { viewModel.play(shuffle = true, onPlay = playList) },
                            onPlay = { viewModel.play(shuffle = false, onPlay = playList) },
                            onMenu = openMenu,
                            onEdit = {
                                EditPlaylist.show(
                                    EditPlaylistData(
                                        id = viewModel.id,
                                        title = page.title,
                                        description = page.description,
                                        privacy = PlaylistPrivacy.entries.firstOrNull { privacy -> privacy.status == page.privacy },
                                        coverUrl = page.thumbnailUrl
                                    )
                                )
                            },
                            onShare = { shareLink(context, viewModel.shareUrl) },
                            isDownloaded = isAllDone,
                            downloadProgress = downloadProgress,
                            onDownload = { if (isAllDone || runningCount > 0) isRemovingDownloads = true else startDownload() },
                            isAlbum = isAlbum,
                            onToggleLibrary = toggleLibrary,
                            onComments = openComments
                        )
                    }

                    // A playlist of the account starts with the row that gives it a song
                    if (page.isEditable && page.kind == CollectionKind.Playlist) {
                        item(key = "add") {
                            AddSongRow(onClick = { AddSong.show(viewModel.id) })
                        }
                    }

                    if (state.tracks.isEmpty()) {
                        item(key = "empty") {
                            HomeMessage(text = stringResource(R.string.collection_empty), modifier = Modifier.height(HomeDimens.EmptyHeight))
                        }
                    } else if (!isSelecting) {
                        if (!isAlbum && (state.tracks.size >= CollectionDimens.FilterMinSongs || filters.isActive)) {
                            item(key = "filters") { CollectionFilterRow(filters = filters, onChange = { filters = it }) }
                        }

                        // The filters left nothing, the page has songs but none of them passes
                        if (songs.isEmpty() && filters.isActive) {
                            item(key = "no_results") {
                                HomeMessage(text = stringResource(R.string.collection_no_results), modifier = Modifier.height(HomeDimens.EmptyHeight))
                            }
                        }
                    }

                    itemsIndexed(songs, key = { _, track -> track.id }) { index, track ->
                        // The menu of a song of a playlist of the account can take it out
                        CompositionLocalProvider(
                            LocalEditablePlaylist provides viewModel.editablePlaylistId,
                            LocalDownloadFolder provides DownloadFolder(viewModel.id, page.title, page.thumbnailUrl)
                        ) {
                            HomeListItem(
                                item = track,
                                onClick = {
                                    if (isSelecting) selectedIds = if (track.id in selectedIds) selectedIds - track.id else selectedIds + track.id
                                    else playList(songs, index, null)
                                },
                                number = if (page.kind == CollectionKind.Album) index + 1 else null,
                                // The songs of a chart have their place after the cover and a title that can take two lines
                                rank = track.rank ?: if (track.trend != null) index + 1 else null,
                                isChart = track.rank != null || track.trend != null,
                                isSelected = if (isSelecting) track.id in selectedIds else null
                            )
                        }
                    }

                    if (state.isLoadingMore) {
                        item(key = "more") {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(CollectionDimens.MorePadding),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(CollectionDimens.MoreSpinner))
                            }
                        }
                    }

                    // Shelves of YouTube Music under the songs, such as the releases for the listener
                    itemsIndexed(page.sections, key = { index, _ -> "section_$index" }) { _, section ->
                        HomeShelf(section = section, onItemClick = onItemClick)
                    }
                }
            }
        }

        // What can be done with the marked songs, over the mini player and the bar of the screens
        if (isSelecting && page != null) {
            val marked = songs.filter { it.id in selectedIds }
            val tracks = marked.mapNotNull { it.toPlayableTrack() }
            SelectionBar(
                hasSelection = marked.isNotEmpty(),
                onPlayNext = {
                    PlayerConnection.enqueueAll(context, tracks, afterCurrent = true)
                    leaveSelection()
                },
                onAddToQueue = {
                    PlayerConnection.enqueueAll(context, tracks, afterCurrent = false)
                    leaveSelection()
                },
                onSave = {
                    SaveToPlaylist.show(marked.map { it.id })
                    leaveSelection()
                },
                onDownload = {
                    Downloads.enqueueAll(context, tracks, DownloadFolder(viewModel.id, page.title, page.thumbnailUrl))
                    leaveSelection()
                },
                onRemoveFromPlaylist = if (viewModel.editablePlaylistId != null && page.isEditable) ({ isRemovingMarked = true }) else null,
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = LocalMiniPlayerInset.current)
            )

            // The yes asked before the marked songs leave the playlist, as the one of the playlist itself
            val playlistId = viewModel.editablePlaylistId
            if (isRemovingMarked && playlistId != null) {
                ConfirmDialog(
                    title = stringResource(R.string.playlist_selection_remove_title),
                    text = pluralStringResource(R.plurals.playlist_selection_remove_text, marked.size, marked.size, page.title),
                    confirmLabel = stringResource(R.string.download_remove_confirm),
                    cancelLabel = stringResource(R.string.playlist_cancel),
                    onConfirm = {
                        isRemovingMarked = false
                        leaveSelection()
                        // The songs leave the list at once and the answer of each one comes later, a refusal brings the list back
                        marked.forEach { PlaylistChanges.emit(PlaylistEvent.SongRemoved(playlistId, it.id, it.setVideoId)) }
                        PlaylistActions.scope.launch {
                            val repository = PlaylistRepository(context)
                            val results = marked.map { repository.removeEntry(playlistId, it.id, it.setVideoId) }
                            val isDone = results.all { it.isSuccess }
                            Toasts.show(resources.getString(if (isDone) R.string.playlist_song_removed else R.string.playlist_action_error), isError = !isDone)
                            if (!isDone) PlaylistChanges.notifyChanged()
                        }
                    },
                    onDismiss = { isRemovingMarked = false }
                )
            }
        }
    }
}

// Arrow to go back, and the name of the shelf that led here when there is one
@Composable
private fun CollectionTopBar(
    pageTitle: String,
    onBack: () -> Unit
) {
    val dimens = WavvyTheme.dimens

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))
            .padding(horizontal = dimens.spaceSmall)
    ) {
        IconButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterStart)) {
            Icon(
                imageVector = WavvyIcons.Back,
                contentDescription = stringResource(R.string.cd_back),
                tint = MaterialTheme.colorScheme.onBackground
            )
        }

        // The name of the shelf that led here stands next to the arrow
        if (pageTitle.isNotEmpty()) {
            Text(
                text = pageTitle,
                style = MaterialTheme.typography.titleLarge.merge(CollectionType.Owner),
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = CollectionDimens.TopTitleStart)
            )
        }
    }
}

// Cover, title and the round buttons, play in the middle and the menu at the end, an album with its save and its comments as YouTube Music has them
@Composable
private fun CollectionHeader(
    page: CollectionPage,
    showTitle: Boolean,
    isEnabled: Boolean,
    isOwned: Boolean,
    onShuffle: () -> Unit,
    onPlay: () -> Unit,
    onMenu: () -> Unit,
    onEdit: () -> Unit,
    onShare: () -> Unit,
    isDownloaded: Boolean,
    downloadProgress: Float?,
    onDownload: () -> Unit,
    isAlbum: Boolean,
    onToggleLibrary: () -> Unit,
    onComments: () -> Unit
) {
    val dimens = WavvyTheme.dimens
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    val alignment = if (isLandscape) Alignment.Start else Alignment.CenterHorizontally
    val textAlign = if (isLandscape) TextAlign.Start else TextAlign.Center

    // The cover, big and in the middle standing up, small and at the end lying down
    val cover: @Composable (Modifier) -> Unit = { modifier ->
        AsyncImage(
            model = page.thumbnailUrl?.resize(HomeDimens.CoverRequestSize, HomeDimens.CoverRequestSize),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier
                .clip(RoundedCornerShape(CollectionDimens.CoverCorner))
                .background(MaterialTheme.colorScheme.surfaceContainer)
        )
    }

    // The title, who made it, the numbers, the description and the buttons
    val info: @Composable ColumnScope.() -> Unit = {
        if (showTitle) {
            Text(
                text = page.title,
                style = MaterialTheme.typography.headlineSmall.merge(CollectionType.Title),
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = textAlign,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }

        // By who made it with their photo, then the kind and the year of an album with the number of songs and how long they take, the same in an album and in a playlist
        page.owner?.let { owner ->
            Spacer(modifier = Modifier.height(CollectionDimens.TextGap))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.collection_by),
                    style = MaterialTheme.typography.bodyMedium.merge(CollectionType.Detail),
                    color = MaterialTheme.colorScheme.secondary
                )
                Spacer(modifier = Modifier.size(CollectionDimens.ByGap))
                OwnerPhotos(page = page)
                Text(
                    text = owner,
                    style = MaterialTheme.typography.bodyMedium.merge(CollectionType.By),
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
            }
        }

        detailLineOf(page)?.let {
            Spacer(modifier = Modifier.height(CollectionDimens.TextGap))
            Text(
                text = it,
                style = MaterialTheme.typography.bodyMedium.merge(CollectionType.Detail),
                color = MaterialTheme.colorScheme.secondary,
                textAlign = textAlign
            )
        }

        // The description, the same in an album and in a playlist
        page.description?.takeIf { it.isNotBlank() }?.let { CollectionDescription(text = it, alignment = alignment, textAlign = textAlign) }

        Spacer(modifier = Modifier.height(CollectionDimens.HeaderGap))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(CollectionDimens.ActionGap)
        ) {
            // The playlist of the account has the download, which is only in its place for now, and the pencil, and the shuffle goes to the menu
            if (isOwned) {
                ActionButton(icon = if (isDownloaded) WavvyIcons.Check else WavvyIcons.Download, description = stringResource(R.string.playlist_download), isEnabled = true, onClick = onDownload, progress = downloadProgress)
                ActionButton(icon = WavvyIcons.Edit, description = stringResource(R.string.playlist_edit), isEnabled = true, onClick = onEdit)
            } else {
                // An album and a playlist have the download and the save to the library, the shuffle is in the menu
                ActionButton(icon = if (isDownloaded) WavvyIcons.Check else WavvyIcons.Download, description = stringResource(R.string.playlist_download), isEnabled = isEnabled, onClick = onDownload, progress = downloadProgress)
                ActionButton(
                    icon = if (page.isInLibrary) WavvyIcons.BookmarkFilled else WavvyIcons.Bookmark,
                    description = stringResource(if (page.isInLibrary) R.string.collection_library_remove else R.string.collection_library_save),
                    isEnabled = page.libraryTarget != null,
                    onClick = onToggleLibrary
                )
            }

            // The play button is the one that stands out, light on the dark theme and dark on the light one
            FilledIconButton(
                onClick = onPlay,
                enabled = isEnabled,
                modifier = Modifier.size(CollectionDimens.PlaySize),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.onBackground,
                    contentColor = MaterialTheme.colorScheme.background
                )
            ) {
                Icon(
                    imageVector = WavvyIcons.PlayArrow,
                    contentDescription = stringResource(R.string.home_play_all),
                    modifier = Modifier.size(CollectionDimens.PlayIcon)
                )
            }

            // The share, and in an album the comments in its place
            if (!isAlbum) ActionButton(icon = WavvyIcons.Share, description = stringResource(R.string.player_share), isEnabled = true, onClick = onShare)
            if (isAlbum) ActionButton(icon = WavvyIcons.Comment, description = stringResource(R.string.comments_title), isEnabled = true, onClick = onComments)
            ActionButton(icon = WavvyIcons.MoreVertical, description = stringResource(R.string.cd_more), isEnabled = true, onClick = onMenu)
        }

        Spacer(modifier = Modifier.height(CollectionDimens.ActionsBottom))
    }

    if (isLandscape) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = dimens.screenPadding)
        ) {
            Column(modifier = Modifier.weight(1f), horizontalAlignment = alignment, content = info)
            Spacer(modifier = Modifier.width(CollectionDimens.LandscapeGap))
            cover(Modifier.size(CollectionDimens.LandscapeCover))
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = dimens.screenPadding),
            horizontalAlignment = alignment
        ) {
            BoxWithConstraints(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                cover(Modifier.size(minOf(maxWidth * CollectionDimens.CoverWidthFraction, CollectionDimens.CoverMaxSize)))
            }

            Spacer(modifier = Modifier.height(CollectionDimens.HeaderGap))
            info()
        }
    }
}

// The photos of who made the page, the ones of the artists of an album stack each over the half of the one before, as the logo of a car brand does
@Composable
private fun OwnerPhotos(page: CollectionPage) {
    val context = LocalContext.current
    val ids = page.owners.map { it.second }.take(CollectionDimens.StackMax)

    // The first photo comes with the page, the others are asked on the pages of their artists, a few at a time and kept for the next time
    val others by produceState(emptyList<String?>(), page.owners) {
        val repository = ArtistRepository(context)
        value = ids.drop(1).map { id -> async { repository.summary(id).getOrNull()?.photoUrl } }.awaitAll()
    }
    val photos = (listOf(page.ownerPhotoUrl) + others).filterNotNull().take(CollectionDimens.StackMax)
    if (photos.isEmpty()) return

    val size = CollectionDimens.TopAvatar
    val ring = MaterialTheme.colorScheme.background
    Box(modifier = Modifier.width(size + (size / 2) * (photos.size - 1)).height(size)) {
        photos.forEachIndexed { index, url ->
            AsyncImage(
                model = url,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .offset(x = (size / 2) * index)
                    .zIndex(index.toFloat())
                    .size(size)
                    .border(CollectionDimens.StackRing, ring, CircleShape)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainer)
            )
        }
    }
    Spacer(modifier = Modifier.size(CollectionDimens.TopAvatarGap))
}

// The line under who made the page, the kind and the year of an album with the number of songs and how long they take
private fun detailLineOf(page: CollectionPage): String? =
    listOfNotNull(page.subtitle?.takeIf { page.kind == CollectionKind.Album }, page.details).joinToString(DetailSeparator).ifEmpty { null }

// What YouTube Music puts between the parts of a line
private const val DetailSeparator = " • "

// The row at the start of a playlist of the account that gives it a song, a square with a plus and the words
@Composable
private fun AddSongRow(onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .height(HomeDimens.ListHeight)
            .clickable(onClick = onClick)
            .padding(horizontal = HomeDimens.ListPadding + HomeDimens.ListCoverPadding)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(HomeDimens.ListCover)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
        ) {
            Icon(imageVector = WavvyIcons.Add, contentDescription = null, tint = MaterialTheme.colorScheme.onBackground)
        }
        Text(
            text = stringResource(R.string.playlist_add_song),
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(start = HomeDimens.ListTextPadding * 2)
        )
    }
}

// Small round button next to the play button
@Composable
private fun ActionButton(
    icon: ImageVector,
    description: String,
    isEnabled: Boolean,
    onClick: () -> Unit,
    // How much of what the button does is done, shown in place of the icon while it goes
    progress: Float? = null
) {
    FilledIconButton(
        onClick = onClick,
        enabled = isEnabled,
        modifier = Modifier.size(CollectionDimens.ActionSize),
        colors = IconButtonDefaults.filledIconButtonColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = MaterialTheme.colorScheme.onBackground
        )
    ) {
        if (progress != null) {
            CircularProgressIndicator(progress = { progress }, modifier = Modifier.size(CollectionDimens.ActionIcon), strokeWidth = CollectionDimens.ProgressStroke)
        } else {
            Icon(imageVector = icon, contentDescription = description, modifier = Modifier.size(CollectionDimens.ActionIcon))
        }
    }
}

// The description under the numbers, three lines and the more at its end when there is more of it, a tap opens all of it
@Composable
private fun CollectionDescription(text: String, alignment: Alignment.Horizontal, textAlign: TextAlign) {
    var isOpen by remember(text) { mutableStateOf(false) }
    var isCut by remember(text) { mutableStateOf(false) }

    Column(
        horizontalAlignment = alignment,
        modifier = Modifier
            .padding(top = CollectionDimens.HeaderGap)
            .widthIn(max = CollectionDimens.CoverMaxSize * 2)
            .then(if (isCut || isOpen) Modifier.clickable { isOpen = !isOpen } else Modifier)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium.merge(CollectionType.Detail),
            color = MaterialTheme.colorScheme.secondary,
            textAlign = textAlign,
            maxLines = if (isOpen) Int.MAX_VALUE else CollectionDimens.DescriptionLines,
            overflow = TextOverflow.Ellipsis,
            onTextLayout = { if (!isOpen) isCut = it.hasVisualOverflow }
        )
        if (isCut || isOpen) {
            Text(
                text = stringResource(if (isOpen) R.string.profile_about_less else R.string.profile_about_more),
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = CollectionDimens.TextGap)
            )
        }
    }
}

// Shares the link of the page through the apps of the device
internal fun shareLink(context: Context, url: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, url)
    }
    context.startActivity(Intent.createChooser(intent, context.getString(R.string.player_share_via)))
}

// Placeholder of the page while it loads, the cover, the title, who made it with their photo, the numbers, the five round buttons and the rows of songs, as the page is drawn
// Lying down the texts and the buttons are at the start and the small cover at the end, as the page is
@Composable
private fun CollectionSkeleton() {
    val dimens = WavvyTheme.dimens
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    val alignment = if (isLandscape) Alignment.Start else Alignment.CenterHorizontally
    val shape = MaterialTheme.shapes.extraSmall

    // The title, who made it, the numbers and the round buttons, the play button bigger in the middle
    val info: @Composable ColumnScope.() -> Unit = {
        Box(modifier = Modifier.fillMaxWidth(CollectionDimens.SkeletonTitleFraction).height(CollectionDimens.SkeletonTitle).skeleton(shape))
        Spacer(modifier = Modifier.height(CollectionDimens.TextGap))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.width(CollectionDimens.SkeletonBy).height(CollectionDimens.SkeletonLine).skeleton(shape))
            Spacer(modifier = Modifier.size(CollectionDimens.ByGap))
            Box(modifier = Modifier.size(CollectionDimens.TopAvatar).skeleton(CircleShape))
            Spacer(modifier = Modifier.size(CollectionDimens.TopAvatarGap))
            Box(modifier = Modifier.width(CollectionDimens.SkeletonOwner).height(CollectionDimens.SkeletonLine).skeleton(shape))
        }
        Spacer(modifier = Modifier.height(CollectionDimens.TextGap))
        Box(modifier = Modifier.fillMaxWidth(CollectionDimens.SkeletonDetailsFraction).height(CollectionDimens.SkeletonLine).skeleton(shape))

        Spacer(modifier = Modifier.height(CollectionDimens.HeaderGap))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(CollectionDimens.ActionGap, if (isLandscape) Alignment.Start else Alignment.CenterHorizontally),
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(modifier = Modifier.size(CollectionDimens.ActionSize).skeleton(CircleShape))
            Box(modifier = Modifier.size(CollectionDimens.ActionSize).skeleton(CircleShape))
            Box(modifier = Modifier.size(CollectionDimens.PlaySize).skeleton(CircleShape))
            Box(modifier = Modifier.size(CollectionDimens.ActionSize).skeleton(CircleShape))
            Box(modifier = Modifier.size(CollectionDimens.ActionSize).skeleton(CircleShape))
        }
    }

    Column {
        SkeletonHost(modifier = Modifier.fillMaxWidth().padding(horizontal = dimens.screenPadding)) {
            if (isLandscape) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.weight(1f), horizontalAlignment = alignment, content = info)
                    Spacer(modifier = Modifier.width(CollectionDimens.LandscapeGap))
                    Box(modifier = Modifier.size(CollectionDimens.LandscapeCover).skeleton(RoundedCornerShape(CollectionDimens.CoverCorner)))
                }
            } else {
                BoxWithConstraints(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Box(
                        modifier = Modifier
                            .size(minOf(maxWidth * CollectionDimens.CoverWidthFraction, CollectionDimens.CoverMaxSize))
                            .skeleton(RoundedCornerShape(CollectionDimens.CoverCorner))
                    )
                }

                Spacer(modifier = Modifier.height(CollectionDimens.HeaderGap))
                Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = alignment, content = info)
            }
        }

        Spacer(modifier = Modifier.height(CollectionDimens.ActionsBottom))
        SearchSkeleton()
    }
}

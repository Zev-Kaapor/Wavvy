package com.wavvy.app.features.podcast.ui

// Android context and sharing
import android.content.Context
import android.content.Intent
// Compose layouts and foundations
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
// Material 3 components
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
// Compose state and runtime
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
// UI styling and utilities
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
// Images and lifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
// Coroutines
import kotlinx.coroutines.launch
// Project resources
import com.wavvy.app.features.library.ui.ChipSlot
import com.wavvy.app.features.library.ui.FilterChips
import kotlinx.coroutines.flow.distinctUntilChanged
import android.content.res.Configuration
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.ui.platform.LocalConfiguration
import com.wavvy.app.features.podcast.data.ScheduleTime
import com.wavvy.app.core.designsystem.components.ConfirmDialog
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.flow.map
import com.wavvy.app.features.collection.ui.CollectionDimens
import com.wavvy.app.features.collection.ui.CollectionType
import com.wavvy.app.R
import com.wavvy.app.features.podcast.data.EpisodesLater
import com.wavvy.app.features.playlist.data.PlaylistActions
import com.wavvy.app.core.designsystem.components.SkeletonHost
import com.wavvy.app.core.designsystem.components.OverlaySheet
import com.wavvy.app.core.designsystem.components.skeleton
import com.wavvy.app.core.designsystem.icons.WavvyIcons
import com.wavvy.app.core.designsystem.theme.WavvyMotion
import com.wavvy.app.core.designsystem.theme.WavvyTheme
import com.wavvy.app.core.innertube.MusicOrigin
import com.wavvy.app.core.navigation.ItemNavigator
import com.wavvy.app.core.download.DownloadFolder
import com.wavvy.app.core.download.DownloadPhase
import com.wavvy.app.core.download.Downloads
import com.wavvy.app.features.home.ui.toPlayableTrack
import com.wavvy.app.features.home.data.HomeItem
import com.wavvy.app.features.home.ui.components.HomeMessage
import com.wavvy.app.features.home.ui.rememberItemPlayer
import com.wavvy.app.features.menu.ItemMenu
import com.wavvy.app.features.player.ui.LocalMiniPlayerInset
import com.wavvy.app.features.playlist.ui.SortChoice
import com.wavvy.app.features.playlist.ui.SortChoiceSheet
import com.wavvy.app.features.podcast.data.PodcastChip
import com.wavvy.app.features.podcast.data.PodcastPage
import com.wavvy.app.features.podcast.data.PodcastSort

// How many episodes a search loads at most to look through
private const val SearchLoadLimit = 400

// How near the end of the list the next episodes are asked
private const val LoadMoreDistance = 4

// Prefix of the id of the page of a podcast, the playlist of its episodes has the same id without it
private const val ShowPagePrefix = "MPSP"

// The page of a podcast, the cover with who makes it and how it is told, the buttons, the filters that stay on top and the episodes
@Composable
fun PodcastScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PodcastViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()
    val onItemClick = rememberItemPlayer()
    val listState = rememberLazyListState()
    val context = LocalContext.current
    val page = state.page

    // The episodes kept for later in the account, read when the page opens so the button of each episode shows whether it is there
    val laterIds by EpisodesLater.kept.collectAsState()
    LaunchedEffect(Unit) { EpisodesLater.refresh(context) }

    val pageColor = MaterialTheme.colorScheme.background

    // The filters take the color of the page once they stay under the bar, while the cover shows they are clear over its color
    val isStuck by remember { derivedStateOf { listState.firstVisibleItemIndex > 0 } }

    // How much of the podcast is downloaded, the button and the question that takes the downloads out tell it and start it, only the episodes that are out have a file
    val downloads by Downloads.items.collectAsState()
    val folder = page?.let { DownloadFolder(viewModel.id, it.title, it.coverUrl) }
    val playable = remember(state.episodes) { state.episodes.filter { it.durationText != null } }
    val doneCount = playable.count { downloads[it.id]?.phase == DownloadPhase.Completed }
    val runningCount = playable.count { downloads[it.id]?.phase == DownloadPhase.Queued || downloads[it.id]?.phase == DownloadPhase.Downloading }
    val isAllDone = playable.isNotEmpty() && doneCount == playable.size
    val downloadProgress = if (runningCount > 0 && playable.isNotEmpty()) doneCount.toFloat() / playable.size else null
    var isRemovingDownloads by remember { mutableStateOf(false) }
    if (isRemovingDownloads && page != null) {
        ConfirmDialog(
            title = stringResource(R.string.download_folder_remove_title),
            text = stringResource(R.string.download_podcast_remove_text, page.title),
            confirmLabel = stringResource(R.string.download_remove_confirm),
            cancelLabel = stringResource(R.string.playlist_cancel),
            onConfirm = {
                isRemovingDownloads = false
                Downloads.removeAll(context, playable.map { it.id })
            },
            onDismiss = { isRemovingDownloads = false }
        )
    }

    // While something is typed only the episodes that have it in the title or the description show
    val searched = state.isSearching && state.query.isNotBlank()
    val shown = remember(state.episodes, state.query, state.isSearching) {
        if (searched) state.episodes.filter { it.title.contains(state.query, ignoreCase = true) || it.description.orEmpty().contains(state.query, ignoreCase = true) } else state.episodes
    }

    // The name of the podcast takes the place of who makes it on the bar once the cover is mostly gone
    val threshold = with(LocalDensity.current) { PodcastDimens.BarScrollThreshold.toPx() }
    val isScrolled by remember { derivedStateOf { listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > threshold } }
    val barAlpha by animateFloatAsState(
        targetValue = if (isScrolled) 1f else 0f,
        animationSpec = tween(PodcastDimens.BarFadeMillis),
        label = "PodcastBar"
    )

    // The next episodes are asked when the end of the list comes near
    LaunchedEffect(listState, state.episodes.size) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0 }
            .collect { last -> if (last >= listState.layoutInfo.totalItemsCount - LoadMoreDistance) viewModel.loadMore() }
    }

    // A search looks at the episodes that are loaded, so the next ones are asked while it goes, up to a limit
    LaunchedEffect(searched, state.continuation, state.isLoadingMore, state.episodes.size) {
        if (searched && state.continuation != null && !state.isLoadingMore && state.episodes.size < SearchLoadLimit) viewModel.loadMore()
    }

    val topInset = WindowInsets.safeDrawing.only(WindowInsetsSides.Top).asPaddingValues().calculateTopPadding()

    // Solid, so the screen under it does not show through while the page slides
    Box(modifier = modifier.fillMaxSize().background(pageColor)) {
        CoverBackdrop(coverUrl = page?.coverUrl, listState = listState, modifier = Modifier.align(Alignment.TopStart))

        when {
            state.status == PodcastStatus.Loading -> PodcastSkeleton(modifier = Modifier.padding(top = topInset + PodcastDimens.BarHeight))

            state.status == PodcastStatus.Error || page == null -> HomeMessage(
                text = stringResource(R.string.podcast_error),
                actionLabel = stringResource(R.string.home_retry),
                onAction = viewModel::load
            )

            else -> LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                // The list starts under the bar, which floats over it, and ends over the mini player
                contentPadding = PaddingValues(
                    top = topInset + PodcastDimens.BarHeight,
                    bottom = LocalMiniPlayerInset.current + if (LocalMiniPlayerInset.current > 0.dp) PodcastDimens.MiniPlayerClearance else 0.dp
                )
            ) {
                item(key = "details") {
                    PodcastDetails(
                        page = page,
                        shareUrl = "$MusicOrigin/playlist?list=${viewModel.id.removePrefix(ShowPagePrefix)}",
                        context = context,
                        isDownloaded = isAllDone,
                        downloadProgress = downloadProgress,
                        hasEpisodes = playable.isNotEmpty(),
                        onDownload = {
                            if (isAllDone || runningCount > 0) isRemovingDownloads = true else Downloads.enqueueAll(context, playable.mapNotNull { it.toPlayableTrack() }, folder)
                        },
                        newest = state.episodes.firstOrNull { it.durationText != null },
                        onPlay = onItemClick,
                        onMoreClick = {
                            OverlaySheet.show {
                                PodcastMenu(
                                    id = viewModel.id,
                                    title = page.title,
                                    author = page.author,
                                    coverUrl = page.coverUrl,
                                    onSearch = viewModel::startSearch,
                                    onDismiss = OverlaySheet::dismiss
                                )
                            }
                        }
                    )
                }

                // The filters stay under the bar while the episodes slide
                stickyHeader(key = "chips") {
                    if (state.isSearching) {
                        PodcastSearchField(query = state.query, onQueryChange = viewModel::setQuery, onClose = viewModel::stopSearch)
                    } else {
                        PodcastChips(
                            chips = page.chips,
                            isStuck = isStuck,
                            sortTitle = state.sort?.title,
                            filter = state.filter,
                            onSortClick = {
                                OverlaySheet.show {
                                    SortSheet(
                                        sorts = page.chips.firstOrNull()?.sorts.orEmpty(),
                                        selected = state.sort,
                                        onSelect = viewModel::selectSort,
                                        onDismiss = OverlaySheet::dismiss
                                    )
                                }
                            },
                            onFilterClick = viewModel::selectFilter
                        )
                    }
                }

                when {
                    // The room of the episodes is kept while they are asked again, so the page does not jump
                    state.isReloading -> items(PodcastDimens.SkeletonRows) { EpisodeSkeleton() }

                    shown.isEmpty() -> item(key = "empty") {
                        Text(
                            text = if (searched) stringResource(R.string.podcast_no_results) else state.message ?: stringResource(R.string.podcast_no_episodes),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.secondary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().padding(PodcastDimens.Side)
                        )
                    }

                    else -> itemsIndexed(shown, key = { _, episode -> episode.id }) { _, episode ->
                        EpisodeRow(
                            episode = episode,
                            folder = DownloadFolder(viewModel.id, page.title, page.coverUrl),
                            isKept = laterIds?.contains(episode.id) == true,
                            onKeep = if (laterIds != null) { { PlaylistActions.scope.launch { EpisodesLater.toggle(context, episode.id) } } } else null,
                            onClick = { ItemNavigator.openEpisode(episode.id) },
                            onPlay = { onItemClick(episode) }
                        )
                    }
                }

                if (state.isLoadingMore) {
                    item(key = "more") {
                        Box(modifier = Modifier.fillMaxWidth().padding(PodcastDimens.Side), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                }
            }
        }

        PodcastBar(
            title = page?.title.orEmpty(),
            scrolledAlpha = barAlpha,
            onBack = onBack
        )
    }
}

// The bar on top, the arrow and, once the cover is mostly gone, the name of the podcast
@Composable
private fun PodcastBar(title: String, scrolledAlpha: Float, onBack: () -> Unit) {
    val dimens = WavvyTheme.dimens

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background.copy(alpha = scrolledAlpha))
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))
            .height(PodcastDimens.BarHeight)
            .padding(horizontal = dimens.spaceSmall)
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = WavvyIcons.Back,
                contentDescription = stringResource(R.string.cd_back),
                tint = MaterialTheme.colorScheme.onBackground
            )
        }

        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge.merge(PodcastType.BarTitle),
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .padding(start = dimens.spaceSmall)
                .graphicsLayer { alpha = scrolledAlpha }
        )
    }
}

// The cover, the name of the podcast, who makes it, how it is told and the buttons, all centered, as the page of an album has them
@Composable
private fun PodcastDetails(
    page: PodcastPage,
    shareUrl: String,
    context: Context,
    isDownloaded: Boolean,
    downloadProgress: Float?,
    hasEpisodes: Boolean,
    onDownload: () -> Unit,
    newest: HomeItem?,
    onPlay: (HomeItem) -> Unit,
    onMoreClick: () -> Unit
) {
    var isExpanded by rememberSaveable { mutableStateOf(false) }
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    val alignment = if (isLandscape) Alignment.Start else Alignment.CenterHorizontally
    val textAlign = if (isLandscape) TextAlign.Start else TextAlign.Center

    // The cover, big and in the middle standing up, small and at the end lying down
    val cover: @Composable (Modifier) -> Unit = { modifier ->
        AsyncImage(
            model = page.coverUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier
                .clip(RoundedCornerShape(PodcastDimens.CoverCorner))
                .background(MaterialTheme.colorScheme.surfaceContainer)
        )
    }

    // The name, who makes it, how it is told and the buttons
    val info: @Composable ColumnScope.() -> Unit = {
        Text(
            text = page.title,
            style = MaterialTheme.typography.headlineMedium.merge(PodcastType.Title),
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = textAlign,
            modifier = Modifier.padding(top = PodcastDimens.TitleTop)
        )

        // By who makes it with their photo, the same line as the page of an album
        page.author?.let { author ->
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = CollectionDimens.TextGap)) {
                Text(
                    text = stringResource(R.string.collection_by),
                    style = MaterialTheme.typography.bodyMedium.merge(CollectionType.Detail),
                    color = MaterialTheme.colorScheme.secondary
                )
                Spacer(modifier = Modifier.size(CollectionDimens.ByGap))
                page.authorPhoto?.let { photo ->
                    AsyncImage(
                        model = photo,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(CollectionDimens.TopAvatar)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceContainer)
                    )
                    Spacer(modifier = Modifier.size(CollectionDimens.TopAvatarGap))
                }
                Text(
                    text = author,
                    style = MaterialTheme.typography.bodyMedium.merge(CollectionType.By),
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
            }
        }

        page.description?.let { description ->
            val more = stringResource(R.string.podcast_more)
            val less = stringResource(R.string.podcast_less)
            val isLong = description.length > PodcastDimens.DescriptionPreviewLetters

            Text(
                text = buildAnnotatedString {
                    if (isLong && !isExpanded) {
                        append(description.take(PodcastDimens.DescriptionPreviewLetters).replace('\n', ' ').trimEnd())
                        append("…")
                        withStyle(SpanStyle(color = MaterialTheme.colorScheme.onBackground)) { append(more) }
                    } else {
                        append(description)
                        if (isLong) withStyle(SpanStyle(color = MaterialTheme.colorScheme.onBackground)) { append(" $less") }
                    }
                },
                style = MaterialTheme.typography.bodyLarge.merge(PodcastType.Description),
                color = MaterialTheme.colorScheme.secondary,
                textAlign = textAlign,
                modifier = Modifier
                    .padding(top = PodcastDimens.DescriptionTop)
                    .clickable(enabled = isLong) { isExpanded = !isExpanded }
            )
        }

        // The buttons in the order of the page of an album, the download of the episodes, the save, the play that stands out, the share and the menu, the automatic downloads are in the menu
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(CollectionDimens.ActionGap),
            modifier = Modifier.padding(top = PodcastDimens.ActionsTop, bottom = PodcastDimens.ActionsBottom)
        ) {
            RoundAction(
                icon = if (isDownloaded) WavvyIcons.Check else WavvyIcons.Download,
                description = stringResource(R.string.playlist_download),
                isEnabled = hasEpisodes,
                progress = downloadProgress,
                onClick = onDownload
            )
            RoundAction(icon = WavvyIcons.Bookmark, description = page.saveLabel ?: stringResource(R.string.podcast_save)) { }

            // The play button is the one that stands out, light on the dark theme and dark on the light one, it plays the newest episode
            FilledIconButton(
                onClick = { newest?.let(onPlay) },
                enabled = newest != null,
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

            RoundAction(icon = WavvyIcons.Share, description = stringResource(R.string.player_share)) { shareLink(context, shareUrl) }
            RoundAction(icon = WavvyIcons.MoreVertical, description = stringResource(R.string.cd_more), onClick = onMoreClick)
        }
    }

    val room = Modifier
        .fillMaxWidth()
        .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
        .padding(horizontal = PodcastDimens.Side)

    if (isLandscape) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = room) {
            Column(modifier = Modifier.weight(1f), horizontalAlignment = alignment, content = info)
            Spacer(modifier = Modifier.width(CollectionDimens.LandscapeGap))
            cover(Modifier.size(CollectionDimens.LandscapeCover))
        }
    } else {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = room) {
            cover(Modifier.size(PodcastDimens.CoverSize))
            info()
        }
    }
}

// A round button with an icon, as the ones of the page of an album
@Composable
internal fun RoundAction(icon: ImageVector, description: String, isEnabled: Boolean = true, progress: Float? = null, onClick: () -> Unit) {
    FilledIconButton(
        onClick = onClick,
        enabled = isEnabled,
        modifier = Modifier.size(CollectionDimens.ActionSize),
        colors = IconButtonDefaults.filledIconButtonColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = MaterialTheme.colorScheme.onBackground
        )
    ) {
        // How much of what the button does is done, shown in place of the icon while it goes
        if (progress != null) {
            CircularProgressIndicator(progress = { progress }, modifier = Modifier.size(CollectionDimens.ActionIcon), strokeWidth = CollectionDimens.ProgressStroke)
        } else {
            Icon(imageVector = icon, contentDescription = description, modifier = Modifier.size(CollectionDimens.ActionIcon))
        }
    }
}

// The row of filters, the first one orders the episodes and has an arrow, the others show only some of them
@Composable
private fun PodcastChips(
    chips: List<PodcastChip>,
    sortTitle: String?,
    filter: Int?,
    isStuck: Boolean,
    onSortClick: () -> Unit,
    onFilterClick: (Int) -> Unit
) {
    FilterChips(
        options = (1 until chips.size).toList(),
        selected = filter,
        id = { it },
        gap = PodcastDimens.ChipGap,
        onClear = { filter?.let(onFilterClick) },
        clearHeight = PodcastDimens.ChipHeight,
        contentPadding = PaddingValues(horizontal = PodcastDimens.Side, vertical = PodcastDimens.ChipVertical),
        modifier = Modifier
            .fillMaxWidth()
            .background(if (isStuck) MaterialTheme.colorScheme.background else Color.Transparent)
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)),
        // The first one orders the episodes and stays where it is
        leading = {
            ChipSlot(gap = PodcastDimens.ChipGap) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .height(PodcastDimens.ChipHeight)
                        .clip(RoundedCornerShape(PodcastDimens.ChipCorner))
                        .background(WavvyTheme.colors.chip)
                        .clickable(onClick = onSortClick)
                        .padding(horizontal = PodcastDimens.ChipPaddingX)
                ) {
                    Text(
                        text = sortTitle ?: chips.firstOrNull()?.title.orEmpty(),
                        style = MaterialTheme.typography.bodyMedium.merge(PodcastType.Chip),
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1
                    )
                    Icon(
                        imageVector = WavvyIcons.ChevronDown,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.size(PodcastDimens.ChipArrow)
                    )
                }
            }
        }
    ) { index, isOn ->
        Text(
            text = chips[index].title,
            style = MaterialTheme.typography.bodyMedium.merge(PodcastType.Chip),
            color = if (isOn) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            modifier = Modifier
                .height(PodcastDimens.ChipHeight)
                .clip(RoundedCornerShape(PodcastDimens.ChipCorner))
                .background(if (isOn) MaterialTheme.colorScheme.onBackground else WavvyTheme.colors.chip)
                .clickable { onFilterClick(index) }
                .padding(horizontal = PodcastDimens.ChipPaddingX)
                .wrapContentHeight(Alignment.CenterVertically)
        )
    }
}

// The ways to order the episodes, the one in use is marked
@Composable
private fun SortSheet(sorts: List<PodcastSort>, selected: PodcastSort?, onSelect: (PodcastSort) -> Unit, onDismiss: () -> Unit) {
    SortChoiceSheet(
        title = stringResource(R.string.collection_sort_by),
        choices = sorts.map { sort -> SortChoice(sort.title, sort.token == selected?.token) { onSelect(sort) } },
        onDismiss = onDismiss
    )
}

// An episode, a small picture with the title and the views and the age, the description and the button that plays it with how much is left
@Composable
internal fun EpisodeRow(episode: HomeItem, folder: DownloadFolder, isKept: Boolean, onKeep: (() -> Unit)?, onClick: () -> Unit, onPlay: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = { ItemMenu.show(episode) })
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
            .padding(top = PodcastDimens.RowTop)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = PodcastDimens.Side)
        ) {
            AsyncImage(
                model = episode.thumbnailUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .width(PodcastDimens.ThumbnailWidth)
                    .aspectRatio(EpisodePictureRatio)
                    .clip(RoundedCornerShape(PodcastDimens.ThumbnailCorner))
                    .background(MaterialTheme.colorScheme.surfaceContainer)
            )

            Column(modifier = Modifier.weight(1f).padding(horizontal = PodcastDimens.Side)) {
                Text(
                    text = episode.title,
                    style = MaterialTheme.typography.bodyLarge.merge(PodcastType.EpisodeTitle),
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = PodcastDimens.TitleLines,
                    overflow = TextOverflow.Ellipsis
                )
                episode.lineText?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium.merge(PodcastType.EpisodeLine),
                        color = MaterialTheme.colorScheme.secondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            IconButton(onClick = { ItemMenu.show(episode) }) {
                Icon(
                    imageVector = WavvyIcons.MoreVertical,
                    contentDescription = stringResource(R.string.cd_more),
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
        }

        // A description that is only a link, as the ones that repeat in every episode, takes no room
        episode.description?.let(::descriptionPreview)?.takeIf { it.isNotEmpty() }?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodyMedium.merge(PodcastType.EpisodeDescription),
                color = MaterialTheme.colorScheme.secondary,
                maxLines = PodcastDimens.DescriptionLines,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = PodcastDimens.Side, vertical = PodcastDimens.RowGap)
            )
        }

        // Download and keep it for later in the account, which only stands in its place while signed out, or the time that is left for an episode that is not out, then how much was heard and the button that plays it with the time that is left
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = PodcastDimens.EpisodeActionsStart, end = PodcastDimens.Side)
                .padding(bottom = PodcastDimens.RowGap)
        ) {
            val download by remember(episode.id) { Downloads.items.map { it[episode.id] }.distinctUntilChanged() }.collectAsState(initial = Downloads.items.value[episode.id])
            val context = LocalContext.current

            // An episode that is not out has the time that is left in place of the two buttons, which come with a fade when the moment comes
            val scheduled = remember(episode.id, episode.lineText) { if (episode.durationText == null) episode.lineText?.let { ScheduleTime.read(it) } else null }
            val remaining = rememberRemaining(scheduled?.at)
            val isWaiting = scheduled != null && (remaining == null || remaining > 0)

            AnimatedContent(
                targetState = isWaiting,
                transitionSpec = { fadeIn(tween(WavvyMotion.ExpandMillis)) togetherWith fadeOut(tween(WavvyMotion.ExpandMillis)) },
                contentAlignment = Alignment.CenterStart,
                label = "EpisodeRowActions"
            ) { waiting ->
                if (waiting) {
                    CountdownPill(
                        remaining = remaining,
                        height = PodcastDimens.PillHeight,
                        container = WavvyTheme.colors.chip,
                        content = MaterialTheme.colorScheme.onBackground,
                        textStyle = PodcastType.Pill,
                        paddingX = PodcastDimens.PillPaddingX * 2,
                        modifier = Modifier.padding(start = PodcastDimens.CountdownStart)
                    )
                } else {
                    Row {
                        EpisodeAction(if (download?.phase == DownloadPhase.Completed) WavvyIcons.Check else WavvyIcons.Download, progress = download?.takeIf { it.phase == DownloadPhase.Downloading }?.percent?.div(PercentTotal)) {
                            val track = episode.toPlayableTrack() ?: return@EpisodeAction
                            if (download?.phase.let { it == null || it == DownloadPhase.Failed }) Downloads.enqueue(context, track, folder) else Downloads.remove(context, track.id)
                        }
                        EpisodeAction(if (isKept) WavvyIcons.CheckCircle else WavvyIcons.AddCircle, onClick = onKeep)
                    }
                }
            }
            Spacer(modifier = Modifier.weight(1f))

            val percent = episode.progressPercent ?: 0
            if (percent > 0) {
                Box(
                    modifier = Modifier
                        .width(PodcastDimens.ProgressWidth)
                        .height(PodcastDimens.ProgressHeight)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(percent / PercentTotal)
                            .height(PodcastDimens.ProgressHeight)
                            .background(WavvyTheme.colors.episodeProgress)
                    )
                }
                Spacer(modifier = Modifier.width(PodcastDimens.ProgressGap))
            }

            (episode.progressText ?: episode.durationText)?.let { remaining ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .height(PodcastDimens.PillHeight)
                        .clip(CircleShape)
                        .background(WavvyTheme.colors.chip)
                        .clickable(onClick = onPlay)
                        .padding(horizontal = PodcastDimens.PillPaddingX)
                ) {
                    Icon(
                        imageVector = WavvyIcons.PlayArrow,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.size(PodcastDimens.PillIcon)
                    )
                    Text(
                        text = remaining,
                        style = MaterialTheme.typography.bodyMedium.merge(PodcastType.Pill),
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1,
                        modifier = Modifier.padding(start = PodcastDimens.AuthorGap)
                    )
                }
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}

// A button of an episode, with the percent in place of its icon while it works, one without a function is only an icon in its place
@Composable
private fun EpisodeAction(icon: ImageVector, progress: Float? = null, onClick: (() -> Unit)? = null) {
    Box(
        modifier = Modifier.size(PodcastDimens.EpisodeAction).then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center
    ) {
        if (progress != null) {
            CircularProgressIndicator(progress = { progress }, modifier = Modifier.size(PodcastDimens.EpisodeActionIcon), strokeWidth = PodcastDimens.ProgressStroke)
        } else {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.size(PodcastDimens.EpisodeActionIcon)
            )
        }
    }
}

// The shape of the little picture of an episode
private const val EpisodePictureRatio = 16f / 9f

// The description of an episode in a single flow of text, without the lines that carry a link
private val Spaces = Regex("\\s+")

private fun descriptionPreview(description: String): String =
    description.lines().filterNot { "://" in it }.joinToString(" ").replace(Spaces, " ").trim()

// What the percent of how much was heard is counted against
private const val PercentTotal = 100f

// The room of the page while it is asked, the cover, the name, who makes it, the description and the buttons where the real ones will be, then the filters and the first episodes
@Composable
private fun PodcastSkeleton(modifier: Modifier = Modifier) {
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    val shape = MaterialTheme.shapes.extraSmall

    SkeletonHost(modifier = modifier.fillMaxSize()) {
        val info: @Composable ColumnScope.() -> Unit = {
            Box(modifier = Modifier.padding(top = PodcastDimens.TitleTop).fillMaxWidth(PodcastDimens.SkeletonNameFraction).height(PodcastDimens.SkeletonNameHeight).skeleton(shape))
            Box(modifier = Modifier.padding(top = CollectionDimens.TextGap * 2).fillMaxWidth(PodcastDimens.SkeletonByFraction).height(PodcastDimens.SkeletonLineHeight).skeleton(shape))
            Box(modifier = Modifier.padding(top = PodcastDimens.DescriptionTop).fillMaxWidth().height(PodcastDimens.SkeletonDescriptionHeight).skeleton(shape))
            Box(modifier = Modifier.padding(top = PodcastDimens.AuthorGap).fillMaxWidth(SkeletonLineFraction).height(PodcastDimens.SkeletonDescriptionHeight).skeleton(shape))
            Row(
                horizontalArrangement = Arrangement.spacedBy(CollectionDimens.ActionGap),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = PodcastDimens.ActionsTop, bottom = PodcastDimens.ActionsBottom)
            ) {
                repeat(PodcastDimens.SkeletonButtons) { index ->
                    val size = if (index == PodcastDimens.SkeletonButtons / 2) CollectionDimens.PlaySize else CollectionDimens.ActionSize
                    Box(modifier = Modifier.size(size).skeleton(CircleShape))
                }
            }
        }
        val room = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
            .padding(horizontal = PodcastDimens.Side)

        if (isLandscape) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = room) {
                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.Start, content = info)
                Spacer(modifier = Modifier.width(CollectionDimens.LandscapeGap))
                Box(modifier = Modifier.size(CollectionDimens.LandscapeCover).skeleton(RoundedCornerShape(PodcastDimens.CoverCorner)))
            }
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = room) {
                Box(modifier = Modifier.size(PodcastDimens.CoverSize).skeleton(RoundedCornerShape(PodcastDimens.CoverCorner)))
                info()
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(PodcastDimens.ChipGap),
            modifier = Modifier
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
                .padding(horizontal = PodcastDimens.Side, vertical = PodcastDimens.ChipVertical)
        ) {
            repeat(PodcastDimens.SkeletonChips) { Box(modifier = Modifier.size(PodcastDimens.SkeletonChipWidth, PodcastDimens.ChipHeight).skeleton(RoundedCornerShape(PodcastDimens.ChipCorner))) }
        }

        repeat(PodcastDimens.SkeletonRows) { EpisodeSkeletonRow() }
    }
}

// The room of an episode while the episodes are asked, the picture and the lines in the places of the real ones
@Composable
private fun EpisodeSkeleton() {
    SkeletonHost { EpisodeSkeletonRow() }
}

@Composable
private fun EpisodeSkeletonRow() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
            .padding(top = PodcastDimens.RowTop, start = PodcastDimens.Side, end = PodcastDimens.Side, bottom = PodcastDimens.RowGap)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .width(PodcastDimens.ThumbnailWidth)
                    .aspectRatio(EpisodePictureRatio)
                    .skeleton(RoundedCornerShape(PodcastDimens.ThumbnailCorner))
            )
            Column(modifier = Modifier.weight(1f).padding(start = PodcastDimens.Side)) {
                Box(modifier = Modifier.fillMaxWidth(SkeletonTitleFraction).height(PodcastDimens.SkeletonLineHeight).skeleton(MaterialTheme.shapes.extraSmall))
                Spacer(modifier = Modifier.height(PodcastDimens.AuthorGap))
                Box(modifier = Modifier.fillMaxWidth(SkeletonLineFraction).height(PodcastDimens.SkeletonLineHeight).skeleton(MaterialTheme.shapes.extraSmall))
            }
        }
        Spacer(modifier = Modifier.height(PodcastDimens.RowGap))
        Box(modifier = Modifier.fillMaxWidth().height(PodcastDimens.SkeletonDescriptionHeight).skeleton(MaterialTheme.shapes.extraSmall))
        Spacer(modifier = Modifier.height(PodcastDimens.AuthorGap))
        Box(modifier = Modifier.fillMaxWidth(SkeletonLineFraction).height(PodcastDimens.SkeletonDescriptionHeight).skeleton(MaterialTheme.shapes.extraSmall))
    }
}

// How much of the width the lines of the placeholder take
private const val SkeletonTitleFraction = 0.8f
private const val SkeletonLineFraction = 0.5f

// Opens the sharing sheet of the system with the link
private fun shareLink(context: Context, url: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, url)
    }
    context.startActivity(Intent.createChooser(intent, context.getString(R.string.player_share_via)))
}

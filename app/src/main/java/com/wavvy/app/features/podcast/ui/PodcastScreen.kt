package com.wavvy.app.features.podcast.ui

// Android context and sharing
import android.content.Context
import android.content.Intent
// Compose layouts and foundations
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
// Project resources
import com.wavvy.app.R
import com.wavvy.app.core.designsystem.components.SkeletonHost
import com.wavvy.app.core.designsystem.components.OverlaySheet
import com.wavvy.app.core.designsystem.components.LocalSheetClose
import com.wavvy.app.core.designsystem.components.WavvySheet
import com.wavvy.app.core.designsystem.components.skeleton
import com.wavvy.app.core.designsystem.icons.WavvyIcons
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
import com.wavvy.app.features.player.ui.components.MenuAction
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
    Box(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        when {
            state.status == PodcastStatus.Loading -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }

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
                        EpisodeRow(episode = episode, folder = DownloadFolder(viewModel.id, page.title, page.coverUrl), onClick = { ItemNavigator.openEpisode(episode.id) }, onPlay = { onItemClick(episode) })
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
            author = page?.author,
            authorPhoto = page?.authorPhoto,
            scrolledAlpha = barAlpha,
            onBack = onBack
        )
    }
}

// The bar on top, the arrow and who makes the podcast in the middle, which gives its place to the name of the podcast when the page scrolls
@Composable
private fun PodcastBar(title: String, author: String?, authorPhoto: String?, scrolledAlpha: Float, onBack: () -> Unit) {
    val dimens = WavvyTheme.dimens

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background.copy(alpha = scrolledAlpha))
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))
            .height(PodcastDimens.BarHeight)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.align(Alignment.CenterStart).padding(horizontal = dimens.spaceSmall)
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

        if (author != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .align(Alignment.Center)
                    .graphicsLayer { alpha = 1f - scrolledAlpha }
            ) {
                AsyncImage(
                    model = authorPhoto,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(PodcastDimens.AuthorPhoto)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceContainer)
                )
                Text(
                    text = author,
                    style = MaterialTheme.typography.bodyMedium.merge(PodcastType.Author),
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    modifier = Modifier.padding(start = PodcastDimens.AuthorGap)
                )
            }
        }
    }
}

// The cover, the name of the podcast, how it is told and the buttons, all centered
@Composable
private fun PodcastDetails(page: PodcastPage, shareUrl: String, context: Context, onMoreClick: () -> Unit) {
    var isExpanded by rememberSaveable { mutableStateOf(false) }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
            .padding(horizontal = PodcastDimens.Side)
    ) {
        AsyncImage(
            model = page.coverUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(PodcastDimens.CoverSize)
                .clip(RoundedCornerShape(PodcastDimens.CoverCorner))
                .background(MaterialTheme.colorScheme.surfaceContainer)
        )

        Text(
            text = page.title,
            style = MaterialTheme.typography.headlineMedium.merge(PodcastType.Title),
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = PodcastDimens.TitleTop)
        )

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
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(top = PodcastDimens.DescriptionTop)
                    .clickable(enabled = isLong) { isExpanded = !isExpanded }
            )
        }

        // Share, save and more, round, the one that saves is wider and light
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(PodcastDimens.ActionGap),
            modifier = Modifier.padding(top = PodcastDimens.ActionsTop, bottom = PodcastDimens.ActionsBottom)
        ) {
            RoundAction(icon = WavvyIcons.Share, description = stringResource(R.string.player_share)) { shareLink(context, shareUrl) }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier
                    .width(PodcastDimens.SaveWidth)
                    .height(PodcastDimens.ActionSize)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onBackground)
                    .clickable { }
            ) {
                Icon(
                    imageVector = WavvyIcons.Bookmark,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.background,
                    modifier = Modifier.size(PodcastDimens.ActionIcon)
                )
                Text(
                    text = page.saveLabel ?: stringResource(R.string.podcast_save),
                    style = MaterialTheme.typography.bodyLarge.merge(PodcastType.Action),
                    color = MaterialTheme.colorScheme.background,
                    maxLines = 1,
                    modifier = Modifier.padding(start = PodcastDimens.AuthorGap)
                )
            }

            RoundAction(icon = WavvyIcons.MoreVertical, description = stringResource(R.string.cd_more), onClick = onMoreClick)
        }
    }
}

// A round button with an icon
@Composable
private fun RoundAction(icon: ImageVector, description: String, onClick: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(PodcastDimens.ActionSize)
            .clip(CircleShape)
            .background(WavvyTheme.colors.chip)
            .clickable(onClick = onClick)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(PodcastDimens.ActionIcon)
        )
    }
}

// The row of filters, the first one orders the episodes and has an arrow, the others show only some of them
@Composable
private fun PodcastChips(
    chips: List<PodcastChip>,
    sortTitle: String?,
    filter: Int?,
    onSortClick: () -> Unit,
    onFilterClick: (Int) -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(PodcastDimens.ChipGap),
        contentPadding = PaddingValues(horizontal = PodcastDimens.Side, vertical = PodcastDimens.ChipVertical),
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
    ) {
        itemsIndexed(chips) { index, chip ->
            val isSort = index == 0
            val isOn = !isSort && filter == index

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .height(PodcastDimens.ChipHeight)
                    .clip(RoundedCornerShape(PodcastDimens.ChipCorner))
                    .background(if (isOn) MaterialTheme.colorScheme.onBackground else WavvyTheme.colors.chip)
                    .clickable { if (isSort) onSortClick() else onFilterClick(index) }
                    .padding(horizontal = PodcastDimens.ChipPaddingX)
            ) {
                Text(
                    text = if (isSort) sortTitle ?: chip.title else chip.title,
                    style = MaterialTheme.typography.bodyMedium.merge(PodcastType.Chip),
                    color = if (isOn) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.onBackground,
                    maxLines = 1
                )
                if (isSort) {
                    Icon(
                        imageVector = WavvyIcons.ChevronDown,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.size(PodcastDimens.ChipArrow)
                    )
                }
            }
        }
    }
}

// The ways to order the episodes, the one in use has a check
@Composable
private fun SortSheet(sorts: List<PodcastSort>, selected: PodcastSort?, onSelect: (PodcastSort) -> Unit, onDismiss: () -> Unit) {
    WavvySheet(onDismiss = onDismiss) {
        val closeSheet = LocalSheetClose.current

        Column(modifier = Modifier.padding(bottom = WavvyTheme.dimens.spaceMedium)) {
            sorts.forEach { sort ->
                MenuAction(if (sort.token == selected?.token) WavvyIcons.Check else WavvyIcons.Blank, sort.title) {
                    closeSheet()
                    onSelect(sort)
                }
            }
        }
    }
}

// An episode, a small picture with the title and the views and the age, the description and the button that plays it with how much is left
@Composable
private fun EpisodeRow(episode: HomeItem, folder: DownloadFolder, onClick: () -> Unit, onPlay: () -> Unit) {
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
                    maxLines = 1,
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

        episode.description?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodyMedium.merge(PodcastType.EpisodeDescription),
                color = MaterialTheme.colorScheme.secondary,
                maxLines = PodcastDimens.DescriptionLines,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = PodcastDimens.Side, vertical = PodcastDimens.RowGap)
            )
        }

        // Download, which works, and put it in the queue of episodes for later, which only stands in its place for now, then how much was heard and the button that plays it with the time that is left
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = PodcastDimens.EpisodeActionsStart, end = PodcastDimens.Side)
                .padding(bottom = PodcastDimens.RowGap)
        ) {
            val download = Downloads.items.collectAsState().value[episode.id]
            val context = LocalContext.current
            EpisodeAction(if (download?.phase == DownloadPhase.Completed) WavvyIcons.Check else WavvyIcons.Download, progress = download?.takeIf { it.phase == DownloadPhase.Downloading }?.percent?.div(PercentTotal)) {
                val track = episode.toPlayableTrack() ?: return@EpisodeAction
                if (download == null || download.phase == DownloadPhase.Failed) Downloads.enqueue(context, track, folder) else Downloads.remove(context, track.id)
            }
            EpisodeAction(WavvyIcons.AddCircle)
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

// What the percent of how much was heard is counted against
private const val PercentTotal = 100f

// The room of an episode while the episodes are asked, the picture and the lines in the places of the real ones
@Composable
private fun EpisodeSkeleton() {
    SkeletonHost {
        Column(
            modifier = Modifier
                .fillMaxWidth()
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

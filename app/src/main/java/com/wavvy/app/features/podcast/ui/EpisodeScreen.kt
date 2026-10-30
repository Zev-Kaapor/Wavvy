package com.wavvy.app.features.podcast.ui

// Android context and sharing
import android.content.Context
import android.content.Intent
// Compose layouts and foundations
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
// Material 3 components
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
// UI styling and utilities
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
// Images, lifecycle and time
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
// Project resources
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import android.content.res.Configuration
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.ui.platform.LocalConfiguration
import com.wavvy.app.R
import com.wavvy.app.core.designsystem.components.SkeletonHost
import com.wavvy.app.core.designsystem.components.skeleton
import com.wavvy.app.core.designsystem.icons.WavvyIcons
import com.wavvy.app.core.designsystem.theme.WavvyMotion
import com.wavvy.app.core.designsystem.theme.WavvyTheme
import com.wavvy.app.core.download.DownloadFolder
import com.wavvy.app.core.download.DownloadPhase
import com.wavvy.app.core.download.Downloads
import com.wavvy.app.core.innertube.MusicOrigin
import com.wavvy.app.core.navigation.ItemNavigator
import com.wavvy.app.features.collection.ui.CollectionDimens
import com.wavvy.app.features.collection.ui.CollectionType
import com.wavvy.app.features.discover.ui.DiscoverShelfTitle
import com.wavvy.app.features.home.data.HomeItem
import com.wavvy.app.features.home.data.HomeItemKind
import com.wavvy.app.features.home.ui.components.HomeMessage
import com.wavvy.app.features.home.ui.rememberItemPlayer
import com.wavvy.app.features.home.ui.toPlayableTrack
import com.wavvy.app.features.menu.ItemMenu
import com.wavvy.app.features.player.ui.LocalMiniPlayerInset
import com.wavvy.app.features.playlist.data.PlaylistActions
import com.wavvy.app.features.podcast.data.EpisodePage
import com.wavvy.app.features.podcast.data.EpisodesLater
import com.wavvy.app.features.podcast.data.PodcastPage

// The page of an episode, its picture with the color of it on top, the name, who makes it, the numbers, the buttons or the count of the days that are left for it to come,
// the description that opens when asked and the other episodes of the podcast
@Composable
fun EpisodeScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: EpisodeViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()
    val onItemClick = rememberItemPlayer()
    val listState = rememberLazyListState()
    val context = LocalContext.current
    val page = state.page

    // The name of the episode comes into the bar once its own is mostly gone
    val threshold = with(LocalDensity.current) { EpisodeDimens.BarScrollThreshold.toPx() }
    val isScrolled by remember { derivedStateOf { listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > threshold } }
    val barAlpha by animateFloatAsState(
        targetValue = if (isScrolled) 1f else 0f,
        animationSpec = tween(PodcastDimens.BarFadeMillis),
        label = "EpisodeBar"
    )

    // The episodes kept for later in the account, read when the page opens
    val laterIds by EpisodesLater.kept.collectAsState()
    LaunchedEffect(Unit) { EpisodesLater.refresh(context) }

    // The time that is left for an episode that is not out, it ticks each second until the moment comes
    val scheduledAt = page?.scheduledAt
    val remaining = rememberRemaining(scheduledAt)
    val isWaiting = page?.isScheduled == true && (remaining == null || remaining > 0)

    // The page is asked again when the moment comes and, while the episode is not out yet, every so often, so the buttons that work come as soon as it is out
    LaunchedEffect(page?.videoId, page?.isScheduled, scheduledAt) {
        if (page?.isScheduled != true) return@LaunchedEffect

        var wait = scheduledAt?.let { (it - System.currentTimeMillis()).coerceAtLeast(0L) } ?: EpisodeDimens.RefreshMillis
        while (true) {
            delay(wait)
            viewModel.refresh()
            wait = EpisodeDimens.RefreshMillis
        }
    }

    val topInset = WindowInsets.safeDrawing.only(WindowInsetsSides.Top).asPaddingValues().calculateTopPadding()

    // Solid, so the screen under it does not show through while the page slides
    Box(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        CoverBackdrop(coverUrl = page?.coverUrl, listState = listState, modifier = Modifier.align(Alignment.TopStart))

        when {
            state.status == EpisodeStatus.Loading -> EpisodeSkeleton(modifier = Modifier.padding(top = topInset + PodcastDimens.BarHeight))

            state.status == EpisodeStatus.Error || page == null -> HomeMessage(
                text = stringResource(R.string.episode_error),
                actionLabel = stringResource(R.string.home_retry),
                onAction = viewModel::load
            )

            else -> {
                val item = HomeItem(
                    kind = HomeItemKind.Episode,
                    id = page.videoId,
                    title = page.title,
                    thumbnailUrl = page.coverUrl,
                    author = page.showName,
                    podcastId = page.showId
                )
                val show = state.show
                val folder = page.showId?.let { DownloadFolder(it, page.showName.orEmpty(), show?.coverUrl ?: page.coverUrl) }

                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        top = topInset + PodcastDimens.BarHeight,
                        bottom = LocalMiniPlayerInset.current + if (LocalMiniPlayerInset.current > 0.dp) PodcastDimens.MiniPlayerClearance else 0.dp
                    )
                ) {
                    item(key = "details") {
                        EpisodeDetails(
                            page = page,
                            show = show,
                            item = item,
                            folder = folder,
                            context = context,
                            isWaiting = isWaiting,
                            remaining = remaining,
                            isKept = laterIds?.contains(item.id) == true,
                            canKeep = laterIds != null,
                            onPlay = { onItemClick(item) }
                        )
                    }

                    item(key = "description") {
                        EpisodeDescription(page = page)
                    }

                    // The other episodes of the podcast, the ones that are out, with the arrow of the title that opens all of them
                    val others = show?.episodes?.episodes.orEmpty().filter { it.id != page.videoId && it.durationText != null }.take(EpisodeDimens.MoreEpisodes)
                    if (others.isNotEmpty() && page.showId != null) {
                        item(key = "more_title") {
                            Spacer(modifier = Modifier.height(EpisodeDimens.SectionTop))
                            DiscoverShelfTitle(title = stringResource(R.string.episode_more_from), link = null, onClick = { ItemNavigator.openPodcast(page.showId) })
                        }
                        items(others.size, key = { "more_${others[it].id}" }) { index ->
                            val episode = others[index]
                            EpisodeRow(
                                episode = episode,
                                folder = DownloadFolder(page.showId, page.showName.orEmpty(), show?.coverUrl ?: page.coverUrl),
                                isKept = laterIds?.contains(episode.id) == true,
                                onKeep = if (laterIds != null) { { PlaylistActions.scope.launch { EpisodesLater.toggle(context, episode.id) } } } else null,
                                onClick = { ItemNavigator.openEpisode(episode.id) },
                                onPlay = { onItemClick(episode) }
                            )
                        }
                    }
                }
            }
        }

        EpisodeBar(title = page?.title.orEmpty(), scrolledAlpha = barAlpha, onBack = onBack)
    }
}

// The bar on top, the arrow and the name of the episode with a line under it when the page scrolls
@Composable
private fun EpisodeBar(title: String, scrolledAlpha: Float, onBack: () -> Unit) {
    val dimens = WavvyTheme.dimens

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background.copy(alpha = scrolledAlpha))
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().height(PodcastDimens.BarHeight).padding(horizontal = dimens.spaceSmall)
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

        if (scrolledAlpha > 0f) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = scrolledAlpha))
        }
    }
}

// The picture, the name, who makes it, the numbers, how much was heard and the buttons, all centered as the page of a podcast has them
@Composable
private fun EpisodeDetails(
    page: EpisodePage,
    show: PodcastPage?,
    item: HomeItem,
    folder: DownloadFolder?,
    context: Context,
    isWaiting: Boolean,
    remaining: Long?,
    isKept: Boolean,
    canKeep: Boolean,
    onPlay: () -> Unit
) {
    val scope = rememberCoroutineScope()

    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    val alignment = if (isLandscape) Alignment.Start else Alignment.CenterHorizontally
    val textAlign = if (isLandscape) TextAlign.Start else TextAlign.Center

    // The picture, wide and in the middle standing up, smaller and at the end lying down
    val picture: @Composable (Modifier) -> Unit = { modifier ->
        AsyncImage(
            model = page.coverUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier
                .aspectRatio(EpisodePictureRatio)
                .clip(RoundedCornerShape(EpisodeDimens.PictureCorner))
                .background(MaterialTheme.colorScheme.surfaceContainer)
        )
    }

    // The name, who makes the podcast, the numbers, how much was heard and the buttons
    val info: @Composable ColumnScope.() -> Unit = {
        Text(
            text = page.title,
            style = MaterialTheme.typography.headlineSmall.merge(EpisodeType.Title),
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = textAlign,
            modifier = Modifier.padding(top = EpisodeDimens.TitleTop)
        )

        // By who makes the podcast with their photo, the same line as the page of an album, a tap opens the podcast, the photo comes with the page of the podcast
        val maker = show?.author ?: page.showName
        if (maker != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .padding(top = CollectionDimens.TextGap)
                    .clip(RoundedCornerShape(EpisodeDimens.ByCorner))
                    .clickable(enabled = page.showId != null) { page.showId?.let { ItemNavigator.openPodcast(it) } }
            ) {
                Text(
                    text = stringResource(R.string.collection_by),
                    style = MaterialTheme.typography.bodyMedium.merge(CollectionType.Detail),
                    color = MaterialTheme.colorScheme.secondary
                )
                Spacer(modifier = Modifier.size(CollectionDimens.ByGap))
                show?.authorPhoto?.let { photo ->
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
                    text = maker,
                    style = MaterialTheme.typography.bodyMedium.merge(CollectionType.By),
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
            }
        }

        // How many watched, how long ago and how long it is, in a single line as YouTube Music writes it, an episode that is not out has none of them
        val numbers = listOfNotNull(page.views, page.age, page.durationText).joinToString(NumbersSeparator)
        val line = if (page.isScheduled) stringResource(R.string.episode_scheduled_on, page.scheduledText.orEmpty()) else numbers
        if (line.isNotEmpty()) {
            Text(
                text = line,
                style = MaterialTheme.typography.bodyMedium.merge(CollectionType.Detail),
                color = MaterialTheme.colorScheme.secondary,
                textAlign = textAlign,
                modifier = Modifier.padding(top = CollectionDimens.TextGap)
            )
        }

        // How much of it was heard, a bar as in the lists of the episodes
        if (page.progressPercent > 0 && !page.isScheduled) {
            Box(
                modifier = Modifier
                    .padding(top = EpisodeDimens.ProgressTop)
                    .width(EpisodeDimens.ProgressWidth)
                    .height(EpisodeDimens.ProgressHeight)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(page.progressPercent / PercentTotal)
                        .height(EpisodeDimens.ProgressHeight)
                        .background(WavvyTheme.colors.episodeProgress)
                )
            }
        }

        // The buttons in the order of the page of a podcast, while the episode is not out there is no play, the share, the time that is left and the menu stand in a row
        // and when the moment comes it fades into the buttons that work
        val download by remember(item.id) { Downloads.items.map { it[item.id] }.distinctUntilChanged() }.collectAsState(initial = Downloads.items.value[item.id])
        val isDone = download?.phase == DownloadPhase.Completed
        val share = { shareLink(context, "$MusicOrigin/watch?v=${page.videoId}") }

        AnimatedContent(
            targetState = isWaiting,
            transitionSpec = {
                (fadeIn(tween(WavvyMotion.ExpandMillis)) togetherWith fadeOut(tween(WavvyMotion.ExpandMillis)))
                    .using(SizeTransform(clip = false) { _, _ -> tween(WavvyMotion.ExpandMillis) })
            },
            contentAlignment = if (isLandscape) Alignment.TopStart else Alignment.TopCenter,
            label = "EpisodeActions"
        ) { waiting ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(CollectionDimens.ActionGap),
                modifier = Modifier.padding(top = PodcastDimens.ActionsTop, bottom = PodcastDimens.ActionsBottom)
            ) {
                if (waiting) {
                    RoundAction(icon = WavvyIcons.Share, description = stringResource(R.string.player_share), onClick = share)
                    CountdownPill(
                        remaining = remaining,
                        height = CollectionDimens.ActionSize,
                        container = MaterialTheme.colorScheme.onBackground,
                        content = MaterialTheme.colorScheme.background,
                        textStyle = EpisodeType.CountdownValue,
                        paddingX = EpisodeDimens.CountdownPaddingX
                    )
                    RoundAction(icon = WavvyIcons.MoreVertical, description = stringResource(R.string.cd_more)) { ItemMenu.show(item) }
                } else {
                    RoundAction(
                        icon = if (isDone) WavvyIcons.Check else WavvyIcons.Download,
                        description = stringResource(R.string.playlist_download),
                        progress = download?.takeIf { it.phase == DownloadPhase.Downloading }?.percent?.div(PercentTotal)
                    ) {
                        val track = item.toPlayableTrack() ?: return@RoundAction
                        if (download?.phase.let { it == null || it == DownloadPhase.Failed }) Downloads.enqueue(context, track, folder) else Downloads.remove(context, track.id)
                    }
                    RoundAction(
                        icon = if (isKept) WavvyIcons.CheckCircle else WavvyIcons.AddCircle,
                        description = stringResource(if (isKept) R.string.menu_later_remove else R.string.menu_later_add),
                        isEnabled = canKeep
                    ) { scope.launch { EpisodesLater.toggle(context, item.id) } }

                    FilledIconButton(
                        onClick = onPlay,
                        modifier = Modifier.size(CollectionDimens.PlaySize),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.onBackground,
                            contentColor = MaterialTheme.colorScheme.background
                        )
                    ) {
                        Icon(
                            imageVector = WavvyIcons.PlayArrow,
                            contentDescription = stringResource(R.string.artist_play),
                            modifier = Modifier.size(CollectionDimens.PlayIcon)
                        )
                    }

                    RoundAction(icon = WavvyIcons.Share, description = stringResource(R.string.player_share), onClick = share)
                    RoundAction(icon = WavvyIcons.MoreVertical, description = stringResource(R.string.cd_more)) { ItemMenu.show(item) }
                }
            }
        }
    }

    val room = Modifier
        .fillMaxWidth()
        .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
        .padding(horizontal = PodcastDimens.Side)

    if (isLandscape) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = room.padding(top = EpisodeDimens.PictureTop)) {
            Column(modifier = Modifier.weight(1f), horizontalAlignment = alignment, content = info)
            Spacer(modifier = Modifier.width(CollectionDimens.LandscapeGap))
            picture(Modifier.width(EpisodeDimens.LandscapePictureWidth))
        }
    } else {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = room) {
            BoxWithConstraints(modifier = Modifier.fillMaxWidth().padding(top = EpisodeDimens.PictureTop), contentAlignment = Alignment.Center) {
                picture(Modifier.width(minOf(maxWidth, EpisodeDimens.PictureMaxWidth)))
            }
            info()
        }
    }
}

// The description, a few lines at first and the word that opens the rest, the pieces that link somewhere in white and the ones with an address open it
@Composable
private fun EpisodeDescription(page: EpisodePage) {
    val link = MaterialTheme.colorScheme.onBackground
    var isOpen by rememberSaveable(page.videoId) { mutableStateOf(false) }
    var isCut by remember(page.videoId) { mutableStateOf(false) }

    if (page.description.isEmpty()) return

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
            .padding(horizontal = PodcastDimens.Side)
    ) {
        val text = remember(page.description, isOpen, link) {
            buildAnnotatedString {
                page.description.forEach { part ->
                    val piece = tidy(part.text, isCompact = !isOpen)
                    when {
                        part.url != null -> withLink(LinkAnnotation.Url(part.url, TextLinkStyles(SpanStyle(color = link)))) { append(piece) }
                        part.isLink -> withStyle(SpanStyle(color = link)) { append(piece) }
                        else -> append(piece)
                    }
                }
            }
        }

        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge.merge(EpisodeType.Description),
            color = MaterialTheme.colorScheme.secondary,
            maxLines = if (isOpen) Int.MAX_VALUE else EpisodeDimens.DescriptionLines,
            overflow = TextOverflow.Ellipsis,
            onTextLayout = { result -> if (!isOpen) isCut = result.hasVisualOverflow },
            modifier = Modifier.fillMaxWidth()
        )

        if (isCut || isOpen) {
            Text(
                text = stringResource(if (isOpen) R.string.podcast_less else R.string.podcast_more),
                style = MaterialTheme.typography.bodyMedium.merge(PodcastType.More),
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier
                    .padding(top = EpisodeDimens.MoreTop)
                    .clickable { isOpen = !isOpen }
            )
        }
    }
}

// A line of dashes or of other signs that only separates, which gives the description a lot of room for nothing, and the empty lines in a row, which are left out while it is closed
private val Separators = Regex("[-–—_=*~•·.]{6,}")
private val EmptyLines = Regex("\\n{3,}")
private val BlankLines = Regex("\\n{2,}")

private fun tidy(text: String, isCompact: Boolean): String =
    text.replace(Separators, "").replace(if (isCompact) BlankLines else EmptyLines, if (isCompact) "\n" else "\n\n")

// The room of the page while it is asked, the picture, the name, who makes it, the numbers and the round buttons in the places of the real ones
@Composable
private fun EpisodeSkeleton(modifier: Modifier = Modifier) {
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    val shape = MaterialTheme.shapes.extraSmall

    SkeletonHost(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
            .padding(horizontal = PodcastDimens.Side)
    ) {
        val picture: @Composable (Modifier) -> Unit = { size ->
            Box(modifier = size.aspectRatio(EpisodePictureRatio).skeleton(RoundedCornerShape(EpisodeDimens.PictureCorner)))
        }
        val info: @Composable ColumnScope.() -> Unit = {
            Box(modifier = Modifier.padding(top = EpisodeDimens.TitleTop).fillMaxWidth(PodcastDimens.SkeletonNameFraction).height(PodcastDimens.SkeletonNameHeight).skeleton(shape))
            Box(modifier = Modifier.padding(top = CollectionDimens.TextGap * 2).fillMaxWidth(PodcastDimens.SkeletonByFraction).height(PodcastDimens.SkeletonLineHeight).skeleton(shape))
            Box(modifier = Modifier.padding(top = CollectionDimens.TextGap * 2).fillMaxWidth(EpisodeDimens.SkeletonNumbersFraction).height(PodcastDimens.SkeletonLineHeight).skeleton(shape))
            Row(
                horizontalArrangement = Arrangement.spacedBy(CollectionDimens.ActionGap),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = PodcastDimens.ActionsTop)
            ) {
                repeat(PodcastDimens.SkeletonButtons) { index ->
                    val size = if (index == PodcastDimens.SkeletonButtons / 2) CollectionDimens.PlaySize else CollectionDimens.ActionSize
                    Box(modifier = Modifier.size(size).skeleton(CircleShape))
                }
            }
        }

        if (isLandscape) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(top = EpisodeDimens.PictureTop)) {
                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.Start, content = info)
                Spacer(modifier = Modifier.width(CollectionDimens.LandscapeGap))
                picture(Modifier.width(EpisodeDimens.LandscapePictureWidth))
            }
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                BoxWithConstraints(modifier = Modifier.fillMaxWidth().padding(top = EpisodeDimens.PictureTop), contentAlignment = Alignment.Center) {
                    picture(Modifier.width(minOf(maxWidth, EpisodeDimens.PictureMaxWidth)))
                }
                info()
            }
        }
    }
}

// The shape of the picture of an episode
private const val EpisodePictureRatio = 16f / 9f

// What the percent of how much was heard is counted against, and the sign between the parts of the numbers
private const val PercentTotal = 100f
private const val NumbersSeparator = " • "

// Opens the sharing sheet of the system with the link
private fun shareLink(context: Context, url: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, url)
    }
    context.startActivity(Intent.createChooser(intent, context.getString(R.string.player_share_via)))
}

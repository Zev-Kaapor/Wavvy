package com.wavvy.app.features.collection.ui

// Compose animation
// Compose layouts and foundations
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
// UI styling and utilities
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
// Lifecycle, image loading and sharing
import android.content.Context
import android.content.Intent
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import coil3.request.transformations
// Project resources
import com.wavvy.app.R
import com.wavvy.app.core.designsystem.components.SkeletonHost
import com.wavvy.app.core.designsystem.components.skeleton
import com.wavvy.app.core.designsystem.icons.WavvyIcons
import com.wavvy.app.core.designsystem.theme.WavvyTheme
import com.wavvy.app.core.innertube.resize
import com.wavvy.app.core.playback.PlayerConnection
import com.wavvy.app.features.collection.data.CollectionKind
import com.wavvy.app.features.collection.data.CollectionPage
import com.wavvy.app.features.home.ui.components.HomeDimens
import com.wavvy.app.features.home.ui.components.HomeListItem
import com.wavvy.app.features.home.ui.components.HomeMessage
import com.wavvy.app.features.home.ui.components.HomeShelf
import com.wavvy.app.features.home.ui.rememberItemPlayer
import com.wavvy.app.features.home.ui.rememberListPlayer
import com.wavvy.app.features.home.ui.toPlayableTrack
import com.wavvy.app.features.player.ui.LocalMiniPlayerInset
import com.wavvy.app.features.player.ui.components.BackdropBlur
import com.wavvy.app.features.player.ui.components.PlayerDimens
import com.wavvy.app.features.search.ui.components.SearchSkeleton

// Page of an album or of a playlist, tinted by its cover, with the cover, the title, the round buttons that play it and under them its songs
@Composable
fun CollectionScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CollectionViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val playList = rememberListPlayer()
    val onItemClick = rememberItemPlayer()
    val listState = rememberLazyListState()
    val page = state.page

    // The cover blurred once into a small picture, the same backdrop the open player has
    val backdrop = remember(page?.thumbnailUrl) {
        page?.thumbnailUrl?.let { url ->
            ImageRequest.Builder(context)
                .data(url.resize(PlayerDimens.BackdropRequestSize, PlayerDimens.BackdropRequestSize))
                .size(PlayerDimens.BackdropRequestSize)
                .transformations(BackdropBlur(PlayerDimens.BackdropBlurRadius, PlayerDimens.BackdropBlurPasses))
                .crossfade(true)
                .build()
        }
    }
    val pageColor = MaterialTheme.colorScheme.background

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

    // The menu of the page, its actions work on every song of it
    val songCount = pluralStringResource(R.plurals.collection_song_count, state.tracks.size, state.tracks.size)
    val openMenu = {
        page?.let {
            CollectionMenu.show(
                CollectionMenuData(
                    title = it.title,
                    subtitle = listOfNotNull(it.owner, songCount).joinToString(" • "),
                    coverUrl = it.thumbnailUrl?.resize(HomeDimens.CoverRequestSize, HomeDimens.CoverRequestSize),
                    onShuffle = { viewModel.play(shuffle = true, onPlay = playList) },
                    onPlayNext = { viewModel.withAllTracks { songs -> PlayerConnection.enqueueAll(context, songs.mapNotNull { it.toPlayableTrack() }, afterCurrent = true) } },
                    onAddToQueue = { viewModel.withAllTracks { songs -> PlayerConnection.enqueueAll(context, songs.mapNotNull { it.toPlayableTrack() }, afterCurrent = false) } },
                    onShare = { shareLink(context, viewModel.shareUrl) }
                )
            )
        }
        Unit
    }

    // Solid, so the screen under it does not show through while the page slides
    Box(modifier = modifier.fillMaxSize().background(pageColor)) {
        if (backdrop != null) {
            AsyncImage(
                model = backdrop,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth().fillMaxHeight(CollectionDimens.BackdropFraction).align(Alignment.TopStart)
            )
        }

        // A little dark on top, and the bottom of the blur fades into the page
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(CollectionDimens.BackdropFraction)
                .align(Alignment.TopStart)
                .background(Brush.verticalGradient(0f to Color.Black.copy(alpha = CollectionDimens.BackdropTopShade), 1f to pageColor))
        )

        Column(modifier = Modifier.fillMaxSize()) {
            CollectionTopBar(page = page, pageTitle = viewModel.pageTitle, onBack = onBack)

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
                        .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)),
                    contentPadding = PaddingValues(bottom = LocalMiniPlayerInset.current)
                ) {
                    item(key = "header") {
                        CollectionHeader(
                            page = page,
                            showTitle = viewModel.pageTitle.isEmpty(),
                            isEnabled = state.tracks.isNotEmpty() && !state.isPreparing,
                            onShuffle = { viewModel.play(shuffle = true, onPlay = playList) },
                            onPlay = { viewModel.play(shuffle = false, onPlay = playList) },
                            onMenu = openMenu
                        )
                    }

                    if (state.tracks.isEmpty()) {
                        item(key = "empty") {
                            HomeMessage(text = stringResource(R.string.collection_empty), modifier = Modifier.height(HomeDimens.EmptyHeight))
                        }
                    }

                    itemsIndexed(state.tracks, key = { _, track -> track.id }) { index, track ->
                        HomeListItem(
                            item = track,
                            onClick = { playList(state.tracks, index, null) },
                            number = if (page.kind == CollectionKind.Album) index + 1 else null,
                            // The songs of a chart have their place before the cover
                            rank = if (track.trend != null) index + 1 else null
                        )
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

                    // How many songs and how long they take, then the description
                    item(key = "footer") { CollectionFooter(page = page, showDetails = viewModel.pageTitle.isEmpty()) }

                    // Shelves of YouTube Music under the songs, such as the releases for the listener
                    itemsIndexed(page.sections, key = { index, _ -> "section_$index" }) { _, section ->
                        HomeShelf(section = section, onItemClick = onItemClick)
                    }
                }
            }
        }
    }
}

// Arrow to go back, and in the middle who made it with their photo and the kind and the year under the name
@Composable
private fun CollectionTopBar(
    page: CollectionPage?,
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

        // A playlist shows who made it under the cover, an album shows it here
        if (page != null && page.kind == CollectionKind.Album && pageTitle.isEmpty()) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = dimens.spaceExtraLarge * 2)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (page.ownerPhotoUrl != null) {
                        AsyncImage(
                            model = page.ownerPhotoUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(CollectionDimens.TopAvatar)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceContainer)
                        )
                        Spacer(modifier = Modifier.size(CollectionDimens.TopAvatarGap))
                    }

                    page.owner?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodyMedium.merge(CollectionType.Owner),
                            color = MaterialTheme.colorScheme.onBackground,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                page.subtitle?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium.merge(CollectionType.Detail),
                        color = MaterialTheme.colorScheme.secondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

// Cover, title and the round buttons, shuffle, play in the middle and the menu
@Composable
private fun CollectionHeader(
    page: CollectionPage,
    showTitle: Boolean,
    isEnabled: Boolean,
    onShuffle: () -> Unit,
    onPlay: () -> Unit,
    onMenu: () -> Unit
) {
    val dimens = WavvyTheme.dimens

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = dimens.screenPadding),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            AsyncImage(
                model = page.thumbnailUrl?.resize(HomeDimens.CoverRequestSize, HomeDimens.CoverRequestSize),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(minOf(maxWidth * CollectionDimens.CoverWidthFraction, CollectionDimens.CoverMaxSize))
                    .clip(RoundedCornerShape(CollectionDimens.CoverCorner))
                    .background(MaterialTheme.colorScheme.surfaceContainer)
            )
        }

        Spacer(modifier = Modifier.height(CollectionDimens.HeaderGap))

        if (showTitle) {
            Text(
                text = page.title,
                style = CollectionType.Title,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Who made it with their photo, and under it the number of songs and how long they take, for a playlist and for any page that has its name on the top bar
        if (page.kind == CollectionKind.Playlist || !showTitle) {
            Spacer(modifier = Modifier.height(CollectionDimens.TextGap))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (page.ownerPhotoUrl != null) {
                    AsyncImage(
                        model = page.ownerPhotoUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(CollectionDimens.TopAvatar)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceContainer)
                    )
                    Spacer(modifier = Modifier.size(CollectionDimens.TopAvatarGap))
                }

                page.owner?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium.merge(CollectionType.Owner),
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            page.details?.let {
                Spacer(modifier = Modifier.height(CollectionDimens.TextGap))
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium.merge(CollectionType.Detail),
                    color = MaterialTheme.colorScheme.secondary,
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.height(CollectionDimens.HeaderGap))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(CollectionDimens.ActionGap)
        ) {
            ActionButton(icon = WavvyIcons.Shuffle, description = stringResource(R.string.collection_shuffle), isEnabled = isEnabled, onClick = onShuffle)

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

            ActionButton(icon = WavvyIcons.MoreVertical, description = stringResource(R.string.cd_more), isEnabled = true, onClick = onMenu)
        }

        Spacer(modifier = Modifier.height(CollectionDimens.HeaderGap))
    }
}

// Small round button next to the play button
@Composable
private fun ActionButton(
    icon: ImageVector,
    description: String,
    isEnabled: Boolean,
    onClick: () -> Unit
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
        Icon(imageVector = icon, contentDescription = description, modifier = Modifier.size(CollectionDimens.ActionIcon))
    }
}

// The line with the number of songs and the time they take, and the description that opens when tapped
@Composable
private fun CollectionFooter(page: CollectionPage, showDetails: Boolean) {
    val dimens = WavvyTheme.dimens
    var isOpen by remember { mutableStateOf(false) }
    val detail = MaterialTheme.typography.bodyMedium.merge(CollectionType.Detail)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = dimens.screenPadding, vertical = CollectionDimens.FooterPadding),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // The line is under the cover of a playlist and of a page with its name on the top bar, an album keeps it here
        if (page.kind == CollectionKind.Album && showDetails) {
            page.details?.let {
                Text(text = it, style = detail, color = MaterialTheme.colorScheme.secondary, textAlign = TextAlign.Center)
            }
        }

        page.description?.let { description ->
            Spacer(modifier = Modifier.height(CollectionDimens.HeaderGap))
            Text(
                text = description,
                style = detail,
                color = MaterialTheme.colorScheme.secondary,
                maxLines = if (isOpen) Int.MAX_VALUE else CollectionDimens.DescriptionLines,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .widthIn(max = CollectionDimens.CoverMaxSize * 2)
                    .clickable { isOpen = !isOpen }
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

// Placeholder of the page while it loads, the cover and two lines of text, and the rows of songs
@Composable
private fun CollectionSkeleton() {
    val dimens = WavvyTheme.dimens

    Column {
        SkeletonHost(modifier = Modifier.fillMaxWidth().padding(horizontal = dimens.screenPadding)) {
            BoxWithConstraints(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .size(minOf(maxWidth * CollectionDimens.CoverWidthFraction, CollectionDimens.CoverMaxSize))
                        .skeleton(RoundedCornerShape(CollectionDimens.CoverCorner))
                )
            }

            Spacer(modifier = Modifier.height(CollectionDimens.HeaderGap))

            Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(CollectionDimens.SkeletonTitleFraction)
                        .height(CollectionDimens.SkeletonLine)
                        .skeleton(MaterialTheme.shapes.extraSmall)
                )
                Spacer(modifier = Modifier.height(CollectionDimens.TextGap))
                Box(
                    modifier = Modifier
                        .fillMaxWidth(CollectionDimens.SkeletonSubtitleFraction)
                        .height(CollectionDimens.SkeletonLine)
                        .skeleton(MaterialTheme.shapes.extraSmall)
                )
            }
        }

        Spacer(modifier = Modifier.height(CollectionDimens.HeaderGap))
        SearchSkeleton()
    }
}

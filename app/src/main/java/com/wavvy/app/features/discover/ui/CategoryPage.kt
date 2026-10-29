package com.wavvy.app.features.discover.ui

// Compose animation
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
// Compose layouts and foundations
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
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
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
// Material 3 components
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
// Compose state and runtime
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
// UI styling and utilities
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
// Image loading
import coil3.compose.AsyncImage
// Project resources
import com.wavvy.app.R
import com.wavvy.app.core.designsystem.components.SkeletonHost
import com.wavvy.app.core.designsystem.components.skeleton
import com.wavvy.app.core.designsystem.icons.WavvyIcons
import com.wavvy.app.core.designsystem.theme.WavvyMotion
import com.wavvy.app.core.designsystem.theme.WavvyTheme
import com.wavvy.app.core.innertube.resize
import com.wavvy.app.core.navigation.ItemNavigator
import com.wavvy.app.features.discover.data.DiscoverBlock
import com.wavvy.app.features.home.data.HomeItem
import com.wavvy.app.features.home.data.HomeItemKind
import com.wavvy.app.features.home.data.HomeSection
import com.wavvy.app.features.home.ui.components.HomeDimens
import com.wavvy.app.features.home.ui.components.HomeMessage
import com.wavvy.app.features.home.ui.components.ItemBadges
import com.wavvy.app.features.home.ui.components.columnSnapLayout
import com.wavvy.app.features.home.ui.rememberListPlayer
import com.wavvy.app.features.library.ui.Chip
import com.wavvy.app.features.player.ui.LocalMiniPlayerInset

// The word YouTube Music writes under the playlists it makes itself, which tells nothing on a page where all of them are
private const val OwnLine = "YouTube Music"

// The page of a mood or of a genre, drawn with its color from the first moment, even before the page comes, the buttons that filter the shelves,
// the playlists as a grid that goes down and shows more when asked, and the songs and the videos in the rows that slide sideways
@Composable
internal fun CategoryPage(
    state: ExploreUiState,
    accent: Long?,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onItemClick: (HomeItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val page = state.page
    val blocks = page?.blocks.orEmpty().filter { it is DiscoverBlock.Covers || it is DiscoverBlock.Wide || it is DiscoverBlock.Ranked }
    val titles = blocks.map { shelfTitleOf(it) }

    // What plays a cover at once and what plays all the songs of a shelf
    val player = rememberCoverPlayer()
    val playList = rememberListPlayer()

    // The shelf that is chosen, none while all of them show
    var selected by rememberSaveable { mutableIntStateOf(NoShelf) }

    // How tall the top of the page is, so the color goes as it scrolls away
    var headerHeight by remember { mutableIntStateOf(0) }

    // The color of the mood fading into the page, the color of the theme when the page did not come from a mood
    val pageColor = MaterialTheme.colorScheme.background
    val isLight = pageColor.luminance() > 0.5f
    val color = accent?.let { Color(it) } ?: MaterialTheme.colorScheme.primary
    val strength = if (isLight) DiscoverDimens.HubBackdropLightAlpha else DiscoverDimens.HubBackdropAlpha

    // The arrow and the title stay in place, the back of the bar comes in as the page goes under it
    val barScroll = with(LocalDensity.current) { DiscoverDimens.HubBarScroll.toPx() }
    val barAlpha by remember {
        derivedStateOf { if (listState.firstVisibleItemIndex > 0) 1f else (listState.firstVisibleItemScrollOffset / barScroll).coerceIn(0f, 1f) }
    }
    val topInset = WindowInsets.safeDrawing.only(WindowInsetsSides.Top).asPaddingValues().calculateTopPadding()

    // Solid, so the screen under it does not show through while the page slides
    Box(modifier = modifier.fillMaxSize().background(pageColor)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(DiscoverDimens.HubBackdropFraction)
                .align(Alignment.TopStart)
                .graphicsLayer {
                    val fadeDistance = headerHeight * DiscoverDimens.HubBackdropFade
                    alpha = if (listState.firstVisibleItemIndex > 0 || fadeDistance <= 0f) 0f else (1f - listState.firstVisibleItemScrollOffset / fadeDistance).coerceIn(0f, 1f)
                }
                .background(Brush.verticalGradient(0f to color.copy(alpha = strength), 1f to pageColor))
        )

        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            // How many covers sit side by side, as many as fit with their least width
            val columns = ((maxWidth - DiscoverDimens.Side * 2 + DiscoverDimens.Gap) / (DiscoverDimens.CategoryCardMin + DiscoverDimens.Gap)).toInt().coerceAtLeast(2)

            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    top = topInset + DiscoverDimens.BarHeight + DiscoverDimens.HubBarGap,
                    bottom = LocalMiniPlayerInset.current + if (LocalMiniPlayerInset.current > 0.dp) DiscoverDimens.MiniPlayerClearance else 0.dp
                )
            ) {
                // The buttons that filter the shelves, the one that is on is the only shelf that shows, the title of a shelf has room of its own under them
                item(key = "header") {
                    Box(modifier = Modifier.onSizeChanged { headerHeight = it.height }) {
                        if (page != null && state.status != ExploreStatus.Loading && titles.size > 1) {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(DiscoverDimens.HubChipGap),
                                contentPadding = PaddingValues(horizontal = DiscoverDimens.Side),
                                modifier = Modifier.windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
                            ) {
                                items(titles.size) { index ->
                                    Chip(text = titles[index], isOn = selected == index) { selected = if (selected == index) NoShelf else index }
                                }
                            }
                        } else {
                            Spacer(modifier = Modifier.height(DiscoverDimens.HubBarGap))
                        }
                    }
                }

                when {
                    state.status == ExploreStatus.Loading -> item(key = "loading") { CategorySkeleton(columns = columns) }

                    page == null -> item(key = "error") {
                        Box(modifier = Modifier.fillMaxWidth().height(DiscoverDimens.LoadingHeight)) {
                            HomeMessage(
                                text = stringResource(R.string.home_error),
                                actionLabel = stringResource(R.string.home_retry),
                                onAction = onRetry
                            )
                        }
                    }

                    else -> item(key = "shelves") {
                        // The shelves of one filter fade into the ones of the next and the page grows or shrinks to them
                        AnimatedContent(
                            targetState = selected,
                            transitionSpec = {
                                (fadeIn(tween(WavvyMotion.ExpandMillis)) togetherWith fadeOut(tween(WavvyMotion.ExpandMillis)))
                                    .using(SizeTransform(clip = false) { _, _ -> tween(WavvyMotion.ExpandMillis) })
                            },
                            contentAlignment = Alignment.TopStart,
                            label = "CategoryShelves"
                        ) { choice ->
                            Column {
                                val shown = if (choice in blocks.indices) listOf(choice) else blocks.indices.toList()

                                shown.forEach { index ->
                                    when (val block = blocks[index]) {
                                        is DiscoverBlock.Covers -> CoverGrid(block = block, columns = columns, player = player, onItemClick = onItemClick)
                                        is DiscoverBlock.Wide -> VideoRows(section = block.section, onItemClick = onItemClick)
                                        is DiscoverBlock.Ranked -> DiscoverRankedShelf(
                                            section = block.section,
                                            onItemClick = onItemClick,
                                            actions = { SongActions(songs = block.section.items.distinctBy { it.id }, onPlay = playList) }
                                        )
                                        else -> Unit
                                    }
                                    Spacer(modifier = Modifier.height(DiscoverDimens.BlockGap))
                                }
                            }
                        }
                    }
                }
            }
        }

        ExploreBar(title = state.title, titleAlpha = 1f, onBack = onBack, backgroundAlpha = barAlpha)
    }
}

// No shelf is chosen, all of them show
private const val NoShelf = -1

// The name of a shelf as its button says it
private fun shelfTitleOf(block: DiscoverBlock): String = when (block) {
    is DiscoverBlock.Covers -> block.section.title
    is DiscoverBlock.Wide -> block.section.title
    is DiscoverBlock.Ranked -> block.section.title
    else -> ""
}

// The covers of a shelf as a grid with a few rows, the arrow of the title opens the page of all of them, the one of YouTube Music when it has it and the one of the app when not
@Composable
private fun CoverGrid(
    block: DiscoverBlock.Covers,
    columns: Int,
    player: CoverPlayer,
    onItemClick: (HomeItem) -> Unit
) {
    val covers = block.section.items.distinctBy { it.id }
    val preview = columns * DiscoverDimens.CategoryPreviewRows
    val opensAll: (() -> Unit)? = if (block.section.link == null && covers.size > preview) {
        {
            ShelfStore.section = block.section
            ItemNavigator.openShelf()
        }
    } else {
        null
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        DiscoverShelfTitle(title = block.section.title, link = block.section.link, onClick = opensAll)

        Column(
            verticalArrangement = Arrangement.spacedBy(DiscoverDimens.CategoryRowGap),
            modifier = Modifier
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
                .padding(horizontal = DiscoverDimens.Side)
        ) {
            covers.take(preview).chunked(columns).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(DiscoverDimens.Gap)) {
                    row.forEach { item ->
                        CategoryCard(item = item, isLoading = player.playingId == item.id, onPlay = { player.play(item) }, onClick = { onItemClick(item) }, modifier = Modifier.weight(1f))
                    }
                    repeat(columns - row.size) { Spacer(modifier = Modifier.weight(1f)) }
                }
            }
        }
    }
}


// The buttons that play all the songs of a shelf, in order or shuffled
@Composable
private fun SongActions(songs: List<HomeItem>, onPlay: (List<HomeItem>, Int, Boolean?) -> Unit) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(DiscoverDimens.CategoryActionGap),
        modifier = Modifier
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
            .padding(horizontal = DiscoverDimens.Side)
            .padding(bottom = DiscoverDimens.CategoryActionsBottom)
    ) {
        ActionPill(icon = WavvyIcons.PlayArrow, label = stringResource(R.string.home_play_all), isFilled = true) { onPlay(songs, 0, null) }
        ActionPill(icon = WavvyIcons.Shuffle, label = stringResource(R.string.collection_shuffle), isFilled = false) { onPlay(songs, songs.indices.random(), true) }
    }
}

// A button with an icon and its words, solid when it is the main one
@Composable
private fun ActionPill(icon: ImageVector, label: String, isFilled: Boolean, onClick: () -> Unit) {
    val container = if (isFilled) MaterialTheme.colorScheme.onBackground else WavvyTheme.colors.chip
    val content = if (isFilled) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.onBackground

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .height(DiscoverDimens.CategoryActionHeight)
            .clip(CircleShape)
            .background(container)
            .clickable(onClick = onClick)
            .padding(horizontal = DiscoverDimens.CategoryActionPadding)
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = content, modifier = Modifier.size(DiscoverDimens.CategoryActionIcon))
        Spacer(modifier = Modifier.width(DiscoverDimens.CategoryActionGap))
        Text(text = label, style = MaterialTheme.typography.bodyMedium.merge(DiscoverType.CardLine).copy(fontWeight = FontWeight.Medium), color = content)
    }
}

// The videos of a shelf in two rows that slide together, stopping with a whole column at the start
@Composable
private fun VideoRows(section: HomeSection, onItemClick: (HomeItem) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        DiscoverShelfTitle(title = section.title, link = null)

        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val cardWidth = DiscoverDimens.CategoryVideoWidth
            val cardHeight = cardWidth / WideAspectRatio + DiscoverDimens.CoverTextHeight
            val state = rememberLazyGridState()
            val snapLayout = remember(state) { columnSnapLayout(state) { _, _ -> 0f } }

            LazyHorizontalGrid(
                state = state,
                rows = GridCells.Fixed(DiscoverDimens.CategoryVideoRows),
                flingBehavior = rememberSnapFlingBehavior(snapLayout),
                horizontalArrangement = Arrangement.spacedBy(DiscoverDimens.Gap),
                verticalArrangement = Arrangement.spacedBy(DiscoverDimens.CategoryRowGap),
                contentPadding = PaddingValues(horizontal = DiscoverDimens.Side),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(cardHeight * DiscoverDimens.CategoryVideoRows + DiscoverDimens.CategoryRowGap * (DiscoverDimens.CategoryVideoRows - 1))
            ) {
                gridItems(section.items.distinctBy { it.id }, key = { it.id }) { video ->
                    WideCard(item = video, onClick = { onItemClick(video) }, modifier = Modifier.width(cardWidth))
                }
            }
        }
    }
}

// A cover with its name in up to two lines and, when it has something to say, the line under it, a tap opens it and the round button plays it at once
@Composable
internal fun CategoryCard(item: HomeItem, isLoading: Boolean, onPlay: () -> Unit, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val line = (item.lineText ?: item.typeText)?.takeUnless { it.endsWith(OwnLine) }

    Column(modifier = modifier.clickable(onClick = onClick)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(DiscoverDimens.CategoryCorner))
                .background(MaterialTheme.colorScheme.surfaceContainer)
        ) {
            AsyncImage(
                model = item.thumbnailUrl?.resize(HomeDimens.CoverRequestSize, HomeDimens.CoverRequestSize),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Only what has songs plays, an artist does not
            if (item.kind == HomeItemKind.Playlist || item.kind == HomeItemKind.Album) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(DiscoverDimens.CategoryPlayInset)
                        .size(DiscoverDimens.CategoryPlay)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.onBackground)
                        .clickable(onClick = onPlay)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.background,
                            strokeWidth = DiscoverDimens.CategoryPlayStroke,
                            modifier = Modifier.size(DiscoverDimens.CategoryPlaySpinner)
                        )
                    } else {
                        Icon(
                            imageVector = WavvyIcons.PlayArrow,
                            contentDescription = stringResource(R.string.home_play_all),
                            tint = MaterialTheme.colorScheme.background,
                            modifier = Modifier.size(DiscoverDimens.CategoryPlayIcon)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(DiscoverDimens.CardTextGap))

        Text(
            text = item.title,
            style = MaterialTheme.typography.bodyMedium.merge(DiscoverType.CardTitle),
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        line?.let {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ItemBadges(item = item)
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium.merge(DiscoverType.CardLine),
                    color = MaterialTheme.colorScheme.secondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

// Placeholder of the page while it loads, the title of a shelf and the first rows of covers with their lines, each where the real one will be
@Composable
private fun CategorySkeleton(columns: Int) {
    val shape = MaterialTheme.shapes.extraSmall

    SkeletonHost(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .padding(horizontal = DiscoverDimens.Side, vertical = DiscoverDimens.TitleVertical)
                .width(DiscoverDimens.SkeletonShelfTitleWidth)
                .height(DiscoverDimens.SkeletonLine)
                .skeleton(shape)
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(DiscoverDimens.CategoryRowGap),
            modifier = Modifier
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
                .padding(horizontal = DiscoverDimens.Side)
        ) {
            repeat(DiscoverDimens.CategoryPreviewRows) {
                Row(horizontalArrangement = Arrangement.spacedBy(DiscoverDimens.Gap)) {
                    repeat(columns) {
                        Column(modifier = Modifier.weight(1f)) {
                            Box(modifier = Modifier.fillMaxWidth().aspectRatio(1f).skeleton(RoundedCornerShape(DiscoverDimens.CategoryCorner)))
                            Spacer(modifier = Modifier.height(DiscoverDimens.CardTextGap))
                            Box(modifier = Modifier.fillMaxWidth(DiscoverDimens.SkeletonTitleFraction).height(DiscoverDimens.SkeletonLine).skeleton(shape))
                        }
                    }
                }
            }
        }

        // The title and the first videos, wide and in two rows as the shelf of videos is drawn
        Box(
            modifier = Modifier
                .padding(horizontal = DiscoverDimens.Side, vertical = DiscoverDimens.TitleVertical)
                .width(DiscoverDimens.SkeletonShelfTitleWidth)
                .height(DiscoverDimens.SkeletonLine)
                .skeleton(shape)
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(DiscoverDimens.Gap),
            modifier = Modifier
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
                .padding(horizontal = DiscoverDimens.Side)
        ) {
            repeat(2) {
                Column(modifier = Modifier.weight(1f)) {
                    Box(modifier = Modifier.fillMaxWidth().aspectRatio(WideAspectRatio).skeleton(RoundedCornerShape(DiscoverDimens.CategoryCorner)))
                    Spacer(modifier = Modifier.height(DiscoverDimens.CardTextGap))
                    Box(modifier = Modifier.fillMaxWidth(DiscoverDimens.SkeletonTitleFraction).height(DiscoverDimens.SkeletonLine).skeleton(shape))
                }
            }
        }
    }
}

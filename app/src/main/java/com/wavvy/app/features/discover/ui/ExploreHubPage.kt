package com.wavvy.app.features.discover.ui

// Compose animation
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
// Compose layouts and foundations
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
// Compose state and runtime
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
// Image loading
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import coil3.request.transformations
// Project resources
import com.wavvy.app.features.library.ui.FilterChips
import com.wavvy.app.core.designsystem.components.rememberBackdropFade
import com.wavvy.app.R
import com.wavvy.app.core.designsystem.components.SkeletonHost
import com.wavvy.app.core.designsystem.components.skeleton
import com.wavvy.app.core.designsystem.icons.WavvyIcons
import com.wavvy.app.core.designsystem.theme.WavvyMotion
import com.wavvy.app.core.designsystem.theme.WavvyTheme
import com.wavvy.app.core.innertube.resize
import com.wavvy.app.features.discover.data.DiscoverBlock
import com.wavvy.app.features.home.data.HomeItem
import com.wavvy.app.features.home.ui.components.HomeDimens
import com.wavvy.app.features.home.ui.components.HomeMessage
import com.wavvy.app.features.home.ui.components.itemSubtitle
import com.wavvy.app.features.library.ui.Chip
import com.wavvy.app.features.player.ui.LocalMiniPlayerInset
import com.wavvy.app.features.player.ui.components.BackdropBlur
import com.wavvy.app.features.player.ui.components.PlayerDimens

// The root of a group of the Explore tab, such as the new releases, drawn as Wavvy draws its pages, the color of the first cover fading into the page as it scrolls,
// the bar with the arrow and the title, the buttons that filter the shelves, the highlight as a card and the shelves under it
@Composable
internal fun ExploreHubPage(
    state: ExploreUiState,
    hasHighlights: Boolean,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onItemClick: (HomeItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val listState = rememberLazyListState()
    val page = state.page

    // The shelf that is chosen, none while all of them show
    val filter = rememberShelfFilter()

    // How tall the top of the page is, the buttons and the highlights, so the color of the cover goes as that part scrolls away
    var headerHeight by remember { mutableIntStateOf(0) }
    val backdropFade = rememberBackdropFade(listState) { headerHeight * DiscoverDimens.HubBackdropFade }


    // The highlights come first as cards, the other blocks are the shelves in the order they come
    val blocks = page?.blocks.orEmpty()
    val highlights = blocks.filterIsInstance<DiscoverBlock.Featured>().flatMap { it.items }.distinctBy { it.id }
    val shelves = blocks.filter { it !is DiscoverBlock.Featured && it !is DiscoverBlock.Grid }
    val shelfTitles = shelves.map { shelfTitleOf(it) }

    // The color of the first cover, blurred once into a small picture, as the page of an album has it
    val pageColor = MaterialTheme.colorScheme.background
    val isLight = pageColor.luminance() > 0.5f
    val coverUrl = highlights.firstOrNull()?.thumbnailUrl
        ?: shelves.firstNotNullOfOrNull { block -> (block as? DiscoverBlock.Covers)?.section?.items?.firstOrNull()?.thumbnailUrl }
    val backdrop = remember(coverUrl) {
        coverUrl?.let { url ->
            ImageRequest.Builder(context)
                .data(url.resize(PlayerDimens.BackdropRequestSize, PlayerDimens.BackdropRequestSize))
                .size(PlayerDimens.BackdropRequestSize)
                .transformations(BackdropBlur(PlayerDimens.BackdropBlurRadius, PlayerDimens.BackdropBlurPasses))
                .crossfade(WavvyMotion.ScreenFadeMillis)
                .build()
        }
    }

    // The arrow and the title stay in place, the back of the bar comes in as the page goes under it
    val barScroll = with(LocalDensity.current) { DiscoverDimens.HubBarScroll.toPx() }
    val barAlpha by remember {
        derivedStateOf { if (listState.firstVisibleItemIndex > 0) 1f else (listState.firstVisibleItemScrollOffset / barScroll).coerceIn(0f, 1f) }
    }

    val topInset = WindowInsets.safeDrawing.only(WindowInsetsSides.Top).asPaddingValues().calculateTopPadding()

    // Solid, so the screen under it does not show through while the page slides
    Box(modifier = modifier.fillMaxSize().background(pageColor)) {
        // The color fades into the page as the list goes down and comes back as it goes up
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(DiscoverDimens.HubBackdropFraction)
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
                    modifier = Modifier.fillMaxSize().graphicsLayer { alpha = if (isLight) DiscoverDimens.HubBackdropLightAlpha else DiscoverDimens.HubBackdropAlpha }
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            DiscoverDimens.HubBackdropHold to Color.Transparent,
                            1f to pageColor
                        )
                    )
            )
        }

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = topInset + DiscoverDimens.BarHeight,
                bottom = LocalMiniPlayerInset.current + if (LocalMiniPlayerInset.current > 0.dp) DiscoverDimens.MiniPlayerClearance else 0.dp
            )
        ) {
            // The top of the page, the buttons that filter the shelves and the highlights, which a filter takes away
            item(key = "header") {
                Column(modifier = Modifier.onSizeChanged { headerHeight = it.height }.padding(top = DiscoverDimens.HubBarGap)) {
                    if (page != null && state.status != ExploreStatus.Loading) {
                        // The buttons are filters, the one that is on is the only shelf that shows and a tap on it again brings them all back
                        // The title of a shelf has room of its own under them, only a highlight needs a little
                        if (shelfTitles.size > 1) {
                            ShelfChips(
                                titles = shelfTitles,
                                selected = filter.selected,
                                onSelect = filter::pick,
                                modifier = Modifier.padding(bottom = if (highlights.isNotEmpty()) DiscoverDimens.HubBarGap else 0.dp)
                            )
                        }

                        AnimatedVisibility(
                            visible = filter.selected == NoShelf,
                            enter = fadeIn(tween(WavvyMotion.ExpandMillis)) + expandVertically(tween(WavvyMotion.ExpandMillis)),
                            exit = fadeOut(tween(WavvyMotion.ExpandMillis)) + shrinkVertically(tween(WavvyMotion.ExpandMillis))
                        ) {
                            Column {
                                highlights.forEach { item ->
                                    ReleaseHighlight(item = item, onClick = { onItemClick(item) })
                                    Spacer(modifier = Modifier.height(DiscoverDimens.HighlightGap))
                                }
                            }
                        }
                    }
                }
            }

            when {
                state.status == ExploreStatus.Loading -> item(key = "loading") { HubSkeleton(hasHighlights = hasHighlights) }

                page == null -> item(key = "error") {
                    Box(modifier = Modifier.fillMaxWidth().height(DiscoverDimens.LoadingHeight)) {
                        HomeMessage(
                            text = stringResource(R.string.home_error),
                            actionLabel = stringResource(R.string.home_retry),
                            onAction = onRetry
                        )
                    }
                }

                else -> {
                    item(key = "shelves") {
                        FilteredShelves(count = shelves.size, filter = filter) { index ->
                            when (val block = shelves[index]) {
                                is DiscoverBlock.Covers -> DiscoverCoverShelf(section = block.section, onItemClick = onItemClick, rows = block.rows)
                                is DiscoverBlock.Wide -> DiscoverVideoRows(section = block.section, onItemClick = onItemClick, hasLink = true)
                                is DiscoverBlock.Ranked -> DiscoverRankedShelf(section = block.section, onItemClick = onItemClick)
                                is DiscoverBlock.Moods -> DiscoverMoodShelf(title = block.title, link = block.link, moods = block.moods)
                                is DiscoverBlock.MoodGrid -> ExploreMoodGrid(block = block)
                                else -> Unit
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

// The filter that is on and the place its shelf takes, which is the place of the shelf chosen while all of them showed,
// so a change from one filter to another happens in that same place
@Stable
internal class ShelfFilter(selected: Int = NoShelf, host: Int = NoShelf) {
    var selected by mutableIntStateOf(selected)
        private set
    var host by mutableIntStateOf(host)
        private set

    // A tap on the filter that is on turns it off and every shelf comes back
    fun pick(index: Int) {
        if (selected == index) {
            selected = NoShelf
            host = NoShelf
        } else {
            if (selected == NoShelf) host = index
            selected = index
        }
    }

    companion object {
        val Saver = listSaver<ShelfFilter, Int>(save = { listOf(it.selected, it.host) }, restore = { ShelfFilter(it[0], it[1]) })
    }
}

@Composable
internal fun rememberShelfFilter(): ShelfFilter = rememberSaveable(saver = ShelfFilter.Saver) { ShelfFilter() }

// The shelves of a page under its filters, turning a filter on folds the other shelves away and the chosen one rises to the top,
// and going from one filter to another fades the next shelf in the place of the one before
@Composable
internal fun FilteredShelves(count: Int, filter: ShelfFilter, shelf: @Composable (Int) -> Unit) {
    Column {
        repeat(count) { index ->
            AnimatedVisibility(
                visible = filter.selected == NoShelf || index == filter.host,
                enter = fadeIn(tween(WavvyMotion.ExpandMillis)) + expandVertically(tween(WavvyMotion.ExpandMillis)),
                exit = fadeOut(tween(WavvyMotion.ExpandMillis)) + shrinkVertically(tween(WavvyMotion.ExpandMillis))
            ) {
                // The place that stays shows the shelf of the filter that is on
                AnimatedContent(
                    targetState = if (index == filter.host && filter.selected != NoShelf) filter.selected else index,
                    transitionSpec = {
                        (fadeIn(tween(WavvyMotion.ExpandMillis)) togetherWith fadeOut(tween(WavvyMotion.ExpandMillis)))
                            .using(SizeTransform(clip = false) { _, _ -> tween(WavvyMotion.ExpandMillis) })
                    },
                    contentAlignment = Alignment.TopStart,
                    label = "FilteredShelf"
                ) { shown ->
                    Column {
                        shelf(shown)
                        Spacer(modifier = Modifier.height(DiscoverDimens.BlockGap))
                    }
                }
            }
        }
    }
}

// The buttons that filter the shelves, as every filter of the app is
@Composable
internal fun ShelfChips(titles: List<String>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    FilterChips(
        options = titles.indices.toList(),
        selected = selected.takeIf { it in titles.indices },
        id = { it },
        gap = DiscoverDimens.HubChipGap,
        onClear = { onSelect(selected) },
        contentPadding = PaddingValues(horizontal = DiscoverDimens.Side),
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
    ) { index, isOn ->
        Chip(text = titles[index], isOn = isOn) { onSelect(index) }
    }
}

// The name of a shelf as its button says it
private fun shelfTitleOf(block: DiscoverBlock): String = when (block) {
    is DiscoverBlock.Covers -> block.section.title
    is DiscoverBlock.Wide -> block.section.title
    is DiscoverBlock.Ranked -> block.section.title
    is DiscoverBlock.Moods -> block.title
    is DiscoverBlock.MoodGrid -> block.title
    else -> ""
}

// A highlight of the page as a card, its cover, what it is and who made it, and the arrow that opens it
@Composable
private fun ReleaseHighlight(item: HomeItem, onClick: () -> Unit) {
    val line = item.lineText ?: itemSubtitle(item)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
            .padding(horizontal = DiscoverDimens.Side)
            .fillMaxWidth()
            .clip(RoundedCornerShape(DiscoverDimens.HighlightCorner))
            .background(WavvyTheme.colors.menuCard)
            .clickable(onClick = onClick)
            .padding(DiscoverDimens.HighlightPadding)
    ) {
        AsyncImage(
            model = item.thumbnailUrl?.resize(HomeDimens.CoverRequestSize, HomeDimens.CoverRequestSize),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(DiscoverDimens.HighlightCover)
                .clip(RoundedCornerShape(DiscoverDimens.HighlightCoverCorner))
                .background(MaterialTheme.colorScheme.surfaceContainer)
        )

        Column(modifier = Modifier.weight(1f).padding(horizontal = DiscoverDimens.HighlightGap)) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.bodyLarge.merge(DiscoverType.HighlightTitle),
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            line?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium.merge(DiscoverType.HighlightLine),
                    color = MaterialTheme.colorScheme.secondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Icon(
            imageVector = WavvyIcons.NavigateNext,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.size(DiscoverDimens.TitleArrow)
        )
    }
}

// Placeholder of the page while it loads, the buttons, the highlight when the page has one and the first covers, each where the real one will be
@Composable
private fun HubSkeleton(hasHighlights: Boolean) {
    val shape = MaterialTheme.shapes.extraSmall

    SkeletonHost(modifier = Modifier.fillMaxWidth()) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(DiscoverDimens.HubChipGap),
            modifier = Modifier
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
                .padding(horizontal = DiscoverDimens.Side)
                .padding(bottom = if (hasHighlights) DiscoverDimens.HubBarGap else 0.dp)
        ) {
            repeat(2) { Box(modifier = Modifier.size(DiscoverDimens.SkeletonChipWidth, DiscoverDimens.SkeletonChipHeight).skeleton(CircleShape)) }
        }

        if (hasHighlights) {
            Box(
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
                    .padding(horizontal = DiscoverDimens.Side)
                    .fillMaxWidth()
                    .height(DiscoverDimens.HighlightCover + DiscoverDimens.HighlightPadding * 2)
                    .skeleton(RoundedCornerShape(DiscoverDimens.HighlightCorner))
            )
            Spacer(modifier = Modifier.height(DiscoverDimens.HighlightGap))
        }

        Box(
            modifier = Modifier
                .padding(horizontal = DiscoverDimens.Side, vertical = DiscoverDimens.TitleVertical)
                .width(DiscoverDimens.SkeletonShelfTitleWidth)
                .height(DiscoverDimens.SkeletonLine)
                .skeleton(shape)
        )

        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val cardWidth = coverWidth(maxWidth)

            Row(
                horizontalArrangement = Arrangement.spacedBy(DiscoverDimens.Gap),
                modifier = Modifier.padding(horizontal = DiscoverDimens.Side)
            ) {
                repeat(2) {
                    Column(modifier = Modifier.width(cardWidth)) {
                        Box(modifier = Modifier.fillMaxWidth().height(cardWidth).skeleton(RoundedCornerShape(DiscoverDimens.CoverCorner)))
                        Spacer(modifier = Modifier.height(DiscoverDimens.CardTextGap))
                        Box(modifier = Modifier.fillMaxWidth(DiscoverDimens.SkeletonTitleFraction).height(DiscoverDimens.SkeletonLine).skeleton(shape))
                        Spacer(modifier = Modifier.height(DiscoverDimens.CardTextGap))
                        Box(modifier = Modifier.fillMaxWidth(DiscoverDimens.SkeletonLineFraction).height(DiscoverDimens.SkeletonLine).skeleton(shape))
                    }
                }
            }
        }
    }
}

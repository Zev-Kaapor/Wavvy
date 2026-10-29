package com.wavvy.app.features.discover.ui

// Compose layouts and foundations
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
// Material 3 components
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
// Compose state and runtime
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
// UI styling and utilities
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
// Lifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
// Project resources
import com.wavvy.app.R
import com.wavvy.app.core.designsystem.icons.WavvyIcons
import com.wavvy.app.core.designsystem.theme.WavvyTheme
import com.wavvy.app.features.discover.data.DiscoverBlock
import com.wavvy.app.features.home.data.HomeItem
import com.wavvy.app.features.home.ui.components.HomeMessage
import com.wavvy.app.features.home.ui.rememberItemPlayer
import com.wavvy.app.features.player.ui.LocalMiniPlayerInset

// A page that opens from the Explore tab, a bar with the arrow and, once the big title scrolls away, the title in it
@Composable
fun ExploreScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ExploreViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()
    val onItemClick = rememberItemPlayer()

    // The roots of the groups, such as the new releases, and the charts have a page of their own
    if (viewModel.isHub) {
        ExploreHubPage(state = state, onBack = onBack, onRetry = viewModel::load, onItemClick = onItemClick, modifier = modifier)
        return
    }
    if (viewModel.isCharts) {
        ChartsPage(
            state = state,
            onBack = onBack,
            onRetry = viewModel::load,
            onCountry = viewModel::selectCountry,
            onItemClick = onItemClick,
            modifier = modifier
        )
        return
    }

    val listState = rememberLazyListState()
    val page = state.page
    val grid = page?.blocks?.singleOrNull() as? DiscoverBlock.Grid

    // A page that opens with the title of a group, such as the moods and genres, keeps less room under its big title
    val startsWithGroup = (page?.blocks?.firstOrNull() as? DiscoverBlock.MoodGrid)?.title?.isNotEmpty() == true

    // The title goes into the bar once the big one is mostly gone, a page without a big title has it in the bar from the start
    val threshold = with(LocalDensity.current) { DiscoverDimens.BarScrollThreshold.toPx() }
    val isTitleInBar by remember(state.isLarge) {
        derivedStateOf { !state.isLarge || listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > threshold }
    }
    val barAlpha by animateFloatAsState(
        targetValue = if (isTitleInBar) 1f else 0f,
        animationSpec = tween(DiscoverDimens.BarFadeMillis),
        label = "ExploreBar"
    )

    // The list starts under the bar, which floats over it
    val topInset = WindowInsets.safeDrawing.only(WindowInsetsSides.Top).asPaddingValues().calculateTopPadding()
    val contentPadding = PaddingValues(
        top = topInset + DiscoverDimens.BarHeight,
        bottom = LocalMiniPlayerInset.current + if (LocalMiniPlayerInset.current > 0.dp) DiscoverDimens.MiniPlayerClearance else 0.dp
    )

    // Solid, so the screen under it does not show through while the page slides
    Box(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        when {
            grid != null -> ExploreGrid(grid = grid, padding = contentPadding, onItemClick = onItemClick)

            else -> LazyColumn(state = listState, modifier = Modifier.fillMaxSize(), contentPadding = contentPadding) {
                if (state.isLarge) {
                    item(key = "title") {
                        Text(
                            text = state.title,
                            style = MaterialTheme.typography.headlineLarge.merge(DiscoverType.PageTitle),
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier
                                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
                                .padding(horizontal = DiscoverDimens.Side)
                                .then(if (state.title.contains(' ')) Modifier.widthIn(max = DiscoverDimens.PageTitleMaxWidth) else Modifier)
                                .padding(top = DiscoverDimens.PageTitleTop, bottom = if (startsWithGroup) DiscoverDimens.PageTitleBottomBeforeGroup else DiscoverDimens.PageTitleBottom)
                        )
                    }
                }

                when {
                    state.status == ExploreStatus.Loading -> item(key = "loading") {
                        Box(modifier = Modifier.fillMaxWidth().height(DiscoverDimens.LoadingHeight), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }

                    page == null -> item(key = "error") {
                        Box(modifier = Modifier.fillMaxWidth().height(DiscoverDimens.LoadingHeight)) {
                            HomeMessage(
                                text = stringResource(R.string.home_error),
                                actionLabel = stringResource(R.string.home_retry),
                                onAction = viewModel::load
                            )
                        }
                    }

                    else -> items(page.blocks.size, key = { "block_$it" }) { index ->
                        when (val block = page.blocks[index]) {
                            is DiscoverBlock.Featured -> ExploreFeatured(block = block, onItemClick = onItemClick)
                            is DiscoverBlock.Covers -> DiscoverCoverShelf(section = block.section, onItemClick = onItemClick, rows = block.rows)
                            is DiscoverBlock.Wide -> DiscoverWideShelf(section = block.section, onItemClick = onItemClick)
                            is DiscoverBlock.Ranked -> DiscoverRankedShelf(section = block.section, onItemClick = onItemClick)
                            is DiscoverBlock.Moods -> DiscoverMoodShelf(title = block.title, link = block.link, moods = block.moods)
                            is DiscoverBlock.MoodGrid -> ExploreMoodGrid(block = block)
                            is DiscoverBlock.Grid -> Unit
                        }
                        Spacer(modifier = Modifier.height(if (page.blocks[index] is DiscoverBlock.MoodGrid) DiscoverDimens.MoodGroupGap else DiscoverDimens.BlockGap))
                    }
                }
            }
        }

        ExploreBar(title = state.title, titleAlpha = barAlpha, onBack = onBack)
    }
}

// The bar on top of the page, the arrow always and the title as it comes in, with a back that comes in too so the list slides under it
@Composable
internal fun ExploreBar(
    title: String,
    titleAlpha: Float,
    onBack: () -> Unit,
    backgroundAlpha: Float = titleAlpha,
    // What sits at the end of the bar, such as the country of the charts
    trailing: (@Composable () -> Unit)? = null
) {
    val dimens = WavvyTheme.dimens

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background.copy(alpha = backgroundAlpha))
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))
            .height(DiscoverDimens.BarHeight)
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
            style = MaterialTheme.typography.titleLarge.merge(DiscoverType.BarTitle),
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .padding(start = dimens.spaceSmall)
                .graphicsLayer { alpha = titleAlpha }
        )

        if (trailing != null) {
            Spacer(modifier = Modifier.weight(1f))
            trailing()
        }
    }
}

// The moods and genres of a group, the buttons two by two under the title of the group
@Composable
internal fun ExploreMoodGrid(block: DiscoverBlock.MoodGrid) {
    Column(modifier = Modifier.fillMaxWidth()) {
        if (block.title.isNotEmpty()) DiscoverShelfTitle(title = block.title, link = null)

        Column(
            verticalArrangement = Arrangement.spacedBy(DiscoverDimens.Gap),
            modifier = Modifier
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
                .padding(horizontal = DiscoverDimens.Side)
        ) {
            block.moods.chunked(FeaturedColumns).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(DiscoverDimens.Gap)) {
                    row.forEach { mood -> MoodButton(mood = mood, modifier = Modifier.weight(1f)) }
                    // A row with one button leaves the room of the other
                    if (row.size < FeaturedColumns) Spacer(modifier = Modifier.weight((FeaturedColumns - row.size).toFloat()))
                }
            }
        }
    }
}

// The first covers of a page, two by two, they do not slide
@Composable
private fun ExploreFeatured(block: DiscoverBlock.Featured, onItemClick: (HomeItem) -> Unit) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val cardWidth = minOf((maxWidth - DiscoverDimens.Side * FeaturedGaps) / FeaturedColumns, DiscoverDimens.CoverMaxWidth)

        Column(
            verticalArrangement = Arrangement.spacedBy(DiscoverDimens.CoverRowGap),
            modifier = Modifier
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
                .padding(horizontal = DiscoverDimens.Side)
        ) {
            block.items.chunked(FeaturedColumns).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(DiscoverDimens.Gap)) {
                    row.forEach { item ->
                        if (block.isWide) {
                            WideCard(item = item, onClick = { onItemClick(item) }, modifier = Modifier.width(cardWidth))
                        } else {
                            CoverCard(item = item, onClick = { onItemClick(item) }, modifier = Modifier.width(cardWidth))
                        }
                    }
                }
            }
        }
    }
}

// How many covers sit side by side in the first ones of a page, and the gaps around them, the two sides and the one between
private const val FeaturedColumns = 2
private const val FeaturedGaps = 3

// A list page of everything, covers in two columns or wide cards in one, as the page of all the albums and singles is drawn
@Composable
private fun ExploreGrid(grid: DiscoverBlock.Grid, padding: PaddingValues, onItemClick: (HomeItem) -> Unit) {
    val side = PaddingValues(horizontal = DiscoverDimens.Side)

    if (grid.isWide) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = padding.calculateTopPadding(), bottom = padding.calculateBottomPadding()),
            verticalArrangement = Arrangement.spacedBy(DiscoverDimens.CoverRowGap)
        ) {
            items(grid.items.distinctBy { it.id }, key = { it.id }) { item ->
                WideCard(
                    item = item,
                    onClick = { onItemClick(item) },
                    modifier = Modifier
                        .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
                        .padding(side)
                )
            }
        }
    } else {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(DiscoverDimens.GridMinWidth),
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)),
            contentPadding = PaddingValues(
                start = DiscoverDimens.Side,
                top = padding.calculateTopPadding(),
                end = DiscoverDimens.Side,
                bottom = padding.calculateBottomPadding()
            ),
            horizontalArrangement = Arrangement.spacedBy(DiscoverDimens.Gap),
            verticalArrangement = Arrangement.spacedBy(DiscoverDimens.GridRowGap)
        ) {
            gridItems(grid.items.distinctBy { it.id }, key = { it.id }) { item ->
                CoverCard(item = item, onClick = { onItemClick(item) })
            }
        }
    }
}

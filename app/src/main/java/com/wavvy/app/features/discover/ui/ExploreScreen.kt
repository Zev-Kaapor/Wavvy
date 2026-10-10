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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
// Material 3 components
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
// Lifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
// Project resources
import com.wavvy.app.R
import com.wavvy.app.core.designsystem.components.OverlaySheet
import com.wavvy.app.core.designsystem.components.LocalSheetClose
import com.wavvy.app.core.designsystem.components.WavvySheet
import com.wavvy.app.core.designsystem.icons.WavvyIcons
import com.wavvy.app.core.designsystem.theme.WavvyTheme
import com.wavvy.app.features.discover.data.ChartCountries
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

                // The country of the charts, kept on screen while the charts of another one load
                page?.countries?.let { countries ->
                    item(key = "countries") {
                        CountryButton(
                            name = countries.selected,
                            onClick = {
                                OverlaySheet.show {
                                    CountrySheet(
                                        countries = countries,
                                        onSelect = viewModel::selectCountry,
                                        onDismiss = OverlaySheet::dismiss
                                    )
                                }
                            },
                            modifier = Modifier.padding(bottom = DiscoverDimens.BlockGap)
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

// The button that shows the country of the charts and opens the list of countries
@Composable
private fun CountryButton(name: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    // The arrow stays at the end of the button, the name at the start, and the button is as wide as the name asks for but never under its least width
    Box(
        contentAlignment = Alignment.CenterStart,
        modifier = modifier
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
            .padding(horizontal = DiscoverDimens.Side)
            .defaultMinSize(minWidth = DiscoverDimens.CountryButtonMinWidth)
            .height(DiscoverDimens.CountryButtonHeight)
            .clip(RoundedCornerShape(DiscoverDimens.CountryButtonCorner))
            .background(WavvyTheme.colors.chip)
            .clickable(onClick = onClick)
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.bodyLarge.merge(DiscoverType.Country),
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            modifier = Modifier.padding(start = DiscoverDimens.Side, end = DiscoverDimens.Side * 2 + DiscoverDimens.CountryChevron)
        )

        Icon(
            imageVector = WavvyIcons.ChevronDown,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = DiscoverDimens.Side)
        )
    }
}

// The list of countries, the one chosen has a check, a tap on another shows its charts
@Composable
private fun CountrySheet(countries: ChartCountries, onSelect: (String) -> Unit, onDismiss: () -> Unit) {
    WavvySheet(onDismiss = onDismiss) {
        val closeSheet = LocalSheetClose.current

        Column(modifier = Modifier.verticalScroll(rememberScrollState()).padding(bottom = WavvyTheme.dimens.spaceMedium)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = DiscoverDimens.Side, end = WavvyTheme.dimens.spaceSmall)
            ) {
                Text(
                    text = countries.title,
                    style = MaterialTheme.typography.bodyLarge.merge(DiscoverType.Country),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = closeSheet) {
                    Icon(
                        imageVector = WavvyIcons.Close,
                        contentDescription = stringResource(R.string.cd_close),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            countries.options.forEach { country ->
                if (country == null) {
                    HorizontalDivider(modifier = Modifier.padding(horizontal = DiscoverDimens.Side), color = MaterialTheme.colorScheme.outlineVariant)
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(DiscoverDimens.CountryRowHeight)
                            .clickable {
                                closeSheet()
                                onSelect(country.code)
                            }
                            .padding(horizontal = DiscoverDimens.Side)
                    ) {
                        Box(modifier = Modifier.width(DiscoverDimens.CountryCheckWidth)) {
                            if (country.isSelected) {
                                Icon(imageVector = WavvyIcons.Check, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                        Text(
                            text = country.name,
                            style = MaterialTheme.typography.bodyLarge.merge(DiscoverType.CountryOption),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

// The bar on top of the page, the arrow always and the title as it comes in, with a solid back so the list slides under it
@Composable
private fun ExploreBar(title: String, titleAlpha: Float, onBack: () -> Unit) {
    val dimens = WavvyTheme.dimens

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background.copy(alpha = titleAlpha))
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
    }
}

// The moods and genres of a group, the buttons two by two under the title of the group
@Composable
private fun ExploreMoodGrid(block: DiscoverBlock.MoodGrid) {
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
        val cardWidth = (maxWidth - DiscoverDimens.Side * FeaturedGaps) / FeaturedColumns

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
            columns = GridCells.Fixed(FeaturedColumns),
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

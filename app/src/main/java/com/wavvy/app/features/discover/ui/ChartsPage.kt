package com.wavvy.app.features.discover.ui

// Compose layouts and foundations
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
// Material 3 components
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
// Compose state and runtime
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
// Image loading
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import coil3.request.transformations
// Project resources
import com.wavvy.app.core.designsystem.components.rememberBackdropFade
import com.wavvy.app.R
import com.wavvy.app.core.designsystem.components.OverlaySheet
import com.wavvy.app.core.designsystem.components.SkeletonHost
import com.wavvy.app.core.designsystem.components.TrendMark
import com.wavvy.app.core.designsystem.components.skeleton
import com.wavvy.app.core.designsystem.icons.WavvyIcons
import com.wavvy.app.core.designsystem.theme.WavvyMotion
import com.wavvy.app.core.designsystem.theme.WavvyTheme
import com.wavvy.app.core.innertube.resize
import com.wavvy.app.features.discover.data.DiscoverBlock
import com.wavvy.app.features.home.data.HomeItem
import com.wavvy.app.features.home.data.HomeItemKind
import com.wavvy.app.features.home.ui.components.HomeDimens
import com.wavvy.app.features.home.ui.components.HomeMessage
import com.wavvy.app.features.home.ui.components.itemSubtitle
import com.wavvy.app.features.menu.ItemMenu
import com.wavvy.app.features.player.ui.LocalMiniPlayerInset
import com.wavvy.app.features.player.ui.components.BackdropBlur
import com.wavvy.app.features.player.ui.components.PlayerDimens
import com.wavvy.app.features.playlist.ui.SortChoice
import com.wavvy.app.features.playlist.ui.SortChoiceSheet

// The page of the charts, the charts as cards, the artists with the first three on a podium and the others in a list that goes all the way down,
// the country in the bar and the color of the first artist fading into the page as it scrolls
@Composable
internal fun ChartsPage(
    state: ExploreUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onCountry: (String) -> Unit,
    onItemClick: (HomeItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val listState = rememberLazyListState()
    val page = state.page
    val blocks = page?.blocks.orEmpty()
    val isLoading = state.status == ExploreStatus.Loading

    // How tall the first part of the page is, so the color of the first artist goes as that part scrolls away
    var topHeight by remember { mutableIntStateOf(0) }
    val backdropFade = rememberBackdropFade(listState) { topHeight * DiscoverDimens.HubBackdropFade }


    // The color of the first one of the list, blurred once into a small picture, as the page of an album has it
    val pageColor = MaterialTheme.colorScheme.background
    val isLight = pageColor.luminance() > 0.5f
    val coverUrl = blocks.filterIsInstance<DiscoverBlock.Ranked>().firstOrNull()?.section?.items?.firstOrNull()?.thumbnailUrl
        ?: blocks.filterIsInstance<DiscoverBlock.Covers>().firstOrNull()?.section?.items?.firstOrNull()?.thumbnailUrl
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
                    .background(Brush.verticalGradient(DiscoverDimens.HubBackdropHold to Color.Transparent, 1f to pageColor))
            )
        }

        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val isWide = maxWidth >= ChartsDimens.WideBreakpoint

            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    top = topInset + DiscoverDimens.BarHeight + DiscoverDimens.HubBarGap,
                    bottom = LocalMiniPlayerInset.current + if (LocalMiniPlayerInset.current > 0.dp) DiscoverDimens.MiniPlayerClearance else 0.dp
                )
            ) {
                when {
                    isLoading -> item(key = "loading") { ChartsSkeleton(isWide = isWide) }

                    page == null -> item(key = "error") {
                        Box(modifier = Modifier.fillMaxWidth().height(DiscoverDimens.LoadingHeight)) {
                            HomeMessage(
                                text = stringResource(R.string.home_error),
                                actionLabel = stringResource(R.string.home_retry),
                                onAction = onRetry
                            )
                        }
                    }

                    else -> blocks.forEachIndexed { blockIndex, block ->
                        when (block) {
                            is DiscoverBlock.Covers -> chartCards(blockIndex, block, isWide, onItemClick, onFirstHeight = { if (blockIndex == 0) topHeight = it })
                            is DiscoverBlock.Ranked -> ranking(blockIndex, block, isWide, onItemClick, onFirstHeight = { if (blockIndex == 0) topHeight = it })
                            else -> Unit
                        }
                    }
                }
            }
        }

        // The country sits in the bar, at its end, and its list opens over the page
        ExploreBar(
            title = state.title,
            titleAlpha = 1f,
            onBack = onBack,
            backgroundAlpha = barAlpha,
            trailing = page?.countries?.let { countries ->
                {
                    CountryPill(
                        name = countries.selected,
                        onClick = {
                            OverlaySheet.show {
                                SortChoiceSheet(
                                    title = countries.title,
                                    choices = countries.options.map { country -> country?.let { SortChoice(it.name, it.isSelected) { onCountry(it.code) } } },
                                    onDismiss = OverlaySheet::dismiss
                                )
                            }
                        }
                    )
                }
            }
        )
    }
}

// The charts of the page as cards, one under the other, or in columns on a wide screen
private fun LazyListScope.chartCards(
    blockIndex: Int,
    block: DiscoverBlock.Covers,
    isWide: Boolean,
    onItemClick: (HomeItem) -> Unit,
    onFirstHeight: (Int) -> Unit
) {
    val columns = if (isWide) ChartsDimens.WideChartColumns else 1
    val rows = block.section.items.distinctBy { it.id }.chunked(columns)

    item(key = "charts_title_$blockIndex") {
        Box(modifier = Modifier.onSizeChanged { onFirstHeight(it.height) }) {
            DiscoverShelfTitle(title = block.section.title, link = null)
        }
    }

    items(rows.size, key = { "charts_${blockIndex}_$it" }) { row ->
        Row(
            horizontalArrangement = Arrangement.spacedBy(ChartsDimens.ColumnGap),
            modifier = Modifier
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
                .padding(horizontal = ChartsDimens.Side)
                .padding(bottom = ChartsDimens.ChartCardGap)
        ) {
            val cards = rows[row]
            cards.forEach { chart -> ChartCard(item = chart, onClick = { onItemClick(chart) }, modifier = Modifier.weight(1f)) }
            repeat(columns - cards.size) { Spacer(modifier = Modifier.weight(1f)) }
        }
    }

    item(key = "charts_gap_$blockIndex") { Spacer(modifier = Modifier.height(ChartsDimens.SectionGap)) }
}

// A list of the ranking, the first three artists on a podium and the rest as rows, two columns on a wide screen
private fun LazyListScope.ranking(
    blockIndex: Int,
    block: DiscoverBlock.Ranked,
    isWide: Boolean,
    onItemClick: (HomeItem) -> Unit,
    onFirstHeight: (Int) -> Unit
) {
    val entries = block.section.items.distinctBy { it.id }
    val hasPodium = entries.size >= PodiumSize && entries.take(PodiumSize).all { it.kind == HomeItemKind.Artist }
    val rows = entries.withIndex().toList().drop(if (hasPodium) PodiumSize else 0)
    val columns = if (isWide) ChartsDimens.WideRankColumns else 1

    item(key = "ranking_title_$blockIndex") {
        Box(modifier = Modifier.onSizeChanged { onFirstHeight(it.height) }) {
            DiscoverShelfTitle(title = block.section.title, link = block.section.link)
        }
    }

    if (hasPodium) {
        item(key = "podium_$blockIndex") {
            Podium(entries = entries.take(PodiumSize), onItemClick = onItemClick)
            Spacer(modifier = Modifier.height(ChartsDimens.SectionGap))
        }
    }

    val grouped = rows.chunked(columns)
    items(grouped.size, key = { "ranking_${blockIndex}_$it" }) { index ->
        Row(
            horizontalArrangement = Arrangement.spacedBy(ChartsDimens.ColumnGap),
            modifier = Modifier
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
                .padding(horizontal = ChartsDimens.Side)
        ) {
            val group = grouped[index]
            group.forEach { (position, item) -> RankRow(rank = position + 1, item = item, onClick = { onItemClick(item) }, modifier = Modifier.weight(1f)) }
            repeat(columns - group.size) { Spacer(modifier = Modifier.weight(1f)) }
        }
    }
}

// How many artists stand on the podium
private const val PodiumSize = 3

// A chart as a card, its cover, its name and who makes it, and the arrow that opens it
@Composable
private fun ChartCard(item: HomeItem, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val line = item.lineText ?: itemSubtitle(item)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clip(RoundedCornerShape(ChartsDimens.ChartCorner))
            .background(WavvyTheme.colors.menuCard)
            .clickable(onClick = onClick)
            .padding(ChartsDimens.ChartPadding)
    ) {
        AsyncImage(
            model = item.thumbnailUrl?.resize(HomeDimens.CoverRequestSize, HomeDimens.CoverRequestSize),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(ChartsDimens.ChartCover)
                .clip(RoundedCornerShape(ChartsDimens.ChartCoverCorner))
                .background(MaterialTheme.colorScheme.surfaceContainer)
        )

        Column(modifier = Modifier.weight(1f).padding(horizontal = ChartsDimens.ChartGap)) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.bodyLarge.merge(ChartsType.ChartTitle),
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            line?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium.merge(ChartsType.ChartLine),
                    color = MaterialTheme.colorScheme.secondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Icon(
            imageVector = WavvyIcons.NavigateNext,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.size(ChartsDimens.ChartArrow)
        )
    }
}

// The first three artists, the second and the third at the sides of the first, which is bigger and has the ring of the brand
@Composable
private fun Podium(entries: List<HomeItem>, onItemClick: (HomeItem) -> Unit) {
    // The first stands in the middle, so the order is second, first, third
    val order = listOf(1, 0, 2)

    Box(
        contentAlignment = Alignment.TopCenter,
        modifier = Modifier
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
            .fillMaxWidth()
            .padding(horizontal = ChartsDimens.Side)
    ) {
        // The photos hang from the top, so the second and the third are level with each other and lower than the first
        Row(verticalAlignment = Alignment.Top, modifier = Modifier.widthIn(max = ChartsDimens.PodiumMaxWidth)) {
            order.forEach { place ->
                PodiumArtist(
                    rank = place + 1,
                    item = entries[place],
                    isFirst = place == 0,
                    onClick = { onItemClick(entries[place]) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

// An artist of the podium, the round photo with the mark of its place, the name, how many follow it, how it moved and its menu, all on one line each for the three
@Composable
private fun PodiumArtist(rank: Int, item: HomeItem, isFirst: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val size = if (isFirst) ChartsDimens.PodiumFirst else ChartsDimens.PodiumOther
    val ring = WavvyTheme.colors.let { Brush.linearGradient(listOf(it.gradientStart, it.gradientMiddle, it.gradientEnd)) }

    // The second and the third stand lower than the first by what its photo and its ring have more
    val drop = if (isFirst) 0.dp else ChartsDimens.PodiumFirst + (ChartsDimens.PodiumRing + ChartsDimens.PodiumRingGap) * 2 - ChartsDimens.PodiumOther

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .padding(top = drop)
            .clip(RoundedCornerShape(ChartsDimens.ChartCorner))
            .combinedClickable(onClick = onClick, onLongClick = { ItemMenu.show(item) })
    ) {
        Box(contentAlignment = Alignment.BottomEnd) {
            AsyncImage(
                model = item.thumbnailUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .then(if (isFirst) Modifier.border(ChartsDimens.PodiumRing, ring, CircleShape).padding(ChartsDimens.PodiumRing + ChartsDimens.PodiumRingGap) else Modifier)
                    .size(size)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainer)
            )

            // The place on a small round mark over the corner of the photo
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(ChartsDimens.PodiumBadge)
                    .border(ChartsDimens.PodiumBadgeBorder, MaterialTheme.colorScheme.background, CircleShape)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onBackground)
            ) {
                Text(
                    text = rank.toString(),
                    style = MaterialTheme.typography.labelMedium.merge(ChartsType.PodiumPlace),
                    color = MaterialTheme.colorScheme.background
                )
            }
        }

        Spacer(modifier = Modifier.height(ChartsDimens.PodiumNameGap))

        // The name and how many follow it on the start, which scroll when the name is long, and the menu on the end
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = ChartsDimens.PodiumTextSide)) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.bodyMedium.merge(ChartsType.RowName),
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    modifier = Modifier.fillMaxWidth().basicMarquee()
                )
                (item.lineText ?: itemSubtitle(item))?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium.merge(ChartsType.RowLine),
                        color = MaterialTheme.colorScheme.secondary,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        modifier = Modifier.fillMaxWidth().basicMarquee()
                    )
                }

                // How it moved keeps its place whether there is a mark or not, so the three columns are as tall as each other
                Box(contentAlignment = Alignment.Center, modifier = Modifier.height(ChartsDimens.PodiumTrendSlot)) {
                    item.trend?.let { TrendMark(it, ChartsDimens.PodiumTrend) }
                }
            }

            IconButton(onClick = { ItemMenu.show(item) }, modifier = Modifier.size(ChartsDimens.PodiumMenu)) {
                Icon(
                    imageVector = WavvyIcons.MoreVertical,
                    contentDescription = stringResource(R.string.cd_more),
                    tint = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.size(ChartsDimens.PodiumMenuIcon)
                )
            }
        }
    }
}

// A row of the list, the place with how it moved, the picture, the name with its line and the menu
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RankRow(rank: Int, item: HomeItem, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .height(ChartsDimens.RowHeight)
            .combinedClickable(onClick = onClick, onLongClick = { ItemMenu.show(item) })
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(ChartsDimens.RowRank)) {
            Text(
                text = rank.toString(),
                style = MaterialTheme.typography.titleMedium.merge(ChartsType.Place),
                color = MaterialTheme.colorScheme.secondary,
                textAlign = TextAlign.Center
            )
            item.trend?.let { TrendMark(it, ChartsDimens.RowTrend) }
        }

        AsyncImage(
            model = item.thumbnailUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(ChartsDimens.RowPicture)
                .clip(if (item.kind == HomeItemKind.Artist) CircleShape else RoundedCornerShape(ChartsDimens.RowCorner))
                .background(MaterialTheme.colorScheme.surfaceContainer)
        )

        Column(modifier = Modifier.weight(1f).padding(horizontal = WavvyTheme.dimens.spaceMedium)) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.bodyMedium.merge(ChartsType.RowName),
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            (item.lineText ?: itemSubtitle(item))?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium.merge(ChartsType.RowLine),
                    color = MaterialTheme.colorScheme.secondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        IconButton(onClick = { ItemMenu.show(item) }) {
            Icon(
                imageVector = WavvyIcons.MoreVertical,
                contentDescription = stringResource(R.string.cd_more),
                tint = MaterialTheme.colorScheme.onBackground
            )
        }
    }
}

// The country of the charts, a small button with its name and an arrow that opens the list of countries
@Composable
private fun CountryPill(name: String, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .padding(end = ChartsDimens.BarEnd)
            .height(ChartsDimens.PillHeight)
            .clip(RoundedCornerShape(ChartsDimens.PillCorner))
            .background(WavvyTheme.colors.chip)
            .clickable(onClick = onClick)
            .padding(horizontal = ChartsDimens.PillPadding)
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.bodyMedium.merge(ChartsType.Country),
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1
        )
        Spacer(modifier = Modifier.width(ChartsDimens.PillGap))
        Icon(
            imageVector = WavvyIcons.ChevronDown,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.size(ChartsDimens.PillChevron)
        )
    }
}

// Placeholder of the page while it loads, the title and the three charts, the title of the artists with their podium and the first rows, each where the real one will be
@Composable
private fun ChartsSkeleton(isWide: Boolean) {
    val shape = MaterialTheme.shapes.extraSmall
    val columns = if (isWide) ChartsDimens.WideChartColumns else 1

    SkeletonHost(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .padding(horizontal = DiscoverDimens.Side, vertical = DiscoverDimens.TitleVertical)
                .width(ChartsDimens.SkeletonTitleWidth)
                .height(ChartsDimens.SkeletonLine)
                .skeleton(shape)
        )

        (0 until ChartsDimens.SkeletonCharts).chunked(columns).forEach { row ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(ChartsDimens.ColumnGap),
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
                    .padding(horizontal = ChartsDimens.Side)
                    .padding(bottom = ChartsDimens.ChartCardGap)
            ) {
                row.forEach {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(ChartsDimens.ChartCover + ChartsDimens.ChartPadding * 2)
                            .skeleton(RoundedCornerShape(ChartsDimens.ChartCorner))
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(ChartsDimens.SectionGap))

        Box(
            modifier = Modifier
                .padding(horizontal = DiscoverDimens.Side, vertical = DiscoverDimens.TitleVertical)
                .width(ChartsDimens.SkeletonTitleWidth)
                .height(ChartsDimens.SkeletonLine)
                .skeleton(shape)
        )

        Box(contentAlignment = Alignment.TopCenter, modifier = Modifier.fillMaxWidth().padding(horizontal = ChartsDimens.Side)) {
            Row(verticalAlignment = Alignment.Top, modifier = Modifier.widthIn(max = ChartsDimens.PodiumMaxWidth)) {
                listOf(ChartsDimens.PodiumOther, ChartsDimens.PodiumFirst, ChartsDimens.PodiumOther).forEach { size ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                        if (size == ChartsDimens.PodiumOther) Spacer(modifier = Modifier.height(ChartsDimens.PodiumFirst - ChartsDimens.PodiumOther))
                        Box(modifier = Modifier.size(size).skeleton(CircleShape))
                        Spacer(modifier = Modifier.height(ChartsDimens.PodiumNameGap))
                        Box(modifier = Modifier.fillMaxWidth(ChartsDimens.SkeletonNameFraction).height(ChartsDimens.SkeletonLine).skeleton(shape))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(ChartsDimens.SectionGap))

        repeat(ChartsDimens.SkeletonRows) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
                    .padding(horizontal = ChartsDimens.Side)
                    .height(ChartsDimens.RowHeight)
            ) {
                Box(modifier = Modifier.width(ChartsDimens.RowRank), contentAlignment = Alignment.Center) {
                    Box(modifier = Modifier.size(ChartsDimens.SkeletonLine).skeleton(shape))
                }
                Box(modifier = Modifier.size(ChartsDimens.RowPicture).skeleton(CircleShape))
                Column(modifier = Modifier.weight(1f).padding(horizontal = WavvyTheme.dimens.spaceMedium)) {
                    Box(modifier = Modifier.fillMaxWidth(ChartsDimens.SkeletonNameFraction).height(ChartsDimens.SkeletonLine).skeleton(shape))
                    Spacer(modifier = Modifier.height(ChartsDimens.PodiumTrend))
                    Box(modifier = Modifier.fillMaxWidth(ChartsDimens.SkeletonLineFraction).height(ChartsDimens.SkeletonLine).skeleton(shape))
                }
            }
        }
    }
}

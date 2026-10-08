package com.wavvy.app.features.home.ui.components

// Compose layouts and foundations
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
// Material 3 components
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
// Compose state and runtime
import androidx.compose.runtime.Composable
// UI styling and utilities
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
// Project resources
import com.wavvy.app.core.designsystem.icons.WavvyIcons
import com.wavvy.app.core.designsystem.theme.WavvyTheme
import com.wavvy.app.core.innertube.resize
import com.wavvy.app.features.home.data.HomeItem
import com.wavvy.app.features.home.data.HomeItemKind
import com.wavvy.app.features.menu.ItemMenu

// Speed dial as Metrolist (GPL-3.0) draws it, pages of square tiles that slide sideways with a tile that picks at random at the end of the first page
@Composable
fun HomeSpeedDial(
    items: List<HomeItem>,
    onItemClick: (HomeItem) -> Unit,
    onRandomClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val availableWidth = maxWidth - HomeDimens.SpeedDialMargin
        val columns = (availableWidth / HomeDimens.SpeedDialTile).toInt().coerceAtLeast(HomeDimens.SpeedDialMinColumns)
        val rows = when {
            columns >= HomeDimens.SpeedDialWideColumns -> 1
            columns >= HomeDimens.SpeedDialMidColumns -> 2
            else -> 3
        }
        val itemsPerPage = columns * rows
        val tileSize = availableWidth / columns
        val pagerState = rememberPagerState(pageCount = { (items.size + itemsPerPage - 1) / itemsPerPage })

        Column(modifier = Modifier.fillMaxWidth()) {
            HorizontalPager(
                state = pagerState,
                contentPadding = PaddingValues(horizontal = HomeDimens.SpeedDialPagePadding),
                pageSpacing = HomeDimens.SpeedDialPageSpacing,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(tileSize * rows)
            ) { page ->
                val pageItems = items.drop(page * itemsPerPage).take(itemsPerPage)

                Column(modifier = Modifier.fillMaxSize()) {
                    for (row in 0 until rows) {
                        Row(modifier = Modifier.fillMaxWidth()) {
                            for (column in 0 until columns) {
                                val index = row * columns + column

                                when {
                                    // The last slot of the first page picks something at random
                                    page == 0 && index == itemsPerPage - 1 -> Box(
                                        modifier = Modifier
                                            .size(tileSize)
                                            .padding(HomeDimens.SpeedDialTilePadding)
                                    ) {
                                        RandomTile(onClick = onRandomClick)
                                    }

                                    index < pageItems.size -> Box(
                                        modifier = Modifier
                                            .size(tileSize)
                                            .padding(HomeDimens.SpeedDialTilePadding)
                                    ) {
                                        val item = pageItems[index]
                                        SpeedDialTile(
                                            item = item,
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .combinedClickable(onClick = { onItemClick(item) }, onLongClick = { ItemMenu.show(item) })
                                        )
                                    }

                                    else -> Spacer(modifier = Modifier.width(tileSize))
                                }
                            }
                        }
                    }
                }
            }

            if (pagerState.pageCount > 1) {
                Row(
                    modifier = Modifier
                        .height(HomeDimens.SpeedDialIndicatorHeight)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(pagerState.pageCount) { page ->
                        val color = if (pagerState.currentPage == page) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = HomeDimens.SpeedDialDotAlpha)
                        }

                        Box(
                            modifier = Modifier
                                .padding(HomeDimens.SpeedDialDotPadding)
                                .clip(CircleShape)
                                .background(color)
                                .size(HomeDimens.SpeedDialDot)
                        )
                    }
                }
            }
        }
    }
}

// Square tile with the cover, a dark that fades in from the top and the bottom, and the title with an arrow when it opens a page
@Composable
private fun SpeedDialTile(
    item: HomeItem,
    modifier: Modifier = Modifier
) {
    val scrim = WavvyTheme.colors.tileScrim
    val onMedia = WavvyTheme.colors.onMedia

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(HomeDimens.CoverCorner))
    ) {
        ItemThumbnail(
            url = item.thumbnailUrl?.resize(HomeDimens.SpeedDialRequestSize, HomeDimens.SpeedDialRequestSize),
            shape = coverShape(item),
            modifier = Modifier.fillMaxSize()
        )

        // Dark that keeps the title readable and the icons visible over bright covers
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            scrim.copy(alpha = HomeDimens.SpeedDialScrimTop),
                            Color.Transparent,
                            scrim.copy(alpha = HomeDimens.SpeedDialScrimMiddle),
                            scrim.copy(alpha = HomeDimens.SpeedDialScrimBottom)
                        )
                    )
                )
        )

        // The badges come after the dark, so it does not dim them
        CoverBadges(isVideo = item.isVideo, isPinned = item.id in LocalPinnedIds.current)

        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(HomeDimens.SpeedDialTitlePadding)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleSmall.merge(HomeType.SpeedDialTitle),
                color = onMedia,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            if (item.kind != HomeItemKind.Song) {
                Icon(
                    imageVector = WavvyIcons.NavigateNext,
                    contentDescription = null,
                    tint = onMedia,
                    modifier = Modifier.size(HomeDimens.SpeedDialArrow)
                )
            }
        }
    }
}

// Tile with five dots like the face of a die, the one that picks something at random
@Composable
private fun RandomTile(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dotColor = MaterialTheme.colorScheme.onSecondaryContainer

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(HomeDimens.CoverCorner))
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .clickable(onClick = onClick)
    ) {
        // Corners of the face and its middle
        listOf(-1 to -1, 1 to -1, 0 to 0, -1 to 1, 1 to 1).forEach { (x, y) ->
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset { IntOffset((HomeDimens.RandomDotOffset * x).roundToPx(), (HomeDimens.RandomDotOffset * y).roundToPx()) }
                    .size(HomeDimens.RandomDot)
                    .clip(CircleShape)
                    .background(dotColor)
            )
        }
    }
}

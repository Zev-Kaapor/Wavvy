package com.wavvy.app.features.discover.ui

// Compose animation
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
// Compose layouts and foundations
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
// Material 3 components
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
// Compose state and runtime
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
// UI styling and utilities
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
// Project resources
import com.wavvy.app.R
import com.wavvy.app.core.designsystem.components.SkeletonHost
import com.wavvy.app.core.designsystem.components.skeleton
import com.wavvy.app.core.designsystem.icons.WavvyIcons
import com.wavvy.app.core.designsystem.theme.WavvyMotion
import com.wavvy.app.features.discover.data.DiscoverBlock
import com.wavvy.app.features.home.ui.components.HomeMessage
import com.wavvy.app.features.player.ui.LocalMiniPlayerInset
import com.wavvy.app.features.podcast.ui.PodcastSearchField

// The page of the moods and genres, the groups of buttons with the colored stripe as the pages of the Explore tab draw them,
// a search that opens from the bar as the one of the library does, and the arrow and the title fixed in a bar that gets its back as the page scrolls
@Composable
internal fun MoodsPage(
    state: ExploreUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val page = state.page
    var isSearching by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }

    // The groups with what the search leaves of them, a group with none is not shown
    val words = query.trim()
    val groups = page?.blocks.orEmpty().filterIsInstance<DiscoverBlock.MoodGrid>().mapNotNull { group ->
        val found = group.moods.filter { words.isEmpty() || it.title.contains(words, ignoreCase = true) }
        if (found.isEmpty()) null else group.copy(moods = found)
    }

    // The arrow and the title stay in place, the back of the bar comes in as the page goes under it
    val barScroll = with(LocalDensity.current) { DiscoverDimens.HubBarScroll.toPx() }
    val barAlpha by remember {
        derivedStateOf { if (listState.firstVisibleItemIndex > 0) 1f else (listState.firstVisibleItemScrollOffset / barScroll).coerceIn(0f, 1f) }
    }
    val topInset = WindowInsets.safeDrawing.only(WindowInsetsSides.Top).asPaddingValues().calculateTopPadding()
    val canSearch = page != null && state.status != ExploreStatus.Loading

    // Solid, so the screen under it does not show through while the page slides
    Box(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val isLandscape = maxWidth > maxHeight
            val skeletonColumns = ((maxWidth - DiscoverDimens.Side * 2 + DiscoverDimens.Gap) / (DiscoverDimens.MoodMinWidth + DiscoverDimens.Gap)).toInt().coerceAtLeast(2)

            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize().then(if (isLandscape) Modifier.imePadding() else Modifier),
                contentPadding = PaddingValues(
                    top = topInset + DiscoverDimens.BarHeight + DiscoverDimens.HubBarGap,
                    bottom = LocalMiniPlayerInset.current + if (LocalMiniPlayerInset.current > 0.dp) DiscoverDimens.MiniPlayerClearance else 0.dp
                )
            ) {
                when {
                    state.status == ExploreStatus.Loading -> item(key = "loading") { MoodsSkeleton(columns = skeletonColumns) }

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
                        // The field comes in from the bar with a fade and the page opens room for it
                        item(key = "search") {
                            AnimatedVisibility(
                                visible = isSearching,
                                enter = fadeIn(tween(WavvyMotion.ExpandMillis)) + expandVertically(tween(WavvyMotion.ExpandMillis)),
                                exit = fadeOut(tween(WavvyMotion.ExpandMillis)) + shrinkVertically(tween(WavvyMotion.ExpandMillis))
                            ) {
                                PodcastSearchField(
                                    query = query,
                                    onQueryChange = { query = it },
                                    onClose = {
                                        query = ""
                                        isSearching = false
                                    },
                                    hint = stringResource(R.string.explore_moods_search)
                                )
                            }
                        }

                        if (groups.isEmpty()) {
                            item(key = "empty") {
                                Box(modifier = Modifier.fillMaxWidth().height(DiscoverDimens.LoadingHeight)) {
                                    HomeMessage(text = stringResource(R.string.explore_moods_empty))
                                }
                            }
                        }

                        groups.forEachIndexed { groupIndex, group ->
                            item(key = "group_$groupIndex") {
                                ExploreMoodGrid(block = group)
                                Spacer(modifier = Modifier.height(DiscoverDimens.MoodGroupGap))
                            }
                        }
                    }
                }
            }
        }

        // The search opens from the bar, its icon is there while the field is not
        ExploreBar(
            title = state.title,
            titleAlpha = 1f,
            onBack = onBack,
            backgroundAlpha = barAlpha,
            trailing = if (canSearch && !isSearching) {
                {
                    IconButton(onClick = { isSearching = true }) {
                        Icon(
                            imageVector = WavvyIcons.Search,
                            contentDescription = stringResource(R.string.explore_moods_search),
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
            } else {
                null
            }
        )
    }
}

// Placeholder of the page while it loads, the title of a group with its buttons and the title of the next with its buttons, each where the real one will be
@Composable
private fun MoodsSkeleton(columns: Int) {
    val shape = MaterialTheme.shapes.extraSmall

    SkeletonHost(modifier = Modifier.fillMaxWidth()) {
        listOf(SkeletonFirstRows, SkeletonSecondRows).forEachIndexed { index, rows ->
            Box(
                modifier = Modifier
                    .padding(horizontal = DiscoverDimens.Side, vertical = DiscoverDimens.TitleVertical)
                    .width(DiscoverDimens.SkeletonShelfTitleWidth)
                    .height(DiscoverDimens.SkeletonLine)
                    .skeleton(shape)
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(DiscoverDimens.Gap),
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
                    .padding(horizontal = DiscoverDimens.Side)
            ) {
                repeat(rows) {
                    Row(horizontalArrangement = Arrangement.spacedBy(DiscoverDimens.Gap)) {
                        repeat(columns) {
                            Box(modifier = Modifier.weight(1f).height(DiscoverDimens.MoodHeight).skeleton(RoundedCornerShape(DiscoverDimens.MoodCorner)))
                        }
                    }
                }
            }

            if (index == 0) Spacer(modifier = Modifier.height(DiscoverDimens.MoodGroupGap))
        }
    }
}

// How many rows of buttons the placeholder has in the first group and in the second
private const val SkeletonFirstRows = 5
private const val SkeletonSecondRows = 3

package com.wavvy.app.features.discover.ui

// Compose layouts and foundations
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
// Material 3 components
import androidx.compose.material3.MaterialTheme
// Compose state and runtime
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
// UI styling and utilities
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
// Project resources
import com.wavvy.app.features.home.data.HomeSection
import com.wavvy.app.features.home.ui.rememberItemPlayer
import com.wavvy.app.features.player.ui.LocalMiniPlayerInset

// The shelf whose covers all show on the page that opens from its title, held outside the screens because the covers of a long shelf do not fit in a route
object ShelfStore {
    var section: HomeSection? = null
}

// All the covers of a shelf in a grid, with the bar that keeps the arrow and the name of the shelf, each cover opens and has its button that plays it
@Composable
fun ShelfScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val section = ShelfStore.section

    // The shelf is lost when the app was closed while the page was open, the page has nothing to show and goes back
    LaunchedEffect(section) { if (section == null) onBack() }
    if (section == null) return

    val player = rememberCoverPlayer()
    val onItemClick = rememberItemPlayer()
    val gridState = rememberLazyGridState()

    // The arrow and the name stay in place, the back of the bar comes in as the covers go under it
    val barScroll = with(LocalDensity.current) { DiscoverDimens.HubBarScroll.toPx() }
    val barAlpha by remember {
        derivedStateOf { if (gridState.firstVisibleItemIndex > 0) 1f else (gridState.firstVisibleItemScrollOffset / barScroll).coerceIn(0f, 1f) }
    }
    val topInset = WindowInsets.safeDrawing.only(WindowInsetsSides.Top).asPaddingValues().calculateTopPadding()

    // Solid, so the screen under it does not show through while the page slides
    Box(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        LazyVerticalGrid(
            state = gridState,
            columns = GridCells.Adaptive(DiscoverDimens.CategoryCardMin),
            contentPadding = PaddingValues(
                start = DiscoverDimens.Side,
                top = topInset + DiscoverDimens.BarHeight + DiscoverDimens.HubBarGap,
                end = DiscoverDimens.Side,
                bottom = LocalMiniPlayerInset.current + if (LocalMiniPlayerInset.current > 0.dp) DiscoverDimens.MiniPlayerClearance else 0.dp
            ),
            horizontalArrangement = Arrangement.spacedBy(DiscoverDimens.Gap),
            verticalArrangement = Arrangement.spacedBy(DiscoverDimens.CategoryRowGap),
            modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
        ) {
            items(section.items.distinctBy { it.id }, key = { it.id }) { item ->
                CategoryCard(item = item, isLoading = player.playingId == item.id, onPlay = { player.play(item) }, onClick = { onItemClick(item) })
            }
        }

        ExploreBar(title = section.title, titleAlpha = 1f, onBack = onBack, backgroundAlpha = barAlpha)
    }
}

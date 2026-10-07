package com.wavvy.app.features.home.ui.components

// Android configuration
import android.content.res.Configuration
// Compose animation
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
// Compose layouts and foundations
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
// Compose state and runtime
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
// UI utilities
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
// Project resources
import com.wavvy.app.features.home.data.HomeItem

// Songs whose cover already showed, so they do not fade in again when the row comes back, kept while the app runs
private val shownCovers = mutableSetOf<String>()

// Row of the songs listened to, as in the old Wavvy, a new song fades in at the start while the others slide over to make room,
// the one that falls off the end fades out, and the row goes back to the start whenever it changes
@Composable
fun HomeRecentRow(
    items: List<HomeItem>,
    onItemClick: (HomeItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    val shown = items.distinctBy { it.id }.take(if (isLandscape) HomeDimens.RecentMaxItems else HomeDimens.RecentPortraitItems)
    val listState = rememberLazyListState()

    // A song that comes in takes the first place, so the row is brought back to it
    LaunchedEffect(shown.map { it.id }) {
        if (shown.isNotEmpty()) listState.animateScrollToItem(0)
    }

    LazyRow(
        state = listState,
        modifier = modifier,
        contentPadding = WindowInsets.systemBars.only(WindowInsetsSides.Horizontal).asPaddingValues()
    ) {
        items(shown, key = { it.id }) { item ->
            RecentCard(
                item = item,
                onClick = { onItemClick(item) },
                modifier = Modifier.animateItem(
                    fadeInSpec = tween(HomeDimens.RecentFadeMillis),
                    fadeOutSpec = tween(HomeDimens.RecentFadeMillis),
                    placementSpec = spring(stiffness = Spring.StiffnessMediumLow)
                )
            )
        }
    }
}

// A card that stays hidden until its cover arrives and then fades in slowly, only the first time it shows
@Composable
private fun RecentCard(
    item: HomeItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isLoaded by remember(item.id) { mutableStateOf(item.id in shownCovers) }
    val alpha = animateFloatAsState(
        targetValue = if (isLoaded || item.thumbnailUrl.isNullOrBlank()) 1f else 0f,
        animationSpec = tween(HomeDimens.RecentContentFadeMillis),
        label = "RecentCardFade"
    )

    HomeGridItem(
        item = item,
        onClick = onClick,
        // The fade is read while drawing, so it never builds the card again
        modifier = modifier.graphicsLayer { this.alpha = alpha.value },
        onCoverLoaded = {
            isLoaded = true
            shownCovers.add(item.id)
        }
    )
}

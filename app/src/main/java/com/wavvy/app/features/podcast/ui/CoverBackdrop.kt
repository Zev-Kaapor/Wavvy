package com.wavvy.app.features.podcast.ui

// Compose layouts and foundations
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyListState
// Material 3 components
import androidx.compose.material3.MaterialTheme
// Compose state and runtime
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
// UI styling and utilities
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
// Image loading
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import coil3.request.transformations
// Project resources
import com.wavvy.app.core.designsystem.components.rememberBackdropFade
import com.wavvy.app.core.designsystem.theme.WavvyMotion
import com.wavvy.app.core.innertube.resize
import com.wavvy.app.features.discover.ui.DiscoverDimens
import com.wavvy.app.features.collection.ui.CollectionDimens
import com.wavvy.app.features.player.ui.components.BackdropBlur
import com.wavvy.app.features.player.ui.components.PlayerDimens

// The cover blurred once into a small picture, the color that fades into the page as the list goes down and comes back as it goes up,
// the same one the page of an album and of a playlist have
@Composable
internal fun CoverBackdrop(coverUrl: String?, listState: LazyListState, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val pageColor = MaterialTheme.colorScheme.background
    val isLight = pageColor.luminance() > 0.5f
    val backdropFade = rememberBackdropFade(listState) { listState.layoutInfo.viewportSize.height * CollectionDimens.BackdropFadeFraction }

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

    Box(
        modifier = modifier
            .fillMaxWidth()
            .fillMaxHeight(CollectionDimens.BackdropFraction)
            .graphicsLayer {
                alpha = backdropFade()
            }
    ) {
        if (backdrop != null) {
            AsyncImage(
                model = backdrop,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().alpha(if (isLight) CollectionDimens.BackdropLightAlpha else 1f)
            )
        }

        // A little dark on top, only on the dark theme, and the bottom of the blur fades into the page
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to if (isLight) Color.Transparent else Color.Black.copy(alpha = CollectionDimens.BackdropTopShade),
                        CollectionDimens.BackdropHold to Color.Transparent,
                        1f to pageColor
                    )
                )
        )
    }
}

// The color of the first picture of a page of the Explore tab as the charts have it, blurred once and put on top, which fades into the page as the top of it scrolls away
// With no picture yet the color of the accent takes its place
@Composable
internal fun HubBackdrop(coverUrl: String?, accent: Color, fade: () -> Float, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val pageColor = MaterialTheme.colorScheme.background
    val isLight = pageColor.luminance() > 0.5f

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
    val strength = if (isLight) DiscoverDimens.HubBackdropLightAlpha else DiscoverDimens.HubBackdropAlpha

    Box(
        modifier = modifier
            .fillMaxWidth()
            .fillMaxHeight(DiscoverDimens.HubBackdropFraction)
            .graphicsLayer { alpha = fade() }
    ) {
        if (backdrop != null) {
            AsyncImage(
                model = backdrop,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().graphicsLayer { alpha = strength }
            )
        } else {
            Box(modifier = Modifier.fillMaxSize().background(accent.copy(alpha = strength)))
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(DiscoverDimens.HubBackdropHold to Color.Transparent, 1f to pageColor))
        )
    }
}

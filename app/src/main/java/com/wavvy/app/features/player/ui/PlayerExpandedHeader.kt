package com.wavvy.app.features.player.ui

// Compose animation
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
// Compose foundation and layouts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
// UI utilities and graphics
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
// Project components
import com.wavvy.app.features.player.ui.components.SongInfo
import com.wavvy.app.features.player.ui.components.SongSideActions

// Expanded player header layout
@Composable
fun PlayerExpandedHeader(
    isLyricsActive: Boolean,
    progress: Float,
    isLandscape: Boolean,
    fullHeight: Dp,
    screenWidth: Dp,
    songTitle: String,
    cleanArtistName: String,
    songUrl: String?,
    isFavorite: Boolean,
    onFavoriteClick: () -> Unit
) {
    AnimatedVisibility(
        visible = !isLyricsActive || progress < 0.8f,
        enter = fadeIn(tween(400)),
        exit = fadeOut(tween(400))
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            val currentNavInsets = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
            val isGesture = currentNavInsets <= 24.dp
            val bottomReserved = if (isGesture) 20.dp + 56.dp else currentNavInsets + 8.dp + 56.dp
            val portraitTextOffsetY = fullHeight - bottomReserved - 255.dp
            val textOffsetX = if (isLandscape) lerp(76.dp, 370.dp, progress) else lerp(76.dp, 30.dp, progress)
            val textOffsetY = if (isLandscape) lerp(10.dp, 75.dp, progress) else lerp(10.dp, portraitTextOffsetY, progress)
            val sideActionsWidth = 110.dp

            val infoWidth = if (isLandscape) {
                val miniLandscapeButtonStartX = (screenWidth * 0.55f) - 56.dp
                val miniInfoWidth = (miniLandscapeButtonStartX - textOffsetX + 25.dp).coerceAtLeast(0.dp)
                val expandedMargin = sideActionsWidth + 40.dp
                val expandedInfoWidth = screenWidth - textOffsetX - expandedMargin

                lerp(miniInfoWidth, expandedInfoWidth, progress)
            } else {
                val miniPlayerButtonStartX = (screenWidth * 0.92f) - 56.dp
                val miniInfoWidth = (miniPlayerButtonStartX - textOffsetX + 25.dp).coerceAtLeast(0.dp)
                val expandedMargin = sideActionsWidth + 10.dp
                val expandedInfoWidth = screenWidth - textOffsetX - expandedMargin

                lerp(miniInfoWidth, expandedInfoWidth, progress)
            }

            Box(
                modifier = Modifier
                    .offset(textOffsetX, textOffsetY)
                    .width(infoWidth)
                    .clipToBounds()
            ) {
                SongInfo(
                    title = songTitle,
                    artist = cleanArtistName,
                    progress = progress,
                    isLandscape = isLandscape,
                    screenWidth = screenWidth
                )
            }

            if (progress > 0.7f) {
                val portraitSideActionsY = portraitTextOffsetY + 12.dp
                SongSideActions(
                    songUrl = songUrl,
                    isFavorite = isFavorite,
                    onFavoriteClick = onFavoriteClick,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(
                            x = if (isLandscape) (-60).dp else (-30).dp,
                            y = if (isLandscape) 85.dp else portraitSideActionsY
                        )
                        .alpha(((progress - 0.7f) * 3.33f).coerceIn(0f, 1f))
                )
            }
        }
    }
}

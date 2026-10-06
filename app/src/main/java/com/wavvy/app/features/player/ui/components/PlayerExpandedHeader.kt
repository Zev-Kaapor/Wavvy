package com.wavvy.app.features.player.ui.components

// Compose layouts and foundations
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
// Compose state and runtime
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
// UI styling and utilities
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.lerp

// Title, artist and side actions, moving from the pill to their place in the open player, as in the old Wavvy
@Composable
fun PlayerExpandedHeader(
    progress: () -> Float,
    isLandscape: Boolean,
    fullHeight: Dp,
    screenWidth: Dp,
    title: String,
    artist: String,
    songId: String?,
    isFavorite: Boolean,
    onFavoriteClick: () -> Unit
) {
    val showSideActions by remember(progress) { derivedStateOf { progress() > PlayerDimens.SideActionsStart } }

    Box(modifier = Modifier.fillMaxSize()) {
        val portraitInfoY = fullHeight - toolbarReservedHeight() - PlayerDimens.InfoAboveBottom
        val endX = if (isLandscape) PlayerDimens.InfoStartLandscape else PlayerDimens.InfoStartExpanded
        val endY = if (isLandscape) PlayerDimens.InfoTopLandscape else portraitInfoY

        // In the pill the text ends under the play button, in the open player it leaves room for the side actions
        val widthFraction = if (isLandscape) PlayerDimens.MiniWidthFractionLandscape else PlayerDimens.MiniWidthFraction
        val miniButtonX = screenWidth * widthFraction - PlayerDimens.ButtonEndInset
        val expandedMargin = PlayerDimens.SideActionsWidth + if (isLandscape) PlayerDimens.SideActionsMarginLandscape else PlayerDimens.SideActionsMargin

        Box(
            modifier = Modifier
                .layout { measurable, constraints ->
                    val opening = progress()
                    val x = lerp(PlayerDimens.InfoStart, endX, opening)
                    val miniWidth = (miniButtonX - x + PlayerDimens.InfoButtonOverlap).coerceAtLeast(PlayerDimens.None)
                    val width = lerp(miniWidth, screenWidth - x - expandedMargin, opening).roundToPx().coerceAtLeast(0)
                    val placeable = measurable.measure(Constraints(minWidth = width, maxWidth = width, maxHeight = constraints.maxHeight))
                    layout(constraints.maxWidth, constraints.maxHeight) {
                        placeable.place(x.roundToPx(), lerp(PlayerDimens.InfoTop, endY, opening).roundToPx())
                    }
                }
                .clipToBounds()
        ) {
            SongInfo(
                title = title,
                artist = artist,
                progress = progress,
                screenWidth = screenWidth,
                isLandscape = isLandscape
            )
        }

        if (showSideActions) {
            SongSideActions(
                songId = songId,
                isFavorite = isFavorite,
                onFavoriteClick = onFavoriteClick,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(
                        x = -(if (isLandscape) PlayerDimens.SideActionsEndLandscape else PlayerDimens.SideActionsEnd),
                        y = if (isLandscape) PlayerDimens.SideActionsTopLandscape else portraitInfoY + PlayerDimens.SideActionsBelowInfo
                    )
                    .graphicsLayer { alpha = ((progress() - PlayerDimens.SideActionsStart) * PlayerDimens.SideActionsFadeSpeed).coerceIn(0f, 1f) }
            )
        }
    }
}

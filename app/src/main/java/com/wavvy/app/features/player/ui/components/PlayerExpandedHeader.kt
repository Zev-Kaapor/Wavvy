package com.wavvy.app.features.player.ui.components

// Compose layouts and foundations
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
// Compose state and runtime
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import com.wavvy.app.core.cast.CastController

// How fast the heart of the pill is swapped for the one that travels, the first tenth of the change
private const val HeartFadeSpeed = 10f

// Title, artist and side actions, moving from the pill to their place in the open player, as in the old Wavvy
@Composable
fun PlayerExpandedHeader(
    progress: () -> Float,
    frame: LandscapeFrame?,
    panel: () -> Float,
    fullHeight: Dp,
    screenWidth: Dp,
    title: String,
    artist: String,
    songId: String?,
    isFavorite: Boolean,
    onFavoriteClick: () -> Unit,
    onTitleClick: () -> Unit,
    onArtistClick: () -> Unit
) {
    val showSideActions by remember(progress, panel) { derivedStateOf { progress() > PlayerDimens.SideActionsStart && panel() < 1f } }

    Box(modifier = Modifier.fillMaxSize()) {
        val portraitInfoY = fullHeight - toolbarReservedHeight() - PlayerDimens.InfoAboveBottom

        // In the pill the text ends under the play button, in the open player it leaves room for the side actions
        val widthFraction = if (frame != null) PlayerDimens.MiniWidthFractionLandscape else PlayerDimens.MiniWidthFraction
        val miniButtonX = screenWidth * widthFraction - PlayerDimens.ButtonEndInset
        // The cast button sits to the left of the play button, so the text of the pill ends before it, without the overlap it has under the play button
        val isCastAvailable by CastController.isAvailable.collectAsState()
        val castRoom = if (isCastAvailable) PlayerDimens.Button + PlayerDimens.CastGap + PlayerDimens.InfoButtonOverlap else PlayerDimens.None
        val expandedMargin = PlayerDimens.SideActionsWidth + PlayerDimens.SideActionsMargin

        Box(
            modifier = Modifier
                .layout { measurable, constraints ->
                    val opening = progress()
                    val x = lerp(PlayerDimens.InfoStart, frame?.titleStart(panel()) ?: PlayerDimens.InfoStartExpanded, opening)
                    val miniWidth = (miniButtonX - x + PlayerDimens.InfoButtonOverlap - castRoom).coerceAtLeast(PlayerDimens.None)
                    val openWidth = (frame?.titleEnd(panel()) ?: (screenWidth - expandedMargin)) - x
                    val width = lerp(miniWidth, openWidth, opening).roundToPx().coerceAtLeast(0)
                    val placeable = measurable.measure(Constraints(minWidth = width, maxWidth = width, maxHeight = constraints.maxHeight))
                    layout(constraints.maxWidth, constraints.maxHeight) {
                        placeable.place(x.roundToPx(), lerp(if (frame != null) PlayerDimens.InfoTopLandscape else PlayerDimens.InfoTop, frame?.titleTop(panel()) ?: portraitInfoY, opening).roundToPx())
                    }
                }
                .clipToBounds()
        ) {
            SongInfo(
                title = title,
                artist = artist,
                progress = progress,
                screenWidth = screenWidth,
                isLandscape = frame != null,
                panel = panel,
                onTitleClick = onTitleClick,
                onArtistClick = onArtistClick
            )
        }

        if (showSideActions) {
            SongSideActions(
                songId = songId,
                isFavorite = isFavorite,
                onFavoriteClick = onFavoriteClick,
                // Lying down the heart leaves the pill as the one that travels takes its place
                favoriteAlpha = { if (frame != null) (1f - panel() * HeartFadeSpeed).coerceIn(0f, 1f) else 1f },
                modifier = Modifier
                    .then(
                        if (frame != null) {
                            Modifier.placedAt(x = { frame.actionsX(panel()) }, y = { frame.actionsY })
                        } else {
                            Modifier
                                .align(Alignment.TopEnd)
                                .offset(x = -PlayerDimens.SideActionsEnd, y = portraitInfoY + PlayerDimens.SideActionsBelowInfo)
                        }
                    )
                    .graphicsLayer { alpha = ((progress() - PlayerDimens.SideActionsStart) * PlayerDimens.SideActionsFadeSpeed).coerceIn(0f, 1f) * (1f - panel() * 2f).coerceAtLeast(0f) }
            )
        }
    }
}

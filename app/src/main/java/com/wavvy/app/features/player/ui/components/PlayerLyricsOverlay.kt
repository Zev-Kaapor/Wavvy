package com.wavvy.app.features.player.ui.components

// Compose animation
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
// Compose layouts and foundations
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
// Material 3 components
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
// Compose state and runtime
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
// UI styling and utilities
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
// Project resources
import com.wavvy.app.R
import com.wavvy.app.core.designsystem.icons.WavvyIcons
import com.wavvy.app.core.designsystem.theme.WavvyTheme
import com.wavvy.app.core.lyrics.LyricsSettings

// Lyrics over the open player with the song name above them, a tap closes them, as in the old Wavvy, with the menu of the lyrics options
@Composable
fun PlayerLyricsOverlay(
    visible: Boolean,
    frame: LandscapeFrame?,
    state: LyricsState,
    settings: LyricsSettings,
    positionMs: () -> Long,
    title: String,
    artist: String,
    onOptionsClick: () -> Unit,
    onSeek: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = WavvyTheme.colors

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(if (frame != null) tween(PlayerDimens.LandscapeSideEnterMillis, PlayerDimens.LandscapeSideEnterDelay) else tween(PlayerDimens.LyricsFadeMillis)),
        exit = fadeOut(tween(if (frame != null) PlayerDimens.LandscapeSideExitMillis else PlayerDimens.LyricsFadeMillis))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (frame != null) {
                        Modifier.padding(start = frame.sideStart, end = frame.sideEnd)
                    } else {
                        Modifier.padding(top = PlayerDimens.LyricsTop, bottom = PlayerDimens.LyricsBottom)
                    }
                )
                // Drags stay in the lyrics, so they do not close the player
                .pointerInput(Unit) { detectDragGestures { change, _ -> change.consume() } }
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                )
        ) {
            LyricsView(
                state = state,
                settings = settings,
                positionMs = positionMs,
                onSeek = onSeek,
                isLandscape = frame != null,
                modifier = if (frame != null) {
                    Modifier
                        .padding(top = frame.barBottom, bottom = frame.queueListBottom)
                        .fadingEnds(PlayerDimens.LyricsFadeFraction)
                } else {
                    Modifier.padding(top = PlayerDimens.LyricsListTop)
                }
            )

            // Lying down the name of the song is already beside the lyrics, so their own name goes on the line of the tools, and a tap on it opens their options
            if (frame != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .sideHeader(frame)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onOptionsClick
                        )
                ) {
                    Text(
                        text = stringResource(R.string.lyrics_title),
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, fontSize = PlayerDimens.LandscapeSideTitleSize),
                        color = colors.onMedia
                    )
                    Spacer(Modifier.width(PlayerDimens.LandscapeOptionsGap))
                    Icon(
                        imageVector = WavvyIcons.Settings,
                        contentDescription = stringResource(R.string.lyrics_options),
                        tint = colors.onMedia.copy(alpha = PlayerDimens.LandscapeOptionsAlpha),
                        modifier = Modifier.size(PlayerDimens.LandscapeOptionsIcon)
                    )
                }
                return@AnimatedVisibility
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = PlayerDimens.LyricsHeaderSide)
                    .align(Alignment.TopCenter),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = PlayerDimens.LyricsTitleSize,
                        lineHeight = PlayerDimens.LyricsTitleLineHeight
                    ),
                    color = colors.onMedia,
                    maxLines = 1,
                    modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE)
                )
                Text(
                    text = artist,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold, fontSize = PlayerDimens.LyricsArtistSize),
                    color = colors.onMedia.copy(alpha = PlayerDimens.LyricsArtistAlpha),
                    maxLines = 1,
                    modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE)
                )
            }

            // Menu of the lyrics options, on the line of the song name and as far from the edge as the arrow that closes the player is from the other edge
            val titleLine = with(LocalDensity.current) { PlayerDimens.LyricsTitleLineHeight.toDp() }
            val edgeGap = PlayerDimens.MinimizeStart + (PlayerDimens.MinimizeButton - PlayerDimens.MinimizeIcon) / 2 -
                (PlayerDimens.LyricsMenuButton - PlayerDimens.LyricsMenuIcon) / 2
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(end = edgeGap)
                    .height(titleLine)
            ) {
                IconButton(onClick = onOptionsClick, modifier = Modifier.requiredSize(PlayerDimens.LyricsMenuButton)) {
                    Icon(
                        imageVector = WavvyIcons.MoreVertical,
                        contentDescription = stringResource(R.string.lyrics_options),
                        tint = colors.onMedia,
                        modifier = Modifier.size(PlayerDimens.LyricsMenuIcon)
                    )
                }
            }
        }
    }
}

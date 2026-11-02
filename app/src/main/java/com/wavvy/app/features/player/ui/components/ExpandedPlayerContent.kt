package com.wavvy.app.features.player.ui.components

// Compose layouts and foundations
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
// Compose state and runtime
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
// UI styling and utilities
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
// Math
import kotlin.math.abs
// Project resources
import com.wavvy.app.R
import com.wavvy.app.core.cast.CastController
import com.wavvy.app.core.designsystem.icons.WavvyIcons
import com.wavvy.app.core.designsystem.theme.DarkColors
import com.wavvy.app.core.designsystem.theme.WavvyTheme
import com.wavvy.app.core.device.isDeviceTrack
import com.wavvy.app.core.playback.RepeatMode

// Seekbar, toolbar and the arrow that closes the open player, always in the dark colors over the picture, as in the old Wavvy
// While it is not active it stands ready and still, with nothing that animates
@Composable
fun ExpandedPlayerContent(
    onMinimize: () -> Unit,
    progress: Float,
    isActive: Boolean,
    durationMs: Long,
    onSeek: (Float) -> Unit,
    isLyricsActive: Boolean,
    onLyricsToggle: () -> Unit,
    onMoreClick: () -> Unit,
    isQueueActive: Boolean,
    onQueueToggle: () -> Unit,
    repeatMode: RepeatMode,
    onRepeatClick: () -> Unit,
    isShuffleActive: Boolean,
    onShuffleClick: () -> Unit,
    songId: String?,
    isFavorite: Boolean,
    onFavoriteClick: () -> Unit,
    frame: LandscapeFrame?,
    panel: () -> Float,
    screenHeight: Dp,
    modifier: Modifier = Modifier
) {
    DarkColors {
        Box(modifier = modifier.fillMaxSize()) {
            if (frame != null) {
                LandscapeContent(
                    frame = frame,
                    panel = panel,
                    onMinimize = onMinimize,
                    progress = progress,
                    isActive = isActive,
                    durationMs = durationMs,
                    onSeek = onSeek,
                    isLyricsActive = isLyricsActive,
                    onLyricsToggle = onLyricsToggle,
                    onMoreClick = onMoreClick,
                    isQueueActive = isQueueActive,
                    onQueueToggle = onQueueToggle,
                    repeatMode = repeatMode,
                    onRepeatClick = onRepeatClick,
                    isShuffleActive = isShuffleActive,
                    onShuffleClick = onShuffleClick,
                    songId = songId,
                    isFavorite = isFavorite,
                    onFavoriteClick = onFavoriteClick
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = PlayerDimens.ContentSide),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Room for the cover and the song, then the seekbar above the controls and the toolbar
                    Spacer(Modifier.height(screenHeight * PlayerDimens.ExpandedCoverTopFraction))
                    Spacer(Modifier.height(screenHeight * PlayerDimens.CoverAreaFraction))
                    Spacer(Modifier.weight(1f))
                    AuroraSeekbar(
                        progress = progress,
                        durationMs = durationMs,
                        onSeek = onSeek,
                        isActive = isActive,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(toolbarReservedHeight() + PlayerDimens.ControlsHeight + PlayerDimens.SeekbarAboveControls))
                }

                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    PlayerActionToolbar(
                        repeatMode = repeatMode,
                        onRepeatClick = onRepeatClick,
                        isShuffleActive = isShuffleActive,
                        onShuffleClick = onShuffleClick,
                        isLyricsActive = isLyricsActive,
                        onLyricsClick = onLyricsToggle,
                        isQueueActive = isQueueActive,
                        onQueueClick = onQueueToggle,
                        onMoreOptionsClick = onMoreClick,
                        accentColor = WavvyTheme.colors.playerAccent,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Arrow that closes the player, it sits on the line of the song name of the lyrics
                val titleLine = with(LocalDensity.current) { PlayerDimens.LyricsTitleLineHeight.toDp() }
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(top = PlayerDimens.LyricsTop, start = PlayerDimens.MinimizeStart)
                        .height(titleLine)
                ) {
                    IconButton(onClick = onMinimize, modifier = Modifier.requiredSize(PlayerDimens.MinimizeButton)) {
                        Icon(
                            imageVector = WavvyIcons.ArrowDown,
                            contentDescription = stringResource(R.string.player_minimize),
                            modifier = Modifier.size(PlayerDimens.MinimizeIcon),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

// How fast the heart of the pill is swapped for the one that travels, the first tenth of the change
private const val HeartFadeSpeed = 10f

// Lying down, the arrow and the tools on a line at the top that never moves, the seekbar under the song and shuffle and repeat at the sides of the controls
// These two and the seekbar follow the song to the start when the lyrics or the queue open, the panel is read only while measuring
@Composable
private fun LandscapeContent(
    frame: LandscapeFrame,
    panel: () -> Float,
    onMinimize: () -> Unit,
    progress: Float,
    isActive: Boolean,
    durationMs: Long,
    onSeek: (Float) -> Unit,
    isLyricsActive: Boolean,
    onLyricsToggle: () -> Unit,
    onMoreClick: () -> Unit,
    isQueueActive: Boolean,
    onQueueToggle: () -> Unit,
    repeatMode: RepeatMode,
    onRepeatClick: () -> Unit,
    isShuffleActive: Boolean,
    onShuffleClick: () -> Unit,
    songId: String?,
    isFavorite: Boolean,
    onFavoriteClick: () -> Unit
) {
    val idle = MaterialTheme.colorScheme.onSurface.copy(alpha = PlayerDimens.LandscapeToolAlpha)
    val accent = WavvyTheme.colors.playerAccent
    val buttonSize = PlayerDimens.LandscapeToolButton
    val iconSize = PlayerDimens.LandscapeToolIcon
    val isCastAvailable by CastController.isAvailable.collectAsState()

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .padding(start = frame.toolsStart, top = frame.barTop)
            .width(frame.toolsEnd - frame.toolsStart)
            .height(buttonSize)
    ) {
        IconButton(onClick = onMinimize, modifier = Modifier.requiredSize(buttonSize)) {
            Icon(
                imageVector = WavvyIcons.ArrowDown,
                contentDescription = stringResource(R.string.player_minimize),
                modifier = Modifier.size(PlayerDimens.MinimizeIcon),
                tint = MaterialTheme.colorScheme.onSurface
            )
        }

        Spacer(Modifier.weight(1f))

        QueueButton(isQueueActive, onQueueToggle, idle, accent, buttonSize, iconSize)
        ToggleButton(WavvyIcons.Lyrics, isLyricsActive, onLyricsToggle, idle, accent, buttonSize, PlayerDimens.LandscapeLyricsIcon)

        // The room of the cast button, which comes from the pill and is drawn with the controls
        if (isCastAvailable) Spacer(Modifier.size(buttonSize))

        AnimatedIconButton(onClick = onMoreClick, buttonSize = buttonSize) { scale ->
            Icon(
                imageVector = WavvyIcons.MoreVertical,
                contentDescription = stringResource(R.string.cd_more),
                tint = idle,
                modifier = scale.size(iconSize)
            )
        }
    }

    AuroraSeekbar(
        progress = progress,
        durationMs = durationMs,
        onSeek = onSeek,
        isActive = isActive,
        timeColor = WavvyTheme.colors.onMedia.copy(alpha = PlayerDimens.LandscapeTimeAlpha),
        timesLift = PlayerDimens.LandscapeTimesLift,
        // The line of the seekbar has room at its sides, so that room stays out of the column
        modifier = Modifier.placedAt(
            x = { frame.columnStart(panel()) - PlayerDimens.SeekbarSide },
            y = { frame.seekTop(panel()) },
            width = { panel().let { frame.columnEnd(it) - frame.columnStart(it) } + PlayerDimens.SeekbarSide * 2 }
        )
    )

    // Shuffle and repeat fade out and back in on the way, so they never go over the controls
    Box(
        modifier = Modifier
            .placedAt(x = { frame.modesStart(panel()) }, y = { frame.modesTop(panel()) })
            .graphicsLayer { alpha = abs(1f - panel() * 2f) }
    ) {
        ToggleButton(WavvyIcons.Shuffle, isShuffleActive, onShuffleClick, idle, accent, buttonSize, PlayerDimens.LandscapeModeIcon)
    }
    Box(
        modifier = Modifier
            .placedAt(x = { frame.modesEnd(panel()) }, y = { frame.modesTop(panel()) }, anchorX = 1f)
            .graphicsLayer { alpha = abs(1f - panel() * 2f) }
    ) {
        RepeatButton(repeatMode, onRepeatClick, idle, accent, buttonSize, PlayerDimens.LandscapeModeIcon)
    }

    // Beside the lyrics or the queue favorite leaves the pill and travels to follow shuffle and repeat, a file of the phone has no like to give
    if (!isDeviceTrack(songId)) {
        val heartScale = PlayerDimens.FavoriteIcon / PlayerDimens.LandscapeModeIcon
        Box(
            modifier = Modifier
                .placedAt(x = { frame.favoriteX(panel()) }, y = { frame.favoriteTop(panel()) })
                .graphicsLayer {
                    alpha = (panel() * HeartFadeSpeed).coerceIn(0f, 1f)
                    val scale = heartScale + (1f - heartScale) * panel()
                    scaleX = scale
                    scaleY = scale
                }
        ) {
            ToggleButton(
                icon = if (isFavorite) WavvyIcons.Favorite else WavvyIcons.FavoriteBorder,
                isActive = isFavorite,
                onClick = { if (panel() > 0f) onFavoriteClick() },
                inactive = idle,
                active = WavvyTheme.colors.playerLiked,
                buttonSize = buttonSize,
                iconSize = PlayerDimens.LandscapeModeIcon,
                contentDescription = stringResource(R.string.player_favorite)
            )
        }
    }
}

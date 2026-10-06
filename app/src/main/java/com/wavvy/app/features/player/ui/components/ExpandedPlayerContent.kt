package com.wavvy.app.features.player.ui.components

// Compose layouts and foundations
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
// Material 3 components
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
// Compose state and runtime
import androidx.compose.runtime.Composable
// UI styling and utilities
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
// Project resources
import com.wavvy.app.R
import com.wavvy.app.core.designsystem.icons.WavvyIcons
import com.wavvy.app.core.designsystem.theme.DarkColors
import com.wavvy.app.core.designsystem.theme.WavvyTheme
import com.wavvy.app.core.playback.RepeatMode

// Seekbar, toolbar and the arrow that closes the open player, always in the dark colors over the picture, as in the old Wavvy
@Composable
fun ExpandedPlayerContent(
    onMinimize: () -> Unit,
    progress: Float,
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
    isLandscape: Boolean,
    screenHeight: Dp,
    modifier: Modifier = Modifier
) {
    DarkColors {
        Box(modifier = modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = PlayerDimens.ContentSide),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (isLandscape) {
                    // Seekbar beside the cover, hidden behind the lyrics
                    if (!isLyricsActive) Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = PlayerDimens.SeekbarTopLandscape),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Spacer(Modifier.width(PlayerDimens.ControlsAreaStartLandscape))
                        AuroraSeekbar(
                            progress = progress,
                            durationMs = durationMs,
                            onSeek = onSeek,
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = PlayerDimens.SeekbarSideLandscape)
                        )
                    }
                } else {
                    // Room for the cover and the song, then the seekbar above the controls and the toolbar
                    Spacer(Modifier.height(screenHeight * PlayerDimens.ExpandedCoverTopFraction))
                    Spacer(Modifier.height(screenHeight * PlayerDimens.CoverAreaFraction))
                    Spacer(Modifier.weight(1f))
                    AuroraSeekbar(
                        progress = progress,
                        durationMs = durationMs,
                        onSeek = onSeek,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(toolbarReservedHeight() + PlayerDimens.ControlsHeight + PlayerDimens.SeekbarAboveControls))
                }
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
                    modifier = if (isLandscape) Modifier.width(PlayerDimens.ToolbarWidthLandscape) else Modifier.fillMaxWidth()
                )
            }

            // Arrow that closes the player, standing it sits on the line of the song name of the lyrics, lying below the cutout
            val titleLine = with(LocalDensity.current) { PlayerDimens.LyricsTitleLineHeight.toDp() }
            if (!(isLandscape && isLyricsActive)) Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .then(
                        if (isLandscape) {
                            Modifier
                                .windowInsetsPadding(WindowInsets.statusBars.union(WindowInsets.displayCutout))
                                .padding(top = PlayerDimens.MinimizeTop, start = PlayerDimens.MinimizeStart)
                        } else {
                            Modifier
                                .padding(top = PlayerDimens.LyricsTop, start = PlayerDimens.MinimizeStart)
                                .height(titleLine)
                        }
                    )
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

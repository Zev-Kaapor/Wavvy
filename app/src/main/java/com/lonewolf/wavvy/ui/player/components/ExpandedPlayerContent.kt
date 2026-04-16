package com.lonewolf.wavvy.ui.player.components

// Compose animation and foundation
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.*
// Material 3 and icons
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
// State and UI tools
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
// Project resources
import com.lonewolf.wavvy.R
import com.lonewolf.wavvy.ui.theme.WavvyTheme
import com.lonewolf.wavvy.ui.theme.accentCyan

// Fullscreen player view with immersive controls
@Composable
fun ExpandedPlayerContent(
    isExpanded: Boolean,
    onMinimize: () -> Unit,
    currentProgress: Float,
    onProgressChange: (Float) -> Unit,
    isLyricsActive: Boolean,
    onLyricsToggle: () -> Unit,
    onMoreClick: () -> Unit,
    modifier: Modifier = Modifier,
    isQueueActive: Boolean,
    onQueueToggle: () -> Unit,
    repeatMode: Int,
    onRepeatClick: () -> Unit,
    isShuffleActive: Boolean,
    onShuffleClick: () -> Unit,
    isLandscape: Boolean = false
) {
    val totalDuration = 0L

    // Forcing the Wavvy dark theme definition to keep brand consistency
    WavvyTheme(darkTheme = true) {
        // Visibility transition
        AnimatedVisibility(
            visible = isExpanded,
            enter = fadeIn(tween(300)),
            exit = fadeOut(tween(250))
        ) {
            Box(modifier = modifier.fillMaxSize()) {
                // Main content column
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (isLandscape) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 135.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Spacer(Modifier.width(320.dp))
                            
                            AuroraSeekbar(
                                progress = currentProgress,
                                duration = totalDuration,
                                isPlaying = true,
                                onSeek = { onProgressChange(it) },
                                onProgressUpdate = {},
                                modifier = Modifier.weight(1f).padding(horizontal = 16.dp)
                            )
                        }
                    } else {
                        // Portrait Layout: Original
                        Spacer(Modifier.height(110.dp))

                        // Layout placeholders for album art and info
                        Spacer(Modifier.height(340.dp))
                        Spacer(Modifier.height(180.dp))

                        // Custom seekbar integration
                        AuroraSeekbar(
                            progress = currentProgress,
                            duration = totalDuration,
                            isPlaying = true,
                            onSeek = { onProgressChange(it) },
                            onProgressUpdate = {},
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // Playback action toolbar
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 10.dp),
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
                        accentColor = MaterialTheme.accentCyan,
                        modifier = if (isLandscape) Modifier.width(540.dp) else Modifier.fillMaxWidth()
                    )
                }

                // Top navigation toolbar
                PlayerToolbar(
                    onMinimize = onMinimize,
                    modifier = Modifier.align(Alignment.TopStart)
                )
            }
        }
    }
}

// Minimal toolbar for player dismissal
@Composable
private fun PlayerToolbar(
    onMinimize: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .padding(start = 8.dp),
        contentAlignment = Alignment.TopStart
    ) {
        // Minimize button
        IconButton(
            onClick = onMinimize,
            modifier = Modifier.size(48.dp).offset(y = (40).dp)
        ) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = stringResource(R.string.cd_minimize),
                modifier = Modifier.size(28.dp),
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

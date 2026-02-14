package com.lonewolf.wavvy.ui.player.components

// Compose animation and foundation
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.*
// Material 3 and icons
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.automirrored.filled.PlaylistAddCheck
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

// Fullscreen player view with immersive controls
@Composable
fun ExpandedPlayerContent(
    isExpanded: Boolean,
    songTitle: String,
    artistName: String,
    onMinimize: () -> Unit,
    currentProgress: Float,
    onProgressChange: (Float) -> Unit,
    isLyricsActive: Boolean,
    onLyricsToggle: () -> Unit,
    onMoreClick: () -> Unit,
    modifier: Modifier = Modifier,
    isQueueActive: Boolean,
    onQueueToggle: () -> Unit
) {
    var isAddedToLibrary by remember { mutableStateOf(false) }
    var repeatMode by remember { mutableIntStateOf(0) }
    var isShuffleActive by remember { mutableStateOf(false) }
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

                // Playback action toolbar
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 10.dp),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    PlayerActionToolbar(
                        isAddedToLibrary = isAddedToLibrary,
                        onLibraryClick = { isAddedToLibrary = !isAddedToLibrary },
                        onDownloadClick = { },
                        repeatMode = repeatMode,
                        onRepeatClick = { repeatMode = (repeatMode + 1) % 3 },
                        isShuffleActive = isShuffleActive,
                        onShuffleClick = { isShuffleActive = !isShuffleActive },
                        isLyricsActive = isLyricsActive,
                        onLyricsClick = onLyricsToggle,
                        onQueueClick = onQueueToggle,
                        isQueueActive = isQueueActive,
                        onMoreOptionsClick = onMoreClick
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

package com.lonewolf.wavvy.ui.player

// Compose foundation and layout
import androidx.compose.foundation.layout.*
// State management
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
// Project components and theme
import com.lonewolf.wavvy.ui.player.components.AuroraSeekbar
import com.lonewolf.wavvy.ui.player.components.PlayerActionToolbar

// Main player screen orchestrating playback controls and metadata
@Composable
fun PlayerScreen(
    songTitle: String,
    artistName: String,
    isLyricsActive: Boolean,
    onLyricsToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Playback and sequence state
    var isAddedToLibrary by remember { mutableStateOf(false) }
    var repeatMode by remember { mutableIntStateOf(0) }
    var isShuffleActive by remember { mutableStateOf(false) }

    // Playback progress state
    var currentProgress by remember { mutableFloatStateOf(0.3f) }
    val totalDuration = 225000L

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Vertical layout spacing
        Spacer(Modifier.height(100.dp))
        Spacer(Modifier.height(340.dp))
        Spacer(Modifier.height(140.dp))

        // Dynamic progress bar
        AuroraSeekbar(
            progress = currentProgress,
            duration = totalDuration,
            isPlaying = true,
            onSeek = { currentProgress = it },
            onProgressUpdate = { },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(16.dp))

        // Bottom playback actions
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
            onMoreOptionsClick = { }
        )

        Spacer(Modifier.weight(1f))
    }
}

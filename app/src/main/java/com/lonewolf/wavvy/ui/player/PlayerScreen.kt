package com.lonewolf.wavvy.ui.player

// Compose foundation and layout
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
// State management
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
// Project components and theme
import com.lonewolf.wavvy.R
import com.lonewolf.wavvy.ui.player.components.AuroraSeekbar
import com.lonewolf.wavvy.ui.player.components.PlayerActionToolbar
import com.lonewolf.wavvy.ui.theme.accentCyan

// Main player screen orchestrating playback controls and metadata
@Composable
fun PlayerScreen(
    isLyricsActive: Boolean,
    onLyricsToggle: () -> Unit,
    isQueueActive: Boolean,
    onQueueToggle: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PlayerViewModel = viewModel()
) {
    // Media controller states from view model
    val isPlaying by viewModel.isPlaying.collectAsState()
    val currentMediaItem by viewModel.currentMediaItem.collectAsState()

    // Playback and sequence state
    var repeatMode by remember { mutableIntStateOf(0) }
    var isShuffleActive by remember { mutableStateOf(false) }

    // Playback progress state
    var currentProgress by remember { mutableFloatStateOf(0.3f) }
    val totalDuration = 225000L

    // Dynamic metadata extraction
    val songTitle = currentMediaItem?.mediaMetadata?.title?.toString() ?: stringResource(R.string.default_song_title)
    val artistName = currentMediaItem?.mediaMetadata?.artist?.toString() ?: stringResource(R.string.default_artist_name)

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
            isPlaying = isPlaying,
            onSeek = {
                currentProgress = it
                viewModel.seekTo((it * totalDuration).toLong())
            },
            onProgressUpdate = { },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(16.dp))

        // Bottom playback actions
        PlayerActionToolbar(
            repeatMode = repeatMode,
            onRepeatClick = { repeatMode = (repeatMode + 1) % 3 },
            isShuffleActive = isShuffleActive,
            onShuffleClick = { isShuffleActive = !isShuffleActive },
            isLyricsActive = isLyricsActive,
            onLyricsClick = onLyricsToggle,
            onQueueClick = onQueueToggle,
            isQueueActive = isQueueActive,
            onMoreOptionsClick = { },
            accentColor = MaterialTheme.accentCyan
        )

        Spacer(Modifier.weight(1f))
    }
}

package com.lonewolf.wavvy.ui.player

// Compose foundation and layout
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
// State management
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
// Project components and theme
import com.lonewolf.wavvy.R
import com.lonewolf.wavvy.ui.player.components.AlbumCover
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
    val currentTrackInfo by viewModel.currentTrackInfo.collectAsState()

    // Playback and sequence state
    var repeatMode by remember { mutableIntStateOf(0) }
    var isShuffleActive by remember { mutableStateOf(false) }

    // Playback progress state
    var currentProgress by remember { mutableFloatStateOf(0.3f) }
    val totalDuration = 225000L

    // Dynamic metadata from currentTrackInfo (updates immediately)
    val songTitle = currentTrackInfo?.title ?: stringResource(R.string.default_song_title)
    val artistName = currentTrackInfo?.artist ?: stringResource(R.string.default_artist_name)

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val screenWidth = with(LocalDensity.current) { constraints.maxWidth.toDp() }
        val screenHeight = with(LocalDensity.current) { constraints.maxHeight.toDp() }

        // Album cover layer: positions itself absolutely based on screen size/progress
        AlbumCover(
            progress = 1f,
            songProgress = currentProgress,
            screenWidth = screenWidth,
            screenHeight = screenHeight,
            imageUrl = currentTrackInfo?.imageUrl
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Vertical layout spacing (reserves space for the AlbumCover layer above)
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
}

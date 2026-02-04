package com.lonewolf.wavvy.ui.player

// Compose foundation and layout
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
// State management
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
// Project components and theme
import com.lonewolf.wavvy.ui.player.components.AuroraSeekbar
import com.lonewolf.wavvy.ui.player.components.PlayerActionToolbar
import com.lonewolf.wavvy.ui.theme.Poppins

// Main player screen orchestrating playback controls and metadata
@Composable
fun PlayerScreen(
    songTitle: String,
    artistName: String,
    modifier: Modifier = Modifier
) {
    // Playback state
    var isFavorite by remember { mutableStateOf(false) }
    var repeatMode by remember { mutableIntStateOf(0) }
    var isShuffleActive by remember { mutableStateOf(false) }

    // Progress state
    var currentProgress by remember { mutableFloatStateOf(0.3f) }
    val totalDuration = 225000L

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Layout anchors for visual consistency
        Spacer(Modifier.height(100.dp))
        Spacer(Modifier.height(340.dp))
        Spacer(Modifier.height(140.dp))

        AuroraSeekbar(
            progress = currentProgress,
            duration = totalDuration,
            isPlaying = true,
            onSeek = { currentProgress = it },
            onProgressUpdate = { },
            modifier = Modifier.fillMaxWidth()
        )

        // Playback timestamp labels
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = "0:00", fontSize = 12.sp, fontFamily = Poppins)
            Text(text = "3:45", fontSize = 12.sp, fontFamily = Poppins)
        }

        Spacer(Modifier.height(16.dp))

        PlayerActionToolbar(
            isFavorite = isFavorite,
            onFavoriteClick = { isFavorite = !isFavorite },
            onDownloadClick = { },
            repeatMode = repeatMode,
            onRepeatClick = { repeatMode = (repeatMode + 1) % 3 },
            isShuffleActive = isShuffleActive,
            onShuffleClick = { isShuffleActive = !isShuffleActive },
            onMoreOptionsClick = { }
        )

        Spacer(Modifier.weight(1f))
    }
}

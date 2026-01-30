package com.lonewolf.wavvy.ui.player

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lonewolf.wavvy.ui.player.components.AuroraSeekbar
import com.lonewolf.wavvy.ui.player.components.PlayerActionToolbar
import com.lonewolf.wavvy.ui.player.components.SongInfo
import com.lonewolf.wavvy.ui.theme.Poppins

@Composable
fun PlayerScreen(
    songTitle: String,
    artistName: String,
    modifier: Modifier = Modifier
) {
    // Player UI States
    var isFavorite by remember { mutableStateOf(false) }
    var repeatMode by remember { mutableIntStateOf(0) }
    var isShuffleActive by remember { mutableStateOf(false) }

    // Seekbar States
    var currentProgress by remember { mutableFloatStateOf(0.3f) }
    val totalDuration = 225000L

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Upper spacing for album cover
        Spacer(modifier = Modifier.height(100.dp))
        Spacer(modifier = Modifier.height(340.dp))
        Spacer(modifier = Modifier.height(32.dp))

        // Song and Artist Information
        SongInfo(
            title = songTitle,
            artist = artistName
        )

        Spacer(modifier = Modifier.height(40.dp))

        // Custom Aurora Seekbar
        AuroraSeekbar(
            progress = currentProgress,
            duration = totalDuration,
            isPlaying = true,
            onSeek = { newProgress -> currentProgress = newProgress },
            onProgressUpdate = { },
            modifier = Modifier.fillMaxWidth()
        )

        // Time indicators
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("0:00", fontSize = 12.sp, fontFamily = Poppins)
            Text("3:45", fontSize = 12.sp, fontFamily = Poppins)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Animated Actions Toolbar
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

        Spacer(modifier = Modifier.weight(1f))
    }
}

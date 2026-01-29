package com.lonewolf.wavvy.ui.player

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lonewolf.wavvy.ui.player.components.PlayerActionToolbar
import com.lonewolf.wavvy.ui.theme.Poppins

@Composable
fun PlayerScreen(
    songTitle: String,
    artistName: String,
    modifier: Modifier = Modifier
) {
    // Temporary states for UI feedback
    var isFavorite by remember { mutableStateOf(false) }
    var repeatMode by remember { mutableIntStateOf(0) }
    var isShuffleActive by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(100.dp))
        Spacer(modifier = Modifier.height(340.dp))
        Spacer(modifier = Modifier.height(32.dp))

        // Song Title
        Text(
            text = songTitle,
            fontFamily = Poppins,
            fontWeight = FontWeight.Bold,
            fontSize = 24.sp,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Artist Name
        Text(
            text = artistName,
            fontFamily = Poppins,
            fontWeight = FontWeight.Medium,
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(40.dp))

        // Progress Slider
        Slider(
            value = 0.3f,
            onValueChange = {},
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary
            ),
            modifier = Modifier.fillMaxWidth()
        )

        // Time indicators
        Row(
            modifier = Modifier.fillMaxWidth(),
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
            onDownloadClick = { /* Download logic here */ },
            repeatMode = repeatMode,
            onRepeatClick = { repeatMode = (repeatMode + 1) % 3 },
            isShuffleActive = isShuffleActive,
            onShuffleClick = { isShuffleActive = !isShuffleActive },
            onMoreOptionsClick = { /* Options logic here */ }
        )

        Spacer(modifier = Modifier.weight(1f))
    }
}

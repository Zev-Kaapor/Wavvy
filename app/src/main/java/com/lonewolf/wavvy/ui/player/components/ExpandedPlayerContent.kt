package com.lonewolf.wavvy.ui.player.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lonewolf.wavvy.ui.theme.Poppins

@Composable
fun ExpandedPlayerContent(
    isExpanded: Boolean,
    songTitle: String,
    artistName: String,
    onMinimize: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isFavorite by remember { mutableStateOf(false) }
    var repeatMode by remember { mutableIntStateOf(0) }
    var isShuffleActive by remember { mutableStateOf(false) }

    var currentProgress by remember { mutableFloatStateOf(0.3f) }
    val totalDuration = 225000L

    AnimatedVisibility(
        visible = isExpanded,
        enter = fadeIn(animationSpec = tween(300)),
        exit = fadeOut(animationSpec = tween(250))
    ) {
        Box(
            modifier = modifier.fillMaxSize()
        ) {
            // Main Content Layer
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(180.dp)) // Increase this value to lower the aurora bar

                // Album Cover Space
                Spacer(modifier = Modifier.height(340.dp))

                // Titles Space (Ensure Title/Artist are here or in PlayerScreen)
                Spacer(modifier = Modifier.height(100.dp))

                // Aurora Seekbar
                AuroraSeekbar(
                    progress = currentProgress,
                    duration = totalDuration,
                    isPlaying = true,
                    onSeek = { currentProgress = it },
                    onProgressUpdate = {},
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
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 10.dp), // This spacer pushes the toolbar down to the bottom area
                contentAlignment = Alignment.BottomCenter
            ) {
                PlayerActionToolbar(
                    isFavorite = isFavorite,
                    onFavoriteClick = { isFavorite = !isFavorite },
                    onDownloadClick = { /* Handle download */ },
                    repeatMode = repeatMode,
                    onRepeatClick = { repeatMode = (repeatMode + 1) % 3 },
                    isShuffleActive = isShuffleActive,
                    onShuffleClick = { isShuffleActive = !isShuffleActive },
                    onMoreOptionsClick = { /* Show options */ }
                )
            }

            // Top Toolbar (Minimize)
            PlayerToolbar(
                onMinimize = onMinimize,
                modifier = Modifier.align(Alignment.TopStart)
            )
        }
    }
}

@Composable
private fun PlayerToolbar(
    onMinimize: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .statusBarsPadding()
            .padding(start = 8.dp),
        contentAlignment = Alignment.TopStart
    ) {
        IconButton(
            onClick = onMinimize,
            modifier = Modifier
                .size(48.dp)
                .offset(y = (-10).dp)
        ) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = "Minimize",
                modifier = Modifier.size(28.dp),
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

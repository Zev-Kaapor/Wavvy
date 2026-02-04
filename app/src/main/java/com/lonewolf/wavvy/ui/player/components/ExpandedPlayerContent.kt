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
import androidx.compose.ui.unit.sp
// Project resources
import com.lonewolf.wavvy.R
import com.lonewolf.wavvy.ui.theme.Poppins

// Fullscreen player view with immersive controls
@Composable
fun ExpandedPlayerContent(
    isExpanded: Boolean,
    songTitle: String,
    artistName: String,
    onMinimize: () -> Unit,
    currentProgress: Float,
    onProgressChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    var isFavorite by remember { mutableStateOf(false) }
    var repeatMode by remember { mutableIntStateOf(0) }
    var isShuffleActive by remember { mutableStateOf(false) }
    val totalDuration = 225000L

    AnimatedVisibility(
        visible = isExpanded,
        enter = fadeIn(tween(300)),
        exit = fadeOut(tween(250))
    ) {
        Box(modifier = modifier.fillMaxSize()) {
            // Content layer
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(110.dp))

                // Art and Info spacers
                Spacer(Modifier.height(340.dp))
                Spacer(Modifier.height(130.dp))

                AuroraSeekbar(
                    progress = currentProgress,
                    duration = totalDuration,
                    isPlaying = true,
                    onSeek = { onProgressChange(it) },
                    onProgressUpdate = {},
                    modifier = Modifier.fillMaxWidth()
                )

                // Time labels
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("0:00", fontSize = 12.sp, fontFamily = Poppins)
                    Text("-0:00", fontSize = 12.sp, fontFamily = Poppins)
                }
            }

            // Bottom controls
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 10.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
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
            }

            PlayerToolbar(
                onMinimize = onMinimize,
                modifier = Modifier.align(Alignment.TopStart)
            )
        }
    }
}

// Navigation toolbar for player dismissal
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
            modifier = Modifier.size(48.dp).offset(y = (-10).dp)
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

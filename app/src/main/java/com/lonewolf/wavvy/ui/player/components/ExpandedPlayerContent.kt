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

@Composable
fun ExpandedPlayerContent(
    isExpanded: Boolean,
    songTitle: String,
    artistName: String,
    onMinimize: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Local states for the animated toolbar
    var isFavorite by remember { mutableStateOf(false) }
    var repeatMode by remember { mutableIntStateOf(0) }
    var isShuffleActive by remember { mutableStateOf(false) }

    AnimatedVisibility(
        visible = isExpanded,
        enter = fadeIn(animationSpec = tween(300)),
        exit = fadeOut(animationSpec = tween(250))
    ) {
        Box(
            modifier = modifier.fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top margin for status bar/toolbar
                Spacer(modifier = Modifier.height(100.dp))

                // Space reserved for AlbumCover (handled by Container)
                Spacer(modifier = Modifier.height(340.dp))

                Spacer(modifier = Modifier.height(32.dp))

                // This spacer pushes the toolbar down to the bottom area
                Spacer(modifier = Modifier.weight(1f))

                // Animated Actions Toolbar
                PlayerActionToolbar(
                    isFavorite = isFavorite,
                    onFavoriteClick = { isFavorite = !isFavorite },
                    onDownloadClick = { /* Handle download */ },
                    repeatMode = repeatMode,
                    onRepeatClick = { repeatMode = (repeatMode + 1) % 3 },
                    isShuffleActive = isShuffleActive,
                    onShuffleClick = { isShuffleActive = !isShuffleActive },
                    onMoreOptionsClick = { /* Show options */ },
                    // Diminua este valor para descer mais o toolbar
                    modifier = Modifier.padding(bottom = 10.dp)
                )
            }

            // Fixed collapse button at the top
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

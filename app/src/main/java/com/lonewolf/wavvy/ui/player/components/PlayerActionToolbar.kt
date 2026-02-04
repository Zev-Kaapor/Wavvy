package com.lonewolf.wavvy.ui.player.components

// Compose animation mechanics
import androidx.compose.animation.*
import androidx.compose.animation.core.*
// Foundation and layout
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
// Material 3 and icons
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
// State and UI tools
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
// Coroutines
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private enum class DownloadState { Idle, Downloading, Done }

// Immersive action bar for the expanded player
@Composable
fun PlayerActionToolbar(
    isFavorite: Boolean,
    onFavoriteClick: () -> Unit,
    onDownloadClick: () -> Unit,
    repeatMode: Int,
    onRepeatClick: () -> Unit,
    isShuffleActive: Boolean,
    onShuffleClick: () -> Unit,
    onMoreOptionsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val inactive = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
    val active = MaterialTheme.colorScheme.tertiary

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 40.dp)
            .padding(bottom = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Playback and secondary actions
                Row(
                    horizontalArrangement = Arrangement.spacedBy(0.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    DownloadButton(onDownloadClick, inactive, active)

                    AnimatedIconButton(onFavoriteClick) { mod ->
                        Icon(
                            imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = null,
                            tint = if (isFavorite) active else inactive,
                            modifier = mod
                        )
                    }

                    RepeatButton(repeatMode, onRepeatClick, inactive, active)
                    ShuffleButton(isShuffleActive, onShuffleClick, inactive, active)
                }

                Spacer(Modifier.weight(1f))

                // Options trigger
                AnimatedIconButton(onMoreOptionsClick) { mod ->
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "More",
                        tint = inactive,
                        modifier = mod
                    )
                }
            }
        }
    }
}

// Interactive button with scale feedback
@Composable
private fun AnimatedIconButton(
    onClick: () -> Unit,
    contentScale: Float = 1f,
    content: @Composable (Modifier) -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()

    val pressScale by animateFloatAsState(
        targetValue = if (pressed) 0.88f else 1f,
        animationSpec = spring(Spring.DampingRatioMediumBouncy),
        label = "PressScale"
    )

    Box(
        modifier = Modifier
            .size(52.dp)
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        content(
            Modifier
                .size(24.dp)
                .graphicsLayer {
                    scaleX = pressScale * contentScale
                    scaleY = pressScale * contentScale
                }
        )
    }
}

// Cycle-through repeat modes with rotation animation
@Composable
private fun RepeatButton(
    repeatMode: Int,
    onClick: () -> Unit,
    inactive: Color,
    active: Color
) {
    val rotation = remember { Animatable(0f) }
    var lastMode by remember { mutableStateOf(repeatMode) }

    LaunchedEffect(repeatMode) {
        if (repeatMode != lastMode) {
            rotation.animateTo(rotation.value + 360f, spring(0.6f))
            lastMode = repeatMode
        }
    }

    AnimatedIconButton(onClick, if (repeatMode > 0) 1.1f else 1f) { mod ->
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.Default.Repeat,
                contentDescription = null,
                tint = if (repeatMode > 0) active else inactive,
                modifier = mod.graphicsLayer { rotationZ = rotation.value }
            )
            if (repeatMode == 2) {
                Text(
                    text = "1",
                    style = MaterialTheme.typography.labelSmall,
                    color = active
                )
            }
        }
    }
}

// Toggle shuffle state
@Composable
private fun ShuffleButton(
    isActive: Boolean,
    onClick: () -> Unit,
    inactive: Color,
    active: Color
) {
    AnimatedIconButton(onClick, if (isActive) 1.15f else 1f) { mod ->
        Icon(
            imageVector = Icons.Default.Shuffle,
            contentDescription = null,
            tint = if (isActive) active else inactive,
            modifier = mod
        )
    }
}

// Download sequence with progress ring and success state
@Composable
private fun DownloadButton(
    onClick: () -> Unit,
    inactive: Color,
    active: Color
) {
    var state by remember { mutableStateOf(DownloadState.Idle) }
    val progress = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

    AnimatedIconButton(
        onClick = {
            if (state != DownloadState.Idle) return@AnimatedIconButton
            onClick()
            scope.launch {
                state = DownloadState.Downloading
                progress.animateTo(1f, tween(1800))
                state = DownloadState.Done
                delay(1200)
                state = DownloadState.Idle
                progress.snapTo(0f)
            }
        }
    ) { mod ->
        AnimatedContent(
            targetState = state,
            transitionSpec = { (fadeIn() + scaleIn()).togetherWith(fadeOut() + scaleOut()) },
            contentAlignment = Alignment.Center,
            label = "DownloadAnim"
        ) { current ->
            when (current) {
                DownloadState.Idle -> Icon(Icons.Outlined.Download, null, tint = inactive, modifier = mod)
                DownloadState.Downloading -> Canvas(mod.size(20.dp)) {
                    drawArc(
                        color = active,
                        startAngle = -90f,
                        sweepAngle = progress.value * 360f,
                        useCenter = false,
                        style = Stroke(2.5.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
                DownloadState.Done -> Icon(Icons.Default.CheckCircle, null, tint = active, modifier = mod)
            }
        }
    }
}

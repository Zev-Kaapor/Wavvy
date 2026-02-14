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
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Download
// Material 3 components
import androidx.compose.material3.*
// State and UI tools
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
// Coroutines
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
// Project Resources
import com.lonewolf.wavvy.R

// Download lifecycle states
private enum class DownloadState { Idle, Downloading, Done }

// Immersive action bar for the expanded player
@Composable
fun PlayerActionToolbar(
    isAddedToLibrary: Boolean,
    onLibraryClick: () -> Unit,
    onDownloadClick: () -> Unit,
    repeatMode: Int,
    onRepeatClick: () -> Unit,
    isShuffleActive: Boolean,
    onShuffleClick: () -> Unit,
    isLyricsActive: Boolean,
    onLyricsClick: () -> Unit,
    isQueueActive: Boolean,
    onQueueClick: () -> Unit,
    onMoreOptionsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val inactive = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
    val active = MaterialTheme.colorScheme.tertiary
    val pillBackgroundColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(bottom = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        // Toolbar background surface
        Surface(
            shape = CircleShape,
            color = pillBackgroundColor,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Main control group
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    QueueButton(isQueueActive, onQueueClick, inactive, active)
                    LyricsToggleButton(isLyricsActive, onLyricsClick, inactive, active)
                    ShuffleButton(isShuffleActive, onShuffleClick, inactive, active)
                    RepeatButton(repeatMode, onRepeatClick, inactive, active)
                }

                Spacer(Modifier.weight(1f))

                // Menu trigger
                AnimatedIconButton(onMoreOptionsClick) { mod ->
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "More",
                        tint = inactive,
                        modifier = mod.size(24.dp)
                    )
                }
            }
        }
    }
}

// Button with tactile scale feedback
@Composable
private fun AnimatedIconButton(
    onClick: () -> Unit,
    content: @Composable (Modifier) -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.85f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "IconScale"
    )

    Box(
        modifier = Modifier
            .size(52.dp)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        content(Modifier.graphicsLayer {
            scaleX = scale
            scaleY = scale
        })
    }
}

// Lyrics state toggle
@Composable
private fun LyricsToggleButton(
    isActive: Boolean,
    onClick: () -> Unit,
    inactive: Color,
    active: Color
) {
    AnimatedIconButton(onClick) { mod ->
        Icon(
            // Using your custom ic_lyrics drawable
            painter = painterResource(id = R.drawable.ic_lyrics),
            contentDescription = "Lyrics",
            tint = if (isActive) active else inactive,
            modifier = mod.size(24.dp)
        )
    }
}

// Repeat modes with rotation
@Composable
private fun RepeatButton(
    repeatMode: Int,
    onClick: () -> Unit,
    inactive: Color,
    active: Color
) {
    val rotation = remember { Animatable(0f) }
    var lastMode by remember { mutableIntStateOf(repeatMode) }

    LaunchedEffect(repeatMode) {
        if (repeatMode != lastMode) {
            rotation.animateTo(rotation.value + 360f, spring(0.6f))
            lastMode = repeatMode
        }
    }

    AnimatedIconButton(onClick) { mod ->
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.Default.Repeat,
                contentDescription = null,
                tint = if (repeatMode > 0) active else inactive,
                modifier = mod.size(24.dp).graphicsLayer { rotationZ = rotation.value }
            )
            // Visual indicator for 'repeat one'
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

// Shuffle toggle
@Composable
private fun ShuffleButton(
    isActive: Boolean,
    onClick: () -> Unit,
    inactive: Color,
    active: Color
) {
    AnimatedIconButton(onClick) { mod ->
        Icon(
            imageVector = Icons.Default.Shuffle,
            contentDescription = null,
            tint = if (isActive) active else inactive,
            modifier = mod.size(24.dp)
        )
    }
}

// Queue button with bounce animation
@Composable
private fun QueueButton(
    isActive: Boolean,
    onClick: () -> Unit,
    inactive: Color,
    active: Color
) {
    val scope = rememberCoroutineScope()
    val offsetY = remember { Animatable(0f) }

    AnimatedIconButton(
        onClick = {
            // Trigger immediately to avoid delay
            onClick()
            scope.launch {
                offsetY.animateTo(-4f, tween(100, easing = LinearOutSlowInEasing))
                offsetY.animateTo(0f, spring(Spring.DampingRatioMediumBouncy))
            }
        }
    ) { mod ->
        Icon(
            imageVector = Icons.AutoMirrored.Filled.QueueMusic,
            contentDescription = "Queue",
            tint = if (isActive) active else inactive,
            modifier = mod
                .size(24.dp)
                .graphicsLayer { translationY = offsetY.value }
        )
    }
}

// Download button with progress
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
        // State transition animation
        AnimatedContent(
            targetState = state,
            transitionSpec = { (fadeIn() + scaleIn()).togetherWith(fadeOut() + scaleOut()) },
            contentAlignment = Alignment.Center,
            label = "DownloadAnim"
        ) { current ->
            when (current) {
                DownloadState.Idle -> Icon(Icons.Outlined.Download, null, tint = inactive, modifier = mod.size(24.dp))
                DownloadState.Downloading -> Canvas(mod.size(20.dp)) {
                    drawArc(
                        color = active,
                        startAngle = -90f,
                        sweepAngle = progress.value * 360f,
                        useCenter = false,
                        style = Stroke(2.5.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
                DownloadState.Done -> Icon(Icons.Default.CheckCircle, null, tint = active, modifier = mod.size(24.dp))
            }
        }
    }
}

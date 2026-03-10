package com.lonewolf.wavvy.ui.player.components

// Compose animation mechanics
import androidx.compose.animation.core.*
// Foundation and layout
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
// Material 3 and icons
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
// Material 3 components
import androidx.compose.material3.*
// State and UI tools
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
// Coroutines
import kotlinx.coroutines.launch
// Project Resources
import com.lonewolf.wavvy.R

// Immersive action bar for the expanded player
@Composable
fun PlayerActionToolbar(
    repeatMode: Int,
    onRepeatClick: () -> Unit,
    isShuffleActive: Boolean,
    onShuffleClick: () -> Unit,
    isLyricsActive: Boolean,
    onLyricsClick: () -> Unit,
    isQueueActive: Boolean,
    onQueueClick: () -> Unit,
    onMoreOptionsClick: () -> Unit,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    val inactive = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
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
                    QueueButton(isQueueActive, onQueueClick, inactive, accentColor)
                    LyricsToggleButton(isLyricsActive, onLyricsClick, inactive, accentColor)
                    ShuffleButton(isShuffleActive, onShuffleClick, inactive, accentColor)
                    RepeatButton(repeatMode, onRepeatClick, inactive, accentColor)
                }

                Spacer(Modifier.weight(1f))

                // Menu trigger
                AnimatedIconButton(onMoreOptionsClick) { mod ->
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = null,
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
            painter = painterResource(id = R.drawable.ic_lyrics),
            contentDescription = null,
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
            if (repeatMode == 2) {
                Text(
                    text = "1",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
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
            onClick()
            scope.launch {
                offsetY.animateTo(-4f, tween(100, easing = LinearOutSlowInEasing))
                offsetY.animateTo(0f, spring(Spring.DampingRatioMediumBouncy))
            }
        }
    ) { mod ->
        Icon(
            imageVector = Icons.AutoMirrored.Filled.QueueMusic,
            contentDescription = null,
            tint = if (isActive) active else inactive,
            modifier = mod
                .size(24.dp)
                .graphicsLayer { translationY = offsetY.value }
        )
    }
}

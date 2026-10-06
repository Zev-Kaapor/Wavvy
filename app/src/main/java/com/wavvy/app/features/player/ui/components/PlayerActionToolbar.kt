package com.wavvy.app.features.player.ui.components

// Compose animation
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
// Compose layouts and foundations
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
// Material 3 components
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
// Compose state and runtime
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
// UI styling and utilities
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
// Coroutines
import kotlinx.coroutines.launch
// Project resources
import com.wavvy.app.R
import com.wavvy.app.core.designsystem.icons.WavvyIcons
import com.wavvy.app.core.playback.RepeatMode

// Pill at the bottom of the open player with the queue, lyrics, shuffle, repeat and more options, as in the old Wavvy
@Composable
fun PlayerActionToolbar(
    repeatMode: RepeatMode,
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
    val inactive = MaterialTheme.colorScheme.onSurface.copy(alpha = PlayerDimens.ToolbarInactiveAlpha)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = PlayerDimens.ToolbarSide)
            .padding(bottom = toolbarBottomPadding()),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = PlayerDimens.ToolbarBackgroundAlpha),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = PlayerDimens.ToolbarPaddingHorizontal, vertical = PlayerDimens.ToolbarPaddingVertical),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(PlayerDimens.ToolbarSpacing),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    QueueButton(isQueueActive, onQueueClick, inactive, accentColor)
                    ToggleButton(WavvyIcons.Lyrics, isLyricsActive, onLyricsClick, inactive, accentColor)
                    ToggleButton(WavvyIcons.Shuffle, isShuffleActive, onShuffleClick, inactive, accentColor)
                    RepeatButton(repeatMode, onRepeatClick, inactive, accentColor)
                }

                Spacer(Modifier.weight(1f))

                AnimatedIconButton(onClick = onMoreOptionsClick) { scale ->
                    Icon(
                        imageVector = WavvyIcons.MoreVertical,
                        contentDescription = null,
                        tint = inactive,
                        modifier = scale.size(PlayerDimens.ToolbarIcon)
                    )
                }
            }
        }
    }
}

// Button that shrinks a little while pressed
@Composable
private fun AnimatedIconButton(
    onClick: () -> Unit,
    content: @Composable (Modifier) -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val isPressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) PlayerDimens.PressedScale else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "IconScale"
    )

    Box(
        modifier = Modifier
            .size(PlayerDimens.ToolbarButton)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        content(
            Modifier.graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
        )
    }
}

// Icon that takes the accent color while its mode is on
@Composable
internal fun ToggleButton(
    icon: ImageVector,
    isActive: Boolean,
    onClick: () -> Unit,
    inactive: Color,
    active: Color
) {
    AnimatedIconButton(onClick) { scale ->
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isActive) active else inactive,
            modifier = scale.size(PlayerDimens.ToolbarIcon)
        )
    }
}

// Repeat turns a full circle on each change, with a small one when it repeats a single song
@Composable
internal fun RepeatButton(
    repeatMode: RepeatMode,
    onClick: () -> Unit,
    inactive: Color,
    active: Color
) {
    val rotation = remember { Animatable(0f) }
    var lastMode by remember { mutableStateOf(repeatMode) }

    LaunchedEffect(repeatMode) {
        if (repeatMode != lastMode) {
            rotation.animateTo(rotation.value + PlayerDimens.FullTurn, spring(PlayerDimens.RotationDamping))
            lastMode = repeatMode
        }
    }

    AnimatedIconButton(onClick) { scale ->
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = WavvyIcons.Repeat,
                contentDescription = null,
                tint = if (repeatMode != RepeatMode.Off) active else inactive,
                modifier = scale
                    .size(PlayerDimens.ToolbarIcon)
                    .graphicsLayer { rotationZ = rotation.value }
            )
            if (repeatMode == RepeatMode.One) {
                Text(
                    text = stringResource(R.string.player_repeat_one),
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = PlayerDimens.RepeatOneSize),
                    color = active
                )
            }
        }
    }
}

// Queue icon that jumps a little when tapped
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
                offsetY.animateTo(PlayerDimens.QueueBouncePx, tween(PlayerDimens.QueueBounceMillis, easing = LinearOutSlowInEasing))
                offsetY.animateTo(0f, spring(Spring.DampingRatioMediumBouncy))
            }
        }
    ) { scale ->
        Icon(
            imageVector = WavvyIcons.QueueMusic,
            contentDescription = null,
            tint = if (isActive) active else inactive,
            modifier = scale
                .size(PlayerDimens.ToolbarIcon)
                .graphicsLayer { translationY = offsetY.value }
        )
    }
}

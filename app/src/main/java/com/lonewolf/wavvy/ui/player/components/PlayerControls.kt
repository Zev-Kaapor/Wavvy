package com.lonewolf.wavvy.ui.player.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun PlayerControls(
    progress: Float,
    isPlaying: Boolean,
    onPlayPauseToggle: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    screenWidth: Dp,
    screenHeight: Dp,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val haptic = LocalHapticFeedback.current

    // Sizes and positions
    val startX = screenWidth * 0.92f - 56.dp
    val startY = 12.dp
    val endX = (screenWidth - 160.dp) / 2

    // Change the screen percentage to move it down or up
    val endY = screenHeight * 0.80f

    val currentX = lerp(startX, endX, progress)
    val currentY = lerp(startY, endY, progress)
    val currentWidth = lerp(40.dp, 160.dp, progress)
    val currentHeight = lerp(40.dp, 68.dp, progress)
    val currentCorner = lerp(20.dp, 50.dp, progress)
    val currentIconSize = lerp(24.dp, 32.dp, progress)

    // Corner for side buttons
    val sideButtonCorner = RoundedCornerShape(18.dp)

    // Rotation animation
    val rotation by animateFloatAsState(
        targetValue = if (isPlaying) 180f else 0f,
        animationSpec = spring(dampingRatio = 0.6f),
        label = "rotation"
    )

    // Play/Pause alpha animation
    val playPauseAlpha by animateFloatAsState(
        targetValue = if (progress > 0.5f) 0.18f else 0.08f,
        animationSpec = tween(durationMillis = 300),
        label = "playPauseAlpha"
    )

    // Interactions
    val mainInteraction = remember { MutableInteractionSource() }
    val previousInteraction = remember { MutableInteractionSource() }
    val nextInteraction = remember { MutableInteractionSource() }

    val isMainPressed by mainInteraction.collectIsPressedAsState()
    val isPreviousPressed by previousInteraction.collectIsPressedAsState()
    val isNextPressed by nextInteraction.collectIsPressedAsState()

    val mainScale by animateFloatAsState(
        targetValue = if (isMainPressed) 0.88f else 1f,
        animationSpec = spring(dampingRatio = 0.4f),
        label = "mainScale"
    )

    // Weights for squish effect
    val previousWeight by animateFloatAsState(
        targetValue = if (isPreviousPressed) 0.65f
        else if (isMainPressed) 0.35f
        else 0.45f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 500f),
        label = "previousWeight"
    )

    val playPauseWeight by animateFloatAsState(
        targetValue = if (isMainPressed) 1.9f
        else if (isPreviousPressed || isNextPressed) 1.1f
        else 1.3f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 500f),
        label = "playPauseWeight"
    )

    val nextWeight by animateFloatAsState(
        targetValue = if (isNextPressed) 0.65f
        else if (isMainPressed) 0.35f
        else 0.45f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 500f),
        label = "nextWeight"
    )

    // Play/Pause button component
    val playPauseButton: @Composable (Modifier) -> Unit = { mod ->
        FilledIconButton(
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onPlayPauseToggle()
            },
            interactionSource = mainInteraction,
            shape = RoundedCornerShape(currentCorner),
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = colors.onSurface.copy(alpha = playPauseAlpha),
                contentColor = colors.onSurface
            ),
            modifier = mod.graphicsLayer {
                scaleX = mainScale
                scaleY = mainScale
            }
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                contentDescription = if (isPlaying) "Pause" else "Play",
                modifier = Modifier
                    .size(currentIconSize)
                    .graphicsLayer { rotationZ = rotation }
            )
        }
    }

    Box(modifier = modifier.fillMaxSize()) {

        val sideAlpha = ((progress - 0.7f) / 0.25f).coerceIn(0f, 1f)
        val useWeightMode = progress > 0.95f

        if (useWeightMode) {
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .offset(y = currentY + (currentHeight / 2) - 34.dp)
                    .padding(horizontal = 40.dp)
            ) {
                // Previous button
                FilledIconButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onPrevious()
                    },
                    interactionSource = previousInteraction,
                    shape = sideButtonCorner,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = colors.onSurface.copy(alpha = 0.08f),
                        contentColor = colors.onSurface
                    ),
                    modifier = Modifier
                        .height(68.dp)
                        .weight(previousWeight)
                ) {
                    Icon(Icons.Rounded.SkipPrevious, "Previous", Modifier.size(32.dp))
                }

                Spacer(Modifier.width(8.dp))

                playPauseButton(
                    Modifier
                        .height(68.dp)
                        .weight(playPauseWeight)
                )

                Spacer(Modifier.width(8.dp))

                // Next button
                FilledIconButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onNext()
                    },
                    interactionSource = nextInteraction,
                    shape = sideButtonCorner,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = colors.onSurface.copy(alpha = 0.08f),
                        contentColor = colors.onSurface
                    ),
                    modifier = Modifier
                        .height(68.dp)
                        .weight(nextWeight)
                ) {
                    Icon(Icons.Rounded.SkipNext, "Next", Modifier.size(32.dp))
                }
            }
        } else {
            playPauseButton(
                Modifier
                    .offset(x = currentX, y = currentY)
                    .width(currentWidth)
                    .height(currentHeight)
            )
        }

        // Side buttons fade
        if (progress > 0.6f && !useWeightMode) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .offset(y = currentY + (currentHeight / 2) - 34.dp)
                    .alpha(sideAlpha)
                    .padding(horizontal = 40.dp)
            ) {
                // Previous
                FilledIconButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onPrevious()
                    },
                    interactionSource = previousInteraction,
                    shape = sideButtonCorner,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = colors.onSurface.copy(alpha = 0.08f),
                        contentColor = colors.onSurface
                    ),
                    modifier = Modifier.size(68.dp)
                ) {
                    Icon(Icons.Rounded.SkipPrevious, "Previous", Modifier.size(32.dp))
                }

                Spacer(Modifier.width(160.dp))

                // Next
                FilledIconButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onNext()
                    },
                    interactionSource = nextInteraction,
                    shape = sideButtonCorner,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = colors.onSurface.copy(alpha = 0.08f),
                        contentColor = colors.onSurface
                    ),
                    modifier = Modifier.size(68.dp)
                ) {
                    Icon(Icons.Rounded.SkipNext, "Next", Modifier.size(32.dp))
                }
            }
        }
    }
}

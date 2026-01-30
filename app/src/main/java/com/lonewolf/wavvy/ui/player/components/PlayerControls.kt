package com.lonewolf.wavvy.ui.player.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.ui.unit.times

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
    val isDark = isSystemInDarkTheme()

    val startX = screenWidth * 0.92f - 56.dp
    val startY = 12.dp
    val targetWidth = 160.dp
    val endY = screenHeight * 0.80f

    val previousInteraction = remember { MutableInteractionSource() }
    val nextInteraction = remember { MutableInteractionSource() }
    val mainInteraction = remember { MutableInteractionSource() }

    val isPreviousPressed by previousInteraction.collectIsPressedAsState()
    val isNextPressed by nextInteraction.collectIsPressedAsState()
    val isMainPressed by mainInteraction.collectIsPressedAsState()

    val buttonContainerColor = if (isDark) {
        colors.onSurface.copy(alpha = 0.08f)
    } else {
        MaterialTheme.colorScheme.surface.copy(alpha = 0.3f)
    }

    val mainButtonContainerColor = if (isDark) {
        colors.onSurface.copy(alpha = if (progress > 0.5f) 0.18f else 0.08f)
    } else {
        MaterialTheme.colorScheme.surface.copy(alpha = 0.3f)
    }

    // Pressed button gets larger
    val previousWeight by animateFloatAsState(
        targetValue = if (isPreviousPressed) 1.1f else if (isMainPressed || isNextPressed) 0.5f else 0.7f,
        animationSpec = spring(0.6f, 500f),
        label = "previousWeight"
    )
    val playPauseWeight by animateFloatAsState(
        targetValue = if (isMainPressed) 2.2f else if (isPreviousPressed || isNextPressed) 1.2f else 1.5f,
        animationSpec = spring(0.6f, 500f),
        label = "playPauseWeight"
    )
    val nextWeight by animateFloatAsState(
        targetValue = if (isNextPressed) 1.1f else if (isMainPressed || isPreviousPressed) 0.5f else 0.7f,
        animationSpec = spring(0.6f, 500f),
        label = "nextWeight"
    )

    val expandedWidth = targetWidth * (playPauseWeight / 1.5f)

    val squishDisplacement = remember(previousWeight, nextWeight) {
        val totalWeight = previousWeight + playPauseWeight + nextWeight
        // Center shift based on weight difference
        val centerShift = (nextWeight - previousWeight) / totalWeight
        centerShift * (screenWidth.value.dp / 2.2f)
    }

    val finalX = (screenWidth / 2) - (expandedWidth / 2) - (squishDisplacement * progress)

    val currentX = lerp(startX, finalX, progress)
    val currentY = lerp(startY, endY, progress)
    val currentWidth = lerp(40.dp, expandedWidth, progress)
    val currentHeight = lerp(40.dp, 68.dp, progress)
    val currentCorner = lerp(20.dp, 50.dp, progress)
    val currentIconSize = lerp(24.dp, 32.dp, progress)

    val sideButtonSize = 68.dp
    val sideButtonCorner = RoundedCornerShape(18.dp)

    val rotation by animateFloatAsState(
        targetValue = if (isPlaying) 180f else 0f,
        animationSpec = spring(0.6f),
        label = "rotation"
    )

    Box(modifier = modifier.fillMaxSize()) {
        val sideAlpha = ((progress - 0.8f) / 0.15f).coerceIn(0f, 1f)

        // Background row for side buttons
        if (progress > 0.8f) {
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .offset(y = endY)
                    .padding(horizontal = 24.dp)
                    .alpha(sideAlpha)
            ) {
                // Previous
                Box(modifier = Modifier.height(sideButtonSize).weight(previousWeight)) {
                    FilledIconButton(
                        onClick = onPrevious,
                        interactionSource = previousInteraction,
                        shape = sideButtonCorner,
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = buttonContainerColor
                        ),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(Icons.Rounded.SkipPrevious, "Previous", Modifier.size(32.dp))
                    }
                }

                Spacer(Modifier.width(8.dp))
                Spacer(Modifier.height(sideButtonSize).weight(playPauseWeight))
                Spacer(Modifier.width(8.dp))

                // Next
                Box(modifier = Modifier.height(sideButtonSize).weight(nextWeight)) {
                    FilledIconButton(
                        onClick = onNext,
                        interactionSource = nextInteraction,
                        shape = sideButtonCorner,
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = buttonContainerColor
                        ),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(Icons.Rounded.SkipNext, "Next", Modifier.size(32.dp))
                    }
                }
            }
        }

        // Main Play/Pause
        FilledIconButton(
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onPlayPauseToggle()
            },
            interactionSource = mainInteraction,
            shape = RoundedCornerShape(currentCorner),
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = mainButtonContainerColor,
                contentColor = colors.onSurface
            ),
            modifier = Modifier
                .offset(x = currentX, y = currentY)
                .width(currentWidth)
                .height(currentHeight)
                .graphicsLayer {
                    val verticalSquish = if (isMainPressed) 0.95f else 1f
                    scaleY = verticalSquish
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
}

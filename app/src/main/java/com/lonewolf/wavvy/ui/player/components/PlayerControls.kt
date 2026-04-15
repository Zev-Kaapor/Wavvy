package com.lonewolf.wavvy.ui.player.components

// Compose animation mechanics
import androidx.compose.animation.core.*
// Foundation and layout
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
// Material 3 and icons
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.*
// State and UI tools
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp as lerpColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.times

// High-performance player controls with dynamic physics and morphing
@Composable
fun PlayerControls(
    progress: Float,
    isPlaying: Boolean,
    onPlayPauseToggle: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    screenWidth: Dp,
    screenHeight: Dp,
    modifier: Modifier = Modifier,
    isLandscape: Boolean = false
) {
    val haptic = LocalHapticFeedback.current

    // Layout anchoring points
    val baseWidthFraction = if (isLandscape) 0.55f else 0.92f
    val startX = screenWidth * baseWidthFraction - 56.dp
    val startY = 12.dp
    val targetWidth = if (isLandscape) 180.dp else 160.dp
    val targetHeight = if (isLandscape) 72.dp else 68.dp
    val endY = if (isLandscape) 180.dp else screenHeight * 0.80f
    val buttonGap = if (isLandscape) 5.dp else 8.dp

    // Interaction states
    val previousInteraction = remember { MutableInteractionSource() }
    val nextInteraction = remember { MutableInteractionSource() }
    val mainInteraction = remember { MutableInteractionSource() }

    val isPreviousPressed by previousInteraction.collectIsPressedAsState()
    val isNextPressed by nextInteraction.collectIsPressedAsState()
    val isMainPressed by mainInteraction.collectIsPressedAsState()

    // Glassmorphism color system
    val mainActiveColor = lerpColor(
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
        Color.White.copy(alpha = 0.1f),
        progress
    )
    val iconTintColor = lerpColor(
        MaterialTheme.colorScheme.onSurface,
        Color.White.copy(alpha = 1f),
        progress
    )

    // Physics-based weights for squish effect
    val previousWeight by animateFloatAsState(
        targetValue = if (isPreviousPressed) 1.1f else if (isMainPressed || isNextPressed) 0.5f else 0.7f,
        animationSpec = spring(0.6f, 500f),
        label = "PrevWeight"
    )
    val playPauseWeight by animateFloatAsState(
        targetValue = if (isMainPressed) 2.2f else if (isPreviousPressed || isNextPressed) 1.2f else 1.5f,
        animationSpec = spring(0.6f, 500f),
        label = "PlayWeight"
    )
    val nextWeight by animateFloatAsState(
        targetValue = if (isNextPressed) 1.1f else if (isMainPressed || isPreviousPressed) 0.5f else 0.7f,
        animationSpec = spring(0.6f, 500f),
        label = "NextWeight"
    )

    // Spatial interpolation logic
    val expandedWidth = targetWidth * (playPauseWeight / 1.5f)
    val squishDisplacement = remember(previousWeight, nextWeight, isLandscape) {
        val totalWeight = previousWeight + playPauseWeight + nextWeight
        val centerShift = (nextWeight - previousWeight) / totalWeight
        val rowWidth = if (isLandscape) targetWidth * 2.1f else screenWidth - 48.dp
        val maxShift = rowWidth / 2.2f
        centerShift * maxShift
    }

    // Calculated dimensions and positions
    val finalX = if (isLandscape) {
        val rightAreaStart = 320.dp
        val rightAreaWidth = screenWidth - rightAreaStart
        rightAreaStart + (rightAreaWidth / 2) - (expandedWidth / 2)
    } else {
        (screenWidth / 2) - (expandedWidth / 2)
    } - (squishDisplacement * progress)

    val currentX = lerp(startX, finalX, progress)
    val currentY = lerp(startY, endY, progress)
    val currentWidth = lerp(40.dp, expandedWidth, progress)
    val currentHeight = lerp(40.dp, targetHeight, progress)
    val currentCorner = lerp(20.dp, 50.dp, progress)
    val currentIconSize = lerp(24.dp, 32.dp, progress)

    // Icon rotation animation
    val rotation by animateFloatAsState(
        targetValue = if (isPlaying) 180f else 0f,
        animationSpec = spring(0.6f),
        label = "Rotation"
    )

    Box(modifier = modifier.fillMaxSize()) {
        // Skip controls layer
        if (progress > 0.8f) {
            val sideAlpha = ((progress - 0.8f) / 0.15f).coerceIn(0f, 1f)
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .then(
                        if (isLandscape) {
                            val rowWidth = targetWidth * 2.1f
                            Modifier
                                .width(rowWidth)
                                .offset(x = 320.dp + (screenWidth - 320.dp - rowWidth) / 2, y = endY)
                        } else {
                            Modifier
                                .fillMaxWidth()
                                .offset(y = endY)
                        }
                    )
                    .padding(horizontal = if (isLandscape) 0.dp else 24.dp)
                    .alpha(sideAlpha)
            ) {
                // Previous button
                Box(modifier = Modifier.height(targetHeight).weight(previousWeight)) {
                    FilledIconButton(
                        onClick = onPrevious,
                        interactionSource = previousInteraction,
                        shape = RoundedCornerShape(18.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = mainActiveColor,
                            contentColor = iconTintColor
                        ),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(Icons.Rounded.SkipPrevious, null, Modifier.size(32.dp))
                    }
                }

                Spacer(Modifier.width(buttonGap))
                Spacer(Modifier.height(targetHeight).weight(playPauseWeight))
                Spacer(Modifier.width(buttonGap))

                // Next button
                Box(modifier = Modifier.height(targetHeight).weight(nextWeight)) {
                    FilledIconButton(
                        onClick = onNext,
                        interactionSource = nextInteraction,
                        shape = RoundedCornerShape(18.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = mainActiveColor,
                            contentColor = iconTintColor
                        ),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(Icons.Rounded.SkipNext, null, Modifier.size(32.dp))
                    }
                }
            }
        }

        // Primary morphing Play/Pause button
        FilledIconButton(
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onPlayPauseToggle()
            },
            interactionSource = mainInteraction,
            shape = RoundedCornerShape(currentCorner),
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = mainActiveColor,
                contentColor = iconTintColor
            ),
            modifier = Modifier
                .offset(x = currentX, y = currentY)
                .width(currentWidth)
                .height(currentHeight)
                .graphicsLayer {
                    scaleY = if (isMainPressed) 0.95f else 1f
                }
        ) {
            // Morphing icon
            Icon(
                imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                contentDescription = null,
                modifier = Modifier
                    .size(currentIconSize)
                    .graphicsLayer { rotationZ = rotation }
            )
        }
    }
}

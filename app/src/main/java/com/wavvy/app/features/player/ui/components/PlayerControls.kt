package com.wavvy.app.features.player.ui.components

// Compose animation
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
// Compose layouts and foundations
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
// Material 3 components
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
// Compose state and runtime
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
// UI styling and utilities
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp as lerpColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.lerp
// Coroutines
import kotlinx.coroutines.delay
// Math
import kotlin.math.roundToInt
// Project resources
import com.wavvy.app.core.designsystem.icons.WavvyIcons
import com.wavvy.app.core.designsystem.theme.WavvyTheme

// Play button that grows from the corner of the pill into the big button of the open player, with the skip buttons beside it, as in the old Wavvy
// The opening is read only while measuring and drawing, the colors of the icons change in a few steps
@Composable
fun PlayerControls(
    progress: () -> Float,
    isPlaying: Boolean,
    isLoading: Boolean,
    onPlayPauseToggle: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    screenWidth: Dp,
    screenHeight: Dp,
    isLandscape: Boolean
) {
    val haptic = LocalHapticFeedback.current
    val onMedia = WavvyTheme.colors.onMedia
    val onSurface = MaterialTheme.colorScheme.onSurface

    // Where the button starts in the pill and how big it gets in the open player
    val widthFraction = if (isLandscape) PlayerDimens.MiniWidthFractionLandscape else PlayerDimens.MiniWidthFraction
    val startX = screenWidth * widthFraction - PlayerDimens.ButtonEndInset
    val targetWidth = if (isLandscape) PlayerDimens.ControlsWidthLandscape else PlayerDimens.ControlsWidth
    val targetHeight = if (isLandscape) PlayerDimens.ControlsHeightLandscape else PlayerDimens.ControlsHeight
    val endY = if (isLandscape) {
        PlayerDimens.ControlsTopLandscape
    } else {
        screenHeight - toolbarReservedHeight() - targetHeight - PlayerDimens.ControlsAboveToolbar
    }
    val buttonGap = if (isLandscape) PlayerDimens.ControlsGapLandscape else PlayerDimens.ControlsGap
    val rowWidth = if (isLandscape) targetWidth * PlayerDimens.ControlsRowWidthFactor else screenWidth - PlayerDimens.ControlsRowInset

    val previousInteraction = remember { MutableInteractionSource() }
    val nextInteraction = remember { MutableInteractionSource() }
    val mainInteraction = remember { MutableInteractionSource() }
    val previousPressed by previousInteraction.collectIsPressedWithMinDurationAsState()
    val nextPressed by nextInteraction.collectIsPressedWithMinDurationAsState()
    val mainPressed by mainInteraction.collectIsPressedWithMinDurationAsState()

    // Faint over the pill, glassy white over the picture of the open player
    val containerStart = onSurface.copy(alpha = PlayerDimens.ButtonContainerAlpha)
    val containerEnd = onMedia.copy(alpha = PlayerDimens.ButtonExpandedAlpha)
    val colorStep by remember(progress) { derivedStateOf { (progress() * PlayerDimens.ColorSteps).roundToInt() / PlayerDimens.ColorSteps.toFloat() } }
    val iconColor = lerpColor(onSurface, onMedia, colorStep)
    val showSkips by remember(progress) { derivedStateOf { progress() > PlayerDimens.SkipStart } }

    // The pressed button takes more room and squeezes the others
    val weightSpring = spring<Float>(dampingRatio = PlayerDimens.WeightDamping, stiffness = PlayerDimens.WeightStiffness)
    val previousWeight by animateFloatAsState(
        targetValue = when {
            previousPressed -> PlayerDimens.SideWeightPressed
            mainPressed -> PlayerDimens.SideWeightMainPressed
            nextPressed -> PlayerDimens.SideWeightOtherPressed
            else -> PlayerDimens.SideWeight
        },
        animationSpec = weightSpring,
        label = "PrevWeight"
    )
    val playWeight by animateFloatAsState(
        targetValue = if (mainPressed) PlayerDimens.PlayWeightPressed else PlayerDimens.PlayWeight,
        animationSpec = weightSpring,
        label = "PlayWeight"
    )
    val nextWeight by animateFloatAsState(
        targetValue = when {
            nextPressed -> PlayerDimens.SideWeightPressed
            mainPressed -> PlayerDimens.SideWeightMainPressed
            previousPressed -> PlayerDimens.SideWeightOtherPressed
            else -> PlayerDimens.SideWeight
        },
        animationSpec = weightSpring,
        label = "NextWeight"
    )

    val rotation by animateFloatAsState(
        targetValue = if (isPlaying) PlayerDimens.PlayingRotation else 0f,
        animationSpec = spring(PlayerDimens.RotationDamping),
        label = "Rotation"
    )

    Box(modifier = Modifier.fillMaxSize()) {
        // Skip buttons, only near the end of the opening
        if (showSkips) {
            val skipContainer = lerpColor(containerStart, containerEnd, colorStep)
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .then(
                        if (isLandscape) {
                            Modifier
                                .width(rowWidth)
                                .offset(
                                    x = PlayerDimens.ControlsAreaStartLandscape + (screenWidth - PlayerDimens.ControlsAreaStartLandscape - rowWidth) / 2,
                                    y = endY
                                )
                        } else {
                            Modifier
                                .fillMaxWidth()
                                .offset(y = endY)
                                .padding(horizontal = PlayerDimens.ControlsRowPadding)
                        }
                    )
                    .graphicsLayer { alpha = ((progress() - PlayerDimens.SkipStart) / PlayerDimens.SkipFadeLength).coerceIn(0f, 1f) }
            ) {
                SkipButton(
                    onClick = onPrevious,
                    interaction = previousInteraction,
                    containerColor = skipContainer,
                    iconColor = iconColor,
                    icon = { Icon(WavvyIcons.SkipPrevious, null, Modifier.size(PlayerDimens.ControlsIcon)) },
                    modifier = Modifier
                        .height(targetHeight)
                        .weight(previousWeight)
                )

                // The play button floats over this gap
                Spacer(Modifier.width(buttonGap))
                Spacer(Modifier.height(targetHeight).weight(playWeight))
                Spacer(Modifier.width(buttonGap))

                SkipButton(
                    onClick = onNext,
                    interaction = nextInteraction,
                    containerColor = skipContainer,
                    iconColor = iconColor,
                    icon = { Icon(WavvyIcons.SkipNext, null, Modifier.size(PlayerDimens.ControlsIcon)) },
                    modifier = Modifier
                        .height(targetHeight)
                        .weight(nextWeight)
                )
            }
        }

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                // Place and size follow the opening and the squeeze of the row, so it stays between the skip buttons
                .layout { measurable, constraints ->
                    val opening = progress()
                    val expandedWidth = targetWidth * (playWeight / PlayerDimens.PlayWeightBase)
                    val totalWeight = previousWeight + playWeight + nextWeight
                    val squish = rowWidth / PlayerDimens.SquishShiftDivisor * ((nextWeight - previousWeight) / totalWeight)
                    val finalX = if (isLandscape) {
                        val areaWidth = screenWidth - PlayerDimens.ControlsAreaStartLandscape
                        PlayerDimens.ControlsAreaStartLandscape + areaWidth / 2 - expandedWidth / 2
                    } else {
                        screenWidth / 2 - expandedWidth / 2
                    } - squish * opening

                    val width = lerp(PlayerDimens.Button, expandedWidth, opening).roundToPx()
                    val height = lerp(PlayerDimens.Button, targetHeight, opening).roundToPx()
                    val placeable = measurable.measure(Constraints.fixed(width, height))
                    layout(constraints.maxWidth, constraints.maxHeight) {
                        placeable.place(lerp(startX, finalX, opening).roundToPx(), lerp(PlayerDimens.ButtonTop, endY, opening).roundToPx())
                    }
                }
                .graphicsLayer {
                    shape = RoundedCornerShape(lerp(PlayerDimens.ButtonCorner, PlayerDimens.ControlsCorner, progress()))
                    clip = true
                    scaleY = if (mainPressed) PlayerDimens.ButtonPressedScale else 1f
                }
                .drawBehind { drawRect(lerpColor(containerStart, containerEnd, progress())) }
                .clickable(
                    interactionSource = mainInteraction,
                    indication = LocalIndication.current,
                    enabled = !isLoading,
                    role = Role.Button
                ) {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onPlayPauseToggle()
                }
        ) {
            // The icon is drawn at its open size and scaled down in the pill
            Box(
                modifier = Modifier.graphicsLayer {
                    val scale = lerp(PlayerDimens.ButtonIcon, PlayerDimens.ControlsIcon, progress()) / PlayerDimens.ControlsIcon
                    scaleX = scale
                    scaleY = scale
                }
            ) {
                MorphingLoadingIcon(
                    size = PlayerDimens.ControlsIcon,
                    color = iconColor,
                    strokeWidth = PlayerDimens.ControlsIcon * PlayerDimens.LoadingStrokeFraction,
                    isLoading = isLoading,
                    iconRotation = rotation
                ) {
                    Icon(
                        imageVector = if (isPlaying) WavvyIcons.Pause else WavvyIcons.PlayArrow,
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(PlayerDimens.ControlsIcon)
                    )
                }
            }
        }
    }
}

// Rounded skip button that fills the room its weight gives it
@Composable
private fun SkipButton(
    onClick: () -> Unit,
    interaction: MutableInteractionSource,
    containerColor: Color,
    iconColor: Color,
    icon: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        FilledIconButton(
            onClick = onClick,
            interactionSource = interaction,
            shape = RoundedCornerShape(PlayerDimens.SkipCorner),
            colors = IconButtonDefaults.filledIconButtonColors(containerColor = containerColor, contentColor = iconColor),
            modifier = Modifier.fillMaxSize()
        ) {
            icon()
        }
    }
}

// Icon that shrinks into a spinning arc while loading and grows back when ready
@Composable
fun MorphingLoadingIcon(
    size: Dp,
    color: Color,
    strokeWidth: Dp,
    isLoading: Boolean,
    modifier: Modifier = Modifier,
    iconRotation: Float = 0f,
    icon: @Composable () -> Unit
) {
    val morph by animateFloatAsState(
        targetValue = if (isLoading) 0f else 1f,
        animationSpec = tween(durationMillis = PlayerDimens.MorphMillis, easing = FastOutSlowInEasing),
        label = "MorphProgress"
    )

    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        // The arc spins only while it is on screen, so a playing song costs no animation
        if (morph < 1f) LoadingArc(color = color, strokeWidth = strokeWidth, morph = morph)

        if (morph > 0f) {
            Box(
                modifier = Modifier.graphicsLayer {
                    alpha = morph
                    scaleX = PlayerDimens.IconStartScale + (1f - PlayerDimens.IconStartScale) * morph
                    scaleY = PlayerDimens.IconStartScale + (1f - PlayerDimens.IconStartScale) * morph
                    rotationZ = iconRotation
                }
            ) {
                icon()
            }
        }
    }
}

// Spinning arc that fades out as the icon comes back
@Composable
private fun LoadingArc(
    color: Color,
    strokeWidth: Dp,
    morph: Float
) {
    val spin = rememberInfiniteTransition(label = "MorphSpin")
    val spinAngle by spin.animateFloat(
        initialValue = 0f,
        targetValue = PlayerDimens.FullTurn,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = PlayerDimens.SpinMillis, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "SpinAngle"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val stroke = strokeWidth.toPx()
        val radius = (size.minDimension - stroke) / 2
        rotate(degrees = spinAngle) {
            drawArc(
                color = color.copy(alpha = 1f - morph),
                startAngle = 0f,
                sweepAngle = PlayerDimens.LoadingSweep * (1f - morph),
                useCenter = false,
                topLeft = Offset(size.width / 2 - radius, size.height / 2 - radius),
                size = Size(radius * 2, radius * 2),
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
        }
    }
}

// Pressed state that stays on for a short while, so a quick tap still shows the squeeze
@Composable
private fun InteractionSource.collectIsPressedWithMinDurationAsState(): State<Boolean> {
    val isPressed = remember { mutableStateOf(false) }
    LaunchedEffect(this) {
        val presses = mutableListOf<PressInteraction.Press>()
        interactions.collect { interaction ->
            when (interaction) {
                is PressInteraction.Press -> {
                    presses.add(interaction)
                    isPressed.value = true
                }
                is PressInteraction.Release -> {
                    presses.remove(interaction.press)
                    if (presses.isEmpty()) {
                        delay(PlayerDimens.PressMinMillis)
                        isPressed.value = false
                    }
                }
                is PressInteraction.Cancel -> {
                    presses.remove(interaction.press)
                    if (presses.isEmpty()) {
                        delay(PlayerDimens.PressMinMillis)
                        isPressed.value = false
                    }
                }
            }
        }
    }
    return isPressed
}

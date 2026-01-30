package com.lonewolf.wavvy.ui.player.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private enum class DownloadState { Idle, Downloading, Done }

@Composable
private fun AnimatedIconButton(
    onClick: () -> Unit,
    contentScale: Float = 1f,
    content: @Composable (Modifier) -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()

    // Scale de feedback ao pressionar
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
                    clip = false
                }
        )
    }
}

@Composable
private fun RepeatButton(
    repeatMode: Int,
    onClick: () -> Unit,
    inactive: Color,
    active: Color
) {
    val rotation = remember { Animatable(0f) }
    var lastMode by remember { mutableStateOf(repeatMode) }

    // Giro a cada mudança de modo
    LaunchedEffect(repeatMode) {
        if (repeatMode != lastMode) {
            rotation.animateTo(
                rotation.value + 360f,
                spring(dampingRatio = 0.6f)
            )
            lastMode = repeatMode
        }
    }

    val scale by animateFloatAsState(
        targetValue = if (repeatMode > 0) 1.1f else 1f,
        animationSpec = spring(),
        label = "RepeatScale"
    )

    AnimatedIconButton(onClick, scale) { mod ->
        Box(contentAlignment = Alignment.Center) {

            // Ícone base
            Icon(
                imageVector = Icons.Default.Repeat,
                contentDescription = null,
                tint = if (repeatMode > 0) active else inactive,
                modifier = mod.graphicsLayer {
                    rotationZ = rotation.value
                }
            )

            // Indicador "Repeat One"
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

@Composable
private fun ShuffleButton(
    isActive: Boolean,
    onClick: () -> Unit,
    inactive: Color,
    active: Color
) {
    val scale by animateFloatAsState(
        targetValue = if (isActive) 1.15f else 1f,
        animationSpec = spring(),
        label = "ShuffleScale"
    )

    AnimatedIconButton(onClick, scale) { mod ->
        Icon(
            imageVector = Icons.Default.Shuffle,
            contentDescription = null,
            tint = if (isActive) active else inactive,
            modifier = mod
        )
    }
}

@Composable
private fun DownloadButton(
    onClick: () -> Unit,
    inactive: Color,
    active: Color
) {
    // Estado local
    var state by remember { mutableStateOf(DownloadState.Idle) }
    val progress = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

    // Escala por estado
    val scale by animateFloatAsState(
        targetValue = when (state) {
            DownloadState.Idle -> 1f
            DownloadState.Downloading -> 1.1f
            DownloadState.Done -> 1.25f
        },
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "DownloadScale"
    )

    AnimatedIconButton(
        onClick = {
            if (state != DownloadState.Idle) return@AnimatedIconButton
            onClick()

            scope.launch {
                state = DownloadState.Downloading
                progress.snapTo(0f)
                progress.animateTo(1f, tween(1800))
                state = DownloadState.Done
                delay(1200)
                state = DownloadState.Idle
            }
        }
    ) { mod ->
        AnimatedContent(
            targetState = state,
            transitionSpec = {
                (fadeIn() + scaleIn()).togetherWith(fadeOut() + scaleOut())
                    .using(SizeTransform(clip = false))
            },
            contentAlignment = Alignment.Center,
            label = "DownloadAnim"
        ) { current ->

            val finalModifier = mod.graphicsLayer {
                scaleX = scale
                scaleY = scale
            }

            when (current) {
                DownloadState.Idle ->
                    Icon(Icons.Outlined.Download, null, tint = inactive, modifier = finalModifier)

                DownloadState.Downloading ->
                    Canvas(finalModifier.size(20.dp)) {
                        drawArc(
                            color = active,
                            startAngle = -90f,
                            sweepAngle = progress.value * 360f,
                            useCenter = false,
                            style = Stroke(
                                width = 2.5.dp.toPx(),
                                cap = StrokeCap.Round
                            )
                        )
                    }

                DownloadState.Done ->
                    Icon(Icons.Default.CheckCircle, null, tint = active, modifier = finalModifier)
            }
        }
    }
}

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

    // Verifica se o sistema está no modo escuro
    val isDark = isSystemInDarkTheme()

    // Define a cor baseada no tema
    val pillBackgroundColor = if (isDark) {
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
    } else {
        MaterialTheme.colorScheme.surface.copy(alpha = 0.3f)
    }
    Box(
        modifier = modifier
            .fillMaxWidth()
            // Adjust horizontal padding to change pill width (Higher = Shorter pill)
            .padding(horizontal = 40.dp)
            .padding(bottom = 20.dp), // Increase this value to raise the toolbar
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = androidx.compose.foundation.shape.CircleShape,
            color = pillBackgroundColor,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 8.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left side actions
                Row(
                    // Adjust spacing between icons here
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

                // Pushes the "More" button to the end of the pill
                Spacer(modifier = Modifier.weight(1f))

                // Right side action (More options)
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

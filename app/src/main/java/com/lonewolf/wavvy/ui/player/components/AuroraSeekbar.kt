package com.lonewolf.wavvy.ui.player.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
fun AuroraSeekbar(
    progress: Float,
    duration: Long,
    isPlaying: Boolean,
    onSeek: (Float) -> Unit,
    onProgressUpdate: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val scope = rememberCoroutineScope()

    // Color
    val neonColor = MaterialTheme.colorScheme.tertiary
    val baseColor = if (isDark) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary
    val trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)

    val animatableProgress = rememberSaveable(
        saver = Saver(
            save = { it.value },
            restore = { Animatable(it) }
        )
    ) { Animatable(progress) }

    var isDragging by remember { mutableStateOf(false) }

    LaunchedEffect(progress, isPlaying) {
        if (!isDragging) {
            if (isPlaying) {
                animatableProgress.animateTo(
                    targetValue = progress,
                    animationSpec = tween(durationMillis = 1000, easing = LinearEasing)
                )
            } else {
                animatableProgress.snapTo(progress)
            }
        }
    }

    // Reporta o progresso para a MiniPill
    LaunchedEffect(animatableProgress.value) {
        onProgressUpdate(animatableProgress.value)
    }

    val thumbScale by animateFloatAsState(
        targetValue = if (isDragging) 1.5f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "ThumbScale"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isDragging = true
                        tryAwaitRelease()
                        isDragging = false
                    }
                ) { offset ->
                    val paddingPx = 16.dp.toPx()
                    val effectiveWidth = size.width - (paddingPx * 2)
                    val newProgress = ((offset.x - paddingPx) / effectiveWidth).coerceIn(0f, 1f)
                    scope.launch { animatableProgress.snapTo(newProgress) }
                    onSeek(newProgress)
                }
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { isDragging = true },
                    onDragEnd = { isDragging = false },
                    onDragCancel = { isDragging = false }
                ) { change, _ ->
                    change.consume()
                    val paddingPx = 16.dp.toPx()
                    val effectiveWidth = size.width - (paddingPx * 2)
                    val newProgress = ((change.position.x - paddingPx) / effectiveWidth).coerceIn(0f, 1f)

                    scope.launch { animatableProgress.snapTo(newProgress) }
                    onSeek(newProgress)
                }
            }
    ) {
        val currentPos = animatableProgress.value.coerceIn(0f, 1f)

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .align(Alignment.Center)
        ) {
            val width = size.width
            val height = size.height
            val centerY = height / 2
            val activeWidth = width * currentPos

            drawLine(
                color = trackColor,
                start = Offset(0f, centerY),
                end = Offset(width, centerY),
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round
            )

            val dynamicBrush = Brush.horizontalGradient(
                colorStops = arrayOf(
                    0.0f to baseColor,
                    0.5f to baseColor.copy(alpha = 0.8f),
                    0.8f to neonColor.copy(alpha = 0.9f),
                    1.0f to neonColor
                ),
                startX = 0f,
                endX = activeWidth.coerceAtLeast(1f)
            )

            drawLine(
                brush = dynamicBrush,
                start = Offset(0f, centerY),
                end = Offset(activeWidth, centerY),
                strokeWidth = 4.dp.toPx(),
                cap = StrokeCap.Round
            )

            val thumbCenter = Offset(activeWidth, centerY)
            drawCircle(
                color = neonColor.copy(alpha = 0.2f),
                radius = (10.dp.toPx() * thumbScale),
                center = thumbCenter
            )
            drawCircle(
                color = neonColor,
                radius = (5.dp.toPx() * thumbScale),
                center = thumbCenter
            )
        }
    }
}

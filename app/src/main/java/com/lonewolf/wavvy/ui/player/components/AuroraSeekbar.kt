package com.lonewolf.wavvy.ui.player.components

// Compose animation mechanics
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
// Material 3 and state
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
// UI tools and positioning
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
// Project resources
import com.lonewolf.wavvy.ui.theme.Poppins
import kotlinx.coroutines.launch

// Custom seekbar with adaptive aurora gradient
@Composable
fun AuroraSeekbar(
    progress: Float,
    duration: Long,
    isPlaying: Boolean,
    onSeek: (Float) -> Unit,
    onProgressUpdate: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val isDark = isSystemInDarkTheme()

    // Adaptive color palette
    val neonColor = if (isDark) MaterialTheme.colorScheme.tertiary else Color.Gray
    val baseColor = if (isDark) Color.White else MaterialTheme.colorScheme.primary
    val trackColor = if (isDark) Color.White.copy(alpha = 0.15f) else Color.Black.copy(alpha = 0.08f)
    val timeTextColor = MaterialTheme.colorScheme.onSurface

    val animatableProgress = rememberSaveable(
        saver = Saver(save = { it.value }, restore = { Animatable(it) })
    ) { Animatable(progress) }

    var isDragging by remember { mutableStateOf(false) }

    // Sync state logic
    LaunchedEffect(progress, isPlaying) {
        if (!isDragging) {
            animatableProgress.snapTo(progress)
        }
    }

    // Mini-player sync update
    LaunchedEffect(animatableProgress.value) {
        onProgressUpdate(animatableProgress.value)
    }

    // Thumb scale animation
    val thumbScale by animateFloatAsState(
        targetValue = if (isDragging) 1.5f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "ThumbScale"
    )

    Column(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
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
                val centerY = size.height / 2
                val activeWidth = width * currentPos

                // Background track
                drawLine(
                    color = trackColor,
                    start = Offset(0f, centerY),
                    end = Offset(width, centerY),
                    strokeWidth = 2.dp.toPx(),
                    cap = StrokeCap.Round
                )

                // Aurora gradient
                val dynamicBrush = Brush.horizontalGradient(
                    colorStops = arrayOf(
                        0.0f to baseColor.copy(alpha = 0.7f),
                        0.5f to baseColor,
                        0.9f to neonColor.copy(alpha = 0.9f),
                        1.0f to neonColor
                    ),
                    startX = 0f,
                    endX = activeWidth.coerceAtLeast(1f)
                )

                // Active line
                drawLine(
                    brush = dynamicBrush,
                    start = Offset(0f, centerY),
                    end = Offset(activeWidth, centerY),
                    strokeWidth = 4.dp.toPx(),
                    cap = StrokeCap.Round
                )

                // Thumb
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

        // Integrated Time Labels
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = formatTime(animatableProgress.value, duration, isCountdown = false),
                color = timeTextColor,
                fontSize = 12.sp,
                fontFamily = Poppins
            )
            Text(
                text = formatTime(animatableProgress.value, duration, isCountdown = true),
                color = timeTextColor,
                fontSize = 12.sp,
                fontFamily = Poppins
            )
        }
    }
}

// Time formatter helper
private fun formatTime(progress: Float, durationMs: Long, isCountdown: Boolean): String {
    if (durationMs <= 0 || durationMs == 225000L) {
        return if (isCountdown) "-0:00" else "0:00"
    }

    val timeToShow = if (isCountdown) {
        durationMs - (durationMs * progress).toLong()
    } else {
        (durationMs * progress).toLong()
    }

    val totalSeconds = (timeToShow / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60

    val formatted = "%d:%02d".format(minutes, seconds)
    return if (isCountdown) "-$formatted" else formatted
}

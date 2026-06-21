package com.lonewolf.wavvy.ui.player.components

// Compose animation mechanics
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.text.font.FontWeight
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

    // Pure monochrome white palette mapping
    val whitePure = Color.White
    val whiteMuted = Color.White.copy(alpha = 0.6f)
    val whiteTranslucent = Color.White.copy(alpha = 0.35f)

    val trackColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
    val timeTextColor = MaterialTheme.colorScheme.onSurfaceVariant

    val animatableProgress = rememberSaveable(
        saver = Saver(save = { it.value }, restore = { Animatable(it) })
    ) { Animatable(progress) }

    var isDragging by remember { mutableStateOf(false) }

    // Aurora wave continuous fluid animation
    val infiniteTransition = rememberInfiniteTransition(label = "AuroraWave")
    val waveOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "WaveOffset"
    )

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
        targetValue = if (isDragging) 1.4f else 1f,
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
                    strokeWidth = 3.dp.toPx(),
                    cap = StrokeCap.Round
                )

                // Shifting dynamic colors based on wave offset loop
                val startXGradient = -width + (waveOffset * width)
                val endXGradient = startXGradient + (width * 2)

                val dynamicBrush = Brush.horizontalGradient(
                    colorStops = arrayOf(
                        0.0f to whitePure,
                        0.3f to whiteMuted,
                        0.6f to whitePure,
                        1.0f to whiteTranslucent
                    ),
                    startX = startXGradient,
                    endX = endXGradient,
                    tileMode = TileMode.Repeated
                )

                // Active line
                if (activeWidth > 0f) {
                    drawLine(
                        brush = dynamicBrush,
                        start = Offset(0f, centerY),
                        end = Offset(activeWidth, centerY),
                        strokeWidth = 5.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }

                // Thumb
                val thumbCenter = Offset(activeWidth, centerY)
                drawCircle(
                    color = whitePure.copy(alpha = 0.2f),
                    radius = (11.dp.toPx() * thumbScale),
                    center = thumbCenter
                )
                drawCircle(
                    color = whitePure,
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
                fontSize = 11.sp,
                fontFamily = Poppins,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = formatTime(animatableProgress.value, duration, isCountdown = true),
                color = timeTextColor,
                fontSize = 11.sp,
                fontFamily = Poppins,
                fontWeight = FontWeight.Medium
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

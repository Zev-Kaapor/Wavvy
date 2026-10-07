package com.wavvy.app.features.player.ui.components

// Compose animation
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
// Compose layouts and foundations
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
// Material 3 components
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
// Compose state and runtime
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
// UI styling and utilities
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
// Coroutines
import kotlinx.coroutines.launch
// Project resources
import com.wavvy.app.R
import com.wavvy.app.core.designsystem.theme.WavvyTheme

// Milliseconds in a second and seconds in a minute, for the time labels
private const val MillisPerSecond = 1000L
private const val SecondsPerMinute = 60L

// Seekbar of the old Wavvy, a white line with a light that keeps flowing along it and the times under it
@Composable
fun AuroraSeekbar(
    progress: Float,
    durationMs: Long,
    onSeek: (Float) -> Unit,
    modifier: Modifier = Modifier,
    isActive: Boolean = true
) {
    val scope = rememberCoroutineScope()
    val onMedia = WavvyTheme.colors.onMedia
    val muted = onMedia.copy(alpha = PlayerDimens.WaveMutedAlpha)
    val translucent = onMedia.copy(alpha = PlayerDimens.WaveTranslucentAlpha)
    val trackColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = PlayerDimens.TrackAlpha)

    val position = remember { Animatable(progress) }
    var isDragging by remember { mutableStateOf(false) }
    var showRemaining by rememberSaveable { mutableStateOf(false) }

    // The light flows along the line, read only while drawing, and only while the seekbar shows
    val waveOffset by if (isActive) rememberWaveOffset() else remember { mutableFloatStateOf(0f) }

    // Glides between the readings of the player, jumps when the gap is big
    LaunchedEffect(progress) {
        if (isDragging) return@LaunchedEffect
        val gap = kotlin.math.abs(progress - position.value)
        if (position.value == 0f || progress == 0f || gap > PlayerDimens.SeekSnapDistance) {
            position.snapTo(progress)
        } else {
            position.animateTo(progress, tween(PlayerDimens.SeekGlideMillis, easing = LinearEasing))
        }
    }

    val thumbScale by animateFloatAsState(
        targetValue = if (isDragging) PlayerDimens.ThumbDragScale else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "ThumbScale"
    )

    // The labels change only when the second does, not on every frame of the line
    val elapsedSeconds by remember(durationMs) {
        derivedStateOf { (position.value * durationMs / MillisPerSecond).toLong() }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(PlayerDimens.SeekbarHeight)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = {
                            isDragging = true
                            tryAwaitRelease()
                            isDragging = false
                        }
                    ) { offset ->
                        val side = PlayerDimens.SeekbarSide.toPx()
                        val target = ((offset.x - side) / (size.width - side * 2)).coerceIn(0f, 1f)
                        scope.launch { position.snapTo(target) }
                        onSeek(target)
                    }
                }
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { isDragging = true },
                        onDragEnd = { isDragging = false },
                        onDragCancel = { isDragging = false }
                    ) { change, _ ->
                        change.consume()
                        val side = PlayerDimens.SeekbarSide.toPx()
                        val target = ((change.position.x - side) / (size.width - side * 2)).coerceIn(0f, 1f)
                        scope.launch { position.snapTo(target) }
                        onSeek(target)
                    }
                }
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = PlayerDimens.SeekbarSide)
                    .align(Alignment.Center)
            ) {
                val width = size.width
                val centerY = size.height / 2
                val activeWidth = width * position.value.coerceIn(0f, 1f)

                drawLine(
                    color = trackColor,
                    start = Offset(0f, centerY),
                    end = Offset(width, centerY),
                    strokeWidth = PlayerDimens.SeekbarTrack.toPx(),
                    cap = StrokeCap.Round
                )

                val gradientStart = -width + waveOffset * width
                if (activeWidth > 0f) {
                    drawLine(
                        brush = Brush.horizontalGradient(
                            0f to onMedia,
                            0.3f to muted,
                            0.6f to onMedia,
                            1f to translucent,
                            startX = gradientStart,
                            endX = gradientStart + width * 2,
                            tileMode = TileMode.Repeated
                        ),
                        start = Offset(0f, centerY),
                        end = Offset(activeWidth, centerY),
                        strokeWidth = PlayerDimens.SeekbarActive.toPx(),
                        cap = StrokeCap.Round
                    )
                }

                val thumbCenter = Offset(activeWidth, centerY)
                drawCircle(color = onMedia.copy(alpha = PlayerDimens.ThumbHaloAlpha), radius = PlayerDimens.ThumbHalo.toPx() * thumbScale, center = thumbCenter)
                drawCircle(color = onMedia, radius = PlayerDimens.Thumb.toPx() * thumbScale, center = thumbCenter)
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = PlayerDimens.SeekbarSide),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Tapping the elapsed time switches it to the time left
            Box(
                modifier = Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { if (durationMs > 0) showRemaining = !showRemaining }
                ),
                contentAlignment = Alignment.CenterStart
            ) {
                TimeLabel(hasDuration = durationMs > 0, alignment = Alignment.CenterStart) {
                    val totalSeconds = durationMs / MillisPerSecond
                    if (showRemaining) "-" + formatTime(totalSeconds - elapsedSeconds) else formatTime(elapsedSeconds)
                }
            }

            TimeLabel(hasDuration = durationMs > 0, alignment = Alignment.CenterEnd) {
                formatTime(durationMs / MillisPerSecond)
            }
        }
    }
}

// How far the light has flowed along the line, which goes from zero to one again and again
@Composable
private fun rememberWaveOffset(): State<Float> {
    val wave = rememberInfiniteTransition(label = "AuroraWave")

    return wave.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(PlayerDimens.WaveMillis, easing = LinearEasing), RepeatMode.Restart),
        label = "WaveOffset"
    )
}

// Time under the seekbar, sliding in when the length of the song becomes known
@Composable
private fun TimeLabel(
    hasDuration: Boolean,
    alignment: Alignment,
    text: () -> String
) {
    val unknown = stringResource(R.string.player_time_unknown)
    val style = MaterialTheme.typography.bodyMedium.copy(fontSize = PlayerDimens.TimeTextSize, fontWeight = FontWeight.Medium)

    AnimatedContent(
        targetState = hasDuration,
        transitionSpec = {
            val slide = tween<IntOffset>(PlayerDimens.TimeSlideMillis)
            val fade = tween<Float>(PlayerDimens.TimeSlideMillis)
            if (targetState) {
                (slideInVertically(slide) { it } + fadeIn(fade)).togetherWith(slideOutVertically(slide) { -it } + fadeOut(fade))
            } else {
                (slideInVertically(slide) { -it } + fadeIn(fade)).togetherWith(slideOutVertically(slide) { it } + fadeOut(fade))
            }.using(SizeTransform(clip = false))
        },
        contentAlignment = alignment,
        label = "TimeLabel"
    ) { known ->
        Text(
            text = if (known) text() else unknown,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = style
        )
    }
}

// Seconds written as minutes and seconds
internal fun formatTime(totalSeconds: Long): String {
    val seconds = totalSeconds.coerceAtLeast(0)
    return "%d:%02d".format(seconds / SecondsPerMinute, seconds % SecondsPerMinute)
}

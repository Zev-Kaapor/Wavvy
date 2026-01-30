package com.lonewolf.wavvy.ui.common

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import com.lonewolf.wavvy.ui.player.components.AlbumCover
import com.lonewolf.wavvy.ui.player.components.ExpandedPlayerContent
import com.lonewolf.wavvy.ui.player.components.PlayerControls
import com.lonewolf.wavvy.ui.player.components.SongInfo
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

fun lerpFloat(start: Float, stop: Float, fraction: Float): Float = start + fraction * (stop - start)

@Composable
fun PlayerContainer(
    isExpanded: Boolean,
    songTitle: String,
    artistName: String,
    imageUrl: String?,
    onPillClick: () -> Unit,
    onDismiss: () -> Unit,
    onProgressUpdate: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = androidx.compose.foundation.isSystemInDarkTheme()
    val config = LocalConfiguration.current
    val density = LocalDensity.current
    val screenHeight = config.screenHeightDp.dp
    val screenWidth = config.screenWidthDp.dp
    val scope = rememberCoroutineScope()

    var currentProgress by rememberSaveable { mutableFloatStateOf(0f) }
    val totalDuration = 225000L

    val bottomMargin = 90.dp
    val maxOffset = with(density) { (screenHeight - 64.dp - bottomMargin).toPx() }

    var savedOffsetY by rememberSaveable { mutableFloatStateOf(maxOffset + 150f) }
    var savedAlpha by rememberSaveable { mutableFloatStateOf(0f) }
    var isPlaying by rememberSaveable { mutableStateOf(false) }

    var isFirstComposition by rememberSaveable { mutableStateOf(true) }

    val containerAlpha = remember { Animatable(savedAlpha) }
    val offsetY = remember { Animatable(savedOffsetY) }

    LaunchedEffect(containerAlpha.value) { savedAlpha = containerAlpha.value }
    LaunchedEffect(offsetY.value) { savedOffsetY = offsetY.value }

    val progress = (1f - (offsetY.value / maxOffset)).coerceIn(0f, 1f)
    LaunchedEffect(progress) { onProgressUpdate(progress) }

    val currentWidthFraction = lerpFloat(0.92f, 1f, progress)
    val currentCorner = lerp(32.dp, 0.dp, progress)
    val currentHeight = if (progress > 0.01f) screenHeight + bottomMargin else 64.dp

    LaunchedEffect(Unit) {
        if (isFirstComposition) {
            launch { containerAlpha.animateTo(1f, tween(500)) }
            launch {
                offsetY.animateTo(
                    if (isExpanded) 0f else maxOffset,
                    spring(0.82f, 350f)
                )
            }
            isFirstComposition = false
        } else {
            containerAlpha.snapTo(1f)
            offsetY.snapTo(if (isExpanded) 0f else maxOffset)
        }
    }

    LaunchedEffect(isExpanded) {
        if (!isFirstComposition) {
            offsetY.animateTo(if (isExpanded) 0f else maxOffset, spring(0.85f, 400f))
        }
    }

    Box(
        modifier = modifier.alpha(containerAlpha.value),
        contentAlignment = Alignment.TopCenter
    ) {
        Surface(
            modifier = Modifier
                .offset { IntOffset(0, offsetY.value.roundToInt()) }
                .fillMaxWidth(currentWidthFraction)
                .height(currentHeight)
                .draggable(
                    orientation = Orientation.Vertical,
                    state = rememberDraggableState { delta ->
                        scope.launch {
                            if (!(offsetY.value >= maxOffset && delta > 0)) {
                                offsetY.snapTo((offsetY.value + delta).coerceIn(0f, maxOffset + 50f))
                            }
                        }
                    },
                    onDragStopped = { velocity ->
                        scope.launch {
                            if (velocity > 600 && offsetY.value >= maxOffset) {
                                containerAlpha.animateTo(0f, tween(200))
                                onDismiss()
                            } else {
                                val target = if (velocity < -700 || offsetY.value < maxOffset * 0.45f) 0f else maxOffset
                                offsetY.animateTo(target, spring(0.85f, 400f))
                                if ((target == 0f && !isExpanded) || (target == maxOffset && isExpanded)) {
                                    onPillClick()
                                }
                            }
                        }
                    }
                ),
            color = if (progress > 0.01f) {
                MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
            } else {
                MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
            },
            shape = RoundedCornerShape(currentCorner),
            shadowElevation = lerp(8.dp, 0.dp, progress),
            onClick = { if (progress < 0.1f) onPillClick() }
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Album Cover
                AlbumCover(
                    progress = progress,
                    songProgress = currentProgress,
                    screenWidth = screenWidth,
                    imageUrl = imageUrl
                )

                // Song Info (Animated transition)
                val textOffsetX = lerp(76.dp, 24.dp, progress)
                val textOffsetY = lerp(10.dp, 530.dp, progress)

                Box(
                    modifier = Modifier.offset(textOffsetX, textOffsetY)
                ) {
                    SongInfo(
                        title = songTitle,
                        artist = artistName,
                        progress = progress
                    )
                }

                // Expanded content (No more internal titles here)
                if (progress > 0.4f) {
                    ExpandedPlayerContent(
                        isExpanded = true,
                        songTitle = songTitle,
                        artistName = artistName,
                        onMinimize = onPillClick,
                        currentProgress = currentProgress,
                        onProgressChange = { currentProgress = it },
                        modifier = Modifier
                            .statusBarsPadding()
                            .alpha(((progress - 0.4f) * 2f).coerceIn(0f, 1f))
                    )
                }

                // Player controls
                PlayerControls(
                    progress = progress,
                    isPlaying = isPlaying,
                    onPlayPauseToggle = { isPlaying = !isPlaying },
                    onNext = { /* next */ },
                    onPrevious = { /* previous */ },
                    screenWidth = screenWidth,
                    screenHeight = screenHeight
                )
            }
        }
    }
}

package com.lonewolf.wavvy.ui.player

// Jetpack Compose animation and core
import android.annotation.SuppressLint
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
// Foundation and layout
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
// Material 3 and lifecycle hooks
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
// UI Utilities
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.lerp as lerpColor
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
// Player specific components
import com.lonewolf.wavvy.ui.player.components.*
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

// Main container handling transition between mini and expanded player
@SuppressLint("ConfigurationScreenWidthHeight")
@Composable
fun PlayerSheet(
    isExpanded: Boolean,
    songTitle: String,
    artistName: String,
    imageUrl: String?,
    songUrl: String?,
    onPillClick: () -> Unit,
    onDismiss: () -> Unit,
    onProgressUpdate: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val config = LocalConfiguration.current
    val density = LocalDensity.current
    val screenHeight = config.screenHeightDp.dp
    val screenWidth = config.screenWidthDp.dp
    val scope = rememberCoroutineScope()

    // Internal playback and init state
    var isLyricsActive by rememberSaveable { mutableStateOf(false) }

    BackHandler(enabled = isExpanded) {
        if (isLyricsActive) isLyricsActive = false else onPillClick()
    }

    var currentProgress by rememberSaveable { mutableFloatStateOf(0f) }
    var isPlaying by rememberSaveable { mutableStateOf(false) }
    var isFirstComposition by rememberSaveable { mutableStateOf(true) }

    // Dimensions for transition logic
    val bottomMargin = 90.dp
    val maxOffset = with(density) { (screenHeight - 64.dp - bottomMargin).toPx() }

    // Animation drivers
    val containerAlpha = remember { Animatable(0f) }
    val offsetY = remember { Animatable(maxOffset + 150f) }

    // Overall transition progress (0.0 to 1.0)
    val progress = (1f - (offsetY.value / maxOffset)).coerceIn(0f, 1f)

    // Dynamic UI transformations
    val currentWidthFraction = 0.92f + (progress * 0.08f)
    val currentCorner = lerp(32.dp, 0.dp, progress)
    val currentHeight = lerp(64.dp, screenHeight + bottomMargin, progress)
    val currentSurfaceColor = lerpColor(
        MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
        MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp),
        progress
    )

    // Initial entry animation sequence
    LaunchedEffect(Unit) {
        if (isFirstComposition) {
            launch { containerAlpha.animateTo(1f, tween(500)) }
            launch { offsetY.animateTo(if (isExpanded) 0f else maxOffset, spring(0.82f, 350f)) }
            isFirstComposition = false
        } else {
            containerAlpha.snapTo(1f)
            offsetY.snapTo(if (isExpanded) 0f else maxOffset)
        }
    }

    // Sync sheet offset with expanded state
    LaunchedEffect(isExpanded) {
        if (!isFirstComposition) {
            offsetY.animateTo(if (isExpanded) 0f else maxOffset, spring(0.85f, 400f))
            if (!isExpanded) isLyricsActive = false
        }
    }

    // Sync progress with parent
    LaunchedEffect(progress) { onProgressUpdate(progress) }

    Box(
        modifier = modifier.alpha(containerAlpha.value),
        contentAlignment = Alignment.TopCenter
    ) {
        // Main interactive surface
        Surface(
            modifier = Modifier
                .offset { IntOffset(0, offsetY.value.roundToInt()) }
                .fillMaxWidth(currentWidthFraction)
                .height(currentHeight)
                .draggable(
                    orientation = Orientation.Vertical,
                    state = rememberDraggableState { delta ->
                        scope.launch {
                            // Manual drag resistance and snapping
                            if (!(offsetY.value >= maxOffset && delta > 0)) {
                                offsetY.snapTo((offsetY.value + delta).coerceIn(0f, maxOffset + 50f))
                            }
                        }
                    },
                    onDragStopped = { velocity ->
                        scope.launch {
                             // Dismiss check
                            if (velocity > 600 && offsetY.value >= maxOffset) {
                                containerAlpha.animateTo(0f, tween(200))
                                onDismiss()
                            } else {
                                val target = if (velocity < -400 || (isExpanded.not() && offsetY.value < maxOffset * 0.75f)) {
                                    0f
                                } else {
                                    maxOffset
                                }

                                offsetY.animateTo(target, spring(0.85f, 400f))

                                // Trigger state change if snap resulted in a different final state
                                if ((target == 0f && !isExpanded) || (target == maxOffset && isExpanded)) {
                                    onPillClick()
                                }
                            }
                        }
                    }
                ),
            color = currentSurfaceColor,
            shape = RoundedCornerShape(currentCorner),
            shadowElevation = lerp(8.dp, 0.dp, progress),
            onClick = { if (progress < 0.1f) onPillClick() }
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Background artwork
                AlbumCover(
                    progress = progress,
                    songProgress = currentProgress,
                    screenWidth = screenWidth,
                    imageUrl = imageUrl,
                    showFrontCard = !isLyricsActive
                )

                // Shared metadata positioning
                Box(modifier = Modifier.fillMaxSize()) {
                    // Standard song info overlay
                    AnimatedVisibility(
                        visible = !isLyricsActive || progress < 0.8f,
                        enter = fadeIn(tween(400)),
                        exit = fadeOut(tween(400))
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            val textOffsetX = lerp(76.dp, 30.dp, progress)
                            val textOffsetY = lerp(15.dp, 550.dp, progress)
                            Box(modifier = Modifier.offset(textOffsetX, textOffsetY)) {
                                SongInfo(title = songTitle, artist = artistName, progress = progress)
                            }
                            if (progress > 0.7f) {
                                SongSideActions(
                                    songUrl = songUrl,
                                    onAddToPlaylist = { },
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .offset(x = (-30).dp, y = 565.dp)
                                        .alpha(((progress - 0.7f) * 3.33f).coerceIn(0f, 1f))
                                )
                            }
                        }
                    }

                    // Lyrics display overlay
                    AnimatedVisibility(
                        visible = isLyricsActive && progress >= 0.8f,
                        enter = fadeIn(tween(600)),
                        exit = fadeOut(tween(600))
                    ) {
                        LyricsView(
                            lyrics = emptyList(),
                            currentTimestamp = (currentProgress * 210000L).toLong(),
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                // Lyrics toggle gesture area
                if (progress > 0.9f) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(0.6f)
                            .align(Alignment.TopCenter)
                            .padding(top = 80.dp)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = { isLyricsActive = !isLyricsActive }
                            )
                    )
                }

                // Expanded view content overlay
                if (progress > 0.4f) {
                    ExpandedPlayerContent(
                        isExpanded = true,
                        songTitle = songTitle,
                        artistName = artistName,
                        onMinimize = onPillClick,
                        currentProgress = currentProgress,
                        onProgressChange = { currentProgress = it },
                        modifier = Modifier.alpha(((progress - 0.4f) * 2f).coerceIn(0f, 1f))
                    )
                }

                // Main playback controls (morphing)
                PlayerControls(
                    progress = progress,
                    isPlaying = isPlaying,
                    onPlayPauseToggle = { isPlaying = !isPlaying },
                    onNext = { },
                    onPrevious = { },
                    screenWidth = screenWidth,
                    screenHeight = screenHeight
                )
            }
        }
    }
}

package com.wavvy.app.features.player.ui

// Android core UI & lifecycle
import android.annotation.SuppressLint
import android.content.res.Configuration
import androidx.activity.compose.BackHandler
// Compose animations & transitions
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
// Compose foundation & gestures
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.surfaceColorAtElevation
// Compose runtime & state
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
// Compose graphics & layout helpers
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp as lerpColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp as lerpDp
import androidx.compose.ui.util.lerp as lerpFloat
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

// Project modules
import com.wavvy.app.core.designsystem.bottomsheet.LocalMenuState
import com.wavvy.app.features.player.ui.components.AlbumCover
import com.wavvy.app.features.player.ui.components.ExpandedPlayerContent
import com.wavvy.app.features.player.ui.components.PlaybackQueue
import com.wavvy.app.features.player.ui.components.PlayerControls
import com.wavvy.app.features.player.ui.components.QueueSong

@SuppressLint("ConfigurationScreenWidthHeight")
@Composable
fun PlayerSheet(
    modifier: Modifier = Modifier,
    isExpanded: Boolean,
    imageUrl: String?,
    songUrl: String?,
    initialTitle: String? = null,
    initialArtist: String? = null,
    onPillClick: () -> Unit,
    onDismiss: () -> Unit,
    onProgressUpdate: (Float) -> Unit,
    isQueueActive: Boolean,
    onQueueToggle: () -> Unit,
    isNavBarVisible: Boolean = true,
    showBorder: Boolean,
    playlist: SnapshotStateList<QueueSong> = remember { mutableStateListOf() },
    viewModel: PlayerViewModel = viewModel()
) {
    val config = LocalConfiguration.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val menuState = LocalMenuState.current
    val currentIsQueueActive by rememberUpdatedState(isQueueActive)

    val isPlaying by viewModel.isPlaying.collectAsState()
    val currentMediaItem by viewModel.currentMediaItem.collectAsState()
    val currentTrackInfo by viewModel.currentTrackInfo.collectAsState()
    val trackDuration by viewModel.duration.collectAsState()
    val playbackProgress by viewModel.progress.collectAsState()
    val backendQueue by viewModel.currentQueue.collectAsState()
    val isPlaybackBusy by viewModel.isBusy.collectAsState()

    LaunchedEffect(songUrl) {
        if (!songUrl.isNullOrBlank() && songUrl != currentMediaItem?.mediaId) {
            viewModel.loadAndPlay(
                youtubeUrl = songUrl,
                title = initialTitle.orEmpty(),
                artist = initialArtist.orEmpty(),
                imageUrl = imageUrl.orEmpty()
            )
        }
    }

    LaunchedEffect(backendQueue) {
        val currentIds = playlist.map { it.id }
        val newIds = backendQueue.map { it.id }

        if (currentIds != newIds) {
            // Queue only grew at the end (e.g. extraction finished, next item released)
            val isSimpleAppend = backendQueue.size >= playlist.size &&
                    currentIds == newIds.subList(0, playlist.size)

            if (isSimpleAppend) {
                // Append only the new items, keep existing ones untouched to avoid a visual jump
                if (backendQueue.size > playlist.size) {
                    playlist.addAll(backendQueue.subList(playlist.size, backendQueue.size))
                }
            } else {
                // Structural change (reorder, removal, queue swap) requires a full replace
                playlist.clear()
                playlist.addAll(backendQueue)
            }
        }
    }

    val trackInfo = rememberPlayerTrackInfo(
        currentTrackInfo = currentTrackInfo,
        currentMediaItem = currentMediaItem,
        imageUrl = imageUrl,
        initialTitle = initialTitle,
        initialArtist = initialArtist
    )

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val fullHeight = maxHeight
        val screenWidth = maxWidth

        var isLyricsActive by rememberSaveable { mutableStateOf(false) }
        var currentProgress by rememberSaveable { mutableFloatStateOf(0f) }
        var isFirstComposition by rememberSaveable { mutableStateOf(true) }

        val repeatMode by viewModel.repeatMode.collectAsState()
        var isShuffleActive by rememberSaveable { mutableStateOf(false) }

        var isQueueLocked by rememberSaveable { mutableStateOf(false) }

        var currentIndex by remember { mutableIntStateOf(0) }

        LaunchedEffect(currentMediaItem, playlist.toList()) {
            val resolvedIndex = viewModel.getCurrentIndex(playlist)
            if (resolvedIndex != currentIndex) {
                currentIndex = resolvedIndex
            }
        }

        var isFavorite by rememberSaveable { mutableStateOf(false) }

        val isLandscape = config.orientation == Configuration.ORIENTATION_LANDSCAPE
        val navInsets = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        val isGestureMode = navInsets <= 24.dp

        val navBarBottom = if (isGestureMode) 20.dp else navInsets + 8.dp
        val targetBottomMargin = if (isLandscape) {
            20.dp
        } else {
            if (isNavBarVisible) navBarBottom + 68.dp + 5.dp else navBarBottom
        }

        val bottomMargin by animateDpAsState(
            targetValue = targetBottomMargin,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMediumLow
            ),
            label = "bottomMarginAnimation"
        )

        val maxOffset = with(density) { (fullHeight - 64.dp - bottomMargin).toPx() }

        val containerAlpha = remember { Animatable(0f) }
        val offsetY = remember { Animatable(maxOffset + 150f) }
        val offsetX = remember { Animatable(0f) }

        var dismissDragOffset by remember { mutableFloatStateOf(0f) }

        val maxQueueOffset = with(density) { fullHeight.toPx() }
        val queueOffsetY = remember { Animatable(if (isQueueActive) 0f else maxQueueOffset) }
        var isDraggingQueue by remember { mutableStateOf(false) }

        val progress = (1f - (offsetY.value / maxOffset)).coerceIn(0f, 1f)

        // Back handler navigation flow management
        BackHandler(enabled = isExpanded || progress > 0.05f) {
            when {
                currentIsQueueActive -> {
                    scope.launch {
                        queueOffsetY.animateTo(
                            targetValue = maxQueueOffset,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioNoBouncy,
                                stiffness = Spring.StiffnessMediumLow
                            )
                        )
                        onQueueToggle()
                    }
                }
                isLyricsActive -> {
                    isLyricsActive = false
                }
                else -> {
                    onPillClick()
                }
            }
        }

        val baseWidthFraction = if (isLandscape) 0.55f else 0.92f
        val currentWidthFraction = baseWidthFraction + (progress * (1f - baseWidthFraction))
        val currentCorner = lerpDp(32.dp, 0.dp, progress)
        val currentCornerShape = RoundedCornerShape(currentCorner)
        val currentHeight = lerpDp(64.dp, fullHeight, progress)

        val currentSurfaceColor = lerpColor(
            MaterialTheme.colorScheme.surface.copy(alpha = 1f),
            MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp),
            progress
        )

        val lyricsBackgroundAlpha by animateFloatAsState(
            targetValue = if (isLyricsActive && progress > 0.9f) 0.35f else 0f,
            animationSpec = tween(600),
            label = "lyricsBackgroundAlpha"
        )

        val queueDragModifier = Modifier.pointerInput(maxQueueOffset) {
            detectDragGestures(
                onDragStart = {
                    isDraggingQueue = true
                },
                onDrag = { change, dragAmount ->
                    change.consume()
                    scope.launch {
                        val newOffset = queueOffsetY.value + dragAmount.y
                        queueOffsetY.snapTo(newOffset.coerceIn(0f, maxQueueOffset))
                    }
                },
                onDragEnd = {
                    isDraggingQueue = false
                    val closedFraction = (queueOffsetY.value / maxQueueOffset).coerceIn(0f, 1f)
                    val dragThreshold = maxQueueOffset * 0.10f

                    scope.launch {
                        val activeQueueState = currentIsQueueActive
                        val shouldClose = if (!activeQueueState) {
                            queueOffsetY.value > (maxQueueOffset - dragThreshold)
                        } else {
                            closedFraction > 0.5f
                        }

                        queueOffsetY.animateTo(
                            targetValue = if (shouldClose) maxQueueOffset else 0f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioLowBouncy,
                                stiffness = Spring.StiffnessMedium
                            )
                        )

                        if (shouldClose == activeQueueState) {
                            onQueueToggle()
                        }
                    }
                },
                onDragCancel = {
                    isDraggingQueue = false
                    scope.launch {
                        queueOffsetY.animateTo(
                            targetValue = if (currentIsQueueActive) 0f else maxQueueOffset,
                            animationSpec = spring()
                        )
                    }
                }
            )
        }

        LaunchedEffect(isQueueActive, maxQueueOffset) {
            if (!isDraggingQueue) {
                queueOffsetY.animateTo(
                    targetValue = if (isQueueActive) 0f else maxQueueOffset,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    )
                )
            }
        }

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

        LaunchedEffect(isExpanded) {
            if (!isFirstComposition) {
                offsetY.animateTo(if (isExpanded) 0f else maxOffset, spring(0.85f, 400f))
                if (!isExpanded) {
                    isLyricsActive = false
                    menuState.dismiss()
                    if (isQueueActive) onQueueToggle()
                }
            }
        }

        LaunchedEffect(progress) { onProgressUpdate(progress) }

        LaunchedEffect(playbackProgress) {
            currentProgress = playbackProgress
        }

        LaunchedEffect(maxOffset) {
            if (!isFirstComposition) {
                offsetY.snapTo(if (isExpanded) 0f else maxOffset)
            }
        }

        val pillBorderWidth = if (progress < 0.1f) 1.dp else lerpDp(0.5.dp, 0.dp, progress)
        val pillBorderColor = if (showBorder && progress < 0.1f) {
            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f)
        } else {
            MaterialTheme.colorScheme.onSurface.copy(alpha = lerpFloat(0.23f, 0f, progress))
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .alpha(containerAlpha.value),
            contentAlignment = Alignment.TopCenter
        ) {
            Surface(
                modifier = Modifier
                    .offset { IntOffset(offsetX.value.roundToInt(), offsetY.value.roundToInt()) }
                    .fillMaxWidth(currentWidthFraction)
                    .height(currentHeight)
                    .border(
                        width = pillBorderWidth,
                        color = pillBorderColor,
                        shape = currentCornerShape
                    )
                    .pointerInput(progress, maxOffset, playlist.size, currentIndex) {
                        if (progress < 0.1f && !currentIsQueueActive) {
                            detectDragGestures(
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    scope.launch {
                                        offsetX.snapTo(offsetX.value + dragAmount.x)
                                    }
                                },
                                onDragEnd = {
                                    scope.launch {
                                        val swipeThreshold = size.width * 0.2f
                                        val hasNext = playlist.size > 1 && currentIndex < playlist.size - 1
                                        val hasPrevious = playlist.size > 1 && currentIndex > 0

                                        if (offsetX.value > swipeThreshold) {
                                            if (hasPrevious) {
                                                viewModel.skipToPrevious()
                                            } else {
                                                viewModel.seekTo(0L)
                                            }
                                            offsetX.animateTo(0f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMediumLow))
                                        } else if (offsetX.value < -swipeThreshold) {
                                            if (hasNext) {
                                                viewModel.skipToNext()
                                            }
                                            offsetX.animateTo(0f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMediumLow))
                                        } else {
                                            offsetX.animateTo(0f, spring())
                                        }
                                    }
                                },
                                onDragCancel = {
                                    scope.launch { offsetX.animateTo(0f, spring()) }
                                }
                            )
                        }
                    }
                    .draggable(
                        orientation = Orientation.Vertical,
                        state = rememberDraggableState { delta ->
                            scope.launch {
                                if (currentIsQueueActive) return@launch

                                if (offsetY.value >= maxOffset && delta > 0) {
                                    dismissDragOffset += delta
                                    val fadeProgress = (dismissDragOffset / 300f).coerceIn(0f, 1f)
                                    containerAlpha.snapTo(1f - fadeProgress)
                                } else if (dismissDragOffset > 0f && delta < 0) {
                                    dismissDragOffset = (dismissDragOffset + delta).coerceAtLeast(0f)
                                    val fadeProgress = (dismissDragOffset / 300f).coerceIn(0f, 1f)
                                    containerAlpha.snapTo(1f - fadeProgress)
                                } else {
                                    offsetY.snapTo((offsetY.value + delta).coerceIn(0f, maxOffset))
                                }
                            }
                        },
                        onDragStopped = { velocity ->
                            scope.launch {
                                if (currentIsQueueActive) return@launch

                                if (dismissDragOffset > 0f) {
                                    if (dismissDragOffset > 300f || velocity > 800f) {
                                        launch { containerAlpha.animateTo(0f, tween(300)) }
                                        launch { offsetY.animateTo(maxOffset + 150f, tween(300)) }
                                        viewModel.stopPlayback()
                                        onDismiss()
                                    } else {
                                        dismissDragOffset = 0f
                                        containerAlpha.animateTo(1f, spring())
                                    }
                                } else {
                                    val shouldExpand = velocity < 0f || (velocity == 0f && offsetY.value < 0f)
                                    val target = if (shouldExpand) 0f else maxOffset

                                    offsetY.animateTo(target, spring(0.85f, 400f))
                                    if ((target == 0f && !isExpanded) || (target == maxOffset && isExpanded)) {
                                        if (currentIsQueueActive) onQueueToggle()
                                        onPillClick()
                                    }
                                }
                            }
                        },
                        enabled = !isQueueActive
                    ),
                color = currentSurfaceColor,
                shape = currentCornerShape,
                shadowElevation = 0.dp,
                tonalElevation = 0.dp,
                onClick = { if (progress < 0.1f && offsetX.value == 0f) onPillClick() }
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    AlbumCover(
                        progress = progress,
                        songProgress = currentProgress,
                        screenWidth = screenWidth,
                        imageUrl = trackInfo.activeImageUrl,
                        showFrontCard = !isLyricsActive,
                        isLandscape = isLandscape
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = lyricsBackgroundAlpha))
                            .then(
                                if (isLyricsActive && progress > 0.9f) {
                                    Modifier.clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null,
                                        onClick = { isLyricsActive = false }
                                    )
                                } else Modifier
                            )
                    )

                    Box(modifier = Modifier.fillMaxSize()) {
                        PlayerExpandedHeader(
                            isLyricsActive = isLyricsActive,
                            progress = progress,
                            isLandscape = isLandscape,
                            fullHeight = fullHeight,
                            screenWidth = screenWidth,
                            songTitle = trackInfo.title,
                            cleanArtistName = trackInfo.cleanArtistName,
                            songUrl = songUrl,
                            isFavorite = isFavorite,
                            onFavoriteClick = { isFavorite = !isFavorite }
                        )

                        PlayerLyricsOverlay(
                            isLyricsActive = isLyricsActive,
                            progress = progress,
                            isLandscape = isLandscape,
                            currentProgress = currentProgress,
                            trackDuration = trackDuration,
                            songTitle = trackInfo.title,
                            cleanArtistName = trackInfo.cleanArtistName,
                            onSeek = { timestamp ->
                                currentProgress = if (trackDuration > 0) timestamp.toFloat() / trackDuration else 0f
                                viewModel.seekTo(timestamp)
                            },
                            onDismiss = { isLyricsActive = false }
                        )
                    }

                    if (progress > 0.9f && !isLyricsActive) {
                        Box(
                            modifier = Modifier
                                .then(
                                    if (isLandscape) {
                                        Modifier
                                            .offset(40.dp, 40.dp)
                                            .size(280.dp)
                                    } else {
                                        Modifier
                                            .fillMaxWidth()
                                            .fillMaxHeight(0.6f)
                                            .align(Alignment.TopCenter)
                                            .padding(top = 80.dp)
                                    }
                                )
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = {
                                        isLyricsActive = true
                                        if (isQueueActive) onQueueToggle()
                                    }
                                )
                        )
                    }

                    if (progress > 0.9f) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(if (isLandscape) 85.dp else 170.dp)
                                .align(Alignment.BottomCenter)
                                .then(queueDragModifier)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = {
                                        if (progress >= 0.95f) {
                                            onQueueToggle()
                                        }
                                    }
                                )
                        )
                    }

                    if (progress > 0.4f) {
                        ExpandedPlayerContent(
                            isExpanded = true,
                            onMinimize = onPillClick,
                            currentProgress = currentProgress,
                            duration = trackDuration,
                            isPlaying = isPlaying,
                            onProgressChange = {
                                currentProgress = it
                                viewModel.seekTo((it * trackDuration).toLong())
                            },
                            isLyricsActive = isLyricsActive,
                            onLyricsToggle = {
                                isLyricsActive = !isLyricsActive
                                if (isLyricsActive && isQueueActive) onQueueToggle()
                            },
                            isQueueActive = isQueueActive,
                            onQueueToggle = onQueueToggle,
                            repeatMode = repeatMode,
                            onRepeatClick = { viewModel.toggleRepeatMode() },
                            isShuffleActive = isShuffleActive,
                            onShuffleClick = {
                                isShuffleActive = !isShuffleActive
                                if (isShuffleActive) viewModel.shuffleQueue() else viewModel.unshuffleQueue()
                            },
                            onMoreClick = {
                                scope.launch {
                                    menuState.show()
                                }
                            },
                            isLandscape = isLandscape,
                            screenHeight = fullHeight,
                            modifier = Modifier.alpha(((progress - 0.4f) * 2f).coerceIn(0f, 1f))
                        )
                    }

                    PlayerControls(
                        progress = progress,
                        isPlaying = isPlaying,
                        isLoading = isPlaybackBusy,
                        onPlayPauseToggle = { viewModel.togglePlayPause() },
                        onNext = { viewModel.skipToNext() },
                        onPrevious = { viewModel.skipToPrevious() },
                        screenWidth = screenWidth,
                        screenHeight = fullHeight,
                        isLandscape = isLandscape,
                        isLyricsActive = isLyricsActive,
                        showBorder = showBorder
                    )

                    // Render queue with precise offset check to eliminate ghosting artifacts
                    if (progress >= 0.8f && (isQueueActive || queueOffsetY.value < maxQueueOffset)) {
                        val queueRevealProgress = (1f - (queueOffsetY.value / maxQueueOffset)).coerceIn(0f, 1f)
                        PlaybackQueue(
                            playlist = playlist,
                            currentIndex = currentIndex,
                            isLocked = isQueueLocked,
                            onLockToggle = { isQueueLocked = it },
                            isPlaying = isPlaying,
                            onIndexChange = { index ->
                                currentIndex = index
                                viewModel.loadAndPlayQueue(playlist, index)
                            },
                            repeatMode = repeatMode,
                            onRepeatClick = { viewModel.toggleRepeatMode() },
                            isShuffleActive = isShuffleActive,
                            onShuffleClick = {
                                isShuffleActive = !isShuffleActive
                                if (isShuffleActive) viewModel.shuffleQueue() else viewModel.unshuffleQueue()
                            },
                            onClose = {
                                scope.launch {
                                    queueOffsetY.animateTo(maxQueueOffset, spring(Spring.DampingRatioNoBouncy, Spring.StiffnessMediumLow))
                                    onQueueToggle()
                                }
                            },
                            viewModel = viewModel,
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    translationY = queueOffsetY.value
                                    alpha = queueRevealProgress
                                }
                        )
                    }
                }
            }
        }
    }
}
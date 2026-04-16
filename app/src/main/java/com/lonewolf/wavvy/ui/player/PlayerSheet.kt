package com.lonewolf.wavvy.ui.player

// Jetpack Compose animation and core
import android.annotation.SuppressLint
import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
// Foundation and layout
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
// Material 3 and lifecycle hooks
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshots.SnapshotStateList
// UI Utilities
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp as lerpColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp
// Project resources
import com.lonewolf.wavvy.ui.theme.Poppins
// Player specific components
import com.lonewolf.wavvy.ui.player.components.*
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

// Main player sheet container
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
    isQueueActive: Boolean,
    onQueueToggle: () -> Unit,
    modifier: Modifier = Modifier,
    playlist: SnapshotStateList<QueueSong> = remember { mutableStateListOf() }
) {
    val config = LocalConfiguration.current
    val density = LocalDensity.current
    val screenHeight = config.screenHeightDp.dp
    val screenWidth = config.screenWidthDp.dp
    val scope = rememberCoroutineScope()

    // UI state controllers
    var isLyricsActive by rememberSaveable { mutableStateOf(false) }
    var currentProgress by rememberSaveable { mutableFloatStateOf(0f) }
    var isPlaying by rememberSaveable { mutableStateOf(false) }
    var isFirstComposition by rememberSaveable { mutableStateOf(true) }
    var showMoreOptions by rememberSaveable { mutableStateOf(false) }

    // Persistent playback modes state
    var repeatMode by rememberSaveable { mutableIntStateOf(0) }
    var isShuffleActive by rememberSaveable { mutableStateOf(false) }

    // Persistent queue lock state
    var isQueueLocked by rememberSaveable { mutableStateOf(false) }

    var currentIndex by remember { mutableIntStateOf(0) }

    // Favorite state synchronization
    var isFavorite by rememberSaveable { mutableStateOf(false) }

    // Motion parameters
    val isLandscape = config.orientation == Configuration.ORIENTATION_LANDSCAPE
    val bottomMargin = if (isLandscape) 20.dp else 95.dp
    val maxOffset = with(density) { (screenHeight - 64.dp - bottomMargin).toPx() }

    // Sheet animation state
    val containerAlpha = remember { Animatable(0f) }
    val offsetY = remember { Animatable(maxOffset + 150f) }

    // Normalized transition progress
    val progress = (1f - (offsetY.value / maxOffset)).coerceIn(0f, 1f)

    // Navigation and back behavior
    BackHandler(enabled = isExpanded || progress > 0.05f) {
        if (showMoreOptions) {
            showMoreOptions = false
        } else if (isQueueActive) {
            onQueueToggle()
        } else if (isLyricsActive) {
            isLyricsActive = false
        } else {
            onPillClick()
        }
    }

    // Derived style values
    val baseWidthFraction = if (isLandscape) 0.55f else 0.92f
    val currentWidthFraction = baseWidthFraction + (progress * (1f - baseWidthFraction))
    val currentCorner = lerp(32.dp, 0.dp, progress)
    val currentHeight = lerp(64.dp, screenHeight + bottomMargin, progress)
    val currentSurfaceColor = lerpColor(
        MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
        MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp),
        progress
    )

    // Background darkening for lyrics mode
    val lyricsBackgroundAlpha by animateFloatAsState(
        targetValue = if (isLyricsActive && progress > 0.9f) 0.35f else 0f,
        animationSpec = tween(600),
        label = "lyricsBackgroundAlpha"
    )

    // Isolated drag modifier for the queue
    val queueDragModifier = Modifier.draggable(
        orientation = Orientation.Vertical,
        state = rememberDraggableState { /* No-op: Prevent moving player offset */ },
        onDragStopped = { velocity ->
            if (velocity > 600) {
                onQueueToggle()
            }
        }
    )

    // Entry animation sequence
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

    // Expanded state synchronization
    LaunchedEffect(isExpanded) {
        if (!isFirstComposition) {
            offsetY.animateTo(if (isExpanded) 0f else maxOffset, spring(0.85f, 400f))
            if (!isExpanded) {
                isLyricsActive = false
                showMoreOptions = false
                // Ensure queue is closed when player minimizes
                if (isQueueActive) onQueueToggle()
            }
        }
    }

    // External progress callback
    LaunchedEffect(progress) { onProgressUpdate(progress) }

    // Orientation change correction
    LaunchedEffect(maxOffset) {
        if (!isFirstComposition) {
            offsetY.snapTo(if (isExpanded) 0f else maxOffset)
        }
    }

    Box(
        modifier = modifier.alpha(containerAlpha.value),
        contentAlignment = Alignment.TopCenter
    ) {
        // Core interactive surface
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
                            // Detect swipe up to open queue when expanded
                            if (isExpanded && offsetY.value <= 10f && velocity < -500) {
                                onQueueToggle()
                            }
                            // Close player when swiping down from mini player
                            else if (velocity > 600 && offsetY.value >= maxOffset) {
                                containerAlpha.animateTo(0f, tween(200))
                                onDismiss()
                            }
                            // Default snapping logic
                            else {
                                val target = if (velocity < -400 || (isExpanded.not() && offsetY.value < maxOffset * 0.75f)) 0f else maxOffset
                                offsetY.animateTo(target, spring(0.85f, 400f))
                                if ((target == 0f && !isExpanded) || (target == maxOffset && isExpanded)) onPillClick()
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
                // Background album artwork
                AlbumCover(
                    progress = progress,
                    songProgress = currentProgress,
                    screenWidth = screenWidth,
                    imageUrl = imageUrl,
                    showFrontCard = !isLyricsActive,
                    isLandscape = isLandscape
                )

                // Adaptive darkening for lyrics readability
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = lyricsBackgroundAlpha))
                )

                // Layout layering
                Box(modifier = Modifier.fillMaxSize()) {
                    // Song info and actions layer
                    AnimatedVisibility(
                        visible = !isLyricsActive || progress < 0.8f,
                        enter = fadeIn(tween(400)),
                        exit = fadeOut(tween(400))
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            // Song info and actions
                            val textOffsetX = if (isLandscape) lerp(76.dp, 320.dp, progress) else lerp(76.dp, 30.dp, progress)
                            val textOffsetY = if (isLandscape) lerp(6.dp, 75.dp, progress) else lerp(6.dp, 550.dp, progress)
                            val infoWidth = if (isLandscape) screenWidth - textOffsetX else screenWidth - 60.dp

                            Box(
                                modifier = Modifier
                                    .offset(textOffsetX, textOffsetY)
                                    .width(infoWidth)
                            ) {
                                SongInfo(
                                    title = songTitle, 
                                    artist = artistName, 
                                    progress = progress,
                                    isLandscape = isLandscape
                                )
                            }

                            if (progress > 0.7f) {
                                // Side actions (Favorite, Share)
                                SongSideActions(
                                    songUrl = songUrl,
                                    isFavorite = isFavorite,
                                    onFavoriteClick = { isFavorite = !isFavorite },
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .offset(
                                            x = if (isLandscape) (-20).dp else (-30).dp, 
                                            y = if (isLandscape) 85.dp else 565.dp
                                        )
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
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight()
                                .padding(top = 40.dp, bottom = 320.dp)
                                .pointerInput(Unit) {
                                    detectDragGestures { change, _ -> change.consume() }
                                }
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = { isLyricsActive = false }
                                )
                        ) {
                            // Scrolling lyrics view
                            Box(modifier = Modifier.padding(top = 80.dp)) {
                                LyricsView(
                                    lyrics = null,
                                    translation = null,
                                    isSynced = true,
                                    currentPosition = (currentProgress * 210000L).toLong(),
                                    onSeek = { timestamp ->
                                        currentProgress = timestamp / 210000f
                                    },
                                    alignment = LyricsAlignment.CENTER,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }

                            // Song and Artist Header
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 24.dp)
                                    .align(Alignment.TopCenter),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = songTitle,
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontFamily = Poppins,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    ),
                                    color = Color.White,
                                    maxLines = 1
                                )
                                Text(
                                    text = artistName,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontFamily = Poppins,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 12.sp
                                    ),
                                    color = Color.White.copy(alpha = 0.7f),
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }

                // Lyrics trigger area
                if (progress > 0.9f && !isLyricsActive) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(0.6f)
                            .align(Alignment.TopCenter)
                            .padding(top = 80.dp)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = { isLyricsActive = true }
                            )
                    )
                }

                // Bottom queue trigger area (One-tap to open queue)
                if (progress > 0.9f && !isQueueActive) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(112.dp)
                            .align(Alignment.BottomCenter)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = onQueueToggle
                            )
                    )
                }

                // Main player controls and expanded view content
                if (progress > 0.4f) {
                    ExpandedPlayerContent(
                        isExpanded = true,
                        onMinimize = onPillClick,
                        currentProgress = currentProgress,
                        onProgressChange = { currentProgress = it },
                        isLyricsActive = isLyricsActive,
                        onLyricsToggle = {
                            isLyricsActive = !isLyricsActive
                            if (isLyricsActive && isQueueActive) onQueueToggle()
                        },
                        isQueueActive = isQueueActive,
                        onQueueToggle = onQueueToggle,
                        repeatMode = repeatMode,
                        onRepeatClick = { repeatMode = (repeatMode + 1) % 3 },
                        isShuffleActive = isShuffleActive,
                        onShuffleClick = { isShuffleActive = !isShuffleActive },
                        onMoreClick = { showMoreOptions = true },
                        isLandscape = isLandscape,
                        modifier = Modifier.alpha(((progress - 0.4f) * 2f).coerceIn(0f, 1f))
                    )
                }

                // Global playback controls
                PlayerControls(
                    progress = progress,
                    isPlaying = isPlaying,
                    onPlayPauseToggle = { isPlaying = !isPlaying },
                    onNext = { },
                    onPrevious = { },
                    screenWidth = screenWidth,
                    screenHeight = screenHeight,
                    isLandscape = isLandscape
                )

                // Playback Queue overlay with entrance/exit animation
                AnimatedVisibility(
                    visible = isQueueActive && progress >= 0.8f,
                    enter = slideInVertically(
                        initialOffsetY = { fullHeight -> fullHeight },
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioLowBouncy,
                            stiffness = Spring.StiffnessLow
                        )
                    ) + fadeIn(),
                    exit = slideOutVertically(
                        targetOffsetY = { fullHeight -> fullHeight },
                        animationSpec = tween(durationMillis = 300)
                    ) + fadeOut(),
                    modifier = Modifier.fillMaxSize()
                ) {
                    PlaybackQueue(
                        playlist = playlist,
                        currentIndex = currentIndex,
                        isLocked = isQueueLocked,
                        onLockToggle = { isQueueLocked = it },
                        isPlaying = isPlaying,
                        onIndexChange = { currentIndex = it },
                        repeatMode = repeatMode,
                        onRepeatClick = { repeatMode = (repeatMode + 1) % 3 },
                        isShuffleActive = isShuffleActive,
                        onShuffleClick = { isShuffleActive = !isShuffleActive },
                        onClose = onQueueToggle,
                        dragModifier = queueDragModifier,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // More Options Bottom Sheet
                if (showMoreOptions) {
                    PlayerMoreOptions(
                        songTitle = songTitle,
                        artistName = artistName,
                        onDismiss = { showMoreOptions = false },
                        onActionClick = { action ->
                            showMoreOptions = false
                        }
                    )
                }
            }
        }
    }
}

package com.lonewolf.wavvy.ui.player

// Jetpack Compose animation and core
import android.annotation.SuppressLint
import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.lerp as lerpColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import androidx.lifecycle.viewmodel.compose.viewModel
// Project resources
import com.lonewolf.wavvy.R
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
    imageUrl: String?,
    songUrl: String?,
    onPillClick: () -> Unit,
    onDismiss: () -> Unit,
    onProgressUpdate: (Float) -> Unit,
    isQueueActive: Boolean,
    onQueueToggle: () -> Unit,
    modifier: Modifier = Modifier,
    playlist: SnapshotStateList<QueueSong> = remember { mutableStateListOf() },
    viewModel: PlayerViewModel = viewModel()
) {
    val config = LocalConfiguration.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()

    // Real-time states injected from Media3 backend
    val isPlaying by viewModel.isPlaying.collectAsState()
    val currentMediaItem by viewModel.currentMediaItem.collectAsState()

    // Extracting dynamic metadata
    val songTitle = currentMediaItem?.mediaMetadata?.title?.toString() ?: stringResource(R.string.default_song_title)
    val extractedArtist = currentMediaItem?.mediaMetadata?.artist?.toString() ?: stringResource(R.string.default_artist_name)
    val artistNames = remember(extractedArtist) { extractedArtist.split(",").map { it.trim() } }

    val fallbackArtist = stringResource(R.string.default_artist_name)
    val cleanArtistName = remember(artistNames) {
        val filtered = artistNames.map { it.trim() }.filter { it.isNotBlank() }
        if (filtered.isNotEmpty()) filtered.joinToString(", ") else fallbackArtist
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val fullHeight = maxHeight
        val screenWidth = maxWidth

        // UI state controllers
        var isLyricsActive by rememberSaveable { mutableStateOf(false) }
        var currentProgress by rememberSaveable { mutableFloatStateOf(0f) }
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
        val navInsets = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        val isGestureMode = navInsets <= 24.dp

        // Calculate margin to stay above DockedNavBar
        val navBarBottom = if (isGestureMode) 20.dp else navInsets + 8.dp
        val bottomMargin = if (isLandscape) 20.dp else navBarBottom + 68.dp + 5.dp

        val maxOffset = with(density) { (fullHeight - 64.dp - bottomMargin).toPx() }

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
        val currentCornerShape = RoundedCornerShape(currentCorner)
        val currentHeight = lerp(64.dp, fullHeight, progress)

        // Dynamically references the design system theme colors with progress interpolation
        val currentSurfaceColor = lerpColor(
            MaterialTheme.colorScheme.surface.copy(alpha = 0.80f),
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
            state = rememberDraggableState { },
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
            modifier = Modifier
                .fillMaxSize()
                .alpha(containerAlpha.value),
            contentAlignment = Alignment.TopCenter
        ) {
            // Core interactive surface
            Surface(
                modifier = Modifier
                    .offset { IntOffset(0, offsetY.value.roundToInt()) }
                    .fillMaxWidth(currentWidthFraction)
                    .height(currentHeight)
                    .border(
                        width = lerp(0.5.dp, 0.dp, progress),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = lerp(0.1f, 0f, progress)),
                        shape = currentCornerShape
                    )
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
                                if (isExpanded && offsetY.value <= 10f && velocity < -500) {
                                    onQueueToggle()
                                }
                                else if (velocity > 600 && offsetY.value >= maxOffset) {
                                    containerAlpha.animateTo(0f, tween(200))
                                    onDismiss()
                                }
                                else {
                                    val target = if (velocity < -400 || (isExpanded.not() && offsetY.value < maxOffset * 0.75f)) 0f else maxOffset
                                    offsetY.animateTo(target, spring(0.85f, 400f))
                                    if ((target == 0f && !isExpanded) || (target == maxOffset && isExpanded)) onPillClick()
                                }
                            }
                        }
                    ),
                color = currentSurfaceColor,
                shape = currentCornerShape,
                shadowElevation = 0.dp,
                tonalElevation = 0.dp,
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

                    // Layout layering
                    Box(modifier = Modifier.fillMaxSize()) {
                        // Song info and actions layer
                        AnimatedVisibility(
                            visible = !isLyricsActive || progress < 0.8f,
                            enter = fadeIn(tween(400)),
                            exit = fadeOut(tween(400))
                        ) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                // Dynamic positioning based on navigation bar type
                                val currentNavInsets = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
                                val isGesture = currentNavInsets <= 24.dp
                                val bottomReserved = if (isGesture) 20.dp + 56.dp else currentNavInsets + 8.dp + 56.dp
                                val portraitTextOffsetY = fullHeight - bottomReserved - 255.dp
                                val textOffsetX = if (isLandscape) lerp(76.dp, 370.dp, progress) else lerp(76.dp, 30.dp, progress)
                                val textOffsetY = if (isLandscape) lerp(10.dp, 75.dp, progress) else lerp(10.dp, portraitTextOffsetY, progress)
                                val sideActionsWidth = 110.dp

                                val infoWidth = if (isLandscape) {
                                    val miniLandscapeButtonStartX = (screenWidth * 0.55f) - 56.dp
                                    val miniInfoWidth = (miniLandscapeButtonStartX - textOffsetX - 12.dp).coerceAtLeast(0.dp)
                                    val expandedMargin = sideActionsWidth + 80.dp
                                    val expandedInfoWidth = screenWidth - textOffsetX - expandedMargin

                                    lerp(miniInfoWidth, expandedInfoWidth, progress)
                                } else {
                                    val miniPlayerButtonStartX = (screenWidth * 0.92f) - 56.dp
                                    val miniInfoWidth = (miniPlayerButtonStartX - textOffsetX - 12.dp).coerceAtLeast(0.dp)
                                    val expandedMargin = sideActionsWidth + 45.dp
                                    val expandedInfoWidth = screenWidth - textOffsetX - expandedMargin

                                    lerp(miniInfoWidth, expandedInfoWidth, progress)
                                }

                                Box(
                                    modifier = Modifier
                                        .offset(textOffsetX, textOffsetY)
                                        .width(infoWidth)
                                        .clipToBounds()
                                ) {
                                    SongInfo(
                                        title = songTitle,
                                        artist = cleanArtistName,
                                        progress = progress,
                                        isLandscape = isLandscape,
                                        screenWidth = screenWidth
                                    )
                                }

                                if (progress > 0.7f) {
                                    // Side actions (Favorite, Share)
                                    val portraitSideActionsY = portraitTextOffsetY + 12.dp
                                    SongSideActions(
                                        songUrl = songUrl,
                                        isFavorite = isFavorite,
                                        onFavoriteClick = { isFavorite = !isFavorite },
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .offset(
                                                x = if (isLandscape) (-60).dp else (-30).dp,
                                                y = if (isLandscape) 85.dp else portraitSideActionsY
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
                            val lyricsModifier = if (isLandscape) {
                                Modifier
                                    .fillMaxSize()
                                    .padding(bottom = 100.dp)
                            } else {
                                Modifier
                                    .fillMaxWidth()
                                    .fillMaxHeight()
                                    .padding(top = 40.dp, bottom = 320.dp)
                            }

                            Box(
                                modifier = lyricsModifier
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
                                            viewModel.seekTo(timestamp)
                                        },
                                        alignment = LyricsAlignment.CENTER,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }

                                // Song and Artist Header
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 40.dp)
                                        .padding(top = if (isLandscape) 20.dp else 0.dp)
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
                                        maxLines = 1,
                                        modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE)
                                    )
                                    Text(
                                        text = cleanArtistName,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontFamily = Poppins,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 12.sp
                                        ),
                                        color = Color.White.copy(alpha = 0.7f),
                                        maxLines = 1,
                                        modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE)
                                    )
                                }
                            }
                        }
                    }

                    // Lyrics trigger area
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

                    // Bottom queue trigger area
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
                            onProgressChange = {
                                currentProgress = it
                                viewModel.seekTo((it * 210000L).toLong())
                            },
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
                            screenHeight = fullHeight,
                            modifier = Modifier.alpha(((progress - 0.4f) * 2f).coerceIn(0f, 1f))
                        )
                    }

                    // Global playback controls
                    PlayerControls(
                        progress = progress,
                        isPlaying = isPlaying,
                        onPlayPauseToggle = { viewModel.togglePlayPause() },
                        onNext = { },
                        onPrevious = { },
                        screenWidth = screenWidth,
                        screenHeight = fullHeight,
                        isLandscape = isLandscape,
                        isLyricsActive = isLyricsActive
                    )

                    // Playback Queue overlay with entrance/exit animation
                    AnimatedVisibility(
                        visible = isQueueActive && progress >= 0.8f,
                        enter = slideInVertically(
                            initialOffsetY = { height -> height },
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioLowBouncy,
                                stiffness = Spring.StiffnessLow
                            )
                        ) + fadeIn(),
                        exit = slideOutVertically(
                            targetOffsetY = { height -> height },
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
                            artistNames = artistNames,
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
}

package com.wavvy.app.features.player.ui

// Android back handling
import androidx.activity.compose.BackHandler
// Compose animation
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
// Compose layouts and foundations
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
// Material 3 components
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.surfaceColorAtElevation
// Compose state and runtime
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
// UI styling and utilities
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp as lerpColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.util.lerp
// Coroutines
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
// Math
import kotlin.math.abs
import kotlin.math.roundToInt
// Project resources
import com.wavvy.app.core.designsystem.theme.WavvyTheme
import com.wavvy.app.core.lyrics.LyricsRepository
import com.wavvy.app.core.lyrics.LyricsSettings
import com.wavvy.app.core.lyrics.LyricsSettingsStore
import com.wavvy.app.core.lyrics.LyricsTranslation
import com.wavvy.app.core.playback.PlayableTrack
import com.wavvy.app.core.navigation.ItemNavigator
import com.wavvy.app.core.playback.PlayerConnection
import com.wavvy.app.core.playback.RadioQueue
import com.wavvy.app.features.player.ui.components.AlbumCover
import com.wavvy.app.features.player.ui.components.ExpandedPlayerContent
import com.wavvy.app.features.player.ui.components.LyricsSearchSheet
import com.wavvy.app.features.player.ui.components.LyricsSettingsSheet
import com.wavvy.app.features.player.ui.components.LyricsState
import com.wavvy.app.features.player.ui.components.PlaybackQueue
import com.wavvy.app.features.player.ui.components.PlayerControls
import com.wavvy.app.features.player.ui.components.PlayerDimens
import com.wavvy.app.features.player.ui.components.PlayerExpandedHeader
import com.wavvy.app.features.player.ui.components.PlayerLyricsOverlay

// Milliseconds in a second, for the length the lyrics are matched by
private const val MillisPerSecond = 1000L

// Room the screens leave at their end so the pill does not cover their last items
val LocalMiniPlayerInset = compositionLocalOf { PlayerDimens.None }

// Direction a drag on the player took when it started
private enum class DragAxis { Horizontal, Vertical }

// Player of the old Wavvy, a pill above the navigation bar that grows into the full player and back
@Composable
fun PlayerSheet(
    bottomPadding: Dp,
    isLandscape: Boolean,
    modifier: Modifier = Modifier
) {
    val track by PlayerConnection.track.collectAsState()

    // A new pill rises each time the player opens, and leaves when it is emptied
    track?.let { current ->
        PlayerSheetContent(
            track = current,
            bottomPadding = bottomPadding,
            isLandscape = isLandscape,
            modifier = modifier
        )
    }
}

// The sheet itself, its place set by how far it is open, from the pill at the bottom to the whole screen
// How far it is open is read only while measuring and drawing, so dragging and the springs never build the player again
@Composable
private fun PlayerSheetContent(
    track: PlayableTrack,
    bottomPadding: Dp,
    isLandscape: Boolean,
    modifier: Modifier = Modifier
) {
    val isPlaying by PlayerConnection.isPlaying.collectAsState()
    val isLoading by PlayerConnection.isLoading.collectAsState()
    val repeatMode by PlayerConnection.repeat.collectAsState()
    val isShuffleActive by PlayerConnection.shuffle.collectAsState()
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val sheetColor = WavvyTheme.colors.miniPlayer
    val openColor = MaterialTheme.colorScheme.surfaceColorAtElevation(PlayerDimens.ExpandedElevation)
    val borderBase = MaterialTheme.colorScheme.onSurface

    var isExpanded by rememberSaveable { mutableStateOf(false) }
    var isFavorite by rememberSaveable(track.id) { mutableStateOf(false) }
    var isLyricsActive by rememberSaveable { mutableStateOf(false) }
    var showLyricsOptions by rememberSaveable { mutableStateOf(false) }
    var showLyricsSearch by rememberSaveable { mutableStateOf(false) }
    var isQueueActive by rememberSaveable { mutableStateOf(false) }
    var lyricsSearch by remember { mutableIntStateOf(0) }
    val context = LocalContext.current

    // Options of the lyrics, saved on the device
    val lyricsStore = remember { LyricsSettingsStore(context) }
    val lyricsSettings by lyricsStore.settings.collectAsState(initial = LyricsSettings())
    val activeSources = lyricsSettings.activeSources

    // A new search starts for each song, each change of the sources and each search asked again
    var lyricsState by remember(track.id, activeSources, lyricsSearch) { mutableStateOf<LyricsState>(LyricsState.Loading) }
    var translation by remember(track.id) { mutableStateOf<LyricsTranslation?>(null) }

    // Lyrics are searched the first time they open for a song
    LaunchedEffect(track.id, isLyricsActive, activeSources, lyricsSearch) {
        if (!isLyricsActive || lyricsState !is LyricsState.Loading) return@LaunchedEffect
        val durationSeconds = (PlayerConnection.durationMs / MillisPerSecond).toInt()
        val lyrics = LyricsRepository.lyrics(context, track.id, track.title, track.artist.orEmpty(), durationSeconds, activeSources)
        lyricsState = if (lyrics == null) LyricsState.NotFound else LyricsState.Ready(lyrics, translation = null)
    }

    // The translation follows the switch and the chosen language
    val foundLyrics = (lyricsState as? LyricsState.Ready)?.lyrics
    LaunchedEffect(foundLyrics, lyricsSettings.translate, lyricsSettings.targetLanguage) {
        translation = if (foundLyrics != null && lyricsSettings.translate) {
            LyricsRepository.translation(track.id, foundLyrics, lyricsSettings.targetLanguage)
        } else {
            null
        }
    }
    val shownLyrics = (lyricsState as? LyricsState.Ready)?.copy(translation = translation) ?: lyricsState

    // Position and length of the song, read from the player only while it plays
    var playbackProgress by remember { mutableFloatStateOf(PlayerConnection.progress) }
    var durationMs by remember { mutableLongStateOf(PlayerConnection.durationMs) }
    LaunchedEffect(isPlaying, track.id) {
        do {
            playbackProgress = PlayerConnection.progress
            durationMs = PlayerConnection.durationMs
            if (isPlaying) {
                delay(
                    when {
                        isLyricsActive -> PlayerDimens.LyricsRefreshMillis
                        isExpanded -> PlayerDimens.ProgressRefreshMillis
                        else -> PlayerDimens.RingRefreshMillis
                    }
                )
            }
        } while (isPlaying)
    }

    // The back button closes the queue, then the lyrics, then the open player, before anything else
    BackHandler(enabled = isExpanded) {
        when {
            isQueueActive -> isQueueActive = false
            isLyricsActive -> isLyricsActive = false
            else -> isExpanded = false
        }
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val fullHeight = maxHeight
        val screenWidth = maxWidth
        val maxOffset = with(density) { (fullHeight - PlayerDimens.MiniHeight - bottomPadding).toPx() }
        val flingVelocity = with(density) { PlayerDimens.FlingVelocity.toPx() }

        val containerAlpha = remember { Animatable(0f) }
        val offsetY = remember { Animatable(maxOffset + PlayerDimens.EntranceOffsetPx) }
        val offsetX = remember { Animatable(0f) }
        var dismissDrag by remember { mutableFloatStateOf(0f) }
        var hasEntered by remember { mutableStateOf(false) }

        // Share of the opening, zero as the pill and one as the whole screen, read while measuring and drawing
        val progress: () -> Float = remember(maxOffset) { { (1f - offsetY.value / maxOffset).coerceIn(0f, 1f) } }
        val isCollapsed by remember(progress) { derivedStateOf { progress() < PlayerDimens.CollapsedThreshold } }
        val showContent by remember(progress) { derivedStateOf { progress() > PlayerDimens.ContentStart } }
        val inLyricsZone by remember(progress) { derivedStateOf { progress() >= PlayerDimens.LyricsStart } }
        val isFullyOpen by remember(progress) { derivedStateOf { progress() > PlayerDimens.LyricsFullStart } }

        // Queue over the open player, hidden one screen below while closed
        val maxQueueOffset = with(density) { fullHeight.toPx() }
        val queueOffsetY = remember { Animatable(maxQueueOffset) }
        var isDraggingQueue by remember { mutableStateOf(false) }
        val inQueueZone by remember(progress) { derivedStateOf { progress() >= PlayerDimens.QueueShowStart } }
        val isQueueMoved by remember(maxQueueOffset) { derivedStateOf { queueOffsetY.value < maxQueueOffset } }

        // Opens and closes the queue when asked by the toolbar, the close button or the back button
        LaunchedEffect(isQueueActive, maxQueueOffset) {
            if (!isDraggingQueue) {
                queueOffsetY.animateTo(
                    if (isQueueActive) 0f else maxQueueOffset,
                    spring(Spring.DampingRatioNoBouncy, Spring.StiffnessMediumLow)
                )
            }
        }

        // With the lyrics open the cover and the song fade out and the player darkens
        val lyricsShown = isLyricsActive && inLyricsZone
        val coverAlpha by animateFloatAsState(if (lyricsShown) 0f else 1f, tween(PlayerDimens.LyricsCoverFadeMillis), label = "CoverAlpha")
        val dimAlpha by animateFloatAsState(
            if (isLyricsActive && isFullyOpen) PlayerDimens.LyricsDimAlpha else 0f,
            tween(PlayerDimens.LyricsDimMillis),
            label = "LyricsDim"
        )
        val controlsAlpha by animateFloatAsState(
            if (isLandscape && lyricsShown) 0f else 1f,
            tween(PlayerDimens.LyricsCoverFadeMillis),
            label = "ControlsAlpha"
        )

        // Rises and fades in the first time, then follows the bottom when the bars change
        LaunchedEffect(maxOffset) {
            if (hasEntered) {
                offsetY.snapTo(if (isExpanded) 0f else maxOffset)
            } else {
                launch { containerAlpha.animateTo(1f, tween(PlayerDimens.EntranceFadeMillis)) }
                offsetY.animateTo(if (isExpanded) 0f else maxOffset, spring(PlayerDimens.EntranceDamping, PlayerDimens.EntranceStiffness))
                hasEntered = true
            }
        }

        // Opens and closes when asked by a tap, the arrow or the back button
        LaunchedEffect(isExpanded) {
            if (!isExpanded) {
                isLyricsActive = false
                isQueueActive = false
            }
            // A tap while the pill is still rising ends its entrance and opens the player, instead of being lost
            if (hasEntered || isExpanded) {
                hasEntered = true
                launch { containerAlpha.animateTo(1f, tween(PlayerDimens.EntranceFadeMillis)) }
                offsetY.animateTo(if (isExpanded) 0f else maxOffset, spring(PlayerDimens.ExpandDamping, PlayerDimens.ExpandStiffness))
            }
        }

        val baseFraction = if (isLandscape) PlayerDimens.MiniWidthFractionLandscape else PlayerDimens.MiniWidthFraction

        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = containerAlpha.value }
        ) {
            Box(
                modifier = Modifier
                    // Grows from the pill to the whole screen and follows the drag
                    .layout { measurable, constraints ->
                        val opening = progress()
                        val width = (constraints.maxWidth * (baseFraction + opening * (1f - baseFraction))).roundToInt()
                        val height = lerp(PlayerDimens.MiniHeight.toPx(), constraints.maxHeight.toFloat(), opening).roundToInt()
                        val placeable = measurable.measure(Constraints.fixed(width, height))
                        layout(constraints.maxWidth, constraints.maxHeight) {
                            placeable.place((constraints.maxWidth - width) / 2 + offsetX.value.roundToInt(), offsetY.value.roundToInt())
                        }
                    }
                    .graphicsLayer {
                        shape = RoundedCornerShape(lerp(PlayerDimens.MiniCorner, PlayerDimens.None, progress()))
                        clip = true
                    }
                    .drawWithContent {
                        val opening = progress()
                        drawRect(lerpColor(sheetColor, openColor, opening))
                        drawContent()

                        // Thin border of the pill, fading away as it opens
                        val borderWidth = (if (opening < PlayerDimens.CollapsedThreshold) PlayerDimens.MiniBorder else lerp(PlayerDimens.OpeningBorder, PlayerDimens.None, opening)).toPx()
                        if (borderWidth > 0f) {
                            val corner = lerp(PlayerDimens.MiniCorner, PlayerDimens.None, opening).toPx()
                            drawRoundRect(
                                color = borderBase.copy(alpha = PlayerDimens.MiniBorderAlpha * (1f - opening)),
                                topLeft = Offset(borderWidth / 2, borderWidth / 2),
                                size = Size(size.width - borderWidth, size.height - borderWidth),
                                cornerRadius = CornerRadius((corner - borderWidth / 2).coerceAtLeast(0f)),
                                style = Stroke(borderWidth)
                            )
                        }
                    }
                    .pointerInput(maxOffset) {
                        var axis: DragAxis? = null
                        var startOffset = 0f
                        var travelled = 0f
                        val velocity = VelocityTracker()

                        detectDragGestures(
                            onDragStart = {
                                axis = null
                                startOffset = offsetY.value
                                travelled = 0f
                                velocity.resetTracking()
                            },
                            onDragEnd = {
                                // The finger moves with the sheet, so its speed comes from the whole way it travelled
                                val speed = velocity.calculateVelocity().y
                                scope.launch {
                                    when {
                                        // Right goes back, left goes on, a short drag springs back
                                        axis == DragAxis.Horizontal -> {
                                            val threshold = size.width * PlayerDimens.SwipeFraction
                                            when {
                                                offsetX.value > threshold -> PlayerConnection.skipToPrevious()
                                                offsetX.value < -threshold -> PlayerConnection.skipToNext()
                                            }
                                            offsetX.animateTo(0f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMediumLow))
                                        }

                                        // Pulled far or fast enough below the pill, the player closes and the song stops
                                        dismissDrag > 0f -> {
                                            if (dismissDrag > PlayerDimens.DismissDistancePx || speed > PlayerDimens.DismissVelocity) {
                                                launch { containerAlpha.animateTo(0f, tween(PlayerDimens.DismissFadeMillis)) }
                                                offsetY.animateTo(maxOffset + PlayerDimens.EntranceOffsetPx, tween(PlayerDimens.DismissFadeMillis))
                                                PlayerConnection.stop()
                                            } else {
                                                dismissDrag = 0f
                                                containerAlpha.animateTo(1f, spring())
                                            }
                                        }

                                        // A fling follows its direction, a slow release goes on once a quarter of the way is dragged
                                        else -> {
                                            val wasOpen = startOffset < maxOffset / 2
                                            val moved = abs(offsetY.value - startOffset) / maxOffset
                                            val open = when {
                                                speed < -flingVelocity -> true
                                                speed > flingVelocity -> false
                                                moved > PlayerDimens.PositionalThreshold -> !wasOpen
                                                else -> wasOpen
                                            }
                                            offsetY.animateTo(if (open) 0f else maxOffset, spring(PlayerDimens.ExpandDamping, PlayerDimens.ExpandStiffness))
                                            isExpanded = open
                                        }
                                    }
                                }
                            },
                            onDragCancel = {
                                scope.launch {
                                    dismissDrag = 0f
                                    launch { offsetX.animateTo(0f, spring()) }
                                    launch { offsetY.animateTo(if (isExpanded) 0f else maxOffset, spring(PlayerDimens.ExpandDamping, PlayerDimens.ExpandStiffness)) }
                                    containerAlpha.animateTo(1f, spring())
                                }
                            }
                        ) { change, drag ->
                            change.consume()
                            travelled += drag.y
                            velocity.addPosition(change.uptimeMillis, Offset(0f, travelled))
                            if (axis == null) {
                                val sideways = abs(drag.x) > abs(drag.y) && offsetY.value >= maxOffset
                                axis = if (sideways) DragAxis.Horizontal else DragAxis.Vertical
                            }

                            scope.launch {
                                if (axis == DragAxis.Horizontal) {
                                    offsetX.snapTo(offsetX.value + drag.x)
                                } else if (offsetY.value >= maxOffset && (drag.y > 0f || dismissDrag > 0f)) {
                                    // Below the pill the drag fades it out instead of moving it
                                    dismissDrag = (dismissDrag + drag.y).coerceAtLeast(0f)
                                    containerAlpha.snapTo(1f - (dismissDrag / PlayerDimens.DismissDistancePx).coerceIn(0f, 1f))
                                } else {
                                    offsetY.snapTo((offsetY.value + drag.y).coerceIn(0f, maxOffset))
                                }
                            }
                        }
                    }
                    // A tap on the pill opens the player
                    .clickable(enabled = isCollapsed) { if (offsetX.value == 0f) isExpanded = true }
            ) {
                AlbumCover(
                    progress = progress,
                    coverAlpha = { coverAlpha },
                    imageUrl = track.artworkUrl,
                    songProgress = { playbackProgress },
                    screenWidth = screenWidth,
                    screenHeight = fullHeight,
                    isLandscape = isLandscape
                )

                // Dark over the player while the lyrics are open, a tap on it closes them
                if (dimAlpha > 0f) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer { alpha = dimAlpha }
                            .background(WavvyTheme.colors.tileScrim)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                enabled = isLyricsActive
                            ) { isLyricsActive = false }
                    )
                }

                // The pages of the song are asked as soon as it plays, so a tap on its names opens them at once
                LaunchedEffect(track.id) { RadioQueue.links(context, track.id) }

                if (!lyricsShown || coverAlpha > 0f) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer { alpha = coverAlpha }
                    ) {
                        PlayerExpandedHeader(
                            progress = progress,
                            isLandscape = isLandscape,
                            fullHeight = fullHeight,
                            screenWidth = screenWidth,
                            title = track.title,
                            artist = track.artist.orEmpty(),
                            songId = track.id,
                            isFavorite = isFavorite,
                            onFavoriteClick = { isFavorite = !isFavorite },
                            onTitleClick = {
                                RadioQueue.cachedLinks(track.id)?.albumId?.let { id ->
                                    if (ItemNavigator.openAlbum(id)) isExpanded = false
                                }
                            },
                            onArtistClick = {
                                RadioQueue.cachedLinks(track.id)?.artistIds?.firstOrNull()?.let { id ->
                                    if (ItemNavigator.openArtist(id)) isExpanded = false
                                }
                            }
                        )
                    }
                }

                PlayerLyricsOverlay(
                    visible = lyricsShown,
                    isLandscape = isLandscape,
                    state = shownLyrics,
                    settings = lyricsSettings,
                    positionMs = { (playbackProgress * durationMs).toLong() },
                    title = track.title,
                    artist = track.artist.orEmpty(),
                    onOptionsClick = { showLyricsOptions = true },
                    onSeek = { time ->
                        if (durationMs > 0) playbackProgress = time.toFloat() / durationMs
                        PlayerConnection.seekTo(playbackProgress)
                    },
                    onDismiss = { isLyricsActive = false }
                )

                // A tap on the open cover shows the lyrics
                if (isFullyOpen && !isLyricsActive) {
                    Box(
                        modifier = Modifier
                            .then(
                                if (isLandscape) {
                                    Modifier
                                        .offset(PlayerDimens.CoverLandscapeOffset, PlayerDimens.CoverLandscapeOffset)
                                        .size(PlayerDimens.ExpandedCoverLandscape)
                                } else {
                                    Modifier
                                        .fillMaxWidth()
                                        .fillMaxHeight(PlayerDimens.LyricsTapHeightFraction)
                                        .padding(top = PlayerDimens.LyricsTapTop)
                                }
                            )
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { isLyricsActive = true }
                    )
                }

                // Dragging up from the bottom of the open player brings the queue, a tap opens it
                if (isFullyOpen) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(if (isLandscape) PlayerDimens.QueueDragAreaLandscape else PlayerDimens.QueueDragArea)
                            .align(Alignment.BottomCenter)
                            .pointerInput(maxQueueOffset) {
                                detectDragGestures(
                                    onDragStart = { isDraggingQueue = true },
                                    onDragEnd = {
                                        isDraggingQueue = false
                                        val threshold = maxQueueOffset * PlayerDimens.QueueOpenFraction
                                        // Closed it opens after a short pull, open it closes past the half
                                        val open = if (isQueueActive) {
                                            queueOffsetY.value < maxQueueOffset / 2
                                        } else {
                                            queueOffsetY.value < maxQueueOffset - threshold
                                        }
                                        scope.launch {
                                            queueOffsetY.animateTo(
                                                if (open) 0f else maxQueueOffset,
                                                spring(Spring.DampingRatioLowBouncy, Spring.StiffnessMedium)
                                            )
                                        }
                                        if (open) isLyricsActive = false
                                        isQueueActive = open
                                    },
                                    onDragCancel = {
                                        isDraggingQueue = false
                                        scope.launch { queueOffsetY.animateTo(if (isQueueActive) 0f else maxQueueOffset, spring()) }
                                    }
                                ) { change, drag ->
                                    change.consume()
                                    scope.launch { queueOffsetY.snapTo((queueOffsetY.value + drag.y).coerceIn(0f, maxQueueOffset)) }
                                }
                            }
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                if (progress() >= PlayerDimens.QueueTapStart) {
                                    isLyricsActive = false
                                    isQueueActive = true
                                }
                            }
                    )
                }

                // Composed from the start, so the work of the first opening is already done when it is tapped, but it is only placed while it shows
                // The song position reaches it only while it shows, so a playing song does not rebuild what nobody sees
                run {
                    ExpandedPlayerContent(
                        onMinimize = { isExpanded = false },
                        progress = if (showContent) playbackProgress else 0f,
                        isActive = showContent,
                        durationMs = durationMs,
                        onSeek = { target ->
                            playbackProgress = target
                            PlayerConnection.seekTo(target)
                        },
                        isLyricsActive = isLyricsActive,
                        onLyricsToggle = {
                            isLyricsActive = !isLyricsActive
                            if (isLyricsActive) isQueueActive = false
                        },
                        onMoreClick = {},
                        isQueueActive = isQueueActive,
                        onQueueToggle = {
                            isQueueActive = !isQueueActive
                            if (isQueueActive) isLyricsActive = false
                        },
                        repeatMode = repeatMode,
                        onRepeatClick = PlayerConnection::toggleRepeat,
                        isShuffleActive = isShuffleActive,
                        onShuffleClick = PlayerConnection::toggleShuffle,
                        isLandscape = isLandscape,
                        screenHeight = fullHeight,
                        modifier = Modifier
                            .layout { measurable, constraints ->
                                val placeable = measurable.measure(constraints)
                                if (showContent) layout(placeable.width, placeable.height) { placeable.place(0, 0) } else layout(0, 0) {}
                            }
                            .graphicsLayer {
                                alpha = ((progress() - PlayerDimens.ContentStart) * PlayerDimens.ContentFadeSpeed).coerceIn(0f, 1f)
                            }
                    )
                }

                // Lying, the controls step aside for the lyrics
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { alpha = controlsAlpha }
                ) {
                    PlayerControls(
                        progress = progress,
                        isPlaying = isPlaying,
                        isLoading = isLoading,
                        onPlayPauseToggle = PlayerConnection::togglePlayPause,
                        onNext = PlayerConnection::skipToNext,
                        onPrevious = PlayerConnection::skipToPrevious,
                        screenWidth = screenWidth,
                        screenHeight = fullHeight,
                        isLandscape = isLandscape
                    )
                }

                // Queue over everything, drawn only while it is open or on its way
                if (inQueueZone && (isQueueActive || isQueueMoved)) {
                    PlaybackQueue(
                        isLandscape = isLandscape,
                        onClose = { isQueueActive = false },
                        modifier = Modifier.graphicsLayer {
                            translationY = queueOffsetY.value
                            alpha = (1f - queueOffsetY.value / maxQueueOffset).coerceIn(0f, 1f)
                        }
                    )
                }

                // Options of the lyrics over everything, every change is saved at once
                if (showLyricsOptions) {
                    LyricsSettingsSheet(
                        settings = lyricsSettings,
                        onChange = { changed -> scope.launch { lyricsStore.save(changed) } },
                        onSearchAgain = {
                            showLyricsOptions = false
                            // The search starts only after the saved lyrics are gone
                            scope.launch {
                                LyricsRepository.forget(context, track.id)
                                lyricsSearch++
                            }
                        },
                        onSearchManually = {
                            showLyricsOptions = false
                            showLyricsSearch = true
                        },
                        onDismiss = { showLyricsOptions = false }
                    )
                }

                // Manual search of the lyrics, the song and the artist can be fixed
                if (showLyricsSearch) {
                    LyricsSearchSheet(
                        initialTitle = track.title,
                        initialArtist = track.artist.orEmpty(),
                        onSearch = { title, artist ->
                            LyricsRepository.search(
                                context, track.id, title, artist, track.title, track.artist.orEmpty(),
                                (durationMs / MillisPerSecond).toInt(), activeSources
                            )
                        },
                        onChoose = { match ->
                            LyricsRepository.choose(context, track.id, match.lyrics)
                            lyricsState = LyricsState.Ready(match.lyrics, translation = null)
                            showLyricsSearch = false
                        },
                        onDismiss = { showLyricsSearch = false }
                    )
                }
            }
        }
    }
}

// Fade from the content into the background behind the pill, so the last items do not show through its sides
@Composable
fun MiniPlayerShade(
    visible: Boolean,
    isLandscape: Boolean,
    modifier: Modifier = Modifier
) {
    val height = if (isLandscape) {
        PlayerDimens.MiniBottomLandscape + PlayerDimens.ShadeAbovePillLandscape
    } else {
        PlayerDimens.MiniGap + PlayerDimens.ShadeAbovePill
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(PlayerDimens.ShadeFadeMillis)),
        exit = fadeOut(tween(PlayerDimens.ShadeFadeMillis)),
        modifier = modifier
            .fillMaxWidth()
            .height(height)
    ) {
        val background = MaterialTheme.colorScheme.background
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, background.copy(alpha = PlayerDimens.ShadeMiddleAlpha), background)
                    )
                )
        )
    }
}

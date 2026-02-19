package com.lonewolf.wavvy.ui.player.components

// Compose layouts and foundations
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
// Material 3 components
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
// UI styling and utilities
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
// Project resources
import com.lonewolf.wavvy.R
import com.lonewolf.wavvy.ui.common.SongOptionsBottomSheet
import com.lonewolf.wavvy.ui.theme.Poppins
import com.lonewolf.wavvy.ui.theme.WavvyTheme
import com.lonewolf.wavvy.ui.theme.accentCyan
import kotlinx.coroutines.launch
// Reorderable library
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

// Song data model
data class QueueSong(
    val id: Int,
    val title: String,
    val artist: String,
    val durationSeconds: Long = 0L
)

// Main queue container
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun PlaybackQueue(
    playlist: SnapshotStateList<QueueSong>,
    currentIndex: Int,
    isLocked: Boolean,
    onLockToggle: (Boolean) -> Unit,
    isPlaying: Boolean,
    onIndexChange: (Int) -> Unit,
    repeatMode: Int,
    onRepeatClick: () -> Unit,
    isShuffleActive: Boolean,
    onShuffleClick: () -> Unit,
    onClose: () -> Unit = {},
    dragModifier: Modifier = Modifier,
    modifier: Modifier = Modifier
) {
    val lazyListState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    // Menu state
    var showMenu by remember { mutableStateOf(false) }
    var selectedSong by remember { mutableStateOf<QueueSong?>(null) }

    // Pull to close gesture state
    val pullOffset = remember { Animatable(0f) }
    val maxPullThreshold = with(LocalDensity.current) { 100.dp.toPx() }
    val pullProgress = (pullOffset.value / maxPullThreshold).coerceIn(0f, 1f)

    // Total duration calculation
    val totalDurationSeconds = remember(playlist.toList()) {
        playlist.sumOf { it.durationSeconds }
    }

    // Reset animations
    LaunchedEffect(Unit) {
        pullOffset.snapTo(0f)
    }

    // Auto-reset offset
    LaunchedEffect(playlist.size) {
        if (playlist.size <= 1 && pullOffset.value != 0f) {
            pullOffset.animateTo(0f)
        }
    }

    // Swipe down to close connection
    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val isAtTop = lazyListState.firstVisibleItemIndex == 0 &&
                        lazyListState.firstVisibleItemScrollOffset == 0

                if (available.y > 0 && isAtTop) {
                    val newOffset = pullOffset.value + available.y * 0.5f
                    scope.launch { pullOffset.snapTo(newOffset) }
                    return Offset(0f, available.y)
                }
                if (available.y < 0 && pullOffset.value > 0f) {
                    scope.launch {
                        pullOffset.snapTo((pullOffset.value + available.y).coerceAtLeast(0f))
                    }
                    return Offset(0f, available.y)
                }
                return Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                if (pullOffset.value >= maxPullThreshold) {
                    onClose()
                } else {
                    pullOffset.animateTo(0f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow))
                }
                return super.onPreFling(available)
            }
        }
    }

    // List reorder logic
    val reorderableState = rememberReorderableLazyListState(
        lazyListState = lazyListState,
        onMove = { from, to ->
            if (!isLocked) {
                // Determine new current index before moving
                val newIdx = when {
                    from.index == currentIndex -> to.index
                    from.index < currentIndex && to.index >= currentIndex -> currentIndex - 1
                    from.index > currentIndex && to.index <= currentIndex -> currentIndex + 1
                    else -> currentIndex
                }

                playlist.apply { add(to.index, removeAt(from.index)) }
                onIndexChange(newIdx)
            }
        }
    )

    // Auto-scroll to playing item
    LaunchedEffect(currentIndex) {
        if (playlist.isNotEmpty() && currentIndex in playlist.indices) {
            lazyListState.animateScrollToItem(currentIndex)
        }
    }

    WavvyTheme(darkTheme = true) {
        Surface(
            modifier = modifier
                .fillMaxSize()
                .then(dragModifier)
                .nestedScroll(nestedScrollConnection)
                .offset(y = with(LocalDensity.current) { (pullOffset.value * 0.4f).toDp() }),
            color = MaterialTheme.colorScheme.background
        ) {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val screenWidth = constraints.maxWidth.toFloat()

                Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
                    // Header section
                    Box(modifier = Modifier.fillMaxWidth().then(dragModifier)) {
                        QueueHeaderWithProgress(
                            pullProgress = pullProgress,
                            onClose = onClose,
                            songCount = playlist.size,
                            totalDurationSeconds = totalDurationSeconds,
                            isLocked = isLocked,
                            onLockToggle = { onLockToggle(!isLocked) }
                        )
                    }

                    Box(modifier = Modifier.fillMaxSize()) {
                        if (playlist.isEmpty()) {
                            // Empty state view
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState())
                            ) {
                                EmptyQueuePlaceholder()
                            }
                        } else {
                            // Songs list
                            LazyColumn(
                                state = lazyListState,
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(bottom = 80.dp)
                            ) {
                                itemsIndexed(playlist, key = { _, song -> song.id }) { index, song ->

                                    val isNowPlaying = index == currentIndex

                                    ReorderableItem(reorderableState, key = song.id) { isDragging ->
                                        if (isNowPlaying || isLocked) {
                                            // Static items (playing or locked)
                                            Box(modifier = Modifier
                                                .padding(vertical = 4.dp)
                                                .scale(if (isDragging) 1.02f else 1f)
                                                .animateItem(placementSpec = tween(500))
                                            ) {
                                                QueueItem(
                                                    song = song,
                                                    isNowPlaying = isNowPlaying,
                                                    isHistory = index < currentIndex,
                                                    isPlaying = isPlaying,
                                                    isLocked = isLocked,
                                                    modifier = if (isLocked) Modifier else Modifier.draggableHandle(),
                                                    onClick = { onIndexChange(index) },
                                                    onMoreClick = {
                                                        selectedSong = song
                                                        showMenu = true
                                                    }
                                                )
                                            }
                                        } else {
                                            // Swipe actions logic
                                            val currentPlaylistState by rememberUpdatedState(playlist)
                                            val currentIndexState by rememberUpdatedState(currentIndex)

                                            // Using key(song.id) here is essential to reset the state when the item moves
                                            val dismissState = key(song.id) {
                                                rememberSwipeToDismissBoxState(
                                                    confirmValueChange = { value ->
                                                        // Find current index of this song by ID to avoid stale index issues
                                                        val actualIndex = currentPlaylistState.indexOfFirst { it.id == song.id }
                                                        if (actualIndex == -1) return@rememberSwipeToDismissBoxState false

                                                        when (value) {
                                                            SwipeToDismissBoxValue.StartToEnd -> {
                                                                // Remove item
                                                                currentPlaylistState.removeAt(actualIndex)
                                                                if (actualIndex < currentIndexState) {
                                                                    onIndexChange(currentIndexState - 1)
                                                                }
                                                                true
                                                            }
                                                            SwipeToDismissBoxValue.EndToStart -> {
                                                                // Move to play next
                                                                if (currentPlaylistState.size > 1) {
                                                                    val item = currentPlaylistState.removeAt(actualIndex)
                                                                    val targetPos = if (actualIndex < currentIndexState) currentIndexState else currentIndexState + 1
                                                                    currentPlaylistState.add(targetPos.coerceIn(0, currentPlaylistState.size), item)

                                                                    if (actualIndex < currentIndexState) {
                                                                        onIndexChange(currentIndexState - 1)
                                                                    }
                                                                }
                                                                false // Snap back
                                                            }
                                                            else -> false
                                                        }
                                                    },
                                                    positionalThreshold = { it * 0.5f }
                                                )
                                            }

                                            SwipeToDismissBox(
                                                state = dismissState,
                                                backgroundContent = { RevealBackground(dismissState) },
                                                modifier = Modifier.padding(vertical = 4.dp),
                                                enableDismissFromStartToEnd = true,
                                                enableDismissFromEndToStart = true
                                            ) {
                                                Box(modifier = Modifier
                                                    .graphicsLayer {
                                                        val currentOffset = try { dismissState.requireOffset() } catch (e: Exception) { 0f }
                                                        val limit = if (dismissState.targetValue == SwipeToDismissBoxValue.EndToStart) screenWidth * 0.4f else screenWidth
                                                        val clamped = currentOffset.coerceIn(-limit, limit)
                                                        translationX = clamped - currentOffset
                                                    }
                                                    .scale(if (isDragging) 1.02f else 1f)
                                                    .animateItem(placementSpec = tween(500))
                                                ) {
                                                    QueueItem(
                                                        song = song,
                                                        isNowPlaying = isNowPlaying,
                                                        isHistory = index < currentIndex,
                                                        isPlaying = isPlaying,
                                                        isLocked = isLocked,
                                                        modifier = Modifier.draggableHandle(),
                                                        onClick = { onIndexChange(index) },
                                                        onMoreClick = {
                                                            selectedSong = song
                                                            showMenu = true
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Bottom action bar
                        QueueActionPill(
                            repeatMode = repeatMode,
                            onRepeatClick = onRepeatClick,
                            isShuffleActive = isShuffleActive,
                            onShuffleClick = onShuffleClick,
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                        )
                    }
                }
            }
        }

        // More options sheet
        if (showMenu && selectedSong != null) {
            SongOptionsBottomSheet(
                songTitle = selectedSong!!.title,
                artistName = selectedSong!!.artist,
                isSimplified = false,
                onDismiss = { showMenu = false },
                onActionClick = { action ->
                    // Action handling logic
                    showMenu = false
                }
            )
        }
    }
}

// Action bar for bottom controls
@Composable
private fun QueueActionPill(
    repeatMode: Int,
    onRepeatClick: () -> Unit,
    isShuffleActive: Boolean,
    onShuffleClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val inactive = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
    val active = MaterialTheme.accentCyan
    val backgroundColor = MaterialTheme.colorScheme.background

    Box(
        modifier = modifier
            .background(backgroundColor)
            .navigationBarsPadding()
            .height(72.dp)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                ShuffleButton(isShuffleActive, onShuffleClick, inactive, active)
                RepeatButton(repeatMode, onRepeatClick, inactive, active)
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Search icon
                AnimatedIconButton(onClick = { /* Search logic */ }) { mod ->
                    Icon(Icons.Default.Search, stringResource(R.string.search_hint), tint = inactive, modifier = mod.size(22.dp))
                }

                // Selection icon
                AnimatedIconButton(onClick = { /* Select/Unselect logic */ }) { mod ->
                    Icon(Icons.Default.Checklist, null, tint = inactive, modifier = mod.size(22.dp))
                }

                // Save playlist icon
                AnimatedIconButton(onClick = { /* Save logic */ }) { mod ->
                    Icon(Icons.Default.PlaylistAdd, stringResource(R.string.queue_menu_add_playlist), tint = inactive, modifier = mod.size(24.dp))
                }

                // Export icon using the local drawable XML
                AnimatedIconButton(onClick = { /* Export logic */ }) { mod ->
                    Icon(
                        painter = painterResource(id = R.drawable.ic_output),
                        contentDescription = stringResource(R.string.queue_menu_share),
                        tint = inactive,
                        modifier = mod.size(22.dp)
                    )
                }
            }
        }
    }
}

// Button with tactile scale feedback
@Composable
private fun AnimatedIconButton(
    onClick: () -> Unit,
    content: @Composable (Modifier) -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.85f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "IconScale"
    )

    Box(
        modifier = Modifier
            .size(44.dp)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        content(Modifier.graphicsLayer {
            scaleX = scale
            scaleY = scale
        })
    }
}

// Repeat modes button
@Composable
private fun RepeatButton(
    repeatMode: Int,
    onClick: () -> Unit,
    inactive: Color,
    active: Color
) {
    val rotation = remember { Animatable(0f) }
    var lastMode by remember { mutableIntStateOf(repeatMode) }

    LaunchedEffect(repeatMode) {
        if (repeatMode != lastMode) {
            rotation.animateTo(rotation.value + 360f, spring(0.6f))
            lastMode = repeatMode
        }
    }

    AnimatedIconButton(onClick) { mod ->
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.Default.Repeat,
                contentDescription = null,
                tint = if (repeatMode > 0) active else inactive,
                modifier = mod.size(22.dp).graphicsLayer { rotationZ = rotation.value }
            )
            if (repeatMode == 2) {
                Text(
                    text = "1",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                    color = active
                )
            }
        }
    }
}

// Shuffle toggle button
@Composable
private fun ShuffleButton(
    isActive: Boolean,
    onClick: () -> Unit,
    inactive: Color,
    active: Color
) {
    AnimatedIconButton(onClick) { mod ->
        Icon(
            imageVector = Icons.Default.Shuffle,
            contentDescription = null,
            tint = if (isActive) active else inactive,
            modifier = mod.size(22.dp)
        )
    }
}

// Empty state placeholder
@Composable
private fun EmptyQueuePlaceholder() {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.QueueMusic,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.empty_queue_title),
            style = MaterialTheme.typography.titleMedium.copy(fontFamily = Poppins, fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = stringResource(R.string.empty_queue_subtitle),
            style = MaterialTheme.typography.bodySmall.copy(fontFamily = Poppins),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

// Swipe actions background
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RevealBackground(state: SwipeToDismissBoxState) {
    val color = when (state.dismissDirection) {
        SwipeToDismissBoxValue.StartToEnd -> MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
        SwipeToDismissBoxValue.EndToStart -> MaterialTheme.accentCyan.copy(alpha = 0.8f)
        else -> Color.Transparent
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(color),
        contentAlignment = if (state.dismissDirection == SwipeToDismissBoxValue.StartToEnd) Alignment.CenterStart else Alignment.CenterEnd
    ) {
        when (state.dismissDirection) {
            SwipeToDismissBoxValue.StartToEnd -> {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = stringResource(R.string.queue_menu_remove_item),
                    modifier = Modifier.padding(start = 20.dp),
                    tint = MaterialTheme.colorScheme.onError
                )
            }
            SwipeToDismissBoxValue.EndToStart -> {
                Icon(
                    imageVector = Icons.Default.PlaylistPlay,
                    contentDescription = stringResource(R.string.queue_menu_play_next_item),
                    modifier = Modifier.padding(end = 20.dp),
                    tint = Color.Black
                )
            }
            else -> {}
        }
    }
}

// Song item component
@Composable
private fun QueueItem(
    song: QueueSong,
    isNowPlaying: Boolean,
    isHistory: Boolean,
    isPlaying: Boolean,
    isLocked: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onMoreClick: () -> Unit
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isNowPlaying) MaterialTheme.accentCyan.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surface,
        label = "bgColor"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp).alpha(if (isHistory) 0.75f else 1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Reorder handle or visual indicator
            Box(
                modifier = Modifier.size(32.dp),
                contentAlignment = Alignment.Center
            ) {
                if (isNowPlaying) {
                    EqualizerBars(isPlaying = isPlaying)
                } else {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = stringResource(R.string.reorder_handle),
                        tint = if (isLocked) Color.Transparent else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        modifier = modifier.size(20.dp)
                    )
                }
            }

            // Cover placeholder
            Box(
                modifier = Modifier
                    .padding(start = 8.dp)
                    .size(50.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer)
            )

            // Info section
            Column(modifier = Modifier.weight(1f).padding(horizontal = 16.dp)) {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.bodyMedium.copy(fontFamily = Poppins, fontWeight = FontWeight.SemiBold),
                    color = if (isNowPlaying) MaterialTheme.accentCyan else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
                Text(
                    text = song.artist,
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = Poppins),
                    color = if (isNowPlaying) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    maxLines = 1
                )
            }

            // More options
            IconButton(onClick = onMoreClick, modifier = Modifier.size(24.dp)) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = stringResource(R.string.track_options),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

// Visual audio indicator
@Composable
private fun EqualizerBars(isPlaying: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "equalizer")
    val heights = listOf(
        infiniteTransition.animateFloat(0.3f, 1f, infiniteRepeatable(tween(400), RepeatMode.Reverse), "b1"),
        infiniteTransition.animateFloat(0.5f, 1f, infiniteRepeatable(tween(600), RepeatMode.Reverse), "b2"),
        infiniteTransition.animateFloat(0.2f, 1f, infiniteRepeatable(tween(500), RepeatMode.Reverse), "b3")
    )
    Row(
        modifier = Modifier.size(20.dp, 14.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        heights.forEach { anim ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(if (isPlaying) anim.value else 0.4f)
                    .background(MaterialTheme.accentCyan, RoundedCornerShape(1.dp))
            )
        }
    }
}

// Header with metadata and controls
@Composable
private fun QueueHeaderWithProgress(
    pullProgress: Float,
    songCount: Int,
    totalDurationSeconds: Long,
    isLocked: Boolean,
    onLockToggle: () -> Unit,
    onClose: () -> Unit
) {
    // Time formatting logic
    val timeLabel = remember(totalDurationSeconds) {
        val hours = totalDurationSeconds / 3600
        val minutes = (totalDurationSeconds % 3600) / 60
        val seconds = totalDurationSeconds % 60

        when {
            hours > 0 -> "${hours}h ${minutes}m"
            minutes > 0 -> "${minutes}m ${seconds}s"
            else -> "${seconds}s"
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier.fillMaxWidth().padding(start = 8.dp, top = 12.dp, end = 8.dp, bottom = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            IconButton(onClick = onClose, modifier = Modifier.align(Alignment.CenterStart)) {
                Icon(Icons.Default.Close, stringResource(R.string.close_button), tint = MaterialTheme.accentCyan)
            }
            // Metadata display
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(stringResource(R.string.queue_title), style = MaterialTheme.typography.bodyMedium.copy(fontFamily = Poppins, fontWeight = FontWeight.Bold, fontSize = 14.sp), color = MaterialTheme.colorScheme.onBackground)
                Text("$songCount tracks • $timeLabel", style = MaterialTheme.typography.labelSmall.copy(fontFamily = Poppins), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            // Lock control
            IconButton(onClick = onLockToggle, modifier = Modifier.align(Alignment.CenterEnd)) {
                Icon(
                    imageVector = if (isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                    contentDescription = null,
                    tint = if (isLocked) MaterialTheme.accentCyan else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        // Pull progress bar
        Box(modifier = Modifier.fillMaxWidth().height(3.dp).padding(horizontal = 24.dp).alpha(pullProgress)) {
            Box(modifier = Modifier.fillMaxHeight().fillMaxWidth(pullProgress).clip(CircleShape).background(MaterialTheme.accentCyan))
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}

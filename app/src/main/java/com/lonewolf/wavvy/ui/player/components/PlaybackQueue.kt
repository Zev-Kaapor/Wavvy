package com.lonewolf.wavvy.ui.player.components

// Compose layouts and foundations
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
// Material 3 components
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.platform.LocalConfiguration
import android.content.res.Configuration
import android.os.Parcelable
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
// Project resources
import com.lonewolf.wavvy.R
import com.lonewolf.wavvy.ui.common.components.SongOptionsBottomSheet
import com.lonewolf.wavvy.ui.player.PlayerViewModel
import com.lonewolf.wavvy.ui.theme.Poppins
import com.lonewolf.wavvy.ui.theme.WavvyTheme
import kotlinx.parcelize.Parcelize
// Image loading (Coil)
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import coil.size.Size as CoilSize
import androidx.compose.ui.platform.LocalContext
// Reorderable library
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

// Song data model
@Parcelize
data class QueueSong(
    val id: String,
    val title: String,
    val artist: String,
    val durationSeconds: Long = 0L,
    val imageUrl: String = ""
) : Parcelable

// Main queue container
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PlaybackQueue(
    modifier: Modifier = Modifier,
    viewModel: PlayerViewModel,
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
    offsetY: Float = 0f,
    maxOffset: Float = 0f,
    onOffsetYChange: (Float) -> Unit = {}
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val lazyListState = rememberLazyListState()
    val isDark = isSystemInDarkTheme()
    val accentColor = if (isDark) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary

    // Drag-to-load physics parameters
    val dragThresholdPx = 200f
    var pullUpDelta by remember { mutableFloatStateOf(0f) }

    val animatedPullUpDelta by animateFloatAsState(
        targetValue = pullUpDelta,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "PullUpReset"
    )

    // Suppress unused warning checks safely
    android.util.Log.v("PlaybackQueue", "Offset state: $offsetY / $maxOffset (drag state handled by container structure)")
    val onOffsetChangeState by rememberUpdatedState(onOffsetYChange)

    // Pagination state flows observation
    val isLoadingMore by viewModel.isLoadingMore.collectAsState()

    // Menu state
    var showMenu by remember { mutableStateOf(false) }
    var selectedSong by remember { mutableStateOf<QueueSong?>(null) }

    // Total duration calculation
    val totalDurationSeconds = remember(playlist.toList()) {
        playlist.sumOf { it.durationSeconds }
    }

    // Intercept nested scrolls to drive the pull-up gesture logic smoothly
    val nestedScrollConnection = remember(isLoadingMore, playlist.size) {
        object : NestedScrollConnection {
            var isTopReached = false

            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (available.y < 0) {
                    isTopReached = false
                }

                if (available.y < 0 && !lazyListState.canScrollForward && !isLoadingMore && playlist.isNotEmpty()) {
                    pullUpDelta += -available.y / 2f
                    return available
                }

                if (available.y > 0 && pullUpDelta > 0f) {
                    val consumed = available.y
                    pullUpDelta = (pullUpDelta - consumed).coerceAtLeast(0f)
                    return Offset(0f, consumed)
                }

                return Offset.Zero
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource,
            ): Offset {
                if (!isTopReached) {
                    isTopReached = consumed.y == 0f && available.y > 0
                }
                return if (isTopReached && source == NestedScrollSource.UserInput) {
                    available
                } else {
                    Offset.Zero
                }
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                if (pullUpDelta >= dragThresholdPx && !isLoadingMore) {
                    viewModel.loadMoreQueueSongs()
                }
                pullUpDelta = 0f
                return if (isTopReached) available else Velocity.Zero
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                isTopReached = false
                pullUpDelta = 0f
                return Velocity.Zero
            }
        }
    }

    WavvyTheme {
        Surface(
            modifier = modifier
                .fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val screenWidth = constraints.maxWidth.toFloat()

                Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
                    if (isLandscape) {
                        Spacer(modifier = Modifier.height(20.dp))
                    }

                    // Header section
                    Box(modifier = Modifier.fillMaxWidth().then(dragModifier)) {
                        QueueHeaderWithProgress(
                            onClose = onClose,
                            songCount = playlist.size,
                            totalDurationSeconds = totalDurationSeconds,
                            isLocked = isLocked,
                            accentColor = accentColor,
                            onLockToggle = {
                                onLockToggle(!isLocked)
                                onOffsetChangeState(0f)
                            }
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
                            // Songs list with drag and hold support
                            LazyColumn(
                                state = lazyListState,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .nestedScroll(nestedScrollConnection),
                                contentPadding = PaddingValues(bottom = 80.dp)
                            ) {
                                itemsIndexed(playlist, key = { _, song -> song.id }) { index, song ->
                                    val isNowPlaying = index == currentIndex
                                    val isLastItem = index == playlist.lastIndex
                                    val reorderableState = rememberReorderableLazyListState(lazyListState) { from, to ->
                                        if (!isLocked) {
                                            playlist.add(to.index, playlist.removeAt(from.index))
                                        }
                                    }

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
                                                    accentColor = accentColor,
                                                    modifier = if (isLocked) Modifier else Modifier.draggableHandle(),
                                                    onClick = {
                                                        onIndexChange(index)
                                                        if (isLastItem && !isLoadingMore) {
                                                            viewModel.loadMoreQueueSongs()
                                                        }
                                                    },
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

                                            val dismissState = rememberSwipeToDismissBoxState(
                                                positionalThreshold = { it * 0.5f }
                                            )

                                            LaunchedEffect(dismissState.currentValue) {
                                                val actualIndex = currentPlaylistState.indexOfFirst { it.id == song.id }
                                                if (actualIndex != -1) {
                                                    when (dismissState.currentValue) {
                                                        SwipeToDismissBoxValue.StartToEnd -> {
                                                            currentPlaylistState.removeAt(actualIndex)
                                                            if (actualIndex < currentIndexState) {
                                                                onIndexChange(currentIndexState - 1)
                                                            }
                                                        }
                                                        SwipeToDismissBoxValue.EndToStart -> {
                                                            if (currentPlaylistState.size > 1) {
                                                                val item = currentPlaylistState.removeAt(actualIndex)
                                                                val targetPos = if (actualIndex < currentIndexState) currentIndexState else currentIndexState + 1
                                                                currentPlaylistState.add(targetPos.coerceIn(0, currentPlaylistState.size), item)

                                                                if (actualIndex < currentIndexState) {
                                                                    onIndexChange(currentIndexState - 1)
                                                                }
                                                            }
                                                            dismissState.snapTo(SwipeToDismissBoxValue.Settled)
                                                        }
                                                        else -> {}
                                                    }
                                                }
                                            }

                                            SwipeToDismissBox(
                                                state = dismissState,
                                                backgroundContent = { RevealBackground(dismissState, accentColor) },
                                                modifier = Modifier.padding(vertical = 4.dp),
                                                enableDismissFromStartToEnd = true,
                                                enableDismissFromEndToStart = true
                                            ) {
                                                Box(modifier = Modifier
                                                    .graphicsLayer {
                                                        val currentOffset = try { dismissState.requireOffset() } catch (_: Exception) { 0f }
                                                        val limit = if (dismissState.targetValue == SwipeToDismissBoxValue.EndToStart) screenWidth * 0.4f else screenWidth
                                                        val clamped = currentOffset.coerceIn(-limit, limit)
                                                        translationX = clamped - currentOffset
                                                    }
                                                    .scale(if (isDragging) 1.02f else 1f)
                                                    .animateItem(placementSpec = tween(500))
                                                ) {
                                                    QueueItem(
                                                        song = song,
                                                        isNowPlaying = false,
                                                        isHistory = index < currentIndex,
                                                        isPlaying = isPlaying,
                                                        isLocked = isLocked,
                                                        accentColor = accentColor,
                                                        modifier = Modifier.draggableHandle(),
                                                        onClick = {
                                                            onIndexChange(index)
                                                            if (isLastItem && !isLoadingMore) {
                                                                viewModel.loadMoreQueueSongs()
                                                            }
                                                        },
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

                                // Interactive loading indicator or Pull-up gesture circle progress bar
                                if (isLoadingMore || animatedPullUpDelta > 0f) {
                                    item {
                                        val rawProgress = (animatedPullUpDelta / dragThresholdPx).coerceIn(0f, 1f)
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 20.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (isLoadingMore) {
                                                CircularProgressIndicator(
                                                    modifier = Modifier.size(28.dp),
                                                    color = accentColor,
                                                    strokeWidth = 2.5.dp
                                                )
                                            } else {
                                                CircularProgressIndicator(
                                                    progress = { rawProgress },
                                                    modifier = Modifier
                                                        .size(28.dp)
                                                        .graphicsLayer {
                                                            rotationZ = rawProgress * 360f
                                                            scaleX = 0.8f + (rawProgress * 0.2f)
                                                            scaleY = 0.8f + (rawProgress * 0.2f)
                                                        },
                                                    color = if (rawProgress >= 1f) accentColor else accentColor.copy(alpha = 0.5f),
                                                    strokeWidth = 2.5.dp,
                                                    trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                                )
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
                            accentColor = accentColor,
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
            val fallbackArtist = stringResource(R.string.default_artist_name)
            val cleanArtistList = remember(selectedSong!!.artist) {
                if (selectedSong!!.artist.isNotBlank()) listOf(selectedSong!!.artist.trim()) else listOf(fallbackArtist)
            }

            SongOptionsBottomSheet(
                songTitle = selectedSong!!.title,
                artistNames = cleanArtistList,
                isSimplified = false,
                onDismiss = { showMenu = false },
                onActionClick = { }
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
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    val inactive = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
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
                ShuffleButton(isShuffleActive, onShuffleClick, inactive, accentColor)
                RepeatButton(repeatMode, onRepeatClick, inactive, accentColor)
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                AnimatedIconButton(onClick = { }) { mod ->
                    Icon(Icons.Default.Search, stringResource(R.string.search_hint), tint = inactive, modifier = mod.size(22.dp))
                }

                AnimatedIconButton(onClick = { }) { mod ->
                    Icon(Icons.Default.Checklist, null, tint = inactive, modifier = mod.size(22.dp))
                }

                AnimatedIconButton(onClick = { }) { mod ->
                    Icon(Icons.AutoMirrored.Filled.PlaylistAdd, stringResource(R.string.queue_menu_add_playlist), tint = inactive, modifier = mod.size(24.dp))
                }

                AnimatedIconButton(onClick = { }) { mod ->
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
            imageVector = Icons.AutoMirrored.Filled.QueueMusic,
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
fun RevealBackground(state: SwipeToDismissBoxState, accentColor: Color) {
    val color = when (state.dismissDirection) {
        SwipeToDismissBoxValue.StartToEnd -> MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
        SwipeToDismissBoxValue.EndToStart -> accentColor.copy(alpha = 0.8f)
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
                    imageVector = Icons.AutoMirrored.Filled.PlaylistPlay,
                    contentDescription = stringResource(R.string.queue_menu_play_next_item),
                    modifier = Modifier.padding(end = 20.dp),
                    tint = if (isSystemInDarkTheme()) Color.Black else Color.White
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
    accentColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onMoreClick: () -> Unit
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isNowPlaying) accentColor.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surface,
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
            Box(
                modifier = Modifier.size(32.dp),
                contentAlignment = Alignment.Center
            ) {
                if (isNowPlaying) {
                    EqualizerBars(isPlaying = isPlaying, accentColor = accentColor)
                } else {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = stringResource(R.string.reorder_handle),
                        tint = if (isLocked) Color.Transparent else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        modifier = modifier.size(20.dp)
                    )
                }
            }

            Box(
                modifier = Modifier
                    .padding(start = 8.dp)
                    .size(50.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer)
            ) {
                if (song.imageUrl.isNotBlank()) {
                    val context = LocalContext.current
                    SubcomposeAsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(song.imageUrl)
                            .crossfade(true)
                            .size(CoilSize.ORIGINAL)
                            .build(),
                        contentDescription = null,
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            Column(modifier = Modifier.weight(1f).padding(horizontal = 16.dp)) {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.bodyMedium.copy(fontFamily = Poppins, fontWeight = FontWeight.SemiBold),
                    color = if (isNowPlaying) accentColor else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
                Text(
                    text = song.artist,
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = Poppins),
                    color = if (isNowPlaying) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    maxLines = 1
                )
            }

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
private fun EqualizerBars(isPlaying: Boolean, accentColor: Color) {
    val infiniteTransition = rememberInfiniteTransition(label = "equalizer")

    val b1 by infiniteTransition.animateFloat(0.3f, 1f, infiniteRepeatable(tween(400), RepeatMode.Reverse), "b1")
    val b2 by infiniteTransition.animateFloat(0.5f, 1f, infiniteRepeatable(tween(600), RepeatMode.Reverse), "b2")
    val b3 by infiniteTransition.animateFloat(0.2f, 1f, infiniteRepeatable(tween(500), RepeatMode.Reverse), "b3")

    val heights = listOf(b1, b2, b3)

    Row(
        modifier = Modifier.size(20.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        heights.forEach { factor ->
            val animatedHeight = if (isPlaying) 16.dp * factor else 4.dp
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(animatedHeight)
                    .background(accentColor, RoundedCornerShape(1.dp))
            )
        }
    }
}

// Placeholder structure for missing header content rendering references
@Composable
private fun QueueHeaderWithProgress(
    onClose: () -> Unit,
    songCount: Int,
    totalDurationSeconds: Long,
    isLocked: Boolean,
    accentColor: Color,
    onLockToggle: () -> Unit
) {
    val mins = totalDurationSeconds / 60
    val timeLabel = "${mins}m"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onClose) {
            Icon(Icons.Default.Close, null, tint = accentColor)
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = stringResource(R.string.queue_title),
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = Poppins,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                ),
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "$songCount tracks • $timeLabel",
                style = MaterialTheme.typography.labelSmall.copy(fontFamily = Poppins),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        IconButton(onClick = onLockToggle) {
            Icon(
                imageVector = if (isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                contentDescription = null,
                tint = if (isLocked) accentColor else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

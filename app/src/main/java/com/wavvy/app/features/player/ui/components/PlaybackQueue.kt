package com.wavvy.app.features.player.ui.components

// Android app, intents and toasts
import android.content.Context
import android.content.Intent
import android.widget.Toast
// Back handling
import androidx.activity.compose.BackHandler
// Compose animation
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode as AnimationRepeat
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
// Compose layouts and foundations
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
// Material 3 components
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxState
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
// Compose state and runtime
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
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
// UI styling and utilities
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Velocity
// Image loading
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.transformations
// Reorderable list
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
// Java text
import java.text.Normalizer
// Coroutines
import kotlinx.coroutines.launch
// Project resources
import com.wavvy.app.R
import com.wavvy.app.core.designsystem.components.VideoBadge
import com.wavvy.app.core.designsystem.components.VideoSquareCrop
import com.wavvy.app.core.designsystem.icons.WavvyIcons
import com.wavvy.app.core.designsystem.theme.WavvyTheme
import com.wavvy.app.core.innertube.MusicOrigin
import com.wavvy.app.core.innertube.isVideoThumbnail
import com.wavvy.app.core.innertube.resize
import com.wavvy.app.core.playback.PlayableTrack
import com.wavvy.app.core.playback.PlayerConnection
import com.wavvy.app.core.playback.QueueEntry

// Units of the length of the queue
private const val MillisPerSecond = 1000L
private const val SecondsPerMinute = 60L
private const val SecondsPerHour = 3_600L
private const val SecondsPerDay = 86_400L
private const val SecondsPerMonth = 2_592_000L
private const val SecondsPerYear = 31_536_000L

// What the icon at the end of the song that plays shows
private enum class QueueIndicator { Loading, Playing, Paused }

// Queue of the old Wavvy over the open player, the songs in the order they play
@Composable
fun PlaybackQueue(
    isLandscape: Boolean,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val queue by PlayerConnection.queue.collectAsState()
    val currentIndex by PlayerConnection.currentIndex.collectAsState()
    val isPlaying by PlayerConnection.isPlaying.collectAsState()
    val isLoading by PlayerConnection.isLoading.collectAsState()
    val isLoadingMore by PlayerConnection.isLoadingMore.collectAsState()
    val repeatMode by PlayerConnection.repeat.collectAsState()
    val isShuffleActive by PlayerConnection.shuffle.collectAsState()
    val accent = WavvyTheme.colors.playerAccent
    val listState = rememberLazyListState()
    var isLocked by rememberSaveable { mutableStateOf(false) }
    val currentOnClose by rememberUpdatedState(onClose)

    // Song whose options are open, kept by its id so the menu follows it if the queue changes
    var menuSongId by rememberSaveable { mutableStateOf<String?>(null) }

    // Search of the queue, open or closed and the words typed
    var isSearching by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }

    // Selection of several songs, on or off and the ids of the ones marked
    var isSelecting by remember { mutableStateOf(false) }
    var selectedIds by remember { mutableStateOf(emptySet<String>()) }

    // Copy of the queue the drag moves, the player gets the new order when the drag ends
    val entries = remember { mutableStateListOf<QueueEntry>() }
    var isDragging by remember { mutableStateOf(false) }
    var shownCurrent by remember { mutableIntStateOf(-1) }
    var shownShuffle by remember { mutableStateOf(isShuffleActive) }
    LaunchedEffect(queue, currentIndex) {
        if (isDragging) return@LaunchedEffect
        entries.clear()
        entries.addAll(queue)

        // A new song or a new order puts the song that plays second again, under the one before it, unless a search is showing part of the queue
        if (query.isBlank() && (currentIndex != shownCurrent || isShuffleActive != shownShuffle)) {
            val position = entries.indexOfFirst { it.index == currentIndex }
            if (position >= 0) {
                listState.scrollToItem((position - 1).coerceAtLeast(0))
                shownCurrent = currentIndex
                shownShuffle = isShuffleActive
            }
        }
    }

    // The songs shown are the ones with the words typed in their title or artist, accents and case left out
    val shown by remember {
        derivedStateOf {
            val words = query.fold()
            if (words.isBlank()) entries.toList() else entries.filter { "${it.track.title} ${it.track.artist.orEmpty()}".fold().contains(words) }
        }
    }
    val isFiltering = query.isNotBlank()

    // Back closes the search before anything else of the queue
    BackHandler(enabled = isSearching) {
        isSearching = false
        query = ""
    }

    // Songs marked, a song that left the queue is no longer one of them, and back leaves the selection first of all
    // Playing next and removing make no sense for the song that plays, so those two wait until it is not among the marked ones
    val selected = entries.filter { it.track.id in selectedIds }
    val selectionHasCurrent = selected.any { it.index == currentIndex }
    BackHandler(enabled = isSelecting) {
        isSelecting = false
        selectedIds = emptySet()
    }

    // A shuffled order is not the order of the player, and a filtered one is not the whole queue, so they are not dragged or played next
    val canReorder = !isLocked && !isShuffleActive && !isFiltering && !isSelecting
    val reorderState = rememberReorderableLazyListState(listState) { from, to ->
        entries.add(to.index, entries.removeAt(from.index))
    }

    val hasEntries = entries.isNotEmpty()

    // Pulled past the end the radio brings more songs, pulled past the top the queue closes
    var pullUp by remember { mutableFloatStateOf(0f) }
    var pullDown by remember { mutableFloatStateOf(0f) }
    val animatedPullUp by animateFloatAsState(pullUp, spring(stiffness = Spring.StiffnessMediumLow), label = "PullUp")
    val animatedPullDown by animateFloatAsState(pullDown, spring(stiffness = Spring.StiffnessMediumLow), label = "PullDown")

    val pullConnection = remember(isLoadingMore, hasEntries) {
        object : NestedScrollConnection {
            var isTopReached = false

            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (available.y < 0) isTopReached = false

                if (available.y < 0 && !listState.canScrollForward && !isLoadingMore && hasEntries) {
                    pullUp += -available.y * PlayerDimens.QueuePullResistance
                    return available
                }
                if (available.y > 0 && pullUp > 0f) {
                    pullUp = (pullUp - available.y).coerceAtLeast(0f)
                    return Offset(0f, available.y)
                }
                if (available.y > 0 && !listState.canScrollBackward) {
                    pullDown += available.y * PlayerDimens.QueuePullResistance
                    return available
                }
                return Offset.Zero
            }

            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                if (!isTopReached) isTopReached = consumed.y == 0f && available.y > 0
                return if (isTopReached && source == NestedScrollSource.UserInput) available else Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                if (pullUp >= PlayerDimens.QueueLoadPullPx && !isLoadingMore) PlayerConnection.loadMore()
                if (pullDown >= PlayerDimens.QueueClosePullPx) currentOnClose()
                pullUp = 0f
                pullDown = 0f
                return if (isTopReached) available else Velocity.Zero
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                isTopReached = false
                pullUp = 0f
                pullDown = 0f
                return Velocity.Zero
            }
        }
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                // Drags outside the list stay in the queue, so pulling the header down closes only the queue and not the player behind it
                .pointerInput(Unit) {
                    detectVerticalDragGestures(
                        onDragEnd = {
                            if (pullDown >= PlayerDimens.QueueClosePullPx) currentOnClose()
                            pullDown = 0f
                        },
                        onDragCancel = { pullDown = 0f }
                    ) { change, drag ->
                        change.consume()
                        pullDown = (pullDown + drag * PlayerDimens.QueuePullResistance).coerceAtLeast(0f)
                    }
                }
                .graphicsLayer {
                    translationY = animatedPullDown * PlayerDimens.QueuePullTranslation
                    alpha = 1f - (animatedPullDown / PlayerDimens.QueueClosePullPx).coerceIn(0f, 1f) * PlayerDimens.QueuePullFade
                }
        ) {
            if (isLandscape) Spacer(Modifier.height(PlayerDimens.QueueTopLandscape))

            if (isSelecting) {
                SelectionHeader(
                    count = selected.size,
                    allSelected = entries.isNotEmpty() && selected.size == entries.size,
                    accent = accent,
                    onExit = {
                        isSelecting = false
                        selectedIds = emptySet()
                    },
                    onToggleAll = {
                        selectedIds = if (selected.size == entries.size) emptySet() else entries.mapTo(HashSet()) { it.track.id }
                    }
                )
            } else {
                QueueHeader(
                    songCount = entries.size,
                    totalMs = entries.sumOf { it.track.durationMs },
                    isLocked = isLocked,
                    accent = accent,
                    onClose = onClose,
                    onLockToggle = { isLocked = !isLocked }
                )
            }

            if (isSearching) {
                QueueSearchField(
                    query = query,
                    onQueryChange = { query = it },
                    accent = accent,
                    onClose = {
                        isSearching = false
                        query = ""
                    }
                )
            }

            // The keyboard lifts the list, so what is typed never hides the songs found
            Box(modifier = Modifier.fillMaxSize().imePadding()) {
                if (entries.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                    ) {
                        EmptyQueue()
                    }
                } else if (shown.isEmpty()) {
                    Text(
                        text = stringResource(R.string.queue_search_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(PlayerDimens.QueueEmptyPadding)
                    )
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .nestedScroll(pullConnection),
                        contentPadding = PaddingValues(bottom = PlayerDimens.QueueListBottom)
                    ) {
                        itemsIndexed(shown, key = { _, entry -> entry.track.id }) { shownPosition, entry ->
                            // Place of the song in the whole queue, which is not its place in the list while a search shows part of it
                            val position = if (isFiltering) entries.indexOf(entry) else shownPosition
                            val isCurrent = entry.index == currentIndex
                            val canSwipe = !isCurrent && !isLocked && !isSelecting

                            ReorderableItem(reorderState, key = entry.track.id) { isItemDragging ->
                                val handle = if (canReorder) {
                                    // The handle keeps its first callbacks, so the song is found again by its id when the drag ends
                                    val songId = entry.track.id
                                    Modifier.draggableHandle(
                                        onDragStarted = { isDragging = true },
                                        onDragStopped = {
                                            val target = entries.indexOfFirst { it.track.id == songId }
                                            entries.getOrNull(target)?.let { moved -> PlayerConnection.move(moved.index, target) }
                                            isDragging = false
                                        }
                                    )
                                } else {
                                    Modifier
                                }

                                SwipeableQueueItem(
                                    entry = entry,
                                    canSwipe = canSwipe,
                                    canPlayNext = canSwipe && !isShuffleActive,
                                    isDragging = isItemDragging,
                                    accent = accent
                                ) {
                                    QueueItem(
                                        entry = entry,
                                        isCurrent = isCurrent,
                                        isHistory = position < entries.indexOfFirst { it.index == currentIndex },
                                        indicator = when {
                                            isLoading -> QueueIndicator.Loading
                                            isPlaying -> QueueIndicator.Playing
                                            else -> QueueIndicator.Paused
                                        },
                                        showHandle = canReorder,
                                        modifier = handle,
                                        accent = accent,
                                        isSelected = if (isSelecting) entry.track.id in selectedIds else null,
                                        onClick = {
                                            if (isSelecting) {
                                                val id = entry.track.id
                                                selectedIds = if (id in selectedIds) selectedIds - id else selectedIds + id
                                            } else if (isCurrent) {
                                                PlayerConnection.togglePlayPause()
                                            } else {
                                                PlayerConnection.playAt(entry.index)
                                                if (position == entries.lastIndex) PlayerConnection.loadMore()
                                            }
                                        },
                                        onMoreClick = { menuSongId = entry.track.id }
                                    )
                                }
                            }
                        }

                        if (isLoadingMore || animatedPullUp > 0f) {
                            item {
                                LoadMoreSpinner(
                                    isLoading = isLoadingMore,
                                    pull = { (animatedPullUp / PlayerDimens.QueueLoadPullPx).coerceIn(0f, 1f) },
                                    accent = accent
                                )
                            }
                        }
                    }
                }

                // The bar steps aside while searching, the field and the keyboard take its place, and it turns into the actions of the selection
                val context = LocalContext.current
                if (isSelecting) {
                    SelectionBar(
                        hasSelection = selected.isNotEmpty(),
                        canPlayNext = !isShuffleActive && !selectionHasCurrent,
                        canRemove = !isLocked && !selectionHasCurrent,
                        accent = accent,
                        onPlayNext = {
                            PlayerConnection.playNextAll(selectedIds)
                            selectedIds = emptySet()
                        },
                        onShare = { shareQueue(context, selected.map { it.track }) },
                        onRemove = {
                            PlayerConnection.removeAll(selectedIds)
                            selectedIds = emptySet()
                        },
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                    )
                } else if (!isSearching) {
                    QueueBar(
                        repeatMode = repeatMode,
                        isShuffleActive = isShuffleActive,
                        accent = accent,
                        onSelectClick = { isSelecting = true },
                        onSearchClick = { isSearching = true },
                        onShareClick = { shareQueue(context, shown.map { it.track }) },
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                    )
                }
            }
        }
    }

    // Options of a song, closed by itself when the song leaves the queue
    val menuEntry = entries.firstOrNull { it.track.id == menuSongId }
    if (menuEntry != null) {
        val isCurrent = menuEntry.index == currentIndex
        QueueItemMenu(
            entry = menuEntry,
            canPlayNext = !isCurrent && !isShuffleActive,
            canRemove = !isCurrent && !isLocked,
            onPlayNext = { PlayerConnection.playNext(menuEntry.index) },
            onRemove = { PlayerConnection.remove(menuEntry.index) },
            onDismiss = { menuSongId = null }
        )
    }
}

// Title, the number of songs and how long they last, with close and lock at the sides
@Composable
private fun QueueHeader(
    songCount: Int,
    totalMs: Long,
    isLocked: Boolean,
    accent: Color,
    onClose: () -> Unit,
    onLockToggle: () -> Unit
) {
    // Length split from years down to seconds, each unit shown only when it is not zero
    var rest = totalMs / MillisPerSecond
    val parts = buildList {
        val years = rest / SecondsPerYear
        rest %= SecondsPerYear
        val months = rest / SecondsPerMonth
        rest %= SecondsPerMonth
        val days = rest / SecondsPerDay
        rest %= SecondsPerDay
        val hours = rest / SecondsPerHour
        rest %= SecondsPerHour
        val minutes = rest / SecondsPerMinute
        val seconds = rest % SecondsPerMinute

        if (years > 0) add(stringResource(R.string.queue_years, years.toInt()))
        if (months > 0) add(pluralStringResource(R.plurals.queue_months, months.toInt(), months.toInt()))
        if (days > 0) add(stringResource(R.string.queue_days, days.toInt()))
        if (hours > 0) add(stringResource(R.string.queue_hours, hours.toInt()))
        if (minutes > 0) add(stringResource(R.string.queue_minutes, minutes.toInt()))
        if (seconds > 0 || isEmpty()) add(stringResource(R.string.queue_seconds, seconds.toInt()))
    }
    val length = parts.joinToString(stringResource(R.string.queue_length_separator))
    val songs = pluralStringResource(R.plurals.queue_songs, songCount, songCount)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.displayCutout)
            .padding(PlayerDimens.QueueHeaderPadding),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onClose) {
            Icon(WavvyIcons.Close, stringResource(R.string.queue_close), tint = accent)
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = stringResource(R.string.queue_title),
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, fontSize = PlayerDimens.QueueTitleSize),
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = stringResource(R.string.queue_summary, songs, length),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        IconButton(onClick = onLockToggle) {
            Icon(
                imageVector = if (isLocked) WavvyIcons.Lock else WavvyIcons.LockOpen,
                contentDescription = stringResource(if (isLocked) R.string.queue_unlock else R.string.queue_lock),
                tint = if (isLocked) accent else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// A song that slides right to leave the queue and left to play next, only a little of the way so it never leaves the screen
@Composable
private fun SwipeableQueueItem(
    entry: QueueEntry,
    canSwipe: Boolean,
    canPlayNext: Boolean,
    isDragging: Boolean,
    accent: Color,
    content: @Composable () -> Unit
) {
    val scope = rememberCoroutineScope()
    val dismissState = rememberSwipeToDismissBoxState(positionalThreshold = { it * PlayerDimens.QueueSwipeThreshold })
    val current by rememberUpdatedState(entry)

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = { if (canSwipe) SwipeBackground(dismissState, accent) },
        modifier = Modifier.padding(vertical = PlayerDimens.QueueItemGap),
        enableDismissFromStartToEnd = canSwipe,
        enableDismissFromEndToStart = canPlayNext,
        onDismiss = { direction ->
            when (direction) {
                SwipeToDismissBoxValue.StartToEnd -> PlayerConnection.remove(current.index)
                SwipeToDismissBoxValue.EndToStart -> {
                    PlayerConnection.playNext(current.index)
                    scope.launch { dismissState.reset() }
                }
                SwipeToDismissBoxValue.Settled -> Unit
            }
        }
    ) {
        Box(
            modifier = Modifier.graphicsLayer {
                if (canSwipe) {
                    // The card stops at a share of the width while it is pulled
                    val offset = runCatching { dismissState.requireOffset() }.getOrDefault(0f)
                    val limit = size.width * PlayerDimens.QueueSwipeLimit
                    translationX = offset.coerceIn(-limit, limit) - offset
                }
                val scale = if (isDragging) PlayerDimens.QueueDraggingScale else 1f
                scaleX = scale
                scaleY = scale
            }
        ) {
            content()
        }
    }
}

// Color behind a song being swiped, red with a bin to remove it and the accent to play it next
@Composable
private fun SwipeBackground(state: SwipeToDismissBoxState, accent: Color) {
    val direction = state.dismissDirection
    val remove = MaterialTheme.colorScheme.error
    val colors = when (direction) {
        SwipeToDismissBoxValue.StartToEnd -> listOf(remove, remove.copy(alpha = PlayerDimens.QueueSwipeFadeAlpha))
        SwipeToDismissBoxValue.EndToStart -> listOf(accent.copy(alpha = PlayerDimens.QueueSwipeFadeAlpha), accent)
        SwipeToDismissBoxValue.Settled -> listOf(Color.Transparent, Color.Transparent)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = PlayerDimens.QueueItemSide)
            .clip(RoundedCornerShape(PlayerDimens.QueueSwipeCorner))
            .background(Brush.horizontalGradient(colors)),
        contentAlignment = if (direction == SwipeToDismissBoxValue.StartToEnd) Alignment.CenterStart else Alignment.CenterEnd
    ) {
        when (direction) {
            SwipeToDismissBoxValue.StartToEnd -> Icon(
                imageVector = WavvyIcons.Delete,
                contentDescription = stringResource(R.string.queue_remove),
                tint = MaterialTheme.colorScheme.onError,
                modifier = Modifier
                    .padding(start = PlayerDimens.QueueSwipeIconSide)
                    .size(PlayerDimens.QueueSwipeIcon)
            )
            SwipeToDismissBoxValue.EndToStart -> Icon(
                imageVector = WavvyIcons.PlaylistPlay,
                contentDescription = stringResource(R.string.queue_play_next),
                tint = MaterialTheme.colorScheme.background,
                modifier = Modifier
                    .padding(end = PlayerDimens.QueueSwipeIconSide)
                    .size(PlayerDimens.QueueSwipeIcon)
            )
            SwipeToDismissBoxValue.Settled -> Unit
        }
    }
}

// A song of the queue, the one that plays lighter with its state at the end, the others with the handle that drags them
@Composable
private fun QueueItem(
    entry: QueueEntry,
    isCurrent: Boolean,
    isHistory: Boolean,
    indicator: QueueIndicator,
    showHandle: Boolean,
    modifier: Modifier = Modifier,
    accent: Color,
    isSelected: Boolean?,
    onClick: () -> Unit,
    onMoreClick: () -> Unit
) {
    val track = entry.track
    val background by animateColorAsState(
        targetValue = if (isCurrent) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant,
        label = "QueueItemBackground"
    )
    val length = track.durationMs.takeIf { it > 0 }?.let { formatTime(it / MillisPerSecond) }
    val subtitle = when {
        track.artist != null && length != null -> stringResource(R.string.queue_summary, track.artist, length)
        else -> track.artist ?: length.orEmpty()
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = PlayerDimens.QueueItemSide)
            .clip(RoundedCornerShape(PlayerDimens.QueueItemCorner))
            .background(background)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(PlayerDimens.QueueItemPadding)
                .graphicsLayer { alpha = if (isHistory) PlayerDimens.QueueHistoryAlpha else 1f },
            verticalAlignment = Alignment.CenterVertically
        ) {
            // While songs are being marked, the box comes before the cover and the whole row marks or unmarks
            if (isSelected != null) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = null,
                    colors = CheckboxDefaults.colors(checkedColor = accent),
                    modifier = Modifier.padding(end = PlayerDimens.QueueCheckboxEnd)
                )
            }

            TrackCover(track = track, size = PlayerDimens.QueueCover)

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = PlayerDimens.QueueTextSide)
            ) {
                Text(
                    text = track.title,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = if (isCurrent) accent else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE)
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (isCurrent) 1f else PlayerDimens.QueueArtistAlpha),
                    maxLines = 1,
                    modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE)
                )
            }

            // The menu of a song is left out while marking, the whole row has one job
            if (isSelected == null) {
                IconButton(onClick = onMoreClick, modifier = Modifier.size(PlayerDimens.QueueMore)) {
                    Icon(
                        imageVector = WavvyIcons.MoreVertical,
                        contentDescription = stringResource(R.string.queue_song_options),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(PlayerDimens.QueueMoreIcon)
                    )
                }
            }

            Box(
                modifier = Modifier
                    .padding(start = PlayerDimens.QueueIndicatorStart)
                    .size(PlayerDimens.QueueIndicator),
                contentAlignment = Alignment.Center
            ) {
                if (isCurrent) {
                    AnimatedContent(targetState = indicator, label = "QueueIndicator") { state ->
                        when (state) {
                            QueueIndicator.Loading -> CircularProgressIndicator(
                                modifier = Modifier.size(PlayerDimens.QueueLoadingIcon),
                                color = accent,
                                strokeWidth = PlayerDimens.QueueLoadingStroke
                            )
                            QueueIndicator.Playing -> EqualizerBars(accent)
                            QueueIndicator.Paused -> Icon(
                                imageVector = WavvyIcons.PlayArrow,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(PlayerDimens.QueueIndicatorIcon)
                            )
                        }
                    }
                } else if (showHandle) {
                    Icon(
                        imageVector = WavvyIcons.DragHandle,
                        contentDescription = stringResource(R.string.queue_reorder),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = PlayerDimens.QueueHandleAlpha),
                        modifier = modifier.size(PlayerDimens.QueueIndicatorIcon)
                    )
                }
            }
        }
    }
}

// Cover of a song with the note of the player under it until the picture arrives, a video cut square and marked with a camera
@Composable
internal fun TrackCover(
    track: PlayableTrack,
    size: Dp,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val cover = remember(track.artworkUrl) {
        track.artworkUrl?.let { url ->
            ImageRequest.Builder(context)
                .data(url.resize(PlayerDimens.QueueCoverRequestSize, PlayerDimens.QueueCoverRequestSize))
                .apply { if (url.isVideoThumbnail()) transformations(VideoSquareCrop) }
                .build()
        }
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(PlayerDimens.QueueCoverCorner))
    ) {
        AlbumPlaceholder()
        AsyncImage(
            model = cover,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        if (track.isVideo) {
            VideoBadge(
                iconSize = PlayerDimens.QueueVideoBadgeIcon,
                padding = PlayerDimens.QueueVideoBadgePadding,
                modifier = Modifier.padding(PlayerDimens.QueueVideoBadgeInset)
            )
        }
    }
}

// Three bars that rise and fall while the song plays, moved only when drawn
@Composable
private fun EqualizerBars(color: Color) {
    val transition = rememberInfiniteTransition(label = "Equalizer")

    Row(
        modifier = Modifier.size(PlayerDimens.EqualizerSize),
        horizontalArrangement = Arrangement.spacedBy(PlayerDimens.EqualizerGap),
        verticalAlignment = Alignment.Bottom
    ) {
        PlayerDimens.EqualizerBars.forEach { (lowest, millis) ->
            val factor by transition.animateFloat(lowest, 1f, infiniteRepeatable(tween(millis), AnimationRepeat.Reverse), label = "EqualizerBar")
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(PlayerDimens.EqualizerBarHeight)
                    .graphicsLayer {
                        scaleY = factor
                        transformOrigin = TransformOrigin(0.5f, 1f)
                    }
                    .background(color, RoundedCornerShape(PlayerDimens.EqualizerBarCorner))
            )
        }
    }
}

// Circle at the end of the list, filling with the pull and spinning while more songs come
@Composable
private fun LoadMoreSpinner(
    isLoading: Boolean,
    pull: () -> Float,
    accent: Color
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = PlayerDimens.QueueSpinnerPadding),
        contentAlignment = Alignment.Center
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(PlayerDimens.QueueSpinner),
                color = accent,
                strokeWidth = PlayerDimens.QueueSpinnerStroke
            )
        } else {
            val progress = pull()
            CircularProgressIndicator(
                progress = pull,
                modifier = Modifier
                    .size(PlayerDimens.QueueSpinner)
                    .graphicsLayer {
                        val share = pull()
                        val scale = PlayerDimens.QueueSpinnerMinScale + share * (1f - PlayerDimens.QueueSpinnerMinScale)
                        rotationZ = share * PlayerDimens.FullTurn
                        scaleX = scale
                        scaleY = scale
                    },
                color = if (progress >= 1f) accent else accent.copy(alpha = PlayerDimens.QueueSpinnerIdleAlpha),
                strokeWidth = PlayerDimens.QueueSpinnerStroke,
                trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = PlayerDimens.QueueSpinnerTrackAlpha)
            )
        }
    }
}

// Bar at the bottom of the queue with shuffle and repeat
@Composable
private fun QueueBar(
    repeatMode: com.wavvy.app.core.playback.RepeatMode,
    isShuffleActive: Boolean,
    accent: Color,
    onSelectClick: () -> Unit,
    onSearchClick: () -> Unit,
    onShareClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val inactive = MaterialTheme.colorScheme.onSurface.copy(alpha = PlayerDimens.ToolbarInactiveAlpha)

    Box(
        modifier = modifier
            // Touches on the bar never reach the songs scrolled behind it
            .pointerInput(Unit) {}
            .background(MaterialTheme.colorScheme.background)
            .navigationBarsPadding()
            .height(PlayerDimens.QueueBarHeight)
            .padding(horizontal = PlayerDimens.QueueBarSide),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Modes on the start, tools on the end
            ToggleButton(WavvyIcons.Shuffle, isShuffleActive, PlayerConnection::toggleShuffle, inactive, accent)
            RepeatButton(repeatMode, PlayerConnection::toggleRepeat, inactive, accent)
            Spacer(Modifier.weight(1f))
            ToggleButton(WavvyIcons.Checklist, false, onSelectClick, inactive, accent)
            ToggleButton(WavvyIcons.Search, false, onSearchClick, inactive, accent)
            ToggleButton(WavvyIcons.Share, false, onShareClick, inactive, accent)
        }
    }
}

// Bar of the selection, the actions for the marked songs, shown only while there are some
@Composable
private fun SelectionBar(
    hasSelection: Boolean,
    canPlayNext: Boolean,
    canRemove: Boolean,
    accent: Color,
    onPlayNext: () -> Unit,
    onShare: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    val inactive = MaterialTheme.colorScheme.onSurface.copy(alpha = PlayerDimens.ToolbarInactiveAlpha)

    Box(
        modifier = modifier
            // Touches on the bar never reach the songs scrolled behind it
            .pointerInput(Unit) {}
            .background(MaterialTheme.colorScheme.background)
            .navigationBarsPadding()
            .height(PlayerDimens.QueueBarHeight)
            .padding(horizontal = PlayerDimens.QueueBarSide),
        contentAlignment = Alignment.Center
    ) {
        if (hasSelection) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(PlayerDimens.QueueSelectionGap),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (canPlayNext) ToggleButton(WavvyIcons.PlaylistPlay, false, onPlayNext, inactive, accent)
                ToggleButton(WavvyIcons.Share, false, onShare, inactive, accent)
                if (canRemove) ToggleButton(WavvyIcons.Delete, false, onRemove, inactive, accent)
            }
        } else {
            Text(
                text = stringResource(R.string.queue_select_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// Header of the selection, how many songs are marked, with the buttons to leave it and to mark all of them or none
@Composable
private fun SelectionHeader(
    count: Int,
    allSelected: Boolean,
    accent: Color,
    onExit: () -> Unit,
    onToggleAll: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.displayCutout)
            .padding(PlayerDimens.QueueHeaderPadding),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onExit) {
            Icon(WavvyIcons.Close, stringResource(R.string.queue_selection_exit), tint = accent)
        }
        Text(
            text = pluralStringResource(R.plurals.queue_selected, count, count),
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, fontSize = PlayerDimens.QueueTitleSize),
            color = MaterialTheme.colorScheme.onBackground
        )
        IconButton(onClick = onToggleAll) {
            Icon(
                imageVector = WavvyIcons.Checklist,
                contentDescription = stringResource(if (allSelected) R.string.queue_select_none else R.string.queue_select_all),
                tint = if (allSelected) accent else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// Field that filters the queue, with a button that clears the search and closes it, focused when it opens
@Composable
private fun QueueSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    accent: Color,
    onClose: () -> Unit
) {
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }

    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = { Text(stringResource(R.string.queue_search_hint)) },
        leadingIcon = {
            IconButton(onClick = onClose) {
                Icon(WavvyIcons.Back, contentDescription = stringResource(R.string.queue_search_close), tint = accent)
            }
        },
        // Clearing is for what was typed, leaving the search is the arrow at the start
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(WavvyIcons.Close, contentDescription = stringResource(R.string.queue_search_clear))
                }
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(PlayerDimens.QueueSearchCorner),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = PlayerDimens.QueueItemSide, vertical = PlayerDimens.QueueItemGap)
            .focusRequester(focus)
    )
}

// Text with the accents and the case taken out, so a search finds songs whatever way they were typed
private fun String.fold(): String =
    Normalizer.normalize(trim(), Normalizer.Form.NFD).replace(Regex("\\p{Mn}+"), "").lowercase()

// Shares the songs as lines with the title, the artist and the YouTube Music link, through the apps of the device
private fun shareQueue(context: Context, songs: List<PlayableTrack>) {
    if (songs.isEmpty()) {
        Toast.makeText(context, context.getString(R.string.player_share_empty), Toast.LENGTH_SHORT).show()
        return
    }

    val text = songs.joinToString("\n\n") { song ->
        listOfNotNull(song.title, song.artist, "$MusicOrigin/watch?v=${song.id}").joinToString("\n")
    }
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(intent, context.getString(R.string.player_share_via)))
}

// Note and a hint when nothing is in the queue
@Composable
private fun EmptyQueue() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(PlayerDimens.QueueEmptyPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = WavvyIcons.QueueMusic,
            contentDescription = null,
            modifier = Modifier.size(PlayerDimens.QueueEmptyIcon),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = PlayerDimens.QueueEmptyIconAlpha)
        )
        Spacer(Modifier.height(PlayerDimens.QueueEmptyGap))
        Text(
            text = stringResource(R.string.queue_empty_title),
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = stringResource(R.string.queue_empty_subtitle),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

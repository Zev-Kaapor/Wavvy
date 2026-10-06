package com.wavvy.app.core.designsystem.components

// Back handling
import androidx.activity.compose.BackHandler
// Compose animation
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
// Compose layouts and foundations
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
// Material 3 components
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
// Compose state and runtime
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
// UI styling and utilities
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
// Coroutines
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

// Corners of the top of the sheet and the room it always leaves above itself
private val SheetCorner = 28.dp
private val SheetTopGap = 48.dp

// Darkness behind the sheet when it is fully open
private const val ScrimAlpha = 0.32f

// A release past this share of the height, or faster than this downwards, closes the sheet
private const val DismissFraction = 0.3f
private val DismissVelocity = 125.dp

// Spring of the sheet moving on its own, without bounce
private val SettleSpec = spring<Float>(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow)

// Sheet that rises from the bottom over the screen, drawn in the same window so it keeps the immersive mode
// The content always scrolls first, the sheet only moves when the content is pulled down from its top or dragged by its handle,
// so quick flings while it opens or at the end of the list never close it or make it jump
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WavvySheet(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val currentOnDismiss by rememberUpdatedState(onDismiss)
    val dismissVelocity = with(density) { DismissVelocity.toPx() }

    // How far the sheet is pushed down from its open place, and its height once measured
    var offset by remember { mutableFloatStateOf(0f) }
    var sheetHeight by remember { mutableIntStateOf(0) }
    var fingerOffset by remember { mutableFloatStateOf(0f) }
    var isClosing by remember { mutableStateOf(false) }
    var isReady by remember { mutableStateOf(false) }
    var animation by remember { mutableStateOf<Job?>(null) }

    // Moves the sheet on its own to a place, starting with the speed the finger left it at
    fun animateTo(target: Float, velocity: Float = 0f, onEnd: () -> Unit = {}) {
        animation?.cancel()
        animation = scope.launch {
            // The spring may run past the open place when the finger left going up, the sheet never rises above the edge
            animate(offset, target, velocity, SettleSpec) { value, _ -> offset = value.coerceAtLeast(0f) }
            onEnd()
        }
    }

    // Slides out and then tells the screen it closed
    fun close(velocity: Float = 0f) {
        if (isClosing) return
        isClosing = true
        animateTo(sheetHeight.toFloat(), velocity) { currentOnDismiss() }
    }

    // A release closes the sheet when it went far or fast enough, otherwise it goes back up
    fun settle(velocity: Float) {
        if (offset > sheetHeight * DismissFraction || velocity > dismissVelocity) close(velocity) else animateTo(0f, velocity)
    }

    // Rises from below the first time its height is known, shown only once it already sits below the screen
    LaunchedEffect(sheetHeight > 0) {
        if (sheetHeight > 0 && !isClosing) {
            offset = sheetHeight.toFloat()
            isReady = true
            animateTo(0f)
        }
    }

    BackHandler { close() }

    // Takes from the content only what it leaves, and only downwards from its top
    val connection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                // A sheet pulled down goes back up before the content scrolls again
                if (available.y < 0 && offset > 0f && source == NestedScrollSource.UserInput) {
                    animation?.cancel()
                    val used = maxOf(available.y, -offset)
                    offset += used
                    return Offset(0f, used)
                }
                return Offset.Zero
            }

            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                // Only the finger pulls the sheet down, a fling that reaches the top never does
                if (available.y > 0 && source == NestedScrollSource.UserInput) {
                    animation?.cancel()
                    offset += available.y
                    return Offset(0f, available.y)
                }
                return Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                if (offset <= 0f) return Velocity.Zero
                settle(available.y)
                return available
            }
        }
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val maxSheetHeight = maxHeight - SheetTopGap

        // Dark behind the sheet, a tap on it closes the sheet and its drags never reach the screen below
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    alpha = if (isReady && sheetHeight > 0) (1f - offset / sheetHeight).coerceIn(0f, 1f) else 0f
                }
                .background(MaterialTheme.colorScheme.scrim.copy(alpha = ScrimAlpha))
                .pointerInput(Unit) { detectTapGestures { close() } }
                .pointerInput(Unit) { detectDragGestures { change, _ -> change.consume() } }
        )

        Surface(
            shape = RoundedCornerShape(topStart = SheetCorner, topEnd = SheetCorner),
            color = MaterialTheme.colorScheme.surfaceContainer,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                // The keyboard lifts the whole sheet, so the fields it opens for stay in sight
                .imePadding()
                .heightIn(max = maxSheetHeight)
                .onSizeChanged { sheetHeight = it.height }
                .graphicsLayer {
                    // Until it is ready it waits below the screen and hidden, so it never flashes open before rising
                    translationY = if (isReady) offset else size.height
                    alpha = if (isReady) 1f else 0f
                }
                // The handle, the title and the parts that do not scroll drag the sheet itself
                .draggable(
                    orientation = Orientation.Vertical,
                    state = rememberDraggableState { delta ->
                        // The finger travel is kept whole, so pulling up past the open place and back down only moves the sheet once the finger is back
                        fingerOffset += delta
                        offset = fingerOffset.coerceAtLeast(0f)
                    },
                    onDragStarted = {
                        animation?.cancel()
                        fingerOffset = offset
                    },
                    onDragStopped = { velocity -> settle(velocity) }
                )
                .nestedScroll(connection)
        ) {
            Column(modifier = Modifier.navigationBarsPadding()) {
                BottomSheetDefaults.DragHandle(modifier = Modifier.align(Alignment.CenterHorizontally))
                content()
            }
        }
    }
}

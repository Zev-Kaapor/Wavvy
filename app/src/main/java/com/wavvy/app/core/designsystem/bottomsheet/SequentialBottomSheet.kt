package com.wavvy.app.core.designsystem.bottomsheet

// Android system annotations
import android.annotation.SuppressLint
// Android Activity integration
import androidx.activity.compose.BackHandler
// Compose animations and specifications
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.spring
// Compose gestures and mutations
import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.DragScope
import androidx.compose.foundation.gestures.DraggableState
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
// Compose layouts and foundations
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.shape.RoundedCornerShape
// Material 3 components
import androidx.compose.material3.MaterialTheme
// Compose state and lifecycle hooks
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
// UI styling and utilities
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
// Asynchronous coroutine utilities
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties

// State values for bottom sheet bounds
enum class SheetStateValue {
    Closed,
    Half,
    Expanded
}

// Bottom-sheet container with smooth drag gestures
@SuppressLint("ConfigurationScreenWidthHeight")
@Composable
fun SequentialBottomSheet(
    onDismiss: () -> Unit,
    autoWrap: Boolean = false,
    startExpanded: Boolean = false,
    content: @Composable ColumnScope.(animateDismiss: () -> Unit) -> Unit
) {
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current

    val statusBarTop = WindowInsets.statusBars.getTop(density)
    val cutoutTop = WindowInsets.displayCutout.getTop(density)
    val topInsetDp = with(density) { maxOf(statusBarTop, cutoutTop).toDp() }

    var contentHeightDp by remember { mutableStateOf<Dp?>(null) }

    val closedBound = 0.dp
    val safeMaxHeight = remember(configuration.screenHeightDp, topInsetDp) {
        (configuration.screenHeightDp.dp - topInsetDp - 12.dp).coerceAtLeast(100.dp)
    }

    val halfBound = remember(safeMaxHeight) { safeMaxHeight * 0.5f }
    val expandedBound = remember(safeMaxHeight, contentHeightDp, autoWrap) {
        val measured = contentHeightDp
        if (autoWrap && measured != null) {
            measured.coerceAtMost(safeMaxHeight)
        } else {
            safeMaxHeight
        }
    }

    val sheetBoxHeight = remember(autoWrap, configuration.screenHeightDp, expandedBound) {
        if (autoWrap) expandedBound else configuration.screenHeightDp.dp
    }

    val scope = rememberCoroutineScope()
    val sheetState = rememberSequentialSheetState(coroutineScope = scope)

    SideEffect {
        sheetState.updateBounds(
            closed = closedBound,
            half = if (autoWrap) expandedBound else halfBound,
            expanded = expandedBound
        )
    }

    val animateDismiss = remember(sheetState, onDismiss) {
        { sheetState.animateToTarget(SheetStateValue.Closed, onDismiss) }
    }

    val scrollState = rememberScrollState()

    // Initial entrance animation when content height is known
    LaunchedEffect(expandedBound, contentHeightDp) {
        if (contentHeightDp == null) return@LaunchedEffect
        if (sheetState.currentAnchor != 0) return@LaunchedEffect

        when {
            autoWrap || startExpanded -> sheetState.animateToTarget(SheetStateValue.Expanded)
            else -> sheetState.animateToTarget(SheetStateValue.Half)
        }
    }

    // Back key handling logic
    BackHandler(enabled = sheetState.currentState != SheetStateValue.Closed) {
        when {
            autoWrap -> animateDismiss()
            sheetState.currentState == SheetStateValue.Expanded -> sheetState.animateToTarget(SheetStateValue.Half)
            sheetState.currentState == SheetStateValue.Half -> animateDismiss()
            else -> Unit
        }
    }

    // Block interactions while animation is running or when sheet is closed
    val isInteractionBlocked by remember {
        derivedStateOf { sheetState.currentState == SheetStateValue.Closed || sheetState.isAnimating }
    }

    val scrimAlpha by remember {
        derivedStateOf {
            if (expandedBound == closedBound) 0f
            else ((sheetState.value - closedBound) / (expandedBound - closedBound))
                .coerceIn(0f, 1f) * 0.6f
        }
    }

    Popup(
        onDismissRequest = { animateDismiss() },
        properties = PopupProperties(focusable = true, clippingEnabled = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = scrimAlpha))
                .pointerInput(isInteractionBlocked) {
                    if (!isInteractionBlocked) {
                        detectTapGestures {
                            animateDismiss()
                        }
                    }
                }
        ) {
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .then(
                    if (autoWrap) Modifier.fillMaxWidth().heightIn(max = safeMaxHeight)
                    else Modifier.fillMaxSize()
                )
                .graphicsLayer {
                    translationY = (sheetBoxHeight - sheetState.value)
                        .toPx()
                        .coerceAtLeast(0f)
                }
                .pointerInput(Unit) {
                    detectTapGestures { }
                }
                .then(
                    if (isInteractionBlocked) {
                        Modifier.pointerInput(Unit) {}
                    } else {
                        Modifier.draggable(
                            orientation = Orientation.Vertical,
                            state = sheetState,
                            onDragStarted = {
                                sheetState.captureDragStart()
                            },
                            onDragStopped = { velocity ->
                                if (scrollState.value == 0) {
                                    sheetState.processVelocity(-velocity, autoWrap, onDismiss)
                                }
                            }
                        )
                    }
                )
                .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .onSizeChanged { size ->
                        contentHeightDp = with(density) { size.height.toDp() }
                    }
            ) {
                // Sheet drag handle indicator
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .then(
                            if (isInteractionBlocked) {
                                Modifier
                            } else {
                                Modifier
                                    .draggable(
                                        orientation = Orientation.Vertical,
                                        state = sheetState,
                                        onDragStarted = {
                                            sheetState.captureDragStart()
                                        },
                                        onDragStopped = { velocity ->
                                            sheetState.processVelocity(-velocity, autoWrap, onDismiss)
                                        }
                                    )
                                    .pointerInput(autoWrap) {
                                        detectTapGestures(
                                            onTap = {
                                                sheetState.handleTap(autoWrap, onDismiss)
                                            }
                                        )
                                    }
                            }
                        )
                        .padding(horizontal = 24.dp, vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(width = 36.dp, height = 4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(scrollState)
                ) {
                    content(animateDismiss)
                }
            }
        }
        }
    }
}

// Controller managing bottom sheet position and anchors
@Stable
class SequentialSheetState(
    private val coroutineScope: CoroutineScope,
    private val density: Density,
    private val animatable: Animatable<Dp, AnimationVector1D>
) : DraggableState {

    var closedBound by mutableStateOf(0.dp)
        private set
    var halfBound by mutableStateOf(0.dp)
        private set
    var expandedBound by mutableStateOf(0.dp)
        private set

    var currentAnchor by mutableIntStateOf(0)
        private set

    private var dragStartValue by mutableStateOf(0.dp)

    val value by animatable.asState()

    // Syncs measured bounds without touching currentAnchor or dragStartValue
    fun updateBounds(closed: Dp, half: Dp, expanded: Dp) {
        closedBound = closed
        halfBound = half
        expandedBound = expanded
        animatable.updateBounds(closed, expanded)
    }

    val isAnimating: Boolean
        get() = animatable.isRunning

    val currentState by derivedStateOf {
        when (currentAnchor) {
            0 -> SheetStateValue.Closed
            1 -> SheetStateValue.Half
            else -> SheetStateValue.Expanded
        }
    }

    private val internalDraggableState = DraggableState { delta ->
        val newValue = (animatable.value - with(density) { delta.toDp() })
            .coerceIn(closedBound, expandedBound)
        coroutineScope.launch { animatable.snapTo(newValue) }
    }

    fun captureDragStart() {
        dragStartValue = animatable.value
    }

    override suspend fun drag(dragPriority: MutatePriority, block: suspend DragScope.() -> Unit) {
        internalDraggableState.drag(dragPriority, block)
    }

    override fun dispatchRawDelta(delta: Float) {
        val newValue = (animatable.value - with(density) { delta.toDp() })
            .coerceIn(closedBound, expandedBound)
        coroutineScope.launch { animatable.snapTo(newValue) }
    }

    // Smooth animation to specified anchor
    fun animateToTarget(target: SheetStateValue, onDismiss: (() -> Unit)? = null) {
        val destination = when (target) {
            SheetStateValue.Closed -> { currentAnchor = 0; closedBound }
            SheetStateValue.Half -> { currentAnchor = 1; halfBound }
            SheetStateValue.Expanded -> { currentAnchor = 2; expandedBound }
        }
        coroutineScope.launch {
            animatable.animateTo(
                targetValue = destination,
                animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = 110f)
            )
            if (target == SheetStateValue.Closed) onDismiss?.invoke()
        }
    }

    // Velocity driven snapping strategy
    fun processVelocity(velocity: Float, autoWrap: Boolean, onDismiss: () -> Unit) {
        val threshold = 600f
        val autoWrapDismissThreshold = expandedBound * 0.7f
        val dismissThreshold = expandedBound * 0.4f

        if (autoWrap) {
            if (velocity < -threshold || value < autoWrapDismissThreshold) {
                animateToTarget(SheetStateValue.Closed, onDismiss)
            } else {
                animateToTarget(SheetStateValue.Expanded)
            }
            return
        }

        if (velocity < -threshold || value < dismissThreshold) {
            if (dragStartValue > halfBound && value >= halfBound && value > dismissThreshold) {
                animateToTarget(SheetStateValue.Half)
            } else {
                animateToTarget(SheetStateValue.Closed, onDismiss)
            }
            return
        }

        if (velocity > threshold) {
            if (dragStartValue <= halfBound) {
                animateToTarget(SheetStateValue.Half)
            } else {
                animateToTarget(SheetStateValue.Expanded)
            }
            return
        }

        evaluateNearestAnchor(onDismiss, dismissThreshold)
    }

    // Static fallback position snapping
    private fun evaluateNearestAnchor(onDismiss: () -> Unit, dismissThreshold: Dp) {
        if (value < dismissThreshold) {
            animateToTarget(SheetStateValue.Closed, onDismiss)
            return
        }

        val midHalfExpanded = halfBound + (expandedBound - halfBound) / 2

        when {
            value > midHalfExpanded -> animateToTarget(SheetStateValue.Expanded)
            else -> animateToTarget(SheetStateValue.Half)
        }
    }

    // Tap-to-toggle behavior for the drag handle
    fun handleTap(autoWrap: Boolean, onDismiss: () -> Unit) {
        when {
            autoWrap -> animateToTarget(SheetStateValue.Closed, onDismiss)
            currentState == SheetStateValue.Half -> animateToTarget(SheetStateValue.Expanded)
            currentState == SheetStateValue.Expanded -> animateToTarget(SheetStateValue.Half)
            else -> Unit
        }
    }
}

// Memory hook preserving bottom sheet state
@Composable
fun rememberSequentialSheetState(coroutineScope: CoroutineScope): SequentialSheetState {
    val density = LocalDensity.current
    return remember {
        SequentialSheetState(
            coroutineScope = coroutineScope,
            density = density,
            animatable = Animatable(0.dp, Dp.VectorConverter)
        )
    }
}

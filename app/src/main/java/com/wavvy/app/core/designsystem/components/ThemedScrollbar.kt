package com.wavvy.app.core.designsystem.components

// Compose animation
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
// Compose gestures
import androidx.compose.foundation.gestures.detectDragGestures
// Compose layouts and foundations
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
// Compose icons
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
// Material 3 components
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
// Compose state and lifecycle hooks
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
// UI styling and utilities
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

// Track dimensions
private val SCROLLBAR_WIDTH = 4.dp
private val SCROLLBAR_TOUCH_WIDTH = 20.dp
private val SCROLLBAR_MIN_THUMB_HEIGHT = 32.dp
private val ARROW_ICON_SIZE = 14.dp

// Scroll metrics
private data class ScrollMetrics(
    val thumbFraction: Float,
    val scrolledFraction: Float,
    val maxScrollPx: Float
)

// Metrics calculation
private fun scrollMetricsOf(state: LazyListState): ScrollMetrics? {
    val layoutInfo = state.layoutInfo
    val visibleItems = layoutInfo.visibleItemsInfo
    if (visibleItems.isEmpty() || layoutInfo.totalItemsCount == 0) return null

    val averageItemSize = visibleItems.sumOf { it.size }.toFloat() / visibleItems.size
    val spacing = layoutInfo.mainAxisItemSpacing.toFloat()
    val itemSpan = averageItemSize + spacing
    val totalContentSize = itemSpan * layoutInfo.totalItemsCount - spacing
    val viewportSize = layoutInfo.viewportSize.height.toFloat()
    if (totalContentSize <= viewportSize) return null

    val scrolledPastPx = itemSpan * state.firstVisibleItemIndex - visibleItems.first().offset
    val maxScrollPx = totalContentSize - viewportSize

    return ScrollMetrics(
        thumbFraction = (viewportSize / totalContentSize).coerceIn(0.05f, 1f),
        scrolledFraction = (scrolledPastPx / maxScrollPx).coerceIn(0f, 1f),
        maxScrollPx = maxScrollPx
    )
}

// Scrollbar UI component
@Composable
fun ThemedScrollbar(
    state: LazyListState,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current

    val metrics by remember { derivedStateOf { scrollMetricsOf(state) } }
    val canScrollUp by remember { derivedStateOf { state.canScrollBackward } }
    val canScrollDown by remember { derivedStateOf { state.canScrollForward } }
    val isScrollable = metrics != null

    var trackHeightPx by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = modifier.width(ARROW_ICON_SIZE),
        contentAlignment = Alignment.TopCenter
    ) {
        // Scroll up indicator
        AnimatedVisibility(
            visible = isScrollable && canScrollUp,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowUp,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                modifier = Modifier.width(ARROW_ICON_SIZE)
            )
        }

        // Scroll down indicator
        AnimatedVisibility(
            visible = isScrollable && canScrollDown,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                modifier = Modifier.width(ARROW_ICON_SIZE)
            )
        }

        // Track and thumb container
        if (metrics != null) {
            val currentMetrics = metrics!!
            val thumbHeightPx = (trackHeightPx * currentMetrics.thumbFraction)
                .coerceAtLeast(with(density) { SCROLLBAR_MIN_THUMB_HEIGHT.toPx() })
            val dragRangePx = (trackHeightPx - thumbHeightPx).coerceAtLeast(0f)
            val thumbOffsetPx = dragRangePx * currentMetrics.scrolledFraction

            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxHeight()
                    .width(SCROLLBAR_TOUCH_WIDTH)
                    .onGloballyPositioned { coordinates ->
                        trackHeightPx = coordinates.size.height.toFloat()
                    },
                contentAlignment = Alignment.TopCenter
            ) {
                // Track background
                Box(
                    modifier = Modifier
                        .width(SCROLLBAR_WIDTH)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(50))
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                )

                // Draggable thumb
                val latestScrollPerDragPx = rememberUpdatedState(
                    if (dragRangePx > 0f) currentMetrics.maxScrollPx / dragRangePx else 0f
                )
                Box(
                    modifier = Modifier
                        .width(SCROLLBAR_TOUCH_WIDTH)
                        .height(with(density) { thumbHeightPx.toDp() })
                        .offset(y = with(density) { thumbOffsetPx.toDp() })
                        .pointerInput(Unit) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                val scrollDelta = dragAmount.y * latestScrollPerDragPx.value
                                coroutineScope.launch { state.scrollBy(scrollDelta) }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .width(SCROLLBAR_WIDTH)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(50))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.6f))
                    )
                }
            }
        }
    }
}

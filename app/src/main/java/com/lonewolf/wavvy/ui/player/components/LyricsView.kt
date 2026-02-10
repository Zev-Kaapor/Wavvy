package com.lonewolf.wavvy.ui.player.components

// Compose foundation and layout
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
// Material 3 components
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
// State and UI tools
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
// Project resources
import com.lonewolf.wavvy.ui.theme.Poppins
import kotlin.math.abs

// Data model for timed lyrics
data class LyricLine(
    val time: Long,
    val text: String
)

// Enum for line visual state
enum class LineState {
    PREVIOUS,
    CURRENT,
    NEXT
}

// Main lyrics list component
@Composable
fun LyricsView(
    lyrics: List<LyricLine>,
    currentTimestamp: Long,
    onLineClick: (Long) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val configuration = LocalConfiguration.current
    val listState = rememberLazyListState()

    // Vertical centering logic
    val screenHeight = configuration.screenHeightDp.dp
    val centerPadding = screenHeight / 2

    // Current active index based on timestamp
    val activeIndex = if (lyrics.isEmpty()) 7
    else lyrics.indexOfLast { it.time <= currentTimestamp }.coerceAtLeast(0)

    // Visual center index based on scroll position
    val visualCenterIndex = remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val viewportCenter = layoutInfo.viewportStartOffset + (layoutInfo.viewportEndOffset - layoutInfo.viewportStartOffset) / 2

            // Center scroll to active line
            layoutInfo.visibleItemsInfo
                .minByOrNull { itemInfo ->
                    abs((itemInfo.offset + itemInfo.size / 2) - viewportCenter)
                }?.index ?: activeIndex
        }
    }

    // Auto-scroll effect when playback updates
    LaunchedEffect(activeIndex) {
        if (lyrics.isNotEmpty() && !listState.isScrollInProgress) {
            listState.animateScrollToItem(activeIndex, scrollOffset = -400)
        }
    }

    // Scrolling lyrics container
    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = centerPadding, bottom = centerPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
        userScrollEnabled = true
    ) {
        if (lyrics.isEmpty()) {
            // Placeholder skeletons
            items(15) { index ->
                val lineState = when {
                    index < visualCenterIndex.value -> LineState.PREVIOUS
                    index == visualCenterIndex.value -> LineState.CURRENT
                    else -> LineState.NEXT
                }

                LyricLineContent(
                    text = null,
                    lineState = lineState,
                    index = index,
                    onClick = {}
                )
            }
        } else {
            // Actual lyrics content
            itemsIndexed(lyrics) { index, line ->
                val lineState = when {
                    index < visualCenterIndex.value -> LineState.PREVIOUS
                    index == visualCenterIndex.value -> LineState.CURRENT
                    else -> LineState.NEXT
                }

                LyricLineContent(
                    text = line.text,
                    lineState = lineState,
                    index = index,
                    onClick = { onLineClick(line.time) }
                )
            }
        }
    }
}

// Individual lyric line or skeleton wrapper
@Composable
private fun LyricLineContent(
    text: String?,
    lineState: LineState,
    index: Int,
    onClick: () -> Unit
) {
    // Dynamic styling values
    val (opacity, scale, fontSize, hasShadow) = when (lineState) {
        LineState.CURRENT -> Quadruple(1f, 1.15f, 28.sp, true)
        LineState.NEXT -> Quadruple(0.5f, 0.95f, 18.sp, false)
        LineState.PREVIOUS -> Quadruple(0.5f, 0.95f, 18.sp, false)
    }

    // Line container with interactions
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = 24.dp,
                vertical = if (lineState == LineState.CURRENT) 20.dp else 12.dp
            )
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .alpha(opacity)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (text != null) {
            // Lyrics text display
            Text(
                text = text,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontFamily = Poppins,
                    fontSize = fontSize,
                    lineHeight = if (lineState == LineState.CURRENT) 36.sp else 24.sp,
                    fontWeight = if (lineState == LineState.CURRENT) FontWeight.Bold else FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    shadow = if (hasShadow) Shadow(
                        color = Color.White.copy(alpha = 0.8f),
                        blurRadius = 30f
                    ) else null
                ),
                color = Color.White
            )
        } else {
            // Loading skeleton state
            LyricSkeleton(lineState, index)
        }
    }
}

// Skeleton loader for lyrics
@Composable
private fun LyricSkeleton(lineState: LineState, index: Int) {
    val baseColor = if (lineState == LineState.CURRENT) Color.White else Color.White.copy(alpha = 0.4f)
    val widthFactor = if (lineState == LineState.CURRENT) 0.7f else 0.5f

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Main skeleton bar
        Box(
            modifier = Modifier
                .fillMaxWidth(if (index % 2 == 0) widthFactor else widthFactor - 0.1f)
                .height(if (lineState == LineState.CURRENT) 26.dp else 20.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(baseColor.copy(alpha = if (lineState == LineState.CURRENT) 0.3f else 0.15f))
        )
        // Secondary skeleton bar for active line
        if (lineState == LineState.CURRENT) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(if (index % 2 == 0) 0.5f else 0.4f)
                    .height(18.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(baseColor.copy(alpha = 0.2f))
            )
        }
    }
}

// Helper container for multiple style properties
private data class Quadruple<A, B, C, D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D
)

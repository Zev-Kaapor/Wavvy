package com.lonewolf.wavvy.ui.player.components

// Compose foundation and layout
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
// Material 3 components
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
// State and UI tools
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
// Project resources
import com.lonewolf.wavvy.ui.theme.Poppins

// Data model for timed lyrics
data class LyricLine(
    val time: Long,
    val text: String
)

// Dynamic lyrics display with auto-scroll and focus
@Composable
fun LyricsView(
    lyrics: List<LyricLine>,
    currentTimestamp: Long,
    modifier: Modifier = Modifier
) {
    // List state for controlling scroll position
    val listState = rememberLazyListState()

    // Active line detection
    val activeIndex = lyrics.indexOfLast { it.time <= currentTimestamp }.coerceAtLeast(0)

    // Center active lyric line
    LaunchedEffect(activeIndex) {
        if (lyrics.isNotEmpty()) {
            listState.animateScrollToItem(activeIndex, scrollOffset = -300)
        }
    }

    // Main lyrics container
    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 400.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        userScrollEnabled = false
    ) {
        itemsIndexed(lyrics) { index, line ->
            val isActive = index == activeIndex

            // Dynamic typography values
            val fontSize = if (isActive) 28.sp else 22.sp
            val opacity = if (isActive) 1f else 0.3f
            val fontWeight = if (isActive) FontWeight.Bold else FontWeight.SemiBold
            val scale = if (isActive) 1.05f else 0.95f

            // Individual lyric line
            Text(
                text = line.text,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontFamily = Poppins,
                    fontSize = fontSize,
                    lineHeight = 34.sp,
                    fontWeight = fontWeight,
                    textAlign = TextAlign.Start
                ),
                color = if (isActive) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp, vertical = 16.dp)
                    .alpha(opacity)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    }
            )
        }
    }
}

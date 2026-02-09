package com.lonewolf.wavvy.ui.player.components

// Compose foundation and layout
import androidx.compose.foundation.background
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

// Data model for timed lyrics
data class LyricLine(
    val time: Long,
    val text: String
)

@Composable
fun LyricsView(
    lyrics: List<LyricLine>,
    currentTimestamp: Long,
    modifier: Modifier = Modifier
) {
    val configuration = LocalConfiguration.current
    val listState = rememberLazyListState()

    // Vertical centering logic
    val screenHeight = configuration.screenHeightDp.dp
    val centerPadding = screenHeight / 2

    val activeIndex = if (lyrics.isEmpty()) 0 else lyrics.indexOfLast { it.time <= currentTimestamp }.coerceAtLeast(0)

    // Center scroll to active line
    LaunchedEffect(activeIndex) {
        if (lyrics.isNotEmpty()) {
            // Negative offset to pull the item to the visual center
            listState.animateScrollToItem(activeIndex, scrollOffset = -400)
        }
    }

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = centerPadding, bottom = centerPadding),
        // Centraliza os itens horizontalmente na coluna
        horizontalAlignment = Alignment.CenterHorizontally,
        userScrollEnabled = false
    ) {
        if (lyrics.isEmpty()) {
            items(15) { index ->
                LyricLineContent(text = null, isActive = index == activeIndex, index = index)
            }
        } else {
            itemsIndexed(lyrics) { index, line ->
                LyricLineContent(text = line.text, isActive = index == activeIndex, index = index)
            }
        }
    }
}

@Composable
private fun LyricLineContent(
    text: String?,
    isActive: Boolean,
    index: Int
) {
    // Dynamic values for focus effect
    val opacity = if (isActive) 1f else 0.3f
    val scale = if (isActive) 1.1f else 1f
    val fontSize = if (isActive) 26.sp else 20.sp

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .alpha(opacity),
        // Centraliza o texto e os skeletons dentro de cada linha
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (text != null) {
            Text(
                text = text,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontFamily = Poppins,
                    fontSize = fontSize,
                    lineHeight = 34.sp,
                    fontWeight = FontWeight.Bold,
                    // Texto centralizado horizontalmente
                    textAlign = TextAlign.Center,
                    // Glow effect inspired by the screenshot
                    shadow = if (isActive) Shadow(
                        color = Color.White.copy(alpha = 0.8f),
                        blurRadius = 25f
                    ) else null
                ),
                color = Color.White
            )
        } else {
            // Skeleton following center alignment
            LyricSkeleton(isActive, index)
        }
    }
}

@Composable
private fun LyricSkeleton(isActive: Boolean, index: Int) {
    val baseColor = if (isActive) Color.White else Color.White.copy(alpha = 0.4f)

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(if (index % 2 == 0) 0.6f else 0.5f)
                .height(24.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(baseColor.copy(alpha = if (isActive) 0.3f else 0.15f))
        )
        Box(
            modifier = Modifier
                .fillMaxWidth(if (index % 2 == 0) 0.4f else 0.3f)
                .height(16.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(baseColor.copy(alpha = if (isActive) 0.2f else 0.1f))
        )
    }
}

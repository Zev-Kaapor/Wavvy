package com.lonewolf.wavvy.ui.player.components

// Compose animation and core
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
// Material 3 components
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
// UI styling and utilities
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
// Project resources
import com.lonewolf.wavvy.ui.theme.Poppins
import com.lonewolf.wavvy.R
import kotlinx.coroutines.launch

// LRC Parser logic
object LrcParser {
    fun parse(lrc: String): List<Pair<Long, String>> {
        val regex = """\[(\d{2}):(\d{2})\.(\d{2})](.*)""".toRegex()
        return lrc.lines().mapNotNull { line ->
            regex.matchEntire(line.trim())?.let { match ->
                val min = match.groupValues[1].toLongOrNull() ?: 0L
                val sec = match.groupValues[2].toLongOrNull() ?: 0L
                val cent = match.groupValues[3].toLongOrNull() ?: 0L
                val text = match.groupValues[4].trim()
                ((min * 60 * 1000) + (sec * 1000) + (cent * 10)) to text
            }
        }.filter { it.second.isNotBlank() }.sortedBy { it.first }
    }
}

// Alignment options for lyrics
enum class LyricsAlignment {
    LEFT, CENTER, RIGHT;
    val textAlign get() = when(this) {
        LEFT -> TextAlign.Start
        CENTER -> TextAlign.Center
        RIGHT -> TextAlign.End
    }
}

// Main scrolling lyrics container
@Composable
fun LyricsView(
    lyrics: String?,
    isSynced: Boolean = false,
    currentPosition: Long = 0L,
    onSeek: (Long) -> Unit = {},
    modifier: Modifier = Modifier,
    alignment: LyricsAlignment = LyricsAlignment.CENTER,
    currentLineColor: Color = Color.White,
    inactiveLineColor: Color = Color.White.copy(alpha = 0.8f),
    enableInteraction: Boolean = true,
    translation: String? = null
) {
    val config = LocalConfiguration.current
    val density = LocalDensity.current
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val screenHeight = config.screenHeightDp.dp
    val centerOffsetPx = with(density) { (screenHeight / 4).roundToPx() }

    // Lyric data processing
    val parsedLyrics = remember(lyrics, isSynced) {
        when {
            lyrics == null -> null // Loading state
            lyrics.isBlank() -> emptyList() // Not found state
            isSynced -> LrcParser.parse(lyrics)
            else -> lyrics.lines().filter { it.isNotBlank() }.mapIndexed { i, s -> (i * 1000L) to s }
        }
    }

    val parsedTranslation = remember(translation, isSynced) {
        if (translation.isNullOrBlank()) emptyList()
        else if (isSynced) LrcParser.parse(translation)
        else translation.lines().filter { it.isNotBlank() }.mapIndexed { i, s -> (i * 1000L) to s }
    }

    var manualOverrideIndex by remember { mutableStateOf<Int?>(null) }
    var lastKnownPosition by remember { mutableLongStateOf(currentPosition) }
    var lastManualSeekTime by remember { mutableLongStateOf(0L) }

    // Sync position with scroll
    LaunchedEffect(currentPosition) {
        if (!isSynced || parsedLyrics.isNullOrEmpty()) return@LaunchedEffect

        val timeSinceManualSeek = System.currentTimeMillis() - lastManualSeekTime
        val positionDiff = kotlin.math.abs(currentPosition - lastKnownPosition)

        if (positionDiff > 1000 && timeSinceManualSeek > 500) {
            manualOverrideIndex = null
            val newIndex = parsedLyrics.indexOfLast { it.first <= currentPosition }
                .let { if (it == -1) 0 else it }
            listState.scrollToItem(index = newIndex + 1, scrollOffset = -centerOffsetPx)
        } else if (timeSinceManualSeek > 1000 && positionDiff > 100) {
            manualOverrideIndex = null
        }
    }

    val activeIndex by remember(currentPosition, parsedLyrics, manualOverrideIndex) {
        derivedStateOf {
            if (!isSynced || parsedLyrics.isNullOrEmpty()) -1
            else if (manualOverrideIndex != null) manualOverrideIndex!!
            else {
                val idx = parsedLyrics.indexOfLast { it.first <= currentPosition }
                if (idx == -1) 0 else idx
            }
        }
    }

    LaunchedEffect(activeIndex) {
        if (isSynced && activeIndex != -1 && !parsedLyrics.isNullOrEmpty()) {
            listState.animateScrollToItem(index = activeIndex + 1, scrollOffset = -centerOffsetPx)
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        when {
            parsedLyrics == null -> EmptyLyricsPlaceholder()
            parsedLyrics.isEmpty() -> NotFoundLyricsPlaceholder()
            else -> {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = when(alignment) {
                        LyricsAlignment.LEFT -> Alignment.Start
                        LyricsAlignment.CENTER -> Alignment.CenterHorizontally
                        LyricsAlignment.RIGHT -> Alignment.End
                    },
                    userScrollEnabled = enableInteraction
                ) {
                    // Top margin spacer
                    item { Spacer(modifier = Modifier.height(if (isSynced) screenHeight / 4 else 40.dp)) }

                    // Lyric lines list
                    itemsIndexed(
                        items = parsedLyrics,
                        key = { index, item -> "$index-${item.second}" }
                    ) { index, (time, text) ->
                        val isCurrent = index == activeIndex
                        val translationText = parsedTranslation.find { it.first == time }?.second

                        LyricLineItem(
                            text = text,
                            translationText = translationText,
                            isCurrent = isCurrent,
                            isSynced = isSynced,
                            distanceFromActive = if (isSynced) index - activeIndex else 0,
                            currentLineColor = currentLineColor,
                            inactiveLineColor = inactiveLineColor,
                            alignment = alignment,
                            onClick = {
                                if (isSynced) {
                                    onSeek(time)
                                    lastManualSeekTime = System.currentTimeMillis()
                                    manualOverrideIndex = index
                                    scope.launch {
                                        listState.scrollToItem(index = index + 1, scrollOffset = -centerOffsetPx)
                                    }
                                }
                            }
                        )
                    }

                    // Bottom margin spacer
                    item { Spacer(modifier = Modifier.height(if (isSynced) screenHeight * 0.75f else 100.dp)) }
                }
            }
        }
    }
}

// Individual lyric line item
@Composable
private fun LyricLineItem(
    text: String,
    translationText: String?,
    isCurrent: Boolean,
    isSynced: Boolean,
    distanceFromActive: Int,
    currentLineColor: Color,
    inactiveLineColor: Color,
    alignment: LyricsAlignment,
    onClick: () -> Unit
) {
    val animProgress = remember { Animatable(if (isCurrent) 1f else 0f) }

    LaunchedEffect(isCurrent) {
        if (isSynced) {
            animProgress.animateTo(
                targetValue = if (isCurrent) 1f else 0f,
                animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium)
            )
        }
    }

    // Dynamic scale logic
    val wheelScale = if (isSynced) {
        when (kotlin.math.abs(distanceFromActive)) {
            0 -> 1f
            1 -> 0.92f
            else -> 0.85f
        }
    } else 1f

    val currentAlpha = if (isSynced) ((1 - animProgress.value) * 0.45f + animProgress.value * 1f) else 1f
    val currentScale = if (isSynced) ((1 - animProgress.value) * 0.95f + animProgress.value * 1f) else 1f
    val finalScale = currentScale * wheelScale

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = if (isSynced) (4 + (animProgress.value * 4)).dp else 8.dp)
            .graphicsLayer {
                scaleX = finalScale
                scaleY = finalScale
                alpha = currentAlpha
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                enabled = isSynced,
                onClick = onClick
            ),
        horizontalAlignment = when(alignment) {
            LyricsAlignment.LEFT -> Alignment.Start
            LyricsAlignment.CENTER -> Alignment.CenterHorizontally
            LyricsAlignment.RIGHT -> Alignment.End
        }
    ) {
        // Original lyric text
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontFamily = Poppins,
                fontSize = if (isSynced) 20.sp else 18.sp,
                lineHeight = if (isSynced) 20.sp else 24.sp,
                fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Normal,
                textAlign = alignment.textAlign,
                color = if (isCurrent || !isSynced) currentLineColor else inactiveLineColor
            )
        )

        // Translated lyric text
        if (!translationText.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Translate,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = (if (isCurrent || !isSynced) currentLineColor else inactiveLineColor).copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "[$translationText]",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = Poppins,
                        fontSize = 16.sp,
                        lineHeight = 18.sp,
                        fontWeight = FontWeight.Normal,
                        textAlign = alignment.textAlign,
                        color = (if (isCurrent || !isSynced) currentLineColor else inactiveLineColor).copy(alpha = 0.7f)
                    )
                )
            }
        }
    }
}

// State: Not Found
@Composable
private fun NotFoundLyricsPlaceholder() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Empty state icon
        Icon(
            imageVector = Icons.Default.MusicNote,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = Color.White.copy(alpha = 0.4f)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Empty state text
        Text(
            text = stringResource(R.string.lyrics_not_found),
            style = MaterialTheme.typography.bodyLarge.copy(
                fontFamily = Poppins,
                color = Color.White.copy(alpha = 0.6f)
            )
        )
    }
}

// State: Loading (Skeleton)
@Composable
private fun EmptyLyricsPlaceholder() {
    val shimmerColors = listOf(
        Color.White.copy(alpha = 0.15f),
        Color.White.copy(alpha = 0.45f),
        Color.White.copy(alpha = 0.15f),
    )

    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1500f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerTranslate"
    )

    val brush = Brush.linearGradient(
        colors = shimmerColors,
        start = Offset.Zero,
        end = Offset(x = translateAnim, y = translateAnim)
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        userScrollEnabled = false
    ) {
        // Initial spacer
        item { Spacer(modifier = Modifier.height(60.dp)) }

        // Skeleton lines
        items(20) { i ->
            val widthFraction = when (i % 5) {
                0 -> 0.8f 1 -> 0.4f 2 -> 0.6f 3 -> 0.7f else -> 0.5f
            }
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .fillMaxWidth(widthFraction)
                    .height(20.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(brush)
            )
        }

        // Bottom spacer
        item { Spacer(modifier = Modifier.height(100.dp)) }
    }
}

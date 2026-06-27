package com.lonewolf.wavvy.ui.player.components

// Compose animation and core
import android.annotation.SuppressLint
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
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
import kotlinx.coroutines.delay

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
@SuppressLint("ConfigurationScreenWidthHeight")
@Composable
fun LyricsView(
    modifier: Modifier = Modifier,
    lyrics: String?,
    isSynced: Boolean = false,
    currentPosition: Long = 0L,
    onSeek: (Long) -> Unit = {},
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
    val isLandscape = config.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    val centerOffsetPx = with(density) { (if (isLandscape) screenHeight / 2 else screenHeight / 4).roundToPx() }

    val parsedLyrics = remember(lyrics, isSynced) {
        when {
            lyrics == null -> null
            lyrics.isBlank() -> emptyList()
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
    var hasTimedOut by remember { mutableStateOf(false) }

    // Timeout after 15 seconds if lyrics still loading
    LaunchedEffect(lyrics) {
        if (lyrics == null) {
            hasTimedOut = false
            delay(15000)
            if (lyrics == null) {
                hasTimedOut = true
            }
        }
    }

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
            lyrics == null && !hasTimedOut -> EmptyLyricsPlaceholder()
            lyrics == null && hasTimedOut -> NotFoundLyricsPlaceholder()
            parsedLyrics?.isEmpty() != false -> NotFoundLyricsPlaceholder()
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
                    item { Spacer(modifier = Modifier.height(if (isSynced) (if (isLandscape) screenHeight / 2 else screenHeight / 4) else 40.dp)) }

                    // Lyric lines list
                    itemsIndexed(
                        items = parsedLyrics!!,
                        key = { index: Int, item: Pair<Long, String> -> "$index-${item.second}" }
                    ) { index: Int, (time: Long, text: String) ->
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
    val isLandscape = LocalConfiguration.current.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
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
            1 -> if (isLandscape) 0.85f else 0.92f
            else -> if (isLandscape) 0.75f else 0.85f
        }
    } else 1f

    val currentAlpha = if (isSynced) ((1 - animProgress.value) * 0.45f + animProgress.value * 1f) else 1f
    val currentScale = if (isSynced) ((1 - animProgress.value) * 0.95f + animProgress.value * 1f) else 1f
    val finalScale = currentScale * wheelScale

    // Black shadow for readability
    val textShadow = Shadow(
        color = Color.Black.copy(alpha = 0.7f),
        offset = Offset(0f, 2f),
        blurRadius = 15f
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = if (isSynced) {
                if (isLandscape) (8 + (animProgress.value * 8)).dp else (4 + (animProgress.value * 4)).dp
            } else 8.dp)
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
                fontSize = if (isSynced) (if (isLandscape) 28.sp else 20.sp) else 18.sp,
                lineHeight = if (isSynced) (if (isLandscape) 34.sp else 20.sp) else 24.sp,
                fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Normal,
                textAlign = alignment.textAlign,
                color = if (isCurrent || !isSynced) currentLineColor else inactiveLineColor,
                shadow = textShadow
            )
        )

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
                        color = (if (isCurrent || !isSynced) currentLineColor else inactiveLineColor).copy(alpha = 0.7f),
                        shadow = textShadow
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
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Icon with background pill
        Box(
            modifier = Modifier
                .size(80.dp)
                .background(
                    color = Color.White.copy(alpha = 0.08f),
                    shape = RoundedCornerShape(24.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.MusicNote,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = Color.White.copy(alpha = 0.5f)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Title
        Text(
            text = stringResource(R.string.lyrics_not_found),
            style = MaterialTheme.typography.titleMedium.copy(
                fontFamily = Poppins,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            ),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Subtitle
        Text(
            text = stringResource(R.string.lyrics_not_found_subtitle),
            style = MaterialTheme.typography.bodyMedium.copy(
                fontFamily = Poppins,
                color = Color.White.copy(alpha = 0.6f)
            ),
            textAlign = TextAlign.Center
        )
    }
}

// State: Loading (Skeleton)
@Composable
private fun EmptyLyricsPlaceholder() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ThreeDotsLoadingAnimation(
            dotColor = Color.White.copy(alpha = 0.8f),
            dotSize = 14,
            travelDistance = 20f
        )

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = stringResource(R.string.lyrics_loading),
            style = MaterialTheme.typography.bodyMedium.copy(
                fontFamily = Poppins,
                color = Color.White.copy(alpha = 0.6f)
            ),
            textAlign = TextAlign.Center
        )
    }
}

// Three dots bouncing loading animation
@Composable
private fun ThreeDotsLoadingAnimation(
    modifier: Modifier = Modifier,
    dotColor: Color = Color.White,
    dotSize: Int = 12,
    travelDistance: Float = 15f
) {
    // List with animation state for each of the 3 dots
    val dotAnimations = listOf(
        remember { Animatable(0f) },
        remember { Animatable(0f) },
        remember { Animatable(0f) }
    )

    // Trigger cascade animation for each dot
    dotAnimations.forEachIndexed { index, animatable ->
        LaunchedEffect(key1 = animatable) {
            // Create proportional delay for wave/bounce effect
            delay(index * 150L)
            animatable.animateTo(
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 400, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                )
            )
        }
    }

    // Align dots horizontally
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        dotAnimations.forEach { animatable ->
            Box(
                modifier = Modifier
                    .size(dotSize.dp)
                    .graphicsLayer { translationY = -animatable.value * travelDistance }
                    .background(color = dotColor, shape = CircleShape)
            )
        }
    }
}

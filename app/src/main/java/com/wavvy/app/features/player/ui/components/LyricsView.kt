package com.wavvy.app.features.player.ui.components

// Compose animation
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
// Compose layouts and foundations
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
// Material 3 components
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
// Compose state and runtime
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
// UI styling and utilities
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
// Coroutines
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
// Math
import kotlin.math.abs
// Project resources
import com.wavvy.app.R
import com.wavvy.app.core.designsystem.icons.WavvyIcons
import com.wavvy.app.core.designsystem.theme.WavvyTheme
import com.wavvy.app.core.lyrics.LyricWord
import com.wavvy.app.core.lyrics.LyricsAlignment
import com.wavvy.app.core.lyrics.LyricsDisplay
import com.wavvy.app.core.lyrics.LyricsSettings
import com.wavvy.app.core.lyrics.LyricsTextSize
import com.wavvy.app.core.lyrics.LyricsTranslation
import com.wavvy.app.core.lyrics.SongLyrics

// What the lyrics overlay shows, still searching, nothing found, or the lyrics with their translation when asked
sealed interface LyricsState {
    data object Loading : LyricsState
    data object NotFound : LyricsState
    data class Ready(val lyrics: SongLyrics, val translation: LyricsTranslation?) : LyricsState
}

// Scrolling lyrics of the old Wavvy, the line being sung stays near the top third and the others fade and shrink like a wheel
@Composable
fun LyricsView(
    state: LyricsState,
    settings: LyricsSettings,
    positionMs: () -> Long,
    onSeek: (Long) -> Unit,
    isLandscape: Boolean,
    modifier: Modifier = Modifier
) {
    var hasTimedOut by remember { mutableStateOf(false) }

    // A search that takes too long is shown as not found
    LaunchedEffect(state is LyricsState.Loading) {
        hasTimedOut = false
        if (state is LyricsState.Loading) {
            delay(PlayerDimens.LyricsLoadingTimeoutMillis)
            hasTimedOut = true
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        when {
            state is LyricsState.Loading && !hasTimedOut -> LoadingLyrics()
            state is LyricsState.Ready -> LyricsList(state = state, settings = settings, positionMs = positionMs, onSeek = onSeek, isLandscape = isLandscape)
            else -> NotFoundLyrics()
        }
    }
}

// The lines, following the song when they are timed
@Composable
private fun LyricsList(
    state: LyricsState.Ready,
    settings: LyricsSettings,
    positionMs: () -> Long,
    onSeek: (Long) -> Unit,
    isLandscape: Boolean
) {
    val lines = state.lyrics.lines
    // Static mode shows timed lyrics as plain text too
    val isSynced = state.lyrics.isSynced && settings.display == LyricsDisplay.Synced
    val style = LineStyle(
        alignment = settings.alignment,
        sizeScale = when (settings.textSize) {
            LyricsTextSize.Small -> PlayerDimens.LyricSmallScale
            LyricsTextSize.Medium -> 1f
            LyricsTextSize.Large -> PlayerDimens.LyricLargeScale
        },
        wordByWord = settings.wordByWord
    )
    val translated = state.translation?.lines
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val height = maxHeight
        val centerOffset = with(LocalDensity.current) {
            (height * if (isLandscape) PlayerDimens.LyricsCenterFractionLandscape else PlayerDimens.LyricsCenterFraction).roundToPx()
        }

        var manualIndex by remember { mutableStateOf<Int?>(null) }
        var lastSeekAt by remember { mutableLongStateOf(0L) }
        var lastPosition by remember { mutableLongStateOf(positionMs()) }

        // Line being sung, it changes only when the song reaches the next line
        val activeIndex by remember(lines, isSynced) {
            derivedStateOf {
                if (!isSynced) -1 else manualIndex ?: lines.indexOfLast { it.timeMs <= positionMs() }.coerceAtLeast(0)
            }
        }

        // A jump in the song, by the seekbar or another app, moves the list at once
        val position by remember { derivedStateOf { positionMs() } }
        LaunchedEffect(position) {
            if (!isSynced) return@LaunchedEffect
            val sinceSeek = System.currentTimeMillis() - lastSeekAt
            if (abs(position - lastPosition) > PlayerDimens.LyricJumpMillis && sinceSeek > PlayerDimens.LyricSeekGraceMillis) {
                manualIndex = null
                val index = lines.indexOfLast { it.timeMs <= position }.coerceAtLeast(0)
                listState.scrollToItem(index + 1, -centerOffset)
            }
            lastPosition = position
        }

        // Slides by the exact distance to the new line, so the list never passes it and comes back
        LaunchedEffect(activeIndex) {
            if (!isSynced || activeIndex < 0) return@LaunchedEffect
            val target = listState.layoutInfo.visibleItemsInfo.firstOrNull { it.index == activeIndex + 1 }
            if (target == null) {
                listState.scrollToItem(activeIndex + 1, -centerOffset)
            } else {
                listState.animateScrollBy(
                    (target.offset - centerOffset).toFloat(),
                    tween(PlayerDimens.LyricScrollMillis, easing = FastOutSlowInEasing)
                )
            }
        }

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = style.horizontal
        ) {
            item { Spacer(Modifier.height(if (isSynced) height * PlayerDimens.LyricsCenterFraction else PlayerDimens.PlainLyricsTop)) }

            itemsIndexed(lines, key = { index, line -> "$index-${line.timeMs}" }) { index, line ->
                LyricLineItem(
                    text = line.text,
                    words = line.words,
                    positionMs = positionMs,
                    translation = translated?.getOrNull(index)?.takeIf { it.isNotBlank() && it != line.text },
                    isCurrent = index == activeIndex,
                    isSynced = isSynced,
                    distanceFromActive = if (isSynced) index - activeIndex else 0,
                    isLandscape = isLandscape,
                    style = style,
                    onClick = {
                        if (isSynced) {
                            onSeek(line.timeMs)
                            lastSeekAt = System.currentTimeMillis()
                            manualIndex = index
                            scope.launch { listState.scrollToItem(index + 1, -centerOffset) }
                        }
                    }
                )
            }

            item { Spacer(Modifier.height(if (isSynced) height * PlayerDimens.LyricsEndFraction else PlayerDimens.PlainLyricsBottom)) }
        }

        // The tap on a line holds it until the song catches up with it
        LaunchedEffect(manualIndex) {
            val held = manualIndex ?: return@LaunchedEffect
            delay(PlayerDimens.LyricJumpMillis)
            if (manualIndex == held) manualIndex = null
        }
    }
}

// How the lines are laid out, from the lyrics options
private class LineStyle(
    alignment: LyricsAlignment,
    val sizeScale: Float,
    val wordByWord: Boolean
) {
    val horizontal: Alignment.Horizontal = when (alignment) {
        LyricsAlignment.Start -> Alignment.Start
        LyricsAlignment.Center -> Alignment.CenterHorizontally
        LyricsAlignment.End -> Alignment.End
    }
    val textAlign: TextAlign = when (alignment) {
        LyricsAlignment.Start -> TextAlign.Start
        LyricsAlignment.Center -> TextAlign.Center
        LyricsAlignment.End -> TextAlign.End
    }
    val origin: TransformOrigin = when (alignment) {
        LyricsAlignment.Start -> TransformOrigin(0f, 0.5f)
        LyricsAlignment.Center -> TransformOrigin.Center
        LyricsAlignment.End -> TransformOrigin(1f, 0.5f)
    }
}

// One line, growing and brightening when it is sung, with its translation under it
@Composable
private fun LyricLineItem(
    text: String,
    words: List<LyricWord>,
    positionMs: () -> Long,
    translation: String?,
    isCurrent: Boolean,
    isSynced: Boolean,
    distanceFromActive: Int,
    isLandscape: Boolean,
    style: LineStyle,
    onClick: () -> Unit
) {
    val colors = WavvyTheme.colors
    val activeColor = colors.onMedia
    val otherColor = colors.onMedia.copy(alpha = PlayerDimens.LyricOtherLineAlpha)
    val emphasis = remember { Animatable(if (isCurrent) 1f else 0f) }

    LaunchedEffect(isCurrent) {
        // No bounce, so the line does not swing while the list slides to it
        if (isSynced) emphasis.animateTo(if (isCurrent) 1f else 0f, spring(Spring.DampingRatioNoBouncy, Spring.StiffnessMedium))
    }

    // Lines farther from the sung one are smaller, like on a wheel
    val wheelScale = if (!isSynced) {
        1f
    } else {
        when (abs(distanceFromActive)) {
            0 -> 1f
            1 -> if (isLandscape) PlayerDimens.LyricNearScaleLandscape else PlayerDimens.LyricNearScale
            else -> if (isLandscape) PlayerDimens.LyricFarScaleLandscape else PlayerDimens.LyricFarScale
        }
    }
    val lineColor = if (isCurrent || !isSynced) activeColor else otherColor
    val shadow = Shadow(
        color = colors.textShadow.copy(alpha = PlayerDimens.LyricShadowAlpha),
        offset = Offset(0f, PlayerDimens.LyricShadowOffsetY),
        blurRadius = PlayerDimens.LyricShadowBlur
    )
    // The space between lines stays the same, the sung line stands out by its size only, so the list keeps its layout while it slides
    val gap = when {
        !isSynced -> PlayerDimens.PlainLyricGap
        isLandscape -> PlayerDimens.LyricGapLandscape
        else -> PlayerDimens.LyricGap
    }

    Column(
        horizontalAlignment = style.horizontal,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = PlayerDimens.LyricSide, vertical = gap)
            .graphicsLayer {
                // Lines shrink toward their own side, so they stay lined up
                transformOrigin = style.origin
                val amount = if (isSynced) emphasis.value else 1f
                val scale = (PlayerDimens.LyricInactiveScale + (1f - PlayerDimens.LyricInactiveScale) * amount) * wheelScale
                scaleX = scale
                scaleY = scale
                alpha = PlayerDimens.LyricInactiveAlpha + (1f - PlayerDimens.LyricInactiveAlpha) * amount
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                enabled = isSynced,
                onClick = onClick
            )
    ) {
        Text(
            text = if (isCurrent && isSynced && style.wordByWord && words.isNotEmpty()) {
                karaokeText(words, positionMs, activeColor)
            } else {
                AnnotatedString(text)
            },
            style = MaterialTheme.typography.bodyLarge.copy(
                fontSize = when {
                    !isSynced -> PlayerDimens.PlainLyricSize
                    isLandscape -> PlayerDimens.LyricSizeLandscape
                    else -> PlayerDimens.LyricSize
                } * style.sizeScale,
                lineHeight = when {
                    !isSynced -> PlayerDimens.PlainLyricLineHeight
                    isLandscape -> PlayerDimens.LyricLineHeightLandscape
                    else -> PlayerDimens.LyricLineHeight
                } * style.sizeScale,
                fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Normal,
                textAlign = style.textAlign,
                color = lineColor,
                shadow = shadow
            )
        )

        if (translation != null) {
            Spacer(Modifier.height(PlayerDimens.TranslationGap))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = WavvyIcons.Translate,
                    contentDescription = null,
                    tint = lineColor.copy(alpha = lineColor.alpha * PlayerDimens.TranslationAlpha),
                    modifier = Modifier.size(PlayerDimens.TranslationIcon)
                )
                Spacer(Modifier.width(PlayerDimens.TranslationIconGap))
                Text(
                    text = "[$translation]",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = PlayerDimens.TranslationSize * style.sizeScale,
                        lineHeight = PlayerDimens.TranslationLineHeight * style.sizeScale,
                        textAlign = style.textAlign,
                        color = lineColor.copy(alpha = lineColor.alpha * PlayerDimens.TranslationAlpha),
                        shadow = shadow
                    )
                )
            }
        }
    }
}

// The sung line word by word, the words already sung are bright, the one being sung brightens as it goes and the next ones wait faint
@Composable
private fun karaokeText(words: List<LyricWord>, positionMs: () -> Long, color: Color): AnnotatedString {
    val now by remember(words) { derivedStateOf { positionMs() } }
    val waiting = color.copy(alpha = color.alpha * PlayerDimens.UnsungWordAlpha)

    return buildAnnotatedString {
        words.forEachIndexed { index, word ->
            val sung = when {
                now >= word.endMs -> 1f
                now <= word.startMs -> 0f
                else -> (now - word.startMs).toFloat() / (word.endMs - word.startMs).coerceAtLeast(1)
            }
            withStyle(SpanStyle(color = lerp(waiting, color, sung))) { append(word.text) }
            if (index < words.lastIndex && !word.text.endsWith('-')) append(" ")
        }
    }
}

// No lyrics for this song
@Composable
private fun NotFoundLyrics() {
    val onMedia = WavvyTheme.colors.onMedia

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(PlayerDimens.LyricsStatePadding),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(PlayerDimens.NotFoundBox)
                .background(onMedia.copy(alpha = PlayerDimens.NotFoundBoxAlpha), RoundedCornerShape(PlayerDimens.NotFoundCorner)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = WavvyIcons.MusicNote,
                contentDescription = null,
                modifier = Modifier.size(PlayerDimens.NotFoundIcon),
                tint = onMedia.copy(alpha = PlayerDimens.NotFoundIconAlpha)
            )
        }

        Spacer(Modifier.height(PlayerDimens.NotFoundGap))

        Text(
            text = stringResource(R.string.lyrics_not_found),
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold, color = onMedia),
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(PlayerDimens.NotFoundTextGap))

        Text(
            text = stringResource(R.string.lyrics_not_found_subtitle),
            style = MaterialTheme.typography.bodyMedium.copy(color = onMedia.copy(alpha = PlayerDimens.LyricsHintAlpha)),
            textAlign = TextAlign.Center
        )
    }
}

// Three dots jumping one after the other while the lyrics are searched
@Composable
private fun LoadingLyrics() {
    val onMedia = WavvyTheme.colors.onMedia

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(PlayerDimens.LyricsStatePadding),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(PlayerDimens.LoadingDotGap)) {
            repeat(PlayerDimens.LoadingDotCount) { index ->
                val jump = remember { Animatable(0f) }
                LaunchedEffect(Unit) {
                    delay(index * PlayerDimens.LoadingDotDelayMillis)
                    jump.animateTo(
                        targetValue = 1f,
                        animationSpec = infiniteRepeatable(tween(PlayerDimens.LoadingDotMillis, easing = FastOutSlowInEasing), RepeatMode.Reverse)
                    )
                }
                Box(
                    modifier = Modifier
                        .size(PlayerDimens.LoadingDot)
                        .graphicsLayer { translationY = -jump.value * PlayerDimens.LoadingDotTravelPx }
                        .background(onMedia.copy(alpha = PlayerDimens.LoadingDotAlpha), CircleShape)
                )
            }
        }

        Spacer(Modifier.height(PlayerDimens.LoadingTextGap))

        Text(
            text = stringResource(R.string.lyrics_loading),
            style = MaterialTheme.typography.bodyMedium.copy(color = onMedia.copy(alpha = PlayerDimens.LyricsHintAlpha)),
            textAlign = TextAlign.Center
        )
    }
}

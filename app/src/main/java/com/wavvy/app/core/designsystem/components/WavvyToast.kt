package com.wavvy.app.core.designsystem.components

// Compose animations and specifications
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
// Compose layouts and foundations
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
// Material 3 components
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
// Compose state and runtime
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
// UI styling and utilities
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
// Coroutines and reactive flows
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
// Project resources
import com.wavvy.app.core.designsystem.icons.WavvyIcons
import com.wavvy.app.core.designsystem.theme.WavvyTheme

// What a notice says, a failure has its own color and icon, and the id tells one notice from the one that comes after it
data class ToastData(
    val message: String,
    val subtitle: String? = null,
    val isError: Boolean = false,
    val id: Long = System.nanoTime()
)

// The notices of the app, any place shows one with a single call and they are drawn over the whole screen by the host
object Toasts {
    private val mutableCurrent = MutableStateFlow<ToastData?>(null)
    val current: StateFlow<ToastData?> = mutableCurrent.asStateFlow()

    // Shows a notice in place of the one that is on, from any thread
    fun show(message: String, subtitle: String? = null, isError: Boolean = false) {
        mutableCurrent.value = ToastData(message, subtitle, isError)
    }

    // Takes a notice away, only when it is still the one on screen
    fun dismiss(id: Long) {
        mutableCurrent.update { if (it?.id == id) null else it }
    }
}

// Draws the notice as an island at the top, a small black pill that comes down, opens to show what happened, closes back to the pill and goes up
// A notice that comes while another is on takes its place inside the same island, a tap or a swipe up closes it
@Composable
fun ToastHost(modifier: Modifier = Modifier) {
    val toast by Toasts.current.collectAsState()

    // What the island holds, which stays after the notice goes so it can still be drawn while it closes, and whether it is on the screen and whether it is open
    var shown by remember { mutableStateOf<ToastData?>(null) }
    var isVisible by remember { mutableStateOf(false) }
    var isOpen by remember { mutableStateOf(false) }

    LaunchedEffect(toast?.id) {
        val next = toast
        if (next == null) {
            // The island closes to the pill and then leaves
            isOpen = false
            delay(IslandCloseMillis)
            if (Toasts.current.value == null) isVisible = false
            return@LaunchedEffect
        }

        shown = next
        if (!isVisible) {
            // It comes as the small pill and opens a moment after
            isVisible = true
            isOpen = false
            delay(IslandPeekMillis)
        }
        isOpen = true
        delay(ToastMillis.toLong())
        Toasts.dismiss(next.id)
    }

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val openWidth = maxWidth * ToastWidthFraction

        AnimatedVisibility(
            visible = isVisible,
            enter = slideInVertically(tween(IslandSlideMillis)) { -it * 2 } + fadeIn(tween(IslandSlideMillis)),
            exit = slideOutVertically(tween(IslandSlideMillis)) { -it * 2 } + fadeOut(tween(IslandSlideMillis)),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top))
                .padding(top = ToastTop)
        ) {
            shown?.let { data -> Island(data = data, isOpen = isOpen, openWidth = openWidth, onDismiss = { Toasts.dismiss(data.id) }) }
        }
    }
}

// The island, the round mark of what happened that stays and the words that appear when it opens
@Composable
private fun Island(data: ToastData, isOpen: Boolean, openWidth: Dp, onDismiss: () -> Unit) {
    val accent = if (data.isError) MaterialTheme.colorScheme.error else WavvyTheme.colors.chartUp
    // How wide the words are, as the widest of their lines once they have wrapped in the room the island has at most, so it opens to the size of what it says
    val density = LocalDensity.current
    val measurer = rememberTextMeasurer()
    val messageStyle = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
    val subtitleStyle = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold)
    val maxTextWidth = openWidth - ToastPaddingX * 2 - ToastBadge - ToastGap
    val textWidth = remember(data.message, data.subtitle, maxTextWidth, messageStyle, subtitleStyle) {
        val limit = Constraints(maxWidth = with(density) { maxTextWidth.roundToPx() })
        val message = measurer.measure(AnnotatedString(data.message), messageStyle, TextOverflow.Ellipsis, true, 2, constraints = limit).size.width
        val subtitle = data.subtitle?.let { measurer.measure(AnnotatedString(it), subtitleStyle, TextOverflow.Ellipsis, true, 3, constraints = limit).size.width } ?: 0
        with(density) { (maxOf(message, subtitle) + 1).toDp() }
    }
    val fittedWidth = (ToastPaddingX * 2 + ToastBadge + ToastGap + textWidth).coerceIn(ToastOpenMinWidth, openWidth)

    val width by animateDpAsState(
        targetValue = if (isOpen) fittedWidth else ToastClosedWidth,
        animationSpec = spring(dampingRatio = ToastBounce, stiffness = Spring.StiffnessMediumLow),
        label = "IslandWidth"
    )
    // The mark is in the middle of the dot while it is closed and goes to the start as it opens, it never gets narrower than it is so it does not turn oval
    val markStart by animateDpAsState(
        targetValue = if (isOpen) ToastPaddingX else (ToastClosedWidth - ToastBadge) / 2,
        animationSpec = spring(dampingRatio = ToastBounce, stiffness = Spring.StiffnessMediumLow),
        label = "IslandMark"
    )
    val markAlpha by animateFloatAsState(
        targetValue = if (isOpen) 1f else 0f,
        animationSpec = tween(ToastMarkMillis, delayMillis = if (isOpen) ToastTextDelayMillis / 2 else 0),
        label = "IslandMarkAlpha"
    )
    val swipe = with(LocalDensity.current) { ToastSwipe.toPx() }
    var drag by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = Modifier
            .graphicsLayer { translationY = drag }
            .width(width)
            .animateContentSize(spring(dampingRatio = ToastBounce, stiffness = Spring.StiffnessMediumLow))
            .heightIn(min = ToastClosedHeight)
            .clip(RoundedCornerShape(ToastCorner))
            .background(ToastContainer)
            .border(ToastBorder, ToastContent.copy(alpha = ToastBorderAlpha), RoundedCornerShape(ToastCorner))
            .pointerInput(data.id) {
                detectVerticalDragGestures(
                    onDragEnd = {
                        if (drag < -swipe) onDismiss()
                        drag = 0f
                    },
                    onDragCancel = { drag = 0f },
                    onVerticalDrag = { _, amount -> drag = (drag + amount).coerceAtMost(0f) }
                )
            }
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDismiss)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(start = markStart, end = ToastPaddingX, top = ToastPaddingY, bottom = ToastPaddingY)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .requiredSize(ToastBadge)
                    .graphicsLayer { alpha = markAlpha }
                    .clip(CircleShape)
                    .background(accent.copy(alpha = ToastBadgeAlpha))
                    .border(ToastBorder, accent.copy(alpha = ToastBadgeBorderAlpha), CircleShape)
            ) {
                // The mark changes with a fade when a notice takes the place of another
                AnimatedContent(
                    targetState = data.isError,
                    transitionSpec = { fadeIn(tween(ToastMarkMillis)) togetherWith fadeOut(tween(ToastMarkMillis)) },
                    label = "IslandMark"
                ) { isError ->
                    Icon(
                        imageVector = if (isError) WavvyIcons.Close else WavvyIcons.Check,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(ToastBadgeIcon)
                    )
                }
            }

            // The words come in once the island has begun to open and leave first when it closes
            AnimatedVisibility(
                visible = isOpen,
                enter = fadeIn(tween(ToastTextMillis, delayMillis = ToastTextDelayMillis)),
                exit = fadeOut(tween(ToastTextExitMillis))
            ) {
                Row {
                    Spacer(modifier = Modifier.width(ToastGap))
                    Column(modifier = Modifier.requiredWidth(textWidth)) {
                        AnimatedContent(
                            targetState = data.message to data.subtitle,
                            transitionSpec = { fadeIn(tween(ToastMarkMillis)) togetherWith fadeOut(tween(ToastMarkMillis)) },
                            label = "IslandText"
                        ) { (message, subtitle) ->
                            Column {
                                Text(
                                    text = message,
                                    style = messageStyle,
                                    color = ToastContent,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                subtitle?.let {
                                    Text(
                                        text = it,
                                        style = subtitleStyle,
                                        color = ToastContent.copy(alpha = ToastSubtitleAlpha),
                                        maxLines = 3,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.padding(top = ToastSubtitleTop)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// How long it stays open, how long it waits as the pill before it opens, how long it takes to close, to come down and go up, and how its parts fade
private const val ToastMillis = 3000
private const val IslandPeekMillis = 220L
private const val IslandCloseMillis = 320L
private const val IslandSlideMillis = 320
private const val ToastMarkMillis = 180
private const val ToastTextMillis = 220
private const val ToastTextDelayMillis = 120
private const val ToastTextExitMillis = 90

// How it is built, the width it opens to, how it springs, the black it is drawn in, the room around it and inside it, and the alphas of its parts
private const val ToastWidthFraction = 0.92f
private const val ToastBounce = 0.72f
private val ToastContainer = Color(0xFF000000)
private val ToastContent = Color(0xFFFFFFFF)
private val ToastTop = 10.dp
private val ToastClosedWidth = 44.dp
private val ToastOpenMinWidth = 150.dp
private val ToastClosedHeight = 44.dp
private val ToastCorner = 30.dp
private val ToastBorder = 0.5.dp
private val ToastPaddingX = 12.dp
private val ToastPaddingY = 8.dp
private val ToastBadge = 28.dp
private val ToastBadgeIcon = 16.dp
private val ToastGap = 12.dp
private val ToastSubtitleTop = 2.dp
private val ToastSwipe = 36.dp
private const val ToastBorderAlpha = 0.16f
private const val ToastBadgeAlpha = 0.16f
private const val ToastBadgeBorderAlpha = 0.24f
private const val ToastSubtitleAlpha = 0.62f

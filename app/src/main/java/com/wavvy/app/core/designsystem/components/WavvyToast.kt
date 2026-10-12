package com.wavvy.app.core.designsystem.components

// Compose animations and specifications
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
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
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.runtime.remember
// UI styling and utilities
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
// Coroutines and reactive flows
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

// Draws the notice at the bottom, over the bar and the mini player, sliding in and out
@Composable
fun ToastHost(bottom: Dp, modifier: Modifier = Modifier) {
    val toast by Toasts.current.collectAsState()

    AnimatedContent(
        targetState = toast,
        transitionSpec = {
            (slideInVertically(tween(ToastSlideMillis)) { it } + fadeIn(tween(ToastSlideMillis)))
                .togetherWith(slideOutVertically(tween(ToastSlideMillis)) { it } + fadeOut(tween(ToastSlideMillis)))
        },
        contentKey = { it?.id },
        label = "Toast",
        modifier = modifier.padding(bottom = bottom)
    ) { data ->
        if (data != null) WavvyToast(data = data, onDismiss = { Toasts.dismiss(data.id) })
    }
}

// The notice, a round mark of the color of what happened, the words and a line at the bottom that runs out as the time of it does
// It is the one of the first Wavvy, drawn again with the tokens of the theme
@Composable
private fun WavvyToast(data: ToastData, onDismiss: () -> Unit) {
    val progress = remember(data.id) { Animatable(1f) }
    val accent = if (data.isError) MaterialTheme.colorScheme.error else WavvyTheme.colors.chartUp
    val pill = RoundedCornerShape(ToastCorner)
    val gradient = remember(accent) { Brush.horizontalGradient(listOf(accent, accent.copy(alpha = ToastGradientEndAlpha))) }

    // The line runs out, and the notice goes with it
    LaunchedEffect(data.id) {
        progress.animateTo(0f, tween(ToastMillis, easing = LinearEasing))
        onDismiss()
    }

    Column(
        modifier = Modifier
            .fillMaxWidth(ToastWidthFraction)
            .heightIn(min = ToastMinHeight)
            .clip(pill)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = ToastAlpha))
            .border(ToastBorder, MaterialTheme.colorScheme.onSurface.copy(alpha = ToastBorderAlpha), pill)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDismiss)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(horizontal = ToastPaddingX, vertical = ToastPaddingY)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(ToastBadge)
                    .clip(CircleShape)
                    .background(accent.copy(alpha = ToastBadgeAlpha))
                    .border(ToastBorder, accent.copy(alpha = ToastBadgeBorderAlpha), CircleShape)
            ) {
                Icon(
                    imageVector = if (data.isError) WavvyIcons.Close else WavvyIcons.Check,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(ToastBadgeIcon)
                )
            }

            Spacer(modifier = Modifier.width(ToastGap))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = data.message,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                data.subtitle?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = ToastSubtitleAlpha),
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = ToastSubtitleTop)
                    )
                }
            }
        }

        Box(modifier = Modifier.fillMaxWidth().height(ToastProgress).background(MaterialTheme.colorScheme.onSurface.copy(alpha = ToastTrackAlpha))) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress.value)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(bottomStart = ToastCorner, bottomEnd = ToastCorner))
                    .background(gradient)
            )
        }
    }
}

// How long it stays and how long it takes to slide, and how it is built, the width, the corners, the room inside and the alphas of its parts
private const val ToastMillis = 3000
private const val ToastSlideMillis = 300
private const val ToastWidthFraction = 0.92f
private val ToastMinHeight = 64.dp
private val ToastCorner = 32.dp
private val ToastBorder = 0.5.dp
private val ToastPaddingX = 18.dp
private val ToastPaddingY = 12.dp
private val ToastBadge = 38.dp
private val ToastBadgeIcon = 18.dp
private val ToastGap = 14.dp
private val ToastSubtitleTop = 2.dp
private val ToastProgress = 3.dp
private const val ToastAlpha = 0.92f
private const val ToastBorderAlpha = 0.23f
private const val ToastBadgeAlpha = 0.12f
private const val ToastBadgeBorderAlpha = 0.20f
private const val ToastSubtitleAlpha = 0.6f
private const val ToastTrackAlpha = 0.04f
private const val ToastGradientEndAlpha = 0.7f

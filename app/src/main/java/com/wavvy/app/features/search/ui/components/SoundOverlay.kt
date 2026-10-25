package com.wavvy.app.features.search.ui.components

// Android back
import androidx.activity.compose.BackHandler
// Compose animation
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
// Compose layouts and foundations
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
// Material 3 components
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
// Compose state and runtime
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
// UI styling and utilities
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import kotlin.math.PI
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.sin
// Project resources
import com.wavvy.app.R
import com.wavvy.app.core.designsystem.icons.WavvyIcons
import com.wavvy.app.core.designsystem.theme.WavvyMotion
import com.wavvy.app.core.designsystem.theme.WavvyTheme
import com.wavvy.app.features.search.ui.SearchDimens

// The search by sound over the other apps when it has to be an activity, the light of the bottom edge and the pill, in a window as tall as they need
// The windows of the service are the better way, this one is for a phone that did not let the service start
@Composable
internal fun SoundOverlay(search: SoundSearch, onClose: () -> Unit) {
    val navigation = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    // Only as tall as the light and the pill, so the window leaves the rest of the screen to the app under it, and the light goes down under the bar of navigation
    Box(modifier = Modifier.fillMaxWidth().height(SearchDimens.SoundOverlayHeight + navigation)) {
        SoundGlow(search = search, modifier = Modifier.fillMaxSize())
        SoundPill(search = search, onClose = onClose, modifier = Modifier.align(Alignment.BottomCenter))
    }
}

// The search by sound inside the app, the same light and pill as over the other apps, drawn over the whole app and leaving its touches to it
// It goes with a fade when it is closed, when the back is pressed, when the song is found, or a moment after nothing was found
@Composable
fun SoundSearchOverlay(onFound: (String) -> Unit, onDismiss: () -> Unit) {
    val scope = rememberCoroutineScope()
    var isShown by remember { mutableStateOf(true) }
    val leave = {
        if (isShown) {
            isShown = false
            scope.launch {
                delay(WavvyMotion.SoundShowMillis.toLong())
                onDismiss()
            }
        }
    }
    val search = rememberSoundSearch(onFound = { query ->
        leave()
        onFound(query)
    })

    BackHandler(enabled = isShown, onBack = leave)
    LaunchedEffect(search.phase) {
        if (search.phase is SoundPhase.Failed) {
            delay(WavvyMotion.SoundFailedHoldMillis.toLong())
            leave()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        SoundGlow(search = search, isShown = isShown, modifier = Modifier.fillMaxSize())
        SoundPill(search = search, isShown = isShown, onClose = leave, modifier = Modifier.align(Alignment.BottomCenter))
    }
}

// The light at the bottom edge, the gradient of the brand as a soft veil and waves of it that move, higher the louder the sound, all fading upward
// It fades in when it appears and out when isShown goes, and when the song was not found it is only a faint veil
@Composable
internal fun SoundGlow(search: SoundSearch, modifier: Modifier = Modifier, isShown: Boolean = true) {
    val isFailed = search.phase is SoundPhase.Failed
    val wave = rememberWave(WavvyMotion.SoundGlowWaveMillis)
    val loudness = rememberLoudness(search.level, WavvyMotion.SoundGlowLevelMillis)
    val colors = WavvyTheme.colors
    val gradient = listOf(colors.gradientStart, colors.gradientMiddle, colors.gradientEnd)
    val swell = if (isFailed) 0f else soundAmplitude(search.phase, loudness)
    val shown = rememberShown(isShown)
    val alpha by animateFloatAsState(if (shown) 1f else 0f, tween(WavvyMotion.SoundShowMillis), label = "glowAlpha")
    val layerPaint = remember { Paint() }

    Canvas(modifier = modifier.graphicsLayer { this.alpha = alpha }) {
        drawGlow(gradient = gradient, wave = wave, swell = swell, isFaint = isFailed, layerPaint = layerPaint)
    }
}

// False for the first frame and then what isShown says, so what is drawn comes in with its animation and not already there
@Composable
private fun rememberShown(isShown: Boolean): Boolean {
    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { entered = true }
    return entered && isShown
}

// The pill with what is happening, at the bottom, the bars while it listens, a spinner while it waits, and the way to try again when the song was not found
// It rises in when it appears, and when the phase changes all that is in it rolls up together, the old phase goes up and the new one comes from below
@Composable
internal fun SoundPill(search: SoundSearch, onClose: () -> Unit, modifier: Modifier = Modifier, isShown: Boolean = true) {
    val wave = rememberWave()
    val loudness = rememberLoudness(search.level)
    val barColor = MaterialTheme.colorScheme.primary
    val millis = WavvyMotion.SoundPhaseMillis

    AnimatedVisibility(
        visible = rememberShown(isShown),
        enter = slideInVertically(tween(WavvyMotion.SoundShowMillis)) { it } + fadeIn(tween(WavvyMotion.SoundShowMillis)),
        exit = slideOutVertically(tween(WavvyMotion.SoundShowMillis)) { it } + fadeOut(tween(WavvyMotion.SoundShowMillis)),
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .navigationBarsPadding()
                .padding(start = SearchDimens.SoundPillSide, end = SearchDimens.SoundPillSide, bottom = SearchDimens.SoundPillSide)
                .widthIn(max = SearchDimens.SoundPillMax)
                .fillMaxWidth()
                .height(SearchDimens.SoundPillHeight)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = SearchDimens.SoundPillAlpha))
                .padding(start = SearchDimens.SoundPillStart, end = SearchDimens.SoundPillEnd)
        ) {
            AnimatedContent(
                targetState = search.phase,
                transitionSpec = {
                    (slideInVertically(tween(millis)) { it } + fadeIn(tween(millis))) togetherWith
                        (slideOutVertically(tween(millis)) { -it } + fadeOut(tween(millis)))
                },
                contentAlignment = Alignment.CenterStart,
                label = "pillPhase",
                modifier = Modifier.weight(1f).fillMaxHeight()
            ) { phase ->
                val isFailed = phase is SoundPhase.Failed

                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxSize()) {
                    // On the start, the bars while it listens or waits, and the resting icon when the song was not found
                    if (isFailed) {
                        Icon(
                            imageVector = WavvyIcons.Sound,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(SearchDimens.SoundPillIcon)
                        )
                    } else {
                        val amplitude = soundAmplitude(phase, loudness)
                        Canvas(modifier = Modifier.size(SearchDimens.SoundPillIcon)) {
                            drawSoundBars(barColor, amplitude, wave, SearchDimens.SoundPillBarWidth, SearchDimens.SoundPillBarGap, SearchDimens.SoundPillBarMin, SearchDimens.SoundPillBarMax)
                        }
                    }
                    Spacer(modifier = Modifier.width(SearchDimens.SoundPillGap))
                    Text(
                        text = stringResource(soundTitleRes(phase)),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    if (phase == SoundPhase.Finding) {
                        CircularProgressIndicator(strokeWidth = SearchDimens.SoundSpinnerStroke, modifier = Modifier.size(SearchDimens.SoundPillSpinner))
                    }
                    // The arrow that goes round is the way to try again, when the microphone was refused it opens the settings of the app
                    if (isFailed) {
                        IconButton(onClick = if (search.isRefused) search.openSettings else search.listenAgain) {
                            Icon(
                                imageVector = WavvyIcons.Refresh,
                                contentDescription = stringResource(R.string.search_sound_again),
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(SearchDimens.SoundPillClose)
                            )
                        }
                    }
                }
            }
            IconButton(onClick = onClose) {
                Icon(
                    imageVector = WavvyIcons.Close,
                    contentDescription = stringResource(R.string.search_sound_close),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(SearchDimens.SoundPillClose)
                )
            }
        }
    }
}

// The light of the bottom edge, drawn on a layer, the gradient as a veil and as waves that cross each other, and then faded upward as one
private fun DrawScope.drawGlow(gradient: List<Color>, wave: Float, swell: Float, isFaint: Boolean, layerPaint: Paint) {
    val top = size.height - SearchDimens.SoundOverlayHeight.toPx()
    val area = Rect(0f, top, size.width, size.height)
    val across = Brush.horizontalGradient(gradient)
    val back = Brush.horizontalGradient(gradient.reversed())

    drawIntoCanvas { canvas ->
        canvas.saveLayer(area, layerPaint)
        drawRect(brush = across, topLeft = area.topLeft, size = area.size, alpha = if (isFaint) SearchDimens.SoundGlowFaintAlpha else SearchDimens.SoundGlowAlpha)

        // Each wave is a shape filled from a line that goes up and down down to the bottom, wider than the screen so neither side is cut
        if (!isFaint) {
            val height = SearchDimens.SoundGlowWave.toPx() * (SearchDimens.SoundGlowWaveBase + (1f - SearchDimens.SoundGlowWaveBase) * swell)
            val step = SearchDimens.SoundGlowStep.toPx()
            SearchDimens.SoundGlowWaves.forEachIndexed { index, alpha ->
                val cycles = SearchDimens.SoundGlowWaveCycles[index]
                val direction = if (index % 2 == 0) 1 else -1
                val baseline = size.height - SearchDimens.SoundGlowLift.toPx() * (index + 1)
                val path = Path()
                path.moveTo(-step, size.height)
                var x = -step
                while (x <= size.width + step) {
                    val turn = 2 * PI * cycles * (x / size.width) + wave * direction
                    path.lineTo(x, baseline - height * sin(turn).toFloat())
                    x += step
                }
                path.lineTo(size.width + step, size.height)
                path.close()
                drawPath(path, brush = if (index % 2 == 0) across else back, alpha = alpha)
            }
        }

        // Everything fades from the bottom edge up to nothing, so the tops of the waves melt away
        drawRect(
            brush = Brush.verticalGradient(listOf(Color.Transparent, Color.Black), startY = top, endY = size.height),
            topLeft = Offset(0f, top),
            size = Size(size.width, area.height),
            blendMode = BlendMode.DstIn
        )
        canvas.restore()
    }
}

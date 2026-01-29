package com.lonewolf.wavvy.ui.common

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import com.lonewolf.wavvy.ui.player.components.AlbumCover
import com.lonewolf.wavvy.ui.player.components.ExpandedPlayerContent
import com.lonewolf.wavvy.ui.player.components.PlayerControls
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

fun lerpFloat(start: Float, stop: Float, fraction: Float): Float = start + fraction * (stop - start)

@Composable
fun PlayerContainer(
    isExpanded: Boolean,
    songTitle: String,
    artistName: String,
    onPillClick: () -> Unit,
    onDismiss: () -> Unit,
    onProgressUpdate: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val config = LocalConfiguration.current
    val density = LocalDensity.current
    val screenHeight = config.screenHeightDp.dp
    val screenWidth = config.screenWidthDp.dp
    val scope = rememberCoroutineScope()

    val bottomMargin = 110.dp
    val maxOffset = with(density) { (screenHeight - 64.dp - bottomMargin).toPx() }

    // Salvar o estado da posição e alpha como Float
    var savedOffsetY by rememberSaveable { mutableFloatStateOf(maxOffset + 150f) }
    var savedAlpha by rememberSaveable { mutableFloatStateOf(0f) }
    var isPlaying by rememberSaveable { mutableStateOf(false) }

    // Flag para controlar se é a primeira composição
    var isFirstComposition by rememberSaveable { mutableStateOf(true) }

    // Criar os Animatables com os valores salvos
    val containerAlpha = remember { Animatable(savedAlpha) }
    val offsetY = remember { Animatable(savedOffsetY) }

    // Atualizar os valores salvos quando mudarem
    LaunchedEffect(containerAlpha.value) { savedAlpha = containerAlpha.value }
    LaunchedEffect(offsetY.value) { savedOffsetY = offsetY.value }

    val progress = (1f - (offsetY.value / maxOffset)).coerceIn(0f, 1f)
    LaunchedEffect(progress) { onProgressUpdate(progress) }

    val currentWidthFraction = lerpFloat(0.92f, 1f, progress)
    val currentCorner = lerp(32.dp, 0.dp, progress)
    val currentHeight = if (progress > 0.01f) screenHeight + bottomMargin else 64.dp

    // Animação inicial apenas na primeira composição
    LaunchedEffect(Unit) {
        if (isFirstComposition) {
            launch { containerAlpha.animateTo(1f, tween(500)) }
            launch {
                offsetY.animateTo(
                    if (isExpanded) 0f else maxOffset,
                    spring(0.82f, 350f)
                )
            }
            isFirstComposition = false
        } else {
            // Se não é primeira composição (ex: mudança de tema), apenas ajusta instantaneamente
            containerAlpha.snapTo(1f)
            offsetY.snapTo(if (isExpanded) 0f else maxOffset)
        }
    }

    // Sincronizar com o estado isExpanded
    LaunchedEffect(isExpanded) {
        if (!isFirstComposition) {
            offsetY.animateTo(if (isExpanded) 0f else maxOffset, spring(0.85f, 400f))
        }
    }

    Box(
        modifier = modifier.alpha(containerAlpha.value),
        contentAlignment = Alignment.TopCenter
    ) {
        Surface(
            modifier = Modifier
                .offset { IntOffset(0, offsetY.value.roundToInt()) }
                .fillMaxWidth(currentWidthFraction)
                .height(currentHeight)
                .draggable(
                    orientation = Orientation.Vertical,
                    state = rememberDraggableState { delta ->
                        scope.launch {
                            if (!(offsetY.value >= maxOffset && delta > 0)) {
                                offsetY.snapTo((offsetY.value + delta).coerceIn(0f, maxOffset + 50f))
                            }
                        }
                    },
                    onDragStopped = { velocity ->
                        scope.launch {
                            if (velocity > 600 && offsetY.value >= maxOffset) {
                                containerAlpha.animateTo(0f, tween(200))
                                onDismiss()
                            } else {
                                val target = if (velocity < -700 || offsetY.value < maxOffset * 0.45f) 0f else maxOffset
                                offsetY.animateTo(target, spring(0.85f, 400f))
                                if ((target == 0f && !isExpanded) || (target == maxOffset && isExpanded)) {
                                    onPillClick()
                                }
                            }
                        }
                    }
                ),
            color = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
                .copy(alpha = lerpFloat(0.7f, 1f, (progress * 1.2f).coerceIn(0f, 1f))),
            shape = RoundedCornerShape(currentCorner),
            shadowElevation = lerp(8.dp, 0.dp, progress),
            onClick = { if (progress < 0.1f) onPillClick() }
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Capa do álbum
                AlbumCover(progress = progress, screenWidth = screenWidth)

                if (progress < 0.2f) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .padding(start = 76.dp)
                            .alpha(1f - progress * 5f)
                    ) {
                        Column {
                            Text(
                                text = songTitle,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                maxLines = 1,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text = artistName,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.tertiary,
                                maxLines = 1
                            )
                        }
                    }
                }

                // Conteúdo expandido
                if (progress > 0.4f) {
                    ExpandedPlayerContent(
                        isExpanded = true,
                        songTitle = songTitle,
                        artistName = artistName,
                        onMinimize = onPillClick,
                        modifier = Modifier
                            .statusBarsPadding()
                            .navigationBarsPadding()
                            .alpha(((progress - 0.4f) * 2f).coerceIn(0f, 1f))
                    )
                }

                // Controles de player
                PlayerControls(
                    progress = progress,
                    isPlaying = isPlaying,
                    onPlayPauseToggle = { isPlaying = !isPlaying },
                    onNext = { /* próximo */ },
                    onPrevious = { /* anterior */ },
                    screenWidth = screenWidth,
                    screenHeight = screenHeight
                )
            }
        }
    }
}

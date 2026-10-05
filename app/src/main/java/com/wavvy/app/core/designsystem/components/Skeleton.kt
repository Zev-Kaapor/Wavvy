package com.wavvy.app.core.designsystem.components

// Compose animation
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
// Compose layouts and foundations
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
// Material 3 components
import androidx.compose.material3.MaterialTheme
// Compose state and runtime
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
// UI styling and utilities
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
// Project resources
import com.wavvy.app.core.designsystem.theme.WavvyMotion
import com.wavvy.app.core.designsystem.theme.WavvyTheme

// Width of the light band as a share of the host width, and how much it leans
private const val BandWidthFraction = 0.6f
private const val BandTilt = 0.4f

// Where the host starts to fade out, as a share of its height
private const val FadeStart = 0.55f

// Flat placeholder shape, the light comes from the SkeletonHost around it
@Composable
fun Modifier.skeleton(shape: Shape = MaterialTheme.shapes.medium): Modifier =
    clip(shape).background(WavvyTheme.colors.skeleton)

// Holds all the placeholders of a screen, one light sweeps over all of them and the bottom fades out
@Composable
fun SkeletonHost(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val highlight = WavvyTheme.colors.skeletonHighlight
    val clear = highlight.copy(alpha = 0f)
    val transition = rememberInfiniteTransition(label = "skeleton")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = WavvyMotion.SkeletonSweepMillis,
                delayMillis = WavvyMotion.SkeletonPauseMillis,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "skeletonSweep"
    )

    Column(
        modifier = modifier
            .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
            .drawWithContent {
                drawContent()

                // Light only over the shapes, from before the left edge to past the right edge
                val band = size.width * BandWidthFraction
                val start = -band + (size.width + band) * progress
                drawRect(
                    brush = Brush.linearGradient(
                        colors = listOf(clear, highlight, clear),
                        start = Offset(start, 0f),
                        end = Offset(start + band, band * BandTilt)
                    ),
                    blendMode = BlendMode.SrcAtop
                )

                // Everything fades out toward the bottom
                drawRect(
                    brush = Brush.verticalGradient(
                        0f to Color.Black,
                        FadeStart to Color.Black,
                        1f to Color.Transparent
                    ),
                    blendMode = BlendMode.DstIn
                )
            },
        content = content
    )
}

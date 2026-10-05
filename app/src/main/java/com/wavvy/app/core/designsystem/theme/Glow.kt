package com.wavvy.app.core.designsystem.theme

// Compose drawing and utilities
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas

// How the glow fades from the top of the screen, as shares of its height and of the strength at the top
private const val GlowReach = 0.5f
private const val GlowMiddleStop = 0.2f
private const val GlowMiddleStrength = 0.45f
private const val GlowTailStop = 0.4f
private const val GlowTailStrength = 0.07f

// Two accent colors blending from left to right across the whole width, strongest at the top edge and gone by the middle of the screen
// The strength goes from 0 to 1 and is read while drawing, so fading it never recomposes
@Composable
fun Modifier.backgroundGlow(strength: () -> Float = { 1f }): Modifier {
    val colors = WavvyTheme.colors
    val layerPaint = remember { Paint() }

    return drawBehind {
        val current = strength()
        if (current <= 0f) return@drawBehind

        val area = Rect(0f, 0f, size.width, size.height * GlowReach)

        // The two colors are drawn on a temporary layer and the vertical fade is applied as a mask over it
        drawIntoCanvas { canvas ->
            canvas.saveLayer(area, layerPaint)
            drawRect(
                brush = Brush.horizontalGradient(listOf(colors.glowStart, colors.glowEnd)),
                size = area.size
            )
            drawRect(
                brush = Brush.verticalGradient(
                    0f to Color.Black.copy(alpha = current),
                    GlowMiddleStop to Color.Black.copy(alpha = current * GlowMiddleStrength),
                    GlowTailStop to Color.Black.copy(alpha = current * GlowTailStrength),
                    GlowReach to Color.Transparent,
                    endY = size.height
                ),
                size = area.size,
                blendMode = BlendMode.DstIn
            )
            canvas.restore()
        }
    }
}

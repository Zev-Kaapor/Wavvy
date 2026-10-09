package com.wavvy.app.core.designsystem.components

// Compose drawing and layouts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
// Material 3 components
import androidx.compose.material3.MaterialTheme
// Compose state and runtime
import androidx.compose.runtime.Composable
// UI styling and utilities
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.Dp
// Project resources
import com.wavvy.app.core.designsystem.theme.WavvyTheme

// The names YouTube Music gives the arrows of a chart, the lists of artists and the lists of songs do not call them the same
private const val UpEnding = "_UP"
private const val DownEnding = "_DOWN"
private const val NeutralWord = "NEUTRAL"

// How an item of a chart moved, a gray dot when it stayed, a green triangle up when it rose and a red one down when it fell
@Composable
fun TrendMark(trend: String, size: Dp, modifier: Modifier = Modifier) {
    val isUp = trend.endsWith(UpEnding)
    val isDown = trend.endsWith(DownEnding)

    when {
        trend.contains(NeutralWord) -> Box(
            modifier = modifier.size(size).background(MaterialTheme.colorScheme.secondary, CircleShape)
        )

        isUp || isDown -> {
            val color = if (isUp) WavvyTheme.colors.chartUp else MaterialTheme.colorScheme.error

            Canvas(modifier = modifier.size(size)) {
                val triangle = Path().apply {
                    if (isUp) {
                        moveTo(this@Canvas.size.width / 2f, 0f)
                        lineTo(this@Canvas.size.width, this@Canvas.size.height)
                        lineTo(0f, this@Canvas.size.height)
                    } else {
                        moveTo(0f, 0f)
                        lineTo(this@Canvas.size.width, 0f)
                        lineTo(this@Canvas.size.width / 2f, this@Canvas.size.height)
                    }
                    close()
                }
                drawPath(triangle, color)
            }
        }
    }
}

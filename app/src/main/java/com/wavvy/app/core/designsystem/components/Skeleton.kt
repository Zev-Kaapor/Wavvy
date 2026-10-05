package com.wavvy.app.core.designsystem.components

// Compose animation
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
// Material 3 components
import androidx.compose.material3.MaterialTheme
// Compose state and runtime
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
// UI styling and utilities
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Shape
// Project resources
import com.wavvy.app.core.designsystem.theme.WavvyMotion
import com.wavvy.app.core.designsystem.theme.WavvyTheme

// Loading placeholder that breathes while the data does not arrive
@Composable
fun Modifier.skeleton(shape: Shape = MaterialTheme.shapes.medium): Modifier {
    val color = WavvyTheme.colors.skeletonPulse
    val transition = rememberInfiniteTransition(label = "skeleton")
    val alpha by transition.animateFloat(
        initialValue = 1f,
        targetValue = WavvyMotion.SkeletonPulseLowAlpha,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = WavvyMotion.SkeletonPulseMillis, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "skeletonAlpha"
    )

    return clip(shape).drawBehind { drawRect(color = color, alpha = alpha) }
}

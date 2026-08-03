package com.wavvy.app.core.designsystem.components

// Compose animations and specifications
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
// Compose layouts and foundations
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
// Material 3 components
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
// Compose state and lifecycle hooks
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
// UI styling and utilities
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
// Project resources
import com.wavvy.app.core.designsystem.theme.MusicStateColors
import com.wavvy.app.core.designsystem.theme.Poppins

// Toast parameter data holder
data class ToastData(
    val id: Long = System.nanoTime(),
    val message: String,
    val subtitle: String? = null,
    val durationMillis: Int = 3000
)

// Custom notification toast popup
@Composable
fun CustomToast(
    modifier: Modifier = Modifier,
    message: String,
    subtitle: String?,
    durationMillis: Int = 3000,
    onDismiss: () -> Unit
) {
    val progress = remember { Animatable(1f) }
    val successColor = MusicStateColors.downloaded
    val pillShape = remember { RoundedCornerShape(32.dp) }
    val bottomPillShape = remember { RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp) }

    // Progress animation loop
    LaunchedEffect(Unit) {
        progress.animateTo(
            targetValue = 0f,
            animationSpec = tween(
                durationMillis = durationMillis,
                easing = LinearEasing
            )
        )
        onDismiss()
    }

    val gradientBrush = remember(successColor) {
        Brush.horizontalGradient(
            colors = listOf(
                successColor,
                successColor.copy(alpha = 0.7f)
            )
        )
    }

    Surface(
        modifier = modifier
            .fillMaxWidth(0.92f)
            .heightIn(min = 64.dp)
            .clip(pillShape)
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.80f))
            .border(
                width = 0.5.dp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.23f),
                shape = pillShape
            )
            .graphicsLayer {
                compositingStrategy = CompositingStrategy.Offscreen
                shadowElevation = 0f
                ambientShadowColor = Color.Transparent
                spotShadowColor = Color.Transparent
            },
        color = Color.Transparent,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Success checkmark badge
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .background(successColor.copy(alpha = 0.12f), RoundedCornerShape(100))
                        .border(1.dp, successColor.copy(alpha = 0.20f), RoundedCornerShape(100)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = null,
                        tint = successColor,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = message,
                        fontFamily = Poppins,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (subtitle != null) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = subtitle,
                            fontFamily = Poppins,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Bottom animated progress indicator
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction = progress.value)
                        .fillMaxHeight()
                        .clip(bottomPillShape)
                        .background(brush = gradientBrush)
                )
            }
        }
    }
}

package com.lonewolf.wavvy.ui.common.components

// Compose foundation and layout
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
// Material 3 and icons
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
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
import com.lonewolf.wavvy.ui.theme.MusicStateColors
import com.lonewolf.wavvy.ui.theme.Poppins

@Composable
fun CustomToast(
    message: String,
    subtitle: String?,
    durationMillis: Int = 3000,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val progress = remember { Animatable(1f) }
    val successColor = MusicStateColors.downloaded
    val pillShape = RoundedCornerShape(32.dp)

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
                        .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    successColor,
                                    successColor.copy(alpha = 0.7f)
                                )
                            )
                        )
                )
            }
        }
    }
}

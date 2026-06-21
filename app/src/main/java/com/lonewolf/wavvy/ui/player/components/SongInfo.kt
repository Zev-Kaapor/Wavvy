package com.lonewolf.wavvy.ui.player.components

// Compose foundation and layout
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.*
// Material 3 and icons
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
// UI tools and state
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.lerp as lerpColor
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp
// Project resources
import com.lonewolf.wavvy.ui.theme.Poppins
import com.lonewolf.wavvy.ui.theme.ElectricCyan

// Animated song metadata for player transitions
@Composable
fun SongInfo(
    title: String,
    artist: String,
    progress: Float,
    modifier: Modifier = Modifier,
    isLandscape: Boolean = false,
    screenWidth: Dp = 400.dp
) {
    val brandCyan = ElectricCyan

    // Title transition
    val titleColor = lerpColor(
        start = MaterialTheme.colorScheme.onSurface,
        stop = Color.White,
        fraction = progress
    )

    // Artist color logic
    val artistColor = lerpColor(
        start = MaterialTheme.colorScheme.onSurfaceVariant,
        stop = brandCyan,
        fraction = progress
    )

    // Text shadow
    val textShadow = Shadow(
        color = Color.Black.copy(alpha = 0.8f * progress),
        offset = Offset(0f, 0f),
        blurRadius = 24f * progress
    )

    val isSmallScreen = screenWidth < 370.dp
    val baseTitleSize = if (isLandscape) 18.sp else if (isSmallScreen) 17.sp else 22.sp
    val baseArtistSize = if (isLandscape) 13.sp else if (isSmallScreen) 13.sp else 16.sp

    val titleScale = (14f / baseTitleSize.value) + (progress * (1f - 14f / baseTitleSize.value))
    val artistScale = (11f / baseArtistSize.value) + (progress * (1f - 11f / baseArtistSize.value))
    // Interpolated alignment and origin for smooth transition
    val horizontalBias = if (isLandscape) -1f else -1f
    val originX = if (isLandscape) 0.0f * progress else 0f
    val verticalSpace = if (isLandscape) lerp((-2).dp, 4.dp, progress) else lerp((-10).dp, 6.dp, progress)
    val iconSize = lerp(14.dp, 18.dp, progress)

    CompositionLocalProvider(
        LocalDensity provides Density(
            density = LocalDensity.current.density,
            fontScale = 1f
        )
    ) {
        Column(
            modifier = modifier.fillMaxWidth(),
            horizontalAlignment = BiasAlignment.Horizontal(horizontalBias),
            verticalArrangement = Arrangement.spacedBy(verticalSpace, Alignment.CenterVertically)
        ) {
            Text(
                text = title,
                color = titleColor,
                style = TextStyle(
                    fontSize = baseTitleSize,
                    fontWeight = FontWeight.Bold,
                    fontFamily = Poppins,
                    shadow = textShadow
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .basicMarquee(
                        iterations = 1,
                        initialDelayMillis = 3000,
                        velocity = 30.dp
                    )
                    .graphicsLayer {
                        scaleX = titleScale
                        scaleY = titleScale
                        transformOrigin = TransformOrigin(originX, 0.5f)
                    },
                maxLines = 1
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.graphicsLayer {
                    transformOrigin = TransformOrigin(originX, 0.5f)
                }
            ) {
                // Icon is now always present and scales smoothly
                Icon(
                    imageVector = Icons.Rounded.Person,
                    contentDescription = null,
                    tint = artistColor,
                    modifier = Modifier
                        .size(iconSize)
                        .alpha(if (progress < 0.1f) 0.7f else progress)
                        .padding(end = 4.dp)
                )

                Text(
                    text = artist,
                    color = artistColor,
                    style = TextStyle(
                        fontSize = baseArtistSize,
                        fontWeight = if (progress > 0.5f) FontWeight.SemiBold else FontWeight.Medium,
                        fontFamily = Poppins,
                        shadow = textShadow
                    ),
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .basicMarquee(
                            iterations = 1,
                            initialDelayMillis = 3000,
                            velocity = 30.dp
                        )
                        .graphicsLayer {
                            scaleX = artistScale
                            scaleY = artistScale
                            transformOrigin = TransformOrigin(0f, 0.5f)
                        },
                    maxLines = 1
                )
            }
        }
    }
}

package com.lonewolf.wavvy.ui.player.components

// Compose foundation and layout
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
// Material 3 and icons
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
// UI tools and state
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp as lerpColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp
// Project resources
import com.lonewolf.wavvy.ui.theme.Poppins
import com.lonewolf.wavvy.ui.theme.accentCyan

// Animated song metadata for player transitions
@Composable
fun SongInfo(
    title: String,
    artist: String,
    progress: Float,
    modifier: Modifier = Modifier,
    isLandscape: Boolean = false
) {
    val isDark = isSystemInDarkTheme()
    val brandCyan = MaterialTheme.accentCyan

    // Title transition
    val titleColor = lerpColor(
        start = MaterialTheme.colorScheme.onSurface,
        stop = Color.White,
        fraction = progress
    )

    // Artist color logic
    val artistColor = if (isDark) {
        brandCyan
    } else {
        lerpColor(
            start = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            stop = brandCyan,
            fraction = progress
        )
    }

    // Text shadow
    val textShadow = Shadow(
        color = Color.Black.copy(alpha = 0.8f * progress),
        offset = Offset(0f, 0f),
        blurRadius = 24f * progress
    )

    // Interpolated sizes
    val baseTitleSize = if (isLandscape) 20.sp else 24.sp
    val baseArtistSize = if (isLandscape) 14.sp else 18.sp
    
    val titleScale = (15f / baseTitleSize.value) + (progress * (1f - 15f / baseTitleSize.value))
    val artistScale = (13f / baseArtistSize.value) + (progress * (1f - 13f / baseArtistSize.value))
    
    val verticalSpace = if (isLandscape) 2.dp else lerp((-10).dp, 6.dp, progress)
    val iconSize = lerp(0.dp, 18.dp, progress)

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = if (isLandscape && progress > 0.5f) Alignment.CenterHorizontally else Alignment.Start,
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
            modifier = Modifier.graphicsLayer {
                scaleX = titleScale
                scaleY = titleScale
                transformOrigin = if (isLandscape && progress > 0.5f) TransformOrigin(0.5f, 0.5f) else TransformOrigin(0f, 0.5f)
            },
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (progress > 0.1f) {
                Icon(
                    imageVector = Icons.Rounded.Person,
                    contentDescription = null,
                    tint = artistColor,
                    modifier = Modifier
                        .size(iconSize)
                        .alpha(progress)
                        .padding(end = 4.dp)
                )
            }

            Text(
                text = artist,
                color = artistColor,
                style = TextStyle(
                    fontSize = baseArtistSize,
                    fontWeight = if (progress > 0.5f) FontWeight.SemiBold else FontWeight.Medium,
                    fontFamily = Poppins,
                    shadow = textShadow
                ),
                modifier = Modifier.graphicsLayer {
                    scaleX = artistScale
                    scaleY = artistScale
                    transformOrigin = if (isLandscape && progress > 0.5f) TransformOrigin(0.5f, 0.5f) else TransformOrigin(0f, 0.5f)
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

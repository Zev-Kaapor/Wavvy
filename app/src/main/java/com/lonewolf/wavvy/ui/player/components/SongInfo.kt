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
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val brandCyan = MaterialTheme.accentCyan

    // Title transition: Surface to Pure White
    val titleColor = lerpColor(
        start = MaterialTheme.colorScheme.onSurface,
        stop = Color.White,
        fraction = progress
    )

    // Artist color logic: Static in dark mode, transition in light mode
    val artistColor = if (isDark) {
        brandCyan
    } else {
        lerpColor(
            start = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
            stop = brandCyan,
            fraction = progress
        )
    }

    // Diffuse shadow for readability
    val textShadow = Shadow(
        color = Color.Black.copy(alpha = 0.8f * progress),
        offset = Offset(0f, 0f),
        blurRadius = 24f * progress
    )

    // Interpolated typography values
    val titleSize = lerp(14.sp, 24.sp, progress)
    val artistSize = lerp(11.sp, 16.sp, progress)
    val verticalSpace = lerp(0.dp, 4.dp, progress)
    val iconSize = lerp(0.dp, 20.dp, progress)
    val horizontalGap = lerp(0.dp, 6.dp, progress)

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(verticalSpace)
    ) {
        Text(
            text = title,
            color = titleColor,
            style = TextStyle(
                fontSize = titleSize,
                fontWeight = FontWeight.Bold,
                fontFamily = Poppins,
                shadow = textShadow
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(horizontalGap)
        ) {
            Icon(
                imageVector = Icons.Rounded.Person,
                contentDescription = null,
                tint = artistColor,
                modifier = Modifier
                    .size(iconSize)
                    .alpha(progress)
            )

            Text(
                text = artist,
                color = artistColor,
                style = TextStyle(
                    fontSize = artistSize,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = Poppins,
                    shadow = textShadow
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

package com.lonewolf.wavvy.ui.player.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp
import com.lonewolf.wavvy.ui.theme.Poppins

@Composable
fun SongInfo(
    title: String,
    artist: String,
    progress: Float, // Required for transition
    modifier: Modifier = Modifier
) {
    // Interpolating sizes and spacing based on animation progress
    val titleSize = lerp(14.sp, 24.sp, progress)
    val artistSize = lerp(11.sp, 16.sp, progress)
    val verticalSpace = lerp(0.dp, 4.dp, progress)
    val iconSize = lerp(0.dp, 20.dp, progress)

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(verticalSpace)
    ) {
        // Song Title
        Text(
            text = title,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = titleSize,
            fontWeight = FontWeight.Bold,
            fontFamily = Poppins,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        // Artist row
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Icon appears only as it expands
            if (progress > 0.1f) {
                Icon(
                    imageVector = Icons.Rounded.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier
                        .size(iconSize)
                        .alpha(progress)
                )
            }
            Text(
                text = artist,
                color = MaterialTheme.colorScheme.tertiary,
                fontSize = artistSize,
                fontWeight = FontWeight.Medium,
                fontFamily = Poppins,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

package com.lonewolf.wavvy.ui.search.results.components

// Compose layouts and foundations
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
// Material 3 components
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp

// Reusable grid item for album display
@Composable
fun AlbumGridItem(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val containerColor = MaterialTheme.colorScheme.surfaceVariant

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(8.dp)
    ) {
        // Cover skeleton
        Box(
            modifier = Modifier
                .aspectRatio(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(containerColor)
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Title skeleton
        Box(
            modifier = Modifier
                .fillMaxWidth(0.7f)
                .height(12.dp)
                .clip(CircleShape)
                .background(containerColor)
        )

        Spacer(modifier = Modifier.height(6.dp))

        // Subtitle skeleton
        Box(
            modifier = Modifier
                .fillMaxWidth(0.45f)
                .height(10.dp)
                .clip(CircleShape)
                .background(containerColor.copy(alpha = 0.6f))
        )
    }
}

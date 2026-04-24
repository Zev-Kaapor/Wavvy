package com.lonewolf.wavvy.ui.search.components

// Compose layouts and foundations
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
// Material 3 components
import androidx.compose.material3.MaterialTheme
// State and composition utilities
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
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

// Individual search result row
@Composable
fun SearchResultItem(
    isArtist: Boolean = false,
    onClick: () -> Unit = {}
) {
    val containerColor = MaterialTheme.colorScheme.surfaceVariant

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(vertical = 8.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar or cover skeleton
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(if (isArtist) CircleShape else RoundedCornerShape(8.dp))
                .background(containerColor)
        )

        Spacer(modifier = Modifier.width(16.dp))

        // Textual metadata skeleton
        Column(modifier = Modifier.weight(1f)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .height(12.dp)
                    .clip(CircleShape)
                    .background(containerColor)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.35f)
                    .height(10.dp)
                    .clip(CircleShape)
                    .background(containerColor.copy(alpha = 0.6f))
            )
        }
    }
}

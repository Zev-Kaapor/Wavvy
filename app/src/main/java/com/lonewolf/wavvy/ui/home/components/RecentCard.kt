package com.lonewolf.wavvy.ui.home.components

// Compose foundation and layout
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
// Material 3 components
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.History
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
// UI utilities
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
// Project resources
import com.lonewolf.wavvy.R
import com.lonewolf.wavvy.ui.common.components.sections.SectionTitle
import com.lonewolf.wavvy.ui.theme.Poppins

// Data model for recent tracks
data class RecentTrack(
    val id: String,
    val title: String,
    val artist: String
)

// Horizontal list of recently played items
@Composable
fun RecentSection(
    modifier: Modifier = Modifier,
    tracks: List<RecentTrack> = emptyList(),
    onItemClick: (String) -> Unit
) {
    Column(modifier = modifier.fillMaxWidth()) {
        // Section header
        SectionTitle(text = stringResource(R.string.section_title_recent))

        if (tracks.isEmpty()) {
            // Empty state card
            RecentEmptyState()
        } else {
            // Horizontal scrolling list
            LazyRow(
                contentPadding = PaddingValues(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(
                    items = tracks,
                    key = { track -> track.id },
                    contentType = { "recent_card" }
                ) { track ->
                    RecentCard(
                        title = track.title,
                        subtitle = track.artist,
                        onClick = { onItemClick(track.id) }
                    )
                }
            }
        }
    }
}

// Recent item card with touch target optimization
@Composable
fun RecentCard(
    title: String?,
    subtitle: String?,
    onClick: () -> Unit
) {
    val containerColor = MaterialTheme.colorScheme.surfaceVariant

    Column(
        modifier = Modifier
            .width(156.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(8.dp)
    ) {
        // Cover placeholder
        Box(
            modifier = Modifier
                .size(140.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(containerColor)
        )

        Spacer(Modifier.height(10.dp))

        // Title or skeleton
        if (title != null) {
            Text(
                text = title,
                fontFamily = Poppins,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        } else {
            Box(
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .fillMaxWidth(0.8f)
                    .height(12.dp)
                    .clip(CircleShape)
                    .background(containerColor)
            )
        }

        Spacer(Modifier.height(6.dp))

        // Subtitle or skeleton
        if (subtitle != null) {
            Text(
                text = subtitle,
                fontFamily = Poppins,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        } else {
            Box(
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .fillMaxWidth(0.5f)
                    .height(10.dp)
                    .clip(CircleShape)
                    .background(containerColor.copy(alpha = 0.6f))
            )
        }
    }
}

// Default empty state
@Composable
fun RecentEmptyState() {
    Box(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .height(180.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.linearGradient(
                    listOf(
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f)
                    )
                )
            )
            .border(
                1.dp,
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                RoundedCornerShape(20.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp)
        ) {
            // Empty state icon
            Icon(
                imageVector = Icons.Rounded.History,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                modifier = Modifier.size(32.dp)
            )

            Spacer(Modifier.height(12.dp))

            // Information text
            Text(
                text = stringResource(R.string.recent_empty_state),
                fontFamily = Poppins,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )
        }
    }
}

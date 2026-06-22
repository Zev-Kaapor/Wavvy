package com.lonewolf.wavvy.ui.home.components

// UI framework and layouts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
// Material 3 and vector icons
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoGraph
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.MusicNote
// Material 3 components
import androidx.compose.material3.*
// Compose state and graphics
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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

// Data model for generic recommendation items
data class SimilarItem(
    val id: String,
    val title: String?,
    val imageUrl: String? = null
)

// Main discovery section that groups artists and songs under a single title
@Composable
fun SimilarDiscoverySection(
    baseName: String?,
    artists: List<SimilarItem>,
    songs: List<SimilarItem>,
    onArtistClick: (String) -> Unit,
    onSongClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
    ) {
        // Unified section header logic
        val headerText = if (!baseName.isNullOrBlank()) {
            stringResource(R.string.section_title_similar_to, baseName)
        } else {
            stringResource(R.string.section_title_similar_to_empty)
        }

        SectionTitle(text = headerText)

        if (artists.isEmpty() && songs.isEmpty()) {
            // Empty state card
            SimilarEmptyState()
        } else {
            // Artist row (Discovery icon)
            if (artists.isNotEmpty()) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    items(items = artists, key = { it.id }) { artist ->
                        SimilarLandscapeItem(
                            text = artist.title,
                            icon = Icons.Rounded.AutoGraph,
                            onClick = { artist.title?.let { onArtistClick(it) } }
                        )
                    }
                }
            }

            // Song row (Music icon)
            if (songs.isNotEmpty()) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(items = songs, key = { it.id }) { song ->
                        SimilarLandscapeItem(
                            text = song.title,
                            icon = Icons.Rounded.MusicNote,
                            onClick = { song.title?.let { onSongClick(it) } }
                        )
                    }
                }
            }
        }
    }
}

// Reusable landscape card with adaptable overlay and ripple
@Composable
private fun SimilarLandscapeItem(
    text: String?,
    icon: ImageVector,
    onClick: () -> Unit
) {
    val isDark = isSystemInDarkTheme()

    // Context-aware scrim opacity
    val scrimColor = if (isDark) {
        Color.Black.copy(alpha = 0.7f)
    } else {
        Color.Black.copy(alpha = 0.35f)
    }

    Column(
        modifier = Modifier
            .width(216.dp)
            .clip(RoundedCornerShape(24.dp))
            .clickable(onClick = onClick)
            .padding(8.dp)
    ) {
        // Card Body
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            // Context icon
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                modifier = Modifier.size(48.dp)
            )

            // Bottom-aligned gradient scrim
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, scrimColor),
                            startY = 130f
                        )
                    )
            )

            // Text or Skeleton placeholder
            if (text != null) {
                Text(
                    text = text,
                    fontFamily = Poppins,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = Color.White,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                )
            } else {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(horizontal = 12.dp, vertical = 12.dp)
                        .width(80.dp)
                        .height(8.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))
                )
            }
        }
    }
}

// Default empty state
@Composable
fun SimilarEmptyState() {
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
                imageVector = Icons.Rounded.Explore,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                modifier = Modifier.size(32.dp)
            )

            Spacer(Modifier.height(12.dp))

            // Information text
            Text(
                text = stringResource(R.string.similar_empty_state),
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

package com.lonewolf.wavvy.ui.home.components

// UI framework and layouts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
// Material 3 and vector icons
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoGraph
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
// Project resources
import com.lonewolf.wavvy.R
import com.lonewolf.wavvy.ui.common.SectionTitle
import com.lonewolf.wavvy.ui.theme.Poppins

// Data model for generic recommendation items
data class SimilarItem(
    val id: String,
    val title: String,
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
    // Artist placeholders (5 items)
    val displayArtists = if (artists.isEmpty()) {
        val artistPlaceholder = stringResource(R.string.placeholder_wavvy_artist)
        listOf(
            SimilarItem("A1", artistPlaceholder),
            SimilarItem("A2", artistPlaceholder),
            SimilarItem("A3", artistPlaceholder),
            SimilarItem("A4", artistPlaceholder),
            SimilarItem("A5", artistPlaceholder)
        )
    } else artists

    // Song placeholders (5 items)
    val displaySongs = if (songs.isEmpty()) {
        val songPlaceholder = stringResource(R.string.placeholder_wavvy_song)
        listOf(
            SimilarItem("S1", songPlaceholder),
            SimilarItem("S2", songPlaceholder),
            SimilarItem("S3", songPlaceholder),
            SimilarItem("S4", songPlaceholder),
            SimilarItem("S5", songPlaceholder)
        )
    } else songs

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

        // Artist row (Discovery icon)
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(bottom = 12.dp)
        ) {
            items(items = displayArtists, key = { it.id }) { artist ->
                SimilarLandscapeItem(
                    text = artist.title,
                    icon = Icons.Rounded.AutoGraph,
                    onClick = { onArtistClick(artist.title) }
                )
            }
        }

        // Song row (Music icon)
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(items = displaySongs, key = { it.id }) { song ->
                SimilarLandscapeItem(
                    text = song.title,
                    icon = Icons.Rounded.MusicNote,
                    onClick = { onSongClick(song.title) }
                )
            }
        }
    }
}

// Reusable landscape card with adaptable overlay
@Composable
private fun SimilarLandscapeItem(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    val cardWidth = 200.dp
    val cardHeight = 110.dp
    val isDark = isSystemInDarkTheme()

    // Context-aware scrim opacity for readability
    val scrimColor = if (isDark) {
        Color.Black.copy(alpha = 0.7f)
    } else {
        Color.Black.copy(alpha = 0.35f)
    }

    Column(
        modifier = Modifier
            .width(cardWidth)
            .clickable(onClick = onClick)
    ) {
        // Card Body
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(cardHeight)
                .clip(RoundedCornerShape(20.dp))
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                            MaterialTheme.colorScheme.surfaceVariant
                        )
                    )
                ),
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

            // Overlay label
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
        }
    }
}

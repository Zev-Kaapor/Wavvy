package com.lonewolf.wavvy.ui.home.components

// Compose layout and foundation
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
// Material icons and components
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.*
// Compose state and graphics
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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

// Data model for discovery chips
data class DiscoveryItem(
    val titleResId: Int,
    val color: Color,
    val icon: ImageVector
)

// Static discovery data
private val discoveryItems = listOf(
    DiscoveryItem(R.string.chip_trending, Color(0xFFE91E63), Icons.AutoMirrored.Filled.TrendingUp),
    DiscoveryItem(R.string.chip_releases, Color(0xFF2196F3), Icons.Rounded.LibraryMusic),
    DiscoveryItem(R.string.chip_mixes, Color(0xFF4CAF50), Icons.Rounded.MusicNote),
    DiscoveryItem(R.string.chip_radios, Color(0xFFFF9800), Icons.Default.Radio)
)

// Explore section with horizontal chips
@Composable
fun PersonalizedCard(
    onItemClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        // Section header
        SectionTitle(text = stringResource(R.string.section_title_explore))

        // Horizontal chips list
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(
                items = discoveryItems,
                key = { it.titleResId },
                contentType = { "discovery_chip" }
            ) { item ->
                val title = stringResource(item.titleResId)
                DiscoveryChip(
                    title = title,
                    color = item.color,
                    icon = item.icon,
                    onClick = { onItemClick(title) }
                )
            }
        }
    }
}

// Individual discovery chip
@Composable
fun DiscoveryChip(
    title: String,
    color: Color,
    icon: ImageVector,
    onClick: () -> Unit
) {
    // Derived colors optimization
    val backgroundColor = remember(color) { color.copy(alpha = 0.12f) }
    val borderColor = remember(color) { color.copy(alpha = 0.25f) }
    val iconBackgroundColor = remember(color) { color.copy(alpha = 0.2f) }

    Row(
        modifier = Modifier
            .width(170.dp)
            .height(60.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(backgroundColor)
            .border(0.5.dp, borderColor, RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon container
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(iconBackgroundColor, RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(Modifier.width(10.dp))

        // Chip title
        Text(
            text = title,
            fontFamily = Poppins,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

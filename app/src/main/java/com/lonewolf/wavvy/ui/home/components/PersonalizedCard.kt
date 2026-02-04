package com.lonewolf.wavvy.ui.home.components

// Compose foundation and layout
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
// Icons
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.*
// Tools and styling
import androidx.compose.runtime.Composable
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
import com.lonewolf.wavvy.ui.theme.DiscoveryChipColors
import com.lonewolf.wavvy.ui.theme.Poppins

// Exploration section with categorized chips
@Composable
fun PersonalizedCard(onItemClick: (String) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        SectionTitle(text = stringResource(R.string.section_title_explore))

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val discoveryItems = listOf(
                Triple(R.string.chip_trending, DiscoveryChipColors.trending, Icons.AutoMirrored.Filled.TrendingUp),
                Triple(R.string.chip_top_50, DiscoveryChipColors.top50, Icons.Default.Star),
                Triple(R.string.chip_releases, DiscoveryChipColors.releases, Icons.Rounded.MusicNote),
                Triple(R.string.chip_mixes, DiscoveryChipColors.mixes, Icons.Rounded.LibraryMusic),
                Triple(R.string.chip_community, DiscoveryChipColors.community, Icons.Default.Groups)
            )

            items(discoveryItems.size) { index ->
                val (titleRes, color, icon) = discoveryItems[index]
                val title = stringResource(titleRes)

                DiscoveryChip(
                    title = title,
                    color = color,
                    icon = icon,
                    onClick = { onItemClick(title) }
                )
            }
        }
    }
}

// Interactive chip for music discovery
@Composable
fun DiscoveryChip(
    title: String,
    color: Color,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .width(170.dp)
            .height(60.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(color.copy(alpha = 0.12f))
            .border(0.5.dp, color.copy(alpha = 0.25f), RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon container
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(color.copy(alpha = 0.2f), RoundedCornerShape(10.dp)),
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

        Text(
            text = title,
            fontFamily = Poppins,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

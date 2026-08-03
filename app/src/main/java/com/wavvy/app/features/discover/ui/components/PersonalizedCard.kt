package com.wavvy.app.features.discover.ui.components

// Compose layout and foundation
import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
// Material icons and components
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
// Compose state and graphics
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
// Project resources
import com.wavvy.app.R
import com.wavvy.app.core.designsystem.components.SectionTitle
import com.wavvy.app.core.designsystem.theme.DiscoveryChipColors
import com.wavvy.app.core.designsystem.theme.Poppins

data class DiscoveryItem(
    val titleResId: Int,
    val color: Color,
    val icon: ImageVector
)

private val discoveryItems = listOf(
    DiscoveryItem(R.string.chip_trending, DiscoveryChipColors.trending, Icons.AutoMirrored.Filled.TrendingUp),
    DiscoveryItem(R.string.chip_highlights, DiscoveryChipColors.releases, Icons.Rounded.AutoAwesome),
    DiscoveryItem(R.string.chip_community, DiscoveryChipColors.community, Icons.Rounded.Groups),
    DiscoveryItem(R.string.chip_releases, DiscoveryChipColors.releases, Icons.Rounded.LibraryMusic),
    DiscoveryItem(R.string.chip_mixes, DiscoveryChipColors.mixes, Icons.Rounded.MusicNote),
    DiscoveryItem(R.string.chip_charts, DiscoveryChipColors.top50, Icons.Rounded.BarChart),
    DiscoveryItem(R.string.chip_events, DiscoveryChipColors.community, Icons.Rounded.ConfirmationNumber),
    DiscoveryItem(R.string.chip_videos, DiscoveryChipColors.releases, Icons.Rounded.PlayCircle),
    DiscoveryItem(R.string.chip_genres, DiscoveryChipColors.playlists, Icons.Rounded.GridView),
    DiscoveryItem(R.string.chip_radios, DiscoveryChipColors.radios, Icons.Rounded.Radio)
)

@Composable
fun PersonalizedCard(
    onItemClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val columnCount = if (isLandscape) 4 else 2

    val chunkedItems = remember(columnCount) { discoveryItems.chunked(columnCount) }

    Column(modifier = modifier.fillMaxWidth()) {
        SectionTitle(text = stringResource(R.string.section_title_explore))

        Spacer(modifier = Modifier.height(6.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            chunkedItems.forEach { rowItems ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    rowItems.forEach { item ->
                        val title = stringResource(item.titleResId)
                        DiscoveryChip(
                            title = title,
                            color = item.color,
                            icon = item.icon,
                            onClick = { onItemClick(title) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (rowItems.size < columnCount) {
                        repeat(columnCount - rowItems.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

// Discovery chip component
@Composable
fun DiscoveryChip(
    title: String,
    color: Color,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = remember(color) { color.copy(alpha = 0.08f) }
    val borderColor = remember(color) { color.copy(alpha = 0.18f) }
    val iconBgColor = remember(color) { color.copy(alpha = 0.14f) }

    Row(
        modifier = modifier
            .height(64.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(backgroundColor)
            .border(1.dp, borderColor, RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(iconBgColor, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(Modifier.width(12.dp))

        Text(
            text = title,
            fontFamily = Poppins,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

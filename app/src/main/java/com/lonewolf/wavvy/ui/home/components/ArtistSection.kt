package com.lonewolf.wavvy.ui.home.components

// Compose layouts and foundations
import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
// Material 3 components
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
// UI styling and utilities
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
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

// Individual artist circular card
@Composable
fun ArtistCard(
    name: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isSkeleton = name.isEmpty()
    val containerColor = MaterialTheme.colorScheme.surfaceVariant

    Column(
        modifier = modifier
            .width(100.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(
                enabled = !isSkeleton,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Avatar container
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(containerColor)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Name content
        if (isSkeleton) {
            Box(
                modifier = Modifier
                    .width(64.dp)
                    .height(10.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(containerColor)
            )
        } else {
            Text(
                text = name,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontFamily = Poppins,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                ),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// Horizontal list of artists
@Composable
fun ArtistSection(
    artists: List<String> = emptyList(),
    onItemClick: (String) -> Unit
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val skeletonCount = if (isLandscape) 10 else 5

    Column(modifier = Modifier.fillMaxWidth()) {
        SectionTitle(text = stringResource(R.string.section_title_artists))

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            if (artists.isEmpty()) {
                items(
                    count = skeletonCount,
                    key = { "skeleton_artist_$it" }
                ) {
                    ArtistCard(name = "", onClick = {})
                }
            } else {
                items(
                    items = artists,
                    key = { it }
                ) { artist ->
                    ArtistCard(
                        name = artist,
                        onClick = { onItemClick(artist) }
                    )
                }
            }
        }
    }
}

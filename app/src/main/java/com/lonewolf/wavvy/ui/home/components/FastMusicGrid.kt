package com.lonewolf.wavvy.ui.home.components

// Compose layouts and grids
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
// Material icons and components
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.MaterialTheme
// State management
import androidx.compose.runtime.*
// UI utilities
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
// Project resources
import com.lonewolf.wavvy.R
import com.lonewolf.wavvy.ui.common.SectionTitle
import com.lonewolf.wavvy.ui.common.SongOptionsBottomSheet

// Music grid with section header
@Composable
fun FastMusicGrid(
    onItemClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    // State for the more options sheet
    var selectedMusicForOptions by remember { mutableStateOf<String?>(null) }

    Column(modifier = modifier.fillMaxWidth()) {
        // Section title
        SectionTitle(text = stringResource(R.string.section_title_fast_choices))

        // Horizontal music grid
        LazyHorizontalGrid(
            rows = GridCells.Fixed(4),
            modifier = Modifier
                .height(190.dp)
                .fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(
                count = 10,
                key = { index -> "fast_music_$index" },
                contentType = { "fast_music_card" }
            ) { index ->
                val musicName = stringResource(R.string.placeholder_music_name, index + 1)

                FastMusicCard(
                    title = musicName,
                    onClick = { onItemClick(musicName) },
                    onMenuAction = { selectedMusicForOptions = musicName }
                )
            }
        }
    }

    // Show simplified options sheet
    selectedMusicForOptions?.let { musicTitle ->
        SongOptionsBottomSheet(
            songTitle = musicTitle,
            artistName = stringResource(R.string.placeholder_artist_moment),
            isSimplified = true,
            onDismiss = { selectedMusicForOptions = null },
            onActionClick = { action ->
                selectedMusicForOptions = null
            }
        )
    }
}

// Individual music card
@Composable
fun FastMusicCard(
    title: String,
    onClick: () -> Unit,
    onMenuAction: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .width(260.dp)
            .height(42.dp)
            .clip(RoundedCornerShape(6.dp))
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Cover placeholder
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(6.dp))
        )

        Spacer(modifier = Modifier.width(10.dp))

        // Music info placeholders
        Column(modifier = Modifier.weight(1f)) {
            // Title placeholder
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.75f)
                    .height(10.dp)
                    .background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(3.dp))
            )
            Spacer(modifier = Modifier.height(4.dp))
            // Subtitle placeholder
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.45f)
                    .height(8.dp)
                    .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f), RoundedCornerShape(3.dp))
            )
        }

        // Options button
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(20.dp))
                .clickable { onMenuAction() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = stringResource(R.string.cd_more_options),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

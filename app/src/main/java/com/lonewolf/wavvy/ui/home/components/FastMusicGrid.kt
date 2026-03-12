package com.lonewolf.wavvy.ui.home.components

// Compose layouts and grids
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
// Material 3 components
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
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
import com.lonewolf.wavvy.ui.common.components.sections.SectionTitle
import com.lonewolf.wavvy.ui.common.components.sheets.SongOptionsBottomSheet

// Quick choices grid section
@Composable
fun FastMusicGrid(
    onItemClick: (String) -> Unit,
    onPlayAllClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedMusicForOptions by remember { mutableStateOf<String?>(null) }
    val defaultTitle = stringResource(R.string.default_song_title)
    val defaultArtist = stringResource(R.string.default_artist_name)

    Column(modifier = modifier.fillMaxWidth()) {
        // Header with Outlined Play All action
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(end = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            SectionTitle(text = stringResource(R.string.section_title_fast_choices))

            // Adaptable 70% opacity button
            OutlinedButton(
                onClick = onPlayAllClick,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                ),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                modifier = Modifier.height(24.dp)
            ) {
                Text(
                    text = stringResource(R.string.cd_play_all),
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }

        // 4-row horizontal grid
        LazyHorizontalGrid(
            rows = GridCells.Fixed(4),
            modifier = Modifier
                .height(240.dp)
                .fillMaxWidth(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(
                count = 10,
                key = { index -> "fast_music_$index" },
                contentType = { "fast_music_card" }
            ) { _ ->
                FastMusicCard(
                    title = "", // Skeleton mode
                    onClick = { onItemClick(defaultTitle) },
                    onMenuAction = { selectedMusicForOptions = defaultTitle }
                )
            }
        }
    }

    // Song options sheet
    selectedMusicForOptions?.let { musicTitle ->
        SongOptionsBottomSheet(
            songTitle = musicTitle,
            artistName = defaultArtist,
            isSimplified = true,
            onDismiss = { selectedMusicForOptions = null },
            onActionClick = { selectedMusicForOptions = null }
        )
    }
}

// Individual music item
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
            .height(60.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Larger square cover
        Box(
            modifier = Modifier
                .padding(0.5.dp)
                .fillMaxHeight()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        )

        Spacer(modifier = Modifier.width(8.dp))

        // Text placeholders
        Column(modifier = Modifier.weight(1f)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(10.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.45f)
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
            )
        }

        // Action menu
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(20.dp))
                .clickable { onMenuAction() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

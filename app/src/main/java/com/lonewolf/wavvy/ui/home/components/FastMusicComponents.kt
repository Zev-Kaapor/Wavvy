package com.lonewolf.wavvy.ui.home.components

// Compose foundation and layout
import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
// Material 3 and icons
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
// State and UI tools
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
// Project resources
import com.lonewolf.wavvy.R
import com.lonewolf.wavvy.ui.common.components.sections.SectionTitle
import com.lonewolf.wavvy.ui.common.components.sheets.SongOptionsBottomSheet
import com.lonewolf.wavvy.ui.theme.Poppins

// Quick choices grid section
@Composable
fun FastMusicGrid(
    onItemClick: (String) -> Unit,
    onPlayAllClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedMusicForOptions by remember { mutableStateOf<String?>(null) }
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    val defaultTitle = stringResource(R.string.default_song_title)
    val defaultArtist = stringResource(R.string.default_artist_name)

    // Adaptive grid rows and height based on orientation
    val gridRows = if (isLandscape) 2 else 4
    val gridHeight = if (isLandscape) 130.dp else 250.dp

    Column(modifier = modifier.fillMaxWidth()) {
        // Header with Outlined action
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(end = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            SectionTitle(text = stringResource(R.string.section_title_fast_choices))

            OutlinedButton(
                onClick = onPlayAllClick,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                ),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                modifier = Modifier.height(28.dp)
            ) {
                Text(
                    text = stringResource(R.string.cd_play_all),
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                )
            }
        }

        // Adaptive horizontal grid
        LazyHorizontalGrid(
            rows = GridCells.Fixed(gridRows),
            modifier = Modifier
                .height(gridHeight)
                .fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(
                count = if (isLandscape) 12 else 8,
                key = { "fast_item_$it" }
            ) {
                FastMusicCard(
                    title = defaultTitle,
                    isSkeleton = true, // Visual placeholder active
                    onClick = { onItemClick(defaultTitle) },
                    onMenuAction = { selectedMusicForOptions = defaultTitle }
                )
            }
        }
    }

    // Song options sheet logic
    selectedMusicForOptions?.let { musicTitle ->
        SongOptionsBottomSheet(
            songTitle = musicTitle,
            artistName = defaultArtist,
            isSimplified = true,
            onDismiss = { selectedMusicForOptions = null },
            onActionClick = { _ -> selectedMusicForOptions = null }
        )
    }
}

// Individual music item
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FastMusicCard(
    title: String,
    onClick: () -> Unit,
    onMenuAction: () -> Unit,
    modifier: Modifier = Modifier,
    isSkeleton: Boolean = false
) {
    val containerColor = MaterialTheme.colorScheme.surfaceVariant

    Row(
        modifier = modifier
            .width(280.dp)
            .height(56.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(containerColor.copy(alpha = 0.3f))
            // Clicks enabled even in skeleton mode
            .combinedClickable(
                onClick = onClick,
                onLongClick = onMenuAction
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Squared cover
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .aspectRatio(1f)
                .background(containerColor)
        )

        Spacer(modifier = Modifier.width(12.dp))

        // Text content
        Column(modifier = Modifier.weight(1f)) {
            if (isSkeleton) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.6f)
                        .height(10.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(containerColor)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.35f)
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(containerColor.copy(alpha = 0.5f))
                )
            } else {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = Poppins,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Action icon
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(0.dp, 8.dp, 8.dp, 0.dp))
                .combinedClickable(
                    onClick = onMenuAction
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

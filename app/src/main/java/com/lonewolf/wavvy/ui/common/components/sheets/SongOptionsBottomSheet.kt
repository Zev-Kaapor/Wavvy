package com.lonewolf.wavvy.ui.common.components.sheets

// Compose foundation and layout
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
// Material 3 and icons
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.automirrored.rounded.PlaylistPlay
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
// UI tools and state
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
// Project resources
import com.lonewolf.wavvy.R
import com.lonewolf.wavvy.ui.theme.Poppins

// Bottom sheet for song-related actions
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongOptionsBottomSheet(
    songTitle: String,
    artistName: String,
    isSimplified: Boolean = false,
    onDismiss: () -> Unit,
    onActionClick: (String) -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val accentColor = if (isDark) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false),
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = {
            // Standard drag handle
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 8.dp)
                    .size(36.dp, 4.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
            )
        },
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
    ) {
        // Content container with centering logic
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (isLandscape) Modifier
                        .fillMaxWidth(0.65f)
                        .align(Alignment.CenterHorizontally)
                    else Modifier
                )
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(bottom = if (isLandscape) 32.dp else 24.dp)
        ) {
            // Song header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(if (isLandscape) 48.dp else 64.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                )

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = songTitle,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = Poppins,
                            fontWeight = FontWeight.Bold
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = artistName,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = Poppins,
                            color = accentColor,
                            fontWeight = FontWeight.Medium
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Quick action row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                listOf(
                    Triple(Icons.Rounded.AutoAwesome, R.string.player_menu_radio_ia, "radio_ia"),
                    Triple(Icons.Rounded.Info, R.string.player_menu_info, "info"),
                    Triple(Icons.Rounded.Share, R.string.queue_menu_share, "share")
                ).forEach { (icon, label, action) ->
                    QuickActionCard(
                        icon = icon,
                        label = stringResource(label),
                        accentColor = accentColor,
                        modifier = Modifier.weight(1f),
                        onClick = { onActionClick(action) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Options list
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val standardOptions = remember(isSimplified) {
                    mutableListOf(
                        Triple(Icons.Rounded.Download, R.string.player_menu_download, "download"),
                        Triple(Icons.AutoMirrored.Rounded.PlaylistAdd, R.string.queue_menu_add_playlist, "add_playlist")
                    ).apply {
                        if (!isSimplified) {
                            add(Triple(Icons.Rounded.Refresh, R.string.player_menu_reload, "reload"))
                        }
                        addAll(listOf(
                            Triple(Icons.Rounded.RssFeed, R.string.player_menu_radio_normal, "radio_normal"),
                            Triple(Icons.AutoMirrored.Rounded.PlaylistPlay, R.string.queue_menu_play_next_item, "play_next"),
                            Triple(Icons.AutoMirrored.Rounded.QueueMusic, R.string.queue_menu_add_end_item, "add_end")
                        ))
                        if (!isSimplified) {
                            add(Triple(Icons.Rounded.DeleteOutline, R.string.queue_menu_remove_item, "remove_queue"))
                        }
                        add(Triple(Icons.Rounded.Album, R.string.player_menu_view_album, "view_album"))
                    }
                }

                standardOptions.forEach { (icon, label, action) ->
                    OptionListItem(
                        icon = icon,
                        label = stringResource(label),
                        accentColor = accentColor,
                        onClick = { onActionClick(action) }
                    )
                }

                OptionListItem(
                    icon = Icons.Rounded.Person,
                    label = stringResource(R.string.player_menu_view_artist),
                    subLabel = artistName,
                    accentColor = accentColor,
                    onClick = { onActionClick("view_artist") }
                )

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                )

                // External sources title
                Text(
                    text = stringResource(R.string.player_menu_external_source_title),
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontFamily = Poppins,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
                )

                // Streaming platforms
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SourceMiniCard(
                        label = stringResource(R.string.player_menu_spotify),
                        modifier = Modifier.weight(1f),
                        onClick = { onActionClick("open_spotify") }
                    )
                    SourceMiniCard(
                        label = stringResource(R.string.player_menu_yt_music),
                        modifier = Modifier.weight(1f),
                        onClick = { onActionClick("open_yt_music") }
                    )
                }

                if (!isSimplified) {
                    OptionListItem(
                        icon = Icons.Rounded.Settings,
                        label = stringResource(R.string.player_menu_settings),
                        accentColor = accentColor,
                        onClick = { onActionClick("settings") }
                    )
                }
            }
        }
    }
}

// Compact card for primary actions
@Composable
private fun QuickActionCard(
    icon: ImageVector,
    label: String,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .height(82.dp)
            .clip(RoundedCornerShape(24.dp))
            .clickable { onClick() },
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(26.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = Poppins,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                ),
                maxLines = 1
            )
        }
    }
}

// Minimal card for source links
@Composable
private fun SourceMiniCard(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() },
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    ) {
        Box(
            modifier = Modifier.padding(vertical = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontFamily = Poppins,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            )
        }
    }
}

// List style option row
@Composable
private fun OptionListItem(
    icon: ImageVector,
    label: String,
    accentColor: Color,
    subLabel: String? = null,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable { onClick() },
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontFamily = Poppins,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                )
                if (subLabel != null) {
                    Text(
                        text = subLabel,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = Poppins,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp
                        )
                    )
                }
            }
        }
    }
}

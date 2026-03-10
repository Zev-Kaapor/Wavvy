package com.lonewolf.wavvy.ui.common

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
// Project resources
import com.lonewolf.wavvy.R
import com.lonewolf.wavvy.ui.theme.Poppins

// Universal song options sheet
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

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false),
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = {
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
        // Main container
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 24.dp)
        ) {
            // Song info header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Cover placeholder
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                )

                Spacer(modifier = Modifier.width(16.dp))

                // Details
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = songTitle,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = Poppins,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
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

            Spacer(modifier = Modifier.height(20.dp))

            // Quick actions
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                QuickActionCard(
                    icon = Icons.Rounded.AutoAwesome,
                    label = stringResource(R.string.player_menu_radio_ia),
                    accentColor = accentColor,
                    modifier = Modifier.weight(1f),
                    onClick = { onActionClick("radio_ia") }
                )
                QuickActionCard(
                    icon = Icons.Rounded.Info,
                    label = stringResource(R.string.player_menu_info),
                    accentColor = accentColor,
                    modifier = Modifier.weight(1f),
                    onClick = { onActionClick("info") }
                )
                QuickActionCard(
                    icon = Icons.Rounded.Share,
                    label = stringResource(R.string.queue_menu_share),
                    accentColor = accentColor,
                    modifier = Modifier.weight(1f),
                    onClick = { onActionClick("share") }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Main options list
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OptionListItem(
                    icon = Icons.Rounded.Download,
                    label = stringResource(R.string.player_menu_download),
                    accentColor = accentColor,
                    onClick = { onActionClick("download") }
                )

                OptionListItem(
                    icon = Icons.AutoMirrored.Rounded.PlaylistAdd,
                    label = stringResource(R.string.queue_menu_add_playlist),
                    accentColor = accentColor,
                    onClick = { onActionClick("add_playlist") }
                )

                if (!isSimplified) {
                    OptionListItem(
                        icon = Icons.Rounded.Refresh,
                        label = stringResource(R.string.player_menu_reload),
                        accentColor = accentColor,
                        onClick = { onActionClick("reload") }
                    )
                }

                OptionListItem(
                    icon = Icons.Rounded.RssFeed,
                    label = stringResource(R.string.player_menu_radio_normal),
                    accentColor = accentColor,
                    onClick = { onActionClick("radio_normal") }
                )

                OptionListItem(
                    icon = Icons.AutoMirrored.Rounded.PlaylistPlay,
                    label = stringResource(R.string.queue_menu_play_next_item),
                    accentColor = accentColor,
                    onClick = { onActionClick("play_next") }
                )

                OptionListItem(
                    icon = Icons.AutoMirrored.Rounded.QueueMusic,
                    label = stringResource(R.string.queue_menu_add_end_item),
                    accentColor = accentColor,
                    onClick = { onActionClick("add_end") }
                )

                if (!isSimplified) {
                    OptionListItem(
                        icon = Icons.Rounded.DeleteOutline,
                        label = stringResource(R.string.queue_menu_remove_item),
                        accentColor = accentColor,
                        onClick = { onActionClick("remove_queue") }
                    )
                }

                OptionListItem(
                    icon = Icons.Rounded.Person,
                    label = stringResource(R.string.player_menu_view_artist),
                    subLabel = artistName,
                    accentColor = accentColor,
                    onClick = { onActionClick("view_artist") }
                )

                OptionListItem(
                    icon = Icons.Rounded.Album,
                    label = stringResource(R.string.player_menu_view_album),
                    accentColor = accentColor,
                    onClick = { onActionClick("view_album") }
                )

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                )

                // External sources
                Text(
                    text = stringResource(R.string.player_menu_external_source_title),
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontFamily = Poppins,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
                )

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

// Quick action card
@Composable
private fun QuickActionCard(
    icon: ImageVector,
    label: String,
    accentColor: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .height(90.dp)
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
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = Poppins,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface
                ),
                maxLines = 1
            )
        }
    }
}

// Source mini card
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

// Option list item
@Composable
private fun OptionListItem(
    icon: ImageVector,
    label: String,
    accentColor: androidx.compose.ui.graphics.Color,
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
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontFamily = Poppins,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
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

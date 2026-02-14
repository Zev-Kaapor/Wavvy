package com.lonewolf.wavvy.ui.player.components

// Compose foundation and layout
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
// Material 3 and icons
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
// Project resources
import com.lonewolf.wavvy.R
import com.lonewolf.wavvy.ui.theme.Poppins
import com.lonewolf.wavvy.ui.theme.ElectricCyan
import com.lonewolf.wavvy.ui.theme.WavvyTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerMoreOptions(
    songTitle: String,
    artistName: String,
    onDismiss: () -> Unit,
    onActionClick: (String) -> Unit
) {
    WavvyTheme(darkTheme = true) {
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false),
            containerColor = MaterialTheme.colorScheme.surface,
            dragHandle = {
                BottomSheetDefaults.DragHandle(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f))
            },
            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
            ) {
                // Header
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = songTitle,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = Poppins,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        maxLines = 1
                    )
                    Text(
                        text = artistName,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = Poppins,
                            color = ElectricCyan
                        ),
                        maxLines = 1
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Primary Action Capsules
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    QuickActionCard(
                        icon = Icons.Rounded.AutoAwesome,
                        label = stringResource(R.string.player_menu_radio_ia),
                        modifier = Modifier.weight(1f),
                        onClick = { onActionClick("radio_ia") }
                    )
                    QuickActionCard(
                        icon = Icons.Rounded.SmartDisplay,
                        label = stringResource(R.string.player_menu_watch_video),
                        modifier = Modifier.weight(1f),
                        onClick = { onActionClick("watch_video") }
                    )
                    QuickActionCard(
                        icon = Icons.Rounded.AutoFixHigh,
                        label = stringResource(R.string.player_menu_snippet),
                        modifier = Modifier.weight(1f),
                        onClick = { onActionClick("share_snippet") }
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // List Options
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OptionListItem(
                        icon = Icons.Rounded.Info,
                        label = stringResource(R.string.player_menu_info),
                        onClick = { onActionClick("info") }
                    )
                    OptionListItem(
                        icon = Icons.Rounded.GraphicEq,
                        label = stringResource(R.string.player_menu_equalizer),
                        onClick = { onActionClick("equalizer") }
                    )
                    OptionListItem(
                        icon = Icons.Rounded.Timer,
                        label = stringResource(R.string.player_menu_timer),
                        onClick = { onActionClick("timer") }
                    )
                    OptionListItem(
                        icon = Icons.Rounded.RssFeed,
                        label = stringResource(R.string.player_menu_radio_normal),
                        onClick = { onActionClick("radio_normal") }
                    )
                    OptionListItem(
                        icon = Icons.Rounded.Person,
                        label = stringResource(R.string.player_menu_view_artist),
                        subLabel = artistName,
                        onClick = { onActionClick("artist") }
                    )
                    OptionListItem(
                        icon = Icons.Rounded.Album,
                        label = stringResource(R.string.player_menu_view_album),
                        onClick = { onActionClick("album") }
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                    )

                    Text(
                        text = stringResource(R.string.player_menu_external_source_title),
                        fontFamily = Poppins,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
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

                    OptionListItem(
                        icon = Icons.Rounded.Settings,
                        label = stringResource(R.string.player_menu_settings),
                        onClick = { onActionClick("settings") }
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickActionCard(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .height(90.dp)
            .clip(RoundedCornerShape(20.dp))
            .clickable { onClick() },
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = ElectricCyan,
                modifier = Modifier.size(26.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = label,
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = Poppins,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                ),
                maxLines = 1
            )
        }
    }
}

@Composable
private fun SourceMiniCard(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() },
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
    ) {
        Box(
            modifier = Modifier.padding(vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontFamily = Poppins,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                )
            )
        }
    }
}

@Composable
private fun OptionListItem(
    icon: ImageVector,
    label: String,
    subLabel: String? = null,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable { onClick() },
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = ElectricCyan,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = label,
                    color = MaterialTheme.colorScheme.onSurface,
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

package com.lonewolf.wavvy.ui.home.components

// Compose foundation and layout
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
// Material 3 components
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.History
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
// UI utilities
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
// Project resources
import com.lonewolf.wavvy.R
import com.lonewolf.wavvy.data.resize
import com.lonewolf.wavvy.ui.common.components.SectionTitle
import com.lonewolf.wavvy.ui.theme.Poppins

// Data model for recent tracks
data class RecentTrack(
    val id: String,
    val title: String,
    val artist: String,
    val imageUrl: String
)

// Horizontal list of recently played items
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RecentSection(
    modifier: Modifier = Modifier,
    tracks: List<RecentTrack> = emptyList(),
    onItemClick: (RecentTrack) -> Unit
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    val limit = if (isLandscape) 10 else 5
    val limitedTracks = tracks.take(limit)
    val listState = rememberLazyListState()

    // Scroll to the first item when the list changes
    LaunchedEffect(tracks) {
        if (tracks.isNotEmpty()) {
            listState.animateScrollToItem(0)
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        // Section header with navigation indicator
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SectionTitle(
                text = stringResource(R.string.section_title_recent),
                modifier = Modifier.weight(1f)
            )
            if (tracks.isNotEmpty()) {
                IconButton(
                    onClick = { },
                    modifier = Modifier.padding(end = 12.dp, top = 12.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        if (limitedTracks.isEmpty()) {
            // Empty state card
            RecentEmptyState()
        } else {
            // Horizontal scrolling list
            LazyRow(
                state = listState,
                contentPadding = PaddingValues(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(
                    items = limitedTracks,
                    key = { track -> track.id },
                    contentType = { "recent_card" }
                ) { track ->
                    RecentCard(
                        title = track.title,
                        subtitle = track.artist,
                        imageUrl = track.imageUrl,
                        onClick = { onItemClick(track) },
                        modifier = Modifier
                            .animateItem(
                                fadeInSpec = tween(durationMillis = 300),
                                fadeOutSpec = tween(durationMillis = 300),
                                placementSpec = androidx.compose.animation.core.spring(
                                    stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow
                                )
                            )
                    )
                }
            }
        }
    }
}

// Recent item card with touch target optimization
@Composable
fun RecentCard(
    title: String?,
    subtitle: String?,
    imageUrl: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val containerColor = MaterialTheme.colorScheme.surfaceVariant

    var imageLoaded by remember(imageUrl) { mutableStateOf(false) }
    val contentAlpha by animateFloatAsState(
        targetValue = if (imageLoaded || imageUrl.isNullOrBlank()) 1f else 0f,
        animationSpec = tween(durationMillis = 1200),
        label = "recent_card_fade"
    )

    Column(
        modifier = modifier
            .width(156.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(8.dp)
            .graphicsLayer(alpha = contentAlpha)
    ) {
        // Cover image or placeholder background
        Box(
            modifier = Modifier
                .size(140.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(containerColor)
        ) {
            if (!imageUrl.isNullOrBlank()) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(imageUrl.resize(width = 1080, height = 1080))
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                    onSuccess = { imageLoaded = true },
                    onError = { imageLoaded = true }
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        // Title or skeleton
        if (title != null) {
            Text(
                text = title,
                fontFamily = Poppins,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        } else {
            Box(
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .fillMaxWidth(0.8f)
                    .height(12.dp)
                    .clip(CircleShape)
                    .background(containerColor)
            )
        }

        Spacer(Modifier.height(6.dp))

        // Subtitle or skeleton
        if (subtitle != null) {
            Text(
                text = subtitle,
                fontFamily = Poppins,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        } else {
            Box(
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .fillMaxWidth(0.5f)
                    .height(10.dp)
                    .clip(CircleShape)
                    .background(containerColor.copy(alpha = 0.6f))
            )
        }
    }
}

// Default empty state
@Composable
fun RecentEmptyState() {
    Box(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .height(180.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.linearGradient(
                    listOf(
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f)
                    )
                )
            )
            .border(
                1.dp,
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                RoundedCornerShape(20.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp)
        ) {
            // Empty state icon
            Icon(
                imageVector = Icons.Rounded.History,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                modifier = Modifier.size(32.dp)
            )

            Spacer(Modifier.height(12.dp))

            // Information text
            Text(
                text = stringResource(R.string.recent_empty_state),
                fontFamily = Poppins,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )
        }
    }
}

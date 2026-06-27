package com.lonewolf.wavvy.ui.home.components

// UI framework and layouts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
// Icons and Material 3
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.rounded.LiveTv
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Mood
import androidx.compose.material3.*
// Compose state and graphics
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
// Project internal
import com.lonewolf.wavvy.R
import com.lonewolf.wavvy.ui.common.components.SectionTitle
import com.lonewolf.wavvy.ui.theme.Poppins
import com.lonewolf.wavvy.ui.theme.WavvyGradient

// Data models for items
data class PodcastTrack(val id: String, val title: String)
data class LiveTrack(val id: String, val title: String)
data class MoodItemData(val id: String, val name: String)

// Main layout for Wavvy IA, Podcasts and Lives
@Composable
fun FinalPilaresSection(
    onItemClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    podcasts: List<PodcastTrack> = emptyList(),
    lives: List<LiveTrack> = emptyList()
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 110.dp)
    ) {
        // Wavvy IA section
        SectionTitle(text = stringResource(R.string.section_title_wavvy_ia))
        RadioIACard(onItemClick)

        // Podcasts section
        Spacer(modifier = Modifier.height(24.dp))
        SectionTitle(text = stringResource(R.string.section_title_podcasts))
        PodcastsRow(podcasts = podcasts, onItemClick = onItemClick)

        // Lives section
        Spacer(modifier = Modifier.height(24.dp))
        SectionTitle(text = stringResource(R.string.section_title_lives))
        LivesRow(lives = lives, onItemClick = onItemClick)
    }
}

// Featured AI radio card
@Composable
fun RadioIACard(onItemClick: (String) -> Unit) {
    val title = stringResource(R.string.ia_radio_title)
    val subtitle = stringResource(R.string.ia_radio_subtitle)
    val liveBadge = stringResource(R.string.badge_live_now)

    val colors = MaterialTheme.WavvyGradient
    val gradientBrush = remember(colors) { Brush.linearGradient(colors) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(115.dp)
            .clip(RoundedCornerShape(12.dp))
            .drawBehind {
                drawRect(gradientBrush)
            }
            .clickable { onItemClick(title) }
            .padding(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.GraphicEq,
                contentDescription = null,
                modifier = Modifier.size(42.dp),
                tint = MaterialTheme.colorScheme.onPrimary
            )

            Spacer(Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontFamily = Poppins,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    maxLines = 1
                )
                Text(
                    text = subtitle,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f),
                    fontFamily = Poppins,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(end = 40.dp)
                )
            }
        }

        Surface(
            color = MaterialTheme.colorScheme.error,
            shape = RoundedCornerShape(4.dp),
            modifier = Modifier.align(Alignment.TopEnd)
        ) {
            Text(
                text = liveBadge,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                color = MaterialTheme.colorScheme.onError,
                fontSize = 9.sp,
                fontFamily = Poppins,
                fontWeight = FontWeight.ExtraBold
            )
        }
    }
}

// Horizontal podcast list
@Composable
fun PodcastsRow(
    podcasts: List<PodcastTrack>,
    onItemClick: (String) -> Unit
) {
    if (podcasts.isEmpty()) {
        GenericPilarEmptyState(
            icon = Icons.Rounded.Mic,
            text = stringResource(R.string.podcasts_empty_state)
        )
    } else {
        LazyRow(
            contentPadding = PaddingValues(horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(
                items = podcasts,
                key = { podcast -> podcast.id }
            ) { podcast ->
                PodcastItem(title = podcast.title, onClick = { onItemClick(podcast.title) })
            }
        }
    }
}

// Podcast square card with ripple optimization
@Composable
fun PodcastItem(title: String, onClick: () -> Unit) {
    val containerColor = MaterialTheme.colorScheme.surfaceVariant
    val iconColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)

    Column(
        modifier = Modifier
            .width(146.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(8.dp)
    ) {
        // Thumbnail placeholder
        Box(
            modifier = Modifier
                .size(130.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(containerColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.GraphicEq,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(32.dp)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = title,
            fontFamily = Poppins,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
    }
}

// Horizontal lives list
@Composable
fun LivesRow(
    lives: List<LiveTrack>,
    onItemClick: (String) -> Unit
) {
    if (lives.isEmpty()) {
        GenericPilarEmptyState(
            icon = Icons.Rounded.LiveTv,
            text = stringResource(R.string.lives_empty_state)
        )
    } else {
        LazyRow(
            contentPadding = PaddingValues(horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(
                items = lives,
                key = { live -> live.id }
            ) { live ->
                LiveItem(title = live.title, onClick = { onItemClick(live.title) })
            }
        }
    }
}

// Live rectangular card with ripple optimization
@Composable
fun LiveItem(title: String, onClick: () -> Unit) {
    val containerColor = MaterialTheme.colorScheme.surfaceVariant
    val iconColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)

    Column(
        modifier = Modifier
            .width(176.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(8.dp)
    ) {
        // Thumbnail placeholder
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(90.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(containerColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.GraphicEq,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(36.dp)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = title,
            fontFamily = Poppins,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
    }
}

// Section for user moods/vibes
@Composable
fun MoodSection(
    modifier: Modifier = Modifier,
    moods: List<MoodItemData> = emptyList(),
    onItemClick: (String) -> Unit
) {
    Column(modifier = modifier.fillMaxWidth()) {
        SectionTitle(text = stringResource(R.string.section_title_moods))

        if (moods.isEmpty()) {
            GenericPilarEmptyState(
                icon = Icons.Rounded.Mood,
                text = stringResource(R.string.moods_empty_state)
            )
        } else {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(
                    items = moods,
                    key = { mood -> mood.id }
                ) { mood ->
                    MoodItem(name = mood.name, onClick = { onItemClick(mood.name) })
                }
            }
        }
    }
}

// Mood circular item with ripple and support
@Composable
fun MoodItem(name: String, onClick: () -> Unit) {
    val containerColor = MaterialTheme.colorScheme.surfaceVariant

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(96.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(8.dp)
    ) {
        // Circular placeholder
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(containerColor)
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = name,
            fontFamily = Poppins,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

// Reusable empty state placeholder matching app design system guidelines
@Composable
fun GenericPilarEmptyState(
    icon: ImageVector,
    text: String
) {
    Box(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .height(140.dp)
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
            modifier = Modifier.padding(20.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                modifier = Modifier.size(32.dp)
            )

            Spacer(Modifier.height(10.dp))

            Text(
                text = text,
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

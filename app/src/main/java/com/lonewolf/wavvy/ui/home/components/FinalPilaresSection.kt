package com.lonewolf.wavvy.ui.home.components

// UI framework and layouts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
// Icons and Material 3
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material3.*
// Compose state and graphics
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
// Project internal
import com.lonewolf.wavvy.R
import com.lonewolf.wavvy.ui.common.SectionTitle
import com.lonewolf.wavvy.ui.theme.Poppins
import com.lonewolf.wavvy.ui.theme.WavvyGradient

// Main layout for Wavvy IA, Podcasts and Lives
@Composable
fun FinalPilaresSection(
    onItemClick: (String) -> Unit,
    modifier: Modifier = Modifier
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
        PodcastsRow(onItemClick)

        // Lives section
        Spacer(modifier = Modifier.height(24.dp))
        SectionTitle(text = stringResource(R.string.section_title_lives))
        LivesRow(onItemClick)
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
fun PodcastsRow(onItemClick: (String) -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items(
            count = 5,
            key = { index -> "podcast_$index" },
            contentType = { "podcast_card" }
        ) { index ->
            PodcastItem(onClick = { onItemClick("Podcast $index") })
        }
    }
}

// Podcast square card with ripple optimization
@Composable
fun PodcastItem(onClick: () -> Unit) {
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

        // Text skeleton
        Box(
            modifier = Modifier
                .padding(horizontal = 4.dp)
                .width(90.dp)
                .height(12.dp)
                .clip(CircleShape)
                .background(containerColor)
        )
    }
}

// Horizontal lives list
@Composable
fun LivesRow(onItemClick: (String) -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items(
            count = 5,
            key = { index -> "live_$index" },
            contentType = { "live_card" }
        ) { index ->
            LiveItem(onClick = { onItemClick("Live $index") })
        }
    }
}

// Live rectangular card with ripple optimization
@Composable
fun LiveItem(onClick: () -> Unit) {
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

        // Text skeleton
        Box(
            modifier = Modifier
                .padding(horizontal = 4.dp)
                .width(110.dp)
                .height(12.dp)
                .clip(CircleShape)
                .background(containerColor)
        )
    }
}

// Section for user moods/vibes
@Composable
fun MoodSection(onItemClick: (String) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        SectionTitle(text = stringResource(R.string.section_title_moods))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(
                count = 10,
                key = { index -> "mood_$index" },
                contentType = { "mood_pill" }
            ) { index ->
                MoodItem(onClick = { onItemClick("Mood $index") })
            }
        }
    }
}

// Mood circular item with ripple and skeleton support
@Composable
fun MoodItem(onClick: () -> Unit) {
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

        // Label skeleton
        Box(
            modifier = Modifier
                .width(50.dp)
                .height(10.dp)
                .clip(CircleShape)
                .background(containerColor)
        )
    }
}

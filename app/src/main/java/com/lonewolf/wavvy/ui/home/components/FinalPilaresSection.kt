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

    // Gradient optimization
    val colors = MaterialTheme.WavvyGradient
    val gradientBrush = remember(colors) { Brush.linearGradient(colors) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(115.dp)
            .clip(RoundedCornerShape(20.dp))
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
            // Radio icon
            Icon(
                Icons.Default.GraphicEq,
                contentDescription = null,
                modifier = Modifier.size(42.dp),
                tint = MaterialTheme.colorScheme.onPrimary
            )

            Spacer(Modifier.width(16.dp))

            // Card text info
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

        // Status badge
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
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(
            count = 5,
            key = { index -> "podcast_$index" },
            contentType = { "podcast_card" }
        ) { index ->
            val podName = stringResource(R.string.placeholder_podcast, index + 1)
            PodcastItem(name = podName, onClick = { onItemClick(podName) })
        }
    }
}

// Podcast square card
@Composable
fun PodcastItem(name: String, onClick: () -> Unit) {
    val containerColor = MaterialTheme.colorScheme.secondaryContainer
    val primaryColor = MaterialTheme.colorScheme.primary
    val iconColor = remember(primaryColor) { primaryColor.copy(alpha = 0.3f) }

    Column(
        modifier = Modifier
            .width(130.dp)
            .clickable(onClick = onClick)
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
        // Podcast title
        Text(
            text = name,
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = 12.sp,
            fontFamily = Poppins,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            modifier = Modifier.padding(top = 8.dp, start = 4.dp)
        )
    }
}

// Horizontal lives list
@Composable
fun LivesRow(onItemClick: (String) -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(
            count = 5,
            key = { index -> "live_$index" },
            contentType = { "live_card" }
        ) { index ->
            val liveName = stringResource(R.string.placeholder_live, index + 1)
            LiveItem(name = liveName, onClick = { onItemClick(liveName) })
        }
    }
}

// Live stream rectangular card
@Composable
fun LiveItem(name: String, onClick: () -> Unit) {
    val containerColor = MaterialTheme.colorScheme.secondaryContainer
    val primaryColor = MaterialTheme.colorScheme.primary
    val iconColor = remember(primaryColor) { primaryColor.copy(alpha = 0.3f) }

    Column(
        modifier = Modifier
            .width(160.dp)
            .clickable(onClick = onClick)
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
        // Live title
        Text(
            text = name,
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = 12.sp,
            fontFamily = Poppins,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            modifier = Modifier.padding(top = 6.dp)
        )
    }
}

// Section for user moods/vibes
@Composable
fun MoodSection(onItemClick: (String) -> Unit) {
    val context = LocalContext.current

    // Pick 10 random vibes from the extended list
    val vibes = remember {
        context.resources.getStringArray(R.array.vibe_types)
            .toList()
            .shuffled()
            .take(10)
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        // Header
        SectionTitle(text = stringResource(R.string.section_title_moods))
        // Mood list
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(
                items = vibes,
                key = { it },
                contentType = { "mood_pill" }
            ) { vibeName ->
                MoodItem(name = vibeName, onClick = { onItemClick(vibeName) })
            }
        }
    }
}

// Mood circular item
@Composable
fun MoodItem(name: String, onClick: () -> Unit) {
    val containerColor = MaterialTheme.colorScheme.secondaryContainer

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(80.dp)
            .clickable(onClick = onClick)
    ) {
        // Avatar placeholder
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(containerColor)
        )
        Spacer(modifier = Modifier.height(8.dp))
        // Mood name
        Text(
            text = name,
            fontSize = 12.sp,
            fontFamily = Poppins,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}

package com.lonewolf.wavvy.ui.home.components

// Compose foundation and layout
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
// Material 3 components
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
// UI utilities
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
// Project resources
import com.lonewolf.wavvy.R
import com.lonewolf.wavvy.ui.common.components.sections.SectionTitle
import com.lonewolf.wavvy.ui.theme.Poppins

// Horizontal list of recently played items
@Composable
fun RecentSection(onItemClick: (String) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // Section header
        SectionTitle(text = stringResource(R.string.section_title_recent))

        // Horizontal scrolling list
        LazyRow(
            contentPadding = PaddingValues(horizontal = 12.dp), // Adjusted for card padding
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(
                count = 5,
                key = { index -> "recent_$index" },
                contentType = { "recent_card" }
            ) { index ->
                RecentCard(
                    title = null,
                    subtitle = null,
                    onClick = { onItemClick("Item $index") }
                )
            }
        }
    }
}

// Recent item card with touch target optimization
@Composable
fun RecentCard(
    title: String?,
    subtitle: String?,
    onClick: () -> Unit
) {
    val containerColor = MaterialTheme.colorScheme.surfaceVariant

    Column(
        modifier = Modifier
            .width(156.dp) // Width + padding
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(8.dp) // Ripple breathing room
    ) {
        // Cover placeholder
        Box(
            modifier = Modifier
                .size(140.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(containerColor)
        )

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

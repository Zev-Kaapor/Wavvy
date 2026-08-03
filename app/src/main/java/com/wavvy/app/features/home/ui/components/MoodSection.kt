package com.wavvy.app.features.home.ui.components

// Compose foundation and layouts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
// Material icons
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Mood
// Material 3 components
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
// UI utilities and text styles
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
// Project components and theme
import com.wavvy.app.R
import com.wavvy.app.core.designsystem.components.SectionTitle
import com.wavvy.app.core.designsystem.theme.Poppins

// Mood domain model
data class MoodItemData(val id: String, val name: String)

// Moods list section
@Composable
fun MoodSection(
    modifier: Modifier = Modifier,
    moods: List<MoodItemData> = emptyList(),
    onItemClick: (String) -> Unit
) {
    Column(modifier = modifier.fillMaxWidth()) {
        SectionTitle(text = stringResource(R.string.section_title_moods))

        if (moods.isEmpty()) {
            SectionEmptyState(
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

// Mood item card
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

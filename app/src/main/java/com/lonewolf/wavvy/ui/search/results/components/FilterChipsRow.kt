package com.lonewolf.wavvy.ui.search.results.components

// UI framework and layout
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
// Material 3 components
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
// UI utilities
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
// Project resources
import com.lonewolf.wavvy.R
import com.lonewolf.wavvy.ui.theme.Poppins
import com.lonewolf.wavvy.ui.theme.accentCyan

// Available search categories
enum class SearchCategory {
    ALL, SONGS, VIDEOS, ALBUMS, ARTISTS
}

// Adaptive horizontal filter selection
@Composable
fun FilterChipsRow(
    selectedCategory: SearchCategory,
    onCategorySelected: (SearchCategory) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val isDark = isSystemInDarkTheme()

    // Mapping categories
    val categories = listOf(
        SearchCategory.ALL to R.string.search_category_all,
        SearchCategory.SONGS to R.string.search_category_songs,
        SearchCategory.VIDEOS to R.string.search_category_videos,
        SearchCategory.ALBUMS to R.string.search_category_albums,
        SearchCategory.ARTISTS to R.string.search_category_artists
    )

    // Check if scrolling is necessary to allow interaction
    val canScroll = listState.canScrollForward || listState.canScrollBackward

    // List with adaptive scroll behavior
    LazyRow(
        state = listState,
        userScrollEnabled = canScroll,
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = if (canScroll) Arrangement.spacedBy(8.dp) else Arrangement.SpaceBetween
    ) {
        items(
            items = categories,
            key = { it.first.name }
        ) { (category, stringRes) ->
            val isSelected = selectedCategory == category

            val containerColor = if (isSelected) {
                if (isDark) MaterialTheme.accentCyan else Color.Black
            } else {
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)
            }

            val contentColor = if (isSelected) {
                if (isDark) Color.Black else Color.White
            } else {
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(containerColor)
                    .clickable { onCategorySelected(category) }
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Text(
                    text = stringResource(stringRes),
                    fontFamily = Poppins,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                    color = contentColor
                )
            }
        }
    }
}

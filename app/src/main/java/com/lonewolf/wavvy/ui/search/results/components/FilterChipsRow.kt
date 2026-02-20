package com.lonewolf.wavvy.ui.search.results.components

// UI framework and layout
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
// Project resources
import com.lonewolf.wavvy.R
import com.lonewolf.wavvy.ui.theme.Poppins

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

    // Check if scrolling is necessary
    val canScroll = listState.canScrollForward || listState.canScrollBackward

    // Mapping categories
    val categories = listOf(
        SearchCategory.ALL to R.string.search_category_all,
        SearchCategory.SONGS to R.string.search_category_songs,
        SearchCategory.VIDEOS to R.string.search_category_videos,
        SearchCategory.ALBUMS to R.string.search_category_albums,
        SearchCategory.ARTISTS to R.string.search_category_artists
    )

    // List with adaptive scroll
    LazyRow(
        state = listState,
        userScrollEnabled = canScroll,
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = if (canScroll) Arrangement.spacedBy(6.dp) else Arrangement.SpaceEvenly
    ) {
        items(
            items = categories,
            key = { it.first.name }
        ) { (category, stringRes) ->
            val isSelected = selectedCategory == category

            // Adaptive pill item
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        if (isSelected)
                            MaterialTheme.colorScheme.tertiary
                        else
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)
                    )
                    .clickable {
                        onCategorySelected(category)
                    }
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                // Category label
                Text(
                    text = stringResource(stringRes),
                    fontFamily = Poppins,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                    color = if (isSelected)
                        MaterialTheme.colorScheme.onTertiary
                    else
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        }
    }
}

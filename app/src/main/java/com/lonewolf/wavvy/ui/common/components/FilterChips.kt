package com.lonewolf.wavvy.ui.common.components

// UI framework and layout
import android.annotation.SuppressLint
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
// State management
import androidx.compose.runtime.*
// UI utilities
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
// Project resources
import com.lonewolf.wavvy.R
import com.lonewolf.wavvy.ui.theme.Poppins
import com.lonewolf.wavvy.ui.theme.accentCyan

// Search categories
enum class SearchCategory {
    ALL, SONGS, VIDEOS, ALBUMS, ARTISTS
}

// Search filter row
@Composable
fun FilterChipsRow(
    selectedCategory: SearchCategory,
    onCategorySelected: (SearchCategory) -> Unit,
    modifier: Modifier = Modifier
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

    // Category mapping
    val categories = remember {
        listOf(
            SearchCategory.ALL to R.string.search_category_all,
            SearchCategory.SONGS to R.string.search_category_songs,
            SearchCategory.VIDEOS to R.string.search_category_videos,
            SearchCategory.ALBUMS to R.string.search_category_albums,
            SearchCategory.ARTISTS to R.string.search_category_artists
        )
    }

    // Adaptive state reset
    key(isLandscape, categories.size) {
        val listState = rememberLazyListState()
        val canScroll by remember {
            derivedStateOf { listState.canScrollForward || listState.canScrollBackward }
        }

        // Horizontal selection list
        LazyRow(
            state = listState,
            userScrollEnabled = canScroll,
            modifier = modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = when {
                isLandscape -> Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
                canScroll -> Arrangement.spacedBy(8.dp)
                else -> Arrangement.SpaceBetween
            },
            verticalAlignment = Alignment.CenterVertically
        ) {
            items(items = categories, key = { it.first.name }) { (category, stringRes) ->
                FilterChipItem(
                    text = stringResource(stringRes),
                    isSelected = selectedCategory == category,
                    onClick = { onCategorySelected(category) }
                )
            }
        }
    }
}

// Mood and genre pills
@SuppressLint("LocalContextResourcesRead")
@Composable
fun FilterPills(
    availableFilters: List<String>,
    selectedFilter: String,
    onFilterSelected: (String) -> Unit,
    onInitializeFilters: (List<String>) -> Unit,
    modifier: Modifier = Modifier
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

    // Adaptive state reset
    key(isLandscape, availableFilters.size) {
        val listState = rememberLazyListState()
        val canScroll by remember {
            derivedStateOf { listState.canScrollForward || listState.canScrollBackward }
        }

        // Horizontal filter list
        LazyRow(
            state = listState,
            userScrollEnabled = canScroll,
            modifier = modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = when {
                isLandscape -> Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
                canScroll -> Arrangement.spacedBy(8.dp)
                else -> Arrangement.SpaceBetween
            },
            verticalAlignment = Alignment.CenterVertically
        ) {
            items(items = availableFilters, key = { it }) { filterText ->
                val isSelected = selectedFilter == filterText
                FilterChipItem(
                    text = filterText,
                    isSelected = isSelected,
                    onClick = { onFilterSelected(if (isSelected) "" else filterText) }
                )
            }
        }
    }
}

// Atomic chip component
@Composable
private fun FilterChipItem(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val activeColor = MaterialTheme.accentCyan
    val onSurface = MaterialTheme.colorScheme.onSurface

    // Color logic
    val containerColor = remember(isSelected, isDark) {
        if (isSelected) {
            if (isDark) activeColor else Color.Black
        } else {
            onSurface.copy(alpha = 0.06f)
        }
    }

    val contentColor = remember(isSelected, isDark) {
        if (isSelected) {
            if (isDark) Color.Black else Color.White
        } else {
            onSurface.copy(alpha = 0.7f)
        }
    }

    // Chip layout
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(containerColor)
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontFamily = Poppins,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp
            ),
            color = contentColor
        )
    }
}

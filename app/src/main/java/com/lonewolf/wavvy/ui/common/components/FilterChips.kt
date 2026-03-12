package com.lonewolf.wavvy.ui.common.components

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lonewolf.wavvy.R
import com.lonewolf.wavvy.ui.theme.Poppins
import com.lonewolf.wavvy.ui.theme.accentCyan

// Available search categories
enum class SearchCategory {
    ALL, SONGS, VIDEOS, ALBUMS, ARTISTS
}

// Base atomic chip component
@Composable
private fun FilterChipItem(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()

    // Unified color logic
    val containerColor = if (isSelected) {
        if (isDark) MaterialTheme.colorScheme.tertiary else Color.Black
    } else {
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)
    }

    val contentColor = if (isSelected) {
        if (isDark) Color.Black else Color.White
    } else {
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(containerColor)
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        Text(
            text = text,
            fontFamily = Poppins,
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.sp,
            color = contentColor
        )
    }
}

// Search specific filter row
@Composable
fun FilterChipsRow(
    selectedCategory: SearchCategory,
    onCategorySelected: (SearchCategory) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val categories = listOf(
        SearchCategory.ALL to R.string.search_category_all,
        SearchCategory.SONGS to R.string.search_category_songs,
        SearchCategory.VIDEOS to R.string.search_category_videos,
        SearchCategory.ALBUMS to R.string.search_category_albums,
        SearchCategory.ARTISTS to R.string.search_category_artists
    )

    LazyRow(
        state = listState,
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
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

// Moods/Generic string filter row
@SuppressLint("LocalContextResourcesRead")
@Composable
fun FilterPills(
    availableFilters: List<String>,
    selectedFilter: String,
    onFilterSelected: (String) -> Unit,
    onInitializeFilters: (List<String>) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        if (availableFilters.isEmpty()) {
            val newFilters = context.resources.getStringArray(R.array.filter_moods)
                .toList()
                .shuffled()
                .take(5)
            onInitializeFilters(newFilters)
        }
    }

    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
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

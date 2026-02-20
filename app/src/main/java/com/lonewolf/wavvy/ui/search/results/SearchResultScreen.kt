package com.lonewolf.wavvy.ui.search.results

// Compose foundation and layout
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
// Project components
import com.lonewolf.wavvy.ui.search.results.components.FilterChipsRow
import com.lonewolf.wavvy.ui.search.results.components.SearchCategory

// Screen to display search results with category filtering
@Composable
fun SearchResultScreen(
    query: String,
    onBack: () -> Unit
) {
    // Category state management
    var selectedCategory by rememberSaveable { mutableStateOf(SearchCategory.ALL) }

    Column(modifier = Modifier.fillMaxSize()) {
        // Category filter pills
        FilterChipsRow(
            selectedCategory = selectedCategory,
            onCategorySelected = { selectedCategory = it },
            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
        )

        // Main results scrollable area
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            when (selectedCategory) {
                SearchCategory.ALL -> {
                    // TODO: Add Top Result component
                    // TODO: Add horizontal Artists row
                    // TODO: Add Songs preview list
                }
                SearchCategory.SONGS -> {
                    // TODO: Full list of song results
                }
                SearchCategory.VIDEOS -> {
                    // TODO: Full list of video results
                }
                SearchCategory.ALBUMS -> {
                    // TODO: Grid of albums
                }
                SearchCategory.ARTISTS -> {
                    // TODO: List of artists with follow button
                }
            }
        }
    }
}

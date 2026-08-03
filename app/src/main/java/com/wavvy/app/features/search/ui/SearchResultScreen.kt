package com.wavvy.app.features.search.ui

// Compose foundation and layout
import androidx.compose.foundation.layout.*
// State and composition utilities
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
// Project resources
import com.wavvy.app.R
import com.wavvy.app.core.designsystem.components.FilterChipsRow
import com.wavvy.app.core.designsystem.components.FilterPills
import com.wavvy.app.core.designsystem.components.SearchCategory
// Project components
import com.wavvy.app.features.home.ui.PlayerState
import com.wavvy.app.features.search.ui.components.SearchResultData
import com.wavvy.app.features.search.ui.components.SearchResultList

// Main search result screen coordinator
@Composable
fun SearchResultScreen(
    playerState: PlayerState
) {
    var selectedCategory by rememberSaveable { mutableStateOf(SearchCategory.ALL) }
    var selectedFilter by rememberSaveable { mutableStateOf("") }

    // UI state for search results
    val searchResults = remember { mutableStateListOf<SearchResultData>() }

    // Filters available for the current category
    val availableFilters = remember(selectedCategory) {
        filtersForCategory(selectedCategory)
    }

    // Default strings for player activation
    val defaultArtist = stringResource(R.string.default_artist_name)
    val defaultSong = stringResource(R.string.default_song_title)
    val bestResultLabel = stringResource(R.string.search_best_result)

    Column(modifier = Modifier.fillMaxSize()) {
        // Category selection
        FilterChipsRow(
            selectedCategory = selectedCategory,
            onCategorySelected = {
                selectedCategory = it
                selectedFilter = ""
            },
            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
        )

        // Secondary filter selection within category
        FilterPills(
            availableFilters = availableFilters,
            selectedFilter = selectedFilter,
            onFilterSelected = { selectedFilter = it },
            onInitializeFilters = { filters ->
                selectedFilter = filters.first()
            },
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // Unified result list component
        SearchResultList(
            category = selectedCategory,
            results = searchResults,
            onItemClick = { id ->
                // Updating playback with list of artists as required by PlayerState
                playerState.updatePlayback(
                    title = if (selectedCategory == SearchCategory.ALL) bestResultLabel else defaultSong,
                    artists = listOf(defaultArtist)
                )
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}

// Static filter set per search category
private fun filtersForCategory(category: SearchCategory): List<String> = when (category) {
    SearchCategory.ALL -> listOf("Trending", "Recent", "For You")
    SearchCategory.SONGS -> listOf("Trending", "New Releases")
    SearchCategory.VIDEOS -> listOf("Music Videos", "Live")
    SearchCategory.ALBUMS -> listOf("New Releases", "Top Albums")
    SearchCategory.ARTISTS -> listOf("Popular", "Following")
}

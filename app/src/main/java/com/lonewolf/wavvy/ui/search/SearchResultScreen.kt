package com.lonewolf.wavvy.ui.search

// Compose foundation and layout
import androidx.compose.foundation.layout.*
// State and composition utilities
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
// Project resources
import com.lonewolf.wavvy.R
import com.lonewolf.wavvy.ui.common.components.FilterChipsRow
import com.lonewolf.wavvy.ui.common.components.SearchCategory
// Project components
import com.lonewolf.wavvy.ui.home.PlayerState
import com.lonewolf.wavvy.ui.search.components.SearchResultData
import com.lonewolf.wavvy.ui.search.components.SearchResultList

// Main search result screen coordinator
@Composable
fun SearchResultScreen(
    playerState: PlayerState
) {
    var selectedCategory by rememberSaveable { mutableStateOf(SearchCategory.ALL) }

    // UI state for search results (replace with viewModel state later)
    val searchResults = remember { mutableStateListOf<SearchResultData>() }

    // Default strings for player activation
    val defaultArtist = stringResource(R.string.default_artist_name)
    val defaultSong = stringResource(R.string.default_song_title)
    val bestResultLabel = stringResource(R.string.search_best_result)

    Column(modifier = Modifier.fillMaxSize()) {
        // Category selection
        FilterChipsRow(
            selectedCategory = selectedCategory,
            onCategorySelected = { selectedCategory = it },
            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
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

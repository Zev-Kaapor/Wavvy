package com.lonewolf.wavvy.ui.search.results

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
import com.lonewolf.wavvy.ui.search.results.components.*

// Main search result screen coordinator
@Composable
fun SearchResultScreen(
    query: String,
    playerState: PlayerState,
    onBack: () -> Unit
) {
    var selectedCategory by rememberSaveable { mutableStateOf(SearchCategory.ALL) }

    // Placeholder strings for player activation
    val wavvyArtist = stringResource(R.string.placeholder_wavvy_artist)
    val wavvySong = stringResource(R.string.placeholder_wavvy_song)
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
            onItemClick = {
                // Logic based on category could be added here if needed
                playerState.updatePlayback(
                    title = if (selectedCategory == SearchCategory.ALL) bestResultLabel else wavvySong,
                    artist = wavvyArtist
                )
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}

package com.wavvy.app.features.search.ui

// Compose layouts and foundations
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
// Compose state and runtime
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
// UI utilities
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
// Lifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
// Project resources
import com.wavvy.app.features.home.data.HomeItem
import com.wavvy.app.features.home.data.HomeItemKind
import com.wavvy.app.features.home.ui.rememberItemPlayer
import com.wavvy.app.features.search.ui.components.SearchBar
import com.wavvy.app.features.search.ui.components.SearchHistory
import com.wavvy.app.features.search.ui.components.SearchResults
import com.wavvy.app.features.search.ui.components.SearchSuggestionList

// Explore tab, the field on top and under it the history, the words that complete what is typed or the results
@Composable
fun SearchScreen(
    modifier: Modifier = Modifier,
    viewModel: SearchViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()
    val focusManager = LocalFocusManager.current
    var isFocused by remember { mutableStateOf(false) }

    // Songs and episodes play, the other cards do nothing until they have pages
    val play = rememberItemPlayer()

    // Searches the words and takes the keyboard away so the results fill the screen
    val search: (String) -> Unit = { text ->
        viewModel.submit(text)
        focusManager.clearFocus()
    }

    // A card that is not a song has no page yet, so it searches for its name
    val onItemClick: (HomeItem) -> Unit = { item ->
        if (item.kind == HomeItemKind.Song || item.kind == HomeItemKind.Episode) play(item) else search(item.title)
    }

    // The history is asked again whenever the tab opens, so what changed in other places shows up
    LaunchedEffect(Unit) { viewModel.refreshAccountHistory() }

    val isActive = isFocused || state.query.isNotEmpty() || state.submitted != null

    // Back first leaves the results and the words, and only then leaves the tab
    BackHandler(enabled = isActive) {
        viewModel.back()
        focusManager.clearFocus()
    }

    Column(modifier = modifier.fillMaxSize()) {
        SearchBar(
            query = state.query,
            isActive = isActive,
            onQueryChange = viewModel::onQueryChange,
            onSearch = { search(state.query) },
            onBack = {
                viewModel.back()
                focusManager.clearFocus()
            },
            onFocusChange = { focus ->
                isFocused = focus.isFocused
                if (focus.isFocused && state.query.isEmpty()) viewModel.refreshAccountHistory()
                if (focus.isFocused && state.submitted != null) viewModel.openSuggestions()
            }
        )

        Box(modifier = Modifier.weight(1f).imePadding()) {
            when {
                state.submitted != null -> SearchResults(
                    state = state,
                    onCategoryClick = viewModel::selectCategory,
                    onLoadMore = viewModel::loadMore,
                    onRetry = viewModel::retry,
                    onItemClick = onItemClick
                )

                state.query.isNotBlank() -> SearchSuggestionList(
                    suggestions = state.suggestions,
                    isLoading = state.isSuggesting,
                    onSearch = search,
                    onInsert = viewModel::onQueryChange,
                    onItemClick = onItemClick
                )

                else -> SearchHistory(
                    history = state.history,
                    onSearch = search,
                    onInsert = viewModel::onQueryChange,
                    onRemove = viewModel::removeFromHistory,
                    onClear = viewModel::clearHistory
                )
            }
        }
    }
}

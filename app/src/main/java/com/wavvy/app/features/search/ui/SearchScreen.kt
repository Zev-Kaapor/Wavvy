package com.wavvy.app.features.search.ui

// Compose layouts and foundations
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
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
import com.wavvy.app.core.designsystem.components.OverlaySheet
import com.wavvy.app.core.recognition.SoundSearchRequest
import com.wavvy.app.features.home.data.HomeItem
import com.wavvy.app.features.home.data.HomeItemKind
import com.wavvy.app.features.home.ui.rememberItemPlayer
import com.wavvy.app.features.profile.ui.LocalProfile
import com.wavvy.app.features.search.ui.components.SearchBar
import com.wavvy.app.features.search.ui.components.SearchHistory
import com.wavvy.app.features.search.ui.components.SearchResults
import com.wavvy.app.features.search.ui.components.SearchSuggestionList
import com.wavvy.app.features.search.ui.components.SoundSearchOverlay
import com.wavvy.app.features.settings.data.PrivacyPrefs

// Explore tab, the field on top and under it the history, the words that complete what is typed or the results
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    modifier: Modifier = Modifier,
    viewModel: SearchViewModel = viewModel()
) {
    // Coming back from another tab starts from the empty search, before the first frame so the old results never show
    remember { viewModel.resetIfLeft() }
    val state by viewModel.state.collectAsState()

    // The searches of the account are taken out one by one while signed in, so there is no button to clear them all
    val isSynced = LocalProfile.current.isSignedIn && !PrivacyPrefs.pauseSearchHistory
    val focusManager = LocalFocusManager.current
    var isFocused by remember { mutableStateOf(false) }

    // Songs and episodes play, the other cards do nothing until they have pages
    val play = rememberItemPlayer()

    // Searches the words and takes the keyboard away so the results fill the screen
    val search: (String) -> Unit = { text ->
        viewModel.submit(text)
        focusManager.clearFocus()
    }

    // A song found by its sound puts its song before its video at the top, as the sound is often found on the video
    val searchSong: (String) -> Unit = { text ->
        viewModel.submit(text, prefersSong = true)
        focusManager.clearFocus()
    }

    // Podcasts have no page yet, so they search for their name
    val onItemClick: (HomeItem) -> Unit = { item ->
        if (item.kind == HomeItemKind.Podcast) search(item.title) else play(item)
    }

    // Listens for the song, and what the tile of the quick settings found over another app is searched here
    val openSound = {
        focusManager.clearFocus()
        OverlaySheet.show { SoundSearchOverlay(onFound = searchSong, onDismiss = OverlaySheet::dismiss) }
    }
    // Writes down what is said and searches it
    val openVoice = {
        focusManager.clearFocus()
        OverlaySheet.show { SoundSearchOverlay(onFound = search, onDismiss = OverlaySheet::dismiss, listensToSpeech = true) }
    }
    val soundFound by SoundSearchRequest.pending.collectAsState()
    LaunchedEffect(soundFound) {
        soundFound?.let { query ->
            SoundSearchRequest.take()
            searchSong(query)
        }
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
            onQueryChange = viewModel::onQueryChange,
            onSearch = { search(state.query) },
            // A tab has no arrow, the back gesture leaves the results and the words
            onBack = null,
            onFocusChange = { focus ->
                isFocused = focus.isFocused
                if (focus.isFocused && state.query.isEmpty()) viewModel.refreshAccountHistory()
                if (focus.isFocused && state.submitted != null) viewModel.openSuggestions()
            },
            // What is said is written down and searched, and the song that plays near the phone is found by its sound and then searched
            onVoiceSearch = openVoice,
            onSoundSearch = openSound
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

                // Pulled down, the history and the suggestions are asked again
                else -> PullToRefreshBox(isRefreshing = state.isRefreshing, onRefresh = viewModel::refresh) {
                    SearchHistory(
                        history = state.history,
                        suggested = state.suggested,
                        onSearch = search,
                        onInsert = viewModel::onQueryChange,
                        onRemove = viewModel::removeFromHistory,
                        onClear = viewModel::clearHistory.takeUnless { isSynced }
                    )
                }
            }
        }
    }
}

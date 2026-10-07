package com.wavvy.app.features.search.ui

// Android application and view model
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
// Coroutines and reactive flows
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
// Project resources
import com.wavvy.app.core.history.PlayHistory
import com.wavvy.app.features.home.data.HomeItem
import com.wavvy.app.features.search.data.SearchCategory
import com.wavvy.app.features.search.data.SearchRepository
import com.wavvy.app.features.search.data.SearchSuggestion

// What the results are doing, nothing asked, waiting for the answer, showing it or failed
enum class SearchStatus { Idle, Loading, Content, Error }

// Everything the search screen draws
data class SearchUiState(
    // What is in the field, and the words the results shown are for, empty while the user is still typing
    val query: String = "",
    val submitted: String? = null,
    val category: SearchCategory = SearchCategory.All,
    val suggestions: List<SearchSuggestion> = emptyList(),
    val isSuggesting: Boolean = false,
    val status: SearchStatus = SearchStatus.Idle,
    val topMatch: HomeItem? = null,
    val items: List<HomeItem> = emptyList(),
    val continuation: String? = null,
    val isLoadingMore: Boolean = false,
    // The searches made before, the most recent first
    val history: List<String> = emptyList()
)

// Follows what is typed, asks for the words under the field and the results, and keeps the searches that were made
class SearchViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = SearchRepository(application)
    private val mutableState = MutableStateFlow(SearchUiState())
    val state: StateFlow<SearchUiState> = mutableState.asStateFlow()

    // The requests that are running, so a new one replaces them
    private var suggestionsJob: Job? = null
    private var searchJob: Job? = null
    private var moreJob: Job? = null

    // The searches kept on the device and the ones kept by the account, the history shows both
    private var localHistory: List<String> = emptyList()
    private var accountHistory: List<SearchSuggestion.Words> = emptyList()

    // The older searches of the account and when they were asked, they are asked again only after a while
    private var olderHistory: List<SearchSuggestion.Words> = emptyList()
    private var probedAt = 0L

    // True once the account answered, from then on it is the one that says what the history is
    private var followsAccount = false

    init {
        viewModelScope.launch {
            PlayHistory.searches(application, SearchDimens.HistoryLimit).collect { searches ->
                localHistory = searches
                publishHistory()
            }
        }
        refreshAccountHistory()
    }

    // The text of the field changed, leaving the results and asking for the words that complete it
    fun onQueryChange(text: String) {
        mutableState.update { it.copy(query = text, submitted = null) }
        suggest(text)
    }

    // The field was touched while the results were shown, so the words come back
    fun openSuggestions() {
        val query = mutableState.value.query
        mutableState.update { it.copy(submitted = null) }
        suggest(query)
    }

    // Searches for the words, keeps them in the history and shows the results of everything
    fun submit(text: String) {
        val query = text.trim()
        if (query.isEmpty()) return

        suggestionsJob?.cancel()
        viewModelScope.launch(Dispatchers.IO) { PlayHistory.saveSearch(getApplication(), query) }
        mutableState.update { it.copy(query = query, submitted = query, suggestions = emptyList(), isSuggesting = false) }
        load(query, SearchCategory.All)
    }

    // Shows only one kind of result, the same search under another filter
    fun selectCategory(category: SearchCategory) {
        val query = mutableState.value.submitted ?: return
        if (category == mutableState.value.category) return

        load(query, category)
    }

    // Asks the same search again after a failure
    fun retry() {
        val state = mutableState.value
        state.submitted?.let { load(it, state.category) }
    }

    // Adds the next page of a filter, called when the list gets close to its end, the search of everything has no more pages
    fun loadMore() {
        val state = mutableState.value
        val continuation = state.continuation
        if (continuation == null || state.isLoadingMore || state.status != SearchStatus.Content) return

        mutableState.update { it.copy(isLoadingMore = true) }
        moreJob = viewModelScope.launch {
            repository.more(continuation)
                .onSuccess { page ->
                    mutableState.update { current ->
                        val known = current.items.map { it.id }.toSet()
                        current.copy(
                            items = current.items + page.items.filter { it.id !in known },
                            continuation = page.continuation,
                            isLoadingMore = false
                        )
                    }
                }
                .onFailure { mutableState.update { it.copy(isLoadingMore = false) } }
        }
    }

    // Goes one step back, from the results to the empty search, true when there was a step to go back
    fun back(): Boolean {
        val state = mutableState.value
        if (state.submitted == null && state.query.isEmpty()) return false

        clear()
        return true
    }

    // Takes one search out of the history
    fun removeFromHistory(query: String) {
        val tokens = accountHistory.filter { it.text.equals(query, ignoreCase = true) }.mapNotNull { it.feedbackToken }
        accountHistory = accountHistory.filterNot { it.text.equals(query, ignoreCase = true) }
        olderHistory = olderHistory.filterNot { it.text.equals(query, ignoreCase = true) }
        publishHistory()

        viewModelScope.launch(Dispatchers.IO) {
            PlayHistory.removeSearch(getApplication(), query)
            repository.removeFromAccount(tokens)
        }
    }

    // Takes every search out of the history
    fun clearHistory() {
        val tokens = accountHistory.mapNotNull { it.feedbackToken }
        accountHistory = emptyList()
        olderHistory = emptyList()
        publishHistory()

        viewModelScope.launch(Dispatchers.IO) {
            PlayHistory.clearSearches(getApplication())
            repository.removeFromAccount(tokens)
        }
    }

    // Asks the account for its searches, the ones made on other devices come in and the ones made here are confirmed
    fun refreshAccountHistory() {
        viewModelScope.launch {
            val probe = System.currentTimeMillis() - probedAt > SearchDimens.HistoryProbeKeepMillis
            repository.accountHistory(probe).onSuccess { history ->
                followsAccount = history != null
                if (history != null && probe) {
                    olderHistory = history.older
                    probedAt = System.currentTimeMillis()
                }

                // What was asked earlier stays, minus what the latest ones already bring
                val recent = history?.recent.orEmpty()
                val known = recent.map { it.text.lowercase() }.toSet()
                accountHistory = if (history == null) emptyList() else recent + olderHistory.filter { it.text.lowercase() !in known }
                publishHistory()
            }
        }
    }

    // Signed in, the history is the one of the account so what is removed in one place leaves the other, signed out it is the one of the device
    private fun publishHistory() {
        val history = if (followsAccount) accountHistory.map { it.text } else localHistory

        mutableState.update { it.copy(history = history.take(SearchDimens.HistoryLimit)) }
    }

    // Back to the empty search, the history stays
    private fun clear() {
        suggestionsJob?.cancel()
        searchJob?.cancel()
        moreJob?.cancel()
        mutableState.update {
            SearchUiState(history = it.history)
        }
    }

    // Asks for the words that complete what was typed, a little after the last letter so each letter does not make a request
    private fun suggest(text: String) {
        suggestionsJob?.cancel()
        if (text.isBlank()) {
            mutableState.update { it.copy(suggestions = emptyList(), isSuggesting = false) }
            return
        }

        mutableState.update { it.copy(isSuggesting = true) }
        suggestionsJob = viewModelScope.launch {
            delay(SearchDimens.SuggestionDelayMillis)
            repository.suggestions(text.trim()).onSuccess { suggestions ->
                mutableState.update { it.copy(suggestions = suggestions) }
            }
            mutableState.update { it.copy(isSuggesting = false) }
        }
    }

    // The first page of a search under a filter, what was on the screen is replaced
    private fun load(query: String, category: SearchCategory) {
        searchJob?.cancel()
        moreJob?.cancel()
        mutableState.update {
            it.copy(
                category = category,
                status = SearchStatus.Loading,
                topMatch = null,
                items = emptyList(),
                continuation = null,
                isLoadingMore = false
            )
        }

        searchJob = viewModelScope.launch {
            repository.search(query, category)
                .onSuccess { page ->
                    mutableState.update {
                        it.copy(
                            status = SearchStatus.Content,
                            topMatch = page.topMatch,
                            items = page.items.distinctBy { item -> item.id },
                            continuation = page.continuation
                        )
                    }

                    // The account has just kept this search
                    if (category == SearchCategory.All) refreshAccountHistory()
                }
                .onFailure { mutableState.update { it.copy(status = SearchStatus.Error) } }
        }
    }
}

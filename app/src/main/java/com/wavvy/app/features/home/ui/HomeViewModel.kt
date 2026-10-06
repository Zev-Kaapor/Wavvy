package com.wavvy.app.features.home.ui

// Android application and view model
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
// Coroutines and reactive flows
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
// Project resources
import com.wavvy.app.features.home.data.HomeFilter
import com.wavvy.app.features.home.data.HomeItem
import com.wavvy.app.features.home.data.HomeRepository
import com.wavvy.app.features.home.data.HomeSection

// What the Home is doing, loading with nothing to show, showing what came, or failed with nothing to show
enum class HomeStatus { Loading, Content, Error }

// Everything the Home screen draws
data class HomeUiState(
    val status: HomeStatus = HomeStatus.Loading,
    val filters: List<HomeFilter> = emptyList(),
    val selectedFilter: HomeFilter? = null,
    val sections: List<HomeSection> = emptyList(),
    // Playlists of the account, shown above the shelves of YouTube Music when no filter is selected
    val accountPlaylists: List<HomeItem> = emptyList(),
    val continuation: String? = null,
    val isLoadingMore: Boolean = false,
    val isRefreshing: Boolean = false
)

// Loads the Home when it opens and follows the filters, the next pages and the refresh
class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = HomeRepository(application)
    private val mutableState = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = mutableState.asStateFlow()

    // The requests that are running, so a new one replaces them
    private var loadJob: Job? = null
    private var accountJob: Job? = null

    init {
        load()
    }

    // Opens the Home under the selected filter, from the start
    fun load() {
        mutableState.update { it.copy(status = HomeStatus.Loading, sections = emptyList(), continuation = null, isLoadingMore = false) }
        request(refreshing = false)
    }

    // Asks again without taking what is on the screen away, a failure keeps it
    fun refresh() {
        mutableState.update { it.copy(isRefreshing = true) }
        request(refreshing = true)
    }

    // Selects a filter, or goes back to the whole Home when it is the one already selected
    fun selectFilter(filter: HomeFilter) {
        mutableState.update { it.copy(selectedFilter = if (it.selectedFilter == filter) null else filter) }
        load()
    }

    // Adds the next page of shelves, called when the list gets close to its end
    fun loadMore() {
        val current = mutableState.value
        val continuation = current.continuation
        if (continuation == null || current.isLoadingMore || current.status != HomeStatus.Content) return

        mutableState.update { it.copy(isLoadingMore = true) }
        loadJob = viewModelScope.launch {
            repository.load(continuation = continuation)
                .onSuccess { page ->
                    mutableState.update { state ->
                        val known = state.sections.map { it.title }.toSet()
                        state.copy(
                            sections = state.sections + page.sections.filter { it.title !in known },
                            continuation = page.continuation,
                            isLoadingMore = false
                        )
                    }
                }
                .onFailure { mutableState.update { it.copy(isLoadingMore = false) } }
        }
    }

    // Playlists of the account, asked next to the first page and left out when a filter is selected
    private fun requestAccountPlaylists() {
        accountJob?.cancel()
        if (mutableState.value.selectedFilter != null) {
            mutableState.update { it.copy(accountPlaylists = emptyList()) }
            return
        }

        accountJob = viewModelScope.launch {
            // A failure keeps the playlists that are already there
            repository.accountPlaylists().onSuccess { playlists ->
                mutableState.update { it.copy(accountPlaylists = playlists) }
            }
        }
    }

    // First page of the Home under the selected filter
    private fun request(refreshing: Boolean) {
        requestAccountPlaylists()
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            repository.load(params = mutableState.value.selectedFilter?.params)
                .onSuccess { page ->
                    mutableState.update { state ->
                        state.copy(
                            status = HomeStatus.Content,
                            // The filters of the first answer stay, the answer under a filter brings none worth showing
                            filters = state.filters.ifEmpty { page.filters },
                            sections = page.sections,
                            continuation = page.continuation,
                            isRefreshing = false
                        )
                    }
                }
                .onFailure {
                    mutableState.update { state ->
                        // On a refresh the shelves that are there stay, otherwise the screen shows the error
                        if (refreshing && state.sections.isNotEmpty()) {
                            state.copy(isRefreshing = false)
                        } else {
                            state.copy(status = HomeStatus.Error, isRefreshing = false)
                        }
                    }
                }
        }
    }
}

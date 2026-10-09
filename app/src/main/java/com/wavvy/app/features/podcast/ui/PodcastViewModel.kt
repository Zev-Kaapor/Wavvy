package com.wavvy.app.features.podcast.ui

// Android application and view model
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
// Coroutines and reactive flows
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
// Project resources
import com.wavvy.app.core.navigation.PodcastIdArg
import com.wavvy.app.features.home.data.HomeItem
import com.wavvy.app.features.podcast.data.PodcastEpisodes
import com.wavvy.app.features.podcast.data.PodcastPage
import com.wavvy.app.features.podcast.data.PodcastRepository
import com.wavvy.app.features.podcast.data.PodcastSort

// What the page is doing, waiting for the answer, showing it or failed
enum class PodcastStatus { Loading, Content, Error }

// Everything the page of a podcast draws
data class PodcastUiState(
    val status: PodcastStatus = PodcastStatus.Loading,
    val page: PodcastPage? = null,
    val episodes: List<HomeItem> = emptyList(),
    val continuation: String? = null,
    // The message of a list with no episodes under the filter that was chosen
    val message: String? = null,
    // The filter that is on, its place in the row of buttons, and the order of the episodes in use
    val filter: Int? = null,
    val sort: PodcastSort? = null,
    // The episodes are being asked again under a filter or an order, or the next ones are coming
    val isReloading: Boolean = false,
    val isLoadingMore: Boolean = false,
    // The field that looks for episodes is open, and what was typed in it
    val isSearching: Boolean = false,
    val query: String = ""
)

// Loads the page of a podcast and asks its episodes again when a filter or an order is chosen, and the next ones near the end
class PodcastViewModel(
    application: Application,
    savedState: SavedStateHandle
) : AndroidViewModel(application) {
    val id: String = requireNotNull(savedState[PodcastIdArg])
    private val repository = PodcastRepository(application)

    private val mutableState = MutableStateFlow(PodcastUiState())
    val state: StateFlow<PodcastUiState> = mutableState.asStateFlow()

    // The request of the episodes that is running, so a new choice replaces it
    private var episodesJob: Job? = null

    init {
        load()
    }

    // Asks the page, again after a failure
    fun load() {
        mutableState.update { PodcastUiState() }
        viewModelScope.launch {
            repository.load(id)
                .onSuccess { page ->
                    mutableState.update {
                        PodcastUiState(
                            status = PodcastStatus.Content,
                            page = page,
                            episodes = page.episodes.episodes.map { it.copy(author = it.author ?: page.author) },
                            continuation = page.episodes.continuation,
                            message = page.episodes.message,
                            sort = page.chips.firstOrNull()?.sorts?.firstOrNull { it.isSelected }
                        )
                    }
                }
                .onFailure { mutableState.update { PodcastUiState(status = PodcastStatus.Error) } }
        }
    }

    // Opens the field that looks for episodes among the ones loaded, the next ones keep coming while something is typed
    fun startSearch() {
        mutableState.update { it.copy(isSearching = true, query = "") }
    }

    fun stopSearch() {
        mutableState.update { it.copy(isSearching = false, query = "") }
    }

    fun setQuery(query: String) {
        mutableState.update { it.copy(query = query) }
    }

    // Turns a filter on, or off when it was on, the order stays
    fun selectFilter(index: Int) {
        val current = mutableState.value
        mutableState.update { it.copy(filter = if (current.filter == index) null else index) }
        reload()
    }

    // Orders the episodes another way, which puts the filter out
    fun selectSort(sort: PodcastSort) {
        mutableState.update { it.copy(sort = sort, filter = null) }
        reload()
    }

    // The episodes under the filter that is on, or under the order when there is none
    private fun reload() {
        val state = mutableState.value
        val page = state.page ?: return
        val token = state.filter?.let { page.chips.getOrNull(it)?.token } ?: state.sort?.token

        episodesJob?.cancel()
        mutableState.update { it.copy(isReloading = true, isLoadingMore = false) }
        episodesJob = viewModelScope.launch {
            repository.episodes(token ?: return@launch)
                .onSuccess(::showEpisodes)
                .onFailure { mutableState.update { it.copy(isReloading = false) } }
        }
    }

    // The next episodes when the end of the list is near
    fun loadMore() {
        val state = mutableState.value
        val token = state.continuation
        if (token == null || state.isLoadingMore || state.isReloading) return

        mutableState.update { it.copy(isLoadingMore = true) }
        episodesJob = viewModelScope.launch {
            repository.episodes(token)
                .onSuccess { more ->
                    mutableState.update { current ->
                        val known = current.episodes.map { it.id }.toSet()
                        current.copy(
                            episodes = current.episodes + more.episodes.filter { it.id !in known }.map { episode -> episode.copy(author = episode.author ?: current.page?.author) },
                            continuation = more.continuation,
                            isLoadingMore = false
                        )
                    }
                }
                .onFailure { mutableState.update { it.copy(isLoadingMore = false) } }
        }
    }

    private fun showEpisodes(episodes: PodcastEpisodes) {
        mutableState.update {
            it.copy(
                episodes = episodes.episodes.map { episode -> episode.copy(author = episode.author ?: it.page?.author) },
                continuation = episodes.continuation,
                message = episodes.message,
                isReloading = false
            )
        }
    }
}

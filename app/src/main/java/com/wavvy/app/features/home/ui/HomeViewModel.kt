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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
// Project resources
import com.wavvy.app.core.history.PinnedEntity
import com.wavvy.app.core.history.PlayHistory
import com.wavvy.app.core.history.SongEntity
import com.wavvy.app.core.playback.PlayableTrack
import com.wavvy.app.core.playback.RadioQueue
import com.wavvy.app.features.home.data.HomeFilter
import com.wavvy.app.features.home.data.HomeItem
import com.wavvy.app.features.home.data.HomeRepository
import com.wavvy.app.features.home.data.HomeSection
import com.wavvy.app.features.home.data.toHomeItem
import com.wavvy.app.features.home.ui.components.HomeDimens
import com.wavvy.app.features.home.ui.components.isVideo

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
    // Songs listened to on this device, the most recent first, empty until something is listened to
    val recent: List<HomeItem> = emptyList(),
    // Songs listened to the longest in the last days, after the pinned ones in the speed dial
    val keepListening: List<HomeItem> = emptyList(),
    // Songs the user pinned, the first ones of the speed dial
    val pinned: List<HomeItem> = emptyList(),
    // Songs like the ones listened to lately, worked out when the Home opens or is refreshed so it does not change while listening
    val quickPicks: List<HomeItem> = emptyList(),
    // Songs listened to a lot before and much less lately, worked out with the quick picks
    val forgotten: List<HomeItem> = emptyList(),
    val continuation: String? = null,
    val isLoadingMore: Boolean = false,
    val isRefreshing: Boolean = false
)

// Videos stay out of the shelves of the Home so it shows music first, they are still found by the search and the pages
private fun List<HomeItem>.withoutVideos(): List<HomeItem> = filterNot { it.isVideo }

// The shelves without their videos, a shelf that was only videos goes away with them
@JvmName("sectionsWithoutVideos")
private fun List<HomeSection>.withoutVideos(): List<HomeSection> =
    mapNotNull { section ->
        val items = section.items.withoutVideos()
        if (items.isEmpty() && section.items.isNotEmpty()) null else section.copy(items = items)
    }

// Loads the Home when it opens and follows the filters, the next pages and the refresh
class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = HomeRepository(application)
    private val mutableState = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = mutableState.asStateFlow()

    // The requests that are running, so a new one replaces them
    private var loadJob: Job? = null
    private var accountJob: Job? = null
    private var localJob: Job? = null

    init {
        load()
        observeHistory()
        loadLocalSections()
    }

    // The quick picks and the forgotten favorites, taken from the history once, a failure of the network leaves only the forgotten ones
    private fun loadLocalSections() {
        localJob?.cancel()
        localJob = viewModelScope.launch {
            val application = getApplication<Application>()
            val forgotten = PlayHistory.forgotten(application, HomeDimens.ForgottenDays, HomeDimens.ForgottenMaxItems).first()
                .map(SongEntity::toHomeItem)
                .withoutVideos()

            // Similar songs come from the radio of YouTube Music for the song listened to last
            val latest = PlayHistory.recent(application, 1).first().firstOrNull()
            val similar = latest?.let { song ->
                RadioQueue.start(application, song.id).getOrNull()?.tracks.orEmpty()
                    .filter { it.id != song.id }
                    .map(PlayableTrack::toHomeItem)
                    .withoutVideos()
            }.orEmpty()

            val picks = (similar + forgotten.take(HomeDimens.QuickPicksForgottenItems))
                .distinctBy { it.id }
                .shuffled()
                .take(HomeDimens.QuickPicksMaxItems)

            mutableState.update { it.copy(quickPicks = picks, forgotten = forgotten) }
        }
    }

    // Follows the history of the device, so a song listened to shows up in the Home without opening it again
    private fun observeHistory() {
        viewModelScope.launch {
            PlayHistory.recent(getApplication(), HomeDimens.RecentMaxItems).collect { songs ->
                mutableState.update { it.copy(recent = songs.map(SongEntity::toHomeItem).withoutVideos()) }
            }
        }
        viewModelScope.launch {
            PlayHistory.pinned(getApplication()).collect { songs ->
                mutableState.update { it.copy(pinned = songs.map(PinnedEntity::toHomeItem)) }
            }
        }
        viewModelScope.launch {
            PlayHistory.mostPlayed(getApplication(), HomeDimens.KeepListeningDays, HomeDimens.KeepListeningMaxItems).collect { songs ->
                mutableState.update { it.copy(keepListening = songs.map(SongEntity::toHomeItem).withoutVideos()) }
            }
        }
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
        loadLocalSections()
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
                            sections = state.sections + page.sections.withoutVideos().filter { it.title !in known },
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
                            sections = page.sections.withoutVideos(),
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

package com.wavvy.app.features.podcast.ui

// Android application and view model
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
// Coroutines and reactive flows
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
// Project resources
import com.wavvy.app.core.navigation.EpisodeIdArg
import com.wavvy.app.features.podcast.data.EpisodePage
import com.wavvy.app.features.podcast.data.EpisodeRepository
import com.wavvy.app.features.podcast.data.PodcastPage
import com.wavvy.app.features.podcast.data.PodcastRepository

// What the page is doing, waiting for the answer, showing it or failed
enum class EpisodeStatus { Loading, Content, Error }

// Everything the page of an episode draws, with the page of its podcast, which comes after and gives the photo of who makes it and the other episodes
data class EpisodeUiState(
    val status: EpisodeStatus = EpisodeStatus.Loading,
    val page: EpisodePage? = null,
    val show: PodcastPage? = null
)

// Loads the page of an episode when it opens
class EpisodeViewModel(
    application: Application,
    savedState: SavedStateHandle
) : AndroidViewModel(application) {
    private val id: String = requireNotNull(savedState[EpisodeIdArg])
    private val repository = EpisodeRepository(application)
    private val shows = PodcastRepository(application)

    private val mutableState = MutableStateFlow(EpisodeUiState())
    val state: StateFlow<EpisodeUiState> = mutableState.asStateFlow()

    init {
        load()
    }

    // Asks the page, again after a failure
    fun load() {
        mutableState.value = EpisodeUiState()
        viewModelScope.launch {
            repository.load(id)
                .onSuccess {
                    mutableState.value = EpisodeUiState(status = EpisodeStatus.Content, page = it)
                    loadShow(it.showId)
                }
                .onFailure { mutableState.value = EpisodeUiState(status = EpisodeStatus.Error) }
        }
    }

    // Asks the page again without leaving the one that is on the screen, as when an episode that was set to come may be out, a failure leaves things as they are
    fun refresh() {
        viewModelScope.launch {
            repository.load(id).onSuccess { page -> mutableState.update { it.copy(status = EpisodeStatus.Content, page = page) } }
        }
    }

    // The podcast of the episode, the page does not wait for it
    private fun loadShow(showId: String?) {
        if (showId == null) return
        viewModelScope.launch {
            shows.load(showId).onSuccess { show -> mutableState.update { it.copy(show = show) } }
        }
    }
}

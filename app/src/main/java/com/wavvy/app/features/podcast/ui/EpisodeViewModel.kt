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
import kotlinx.coroutines.launch
// Project resources
import com.wavvy.app.core.navigation.EpisodeIdArg
import com.wavvy.app.features.podcast.data.EpisodePage
import com.wavvy.app.features.podcast.data.EpisodeRepository

// What the page is doing, waiting for the answer, showing it or failed
enum class EpisodeStatus { Loading, Content, Error }

// Everything the page of an episode draws
data class EpisodeUiState(
    val status: EpisodeStatus = EpisodeStatus.Loading,
    val page: EpisodePage? = null
)

// Loads the page of an episode when it opens
class EpisodeViewModel(
    application: Application,
    savedState: SavedStateHandle
) : AndroidViewModel(application) {
    private val id: String = requireNotNull(savedState[EpisodeIdArg])
    private val repository = EpisodeRepository(application)

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
                .onSuccess { mutableState.value = EpisodeUiState(status = EpisodeStatus.Content, page = it) }
                .onFailure { mutableState.value = EpisodeUiState(status = EpisodeStatus.Error) }
        }
    }
}

package com.wavvy.app.features.discover.ui

// Android application and view model
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
// Coroutines and reactive flows
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
// Project resources
import com.wavvy.app.features.discover.data.DiscoverPage
import com.wavvy.app.features.discover.data.DiscoverRepository

// What the tab is doing, waiting for the answer, showing it or failed
enum class DiscoverStatus { Loading, Content, Error }

// Everything the Discover tab draws
data class DiscoverUiState(
    val status: DiscoverStatus = DiscoverStatus.Loading,
    val page: DiscoverPage? = null
)

// Loads the Explore page of YouTube Music when the tab opens
class DiscoverViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = DiscoverRepository(application)

    private val mutableState = MutableStateFlow(DiscoverUiState())
    val state: StateFlow<DiscoverUiState> = mutableState.asStateFlow()

    init {
        load()
    }

    // Asks the page, again after a failure
    fun load() {
        mutableState.value = DiscoverUiState(status = DiscoverStatus.Loading)
        viewModelScope.launch {
            repository.load()
                .onSuccess { mutableState.value = DiscoverUiState(status = DiscoverStatus.Content, page = it) }
                .onFailure { mutableState.value = DiscoverUiState(status = DiscoverStatus.Error) }
        }
    }
}

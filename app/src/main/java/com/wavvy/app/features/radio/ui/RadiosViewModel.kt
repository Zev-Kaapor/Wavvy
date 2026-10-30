package com.wavvy.app.features.radio.ui

// Android application and view models
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
import com.wavvy.app.core.radio.RadioStation
import com.wavvy.app.core.radio.RadiosReader

// What the list of the radios shows, the stations read so far and how many the site has
data class RadiosUiState(
    val stations: List<RadioStation> = emptyList(),
    val total: Int = 0,
    // How many pages were read, a list that came whole in one page is the only one that can be put in order without moving what the user already sees
    val pages: Int = 0,
    val isLoading: Boolean = true,
    // Why the last page could not be read, empty when it could
    val error: String? = null
)

// Reads the stations of a list page by page as it is scrolled
class RadiosViewModel(application: Application) : AndroidViewModel(application) {
    private val mutableState = MutableStateFlow(RadiosUiState())
    val state: StateFlow<RadiosUiState> = mutableState.asStateFlow()

    private var path: String? = null
    private var page = 0
    private var loading: Job? = null

    // Begins the list of a page of the site, nothing happens when it is the one that is already on
    fun start(newPath: String) {
        if (newPath == path) return

        loading?.cancel()
        path = newPath
        page = 0
        mutableState.value = RadiosUiState()
        loadMore()
    }

    // Reads the next page, nothing happens while one is on its way or when the list is whole
    fun loadMore() {
        val current = mutableState.value
        val address = path ?: return
        if (loading?.isActive == true || (page > 0 && current.stations.size >= current.total)) return

        mutableState.update { it.copy(isLoading = true, error = null) }
        loading = viewModelScope.launch {
            RadiosReader.list(getApplication(), address, page + 1)
                .onSuccess { next ->
                    page++
                    mutableState.update { state ->
                        state.copy(stations = (state.stations + next.stations).distinctBy { it.id }, total = next.total, pages = page, isLoading = false)
                    }
                }
                .onFailure { failure -> mutableState.update { it.copy(isLoading = false, error = failure.message.orEmpty()) } }
        }
    }
}

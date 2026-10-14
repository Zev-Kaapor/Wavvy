package com.wavvy.app.features.radio.ui

// Android application and view models
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
// Coroutines and reactive flows
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
// Project resources
import com.wavvy.app.core.radio.RadiosBrowse
import com.wavvy.app.core.radio.RadiosReader

// The places of a page, the states, the cities or the countries, while they are read
data class RadiosPlacesState(
    val browse: RadiosBrowse? = null,
    val isLoading: Boolean = true,
    // Why the page could not be read, empty when it could
    val error: String? = null
)

// Reads the places a page of the site lists
class RadiosPlacesViewModel(application: Application) : AndroidViewModel(application) {
    private val mutableState = MutableStateFlow(RadiosPlacesState())
    val state: StateFlow<RadiosPlacesState> = mutableState.asStateFlow()

    private var path: String? = null

    // Reads a page, nothing happens when it is the one that is already on
    fun load(newPath: String) {
        if (newPath == path && mutableState.value.error == null) return

        path = newPath
        mutableState.value = RadiosPlacesState()
        viewModelScope.launch {
            RadiosReader.browse(getApplication(), newPath)
                .onSuccess { mutableState.value = RadiosPlacesState(browse = it, isLoading = false) }
                .onFailure { mutableState.value = RadiosPlacesState(isLoading = false, error = it.message.orEmpty()) }
        }
    }

    // Tries the same page again
    fun retry() {
        path?.let(::load)
    }
}

package com.wavvy.app.features.discover.ui

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
import com.wavvy.app.core.navigation.ExploreIdArg
import com.wavvy.app.core.navigation.ExploreLargeArg
import com.wavvy.app.core.navigation.ExploreParamsArg
import com.wavvy.app.core.navigation.ExploreTitleArg
import com.wavvy.app.features.discover.data.ExplorePage
import com.wavvy.app.features.discover.data.ExploreRepository

// What the page is doing, waiting for the answer, showing it or failed
enum class ExploreStatus { Loading, Content, Error }

// Everything a page of the Explore tab draws, the title is the one that came with the button until the page brings its own
data class ExploreUiState(
    val title: String,
    // The title is big on top of the page and shrinks into the bar when it scrolls away, as on the pages that the big buttons open
    val isLarge: Boolean,
    val status: ExploreStatus = ExploreStatus.Loading,
    val page: ExplorePage? = null
)

// Loads a page that opens from the Explore tab
class ExploreViewModel(
    application: Application,
    savedState: SavedStateHandle
) : AndroidViewModel(application) {
    private val id: String = requireNotNull(savedState[ExploreIdArg])
    private val params: String? = savedState.get<String>(ExploreParamsArg)?.takeIf { it.isNotEmpty() }
    private val repository = ExploreRepository(application)

    private val mutableState = MutableStateFlow(
        ExploreUiState(
            title = savedState.get<String>(ExploreTitleArg).orEmpty(),
            isLarge = savedState.get<Boolean>(ExploreLargeArg) ?: false
        )
    )
    val state: StateFlow<ExploreUiState> = mutableState.asStateFlow()

    init {
        load()
    }

    // The country picked on the charts, none until one is picked
    private var country: String? = null

    // Shows the charts of another country
    fun selectCountry(code: String) {
        country = code
        load()
    }

    // Asks the page, again after a failure
    fun load() {
        mutableState.update { it.copy(status = ExploreStatus.Loading) }
        viewModelScope.launch {
            repository.load(id, params, country)
                .onSuccess { page -> mutableState.update { it.copy(status = ExploreStatus.Content, page = page, title = page.title) } }
                .onFailure { mutableState.update { it.copy(status = ExploreStatus.Error) } }
        }
    }
}

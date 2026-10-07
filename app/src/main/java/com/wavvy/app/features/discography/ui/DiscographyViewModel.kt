package com.wavvy.app.features.discography.ui

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
import com.wavvy.app.core.navigation.DiscographyFilterArg
import com.wavvy.app.core.navigation.DiscographyIdArg
import com.wavvy.app.core.navigation.DiscographyParamsArg
import com.wavvy.app.core.navigation.DiscographyTitleArg
import com.wavvy.app.features.discography.data.DiscographyChip
import com.wavvy.app.features.discography.data.DiscographyPage
import com.wavvy.app.features.discography.data.DiscographyRepository
import com.wavvy.app.features.home.data.HomeItem

// What the page is doing, waiting for the answer, showing it or failed
enum class DiscographyStatus { Loading, Content, Error }

// Everything the full list of a shelf draws
data class DiscographyUiState(
    // Name of the artist on top, and the name of the shelf, which is the filter shown until the list brings its own
    val title: String,
    val filter: String,
    val status: DiscographyStatus = DiscographyStatus.Loading,
    val chips: List<DiscographyChip> = emptyList(),
    val items: List<HomeItem> = emptyList()
)

// Loads the full list behind a shelf of an artist and asks it again when another filter is chosen
class DiscographyViewModel(
    application: Application,
    savedState: SavedStateHandle
) : AndroidViewModel(application) {
    private val id: String = requireNotNull(savedState[DiscographyIdArg])
    private val params: String? = savedState.get<String>(DiscographyParamsArg)?.takeIf { it.isNotEmpty() }
    private val repository = DiscographyRepository(application)

    private val mutableState = MutableStateFlow(
        DiscographyUiState(
            title = savedState.get<String>(DiscographyTitleArg).orEmpty(),
            filter = savedState.get<String>(DiscographyFilterArg).orEmpty()
        )
    )
    val state: StateFlow<DiscographyUiState> = mutableState.asStateFlow()

    init {
        load()
    }

    // Asks the list, again after a failure
    fun load() {
        mutableState.update { it.copy(status = DiscographyStatus.Loading) }
        viewModelScope.launch {
            repository.load(id, params)
                .onSuccess(::show)
                .onFailure { mutableState.update { it.copy(status = DiscographyStatus.Error) } }
        }
    }

    // Shows the list under another filter
    fun select(chip: DiscographyChip) {
        val token = chip.token ?: return
        if (chip.isSelected) return

        mutableState.update { it.copy(status = DiscographyStatus.Loading) }
        viewModelScope.launch {
            repository.reload(token)
                .onSuccess(::show)
                .onFailure { mutableState.update { it.copy(status = DiscographyStatus.Error) } }
        }
    }

    // Puts the list and its filters on the screen
    private fun show(page: DiscographyPage) {
        mutableState.update { it.copy(status = DiscographyStatus.Content, chips = page.chips, items = page.items) }
    }
}

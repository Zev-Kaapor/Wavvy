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
import com.wavvy.app.core.navigation.ExploreColorArg
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

    // The color of the mood or of the genre that led to the page, none for the pages that come from elsewhere
    val accent: Long? = savedState.get<String>(ExploreColorArg)?.toLongOrNull()

    // True for the pages that open from the root of a group, such as the new releases, and for the charts and the moods, which are drawn on their own
    val isHub: Boolean = id in HubIds
    val isCharts: Boolean = id == ChartsId
    val isMoods: Boolean = id == MoodsId

    // True for the page of the podcasts, which has no highlights on top of its shelves
    val isPodcasts: Boolean = id == PodcastsId

    // True for the page of a mood or of a genre, drawn as the other pages of the Explore tab with its shelves in a single row
    val isCategory: Boolean = id == CategoryId

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

// The pages of YouTube Music that open from the root of a group, the new releases and the podcasts, and the one of the charts
private val HubIds = setOf("FEmusic_new_releases", "FEmusic_non_music_audio")
private const val PodcastsId = "FEmusic_non_music_audio"
private const val ChartsId = "FEmusic_charts"
private const val MoodsId = "FEmusic_moods_and_genres"
private const val CategoryId = "FEmusic_moods_and_genres_category"

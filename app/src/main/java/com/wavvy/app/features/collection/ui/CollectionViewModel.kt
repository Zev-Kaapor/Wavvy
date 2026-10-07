package com.wavvy.app.features.collection.ui

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
import kotlin.random.Random
// Project resources
import com.wavvy.app.core.innertube.MusicOrigin
import com.wavvy.app.core.navigation.CollectionIdArg
import com.wavvy.app.core.navigation.CollectionKindArg
import com.wavvy.app.core.navigation.CollectionTitleArg
import com.wavvy.app.features.collection.data.CollectionKind
import com.wavvy.app.features.collection.data.CollectionPage
import com.wavvy.app.features.collection.data.CollectionRepository
import com.wavvy.app.features.home.data.HomeItem

// What the page is doing, waiting for the answer, showing it or failed
enum class CollectionStatus { Loading, Content, Error }

// Everything the page of an album or of a playlist draws
data class CollectionUiState(
    val status: CollectionStatus = CollectionStatus.Loading,
    val page: CollectionPage? = null,
    val tracks: List<HomeItem> = emptyList(),
    val continuation: String? = null,
    val isLoadingMore: Boolean = false,
    // True while the rest of a long playlist is on its way to be played
    val isPreparing: Boolean = false
)

// Loads the page of the album or of the playlist the route points to, and plays its songs
class CollectionViewModel(
    application: Application,
    savedState: SavedStateHandle
) : AndroidViewModel(application) {
    private val kind = CollectionKind.valueOf(requireNotNull(savedState[CollectionKindArg]))
    private val id: String = requireNotNull(savedState[CollectionIdArg])

    // Name of the shelf that led to the page, which then stands on the top bar in place of the title under the cover
    val pageTitle: String = savedState.get<String>(CollectionTitleArg).orEmpty()
    private val repository = CollectionRepository(application)

    private val mutableState = MutableStateFlow(CollectionUiState())
    val state: StateFlow<CollectionUiState> = mutableState.asStateFlow()

    init {
        load()
    }

    // Asks the page, again after a failure
    fun load() {
        mutableState.update { CollectionUiState(status = CollectionStatus.Loading) }
        viewModelScope.launch {
            repository.load(kind, id)
                .onSuccess { page ->
                    mutableState.update {
                        CollectionUiState(
                            status = CollectionStatus.Content,
                            page = page,
                            tracks = page.tracks.distinctBy { track -> track.id },
                            continuation = page.continuation
                        )
                    }
                }
                .onFailure { mutableState.update { CollectionUiState(status = CollectionStatus.Error) } }
        }
    }

    // Adds the next songs of a long playlist, called when the list gets close to its end
    fun loadMore() {
        val state = mutableState.value
        val continuation = state.continuation ?: return
        if (state.isLoadingMore || state.status != CollectionStatus.Content) return

        mutableState.update { it.copy(isLoadingMore = true) }
        viewModelScope.launch {
            repository.more(continuation)
                .onSuccess { more -> append(more.tracks, more.continuation) }
                .onFailure { mutableState.update { it.copy(isLoadingMore = false) } }
        }
    }

    // Plays every song of the page, in order or shuffled
    fun play(shuffle: Boolean, onPlay: (List<HomeItem>, Int, Boolean?) -> Unit) {
        withAllTracks { tracks ->
            onPlay(tracks, if (shuffle && tracks.isNotEmpty()) Random.nextInt(tracks.size) else 0, shuffle)
        }
    }

    // Hands over every song of the page, after asking the rest of a long playlist
    fun withAllTracks(action: (List<HomeItem>) -> Unit) {
        if (mutableState.value.isPreparing) return

        mutableState.update { it.copy(isPreparing = true) }
        viewModelScope.launch {
            var pages = 0
            while (mutableState.value.continuation != null && pages < CollectionDimens.MaxPages) {
                val continuation = mutableState.value.continuation ?: break
                val more = repository.more(continuation).getOrNull() ?: break
                append(more.tracks, more.continuation)
                pages++
            }

            val tracks = mutableState.value.tracks
            mutableState.update { it.copy(isPreparing = false) }
            action(tracks)
        }
    }

    // Link of the page on YouTube Music, for sharing
    val shareUrl: String
        get() = if (kind == CollectionKind.Album) "$MusicOrigin/browse/$id" else "$MusicOrigin/playlist?list=$id"

    // Puts the songs that came after the ones already on the page, without repeats
    private fun append(tracks: List<HomeItem>, continuation: String?) {
        mutableState.update { current ->
            val known = current.tracks.map { it.id }.toSet()
            current.copy(
                tracks = current.tracks + tracks.filter { it.id !in known }.distinctBy { it.id },
                continuation = continuation,
                isLoadingMore = false
            )
        }
    }
}

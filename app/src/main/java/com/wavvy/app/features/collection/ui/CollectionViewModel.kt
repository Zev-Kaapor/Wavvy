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
import kotlinx.coroutines.flow.drop
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
import androidx.annotation.StringRes
import com.wavvy.app.R
import com.wavvy.app.features.collection.data.CollectionMore
import com.wavvy.app.features.home.data.HomeItem
import com.wavvy.app.features.library.data.LibrarySortOption
import com.wavvy.app.features.playlist.data.PlaylistChanges
import com.wavvy.app.features.playlist.data.PlaylistEvent
import com.wavvy.app.features.playlist.data.PlaylistRepository

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
    val isPreparing: Boolean = false,
    // The name of the order that was chosen, empty while the page is in the order it came in
    val sortSelected: String? = null,
    // The order made here, used when the page has no order of YouTube Music to ask
    val order: CollectionOrder = CollectionOrder.Recent,
    // True while the page is pulled to be asked again
    val isRefreshing: Boolean = false
) {
    // The songs in the order that was chosen
    val shown: List<HomeItem>
        get() = when (order) {
            CollectionOrder.Recent -> tracks
            CollectionOrder.Oldest -> tracks.reversed()
            CollectionOrder.Title -> tracks.sortedBy { it.title.lowercase() }
            CollectionOrder.Artist -> tracks.sortedBy { it.artists.firstOrNull().orEmpty().lowercase() }
        }
}

// The orders that are made here, the first is the one the playlist comes in
enum class CollectionOrder(@StringRes val titleRes: Int) {
    Recent(R.string.collection_order_recent),
    Oldest(R.string.collection_order_oldest),
    Title(R.string.collection_order_title),
    Artist(R.string.collection_order_artist)
}

// Loads the page of the album or of the playlist the route points to, and plays its songs
class CollectionViewModel(
    application: Application,
    savedState: SavedStateHandle
) : AndroidViewModel(application) {
    private val kind = CollectionKind.valueOf(requireNotNull(savedState[CollectionKindArg]))
    val id: String = requireNotNull(savedState[CollectionIdArg])

    // Name of the shelf that led to the page, which then stands on the top bar in place of the title under the cover
    val pageTitle: String = savedState.get<String>(CollectionTitleArg).orEmpty()
    private val repository = CollectionRepository(application)
    private val playlists = PlaylistRepository(application)

    private val mutableState = MutableStateFlow(CollectionUiState())
    val state: StateFlow<CollectionUiState> = mutableState.asStateFlow()

    init {
        load()

        // A song saved or taken out anywhere shows on the page without its placeholder
        viewModelScope.launch {
            PlaylistChanges.version.drop(1).collect { refresh() }
        }

        // A song that left the playlist, or lost its like in the list of the liked ones, leaves the page before the answer comes
        viewModelScope.launch {
            PlaylistChanges.events.collect { event ->
                val isThisPage = when (event) {
                    is PlaylistEvent.SongRemoved -> event.playlistId.removePrefix("VL") == id.removePrefix("VL")
                    is PlaylistEvent.LikeChanged -> !event.isLiked && id.removePrefix("VL") == LikedId
                }
                if (isThisPage) {
                    mutableState.update { current ->
                        current.copy(
                            tracks = current.tracks.filterNot { track ->
                                when (event) {
                                    is PlaylistEvent.SongRemoved -> track.id == event.videoId && (event.setVideoId == null || track.setVideoId == event.setVideoId)
                                    is PlaylistEvent.LikeChanged -> track.id == event.videoId
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    // The playlist the songs of the page can be taken out of, only for one the account made
    val editablePlaylistId: String?
        get() = id.takeIf { kind == CollectionKind.Playlist && (mutableState.value.page?.isEditable == true || id.removePrefix("VL") == EpisodesLaterId) }

    // Asks the page again and keeps showing it while the answer comes
    private fun refresh() {
        if (mutableState.value.status != CollectionStatus.Content) return

        viewModelScope.launch {
            repository.load(kind, id).onSuccess { page ->
                mutableState.update { it.copy(page = page, tracks = page.tracks.distinctBy { track -> track.id }, continuation = page.continuation, sortSelected = null) }
            }
        }
    }

    // The page pulled down by the finger, asked again with the indicator on until the answer comes
    fun pullRefresh() {
        if (mutableState.value.status != CollectionStatus.Content || mutableState.value.isRefreshing) return

        mutableState.update { it.copy(isRefreshing = true) }
        viewModelScope.launch {
            repository.load(kind, id)
                .onSuccess { page ->
                    mutableState.update { it.copy(page = page, tracks = page.tracks.distinctBy { track -> track.id }, continuation = page.continuation, sortSelected = null, isRefreshing = false) }
                }
                .onFailure { mutableState.update { it.copy(isRefreshing = false) } }
        }
    }

    // Deletes the playlist of the page
    fun delete(onDone: (Boolean) -> Unit) {
        viewModelScope.launch { onDone(playlists.delete(id).isSuccess) }
    }

    // The order of the songs now, as YouTube Music names it
    val sortTitle: String?
        get() {
            val state = mutableState.value
            val options = state.page?.sortOptions.orEmpty()
            if (options.isEmpty()) return getApplication<Application>().getString(state.order.titleRes)
            return state.sortSelected ?: (options.firstOrNull { it.isSelected } ?: options.firstOrNull { it.token == null })?.title
        }

    // Puts the songs that are on the page in an order made here
    fun selectOrder(order: CollectionOrder) {
        mutableState.update { it.copy(order = order) }
    }

    // Asks the songs again in another order, the ones that are on the page stay until the answer comes
    fun selectSort(option: LibrarySortOption) {
        mutableState.update { it.copy(sortSelected = option.title) }
        viewModelScope.launch {
            val result = if (option.token != null) {
                repository.sorted(option.token)
            } else {
                repository.load(kind, id).map { page -> CollectionMore(page.tracks, page.continuation) }
            }
            result.onSuccess { more ->
                mutableState.update { it.copy(tracks = more.tracks.distinctBy { track -> track.id }, continuation = more.continuation) }
            }
        }
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

            val tracks = mutableState.value.shown
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

// The playlist of YouTube Music that keeps the liked songs
private const val LikedId = "LM"

// The playlist of YouTube Music that keeps the episodes to listen to later, whose songs can be taken out
private const val EpisodesLaterId = "SE"

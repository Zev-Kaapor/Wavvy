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
import com.wavvy.app.features.library.ui.nameOrder
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
    // The order made here, and whether the rest of the songs it needs is on its way
    val order: CollectionOrder = CollectionOrder.Playlist,
    val isOrdering: Boolean = false,
    // True while the page is pulled to be asked again
    val isRefreshing: Boolean = false
) {
    // The songs in the order that was chosen, a song with nothing to be ordered by goes last in both ways
    val shown: List<HomeItem> by lazy {
        val names = nameOrder()
        when (order) {
            CollectionOrder.Playlist -> tracks
            CollectionOrder.Reversed -> tracks.reversed()
            CollectionOrder.AToZ -> tracks.sortedWith(compareBy(names) { it.title })
            CollectionOrder.ZToA -> tracks.sortedWith(compareByDescending(names) { it.title })
            CollectionOrder.ArtistAToZ -> tracks.sortedWith(compareBy<HomeItem, String?>(nullsLast<String>(names)) { it.artists.firstOrNull() }.thenBy(names) { it.title })
            CollectionOrder.ArtistZToA -> tracks.sortedWith(compareBy<HomeItem, String?>(nullsLast<String>(names.reversed())) { it.artists.firstOrNull() }.thenBy(names) { it.title })
            CollectionOrder.Longest -> tracks.sortedWith(compareBy(nullsLast(reverseOrder<Int>())) { it.durationSeconds })
            CollectionOrder.Shortest -> tracks.sortedWith(compareBy(nullsLast(naturalOrder<Int>())) { it.durationSeconds })
        }
    }
}

// The orders made here, with the words that say it and the short word of its button in the sheet, the first is the one the playlist comes in
enum class CollectionOrder(@StringRes val titleRes: Int, @StringRes val wordRes: Int) {
    Playlist(R.string.collection_order_playlist, R.string.collection_order_word_normal),
    Reversed(R.string.collection_order_reversed, R.string.collection_order_word_reversed),
    AToZ(R.string.library_sort_a_to_z, R.string.library_sort_word_a_to_z),
    ZToA(R.string.library_sort_z_to_a, R.string.library_sort_word_z_to_a),
    ArtistAToZ(R.string.library_sort_artist_a_to_z, R.string.library_sort_word_a_to_z),
    ArtistZToA(R.string.library_sort_artist_z_to_a, R.string.library_sort_word_z_to_a),
    Longest(R.string.library_sort_longest, R.string.library_sort_word_longest),
    Shortest(R.string.library_sort_shortest, R.string.library_sort_word_shortest)
}

// What the songs can be put in order by and its two ways, a tap on its name picks the first
// A playlist keeps no date for its songs, what it has is its own order, that is the one it comes in or the other way around
enum class CollectionOrderKind(@StringRes val titleRes: Int, val first: CollectionOrder, val second: CollectionOrder) {
    Playlist(R.string.collection_order_playlist, CollectionOrder.Playlist, CollectionOrder.Reversed),
    Title(R.string.collection_order_title, CollectionOrder.AToZ, CollectionOrder.ZToA),
    Artist(R.string.collection_order_artist, CollectionOrder.ArtistAToZ, CollectionOrder.ArtistZToA),
    Length(R.string.library_sort_length, CollectionOrder.Longest, CollectionOrder.Shortest)
}

// Loads the page of the album or of the playlist the route points to, and plays its songs
class CollectionViewModel(
    application: Application,
    savedState: SavedStateHandle
) : AndroidViewModel(application) {
    val kind = CollectionKind.valueOf(requireNotNull(savedState[CollectionKindArg]))
    val id: String = requireNotNull(savedState[CollectionIdArg])

    // True while the comments were put aside to see who wrote one, they open again when the page comes back
    var isCommentsParked = false

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
                    // A new playlist changes nothing in the page of another one
                    is PlaylistEvent.Created -> false
                }
                if (isThisPage) {
                    mutableState.update { current ->
                        current.copy(
                            tracks = current.tracks.filterNot { track ->
                                when (event) {
                                    is PlaylistEvent.SongRemoved -> track.id == event.videoId && (event.setVideoId == null || track.setVideoId == event.setVideoId)
                                    is PlaylistEvent.LikeChanged -> track.id == event.videoId
                                    is PlaylistEvent.Created -> false
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
                val whole = restOf(page.tracks, page.continuation)
                mutableState.update { it.copy(page = page, tracks = whole.tracks.distinctBy { track -> track.id }, continuation = whole.continuation, sortSelected = null) }
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
                    val whole = restOf(page.tracks, page.continuation)
                    mutableState.update { it.copy(page = page, tracks = whole.tracks.distinctBy { track -> track.id }, continuation = whole.continuation, sortSelected = null, isRefreshing = false) }
                }
                .onFailure { mutableState.update { it.copy(isRefreshing = false) } }
        }
    }

    // Saves the page to the library or takes it out, the page shows the new state at once and goes back if the account refuses
    fun toggleLibrary(onDone: (isSaved: Boolean, isDone: Boolean) -> Unit) {
        val page = mutableState.value.page ?: return
        val target = page.libraryTarget ?: return
        val saved = !page.isInLibrary

        mutableState.update { it.copy(page = page.copy(isInLibrary = saved)) }
        viewModelScope.launch {
            val isDone = repository.setSaved(target, saved).isSuccess
            if (!isDone) mutableState.update { state -> state.copy(page = state.page?.copy(isInLibrary = !saved)) }
            onDone(saved, isDone)
        }
    }

    // Deletes the playlist of the page
    fun delete(onDone: (Boolean) -> Unit) {
        viewModelScope.launch { onDone(playlists.delete(id).isSuccess) }
    }

    // The order of YouTube Music in use, none while one made here is, or when the page says no orders
    val currentRemote: LibrarySortOption?
        get() {
            val state = mutableState.value
            val options = state.page?.sortOptions.orEmpty()
            if (state.order != CollectionOrder.Playlist) return null
            return options.firstOrNull { it.title == state.sortSelected } ?: options.firstOrNull { it.isSelected } ?: options.firstOrNull { it.token == null }
        }

    // The order of YouTube Music that does the same as one made here, it reaches the whole playlist without having to read it
    fun remoteOf(order: CollectionOrder): LibrarySortOption? {
        val options = mutableState.value.page?.sortOptions.orEmpty()
        return when (order) {
            CollectionOrder.AToZ -> options.firstOrNull { it.isAToZ }
            CollectionOrder.ZToA -> options.firstOrNull { it.isZToA }
            else -> null
        }
    }

    // The order of the songs now as a phrase, the one by the name of YouTube Music says it the same as the one made here
    val sortTitle: String
        get() {
            val context = getApplication<Application>()
            val remote = currentRemote ?: return context.getString(mutableState.value.order.titleRes)
            return when {
                remote.isAToZ -> context.getString(R.string.library_sort_a_to_z)
                remote.isZToA -> context.getString(R.string.library_sort_z_to_a)
                else -> remote.title
            }
        }

    // Puts the songs in an order made here, which needs all of them, the ones on the page make way for the placeholder while the rest comes
    fun selectOrder(order: CollectionOrder) {
        val state = mutableState.value
        if (order == CollectionOrder.Playlist || state.continuation == null) {
            mutableState.update { it.copy(order = order) }
            return
        }

        mutableState.update { it.copy(isOrdering = true) }
        viewModelScope.launch {
            val whole = restOf(state.tracks, state.continuation, order)
            mutableState.update { it.copy(order = order, tracks = whole.tracks.distinctBy { track -> track.id }, continuation = whole.continuation, isOrdering = false) }
        }
    }

    // The songs that come after the ones given when an order made here needs them
    private suspend fun restOf(tracks: List<HomeItem>, continuation: String?, order: CollectionOrder = mutableState.value.order): CollectionMore {
        if (order == CollectionOrder.Playlist) return CollectionMore(tracks, continuation)

        var all = tracks
        var next = continuation
        var pages = 0
        while (next != null && pages < CollectionDimens.MaxPages) {
            val more = repository.more(next).getOrNull() ?: break
            all = all + more.tracks
            next = more.continuation
            pages++
        }
        return CollectionMore(all, next)
    }

    // Asks the songs again in another order, the ones that are on the page stay until the answer comes
    fun selectSort(option: LibrarySortOption) {
        mutableState.update { it.copy(sortSelected = option.title, order = CollectionOrder.Playlist) }
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

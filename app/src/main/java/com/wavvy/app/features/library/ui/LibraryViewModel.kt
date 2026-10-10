package com.wavvy.app.features.library.ui

// Android application and view model
import android.app.Application
import androidx.annotation.StringRes
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
// Coroutines and reactive flows
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
// Project resources
import com.wavvy.app.R
import com.wavvy.app.core.innertube.MusicOrigin
import com.wavvy.app.features.collection.data.CollectionKind
import com.wavvy.app.features.collection.data.CollectionRepository
import com.wavvy.app.features.collection.ui.CollectionDimens
import com.wavvy.app.features.home.data.HomeItem
import com.wavvy.app.features.home.data.HomeItemKind
import com.wavvy.app.features.library.data.LibraryRepository
import com.wavvy.app.features.library.data.LibrarySortOption
import com.wavvy.app.features.library.data.LibrarySource
import com.wavvy.app.features.library.data.LibraryStore
import com.wavvy.app.features.playlist.data.PlaylistChanges
import com.wavvy.app.features.playlist.data.PlaylistEvent
import com.wavvy.app.features.playlist.data.PlaylistRepository

// The buttons of the top of the library, each has one list or, when it has more, a row of them that shows once it is chosen
enum class LibraryGroup(@StringRes val titleRes: Int, val sources: List<LibrarySource>, @StringRes val sourceTitles: List<Int>) {
    Downloads(R.string.library_downloads, emptyList(), emptyList()),
    Playlists(R.string.library_playlists, listOf(LibrarySource.Playlists), listOf(R.string.library_playlists)),
    Podcasts(
        R.string.library_podcasts,
        listOf(LibrarySource.Podcasts, LibrarySource.Channels),
        listOf(R.string.library_podcasts, R.string.library_channels)
    ),
    Songs(R.string.library_songs, listOf(LibrarySource.Songs), listOf(R.string.library_songs)),
    Albums(R.string.library_albums, listOf(LibrarySource.Albums), listOf(R.string.library_albums)),
    Artists(
        R.string.library_artists,
        listOf(LibrarySource.Artists, LibrarySource.Subscriptions),
        listOf(R.string.library_artists, R.string.library_subscriptions)
    )
}

// The ways to put a list in order, what is first in the list of YouTube Music or by the name
enum class LibrarySort(@StringRes val titleRes: Int) {
    Recent(R.string.library_sort_recent),
    Saved(R.string.library_sort_saved),
    Played(R.string.library_sort_played),
    AToZ(R.string.library_sort_a_to_z),
    ZToA(R.string.library_sort_z_to_a),

    // The two of the downloads, the newest first and the biggest first
    Downloaded(R.string.library_sort_downloaded),
    Size(R.string.library_sort_size)
}

// What a list is doing, waiting for the answer, showing it or failed
enum class LibraryStatus { Loading, Content, Error }

// Everything the library draws
data class LibraryUiState(
    // The button of the top that is on, none shows the list of what was used lately
    val group: LibraryGroup? = null,
    val source: LibrarySource = LibrarySource.Recent,
    val status: LibraryStatus = LibraryStatus.Loading,
    val items: List<HomeItem> = emptyList(),
    val continuation: String? = null,
    val isLoadingMore: Boolean = false,
    val sort: LibrarySort = LibrarySort.Recent,
    val isGrid: Boolean = false,
    // The words typed in the search, none while the search is closed
    val query: String? = null,
    // The orders that YouTube Music offers for the list in use and the one that is on, empty until the list says them
    val remoteSorts: List<LibrarySortOption> = emptyList(),
    val remoteSelected: String? = null,
    // True while the list is pulled to be asked again
    val isRefreshing: Boolean = false
) {
    // The ways that fit the list in use, what was used lately has the three of YouTube Music and the other lists have the one that is theirs
    // The ones of YouTube Music keep the order the list comes in until they can be told apart
    val sorts: List<LibrarySort>
        get() = if (group == LibraryGroup.Downloads) {
            listOf(LibrarySort.Downloaded, LibrarySort.AToZ, LibrarySort.ZToA, LibrarySort.Size)
        } else if (source == LibrarySource.Recent) {
            listOf(LibrarySort.Recent, LibrarySort.Saved, LibrarySort.Played, LibrarySort.AToZ, LibrarySort.ZToA)
        } else {
            listOf(LibrarySort.Saved, LibrarySort.AToZ, LibrarySort.ZToA)
        }

    // The items in the order that was chosen
    val shown: List<HomeItem>
        get() {
            val ordered = when (sort) {
                LibrarySort.AToZ -> items.sortedBy { it.title.lowercase() }
                LibrarySort.ZToA -> items.sortedByDescending { it.title.lowercase() }
                else -> items
            }
            val words = query?.trim().orEmpty()
            return if (words.isEmpty()) ordered else ordered.filter { item -> listOfNotNull(item.title, item.lineText, item.countText).any { it.contains(words, ignoreCase = true) } }
        }
}

// Loads the lists of the library as the buttons are chosen, the next pages near the end, and keeps the choice of order and of how it is drawn
class LibraryViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = LibraryRepository(application)
    private val collections = CollectionRepository(application)
    private val store = LibraryStore(application)
    private val playlists = PlaylistRepository(application)
    private val mutableState = MutableStateFlow(LibraryUiState())
    val state: StateFlow<LibraryUiState> = mutableState.asStateFlow()

    private var job: Job? = null

    // The order of YouTube Music that was chosen last time, asked again as soon as the list says its orders
    private var pendingRemote: String? = null

    // Opens in the button and the list that were on the last time
    init {
        viewModelScope.launch {
            val (savedGroup, savedSource) = store.read()
            val group = LibraryGroup.entries.firstOrNull { it.name == savedGroup }
            val source = group?.sources?.firstOrNull { it.name == savedSource } ?: group?.sources?.firstOrNull()

            if (group != null) {
                mutableState.update { it.copy(group = group, source = source ?: it.source, sort = defaultSort(source ?: it.source)) }
            }
            if (source != null || group == null) {
                restoreSort(sortKeyOf(mutableState.value))
                load()
            } else {
                mutableState.update { it.copy(status = LibraryStatus.Content, sort = LibrarySort.Downloaded) }
                restoreSort(DownloadsSortKey)
            }
        }

        // A playlist made, changed or deleted anywhere shows in the list
        viewModelScope.launch {
            PlaylistChanges.version.drop(1).collect { refresh() }
        }

        // A song that lost its like leaves the list of the songs before the answer comes
        viewModelScope.launch {
            PlaylistChanges.events.collect { event ->
                if (event is PlaylistEvent.LikeChanged && !event.isLiked && mutableState.value.source == LibrarySource.Songs) {
                    mutableState.update { current -> current.copy(items = current.items.filterNot { it.id == event.videoId }) }
                }
            }
        }
    }

    // Turns a button of the top on, or off when it was on
    fun selectGroup(group: LibraryGroup) {
        val source = group.sources.firstOrNull()
        mutableState.update {
            it.copy(group = group, source = source ?: it.source, sort = defaultSort(source ?: it.source), items = emptyList(), continuation = null, remoteSorts = emptyList(), remoteSelected = null)
        }
        saveChoice()
        if (source != null) {
            open(source)
        } else {
            mutableState.update { it.copy(status = LibraryStatus.Content, sort = LibrarySort.Downloaded) }
            viewModelScope.launch { restoreSort(DownloadsSortKey) }
        }
    }

    // Goes to another list of the same button, such as the channels of the podcasts
    fun selectSource(source: LibrarySource) {
        mutableState.update { it.copy(source = source, sort = defaultSort(source), items = emptyList(), continuation = null, remoteSorts = emptyList(), remoteSelected = null) }
        saveChoice()
        open(source)
    }

    // Back to what was used lately
    fun clear() {
        mutableState.update { it.copy(group = null, source = LibrarySource.Recent, sort = LibrarySort.Recent, items = emptyList(), continuation = null, remoteSorts = emptyList(), remoteSelected = null) }
        saveChoice()
        open(LibrarySource.Recent)
    }

    fun selectSort(sort: LibrarySort) {
        mutableState.update { it.copy(sort = sort) }
        saveSort(LocalPrefix + sort.name)
    }

    // Asks the list again in an order of YouTube Music
    fun selectRemoteSort(option: LibrarySortOption) {
        mutableState.update { it.copy(sort = LibrarySort.Recent, remoteSelected = option.title, items = emptyList(), continuation = null) }
        saveSort(RemotePrefix + option.title)
        load(option.token)
    }

    // Opens the search among what is loaded, and closes it
    fun startSearch() {
        mutableState.update { it.copy(query = "") }
    }

    fun setQuery(query: String) {
        mutableState.update { it.copy(query = query) }
    }

    fun stopSearch() {
        mutableState.update { it.copy(query = null) }
    }

    fun toggleGrid() {
        mutableState.update { it.copy(isGrid = !it.isGrid) }
    }

    // Asks the list that is chosen, again after a failure
    fun load(sortToken: String? = null) {
        val source = mutableState.value.source
        job?.cancel()
        mutableState.update { it.copy(status = LibraryStatus.Loading) }
        job = viewModelScope.launch {
            (if (sortToken != null) repository.more(sortToken) else repository.load(source))
                .onSuccess { page ->
                    var asked: LibrarySortOption? = null
                    mutableState.update {
                        // The orders come with the list asked in its own order, and stay while it is asked in another one
                        val hasOptions = sortToken == null && it.remoteSorts.isEmpty() && page.sortOptions.isNotEmpty()
                        it.copy(
                            status = LibraryStatus.Content,
                            items = page.items.distinctBy { item -> item.id },
                            continuation = page.continuation,
                            remoteSorts = if (hasOptions) page.sortOptions else it.remoteSorts,
                            remoteSelected = if (hasOptions) (page.sortOptions.firstOrNull { option -> option.isSelected } ?: (page.sortOptions.firstOrNull { option -> option.token == null } ?: page.sortOptions.first())).title else it.remoteSelected
                        ).also {
                            // The order chosen last time is asked once the list says which orders it has
                            if (hasOptions) asked = page.sortOptions.firstOrNull { option -> option.title == pendingRemote && option.token != null }
                        }
                    }
                    pendingRemote = null
                    asked?.let(::selectRemoteSort)
                }
                .onFailure { mutableState.update { it.copy(status = LibraryStatus.Error) } }
        }
    }

    // The next page of the list when the end is near
    fun loadMore() {
        val current = mutableState.value
        val token = current.continuation
        if (token == null || current.isLoadingMore || current.status != LibraryStatus.Content) return

        mutableState.update { it.copy(isLoadingMore = true) }
        job = viewModelScope.launch {
            repository.more(token)
                .onSuccess { page ->
                    mutableState.update {
                        val known = it.items.map { item -> item.id }.toSet()
                        it.copy(items = it.items + page.items.filter { item -> item.id !in known }, continuation = page.continuation, isLoadingMore = false)
                    }
                }
                .onFailure { mutableState.update { it.copy(isLoadingMore = false) } }
        }
    }

    // Hands over every song of an album or of a playlist of the list, after asking the rest of a long one
    fun withTracks(item: HomeItem, action: (List<HomeItem>) -> Unit) {
        val kind = if (item.kind == HomeItemKind.Album) CollectionKind.Album else CollectionKind.Playlist

        viewModelScope.launch {
            val page = collections.load(kind, item.id).getOrNull() ?: return@launch
            var tracks = page.tracks
            var continuation = page.continuation
            var pages = 0
            while (continuation != null && pages < CollectionDimens.MaxPages) {
                val more = collections.more(continuation).getOrNull() ?: break
                tracks = tracks + more.tracks
                continuation = more.continuation
                pages++
            }
            action(tracks.distinctBy { it.id })
        }
    }

    // Link of an album or of a playlist on YouTube Music, for sharing
    fun shareUrl(item: HomeItem): String =
        if (item.kind == HomeItemKind.Album) "$MusicOrigin/browse/${item.id}" else "$MusicOrigin/playlist?list=${item.id}"

    // Asks the list again and swaps it in without the placeholder, the order of YouTube Music that was chosen is not asked again
    private fun refresh() {
        val current = mutableState.value
        val isDefaultOrder = current.remoteSorts.isEmpty() || current.remoteSorts.firstOrNull { it.token == null }?.title == current.remoteSelected
        if (current.group == LibraryGroup.Downloads || current.status != LibraryStatus.Content || !isDefaultOrder) return

        viewModelScope.launch {
            repository.load(current.source).onSuccess { page ->
                mutableState.update { it.copy(items = page.items.distinctBy { item -> item.id }, continuation = page.continuation) }
            }
        }
    }

    // The list pulled down by the finger, asked again in the order that is on, with the indicator on until the answer comes
    fun pullRefresh() {
        val current = mutableState.value
        if (current.status != LibraryStatus.Content || current.isRefreshing || current.group == LibraryGroup.Downloads) return

        val token = current.remoteSorts.firstOrNull { it.title == current.remoteSelected }?.token
        mutableState.update { it.copy(isRefreshing = true) }
        viewModelScope.launch {
            (if (token != null) repository.more(token) else repository.load(current.source))
                .onSuccess { page ->
                    mutableState.update { it.copy(items = page.items.distinctBy { item -> item.id }, continuation = page.continuation, isRefreshing = false) }
                }
                .onFailure { mutableState.update { it.copy(isRefreshing = false) } }
        }
    }

    // Deletes a playlist of the account and takes it out of the list
    fun delete(item: HomeItem, onDone: (Boolean) -> Unit) {
        viewModelScope.launch {
            playlists.delete(item.id)
                .onSuccess { mutableState.update { it.copy(items = it.items.filter { other -> other.id != item.id }) } }
                .let { onDone(it.isSuccess) }
        }
    }

    // Opens a list in the order that was chosen for it the last time
    private fun open(source: LibrarySource) {
        viewModelScope.launch {
            restoreSort(source.name)
            load()
        }
    }

    private suspend fun restoreSort(key: String) {
        pendingRemote = null
        val saved = store.readSort(key) ?: return

        when {
            saved.startsWith(LocalPrefix) -> LibrarySort.entries.firstOrNull { it.name == saved.removePrefix(LocalPrefix) }?.let { sort ->
                mutableState.update { if (sort in it.sorts) it.copy(sort = sort) else it }
            }
            saved.startsWith(RemotePrefix) -> pendingRemote = saved.removePrefix(RemotePrefix)
        }
    }

    // Keeps the order of the list that is on for the next time
    private fun saveSort(value: String) {
        val key = sortKeyOf(mutableState.value)
        viewModelScope.launch { store.saveSort(key, value) }
    }

    // The name the order of the list in use is kept under, the downloads have no list of YouTube Music
    private fun sortKeyOf(state: LibraryUiState): String = if (state.group == LibraryGroup.Downloads) DownloadsSortKey else state.source.name

    // Keeps the button and the list that are on for the next time
    private fun saveChoice() {
        val current = mutableState.value
        viewModelScope.launch { store.save(current.group?.name, current.source.name.takeIf { current.group != null }) }
    }

    private fun defaultSort(source: LibrarySource): LibrarySort =
        if (source == LibrarySource.Recent) LibrarySort.Recent else LibrarySort.Saved
}

// How the order kept for a list says if it was made here or asked to YouTube Music
private const val DownloadsSortKey = "downloads"
private const val LocalPrefix = "local:"
private const val RemotePrefix = "remote:"

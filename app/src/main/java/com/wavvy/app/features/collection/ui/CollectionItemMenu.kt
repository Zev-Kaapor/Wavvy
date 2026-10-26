package com.wavvy.app.features.collection.ui

// Android context
import android.content.Context
// Coroutines
import kotlinx.coroutines.launch
import kotlin.random.Random
// Project resources
import com.wavvy.app.R
import com.wavvy.app.core.designsystem.components.Toasts
import com.wavvy.app.core.download.DownloadFolder
import com.wavvy.app.core.download.Downloads
import com.wavvy.app.core.innertube.MusicOrigin
import com.wavvy.app.core.navigation.ItemNavigator
import com.wavvy.app.core.playback.PlayerConnection
import com.wavvy.app.core.playback.RadioQueue
import com.wavvy.app.features.collection.data.CollectionKind
import com.wavvy.app.features.collection.data.CollectionRepository
import com.wavvy.app.features.home.data.HomeItem
import com.wavvy.app.features.home.data.HomeItemKind
import com.wavvy.app.features.home.ui.toPlayableTrack
import com.wavvy.app.features.playlist.data.PlaylistActions
import com.wavvy.app.features.playlist.ui.SaveToPlaylist

// The line under the name in the menu of an album or of a playlist, who made it and what the card says of it
private fun lineOf(item: HomeItem): String? =
    listOfNotNull(
        if (item.kind == HomeItemKind.Album) item.artists.joinToString(", ").ifEmpty { null } else item.author,
        item.countText
    ).joinToString(" • ").ifEmpty { null }

// Opens the menu of an album or of a playlist from its card, as the one of its own page is
// What the actions do to the songs is asked when one is used, and the page is asked for the save to the library, which comes into the menu when it arrives
fun showCollectionMenu(context: Context, item: HomeItem) {
    val appContext = context.applicationContext
    val repository = CollectionRepository(appContext)
    val kind = if (item.kind == HomeItemKind.Album) CollectionKind.Album else CollectionKind.Playlist
    val scope = PlaylistActions.scope

    // Hands over every song, after asking the rest of a long one
    fun withTracks(action: (List<HomeItem>) -> Unit) {
        scope.launch {
            val page = repository.load(kind, item.id).getOrNull() ?: return@launch Toasts.show(appContext.getString(R.string.playlist_action_error), isError = true)
            var tracks = page.tracks
            var continuation = page.continuation
            var pages = 0
            while (continuation != null && pages < CollectionDimens.MaxPages) {
                val more = repository.more(continuation).getOrNull() ?: break
                tracks = tracks + more.tracks
                continuation = more.continuation
                pages++
            }
            action(tracks.distinctBy { it.id })
        }
    }

    val shareUrl = if (kind == CollectionKind.Album) "$MusicOrigin/browse/${item.id}" else "$MusicOrigin/playlist?list=${item.id}"
    val base = CollectionMenuData(
        id = item.id,
        kindName = item.kind.name,
        title = item.title,
        subtitle = lineOf(item),
        coverUrl = item.thumbnailUrl,
        onShuffle = {
            withTracks { songs ->
                val tracks = songs.mapNotNull { it.toPlayableTrack() }
                if (tracks.isNotEmpty()) PlayerConnection.playAll(appContext, tracks, Random.nextInt(tracks.size), true)
            }
        },
        onPlayNext = { withTracks { songs -> PlayerConnection.enqueueAll(appContext, songs.mapNotNull { it.toPlayableTrack() }, afterCurrent = true) } },
        onAddToQueue = { withTracks { songs -> PlayerConnection.enqueueAll(appContext, songs.mapNotNull { it.toPlayableTrack() }, afterCurrent = false) } },
        onSaveToPlaylist = { withTracks { songs -> SaveToPlaylist.show(songs.map { it.id }) } },
        onShare = { shareLink(appContext, shareUrl) },
        onDownload = {
            withTracks { songs -> Downloads.enqueueAll(appContext, songs.mapNotNull { it.toPlayableTrack() }, DownloadFolder(item.id, item.title, item.thumbnailUrl)) }
        },
        downloadLabel = appContext.getString(R.string.download_do),
        // The radio of the first song, and the page of the artist of an album, found by the links of its first song
        onMix = { withTracks { songs -> songs.firstNotNullOfOrNull { it.toPlayableTrack() }?.let { PlayerConnection.play(appContext, it) } } },
        onGoToArtist = if (kind != CollectionKind.Album) null else ({
            withTracks { songs ->
                scope.launch {
                    val artistId = songs.firstOrNull()?.let { RadioQueue.links(appContext, it.id).getOrNull() }
                        ?.artists?.firstOrNull { artist -> artist.second.isNotEmpty() }?.second
                    if (artistId == null) Toasts.show(appContext.getString(R.string.menu_link_error), isError = true) else ItemNavigator.openArtist(artistId)
                }
            }
        })
    )
    CollectionMenu.show(base)

    // The save to the library needs the page, so it joins the menu when the page comes, if the menu is still the one of this card
    scope.launch {
        val page = repository.load(kind, item.id).getOrNull() ?: return@launch
        val target = page.libraryTarget ?: return@launch
        var isSaved = page.isInLibrary

        val withLibrary = base.copy(
            isInLibrary = isSaved,
            onToggleLibrary = {
                scope.launch {
                    val wanted = !isSaved
                    repository.setSaved(target, wanted)
                        .onSuccess {
                            isSaved = wanted
                            Toasts.show(appContext.getString(if (wanted) R.string.collection_library_saved else R.string.collection_library_removed))
                        }
                        .onFailure { Toasts.show(appContext.getString(R.string.playlist_action_error), isError = true) }
                }
            }
        )
        if (CollectionMenu.data.value?.id == item.id) CollectionMenu.show(withLibrary)
    }
}

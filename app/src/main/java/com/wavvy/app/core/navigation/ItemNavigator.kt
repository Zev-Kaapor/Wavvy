package com.wavvy.app.core.navigation

// Android utilities
import android.net.Uri
// Navigation
import androidx.navigation.NavController
// Project resources
import com.wavvy.app.features.home.data.HomeItem
import com.wavvy.app.features.home.data.HomeItemKind
import com.wavvy.app.features.home.data.HomeLink
import com.wavvy.app.features.home.data.PlaylistPagePrefix

// Route of the page of an album or of a playlist, the kind and the id are filled in when it opens, the title is the name of the shelf that led to it
const val CollectionRoute = "collection/{kind}/{id}?title={title}"
const val CollectionKindArg = "kind"
const val CollectionIdArg = "id"
const val CollectionTitleArg = "title"

// Route of the page of an artist
const val ArtistRoute = "artist/{id}"
const val ArtistIdArg = "id"

// Route of the full list behind a shelf of an artist, with the name that shows on top and the name of the shelf as the filter
const val DiscographyRoute = "discography/{id}?params={params}&title={title}&filter={filter}"
const val DiscographyIdArg = "id"
const val DiscographyParamsArg = "params"
const val DiscographyTitleArg = "title"
const val DiscographyFilterArg = "filter"

// Prefixes of the ids of the pages a shelf can open
private const val ArtistPrefix = "UC"
private const val AlbumPrefix = "MPRE"
private const val DiscographyPrefix = "MPAD"

// Opens the page a card or a shelf leads to from any screen, the navigation of the main screen is the one that does it
object ItemNavigator {
    private var controller: NavController? = null

    // The main screen hands its navigation here while it is on the screen
    fun attach(navController: NavController?) {
        controller = navController
    }

    // Opens the page of an album, of a playlist or of an artist, false for the cards that have no page yet
    // A title puts the name of the page next to the arrow, as a page that a shelf leads to has it, in place of the one under the cover
    fun open(item: HomeItem, title: String? = null): Boolean {
        val suffix = title?.let { "?title=${Uri.encode(it)}" }.orEmpty()
        val route = when (item.kind) {
            HomeItemKind.Album -> "collection/${HomeItemKind.Album.name}/${Uri.encode(item.id)}$suffix"
            HomeItemKind.Playlist -> "collection/${HomeItemKind.Playlist.name}/${Uri.encode(item.id)}$suffix"
            HomeItemKind.Artist -> "artist/${Uri.encode(item.id)}"
            else -> return false
        }
        val navController = controller ?: return false

        navController.navigate(route)
        return true
    }

    // Opens the page of an artist or of an album by its id, false when the navigation is not on the screen
    fun openArtist(id: String): Boolean {
        val navController = controller ?: return false
        navController.navigate("artist/${Uri.encode(id)}") { launchSingleTop = true }
        return true
    }

    fun openAlbum(id: String): Boolean {
        val navController = controller ?: return false
        navController.navigate("collection/${HomeItemKind.Album.name}/${Uri.encode(id)}") { launchSingleTop = true }
        return true
    }

    // Opens the page a shelf points to, a playlist, an album, an artist or the full list of an artist, false for the ones that have no page yet
    fun openLink(link: HomeLink, filter: String, title: String = filter): Boolean {
        val id = link.browseId
        val route = when {
            id.startsWith(PlaylistPagePrefix) -> "collection/${HomeItemKind.Playlist.name}/${Uri.encode(id.removePrefix(PlaylistPagePrefix))}?title=${Uri.encode(filter)}"
            id.startsWith(AlbumPrefix) -> "collection/${HomeItemKind.Album.name}/${Uri.encode(id)}"
            id.startsWith(ArtistPrefix) -> "artist/${Uri.encode(id)}"
            id.startsWith(DiscographyPrefix) -> "discography/${Uri.encode(id)}?params=${Uri.encode(link.params.orEmpty())}&title=${Uri.encode(title)}&filter=${Uri.encode(filter)}"
            else -> return false
        }
        val navController = controller ?: return false

        navController.navigate(route)
        return true
    }
}

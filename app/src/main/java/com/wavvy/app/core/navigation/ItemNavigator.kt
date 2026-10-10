package com.wavvy.app.core.navigation

// Android utilities
import android.net.Uri
// Navigation
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
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

// Route of a page of the Explore tab, with the parameters its button brings, the title and if the title is big
const val ExploreRoute = "explore/{id}?params={params}&title={title}&large={large}"
const val ExploreIdArg = "id"
const val ExploreParamsArg = "params"
const val ExploreTitleArg = "title"
const val ExploreLargeArg = "large"

// Pages of the Explore tab that are already built, the others do nothing yet
private val ExplorePages = setOf(
    "FEmusic_new_releases", "FEmusic_new_releases_albums", "FEmusic_new_releases_videos", "FEmusic_moods_and_genres_category",
    "FEmusic_charts", "FEmusic_moods_and_genres", "FEmusic_non_music_audio", "FEmusic_top_non_music_audio_shows",
    "FEmusic_top_non_music_audio_episodes"
)

// Pages of the Explore tab that start with a big title, whatever opens them
private val ExploreRoots = setOf(
    "FEmusic_new_releases", "FEmusic_charts", "FEmusic_moods_and_genres", "FEmusic_non_music_audio"
)

// Route of the page of an episode, the id is the one of its page and not the one of its video
const val EpisodeRoute = "episode/{id}"
const val EpisodeIdArg = "id"

// Route of the page of a podcast
const val PodcastRoute = "podcast/{id}"
const val PodcastIdArg = "id"

// Route of the screen of notifications
const val NotificationsRoute = "notifications"

// Prefixes of the ids of the pages a shelf can open
private const val ArtistPrefix = "UC"
private const val PodcastPrefix = "MPSP"
private const val EpisodePagePrefix = "MPED"
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
            HomeItemKind.Podcast -> "podcast/${Uri.encode(item.id)}"
            else -> return false
        }
        val navController = controller ?: return false

        navController.navigate(route)
        return true
    }

    // Opens a page of the Explore tab, false for the ones that are not built yet
    fun openExplore(browseId: String, params: String?, title: String, large: Boolean): Boolean {
        if (browseId !in ExplorePages) return false
        val navController = controller ?: return false

        navController.navigate("explore/${Uri.encode(browseId)}?params=${Uri.encode(params.orEmpty())}&title=${Uri.encode(title)}&large=${large || browseId in ExploreRoots}")
        return true
    }

    // Opens the page of an episode by its video, the id of its page is the id of the video with a prefix
    fun openEpisode(videoId: String): Boolean {
        val navController = controller ?: return false

        navController.navigate("episode/${Uri.encode(EpisodePagePrefix + videoId)}")
        return true
    }

    // Opens the page of a podcast by the id of its page
    fun openPodcast(id: String): Boolean {
        val navController = controller ?: return false

        navController.navigate("podcast/${Uri.encode(id)}")
        return true
    }

    // Goes to a tab of the bottom bar, the way a tap on the bar does
    fun openTab(route: String): Boolean {
        val navController = controller ?: return false

        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
        return true
    }

    // Opens the screen of notifications, once
    fun openNotifications(): Boolean {
        val navController = controller ?: return false
        navController.navigate(NotificationsRoute) { launchSingleTop = true }
        return true
    }

    // Opens the page of an artist or of an album by its id, false when the navigation is not on the screen
    fun openArtist(id: String): Boolean {
        val navController = controller ?: return false
        if (isOnTop(navController, ArtistRoute, ArtistIdArg, id)) return true

        navController.navigate("artist/${Uri.encode(id)}")
        return true
    }

    fun openAlbum(id: String): Boolean {
        val navController = controller ?: return false
        if (isOnTop(navController, CollectionRoute, CollectionIdArg, id)) return true

        navController.navigate("collection/${HomeItemKind.Album.name}/${Uri.encode(id)}")
        return true
    }

    // True when the page on the screen is this very one, so a repeated tap does not stack it again, another id of the same kind of page still opens
    private fun isOnTop(navController: NavController, route: String, argument: String, id: String): Boolean {
        val entry = navController.currentBackStackEntry ?: return false
        return entry.destination.route == route && entry.arguments?.getString(argument) == id
    }

    // Opens the page a shelf points to, a playlist, an album, an artist or the full list of an artist, false for the ones that have no page yet
    fun openLink(link: HomeLink, filter: String, title: String = filter): Boolean {
        val id = link.browseId
        val route = when {
            id.startsWith(PlaylistPagePrefix) -> "collection/${HomeItemKind.Playlist.name}/${Uri.encode(id.removePrefix(PlaylistPagePrefix))}?title=${Uri.encode(filter)}"
            id.startsWith(AlbumPrefix) -> "collection/${HomeItemKind.Album.name}/${Uri.encode(id)}"
            id.startsWith(ArtistPrefix) -> "artist/${Uri.encode(id)}"
            id.startsWith(PodcastPrefix) -> "podcast/${Uri.encode(id)}"
            id in ExplorePages -> return openExplore(id, link.params, title, large = false)
            id.startsWith(DiscographyPrefix) -> "discography/${Uri.encode(id)}?params=${Uri.encode(link.params.orEmpty())}&title=${Uri.encode(title)}&filter=${Uri.encode(filter)}"
            else -> return false
        }
        val navController = controller ?: return false

        navController.navigate(route)
        return true
    }
}

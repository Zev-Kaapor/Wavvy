package com.wavvy.app.features.collection.data

// Project resources
import com.wavvy.app.features.home.data.HomeItem
import com.wavvy.app.features.home.data.HomeSection
import com.wavvy.app.features.library.data.LibrarySortOption

// What a page lists, the songs of an album or the songs of a playlist
enum class CollectionKind { Album, Playlist }

// Page of an album or of a playlist, the continuation asks for the next songs of a long playlist
data class CollectionPage(
    val kind: CollectionKind,
    val title: String,
    // The kind and the year as YouTube Music writes them, who made it with their photo, and the line with the number of songs and the time they take
    val subtitle: String?,
    val owner: String?,
    val ownerPhotoUrl: String?,
    val details: String?,
    val description: String?,
    val thumbnailUrl: String?,
    val tracks: List<HomeItem>,
    val continuation: String?,
    // Shelves under the songs, such as the releases for the listener
    val sections: List<HomeSection>,
    // A playlist the account made can be changed, and says who can find it
    val isEditable: Boolean = false,
    val privacy: String? = null,
    // The orders that YouTube Music offers for the songs, empty when the page does not say them
    val sortOptions: List<LibrarySortOption> = emptyList()
)

// More songs of a long playlist
data class CollectionMore(
    val tracks: List<HomeItem>,
    val continuation: String?
)

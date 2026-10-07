package com.wavvy.app.features.home.data

// What a card of the Home is
enum class HomeItemKind { Song, Album, Playlist, Artist, Podcast, Episode }

// One card of a shelf, with the same data Metrolist keeps for it, the id opens it, a song or an episode by its video and the others by their page
data class HomeItem(
    val kind: HomeItemKind,
    val id: String,
    val title: String,
    val thumbnailUrl: String?,
    // Artists of a song or an album, the names that link to the page of an artist
    val artists: List<String> = emptyList(),
    // Owner of a playlist, author of a podcast or of an episode
    val author: String? = null,
    // Length of a song or an episode, in seconds
    val durationSeconds: Int? = null,
    // How many songs a playlist of the account has, how many times a song of an album was played, or the listeners of an artist, as YouTube Music writes it
    val countText: String? = null,
    val isExplicit: Boolean = false,
    // Playlist that plays an album, empty for the other kinds
    val playlistId: String? = null
)

// Page that a shelf opens when its title is tapped, with the filter parameters when it has them
data class HomeLink(
    val browseId: String,
    val params: String?,
    val isArtist: Boolean
)

// Row of cards with a title, the label is the small line above the title and the photo sits next to it when YouTube Music sends them
data class HomeSection(
    val title: String,
    val label: String?,
    val thumbnailUrl: String?,
    val link: HomeLink?,
    val items: List<HomeItem>
)

// Filter of the top of the Home, YouTube Music sends them already in the language of the request
data class HomeFilter(
    val title: String,
    val params: String
)

// An answer of the Home, the filters come only with the first page and the continuation asks for the next one
data class HomePage(
    val filters: List<HomeFilter>,
    val sections: List<HomeSection>,
    val continuation: String?
)

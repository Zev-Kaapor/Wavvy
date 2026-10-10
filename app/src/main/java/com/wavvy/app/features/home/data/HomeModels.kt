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
    val playlistId: String? = null,
    // The second line of a card as YouTube Music writes it, with the views and the age, and the word that tells the kind of an album, such as Single or EP, for the cards of the Discover tab
    val lineText: String? = null,
    val typeText: String? = null,
    // How a ranked item moved since the last chart, the name YouTube Music gives the arrow, such as ARROW_CHART_UP
    val trend: String? = null,
    // The length of an episode as YouTube Music writes it, such as 6 h 1 min
    val durationText: String? = null,
    // What an episode tells about itself, its description, how much of it was heard and the words of the button that plays it
    val description: String? = null,
    val progressPercent: Int? = null,
    val progressText: String? = null,
    // The place of a song in a playlist, which tells which one to take out when the song is there more than once
    val setVideoId: String? = null,
    // The page of the podcast an episode belongs to, when the list that has it says so
    val podcastId: String? = null
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

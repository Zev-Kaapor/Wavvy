package com.wavvy.app.features.library.data

// Project resources
import com.wavvy.app.features.home.data.HomeItem

// The lists of the library, each is a page of YouTube Music that is asked by its id
enum class LibrarySource(val browseId: String) {
    // What was used lately, all the kinds in one list
    Recent("FEmusic_library_landing"),
    Playlists("FEmusic_liked_playlists"),
    Podcasts("FEmusic_library_non_music_audio_list"),
    Channels("FEmusic_library_non_music_audio_channels_list"),
    Songs("FEmusic_liked_videos"),
    Albums("FEmusic_liked_albums"),

    // The artists of the songs that were saved, and the ones the account subscribes to
    Artists("FEmusic_library_corpus_track_artists"),
    Subscriptions("FEmusic_library_corpus_artists")
}

// An order that YouTube Music offers for a list, the token asks the list in that order and is empty for the order the list comes in
data class LibrarySortOption(
    val title: String,
    val token: String?,
    val isSelected: Boolean
)

// A page of a list of the library, the token asks the next one
data class LibraryPage(
    val items: List<HomeItem>,
    val continuation: String?,
    val sortOptions: List<LibrarySortOption> = emptyList()
)

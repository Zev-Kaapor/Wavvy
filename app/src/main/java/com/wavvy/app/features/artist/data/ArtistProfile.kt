package com.wavvy.app.features.artist.data

// A place of the web where the artist can be found, with the name of the site it is on
data class ArtistLink(
    val platform: String,
    val url: String,
    // True for the own site of the artist, which has no name of its own
    val isWebsite: Boolean = false
)

// What MusicBrainz knows about the artist, which YouTube Music does not say
data class ArtistProfile(
    // Whether it is a person or a group, as MusicBrainz writes it
    val type: String?,
    // The day the person was born or the group was formed, as MusicBrainz writes it, a whole date, a month or only a year
    val begin: String?,
    // The day it ended, when it did
    val end: String?,
    // The code of the country, the city it started in and the name of the area
    val countryCode: String?,
    val city: String?,
    val genres: List<String>,
    val links: List<ArtistLink>
) {
    // True when there is anything to show
    val hasContent: Boolean
        get() = begin != null || countryCode != null || city != null || genres.isNotEmpty() || links.isNotEmpty()
}

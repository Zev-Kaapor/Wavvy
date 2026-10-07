package com.wavvy.app.features.home.ui

// Project resources
import com.wavvy.app.core.innertube.resize
import com.wavvy.app.core.playback.PlayableTrack
import com.wavvy.app.features.home.data.HomeItem
import com.wavvy.app.features.home.data.HomeItemKind
import com.wavvy.app.features.home.ui.components.HomeDimens
import com.wavvy.app.features.home.ui.components.isVideo

// Milliseconds in a second, for the length of a song in the queue
private const val MillisPerSecond = 1000L

// A song or an episode as a track for the player, empty for the cards that open a page
fun HomeItem.toPlayableTrack(): PlayableTrack? {
    if (kind != HomeItemKind.Song && kind != HomeItemKind.Episode) return null

    return PlayableTrack(
        id = id,
        title = title,
        artist = artists.joinToString(", ").ifEmpty { author.orEmpty() }.ifEmpty { null },
        artworkUrl = thumbnailUrl?.resize(HomeDimens.CoverRequestSize, HomeDimens.CoverRequestSize),
        durationMs = durationSeconds?.let { it * MillisPerSecond } ?: 0L,
        isVideo = isVideo
    )
}

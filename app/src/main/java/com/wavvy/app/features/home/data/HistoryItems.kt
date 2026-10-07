package com.wavvy.app.features.home.data

// Project resources
import com.wavvy.app.core.history.PinnedEntity
import com.wavvy.app.core.history.SongEntity
import com.wavvy.app.core.playback.PlayableTrack

// Milliseconds in a second, for the length the cards show
private const val MillisPerSecond = 1000L

// A song the player found, such as one of a radio, as a card of the Home
fun PlayableTrack.toHomeItem(): HomeItem =
    HomeItem(
        kind = HomeItemKind.Song,
        id = id,
        title = title,
        thumbnailUrl = artworkUrl,
        artists = listOfNotNull(artist),
        durationSeconds = (durationMs / MillisPerSecond).toInt().takeIf { it > 0 }
    )

// A song pinned to the speed dial as a card of the Home
fun PinnedEntity.toHomeItem(): HomeItem =
    HomeItem(
        kind = HomeItemKind.Song,
        id = id,
        title = title,
        thumbnailUrl = artworkUrl,
        artists = listOfNotNull(artist),
        durationSeconds = (durationMs / MillisPerSecond).toInt().takeIf { it > 0 }
    )

// A song of the history as a card of the Home, the same as one that YouTube Music sends
fun SongEntity.toHomeItem(): HomeItem =
    HomeItem(
        kind = HomeItemKind.Song,
        id = id,
        title = title,
        thumbnailUrl = artworkUrl,
        artists = listOfNotNull(artist),
        durationSeconds = (durationMs / MillisPerSecond).toInt().takeIf { it > 0 }
    )

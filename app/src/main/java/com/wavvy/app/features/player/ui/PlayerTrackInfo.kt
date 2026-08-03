package com.wavvy.app.features.player.ui

// Media3 player components
import androidx.media3.common.MediaItem
// Compose runtime
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
// Project resources
import com.wavvy.app.R

// Resolved track info
data class PlayerTrackInfo(
    val activeImageUrl: String,
    val title: String,
    val artistNames: List<String>,
    val cleanArtistName: String
)

// Track metadata resolver
@Composable
fun rememberPlayerTrackInfo(
    currentTrackInfo: PlayerViewModel.TrackInfo?,
    currentMediaItem: MediaItem?,
    imageUrl: String?,
    initialTitle: String?,
    initialArtist: String?
): PlayerTrackInfo {
    val defaultTitle = stringResource(R.string.default_song_title)
    val defaultArtist = stringResource(R.string.default_artist_name)

    val activeImageUrl = remember(currentTrackInfo, imageUrl) {
        currentTrackInfo?.imageUrl ?: imageUrl ?: ""
    }

    val songTitle = remember(currentTrackInfo, currentMediaItem, initialTitle) {
        currentTrackInfo?.title
            ?: currentMediaItem?.mediaMetadata?.title?.toString()
            ?: initialTitle
            ?: defaultTitle
    }

    val extractedArtist = remember(currentTrackInfo, currentMediaItem, initialArtist) {
        currentTrackInfo?.artist
            ?: currentMediaItem?.mediaMetadata?.artist?.toString()
            ?: initialArtist
            ?: defaultArtist
    }

    val artistNames = remember(extractedArtist) {
        extractedArtist.split(",")
            .map { it.trim() }
            .filter { it.isNotBlank() && it != defaultArtist }
            .ifEmpty { listOf(defaultArtist) }
    }

    val cleanArtistName = remember(artistNames) {
        artistNames.joinToString(", ")
    }

    return PlayerTrackInfo(
        activeImageUrl = activeImageUrl,
        title = songTitle,
        artistNames = artistNames,
        cleanArtistName = cleanArtistName
    )
}

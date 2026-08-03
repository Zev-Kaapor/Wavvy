package com.wavvy.app.features.home.ui

// Compose runtime and state operations
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver

// State holder for playback UI
@Stable
class PlayerState(
    isMiniPlayerActive: Boolean = false,
    isPlayerExpanded: Boolean = false,
    isQueueActive: Boolean = false,
    currentSongTitle: String = "",
    currentArtistNames: List<String> = emptyList(),
    currentImageUrl: String? = null,
    currentSongUrl: String? = null
) {
    var isMiniPlayerActive by mutableStateOf(isMiniPlayerActive)
    var isPlayerExpanded by mutableStateOf(isPlayerExpanded)
    var isQueueActive by mutableStateOf(isQueueActive)
    var currentSongTitle by mutableStateOf(currentSongTitle)
    var currentArtistNames by mutableStateOf(currentArtistNames)
    var currentImageUrl by mutableStateOf(currentImageUrl)
    var currentSongUrl by mutableStateOf(currentSongUrl)

    // Update playback state
    fun updatePlayback(title: String, artists: List<String>, imageUrl: String? = null, url: String? = null, expand: Boolean = false) {
        currentSongTitle = title
        currentArtistNames = artists
        currentImageUrl = imageUrl
        currentSongUrl = url
        isMiniPlayerActive = true
        isPlayerExpanded = expand
        isQueueActive = false
    }

    // Play all quick choices songs
    fun playAllQuickChoices(artists: List<String>, mixTitle: String) {
        currentSongTitle = mixTitle
        currentArtistNames = artists
        currentImageUrl = null
        currentSongUrl = null
        isMiniPlayerActive = true
        isPlayerExpanded = false
        isQueueActive = false
    }

    companion object {
        val Saver: Saver<PlayerState, *> = listSaver(
            save = {
                listOf(
                    it.isMiniPlayerActive,
                    it.isPlayerExpanded,
                    it.isQueueActive,
                    it.currentSongTitle,
                    it.currentArtistNames,
                    it.currentImageUrl,
                    it.currentSongUrl
                )
            },
            restore = {
                @Suppress("UNCHECKED_CAST")
                PlayerState(
                    isMiniPlayerActive = it[0] as Boolean,
                    isPlayerExpanded = it[1] as Boolean,
                    isQueueActive = it[2] as Boolean,
                    currentSongTitle = it[3] as String,
                    currentArtistNames = it[4] as List<String>,
                    currentImageUrl = it[5] as? String,
                    currentSongUrl = it[6] as? String
                )
            }
        )
    }
}

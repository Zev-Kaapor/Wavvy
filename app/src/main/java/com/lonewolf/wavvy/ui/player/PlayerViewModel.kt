package com.lonewolf.wavvy.ui.player

// Android core architecture components
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
// Coroutines state observation flows
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
// Project background infrastructure
import com.lonewolf.wavvy.data.RecentHistoryManager
import com.lonewolf.wavvy.ui.home.components.RecentTrack

// Shared and internal components
class PlayerViewModel(application: Application) : AndroidViewModel(application) {

    private val playerManager = PlayerManager(application)
    private val recentHistoryManager = RecentHistoryManager(application)

    val isPlaying = playerManager.isPlaying
    val currentMediaItem = playerManager.currentMediaItem
    val duration = playerManager.duration
    val progress = playerManager.progress

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Process player activation and track media loading
    fun loadAndPlay(youtubeUrl: String, title: String, artist: String, imageUrl: String) {
        viewModelScope.launch {
            _isLoading.value = true

            val videoId = youtubeUrl.substringAfter("v=").substringBefore("&")
            val directAudioUrl = ExtractorHelper.extractAudioUrl(getApplication<Application>(), videoId)

            if (directAudioUrl != null) {
                val track = RecentTrack(
                    id = videoId,
                    title = title,
                    artist = artist,
                    imageUrl = imageUrl
                )
                recentHistoryManager.saveTrack(track)

                playerManager.playTrack(
                    url = directAudioUrl,
                    title = title,
                    artist = artist,
                    imageUrl = imageUrl
                )
            }

            _isLoading.value = false
        }
    }

    // Direct interface player interaction control
    fun togglePlayPause() {
        playerManager.playPause()
    }

    // Direct interface player interaction control
    fun seekTo(positionMs: Long) {
        playerManager.seekTo(positionMs)
    }

    // Infrastructure lifecycle breakdown handler
    override fun onCleared() {
        super.onCleared()
        playerManager.release()
    }
}

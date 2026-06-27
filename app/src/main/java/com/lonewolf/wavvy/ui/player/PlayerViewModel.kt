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

// Playback error states
sealed class PlaybackError {
    object ExtractionFailed : PlaybackError()
    object PlayerNotReady : PlaybackError()
}

// Player UI state manager
class PlayerViewModel(application: Application) : AndroidViewModel(application) {

    private val playerManager = PlayerManager(application)
    private val recentHistoryManager = RecentHistoryManager(application)

    val isPlaying = playerManager.isPlaying
    val currentMediaItem = playerManager.currentMediaItem
    val duration = playerManager.duration
    val progress = playerManager.progress

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<PlaybackError?>(null)
    val error: StateFlow<PlaybackError?> = _error.asStateFlow()

    // Current track metadata for immediate UI updates
    data class TrackInfo(val title: String, val artist: String, val imageUrl: String)
    private val _currentTrackInfo = MutableStateFlow<TrackInfo?>(null)
    val currentTrackInfo: StateFlow<TrackInfo?> = _currentTrackInfo.asStateFlow()

    // Timestamp of user tap (perf test)
    private var perfClickTimestamp: Long = 0L
    private var pendingTrackForRecent: RecentTrack? = null

    init {
        // Prewarm extractor early in lifecycle
        viewModelScope.launch {
            ExtractorHelper.initExtractor()
        }

        // Save to recent only when playback actually starts
        viewModelScope.launch {
            playerManager.isPlaying.collect { isPlaying ->
                if (isPlaying && pendingTrackForRecent != null) {
                    recentHistoryManager.saveTrack(pendingTrackForRecent!!)
                    pendingTrackForRecent = null
                }
            }
        }

        // Track playback start latency (perf test)
        viewModelScope.launch {
            playerManager.playbackStartedTimestamp.collect { startedAt ->
                if (startedAt > 0L && perfClickTimestamp > 0L) {
                    val total = startedAt - perfClickTimestamp
                    android.util.Log.d("PerfTest", "t3 — audio actually playing. TOTAL: ${total}ms")
                }
            }
        }
    }

    // Extract and play track
    fun loadAndPlay(youtubeUrl: String, title: String, artist: String, imageUrl: String) {
        perfClickTimestamp = System.currentTimeMillis()
        android.util.Log.d("PerfTest", "t0 — click received")

        // Pause immediately while new track loads
        playerManager.pause()

        // Update UI immediately with track info
        _currentTrackInfo.value = TrackInfo(title, artist, imageUrl)
        playerManager.resetProgress()

        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            val videoId = youtubeUrl.substringAfter("v=").substringBefore("&")
            val directAudioUrl = ExtractorHelper.extractAudioUrl(getApplication<Application>(), videoId)

            android.util.Log.d(
                "PerfTest",
                "t1 — extraction done at +${System.currentTimeMillis() - perfClickTimestamp}ms"
            )

            if (directAudioUrl == null) {
                _error.value = PlaybackError.ExtractionFailed
                _isLoading.value = false
                return@launch
            }

            // Defer saving to recent until playback actually starts
            pendingTrackForRecent = RecentTrack(
                id = videoId,
                title = title,
                artist = artist,
                imageUrl = imageUrl
            )

            // Attempt playback
            val started = playerManager.playTrack(
                url = directAudioUrl,
                title = title,
                artist = artist,
                imageUrl = imageUrl
            )

            android.util.Log.d(
                "PerfTest",
                "t2 — play() called at +${System.currentTimeMillis() - perfClickTimestamp}ms"
            )

            if (!started) {
                _error.value = PlaybackError.PlayerNotReady
                pendingTrackForRecent = null
            }

            _isLoading.value = false
        }
    }

    // Toggle playback
    fun togglePlayPause() {
        playerManager.playPause()
    }

    // Seek to position
    fun seekTo(positionMs: Long) {
        playerManager.seekTo(positionMs)
    }

    // Clear error state
    fun clearError() {
        _error.value = null
    }

    // Cleanup on destruction
    override fun onCleared() {
        super.onCleared()
        playerManager.release()
    }
}

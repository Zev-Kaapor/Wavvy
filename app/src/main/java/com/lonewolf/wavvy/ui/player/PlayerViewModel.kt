package com.lonewolf.wavvy.ui.player

// Android core architecture components
import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
// Coroutines state observation flows
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
// Project background infrastructure
import com.lonewolf.wavvy.data.RecentHistoryManager
import com.lonewolf.wavvy.ui.home.components.RecentTrack
import com.lonewolf.wavvy.ui.player.components.QueueSong
import com.lonewolf.wavvy.ui.player.service.MusicService

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

    private val _isLoadingMore = MutableStateFlow(false)
    val isLoadingMore: StateFlow<Boolean> = _isLoadingMore.asStateFlow()

    private val _error = MutableStateFlow<PlaybackError?>(null)
    val error: StateFlow<PlaybackError?> = _error.asStateFlow()

    // Current track metadata for immediate UI updates
    data class TrackInfo(val title: String, val artist: String, val imageUrl: String)
    private val _currentTrackInfo = MutableStateFlow<TrackInfo?>(null)
    val currentTrackInfo: StateFlow<TrackInfo?> = _currentTrackInfo.asStateFlow()

    // Active playlist structure reference sequence state flow
    private val _currentQueue = MutableStateFlow<List<QueueSong>>(emptyList())
    val currentQueue: StateFlow<List<QueueSong>> = _currentQueue.asStateFlow()

    // Playback modes state
    private val _repeatMode = MutableStateFlow(0)
    val repeatMode: StateFlow<Int> = _repeatMode.asStateFlow()

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

        viewModelScope.launch {
            playerManager.currentMediaItem.collect { mediaItem ->
                mediaItem?.let { syncTrackInfoFromMediaItem(it) }
            }
        }
    }

    // Derive TrackInfo straight from the Media3 metadata of the currently playing item
    private fun syncTrackInfoFromMediaItem(mediaItem: MediaItem) {
        val metadata = mediaItem.mediaMetadata
        val title = metadata.title?.toString() ?: return
        val artist = metadata.artist?.toString() ?: ""

        // First try to get image from metadata
        var imageUrl = metadata.artworkUri?.toString() ?: ""

        // If metadata doesn't have image, search in queue by multiple criteria
        if (imageUrl.isBlank()) {
            val queueItem = _currentQueue.value.find { queueSong ->
                queueSong.id == mediaItem.mediaId ||
                        (queueSong.title.equals(title, ignoreCase = true) && queueSong.artist.equals(artist, ignoreCase = true))
            }

            imageUrl = queueItem?.imageUrl ?: ""
        }

        _currentTrackInfo.value = TrackInfo(title, artist, imageUrl)
    }

    // Fetch and append new tracks to make the playback queue infinite
    fun loadMoreQueueSongs() {
        if (_isLoadingMore.value) return

        val currentList = _currentQueue.value
        if (currentList.isEmpty()) return

        viewModelScope.launch {
            _isLoadingMore.value = true
            try {
                val lastVideoId = currentList.last().id
                val nextTracks = ExtractorHelper.fetchUpNextQueue(getApplication(), lastVideoId)

                if (nextTracks.isNotEmpty()) {
                    val existingIds = currentList.map { it.id }.toSet()
                    val filteredNewTracks = nextTracks.filter { it.id !in existingIds }

                    if (filteredNewTracks.isNotEmpty()) {
                        val updatedQueue = currentList + filteredNewTracks
                        _currentQueue.value = updatedQueue

                        val intent = Intent(getApplication(), MusicService::class.java).apply {
                            putExtra("EXTRA_PLAYLIST", ArrayList(updatedQueue))
                            putExtra("EXTRA_IS_APPEND", true)
                        }
                        getApplication<Application>().startService(intent)
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("PlayerViewModel", "Failed to load more queue songs", e)
            } finally {
                _isLoadingMore.value = false
            }
        }
    }

    // Extract and play a full queue from NewPipe list data structures
    fun loadAndPlayQueue(playlist: List<QueueSong>, startIndex: Int) {
        if (playlist.isEmpty()) return

        viewModelScope.launch {
            _error.value = null

            val targetTrack = playlist[startIndex]
            _currentTrackInfo.value = TrackInfo(
                targetTrack.title,
                targetTrack.artist,
                targetTrack.imageUrl
            )

            // Sync layout changes to upstream collectors
            _currentQueue.value = playlist

            // Sync with backend notification media background services layout binding
            val intent = Intent(getApplication(), MusicService::class.java).apply {
                putExtra("EXTRA_PLAYLIST", ArrayList(playlist))
                putExtra("EXTRA_START_INDEX", startIndex)
            }
            getApplication<Application>().startService(intent)

            // Force immediate playback kickstart on manual selection
            playerManager.play()
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
            _error.value = null

            val videoId = youtubeUrl.substringAfter("v=").substringBefore("&")
            val directAudioUrl = ExtractorHelper.extractAudioUrl(getApplication<Application>(), videoId)

            android.util.Log.d(
                "PerfTest",
                "t1 — extraction done at +${System.currentTimeMillis() - perfClickTimestamp}ms"
            )

            if (directAudioUrl == null) {
                _error.value = PlaybackError.ExtractionFailed
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
                return@launch
            }

            // Fetch auto-generated rádio context queue using up-next endpoints async pipeline
            val upNextPlaylist = ExtractorHelper.fetchUpNextQueue(getApplication<Application>(), videoId)

            // Build absolute timeline list including target track as initial item index
            val fullQueue = mutableListOf<QueueSong>().apply {
                add(QueueSong(
                    id = videoId,
                    title = title,
                    artist = artist,
                    imageUrl = imageUrl
                ))
                addAll(upNextPlaylist)
            }

            // Sync structural changes upstream to subscribers
            _currentQueue.value = fullQueue

            // Forward generated queue array downstream to keep UI layouts safely synced
            val intent = Intent(getApplication(), MusicService::class.java).apply {
                putExtra("EXTRA_PLAYLIST", ArrayList(fullQueue))
                putExtra("EXTRA_START_INDEX", 0)
            }
            getApplication<Application>().startService(intent)
        }
    }

    // Toggle playback
    fun togglePlayPause() {
        playerManager.playPause()
    }

    // Skip to next track
    fun skipToNext() {
        playerManager.next()
    }

    // Skip to previous track
    fun skipToPrevious() {
        playerManager.previous()
    }

    // Seek to position
    fun seekTo(positionMs: Long) {
        playerManager.seekTo(positionMs)
    }

    // Cycle through repeat modes (0 = off, 1 = all, 2 = one)
    fun toggleRepeatMode() {
        val nextMode = (_repeatMode.value + 1) % 3
        _repeatMode.value = nextMode
        playerManager.setRepeatMode(nextMode)
    }

    // Resolve track index dynamically from memory sequence map
    fun getCurrentIndex(playlist: List<QueueSong>): Int {
        val currentUrl = currentMediaItem.value?.mediaId ?: return 0
        val index = playlist.indexOfFirst { it.id == currentUrl || it.title == _currentTrackInfo.value?.title }
        return if (index != -1) index else 0
    }

    // Stop and clear playback entirely
    fun stopPlayback() {
        playerManager.pause()
        playerManager.resetProgress()
        _currentTrackInfo.value = null
    }

    // Cleanup on destruction
    override fun onCleared() {
        super.onCleared()
        playerManager.release()
    }
}

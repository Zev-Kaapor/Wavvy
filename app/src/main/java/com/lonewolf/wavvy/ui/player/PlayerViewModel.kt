package com.lonewolf.wavvy.ui.player

// Android core architecture components
import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
// Coroutines state observation flows
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
// Project background infrastructure
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import java.util.LinkedHashMap
import com.lonewolf.wavvy.data.RecentHistoryManager
import com.lonewolf.wavvy.ui.home.components.RecentTrack
import com.lonewolf.wavvy.ui.player.components.QueueSong
import com.lonewolf.wavvy.ui.player.service.MusicService

// Playback error states
sealed class PlaybackError {
    object ExtractionFailed : PlaybackError()
}

// Metadata-only track cache, never audio urls
private object TrackMetadataCache {
    private const val CAPACITY = 40

    private val map = object : LinkedHashMap<String, QueueSong>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, QueueSong>?): Boolean {
            return size > CAPACITY
        }
    }

    @Synchronized
    fun get(id: String): QueueSong? = map[id]

    @Synchronized
    fun putAll(songs: List<QueueSong>) {
        songs.forEach { map[it.id] = it }
    }
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

    // True while a track swap is in progress
    private val _isTrackLoading = MutableStateFlow(false)
    val isTrackLoading: StateFlow<Boolean> = _isTrackLoading.asStateFlow()

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

    // Generation token to drop stale async results
    private var loadGeneration = 0L

    private val totalWait: Duration = 2500.milliseconds
    private val mediaItemPollInterval: Duration = 100.milliseconds
    private val durationWait: Duration = 1200.milliseconds
    private val smallPoll: Duration = 50.milliseconds

    init {
        viewModelScope.launch { ExtractorHelper.initExtractor() }

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

        // If metadata doesn't have image, search in queue, then in metadata cache
        if (imageUrl.isBlank()) {
            val queueItem = _currentQueue.value.find { queueSong ->
                queueSong.id == mediaItem.mediaId ||
                        (queueSong.title.equals(title, ignoreCase = true) && queueSong.artist.equals(artist, ignoreCase = true))
            }
            imageUrl = queueItem?.imageUrl ?: TrackMetadataCache.get(mediaItem.mediaId)?.imageUrl ?: ""
        }
        _currentTrackInfo.value = TrackInfo(title, artist, imageUrl)
    }

    // Waits for the controller to reflect the new track
    private suspend fun awaitTrackSwap(myGeneration: Long, targetId: String) {
        val monitored = withTimeoutOrNull(totalWait) {
            while (true) {
                val ready = playerManager.awaitReady()
                if (!ready) { delay(mediaItemPollInterval); continue }
                val current = playerManager.currentMediaItem.value
                if (current != null && current.mediaId == targetId) return@withTimeoutOrNull true
                delay(mediaItemPollInterval)
            }
        }
        if (monitored == null) android.util.Log.d("PlayerViewModel", "Service did not reflect media item within timeout.")

        // Only clear loading if this is still the active request
        if (myGeneration == loadGeneration) {
            _isTrackLoading.value = false
        }
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
                        TrackMetadataCache.putAll(filteredNewTracks)
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
        val myGeneration = ++loadGeneration
        val targetTrack = playlist[startIndex]

        // Pause immediately to avoid resuming the old track mid-swap
        playerManager.pause()
        _isTrackLoading.value = true
        _currentTrackInfo.value = TrackInfo(targetTrack.title, targetTrack.artist, targetTrack.imageUrl)
        playerManager.resetProgress()

        viewModelScope.launch {
            _error.value = null
            TrackMetadataCache.putAll(playlist)
            _currentQueue.value = playlist
            // Sync with backend notification media background services layout binding
            val intent = Intent(getApplication(), MusicService::class.java).apply {
                putExtra("EXTRA_PLAYLIST", ArrayList(playlist))
                putExtra("EXTRA_START_INDEX", startIndex)
                putExtra(MusicService.EXTRA_AUTOPLAY, true)
            }
            getApplication<Application>().startService(intent)

            awaitTrackSwap(myGeneration, targetTrack.id)
        }
    }

    // Extract and play track
    fun loadAndPlay(youtubeUrl: String, title: String, artist: String, imageUrl: String) {
        perfClickTimestamp = System.currentTimeMillis()
        android.util.Log.d("PerfTest", "t0 — click received")

        val myGeneration = ++loadGeneration

        // Pause immediately while new track loads
        playerManager.pause()
        _isTrackLoading.value = true
        // Update UI immediately with track info
        _currentTrackInfo.value = TrackInfo(title, artist, imageUrl)
        playerManager.resetProgress()

        viewModelScope.launch {
            _error.value = null
            val videoId = youtubeUrl.substringAfter("v=").substringBefore("&")
            val directAudioUrl = ExtractorHelper.extractAudioUrl(getApplication<Application>(), videoId)
            android.util.Log.d("PerfTest", "t1 — extraction done at +${System.currentTimeMillis() - perfClickTimestamp}ms")

            // Drop stale result from a superseded click
            if (myGeneration != loadGeneration) return@launch

            if (directAudioUrl == null) {
                _error.value = PlaybackError.ExtractionFailed
                _isTrackLoading.value = false
                return@launch
            }

            pendingTrackForRecent = RecentTrack(id = videoId, title = title, artist = artist, imageUrl = imageUrl)
            val upNextPlaylist = ExtractorHelper.fetchUpNextQueue(getApplication<Application>(), videoId)

            // Drop stale result from a superseded click
            if (myGeneration != loadGeneration) return@launch

            val fullQueue = mutableListOf<QueueSong>().apply {
                add(QueueSong(id = videoId, title = title, artist = artist, imageUrl = imageUrl))
                addAll(upNextPlaylist)
            }
            TrackMetadataCache.putAll(fullQueue)
            _currentQueue.value = fullQueue

            val intent = Intent(getApplication(), MusicService::class.java).apply {
                putExtra("EXTRA_PLAYLIST", ArrayList(fullQueue))
                putExtra("EXTRA_START_INDEX", 0)
                putExtra("EXTRA_START_AUDIO_URL", directAudioUrl)
                putExtra(MusicService.EXTRA_AUTOPLAY, true)
            }
            getApplication<Application>().startService(intent)

            awaitTrackSwap(myGeneration, videoId)
        }
    }

    // Ignore taps while a track swap is in progress
    fun togglePlayPause() {
        if (_isTrackLoading.value) return
        playerManager.playPause()
    }

    fun skipToNext() { playerManager.next() }
    fun skipToPrevious() { playerManager.previous() }
    fun seekTo(positionMs: Long) { playerManager.seekTo(positionMs) }

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
        // Invalidate any load still in flight
        loadGeneration++
        _isTrackLoading.value = false
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

package com.lonewolf.wavvy.ui.player

// Android core architecture components
import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.localbroadcastmanager.content.LocalBroadcastManager
// Coroutines state observation flows
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
// Project background infrastructure
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import java.util.LinkedHashMap
import com.lonewolf.wavvy.data.RecentHistoryManager
import com.lonewolf.wavvy.ui.home.components.RecentTrack
import com.lonewolf.wavvy.ui.player.components.QueueSong
import com.lonewolf.wavvy.ui.player.extractor.ExtractorHelper
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

    private val _error = MutableStateFlow<PlaybackError?>(null)
    val error: StateFlow<PlaybackError?> = _error.asStateFlow()

    // Combined state for active blocking actions
    val isBusy: StateFlow<Boolean> = _error.map { error ->
        error != null
    }.stateIn(viewModelScope, SharingStarted.Eagerly, false)

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

    // Pending job that saves the current track once it's confirmed playing
    private var recentTrackJob: Job? = null

    // Snapshot of the queue order right before the last shuffle, used to restore it
    private var preShuffleOrder: List<QueueSong>? = null

    // Generation token to drop stale async results
    private var loadGeneration = 0L

    // Seek job in progress
    private var seekJob: Job? = null

    private val totalWait: Duration = 2500.milliseconds
    private val mediaItemPollInterval: Duration = 100.milliseconds
    private val smallPoll: Duration = 50.milliseconds

    private val loadMoreReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == MusicService.ACTION_LOAD_MORE_QUEUE) {
                android.util.Log.d("PlayerViewModel", "Received LOAD_MORE signal from service")
                loadMoreQueueSongs()
            }
        }
    }

    init {
        viewModelScope.launch { ExtractorHelper.initExtractor() }

        val filter = IntentFilter(MusicService.ACTION_LOAD_MORE_QUEUE)
        LocalBroadcastManager.getInstance(getApplication()).registerReceiver(loadMoreReceiver, filter)

        // Save whatever track is actually playing, no matter how it started
        viewModelScope.launch {
            playerManager.currentMediaItem.collect { mediaItem ->
                recentTrackJob?.cancel()
                if (mediaItem == null) return@collect
                val mediaId = mediaItem.mediaId
                if (mediaId.isBlank()) return@collect

                recentTrackJob = viewModelScope.launch {
                    // Wait until this exact track is confirmed playing
                    playerManager.isPlaying.first { playing ->
                        playing && playerManager.currentMediaItem.value?.mediaId == mediaId
                    }
                    val info = resolveTrackInfo(mediaItem) ?: return@launch
                    recentHistoryManager.saveTrack(
                        RecentTrack(id = mediaId, title = info.title, artist = info.artist, imageUrl = info.imageUrl)
                    )
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
                mediaItem?.let { _currentTrackInfo.value = resolveTrackInfo(it) }
            }
        }

        // Fetch more tracks automatically once playback lands on the last queued song
        viewModelScope.launch {
            combine(playerManager.currentMediaItem, _currentQueue) { mediaItem, queue -> mediaItem to queue }
                .collect { (mediaItem, queue) ->
                    if (mediaItem == null || queue.isEmpty()) return@collect
                    val isLastTrack = queue.last().id == mediaItem.mediaId
                    if (isLastTrack) loadMoreQueueSongs()
                }
        }
    }

    // Resolve display info for a media item from its own metadata, then queue, then cache
    private fun resolveTrackInfo(mediaItem: MediaItem): TrackInfo? {
        val metadata = mediaItem.mediaMetadata
        val title = metadata.title?.toString() ?: return null
        val artist = metadata.artist?.toString() ?: ""
        var imageUrl = metadata.artworkUri?.toString() ?: ""

        if (imageUrl.isBlank()) {
            val queueItem = _currentQueue.value.find { queueSong ->
                queueSong.id == mediaItem.mediaId ||
                        (queueSong.title.equals(title, ignoreCase = true) && queueSong.artist.equals(artist, ignoreCase = true))
            }
            imageUrl = queueItem?.imageUrl ?: TrackMetadataCache.get(mediaItem.mediaId)?.imageUrl ?: ""
        }
        return TrackInfo(title, artist, imageUrl)
    }

    // Waits for the controller to reflect the new track
    private suspend fun awaitTrackSwap(targetId: String) {
        val monitored = withTimeoutOrNull(totalWait) {
            while (true) {
                val ready = playerManager.awaitReady()
                if (!ready) { delay(mediaItemPollInterval); continue }
                val current = playerManager.currentMediaItem.value
                val isTargetTrack = current != null && current.mediaId == targetId
                if (isTargetTrack && playerManager.isPlaying.value) return@withTimeoutOrNull true
                delay(mediaItemPollInterval)
            }
        }
        if (monitored == null) android.util.Log.d("PlayerViewModel", "Service did not reflect media item within timeout.")
    }

    // Updates local state and pushes an ordered queue to the service without restarting playback
    private fun applyQueueEdit(newQueue: List<QueueSong>) {
        TrackMetadataCache.putAll(newQueue)
        _currentQueue.value = newQueue
        val intent = Intent(getApplication(), MusicService::class.java).apply {
            putExtra("EXTRA_PLAYLIST", ArrayList(newQueue))
            putExtra(MusicService.EXTRA_SYNC_QUEUE, true)
        }
        getApplication<Application>().startService(intent)
    }

    // Commit a full reorder, e.g. after a drag-and-drop gesture in the queue
    fun commitQueueOrder(newOrder: List<QueueSong>) {
        if (newOrder.map { it.id } == _currentQueue.value.map { it.id }) return
        applyQueueEdit(newOrder)
    }

    // Remove a track from the queue
    fun removeFromQueue(songId: String) {
        val updated = _currentQueue.value.filterNot { it.id == songId }
        if (updated.size != _currentQueue.value.size) applyQueueEdit(updated)
    }

    // Move a track to play right after the one that's currently playing
    fun playNext(songId: String) {
        val current = _currentQueue.value
        val fromIndex = current.indexOfFirst { it.id == songId }
        if (fromIndex == -1) return

        val playingId = currentMediaItem.value?.mediaId
        val playingIndex = current.indexOfFirst { it.id == playingId }.takeIf { it != -1 } ?: 0

        val mutable = current.toMutableList()
        val item = mutable.removeAt(fromIndex)
        val insertPos = if (fromIndex < playingIndex) playingIndex else playingIndex + 1
        mutable.add(insertPos.coerceIn(0, mutable.size), item)
        applyQueueEdit(mutable)
    }

    // Shuffle every track except the one currently playing, which keeps its spot
    fun shuffleQueue() {
        val current = _currentQueue.value
        if (current.size <= 2) return

        preShuffleOrder = current

        val playingId = currentMediaItem.value?.mediaId
        val playingIndex = current.indexOfFirst { it.id == playingId }.takeIf { it != -1 } ?: 0
        val playingItem = current[playingIndex]

        val shuffledRest = current.filterIndexed { index, _ -> index != playingIndex }.shuffled()
        val reordered = shuffledRest.toMutableList().apply {
            add(playingIndex.coerceIn(0, size), playingItem)
        }
        applyQueueEdit(reordered)
    }

    // Restore the queue order captured right before the last shuffle
    fun unshuffleQueue() {
        val original = preShuffleOrder ?: return
        val current = _currentQueue.value
        val currentIds = current.map { it.id }.toSet()

        // Keep original relative order for songs still present
        val restored = original.filter { it.id in currentIds }.toMutableList()
        val restoredIds = restored.map { it.id }.toSet()
        // Append anything added after the shuffle (e.g. infinite scroll) at the end
        restored.addAll(current.filterNot { it.id in restoredIds })

        preShuffleOrder = null
        applyQueueEdit(restored)
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
                            putParcelableArrayListExtra("EXTRA_PLAYLIST", ArrayList(updatedQueue))
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

            if (myGeneration == loadGeneration) {
                awaitTrackSwap(targetTrack.id)
            }
        }
    }

    // Extract and play track
    fun loadAndPlay(youtubeUrl: String, title: String, artist: String, imageUrl: String) {
        perfClickTimestamp = System.currentTimeMillis()
        android.util.Log.d("PerfTest", "t0 — click received")

        val myGeneration = ++loadGeneration
        val videoId = youtubeUrl.substringAfter("v=").substringBefore("&")

        // Pause immediately while new track loads
        playerManager.pause()
        // Update UI immediately with track info
        _currentTrackInfo.value = TrackInfo(title, artist, imageUrl)
        // Reset the queue right away so a quick queue-open doesn't show the previous track's list
        _currentQueue.value = listOf(QueueSong(id = videoId, title = title, artist = artist, imageUrl = imageUrl))
        playerManager.resetProgress()

        viewModelScope.launch {
            _error.value = null
            val directAudioUrl = ExtractorHelper.extractAudioUrl(getApplication<Application>(), videoId)
            android.util.Log.d("PerfTest", "t1 — extraction done at +${System.currentTimeMillis() - perfClickTimestamp}ms")

            // Drop stale result from a superseded click
            if (myGeneration != loadGeneration) return@launch

            if (directAudioUrl == null) {
                _error.value = PlaybackError.ExtractionFailed
                return@launch
            }

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

            awaitTrackSwap(videoId)
        }
    }

    // Ignore taps while no data is available yet (loading OR unresolved error)
    fun togglePlayPause() {
        if (isBusy.value) return
        playerManager.playPause()
    }

    // Waits until playback actually resumes on a different track (used for skip prev/next)
    private suspend fun awaitTrackChange(myGeneration: Long, previousId: String?) {
        withTimeoutOrNull(totalWait) {
            while (true) {
                // Bail immediately if a newer skip/load has superseded this one
                if (myGeneration != loadGeneration) return@withTimeoutOrNull false

                val ready = playerManager.awaitReady()
                if (!ready) { delay(mediaItemPollInterval); continue }

                val current = playerManager.currentMediaItem.value
                val idChanged = current != null && current.mediaId != previousId
                val actuallyPlaying = playerManager.isPlaying.value

                if (idChanged && actuallyPlaying) return@withTimeoutOrNull true
                delay(mediaItemPollInterval)
            }
        }
    }

    fun skipToNext() {
        val myGeneration = ++loadGeneration
        val previousId = currentMediaItem.value?.mediaId
        _error.value = null
        playerManager.next()
        viewModelScope.launch { awaitTrackChange(myGeneration, previousId) }
    }

    fun skipToPrevious() {
        val myGeneration = ++loadGeneration
        val previousId = currentMediaItem.value?.mediaId
        _error.value = null
        playerManager.previous()
        viewModelScope.launch { awaitTrackChange(myGeneration, previousId) }
    }

    fun seekTo(positionMs: Long) {
        playerManager.seekTo(positionMs)
        seekJob?.cancel()
        seekJob = viewModelScope.launch {
            delay(120.milliseconds)

            withTimeoutOrNull(3000.milliseconds) {
                while (true) {
                    val ready = playerManager.awaitReady()
                    if (ready && playerManager.isPlaying.value) break
                    delay(smallPoll)
                }
            }
        }
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
        // Invalidate any load still in flight
        loadGeneration++
        seekJob?.cancel()
        _error.value = null
        preShuffleOrder = null
        recentTrackJob?.cancel()
        playerManager.pause()
        playerManager.resetProgress()
        _currentTrackInfo.value = null
    }

    // Cleanup on destruction
    override fun onCleared() {
        super.onCleared()
        LocalBroadcastManager.getInstance(getApplication()).unregisterReceiver(loadMoreReceiver)
        seekJob?.cancel()
        playerManager.release()
    }
}

package com.wavvy.app.core.history

// Android context and clock
import android.content.Context
import android.os.SystemClock
// Media3 item and player
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
// Coroutines
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Measures how long each song really plays, and writes it to the history as soon as it plays long enough
// The time only runs while the sound is coming out, so the extraction, a pause or a wait for the network does not count
class ListenTracker(
    private val context: Context,
    private val player: Player,
    private val scope: CoroutineScope
) : Player.Listener {
    private var item: MediaItem? = null

    // Time of the song played so far, the part before the current stretch and when the current stretch began, zero while paused
    private var closedMs = 0L
    private var playingSince = 0L
    private var counted = false
    private var countJob: Job? = null

    init {
        start(player.currentMediaItem)
    }

    // A new song begins, the one before is closed
    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
        finish()
        start(mediaItem)
    }

    override fun onIsPlayingChanged(isPlaying: Boolean) {
        if (isPlaying) {
            if (playingSince == 0L) playingSince = now()
            scheduleCount()
        } else {
            closedMs = playedMs()
            playingSince = 0L
            countJob?.cancel()
        }
    }

    // Closes the song that was playing, adding the time it played after it was counted, called when it ends and when the service closes
    fun finish() {
        countJob?.cancel()
        val finished = item
        val extraMs = playedMs() - MinimumListenMillis
        if (finished != null && counted && extraMs > 0) {
            scope.launch(Dispatchers.IO) { PlayHistory.addTime(context, finished.mediaId, extraMs) }
        }

        item = null
        closedMs = 0L
        playingSince = 0L
        counted = false
    }

    private fun start(mediaItem: MediaItem?) {
        item = mediaItem
        closedMs = 0L
        counted = false
        playingSince = if (player.isPlaying) now() else 0L
        scheduleCount()
    }

    // Waits for the rest of the minimum time and then writes the listening, nothing is scheduled while paused
    private fun scheduleCount() {
        countJob?.cancel()
        val current = item ?: return
        if (counted || playingSince == 0L) return

        val remainingMs = (MinimumListenMillis - playedMs()).coerceAtLeast(0L)
        countJob = scope.launch {
            delay(remainingMs)
            counted = true
            launch(Dispatchers.IO) {
                PlayHistory.record(context, current, MinimumListenMillis)
                YouTubeHistory.report(context, current.mediaId)
            }
        }
    }

    // Time the song has played, the stretch that is running included
    private fun playedMs(): Long = closedMs + if (playingSince != 0L) now() - playingSince else 0L

    private fun now(): Long = SystemClock.elapsedRealtime()
}

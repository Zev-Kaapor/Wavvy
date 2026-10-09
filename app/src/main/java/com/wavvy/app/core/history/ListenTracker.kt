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

// How often the place of a long listening is sent while it plays
private const val ReportIntervalMillis = 10_000L

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

    // The sending of the place of a long listening, and the last place sent, for when the next song begins
    private var reportJob: Job? = null
    private var lastItem: MediaItem? = null
    private var lastPositionMs = 0L
    private var lastLengthMs = 0L

    init {
        start(player.currentMediaItem)
    }

    // A new song begins, the one before is closed, and the place where it stopped is sent
    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
        reportLastPosition()
        reportJob?.cancel()
        finish()
        start(mediaItem)
    }

    override fun onIsPlayingChanged(isPlaying: Boolean) {
        if (isPlaying) {
            if (playingSince == 0L) playingSince = now()
            scheduleCount()
            // The place is sent as it plays again and then every few seconds
            reportPosition()
            startReporting()
        } else {
            closedMs = playedMs()
            playingSince = 0L
            countJob?.cancel()
            reportJob?.cancel()
            reportPosition()
        }
    }

    // A jump to another place of the same song sends the new place at once, without waiting for the next time
    override fun onPositionDiscontinuity(oldPosition: Player.PositionInfo, newPosition: Player.PositionInfo, reason: Int) {
        if (reason == Player.DISCONTINUITY_REASON_SEEK) reportPosition()
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

    // While a long listening plays its place is sent now and then, so what is heard counts even when the app is closed without a pause
    private fun startReporting() {
        reportJob?.cancel()
        reportJob = scope.launch {
            while (true) {
                delay(ReportIntervalMillis)
                reportPosition()
            }
        }
    }

    // Where the listening stands, so the progress of an episode follows the account, the place is kept for when the next one begins
    private fun reportPosition() {
        val current = item ?: return
        val position = player.currentPosition
        val length = current.mediaMetadata.durationMs ?: player.duration.takeIf { it > 0 } ?: 0L

        lastItem = current
        lastPositionMs = position
        lastLengthMs = length
        scope.launch(Dispatchers.IO) { YouTubeHistory.reportPosition(context, current.mediaId, position, length) }
    }

    // The place where the song before stopped, it is the last one that was sent
    private fun reportLastPosition() {
        val previous = lastItem ?: return
        val position = lastPositionMs
        val length = lastLengthMs

        scope.launch(Dispatchers.IO) { YouTubeHistory.reportPosition(context, previous.mediaId, position, length) }
        lastItem = null
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

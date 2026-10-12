package com.wavvy.app.core.playback

// Android clock and thread
import android.os.Looper
import android.os.SystemClock
// Media3 player
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.PlayerMessage
import androidx.media3.exoplayer.source.ShuffleOrder.DefaultShuffleOrder
// Coroutines
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
// Project resources
import com.wavvy.app.features.settings.data.CrossfadeSettings

// How often the volumes are set while a crossfade runs
private const val TickMillis = 40L

// How long before the song would end an automatic crossfade starts, on top of its length, so the song that goes away is quiet when it ends
private const val TriggerBufferMillis = 300L

// How long before that the link of the next song is asked for
private const val PrefetchMillis = 10_000L

// The end of a song that is too near to be worth a fade, and how long a song that goes away takes to go when a new change comes over it
private const val MinTailMillis = 1500L
private const val QuickFadeMillis = 400L

// How long the new song may take to start before the fade is given up
private const val GiveUpMillis = 15_000L

// Where the song goes on from, a place of the queue that is already there or a new queue
sealed interface CrossfadePlan {
    data class ToIndex(val index: Int, val positionMs: Long = 0L) : CrossfadePlan
    data class NewList(val items: List<MediaItem>, val startIndex: Int, val positionMs: Long) : CrossfadePlan
}

// A song that goes away, its player, the volume it had, when its fade began and how long it lasts
// One that waits for the new song holds its volume until that song starts, so the sound never dips while the new one loads
private class FadingSong(val player: ExoPlayer, val startVolume: Float, var startMs: Long, val durationMs: Long, val waitsForIncoming: Boolean) {
    // The volume follows the square of what is left of the fade, as the crossfade of Metrolist does
    fun volumeAt(now: Long): Float {
        val left = 1f - ((now - startMs).toFloat() / durationMs).coerceIn(0f, 1f)
        return startVolume * left * left
    }
}

// The song that comes in, the volume it started from, how long its fade lasts and how much of it already passed, which counts only while sound comes out
private class IncomingSong(val startVolume: Float, val durationMs: Long, val createdMs: Long) {
    var elapsedMs = 0L
    var lastMs = 0L
    var isStarted = false
}

// Blends a song into the next one as Metrolist (GPL-3.0) does, a second player gets the queue and starts the next song silent, and takes the place of the first in the session
// while the first goes on with the end of its song and fades out, the settings tell when it happens, as the crossfade of Morphe does
@OptIn(UnstableApi::class)
class CrossfadeEngine(
    initial: ExoPlayer,
    private val scope: CoroutineScope,
    private val createPlayer: () -> ExoPlayer,
    private val attributes: AudioAttributes,
    private val onSwap: (from: ExoPlayer, to: ExoPlayer) -> Unit,
    private val prefetch: (MediaItem) -> Unit
) : Player.Listener {
    // The player of the queue that is on the screen, it changes each time a crossfade ends its swap
    var player: ExoPlayer = initial
        private set

    var settings = CrossfadeSettings()
        set(value) {
            field = value
            if (!value.enabled) endAll()
            schedule()
        }

    private val looper = Looper.getMainLooper()
    private val fading = mutableListOf<FadingSong>()
    private var incoming: IncomingSong? = null
    private var prefetchMessage: PlayerMessage? = null
    private var triggerMessage: PlayerMessage? = null
    private var scheduledKey: String? = null
    private var ticker: Job? = null

    // Runs a change of the song, with a fade when the settings ask for it and a song is playing, and as it is when not
    fun change(plan: CrossfadePlan, otherwise: () -> Unit) {
        val duration = player.duration
        val isShort = duration != C.TIME_UNSET && duration - player.currentPosition < MinTailMillis

        if (!settings.enabled || !settings.onSkip || player.currentMediaItem == null || !player.isPlaying || isShort) {
            otherwise()
            return
        }
        crossfadeTo(plan, waitsForIncoming = true)
    }

    // The new player takes the queue and the song that goes away stays in the old one
    private fun crossfadeTo(plan: CrossfadePlan, waitsForIncoming: Boolean) {
        val old = player
        val now = SystemClock.uptimeMillis()
        val fadeMs = settings.durationMs.toLong()
        cancelAuto()

        // The old player lets go of the audio focus first, so the new one taking it does not pause it
        old.setAudioAttributes(attributes, false)

        val next = createPlayer()
        next.playbackParameters = old.playbackParameters
        next.repeatMode = old.repeatMode
        next.shuffleModeEnabled = old.shuffleModeEnabled
        when (plan) {
            is CrossfadePlan.ToIndex -> {
                next.setMediaItems((0 until old.mediaItemCount).map(old::getMediaItemAt), plan.index, plan.positionMs)
                keepShuffleOrder(old, next)
            }
            is CrossfadePlan.NewList -> next.setMediaItems(plan.items, plan.startIndex, plan.positionMs)
        }
        next.volume = 0f
        next.prepare()
        next.playWhenReady = true

        // What was still fading goes away quickly, and the song that was coming in goes away from the volume it reached
        val before = fading.toList()
        fading.clear()
        before.forEach { fading += FadingSong(it.player, it.volumeAt(now), now, QuickFadeMillis, waitsForIncoming = false) }
        val startVolume = old.volume

        old.removeListener(this)
        next.addListener(this)
        player = next
        onSwap(old, next)

        // The old player only has the end of its song, so it ends there instead of going on in the queue
        old.shuffleModeEnabled = false
        old.repeatMode = Player.REPEAT_MODE_OFF
        if (old.currentMediaItemIndex < old.mediaItemCount - 1) old.removeMediaItems(old.currentMediaItemIndex + 1, old.mediaItemCount)

        fading += FadingSong(old, startVolume, if (waitsForIncoming) 0L else now, fadeMs, waitsForIncoming)
        incoming = IncomingSong(startVolume = 0f, durationMs = fadeMs, createdMs = now)
        ensureTicker()
    }

    // The queue keeps the order it had when it was shuffled, the new player would shuffle it again by itself
    private fun keepShuffleOrder(from: Player, to: ExoPlayer) {
        if (!from.shuffleModeEnabled) return

        val timeline = from.currentTimeline
        val order = mutableListOf<Int>()
        var index = timeline.getFirstWindowIndex(true)
        while (index != C.INDEX_UNSET) {
            order += index
            index = timeline.getNextWindowIndex(index, Player.REPEAT_MODE_OFF, true)
        }
        if (order.size == to.mediaItemCount) to.setShuffleOrder(DefaultShuffleOrder(order.toIntArray(), System.nanoTime()))
    }

    // Sets the volumes of the songs that go away and of the one that comes in, and stops when nothing is left to fade
    private fun ensureTicker() {
        if (ticker?.isActive == true) return

        ticker = scope.launch {
            while (isActive) {
                tick()
                if (fading.isEmpty() && incoming == null) break
                delay(TickMillis)
            }
            ticker = null
        }
    }

    private fun tick() {
        val now = SystemClock.uptimeMillis()
        val coming = incoming

        // The songs that go away follow the pause of the player, and are let go when their time is over
        val iterator = fading.iterator()
        while (iterator.hasNext()) {
            val song = iterator.next()

            // A song that waits for the new one starts to go away when that one starts to play
            if (song.waitsForIncoming && song.startMs == 0L) {
                if (coming == null || coming.isStarted) song.startMs = now
            }
            val isWaiting = song.waitsForIncoming && song.startMs == 0L

            if ((!isWaiting && now - song.startMs >= song.durationMs) || song.player.playbackState == Player.STATE_ENDED) {
                release(song.player)
                iterator.remove()
            } else {
                song.player.playWhenReady = player.playWhenReady
                song.player.volume = if (isWaiting) song.startVolume else song.volumeAt(now)
            }
        }

        val song = coming ?: return
        val last = if (song.lastMs == 0L) now else song.lastMs
        song.lastMs = now
        if (player.isPlaying) {
            song.isStarted = true
            song.elapsedMs += now - last
        } else if (!song.isStarted && now - song.createdMs > GiveUpMillis) {
            endAll()
            return
        }

        val progress = (song.elapsedMs.toFloat() / song.durationMs).coerceIn(0f, 1f)
        if (progress >= 1f) {
            player.volume = 1f
            incoming = null
        } else {
            val left = 1f - progress
            player.volume = song.startVolume + (1f - song.startVolume) * (1f - left * left)
        }
    }

    // Plans the automatic crossfade of the song that plays, a little before its end so the song that goes away is quiet when it ends
    private fun schedule() {
        val item = player.currentMediaItem
        val duration = player.duration
        val fadeMs = settings.durationMs.toLong()
        val hasTarget = player.hasNextMediaItem() || player.repeatMode == Player.REPEAT_MODE_ONE

        if (!settings.enabled || !settings.onAutoAdvance || item == null || duration == C.TIME_UNSET || !hasTarget || duration < fadeMs * 2) {
            cancelAuto()
            return
        }

        val triggerAt = duration - fadeMs - TriggerBufferMillis
        if (player.currentPosition >= triggerAt) {
            cancelAuto()
            return
        }

        // The same plan for the same song is kept, so the changes of the queue do not start it again
        val key = "${item.mediaId}:$triggerAt"
        if (key == scheduledKey && triggerMessage != null) return
        cancelAuto()
        scheduledKey = key

        // The link of the next song is found ahead, so it starts at once when the crossfade begins
        val prefetchAt = triggerAt - PrefetchMillis
        val ask = { if (player.currentMediaItem?.mediaId == item.mediaId) prefetch(nextItem(item)) }
        if (player.currentPosition >= prefetchAt) ask() else prefetchMessage = messageAt(prefetchAt, ask)
        triggerMessage = messageAt(triggerAt) { autoAdvance(item.mediaId) }
    }

    // The song the queue goes to when this one ends
    private fun nextItem(current: MediaItem): MediaItem =
        if (player.repeatMode == Player.REPEAT_MODE_ONE) current else player.getMediaItemAt(player.nextMediaItemIndex)

    // A call that comes when the song of the current place of the queue reaches that time
    private fun messageAt(positionMs: Long, run: () -> Unit): PlayerMessage =
        player.createMessage { _, _ -> run() }
            .setLooper(looper)
            .setPosition(player.currentMediaItemIndex, positionMs)
            .setDeleteAfterDelivery(true)
            .also { it.send() }

    // The song is about to end, so the next one starts silent in a new player and the fade begins
    private fun autoAdvance(mediaId: String) {
        scheduledKey = null
        val hasTarget = player.hasNextMediaItem() || player.repeatMode == Player.REPEAT_MODE_ONE
        if (!settings.enabled || !settings.onAutoAdvance || !player.isPlaying || player.currentMediaItem?.mediaId != mediaId || !hasTarget) return

        // The sleep timer waits for the end of this song, so it ends as it is, without a crossfade into the next one
        if (SleepTimer.state.value.atEndOfSong) return

        val index = if (player.repeatMode == Player.REPEAT_MODE_ONE) player.currentMediaItemIndex else player.nextMediaItemIndex
        crossfadeTo(CrossfadePlan.ToIndex(index), waitsForIncoming = false)
    }

    private fun cancelAuto() {
        prefetchMessage?.cancel()
        triggerMessage?.cancel()
        prefetchMessage = null
        triggerMessage = null
        scheduledKey = null
    }

    // Everything goes back to a plain playback, the songs that go away stop and the volume is full
    private fun endAll() {
        cancelAuto()
        fading.forEach { release(it.player) }
        fading.clear()
        incoming = null
        player.volume = 1f
    }

    // Frees the engine, called when the service ends
    fun release() {
        endAll()
        ticker?.cancel()
    }

    // A player that goes away stops first, so it does not end its song loud
    private fun release(old: ExoPlayer) {
        runCatching {
            old.stop()
            old.clearMediaItems()
            old.release()
        }
    }

    // The plan follows what changes in the song and in the queue
    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) = schedule()

    override fun onPlaybackStateChanged(playbackState: Int) {
        if (playbackState == Player.STATE_IDLE) endAll()
        else if (playbackState == Player.STATE_READY) schedule()
    }

    override fun onPositionDiscontinuity(oldPosition: Player.PositionInfo, newPosition: Player.PositionInfo, reason: Int) {
        if (reason == Player.DISCONTINUITY_REASON_SEEK) schedule()
    }

    override fun onTimelineChanged(timeline: Timeline, reason: Int) = schedule()
    override fun onRepeatModeChanged(repeatMode: Int) = schedule()
    override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) = schedule()
    override fun onPlayerError(error: PlaybackException) = endAll()
}

// The player the session shows to the screens and to the notification, a change of the song goes through the crossfade first
@OptIn(UnstableApi::class)
class CrossfadePlayer(player: ExoPlayer, private val engine: CrossfadeEngine) : ForwardingPlayer(player) {
    override fun seekToNext() {
        val index = nextMediaItemIndex
        if (index == C.INDEX_UNSET) super.seekToNext() else engine.change(CrossfadePlan.ToIndex(index)) { super.seekToNext() }
    }

    override fun seekToNextMediaItem() {
        val index = nextMediaItemIndex
        if (index == C.INDEX_UNSET) super.seekToNextMediaItem() else engine.change(CrossfadePlan.ToIndex(index)) { super.seekToNextMediaItem() }
    }

    // Going back only changes the song when the song just began, otherwise it only starts again
    override fun seekToPrevious() {
        val index = previousMediaItemIndex
        if (index != C.INDEX_UNSET && currentPosition <= maxSeekToPreviousPosition) engine.change(CrossfadePlan.ToIndex(index)) { super.seekToPrevious() } else super.seekToPrevious()
    }

    override fun seekToPreviousMediaItem() {
        val index = previousMediaItemIndex
        if (index == C.INDEX_UNSET) super.seekToPreviousMediaItem() else engine.change(CrossfadePlan.ToIndex(index)) { super.seekToPreviousMediaItem() }
    }

    // A jump to another place of the queue is a change of the song, a jump inside the same song is not
    override fun seekToDefaultPosition(mediaItemIndex: Int) {
        if (mediaItemIndex == currentMediaItemIndex) super.seekToDefaultPosition(mediaItemIndex)
        else engine.change(CrossfadePlan.ToIndex(mediaItemIndex)) { super.seekToDefaultPosition(mediaItemIndex) }
    }

    override fun seekTo(mediaItemIndex: Int, positionMs: Long) {
        if (mediaItemIndex == currentMediaItemIndex) super.seekTo(mediaItemIndex, positionMs)
        else engine.change(CrossfadePlan.ToIndex(mediaItemIndex, positionMs)) { super.seekTo(mediaItemIndex, positionMs) }
    }

    // A new song or list that replaces the queue, the ones that keep the old place are left as they are
    override fun setMediaItem(mediaItem: MediaItem) =
        engine.change(CrossfadePlan.NewList(listOf(mediaItem), 0, 0L)) { super.setMediaItem(mediaItem) }

    override fun setMediaItem(mediaItem: MediaItem, startPositionMs: Long) =
        engine.change(CrossfadePlan.NewList(listOf(mediaItem), 0, startPositionMs)) { super.setMediaItem(mediaItem, startPositionMs) }

    override fun setMediaItems(mediaItems: MutableList<MediaItem>, startIndex: Int, startPositionMs: Long) {
        if (startIndex in mediaItems.indices) {
            engine.change(CrossfadePlan.NewList(mediaItems.toList(), startIndex, startPositionMs.coerceAtLeast(0L))) {
                super.setMediaItems(mediaItems, startIndex, startPositionMs)
            }
        } else {
            super.setMediaItems(mediaItems, startIndex, startPositionMs)
        }
    }
}

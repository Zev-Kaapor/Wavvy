package com.wavvy.app.core.cast

// Media3 players and items
import androidx.annotation.OptIn
import androidx.media3.cast.RemoteCastPlayer
import androidx.media3.common.C
import androidx.media3.common.ForwardingSimpleBasePlayer
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.SimpleBasePlayer
import androidx.media3.common.util.UnstableApi
// Guava futures
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture

// The player of the session while the sound comes out of a TV, it has the queue and the songs of the phone but takes the state, the place and the buttons from the TV
// The phone player under it stays paused, so nothing plays twice
@OptIn(UnstableApi::class)
class CastingPlayer(local: Player, private val cast: RemoteCastPlayer, private val bridge: CastBridge) : ForwardingSimpleBasePlayer(local) {
    // What the screens and the notification read, the TV decides whether it plays, where it is and which song of the queue it is on
    override fun getState(): State {
        val base = super.getState()
        val index = bridge.localIndexOf(cast.currentMediaItemIndex)
        val builder = base.buildUpon()

        // A song that is being sent to the TV is shown as loading
        if (index == null) {
            return if (bridge.isLoading) builder.setPlaybackState(Player.STATE_BUFFERING).build() else base
        }

        return builder
            .setCurrentMediaItemIndex(index)
            .setPlayWhenReady(cast.playWhenReady, Player.PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST)
            .setPlaybackState(cast.playbackState)
            .setIsLoading(cast.isLoading)
            .setContentPositionMs(SimpleBasePlayer.PositionSupplier { cast.currentPosition.coerceAtLeast(0L) })
            .build()
    }

    // Tells the session that something changed in the TV
    fun refresh() {
        invalidateState()
    }

    // A play after the queue ended has nothing to resume on the TV, so the song is sent again
    override fun handleSetPlayWhenReady(playWhenReady: Boolean): ListenableFuture<*> {
        val hasEnded = cast.playbackState == Player.STATE_IDLE || cast.playbackState == Player.STATE_ENDED

        if (playWhenReady && hasEnded && !bridge.isLoading) bridge.replay() else cast.playWhenReady = playWhenReady
        return Futures.immediateVoidFuture()
    }

    // A jump inside the songs the TV has goes there, a jump to another song makes the TV start from it
    override fun handleSeek(mediaItemIndex: Int, positionMs: Long, seekCommand: Int): ListenableFuture<*> {
        val position = positionMs.takeIf { it != C.TIME_UNSET } ?: 0L
        val remote = bridge.remoteIndexOf(mediaItemIndex)

        if (remote != null) cast.seekTo(remote, position) else bridge.rebuild(mediaItemIndex, position)
        return Futures.immediateVoidFuture()
    }

    override fun handleStop(): ListenableFuture<*> {
        bridge.stopRemote()
        return super.handleStop()
    }

    // A new queue is played on the TV from its first song, the phone is kept quiet
    override fun handleSetMediaItems(mediaItems: List<MediaItem>, startIndex: Int, startPositionMs: Long): ListenableFuture<*> {
        val result = super.handleSetMediaItems(mediaItems, startIndex, startPositionMs)
        bridge.onQueueReplaced(startPositionMs.takeIf { it != C.TIME_UNSET } ?: 0L)
        return result
    }

    // A song put in, moved or taken out changes what comes next on the TV
    override fun handleAddMediaItems(index: Int, mediaItems: List<MediaItem>): ListenableFuture<*> =
        super.handleAddMediaItems(index, mediaItems).also { bridge.onQueueChanged() }

    override fun handleMoveMediaItems(fromIndex: Int, toIndex: Int, newIndex: Int): ListenableFuture<*> =
        super.handleMoveMediaItems(fromIndex, toIndex, newIndex).also { bridge.onQueueChanged() }

    override fun handleRemoveMediaItems(fromIndex: Int, toIndex: Int): ListenableFuture<*> =
        super.handleRemoveMediaItems(fromIndex, toIndex).also { bridge.onQueueChanged() }
}

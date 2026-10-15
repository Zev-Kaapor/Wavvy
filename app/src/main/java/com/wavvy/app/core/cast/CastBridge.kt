package com.wavvy.app.core.cast

// Android context and addresses
import android.content.Context
import androidx.core.net.toUri
// Media3 players and items
import androidx.annotation.OptIn
import androidx.media3.cast.RemoteCastPlayer
import androidx.media3.cast.SessionAvailabilityListener
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
// Coroutines
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
// Project resources
import com.wavvy.app.R
import com.wavvy.app.core.designsystem.components.Toasts
import com.wavvy.app.core.device.isDeviceTrack
import com.wavvy.app.core.playback.StreamResolver

// How many songs the TV has ahead of the one it plays, enough that the next one starts at once and few enough that their links do not expire
private const val SongsAhead = 2

// The mime type when the link of the audio does not say it
private const val DefaultAudioType = MimeTypes.AUDIO_MP4

// Takes the sound of the phone to a TV and back, for the service of the playback
// While a TV is connected the player of the session is a CastingPlayer, the songs go to the TV a few at a time with their links, and the phone stays paused
@OptIn(UnstableApi::class)
class CastBridge(
    private val context: Context,
    private val scope: CoroutineScope,
    private val localPlayer: () -> ExoPlayer,
    // The player the session has when nothing is cast, over the player of the phone that is in use
    private val normalSessionPlayer: () -> Player,
    private val setSessionPlayer: (Player) -> Unit
) : SessionAvailabilityListener, Player.Listener {
    private val cast: RemoteCastPlayer = RemoteCastPlayer.Builder(context).build()
    private var castingPlayer: CastingPlayer? = null
    private var job: Job? = null

    // True while a TV is connected and the session has the player of the TV
    var isCasting = false
        private set

    // True while the first song is being sent to the TV
    var isLoading = false
        private set

    // True while the TV plays, which keeps the service alive when the app is closed
    val isPlaying: Boolean
        get() = isCasting && cast.playWhenReady && cast.playbackState != Player.STATE_IDLE && cast.playbackState != Player.STATE_ENDED

    init {
        cast.addListener(this)
        cast.setSessionAvailabilityListener(this)
        if (cast.isCastSessionAvailable) onCastSessionAvailable()
    }

    fun release() {
        // A TV does not go on playing for a phone that has no player anymore
        if (isCasting) stopRemote()
        cast.setSessionAvailabilityListener(null)
        cast.removeListener(this)
        cast.release()
    }

    override fun onCastSessionAvailable() {
        val local = localPlayer()
        isCasting = true
        castingPlayer = CastingPlayer(normalSessionPlayer(), cast, this).also(setSessionPlayer)

        if (local.mediaItemCount > 0) rebuild(local.currentMediaItemIndex, local.currentPosition)
    }

    // The TV went away, the phone takes the place where the TV was and waits paused
    override fun onCastSessionUnavailable() {
        if (!isCasting) return

        val local = localPlayer()
        val index = localIndexOf(cast.currentMediaItemIndex)
        val position = cast.currentPosition.coerceAtLeast(0L)

        job?.cancel()
        isCasting = false
        isLoading = false
        castingPlayer = null
        setSessionPlayer(normalSessionPlayer())

        if (index != null && index in 0 until local.mediaItemCount) local.seekTo(index, position)
        local.pause()
        cast.clearMediaItems()
    }

    // Starts the TV from a song of the queue of the phone, with the next ones after it
    fun rebuild(index: Int, positionMs: Long) {
        val local = localPlayer()
        if (index !in 0 until local.mediaItemCount) return

        // What cannot be cast keeps playing on the phone
        if (isDeviceTrack(local.getMediaItemAt(index).mediaId)) {
            abandon(R.string.cast_unsupported)
            return
        }

        local.pause()
        isLoading = true
        castingPlayer?.refresh()

        job?.cancel()
        job = scope.launch {
            val first = remoteItem(index)
            if (first == null) {
                isLoading = false
                abandon(R.string.cast_failed)
                return@launch
            }

            cast.setMediaItems(listOf(first), 0, positionMs)
            cast.prepare()
            cast.play()
            isLoading = false
            castingPlayer?.refresh()
            extend()
        }
    }

    // A new queue played on the phone, the TV starts it
    fun onQueueReplaced(positionMs: Long) {
        rebuild(localPlayer().currentMediaItemIndex, positionMs)
    }

    // The queue of the phone changed, the songs the TV has after the one it plays are asked again
    fun onQueueChanged() {
        if (!isCasting || cast.mediaItemCount == 0) return

        val current = cast.currentMediaItemIndex
        if (current < cast.mediaItemCount - 1) cast.removeMediaItems(current + 1, cast.mediaItemCount)
        job = scope.launch { extend() }
    }

    // Ends what the TV plays
    fun stopRemote() {
        job?.cancel()
        cast.stop()
        cast.clearMediaItems()
    }

    // The place of a song of the TV in the queue of the phone, none when the TV has nothing
    fun localIndexOf(remoteIndex: Int): Int? {
        if (!isCasting || remoteIndex !in 0 until cast.mediaItemCount) return null

        val id = cast.getMediaItemAt(remoteIndex).mediaId
        val local = localPlayer()
        return (0 until local.mediaItemCount).firstOrNull { local.getMediaItemAt(it).mediaId == id }
    }

    // The place of a song of the phone among the ones the TV has, none when the TV does not have it
    fun remoteIndexOf(localIndex: Int): Int? {
        val local = localPlayer()
        if (localIndex !in 0 until local.mediaItemCount) return null

        val id = local.getMediaItemAt(localIndex).mediaId
        return (0 until cast.mediaItemCount).firstOrNull { cast.getMediaItemAt(it).mediaId == id }
    }

    // The songs that follow the one the TV plays go to it until it has a few
    private suspend fun extend() {
        while (isCasting && cast.mediaItemCount > 0 && cast.mediaItemCount - cast.currentMediaItemIndex - 1 < SongsAhead) {
            val last = localIndexOf(cast.mediaItemCount - 1) ?: break
            val next = nextLocalIndex(last) ?: break
            val item = remoteItem(next) ?: break

            if (!isCasting) break
            cast.addMediaItem(item)
        }
    }

    // The song that follows another in the order the phone plays them
    private fun nextLocalIndex(from: Int): Int? {
        val local = localPlayer()
        val timeline = local.currentTimeline
        if (timeline.isEmpty) return null

        return timeline.getNextWindowIndex(from, Player.REPEAT_MODE_OFF, local.shuffleModeEnabled).takeIf { it != C.INDEX_UNSET }
    }

    // A song of the queue of the phone as the TV takes it, with the link of its audio, none when the link cannot be found
    private suspend fun remoteItem(index: Int): MediaItem? {
        val local = localPlayer()
        if (index !in 0 until local.mediaItemCount) return null
        val item = local.getMediaItemAt(index)
        if (isDeviceTrack(item.mediaId)) return null

        val stream = withContext(Dispatchers.IO) { StreamResolver.resolve(item.mediaId) }.getOrNull() ?: return null
        val type = stream.url.toUri().getQueryParameter("mime") ?: DefaultAudioType

        return item.buildUpon()
            .setUri(stream.url)
            .setMimeType(type)
            .setMediaMetadata(item.mediaMetadata.buildUpon().setMediaType(MediaMetadata.MEDIA_TYPE_MUSIC).build())
            .build()
    }

    // The TV cannot play it, the phone goes back to being the player
    private fun abandon(messageRes: Int) {
        Toasts.show(context.getString(messageRes), isError = true)
        if (!isCasting) return

        isCasting = false
        isLoading = false
        castingPlayer = null
        job?.cancel()
        cast.clearMediaItems()
        setSessionPlayer(normalSessionPlayer())
    }

    override fun onEvents(player: Player, events: Player.Events) {
        castingPlayer?.refresh()
    }

    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
        if (!isCasting) return
        job = scope.launch { extend() }
    }

    override fun onPlayerError(error: PlaybackException) {
        Toasts.show(context.getString(R.string.cast_failed), subtitle = error.errorCodeName, isError = true)
    }
}

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
// Google Cast session and its messages
import com.google.android.gms.cast.Cast
import com.google.android.gms.cast.framework.CastContext
import com.google.android.gms.cast.framework.CastSession
import org.json.JSONObject
import java.util.Locale
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

// A previous after this long starts the same song again instead of going back
private const val RestartPreviousMillis = 3000L

// The mime type when the link of the audio does not say it
private const val DefaultAudioType = MimeTypes.AUDIO_MP4

// The channel the screen of the TV uses to ask the phone for the buttons it cannot answer, and the words of that talk
private const val ControlNamespace = "urn:x-cast:com.wavvy.app.control"
private const val ActionKey = "action"
private const val ActionNext = "next"
private const val ActionPrevious = "previous"
private const val ActionRepeat = "repeat"
private const val ActionShuffle = "shuffle"
private const val ActionReplay = "replay"
private const val ActionModes = "modes"
private const val RepeatKey = "repeat"
private const val ShuffleKey = "shuffle"
private const val LanguageKey = "language"
private const val RepeatOff = "off"
private const val RepeatAll = "all"
private const val RepeatOne = "one"

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
    private var extendJob: Job? = null
    private var session: CastSession? = null

    // The screen of the TV asks for a button, the phone does it and tells the TV how the repeat and the shuffle are
    private val controlCallback = Cast.MessageReceivedCallback { _, _, message -> onControl(message) }

    // A change of the repeat or the shuffle, made on the phone or on the TV, reaches the other and the songs the TV has next
    private val modesListener = object : Player.Listener {
        override fun onRepeatModeChanged(repeatMode: Int) = modesChanged()
        override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) = modesChanged()
    }

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
        connectChannel()

        if (local.mediaItemCount > 0) rebuild(local.currentMediaItemIndex, local.currentPosition)
    }

    // The TV went away, the phone takes the place where the TV was and waits paused
    override fun onCastSessionUnavailable() {
        if (!isCasting) return

        val local = localPlayer()
        val index = localIndexOf(cast.currentMediaItemIndex)
        val position = cast.currentPosition.coerceAtLeast(0L)

        cancelJobs()
        disconnectChannel()
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

        cancelJobs()
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
            syncModes()
            extend()
        }
    }

    // Plays the song of the phone again from its start, for a queue that ended
    fun replay() {
        rebuild(localPlayer().currentMediaItemIndex, 0L)
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
        extendLater()
    }

    // Ends what the TV plays
    fun stopRemote() {
        cancelJobs()
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

    // Asks for the songs that follow, the ask that was running stops so two never add the same songs
    private fun extendLater() {
        extendJob?.cancel()
        extendJob = scope.launch { extend() }
    }

    // Stops the load of the first song and the ask for the next ones
    private fun cancelJobs() {
        job?.cancel()
        extendJob?.cancel()
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

    // The song that follows another in the order the phone plays them, the queue starts again when the repeat is on
    private fun nextLocalIndex(from: Int): Int? {
        val local = localPlayer()
        val timeline = local.currentTimeline
        if (timeline.isEmpty) return null

        val repeat = if (local.repeatMode == Player.REPEAT_MODE_ALL) Player.REPEAT_MODE_ALL else Player.REPEAT_MODE_OFF
        return timeline.getNextWindowIndex(from, repeat, local.shuffleModeEnabled).takeIf { it != C.INDEX_UNSET }
    }

    // The channel with the screen of the TV is open while the session lasts
    private fun connectChannel() {
        runCatching {
            session = CastContext.getSharedInstance(context).sessionManager.currentCastSession
            session?.setMessageReceivedCallbacks(ControlNamespace, controlCallback)
        }
        localPlayer().addListener(modesListener)
    }

    private fun disconnectChannel() {
        runCatching { session?.removeMessageReceivedCallbacks(ControlNamespace) }
        localPlayer().removeListener(modesListener)
        session = null
    }

    // The TV repeats one song by itself, the other repeats are done by the songs the phone sends
    private fun syncModes() {
        cast.repeatMode = if (localPlayer().repeatMode == Player.REPEAT_MODE_ONE) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
        sendModes()
    }

    private fun modesChanged() {
        if (!isCasting) return

        syncModes()
        onQueueChanged()
    }

    // Tells the screen of the TV how the repeat and the shuffle are
    private fun sendModes() {
        val local = localPlayer()
        val repeat = when (local.repeatMode) {
            Player.REPEAT_MODE_ALL -> RepeatAll
            Player.REPEAT_MODE_ONE -> RepeatOne
            else -> RepeatOff
        }

        val message = JSONObject()
            .put(RepeatKey, repeat)
            .put(ShuffleKey, local.shuffleModeEnabled)
            .put(LanguageKey, Locale.getDefault().language)
            .toString()
        runCatching { session?.sendMessage(ControlNamespace, message) }
    }

    // The next or the previous song in the order the phone plays them, a previous after a few seconds starts the same song again
    private fun skip(forward: Boolean) {
        val local = localPlayer()
        val timeline = local.currentTimeline
        if (timeline.isEmpty) return

        val current = localIndexOf(cast.currentMediaItemIndex) ?: local.currentMediaItemIndex
        val repeat = if (local.repeatMode == Player.REPEAT_MODE_ALL) Player.REPEAT_MODE_ALL else Player.REPEAT_MODE_OFF
        val target = when {
            forward -> timeline.getNextWindowIndex(current, repeat, local.shuffleModeEnabled)
            cast.currentPosition > RestartPreviousMillis -> current
            else -> timeline.getPreviousWindowIndex(current, repeat, local.shuffleModeEnabled).takeIf { it != C.INDEX_UNSET } ?: current
        }

        if (target != C.INDEX_UNSET) jumpTo(target)
    }

    // Plays a song of the queue of the phone from its start, the TV already has it or is asked for it
    fun jumpTo(index: Int) {
        val remote = remoteIndexOf(index)
        if (remote != null) cast.seekTo(remote, 0L) else rebuild(index, 0L)
    }

    // A button of the TV, done on the player of the session so the phone shows it too
    private fun onControl(message: String) {
        val player = castingPlayer ?: return
        val action = runCatching { JSONObject(message).getString(ActionKey) }.getOrNull() ?: return

        when (action) {
            ActionNext -> skip(forward = true)
            ActionPrevious -> skip(forward = false)
            ActionRepeat -> player.repeatMode = when (player.repeatMode) {
                Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
                Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
                else -> Player.REPEAT_MODE_OFF
            }
            ActionShuffle -> player.shuffleModeEnabled = !player.shuffleModeEnabled
            ActionReplay -> replay()
            ActionModes -> sendModes()
        }
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

        disconnectChannel()
        isCasting = false
        isLoading = false
        castingPlayer = null
        cancelJobs()
        cast.clearMediaItems()
        setSessionPlayer(normalSessionPlayer())
    }

    override fun onEvents(player: Player, events: Player.Events) {
        castingPlayer?.refresh()
    }

    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
        if (!isCasting) return
        extendLater()
    }

    override fun onPlayerError(error: PlaybackException) {
        Toasts.show(context.getString(R.string.cast_failed), subtitle = error.errorCodeName, isError = true)
    }
}

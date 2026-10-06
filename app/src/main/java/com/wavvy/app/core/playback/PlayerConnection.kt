package com.wavvy.app.core.playback

// Android context and components
import android.content.ComponentName
import android.content.Context
// Android utilities
import androidx.annotation.OptIn
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
// Media3 items, player and controller
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
// Concurrency and reactive flows
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// A song or an episode the player can open, the id is its video
data class PlayableTrack(
    val id: String,
    val title: String,
    val artist: String?,
    val artworkUrl: String?,
    val durationMs: Long = 0L,
    val isVideo: Boolean = false
)

// A song of the queue and its place in the player
data class QueueEntry(
    val track: PlayableTrack,
    val index: Int
)

// Repeat modes, in the order a tap goes through them
enum class RepeatMode { Off, All, One }

// The radio that feeds the queue, with the page to ask next
private data class RadioState(
    val appContext: Context,
    val videoId: String,
    val playlistId: String,
    val continuation: String?
)

// Songs left after the current one when the next page of the radio is asked
private const val LoadMoreMargin = 5

// How often the radio checks that the song started, and how long it waits at most before it goes on anyway
private const val RadioPollMillis = 100L
private const val RadioMaxWaitMillis = 15_000L

// The way the screens talk to the playback service, and what it is playing for them to show
object PlayerConnection {
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var controller: MediaController? = null

    private val mutableTrack = MutableStateFlow<PlayableTrack?>(null)
    private val mutableIsPlaying = MutableStateFlow(false)
    private val mutableIsLoading = MutableStateFlow(false)
    private val mutableRepeat = MutableStateFlow(RepeatMode.Off)
    private val mutableShuffle = MutableStateFlow(false)
    private val mutableQueue = MutableStateFlow<List<QueueEntry>>(emptyList())
    private val mutableCurrentIndex = MutableStateFlow(0)
    private val mutableIsLoadingMore = MutableStateFlow(false)

    private val scope = MainScope()
    private var radio: RadioState? = null
    private var radioJob: Job? = null

    // What is loaded in the player, empty when nothing is
    val track: StateFlow<PlayableTrack?> = mutableTrack.asStateFlow()

    // True while the sound is coming out
    val isPlaying: StateFlow<Boolean> = mutableIsPlaying.asStateFlow()

    // True while the player waits for the audio it was asked to play
    val isLoading: StateFlow<Boolean> = mutableIsLoading.asStateFlow()

    // Repeat mode of the player
    val repeat: StateFlow<RepeatMode> = mutableRepeat.asStateFlow()

    // True when the order of the queue is shuffled
    val shuffle: StateFlow<Boolean> = mutableShuffle.asStateFlow()

    // Songs of the queue in the order they play
    val queue: StateFlow<List<QueueEntry>> = mutableQueue.asStateFlow()

    // Place in the player of the song that is loaded
    val currentIndex: StateFlow<Int> = mutableCurrentIndex.asStateFlow()

    // True while the next page of the radio is on its way
    val isLoadingMore: StateFlow<Boolean> = mutableIsLoadingMore.asStateFlow()

    // Length of the song in milliseconds, zero while it is not known
    val durationMs: Long
        get() = controller?.duration?.takeIf { it != C.TIME_UNSET && it > 0 } ?: 0L

    // Share of the song already played, read when it is drawn instead of being pushed to the screens
    val progress: Float
        get() {
            val player = controller ?: return 0f
            val duration = player.duration.takeIf { it != C.TIME_UNSET && it > 0 } ?: return 0f
            return (player.currentPosition.toFloat() / duration).coerceIn(0f, 1f)
        }

    // Connects to the service when the app opens, so a song that kept playing shows up again
    fun connect(context: Context) {
        withController(context) {}
    }

    // Replaces what is playing with this track and starts it, then fills the queue with its radio
    fun play(context: Context, track: PlayableTrack) {
        val appContext = context.applicationContext
        withController(context) { player ->
            player.setMediaItem(track.toMediaItem())
            player.prepare()
            player.play()

            radio = null
            radioJob?.cancel()
            radioJob = scope.launch {
                // The radio waits for the first sound, so its requests and the change of the queue never compete with the opening of the song
                var waited = 0L
                while (!player.isPlaying && waited < RadioMaxWaitMillis) {
                    delay(RadioPollMillis)
                    waited += RadioPollMillis
                }

                RadioQueue.start(appContext, track.id).onSuccess { page ->
                    // Another song may have been chosen while the radio was on its way
                    if (controller?.currentMediaItem?.mediaId != track.id) return@onSuccess
                    player.addMediaItems(page.tracks.filter { it.id != track.id }.distinctBy { it.id }.map { it.toMediaItem() })
                    radio = RadioState(appContext, track.id, page.playlistId, page.continuation)
                }
            }
        }
    }

    // Asks the next page of the radio and adds the songs that are not in the queue yet
    fun loadMore() {
        val state = radio ?: return
        val continuation = state.continuation ?: return
        if (mutableIsLoadingMore.value) return

        mutableIsLoadingMore.value = true
        scope.launch {
            RadioQueue.more(state.appContext, state.videoId, state.playlistId, continuation).onSuccess { page ->
                val player = controller
                if (player == null || radio !== state) return@onSuccess
                val known = (0 until player.mediaItemCount).mapTo(HashSet()) { player.getMediaItemAt(it).mediaId }
                player.addMediaItems(page.tracks.filter { known.add(it.id) }.map { it.toMediaItem() })
                radio = state.copy(continuation = page.continuation)
            }
            mutableIsLoadingMore.value = false
        }
    }

    // Plays the song at this place of the player
    fun playAt(index: Int) {
        val player = controller?.takeIf { index in 0 until it.mediaItemCount } ?: return
        player.seekTo(index, 0L)
        player.play()
    }

    // Takes the song at this place out of the queue
    fun remove(index: Int) {
        controller?.takeIf { index in 0 until it.mediaItemCount }?.removeMediaItem(index)
    }

    // Moves the song at this place to play right after the current one
    fun playNext(index: Int) {
        val player = controller?.takeIf { index in 0 until it.mediaItemCount } ?: return
        val current = player.currentMediaItemIndex
        if (index == current) return
        player.moveMediaItem(index, if (index < current) current else current + 1)
    }

    // Takes several songs out of the queue, the one that plays stays, they are found by their ids so no place can be out of date
    fun removeAll(ids: Set<String>) {
        val player = controller ?: return
        val current = player.currentMediaItemIndex
        // From the end, so the places of the ones still to remove do not move
        for (index in player.mediaItemCount - 1 downTo 0) {
            if (index != current && player.getMediaItemAt(index).mediaId in ids) player.removeMediaItem(index)
        }
    }

    // Moves several songs to play right after the one that plays, one after the other in the order they have in the queue
    fun playNextAll(ids: Set<String>) {
        val player = controller ?: return
        val ordered = (0 until player.mediaItemCount).map { player.getMediaItemAt(it).mediaId }.filter { it in ids }
        var moved = 0

        for (id in ordered) {
            val from = (0 until player.mediaItemCount).firstOrNull { player.getMediaItemAt(it).mediaId == id } ?: continue
            val current = player.currentMediaItemIndex
            if (from == current) continue

            // A song from before the one that plays leaves a gap behind it, so it lands one place earlier
            player.moveMediaItem(from, if (from < current) current + moved else current + 1 + moved)
            moved++
        }
    }

    // Moves a song from one place of the queue to another, places that do not exist are ignored
    fun move(from: Int, to: Int) {
        val player = controller ?: return
        val count = player.mediaItemCount
        if (from != to && from in 0 until count && to in 0 until count) player.moveMediaItem(from, to)
    }

    // Pauses what plays and plays what is paused
    fun togglePlayPause() {
        val player = controller ?: return
        if (player.isPlaying) player.pause() else player.play()
    }

    // Jumps to a share of the song
    fun seekTo(fraction: Float) {
        val duration = durationMs.takeIf { it > 0 } ?: return
        controller?.seekTo((duration * fraction.coerceIn(0f, 1f)).toLong())
    }

    // Goes from no repeat to repeat all, then to repeat one, then back
    fun toggleRepeat() {
        val player = controller ?: return
        player.repeatMode = when (player.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
    }

    // Turns the shuffled order on or off
    fun toggleShuffle() {
        val player = controller ?: return
        player.shuffleModeEnabled = !player.shuffleModeEnabled
    }

    // Goes to the next item when there is one
    fun skipToNext() {
        controller?.takeIf { it.hasNextMediaItem() }?.seekToNextMediaItem()
    }

    // Goes to the previous item, or back to the start when there is none
    fun skipToPrevious() {
        val player = controller ?: return
        if (player.hasPreviousMediaItem()) player.seekToPreviousMediaItem() else player.seekTo(0L)
    }

    // Stops and empties the player, which closes the notification
    fun stop() {
        radio = null
        radioJob?.cancel()
        controller?.run {
            stop()
            clearMediaItems()
        }
    }

    // Runs an action once the controller is connected to the service
    private fun withController(context: Context, action: (MediaController) -> Unit) {
        val appContext = context.applicationContext
        val future = controllerFuture ?: MediaController.Builder(
            appContext,
            SessionToken(appContext, ComponentName(appContext, PlaybackService::class.java))
        ).buildAsync().also { controllerFuture = it }

        future.addListener(
            {
                // A connection that failed is forgotten, so the next call tries again
                runCatching { future.get() }
                    .onSuccess { player ->
                        if (controller !== player) attach(player)
                        action(player)
                    }
                    .onFailure { controllerFuture = null }
            },
            ContextCompat.getMainExecutor(appContext)
        )
    }

    // Follows the player of the service
    private fun attach(player: MediaController) {
        controller = player
        player.addListener(object : Player.Listener {
            override fun onEvents(player: Player, events: Player.Events) {
                sync(player)
                if (events.containsAny(Player.EVENT_TIMELINE_CHANGED, Player.EVENT_SHUFFLE_MODE_ENABLED_CHANGED)) syncQueue(player)

                // Near the end of the queue the radio brings more songs
                if (events.contains(Player.EVENT_MEDIA_ITEM_TRANSITION) &&
                    player.mediaItemCount - player.currentMediaItemIndex <= LoadMoreMargin
                ) {
                    loadMore()
                }
            }
        })
        sync(player)
        syncQueue(player)
    }

    // Copies the songs of the player to the queue, in the order they play
    private fun syncQueue(player: Player) {
        val timeline = player.currentTimeline
        val shuffled = player.shuffleModeEnabled
        val entries = mutableListOf<QueueEntry>()
        var index = if (timeline.isEmpty) C.INDEX_UNSET else timeline.getFirstWindowIndex(shuffled)
        while (index != C.INDEX_UNSET) {
            entries += QueueEntry(player.getMediaItemAt(index).toPlayableTrack(), index)
            index = timeline.getNextWindowIndex(index, Player.REPEAT_MODE_OFF, shuffled)
        }
        mutableQueue.value = entries
    }

    // Copies the state of the player to the flows the screens read
    private fun sync(player: Player) {
        mutableTrack.value = player.currentMediaItem?.toPlayableTrack()
        mutableIsPlaying.value = player.isPlaying
        mutableIsLoading.value = player.playWhenReady && player.playbackState == Player.STATE_BUFFERING
        mutableRepeat.value = when (player.repeatMode) {
            Player.REPEAT_MODE_ALL -> RepeatMode.All
            Player.REPEAT_MODE_ONE -> RepeatMode.One
            else -> RepeatMode.Off
        }
        mutableShuffle.value = player.shuffleModeEnabled
        mutableCurrentIndex.value = player.currentMediaItemIndex
    }

    // The id is also the key the service asks the link with, the details show on the notification
    @OptIn(UnstableApi::class)
    private fun PlayableTrack.toMediaItem(): MediaItem =
        MediaItem.Builder()
            .setMediaId(id)
            .setUri(id)
            .setCustomCacheKey(id)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(title)
                    .setDisplayTitle(title)
                    .setArtist(artist)
                    .setSubtitle(artist)
                    .setArtworkUri(artworkUrl?.toUri())
                    .setDurationMs(durationMs.takeIf { it > 0 })
                    .setMediaType(if (isVideo) MediaMetadata.MEDIA_TYPE_VIDEO else MediaMetadata.MEDIA_TYPE_MUSIC)
                    .setIsBrowsable(false)
                    .setIsPlayable(true)
                    .build()
            )
            .build()

    // A loaded item back as a track
    private fun MediaItem.toPlayableTrack(): PlayableTrack =
        PlayableTrack(
            id = mediaId,
            title = mediaMetadata.title?.toString().orEmpty(),
            artist = mediaMetadata.artist?.toString(),
            artworkUrl = mediaMetadata.artworkUri?.toString(),
            durationMs = mediaMetadata.durationMs ?: 0L,
            isVideo = mediaMetadata.mediaType == MediaMetadata.MEDIA_TYPE_VIDEO
        )
}

package com.wavvy.app.core.playback

// Android context and links
import android.net.Uri
import androidx.annotation.StringRes
// Media3 player
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
// Network and files
import java.io.IOException
import java.security.MessageDigest
import okhttp3.Request
// JSON parsing
import org.json.JSONArray
// Coroutines
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
// Project resources
import com.wavvy.app.R
import com.wavvy.app.core.network.WavvyProxy
import com.wavvy.app.features.settings.data.SponsorBlockSettings

// The kinds of part the community marks, the name the database knows each by, the words that name it for the user and whether it is on until the user chooses
enum class SponsorCategory(val apiName: String, @StringRes val titleRes: Int, val isDefault: Boolean) {
    NonMusic("music_offtopic", R.string.settings_sponsorblock_non_music, true),
    Sponsor("sponsor", R.string.settings_sponsorblock_sponsor, true),
    SelfPromo("selfpromo", R.string.settings_sponsorblock_self_promo, false),
    Interaction("interaction", R.string.settings_sponsorblock_interaction, false),
    Intro("intro", R.string.settings_sponsorblock_intro, false),
    Outro("outro", R.string.settings_sponsorblock_outro, false),
    Preview("preview", R.string.settings_sponsorblock_preview, false)
}

// The database of the marked parts and the first letters of the code of a video that are sent to it, so it never learns which video it is
private const val SegmentsAddress = "https://sponsor.ajay.app/api/skipSegments/"
private const val PrefixLength = 4
private const val SkipAction = "skip"
private const val NotFound = 404
private const val MillisPerSecond = 1000

// How often the place of the song is checked against the marked parts, how many videos are kept in memory, and how near to the end a part is to count as the end
private const val WatchMillis = 100L
private const val MaxCached = 64
private const val EndMarginMillis = 500L

// A marked part that the seekbar draws, and the video it belongs to
data class SponsorMark(val category: SponsorCategory, val startMs: Long, val endMs: Long)
data class SponsorMarkState(val videoId: String? = null, val marks: List<SponsorMark> = emptyList())

// The marked parts of the song that plays, of the kinds the user chose, for the screens that draw them
object SponsorMarks {
    private val mutableState = MutableStateFlow(SponsorMarkState())
    val state: StateFlow<SponsorMarkState> = mutableState.asStateFlow()

    internal fun publish(videoId: String?, marks: List<SponsorMark>) {
        mutableState.value = SponsorMarkState(videoId, marks)
    }
}

// One marked part of a video, in milliseconds
private class Segment(val uuid: String, val category: SponsorCategory, val startMs: Long, val endMs: Long)

// Skips the parts of a song, an episode or a video that the community of SponsorBlock marked, the ones of the kinds the user chose, each only once
class SponsorBlock(private val scope: CoroutineScope) : Player.Listener {
    var settings = SponsorBlockSettings()
        set(value) {
            field = value
            restart()
        }

    private var player: ExoPlayer? = null
    private var segments: List<Segment> = emptyList()
        set(value) {
            field = value
            publish()
        }
    private val skipped = mutableSetOf<String>()
    private var loadJob: Job? = null
    private var watchJob: Job? = null

    // The marks already asked for, the most recent ones, so a song heard again does not ask again
    private val cache = object : LinkedHashMap<String, List<Segment>>(MaxCached, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, List<Segment>>?) = size > MaxCached
    }

    // Follows the player of the queue, which changes when a crossfade swaps it
    fun attach(next: ExoPlayer) {
        player?.removeListener(this)
        player = next
        next.addListener(this)
        skipped.clear()
        restart()
    }

    fun release() {
        watchJob?.cancel()
        loadJob?.cancel()
        player?.removeListener(this)
        player = null
    }

    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
        skipped.clear()
        load(mediaItem?.mediaId)
    }

    // Tells the screens which parts of the song that plays are marked and are of a kind that the user chose
    private fun publish() {
        val id = player?.currentMediaItem?.mediaId
        val marks = if (settings.enabled && id != null) {
            segments.filter { it.category in settings.categories }.map { SponsorMark(it.category, it.startMs, it.endMs) }
        } else {
            emptyList()
        }
        SponsorMarks.publish(id, marks)
    }

    // Starts or stops the watching as the settings say, a song that is playing gets its marks at once
    private fun restart() {
        watchJob?.cancel()
        if (!settings.enabled) {
            loadJob?.cancel()
            segments = emptyList()
            return
        }

        load(player?.currentMediaItem?.mediaId)
        watchJob = scope.launch {
            while (isActive) {
                check()
                delay(WatchMillis)
            }
        }
    }

    // Asks for the marks of a video, or takes them from the memory
    private fun load(videoId: String?) {
        loadJob?.cancel()
        segments = emptyList()
        if (!settings.enabled || videoId == null) return

        cache[videoId]?.let {
            segments = it
            return
        }
        loadJob = scope.launch {
            val found = fetch(videoId) ?: return@launch
            cache[videoId] = found
            if (player?.currentMediaItem?.mediaId == videoId) segments = found
        }
    }

    // Jumps over the part that the song is in, when it is one of the kinds the user chose and it was not skipped yet
    private fun check() {
        val current = player?.takeIf { it.isPlaying } ?: return
        val position = current.currentPosition
        val part = segments.firstOrNull {
            it.category in settings.categories && it.uuid !in skipped && position >= it.startMs && position < it.endMs
        } ?: return

        skipped += part.uuid
        val duration = current.duration
        val isAtEnd = duration != C.TIME_UNSET && part.endMs >= duration - EndMarginMillis
        if (isAtEnd) {
            if (current.hasNextMediaItem()) current.seekToNextMediaItem() else current.seekTo(duration)
        } else {
            current.seekTo(part.endMs)
        }

    }

    // The marked parts of a video, none when the community has not marked any, and null when the answer did not come
    private suspend fun fetch(videoId: String): List<Segment>? = withContext(Dispatchers.IO) {
        runCatching {
            val prefix = MessageDigest.getInstance("SHA-256").digest(videoId.toByteArray()).joinToString("") { "%02x".format(it) }.take(PrefixLength)
            val categories = SponsorCategory.entries.joinToString(prefix = "[", postfix = "]", separator = ",") { "\"${it.apiName}\"" }
            val actions = "[\"$SkipAction\"]"
            val request = Request.Builder()
                .url("$SegmentsAddress$prefix?categories=${Uri.encode(categories)}&actionTypes=${Uri.encode(actions)}")
                .build()

            WavvyProxy.client.newCall(request).execute().use { response ->
                when {
                    response.code == NotFound -> emptyList()
                    !response.isSuccessful -> throw IOException("SponsorBlock answered ${response.code}")
                    else -> parse(JSONArray(response.body.string()), videoId)
                }
            }
        }.getOrNull()
    }

    // The answer has the videos that share the first letters, the ones of this video are the ones that count
    private fun parse(answer: JSONArray, videoId: String): List<Segment> {
        val parts = mutableListOf<Segment>()
        for (index in 0 until answer.length()) {
            val video = answer.optJSONObject(index) ?: continue
            if (video.optString("videoID") != videoId) continue

            val list = video.optJSONArray("segments") ?: continue
            for (position in 0 until list.length()) {
                val item = list.optJSONObject(position) ?: continue
                val category = SponsorCategory.entries.firstOrNull { it.apiName == item.optString("category") } ?: continue
                val range = item.optJSONArray("segment") ?: continue
                if (item.optString("actionType", SkipAction) != SkipAction || range.length() < 2) continue

                parts += Segment(item.optString("UUID"), category, (range.optDouble(0) * MillisPerSecond).toLong(), (range.optDouble(1) * MillisPerSecond).toLong())
            }
        }
        return parts
    }
}

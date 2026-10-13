package com.wavvy.app.core.playback

// Android context
import android.content.Context
// Media3 audio and items
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.util.UnstableApi
// Files, time and JSON
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.pow
import org.json.JSONArray
import org.json.JSONObject
// Reactive flows
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
// Project resources
import com.wavvy.app.features.settings.data.SleepSettings

// How far the volume of a song can go down and up, in millibels
private const val MinGainMb = -1500
private const val MaxGainMb = 300

// YouTube measures the loudness of a song against this level, so the measure is brought back to LUFS
private const val ReferenceLufs = -7f
private const val MillibelsPerDecibel = 100.0

// Millibels in a step of two, the way a gain in millibels becomes a multiplier of the sound
private const val GainDivisor = 2000.0

// How many songs keep their loudness at most
private const val MaxLoudnessEntries = 500

// How loud each song is, told by YouTube when the link of the song is found, kept as the gain that brings it to the same level
object Loudness {
    private val measures = ConcurrentHashMap<String, Float>()
    private val mutableUpdates = MutableSharedFlow<String>(extraBufferCapacity = 16)

    // The id of a song whose loudness became known
    val updates: SharedFlow<String> = mutableUpdates.asSharedFlow()

    // Keeps the loudness of a song in LUFS, the perceptual one is used when YouTube gives it, adapted from Metrolist (GPL-3.0)
    fun report(mediaId: String, loudnessDb: Double?, perceptualLoudnessDb: Double?) {
        val measured = perceptualLoudnessDb?.toFloat() ?: loudnessDb?.let { (it + ReferenceLufs).toFloat() } ?: return
        if (measures.size >= MaxLoudnessEntries) measures.clear()
        measures[mediaId] = measured
        mutableUpdates.tryEmit(mediaId)
    }

    // The gain in millibels that brings a song to the level asked, none while its loudness is not known
    fun gainMb(mediaId: String?, targetLufs: Float): Int? =
        mediaId?.let(measures::get)?.let { (-(it - targetLufs) * MillibelsPerDecibel).toInt().coerceIn(MinGainMb, MaxGainMb) }
}

// Makes the sound louder or softer by a fixed gain, which brings every song to the same loudness, adapted from Metrolist (GPL-3.0)
@UnstableApi
class VolumeNormalizer : BaseAudioProcessor() {
    private var encoding = C.ENCODING_INVALID
    private var bytesPerSample = 0

    @Volatile
    var enabled = false

    @Volatile
    private var linearGain = 1.0

    fun setTargetGain(gainMb: Int) {
        linearGain = 10.0.pow(gainMb / GainDivisor)
    }

    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        encoding = inputAudioFormat.encoding
        bytesPerSample = when (encoding) {
            C.ENCODING_PCM_16BIT -> 2
            C.ENCODING_PCM_FLOAT -> 4
            else -> throw AudioProcessor.UnhandledAudioFormatException(inputAudioFormat)
        }
        return inputAudioFormat
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        if (!inputBuffer.hasRemaining()) return

        val gain = linearGain
        val isApplied = enabled && gain != 1.0
        val count = inputBuffer.remaining() / bytesPerSample
        val output = replaceOutputBuffer(count * bytesPerSample)
        inputBuffer.order(ByteOrder.LITTLE_ENDIAN)
        output.order(ByteOrder.LITTLE_ENDIAN)

        repeat(count) {
            if (encoding == C.ENCODING_PCM_16BIT) {
                val sample = inputBuffer.getShort()
                output.putShort(if (isApplied) (sample * gain).coerceIn(Short.MIN_VALUE.toDouble(), Short.MAX_VALUE.toDouble()).toInt().toShort() else sample)
            } else {
                val sample = inputBuffer.getFloat()
                output.putFloat(if (isApplied) (sample * gain.toFloat()).coerceIn(-1f, 1f) else sample)
            }
        }
        output.flip()
    }

    override fun onReset() {
        encoding = C.ENCODING_INVALID
        bytesPerSample = 0
    }
}

// What the queue keeps on the phone so it comes back, the songs, the place in the queue and in the song and the modes
@UnstableApi
class QueueSaver(context: Context) {
    private val appContext = context.applicationContext
    private val file = File(appContext.filesDir, FileName)

    // The text to write, made on the main thread because the player is read there, an empty queue has none
    fun snapshot(player: Player): String? {
        if (player.mediaItemCount == 0) return null

        val items = JSONArray()
        for (index in 0 until player.mediaItemCount) {
            val track = with(PlayerConnection) { player.getMediaItemAt(index).toPlayableTrack() }
            items.put(
                JSONObject()
                    .put("id", track.id)
                    .put("title", track.title)
                    .put("artist", track.artist.orEmpty())
                    .put("artwork", track.artworkUrl.orEmpty())
                    .put("durationMs", track.durationMs)
                    .put("isVideo", track.isVideo)
            )
        }
        return JSONObject()
            .put("items", items)
            .put("index", player.currentMediaItemIndex)
            .put("positionMs", player.currentPosition)
            .put("repeat", player.repeatMode)
            .put("shuffle", player.shuffleModeEnabled)
            .toString()
    }

    // Writes the text, or deletes the file when the queue is empty
    fun write(text: String?) {
        if (text == null) file.delete() else file.writeText(text)
    }

    // Puts the queue back in the player, paused, true when there was one
    fun restore(player: Player): Boolean {
        val saved = runCatching { JSONObject(file.readText()) }.getOrNull() ?: return false
        val array = saved.optJSONArray("items") ?: return false

        val items = (0 until array.length()).mapNotNull { index ->
            val json = array.optJSONObject(index) ?: return@mapNotNull null
            val id = json.optString("id").takeIf { it.isNotEmpty() } ?: return@mapNotNull null
            with(PlayerConnection) {
                PlayableTrack(
                    id = id,
                    title = json.optString("title"),
                    artist = json.optString("artist").takeIf { it.isNotEmpty() },
                    artworkUrl = json.optString("artwork").takeIf { it.isNotEmpty() },
                    durationMs = json.optLong("durationMs"),
                    isVideo = json.optBoolean("isVideo")
                ).toMediaItem(appContext)
            }
        }
        if (items.isEmpty()) return false

        player.repeatMode = saved.optInt("repeat", Player.REPEAT_MODE_OFF)
        player.shuffleModeEnabled = saved.optBoolean("shuffle")
        player.setMediaItems(items, saved.optInt("index").coerceIn(items.indices), saved.optLong("positionMs").coerceAtLeast(0L))
        player.playWhenReady = false
        return true
    }

    private companion object {
        const val FileName = "persistent_queue.json"
    }
}

// The day a window of the sleep timer started on if this moment is inside one that the user chose, none otherwise, a window that crosses midnight belongs to the day it started
fun SleepSettings.activeWindow(now: LocalDateTime): LocalDate? {
    val minute = now.hour * MinutesPerHour + now.minute
    val startedYesterday = startMinute > endMinute && minute < endMinute
    val startDate = if (startedYesterday) now.toLocalDate().minusDays(1) else now.toLocalDate()

    val isInside = when {
        startMinute == endMinute -> true
        startMinute < endMinute -> minute in startMinute until endMinute
        else -> minute >= startMinute || minute < endMinute
    }
    return startDate.takeIf { isInside && it.dayOfWeek.value in days }
}

private const val MinutesPerHour = 60

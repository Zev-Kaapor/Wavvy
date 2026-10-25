package com.wavvy.app.core.recognition

// Android audio and permission
import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.SystemClock
import androidx.core.content.ContextCompat
// Coroutines
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
// The web and data
import java.io.IOException
import java.util.concurrent.atomic.AtomicBoolean
import okhttp3.Request
import org.json.JSONObject
// Project resources
import com.wavvy.app.core.innertube.currentSession
import com.wavvy.app.core.network.WavvyProxy

// How the microphone is read, in pieces of a tenth of a second, how long it listens at most, and how long the answer of YouTube is waited for after that
private const val BytesPerSample = 2
private const val ByteMask = 0xFF
private const val PieceBytes = SoundRate / 10 * BytesPerSample
private const val ListenMaxMillis = 12_000L
private const val AnswerWaitMillis = 8_000L

// Where the name of a video is asked, YouTube answers it for anyone
private const val NameAddress = "https://www.youtube.com/oembed?format=json&url=https://www.youtube.com/watch?v="

// Why a song was not found, there was nothing to match, the network failed or the microphone could not be used
enum class RecognitionFailure { NoMatch, Offline, NoMicrophone }

class RecognitionException(val failure: RecognitionFailure) : Exception(failure.name)

// Finds a song by its sound, played, sung or hummed, with the search by sound of YouTube Music
object MusicRecognizer {
    fun hasPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED

    // Listens while the sound goes to YouTube, onLevel tells how loud it is from 0 to 1, onListened the moment the listening ended and the waiting begins, and gives the words to search for the song
    suspend fun recognize(context: Context, onLevel: (Float) -> Unit, onListened: () -> Unit): String = withContext(Dispatchers.IO) {
        if (!hasPermission(context)) throw RecognitionException(RecognitionFailure.NoMicrophone)

        val session = currentSession(context)
        val youTube = YouTubeSoundSearch(session.locale, session.visitorData)
        val isFound = AtomicBoolean(false)

        coroutineScope {
            val answer = async { runCatching { youTube.answers { isFound.set(true) } } }

            // It listens until YouTube finds the song or the time is over
            try {
                listen(stop = { isFound.get() }, onPiece = { piece ->
                    youTube.send(piece)
                    onLevel(levelOf(piece))
                })
            } catch (error: Throwable) {
                youTube.cancel()
                throw error
            }
            youTube.finish()
            onListened()

            val result = withTimeoutOrNull(AnswerWaitMillis) { answer.await() }
            if (result == null) youTube.cancel()
            if (result?.isFailure == true) throw RecognitionException(RecognitionFailure.Offline)
            val videoId = result?.getOrNull() ?: throw RecognitionException(RecognitionFailure.NoMatch)

            // The name of the song is what is searched, so every version of it shows up, the one that sounded like it first
            nameOf(videoId) ?: videoId
        }
    }

    // Reads the microphone at the rate YouTube takes, giving each piece as it comes, until it is told to stop or the time is over
    @SuppressLint("MissingPermission")
    private suspend fun listen(stop: () -> Boolean, onPiece: (ByteArray) -> Unit) {
        val bufferSize = maxOf(PieceBytes, AudioRecord.getMinBufferSize(SoundRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT))
        val record = AudioRecord(MediaRecorder.AudioSource.VOICE_RECOGNITION, SoundRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, bufferSize)
        if (record.state != AudioRecord.STATE_INITIALIZED) {
            record.release()
            throw RecognitionException(RecognitionFailure.NoMicrophone)
        }

        val started = SystemClock.elapsedRealtime()
        try {
            record.startRecording()
            while (SystemClock.elapsedRealtime() - started < ListenMaxMillis && !stop()) {
                currentCoroutineContext().ensureActive()
                val piece = ByteArray(PieceBytes)
                val read = record.read(piece, 0, PieceBytes)
                if (read < 0) throw RecognitionException(RecognitionFailure.NoMicrophone)
                if (read > 0) onPiece(piece.copyOf(read))
            }
        } finally {
            record.stop()
            record.release()
        }
    }

    // How loud a piece of sound is, from 0 to 1, as the strength of its samples
    private fun levelOf(piece: ByteArray): Float {
        var sum = 0.0
        val count = piece.size / BytesPerSample
        if (count == 0) return 0f
        for (index in 0 until count) {
            val sample = (piece[index * BytesPerSample].toInt() and ByteMask) or (piece[index * BytesPerSample + 1].toInt() shl Byte.SIZE_BITS)
            sum += sample.toShort().toDouble() * sample.toShort()
        }
        return (kotlin.math.sqrt(sum / count) / Short.MAX_VALUE).toFloat().coerceIn(0f, 1f)
    }

    // The name of a video as YouTube gives it, null when it cannot be asked
    private fun nameOf(videoId: String): String? = try {
        val request = Request.Builder().url(NameAddress + videoId).build()
        WavvyProxy.client.newCall(request).execute().use { response ->
            if (response.isSuccessful) JSONObject(response.body.string()).optString("title").ifBlank { null } else null
        }
    } catch (error: IOException) {
        null
    }
}

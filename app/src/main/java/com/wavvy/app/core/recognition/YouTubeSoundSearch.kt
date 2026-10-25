package com.wavvy.app.core.recognition

// Coroutines
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
// The web
import java.io.ByteArrayOutputStream
import java.net.URLDecoder
import java.nio.ByteBuffer
import java.util.concurrent.TimeUnit
import okhttp3.Call
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody
import okio.BufferedSink
// Project resources
import com.wavvy.app.core.innertube.YouTubeLocale
import com.wavvy.app.core.network.WavvyProxy

// Where YouTube Music sends the sound of its search, the call of the Assistant made for it, and the key of its app, the same for every user
private const val AssistAddress = "https://embeddedassistant.googleapis.com/google.assistant.embedded.v1.EmbeddedAssistant/YTAssist"
private const val AppKey = "AIzaSyAOghZGza2MQSZkY_zfZ370N-PUdXEo8AI"
private const val AppUserAgent = "com.google.android.apps.youtube.music/9.20.53 (Linux; U; Android 14) grpc-java-cronet/1.62.2"
private val GrpcType: MediaType = "application/grpc".toMediaType()

// Who the sound comes from, the Android app of YouTube Music, as its search says it
private const val ClientName = 21L
private const val ClientVersion = "9.20.53"
private const val ClientOs = "Android"
private const val DeviceName = "PLACEHOLDER"

// The sound as YouTube takes it, plain samples of 16 bits at 16 kHz, and how loud its answer would be, which is not used
private const val Linear16 = 1L
const val SoundRate = 16_000
private const val AnswerVolume = 100L

// The numbers of the fields of the call, the request, its settings and the part of YouTube in them
private const val RequestAudio = 1
private const val RequestConfig = 2
private const val ConfigAudioIn = 1
private const val ConfigAudioOut = 2
private const val ConfigDialog = 3
private const val ConfigDevice = 4
private const val ConfigYouTube = 10
private const val YouTubeEnvelope = 15
private const val EnvelopeRequest = 1
private const val EnvelopeSound = 4
private const val SoundLocale = 1
private const val SoundFormat = 2
private const val SoundRegionKind = 10L
private const val SoundFormatKind = 5L
private const val RequestMode = 11
private const val SoundMode = 1L
private const val RequestSensitivity = 13
private const val SoundSensitivity = 0.7f
private const val RequestContext = 1
private const val ContextClient = 1

// The numbers of the fields of the answers, the part of YouTube that holds the page of matches
private const val AnswerYouTube = 7
private const val YouTubeAnswer = 9
private const val AnswerResponse = 1

// The song to play inside a match, and the length of the code of a video
private const val WatchCommand = 48687757
private const val WatchVideo = 1
private const val VideoIdLength = 11

// How a message of gRPC starts, a byte that says it is not compressed and four with its length
private const val FrameHeader = 5

// One search by sound with YouTube, the sound goes while it is heard and the answers come back at the same time
// Found by reading the app of YouTube Music, it is the call its search by sound makes, which listens to songs, singing and humming
class YouTubeSoundSearch(locale: YouTubeLocale, visitorData: String?) {
    private val body = StreamBody()
    private val pending = ByteArrayOutputStream()
    private val call: Call

    init {
        val request = Request.Builder()
            .url(AssistAddress)
            .header("te", "trailers")
            .header("x-goog-api-key", AppKey)
            .header("user-agent", AppUserAgent)
            .apply { visitorData?.let { header("x-goog-visitor-id", URLDecoder.decode(it, Charsets.UTF_8.name())) } }
            .post(body)
            .build()
        call = WavvyProxy.client.newBuilder().readTimeout(0, TimeUnit.MILLISECONDS).build().newCall(request)
        body.first = frame(ProtoWriter().bytes(RequestConfig, config(locale)).toByteArray())
    }

    // Waits for the answers until the end, telling the first song as soon as it comes, and gives it, null when there was none
    // The matches come in order, the one that fits the sound the most first
    fun answers(onFound: (String) -> Unit): String? {
        call.execute().use { response ->
            if (!response.isSuccessful) return null
            val source = response.body.source()
            var videoId: String? = null
            while (!source.exhausted()) {
                source.readByte()
                val message = ProtoMessage.parse(source.readByteArray(source.readInt().toLong())) ?: continue

                val page = message.message(AnswerYouTube)?.message(YouTubeAnswer)?.message(AnswerResponse) ?: continue
                val first = page.deepMessages(WatchCommand).firstNotNullOfOrNull { it.string(WatchVideo)?.takeIf { id -> id.length == VideoIdLength } }
                if (first != null && videoId == null) {
                    videoId = first
                    onFound(first)
                }
            }
            return videoId
        }
    }

    // Sends a piece of the sound, the pieces wait while the call is not open yet, false when the call is closed
    @OptIn(ExperimentalCoroutinesApi::class)
    fun send(samples: ByteArray): Boolean = runCatching {
        pending.write(frame(ProtoWriter().bytes(RequestAudio, samples).toByteArray()))
        if (body.sink.isCompleted) {
            val sink = body.sink.getCompleted()
            sink.write(pending.toByteArray())
            sink.flush()
            pending.reset()
        }
    }.isSuccess

    // No more sound comes, so YouTube gives its result
    @OptIn(ExperimentalCoroutinesApi::class)
    fun finish() {
        if (body.sink.isCompleted) runCatching { body.sink.getCompleted().close() }
    }

    fun cancel() = call.cancel()

    // What the request says once at the start, the sound, the language, the device and the part of YouTube with the context of its app
    private fun config(locale: YouTubeLocale): ByteArray = ProtoWriter()
        .message(ConfigAudioIn) { varint(1, Linear16).varint(2, SoundRate.toLong()) }
        .message(ConfigAudioOut) { varint(1, Linear16).varint(2, SoundRate.toLong()).varint(3, AnswerVolume) }
        .message(ConfigDialog) { string(2, locale.hl) }
        .message(ConfigDevice) { string(1, DeviceName).string(2, DeviceName) }
        .message(ConfigYouTube) {
            message(YouTubeEnvelope) {
                message(EnvelopeRequest) {
                    message(RequestContext) {
                        message(ContextClient) {
                            string(1, locale.language)
                            string(2, locale.country)
                            varint(16, ClientName)
                            string(17, ClientVersion)
                            string(18, ClientOs)
                        }
                    }
                    // What turns the call into the search by sound, which listens to songs and melodies and does not write down speech
                    varint(RequestMode, SoundMode)
                    float32(RequestSensitivity, SoundSensitivity)
                }
                message(EnvelopeSound) {
                    message(SoundLocale) {
                        varint(2, 0)
                        string(4, locale.hl.lowercase())
                        string(7, locale.country)
                        varint(13, SoundRegionKind)
                    }
                    message(SoundFormat) { varint(1, SoundFormatKind).varint(2, 0) }
                }
            }
        }
        .toByteArray()

    private fun frame(message: ByteArray): ByteArray =
        ByteBuffer.allocate(FrameHeader + message.size).put(0).putInt(message.size).put(message).array()

    // The body of the call that stays open while the sound is heard, the start goes at once and the rest when it comes
    private class StreamBody : RequestBody() {
        val sink = CompletableDeferred<BufferedSink>()
        var first: ByteArray = ByteArray(0)

        override fun contentType(): MediaType = GrpcType

        override fun isDuplex(): Boolean = true

        override fun writeTo(sink: BufferedSink) {
            sink.write(first)
            sink.flush()
            this.sink.complete(sink)
        }
    }
}

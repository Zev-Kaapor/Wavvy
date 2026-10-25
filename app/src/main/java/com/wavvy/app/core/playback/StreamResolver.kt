package com.wavvy.app.core.playback

// Android context, network and web storage
import android.content.Context
import android.net.ConnectivityManager
import android.webkit.CookieManager
import androidx.core.content.edit
// Coroutines
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
// Ktor networking
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.compression.ContentEncoding
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
// Stream extraction library of Metrolist
import com.metrolist.innertubex.InnerTube
import com.metrolist.innertubex.cipher.PlayerConfigRepository
import com.metrolist.innertubex.cipher.RemotePlayerConfigStore
import com.metrolist.innertubex.cipher.YouTubeCipherService
import com.metrolist.innertubex.extraction.AudioQuality
import com.metrolist.innertubex.extraction.ContentHints
import com.metrolist.innertubex.extraction.ExtractedStream
import com.metrolist.innertubex.extraction.InnerTubeExtractor
import com.metrolist.innertubex.extraction.PoTokenResult
import com.metrolist.innertubex.extraction.StreamResolveException
import com.metrolist.innertubex.extraction.TokenProvider
import com.metrolist.innertubex.extraction.TokenProviderCapabilities
import com.metrolist.innertubex.extraction.YtConfigParser
import com.metrolist.innertubex.extraction.YtConfigParserImpl
import com.metrolist.innertubex.extraction.generateClientPlaybackNonce
import com.metrolist.innertubex.extraction.strategy.PoTokenProviderKind
import com.metrolist.innertubex.models.YouTubeClient
import com.metrolist.innertubex.models.YouTubeLocale as ExtractionLocale
// Java time and concurrency
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import kotlin.time.Clock
// Project resources
import com.wavvy.app.core.innertube.MusicOrigin
import com.wavvy.app.core.innertube.VisitorStore
import com.wavvy.app.core.innertube.deviceLocale
import com.wavvy.app.core.network.WavvyProxy
import com.wavvy.app.core.playback.potoken.PoTokenGenerator
import com.wavvy.app.features.auth.data.Entry
import com.wavvy.app.features.auth.data.EntryStore

// Where the audio of a video is, with what the request for it needs
class ResolvedStream(
    val url: String,
    val headers: Map<String, String>,
    val clientName: String,
    val expiresInSeconds: Int,
    val requireBoundedRange: Boolean,
    val rangeChunkSizeBytes: Long,
    val useRangeChunks: Boolean,
    // The size of the file, known for the download, which asks all of it at once
    val contentLengthBytes: Long? = null,
    // How loud the song is, as YouTube measured it
    val loudnessDb: Double? = null,
    val perceptualLoudnessDb: Double? = null
)

// The addresses that count a listening and the time watched of a video, they come with its stream
class StreamTracking(val playbackUrl: String?, val watchtimeUrl: String?)

// Finds the audio of a video with the extraction library of Metrolist, the only way the app gets a stream, adapted from Metrolist (GPL-3.0)
object StreamResolver {
    @Volatile
    private var appContext: Context? = null

    private val bundleLock = Mutex()
    private var bundle: ExtractionBundle? = null

    // The library reads the session from shared fields, so one request at a time sets them and uses them
    private val sessionLock = Mutex()

    // The addresses that count a listening in the history of the account, they only come with a stream asked with the account, since one asked without it counts for a visitor
    private val accountTrackings = ConcurrentHashMap<String, StreamTracking>()

    // Clients that failed for a video lately, left out for a while so another one is tried
    private val failedClients = ConcurrentHashMap<String, FailedClients>()

    // Keeps the context, called once when the app starts
    @Synchronized
    fun initialize(context: Context) {
        if (appContext == null) appContext = context.applicationContext
    }

    // Loads the player config and the cipher before the first song, so it starts faster, without the account so it never changes the session of a song that opens
    suspend fun prewarm() {
        val context = requireContext()
        innerTube.locale = deviceLocale().let { ExtractionLocale(gl = it.country, hl = it.hl) }
        innerTube.visitorData = VisitorStore(context).get(deviceLocale())
        bundle().extractor.prewarm()
    }

    // The audio of a video in the quality the network allows
    // Without the account the library takes its direct path, a request and nothing else, with it the library signs in and builds tokens, which takes seconds
    // So the account is only used when the direct path cannot play the video, as with age restricted songs and private uploads
    // A download asks the best quality there is and the whole file in one piece, whatever the network is
    suspend fun resolve(videoId: String, forDownload: Boolean = false, downloadQuality: AudioQuality = AudioQuality.HIGH): Result<ResolvedStream> =
        try {
            val stream = sessionLock.withLock {
                val direct = if (hasAccount()) {
                    try {
                        extract(videoId, withAccount = false, forDownload = forDownload, downloadQuality = downloadQuality)
                    } catch (error: CancellationException) {
                        throw error
                    } catch (error: Exception) {
                        null
                    }
                } else {
                    null
                }
                direct ?: extract(videoId, withAccount = true, forDownload = forDownload, downloadQuality = downloadQuality)
            }
            check(stream.sabrBootstrap == null) { "SABR is not supported" }

            val now = Clock.System.now().toEpochMilliseconds()
            Result.success(
                ResolvedStream(
                    url = stream.audioUrl,
                    headers = stream.headers,
                    clientName = stream.clientName,
                    expiresInSeconds = stream.expiresAt
                        ?.let { ((it.toEpochMilliseconds() - now) / MillisPerSecond).toInt() }
                        ?.coerceAtLeast(1)
                        ?: DefaultStreamTtlSeconds,
                    requireBoundedRange = stream.requireBoundedRange,
                    rangeChunkSizeBytes = stream.rangeChunkSizeBytes,
                    useRangeChunks = stream.useRangeChunks,
                    contentLengthBytes = stream.contentLengthBytes,
                    loudnessDb = stream.loudnessDb,
                    perceptualLoudnessDb = stream.perceptualLoudnessDb
                ).also { Loudness.report(videoId, it.loudnessDb, it.perceptualLoudnessDb) }
            )
        } catch (error: CancellationException) {
            throw error
        } catch (error: StreamResolveException) {
            val cause = error.cause
            Result.failure(if (error.reason == StreamResolveException.Reason.NETWORK && cause != null) cause else error)
        } catch (error: Exception) {
            Result.failure(error)
        }

    // The addresses that count the listening of a video in the history of the account
    // The stream the song plays from comes from a client that does not sign in, so its addresses count for a visitor, these come from the web player of YouTube Music asked with the account
    suspend fun accountTrackingOf(videoId: String): StreamTracking? {
        accountTrackings[videoId]?.let { return it }
        if (!hasAccount()) return null

        val answer = try {
            sessionLock.withLock {
                syncSession(withAccount = true)
                val config = bundle().configParser.fetchConfig(videoId, useLoginCookies = true)
                val visitorData = innerTube.visitorData ?: config.visitorData
                val poToken = tokenProvider.getPoToken(videoId, visitorData.orEmpty(), innerTube.cookie)?.playerRequestToken
                innerTube.player(
                    client = YouTubeClient.WEB_REMIX,
                    videoId = videoId,
                    signatureTimestamp = config.signatureTimestamp,
                    poToken = poToken
                ).bodyAsText()
            }
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            return null
        }

        val player = Json.parseToJsonElement(answer).jsonObject
        val tracking = player["playbackTracking"]?.jsonObject
        val playback = tracking?.get("videostatsPlaybackUrl")?.jsonObject?.get("baseUrl")?.jsonPrimitive?.contentOrNull
        val watchtime = tracking?.get("videostatsWatchtimeUrl")?.jsonObject?.get("baseUrl")?.jsonPrimitive?.contentOrNull
        if (playback == null && watchtime == null) return null

        return StreamTracking(playback, watchtime).also {
            if (accountTrackings.size >= TrackingKept) accountTrackings.clear()
            accountTrackings[videoId] = it
        }
    }

    // A client gave a link YouTube refused, it is left out for this video for a while
    fun markClientFailed(videoId: String, clientName: String) {
        val now = System.currentTimeMillis()
        failedClients.compute(videoId) { _, failures -> FailedClients(failures?.names.orEmpty() + clientName, now) }
    }

    // One extraction with the session set for it, the stream is empty when the library found nothing playable
    private suspend fun extract(videoId: String, withAccount: Boolean, forDownload: Boolean, downloadQuality: AudioQuality): ExtractedStream {
        syncSession(withAccount)
        val hints = ContentHints().withStreamCapabilities(allowHls = false, allowSabr = false, allowBoundedRange = !forDownload)
        val stream = requireNotNull(
            bundle().extractor.extract(
                videoId = videoId,
                hints = hints,
                excludedClients = failedClientsOf(videoId),
                audioQuality = if (forDownload) downloadQuality else audioQuality(),
                clientPlaybackNonce = generateClientPlaybackNonce()
            )
        ) { "No playable stream" }
        return stream
    }

    // True when the user signed in with Google, so there is an account to fall back to
    private suspend fun hasAccount(): Boolean = EntryStore(requireContext()).entry.first() == Entry.Google

    // Captions of a video as timed lines, the last source of lyrics, adapted from Metrolist (GPL-3.0)
    suspend fun transcript(videoId: String): Result<String> = runCatching {
        val body = sessionLock.withLock {
            syncSession(withAccount = true)
            innerTube.getTranscript(YouTubeClient.WEB, videoId).bodyAsText()
        }
        val groups = Json.parseToJsonElement(body).jsonObject["actions"]?.jsonArray?.firstOrNull()?.jsonObject
            ?.get("updateEngagementPanelAction")?.jsonObject?.get("content")?.jsonObject
            ?.get("transcriptRenderer")?.jsonObject?.get("body")?.jsonObject
            ?.get("transcriptBodyRenderer")?.jsonObject?.get("cueGroups")?.jsonArray
            ?: throw IllegalStateException("No transcript")

        groups.joinToString("\n") { group ->
            val cue = group.jsonObject["transcriptCueGroupRenderer"]?.jsonObject?.get("cues")?.jsonArray?.firstOrNull()
                ?.jsonObject?.get("transcriptCueRenderer")?.jsonObject
            val time = cue?.get("startOffsetMs")?.jsonPrimitive?.contentOrNull?.toLongOrNull() ?: 0L
            val text = cue?.get("cue")?.jsonObject?.get("simpleText")?.jsonPrimitive?.contentOrNull.orEmpty().trim('♪', ' ')
            "[%02d:%02d.%03d]%s".format(time / MillisPerMinute, (time / MillisPerSecond) % SecondsPerMinute, time % MillisPerSecond, text)
        }
    }

    // Lets every client be tried again, after the player config changed
    fun clearClientFailures() {
        failedClients.clear()
    }

    // Reloads the player config after YouTube refused a stream, true when it changed
    suspend fun refreshAfterStreamRejection(): Boolean = bundle().cipherService.refreshAfterStreamRejection()

    // Clients left out for a video, forgotten once their time passes
    private fun failedClientsOf(videoId: String): Set<String> {
        val failures = failedClients[videoId] ?: return emptySet()
        if (System.currentTimeMillis() - failures.failedAtMs !in 0 until ClientFailureTtlMs) {
            failedClients.remove(videoId, failures)
            return emptySet()
        }
        return failures.names
    }

    // Gives the library the same identity the Home uses, the device language, the saved visitor and the account when there is one and it is asked for
    private suspend fun syncSession(withAccount: Boolean) {
        val context = requireContext()
        val locale = deviceLocale()
        innerTube.locale = ExtractionLocale(gl = locale.country, hl = locale.hl)
        innerTube.visitorData = VisitorStore(context).get(locale)
        innerTube.cookie = if (withAccount && EntryStore(context).entry.first() == Entry.Google) {
            withContext(Dispatchers.Main) { CookieManager.getInstance().getCookie(MusicOrigin) }
        } else {
            null
        }
    }

    // The quality of what is played
    @Volatile
    var streamQuality: AudioQuality = AudioQuality.HIGH

    // The one the user chose, and when it is left to the app the best on Wi-Fi and a lighter one on a metered network
    private fun audioQuality(): AudioQuality {
        if (streamQuality != AudioQuality.AUTO) return streamQuality

        val connectivity = requireContext().getSystemService(ConnectivityManager::class.java)
        return if (connectivity?.isActiveNetworkMetered == true) AudioQuality.LOW else AudioQuality.AUTO
    }

    // The cipher and the extractor, built once
    private suspend fun bundle(): ExtractionBundle {
        bundle?.let { return it }
        return bundleLock.withLock {
            bundle ?: run {
                val remoteStore = RemotePlayerConfigStore(httpClient, configRepository)
                val cipherService = YouTubeCipherService(httpClient, remoteStore)
                val configParser = YtConfigParserImpl(httpClient, innerTube, remoteStore).withEmbeddedConfigFallback()
                val extractor = InnerTubeExtractor(
                    configParser = configParser,
                    cipherService = cipherService,
                    innerTube = innerTube,
                    tokenProvider = tokenProvider
                )
                ExtractionBundle(cipherService, extractor, configParser).also { bundle = it }
            }
        }
    }

    // Falls back to the embedded page when the watch page does not give the config
    private fun YtConfigParser.withEmbeddedConfigFallback(): YtConfigParser =
        object : YtConfigParser by this {
            override suspend fun fetchConfig(videoId: String, useLoginCookies: Boolean) =
                try {
                    this@withEmbeddedConfigFallback.fetchConfig(videoId, useLoginCookies)
                } catch (_: IllegalStateException) {
                    this@withEmbeddedConfigFallback.fetchEmbeddedConfig(videoId, useLoginCookies = false)
                }
        }

    private fun requireContext(): Context = requireNotNull(appContext) { "StreamResolver is not initialized" }

    // HTTP client of the library, configured as Metrolist does
    private val httpClient by lazy {
        HttpClient(OkHttp) {
            // The library checks the status of each answer itself
            expectSuccess = false

            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true; explicitNulls = false; encodeDefaults = true })
            }
            install(ContentEncoding) {
                gzip(GzipQuality)
                deflate(DeflateQuality)
            }
            engine {
                preconfigured = WavvyProxy.client
                config {
                    connectTimeout(ConnectTimeoutSeconds, TimeUnit.SECONDS)
                    readTimeout(ReadTimeoutSeconds, TimeUnit.SECONDS)
                    writeTimeout(ReadTimeoutSeconds, TimeUnit.SECONDS)
                    retryOnConnectionFailure(true)
                }
            }
            install(HttpTimeout) {
                requestTimeoutMillis = ReadTimeoutSeconds * MillisPerSecond
                connectTimeoutMillis = ConnectTimeoutSeconds * MillisPerSecond
                socketTimeoutMillis = ReadTimeoutSeconds * MillisPerSecond
            }
            defaultRequest {
                url("$MusicOrigin/youtubei/v1/")
                header("Accept", "application/json")
                header("Cache-Control", "no-cache")
            }
        }
    }

    private val innerTube by lazy { InnerTube(httpClient) }

    // Player config shared by the Metrolist projects, kept on the device so it works offline between updates
    private val configRepository: PlayerConfigRepository by lazy {
        val preferences = requireContext().getSharedPreferences(ConfigPreferences, Context.MODE_PRIVATE)
        object : PlayerConfigRepository {
            override val enabled: Boolean = true
            override val sourceUrl: String = PlayerConfigUrl
            override val defaultSourceUrl: String = PlayerConfigUrl
            override var cachedJson: String
                get() = preferences.getString("json", "").orEmpty()
                set(value) = preferences.edit { putString("json", value) }
            override var cachedAtMs: Long
                get() = preferences.getLong("cached_at_ms", 0L)
                set(value) = preferences.edit { putLong("cached_at_ms", value) }
            override var cachedSourceUrl: String
                get() = preferences.getString("source_url", "").orEmpty()
                set(value) = preferences.edit { putString("source_url", value) }
            override var cachedEtag: String
                get() = preferences.getString("etag", "").orEmpty()
                set(value) = preferences.edit { putString("etag", value) }
        }
    }

    private val poTokenGenerator by lazy { PoTokenGenerator(requireContext()) }

    // Tokens of the web player, made by the hidden WebView
    private val tokenProvider = object : TokenProvider {
        override val capabilities = TokenProviderCapabilities(providers = setOf(PoTokenProviderKind.WEB_BOTGUARD), usesWebView = true)

        override suspend fun getPoToken(videoId: String, visitorData: String, cookie: String?): PoTokenResult? =
            poTokenGenerator.getWebClientPoToken(videoId, visitorData)?.let { token ->
                PoTokenResult(
                    playerRequestToken = token.playerRequestPoToken,
                    streamingDataToken = token.streamingDataPoToken,
                    visitorData = visitorData
                )
            }

        override suspend fun close() {
            poTokenGenerator.close()
        }
    }

    private class ExtractionBundle(
        val cipherService: YouTubeCipherService,
        val extractor: InnerTubeExtractor,
        val configParser: YtConfigParser
    )

    private class FailedClients(
        val names: Set<String>,
        val failedAtMs: Long
    )

    // Player config source, the file where it is kept, and how long a link and a failed client last
    private const val PlayerConfigUrl = "https://raw.githubusercontent.com/ZemerTeam/zemer-cipher/master/library/src/main/assets/player_configs.json"
    private const val ConfigPreferences = "innertubex_player_config"
    private const val DefaultStreamTtlSeconds = 5 * 60
    private const val TrackingKept = 200
    private const val ClientFailureTtlMs = 5 * 60 * 1000L

    // Timeouts of the requests, and the weights of the compressions the client accepts
    private const val ConnectTimeoutSeconds = 30L
    private const val ReadTimeoutSeconds = 60L
    private const val MillisPerSecond = 1000L
    private const val MillisPerMinute = 60_000L
    private const val SecondsPerMinute = 60L
    private const val GzipQuality = 0.9F
    private const val DeflateQuality = 0.8F
}

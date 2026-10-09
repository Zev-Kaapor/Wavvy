package com.wavvy.app.core.innertube

// Coroutines
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
// JSON and network
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

// Limits and answers of the requests
private const val TimeoutMillis = 30_000
private const val HttpOk = 200
private const val Attempts = 3
private const val ErrorBodyMax = 400
private const val FirstDelayMillis = 500L
private const val HttpOkEnd = 300

// Random name of a playback, the characters and the length YouTube accepts
private const val NonceAlphabet = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789-_"
private const val NonceLength = 16
private val TransientStatus = setOf(408, 425, 429, 500, 502, 503, 504)

// Where the guest identity of YouTube comes from, the service worker data first and the page of the player after it
private const val VisitorDataUrl = "https://www.youtube.com/sw.js_data"
private val VisitorDataPattern = Regex("\"(?:VISITOR_DATA|visitorData)\"\\s*:\\s*\"([^\"]+)\"")
private const val VisitorDataPosition = 13

// A client of YouTube Music as it presents itself, the web player by default
data class ClientProfile(
    val name: String = ClientName,
    val version: String = ClientVersion,
    val id: String = ClientId,
    val userAgent: String = WebUserAgent,
    // What the request says about where it comes from and who signs it, the web player says all of it
    val sendOrigin: Boolean = true,
    val sendSignature: Boolean = true,
    val sendVisitor: Boolean = true
)

// The answer of YouTube is not what was expected
class InnerTubeException(message: String) : IOException(message)

// Requests to the YouTube Music API, as the web player makes them
object InnerTubeClient {
    // Home and other pages that are opened by an id, a filter of the page or the continuation of a previous answer
    suspend fun browse(
        session: YouTubeSession,
        browseId: String? = null,
        params: String? = null,
        continuation: String? = null,
        profile: ClientProfile = ClientProfile(),
        // The values chosen in the form of the page, such as the country of the charts
        selectedValues: List<String>? = null
    ): Result<JSONObject> = withContext(Dispatchers.IO) {
        runCatching {
            val body = JSONObject()
                .put("context", contextFor(session, profile))
                .apply {
                    browseId?.let { put("browseId", it) }
                    params?.let { put("params", it) }
                    continuation?.let { put("continuation", it) }
                    selectedValues?.let { put("formData", JSONObject().put("selectedValues", JSONArray(it))) }
                }

            JSONObject(post("$MusicApi/browse?prettyPrint=false", session, body.toString(), profile))
        }
    }

    // Page of a video in the player, with its tabs such as the lyrics, and its queue when a playlist is given
    suspend fun next(
        session: YouTubeSession,
        videoId: String,
        playlistId: String? = null,
        continuation: String? = null
    ): Result<JSONObject> = withContext(Dispatchers.IO) {
        runCatching {
            val body = JSONObject()
                .put("context", contextFor(session))
                .put("videoId", videoId)
                .apply {
                    playlistId?.let { put("playlistId", it) }
                    continuation?.let { put("continuation", it) }
                }

            JSONObject(post("$MusicApi/next?prettyPrint=false", session, body.toString()))
        }
    }

    // Results of a search, under the filter its parameters carry, or the next page of them when a continuation is given
    suspend fun search(
        session: YouTubeSession,
        query: String? = null,
        params: String? = null,
        continuation: String? = null
    ): Result<JSONObject> = withContext(Dispatchers.IO) {
        runCatching {
            val body = JSONObject()
                .put("context", contextFor(session))
                .apply {
                    query?.let { put("query", it) }
                    params?.let { put("params", it) }
                    continuation?.let { put("continuation", it) }
                }

            JSONObject(post("$MusicApi/search?prettyPrint=false", session, body.toString()))
        }
    }

    // Words YouTube Music suggests for what was typed so far, with some songs and artists that match it
    suspend fun searchSuggestions(
        session: YouTubeSession,
        input: String
    ): Result<JSONObject> = withContext(Dispatchers.IO) {
        runCatching {
            val body = JSONObject()
                .put("context", contextFor(session))
                .put("input", input)

            JSONObject(post("$MusicApi/music/get_search_suggestions?prettyPrint=false", session, body.toString()))
        }
    }

    // Removes what the tokens point to, such as searches of the history of the account
    suspend fun feedback(
        session: YouTubeSession,
        tokens: List<String>
    ): Result<JSONObject> = withContext(Dispatchers.IO) {
        runCatching {
            val body = JSONObject()
                .put("context", contextFor(session))
                .put("feedbackTokens", JSONArray(tokens))

            JSONObject(post("$MusicApi/feedback?prettyPrint=false", session, body.toString()))
        }
    }

    // Subscribes the account to a channel, or takes the subscription away, the params come with the button of the page
    suspend fun subscription(
        session: YouTubeSession,
        channelId: String,
        subscribe: Boolean,
        params: String? = null
    ): Result<JSONObject> = withContext(Dispatchers.IO) {
        runCatching {
            val body = JSONObject()
                .put("context", contextFor(session))
                .put("channelIds", JSONArray().put(channelId))
                .apply { params?.let { put("params", it) } }

            val action = if (subscribe) "subscribe" else "unsubscribe"
            JSONObject(post("$MusicApi/subscription/$action?prettyPrint=false", session, body.toString()))
        }
    }

    // The answer about the count as it comes
    suspend fun unseenAnswer(session: YouTubeSession): JSONObject {
        val body = JSONObject().put("context", contextFor(session))
        return JSONObject(post("$MusicApi/notification/get_unseen_count?prettyPrint=false", session, body.toString()))
    }

    // Registers a listening in the history of the account, the player answer has the address that counts it
    suspend fun registerPlayback(
        session: YouTubeSession,
        videoId: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val body = JSONObject()
                .put("context", contextFor(session))
                .put("videoId", videoId)

            val answer = JSONObject(post("$MusicApi/player?prettyPrint=false", session, body.toString()))
            val address = answer.optJSONObject("playbackTracking")
                ?.optJSONObject("videostatsPlaybackUrl")
                ?.optString("baseUrl")
                ?.takeIf { it.isNotBlank() }
                ?: throw InnerTubeException("YouTube did not return the tracking address")

            val nonce = (1..NonceLength).map { NonceAlphabet.random() }.joinToString("")
            val connection = URL("$address&ver=2&c=$ClientName&cpn=$nonce").openConnection() as HttpURLConnection
            try {
                connection.connectTimeout = TimeoutMillis
                connection.readTimeout = TimeoutMillis
                headersFor(session).forEach { (name, value) -> connection.setRequestProperty(name, value) }
                if (connection.responseCode !in HttpOk until HttpOkEnd) throw InnerTubeException("YouTube answered ${connection.responseCode}")
            } finally {
                connection.disconnect()
            }
        }
    }

    // Tells YouTube Music where the listening of a long episode stands, so its progress follows the account on the other apps
    // The answer of the player has the address that counts the time watched, it is asked with the place the episode is at
    suspend fun reportWatchTime(
        session: YouTubeSession,
        videoId: String,
        positionSeconds: Long,
        lengthSeconds: Long
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val body = JSONObject()
                .put("context", contextFor(session))
                .put("videoId", videoId)

            val answer = JSONObject(post("$MusicApi/player?prettyPrint=false", session, body.toString()))
            val address = answer.optJSONObject("playbackTracking")
                ?.optJSONObject("videostatsWatchtimeUrl")
                ?.optString("baseUrl")
                ?.takeIf { it.isNotBlank() }
                ?: throw InnerTubeException("YouTube did not return the watch time address")

            val nonce = (1..NonceLength).map { NonceAlphabet.random() }.joinToString("")
            val query = "ver=2&c=$ClientName&cpn=$nonce&cmt=$positionSeconds&st=0&et=$positionSeconds&len=$lengthSeconds&state=paused&volume=100&muted=0"
            val connection = URL("$address&$query").openConnection() as HttpURLConnection
            try {
                connection.connectTimeout = TimeoutMillis
                connection.readTimeout = TimeoutMillis
                headersFor(session).forEach { (name, value) -> connection.setRequestProperty(name, value) }
                if (connection.responseCode !in HttpOk until HttpOkEnd) throw InnerTubeException("YouTube answered ${connection.responseCode}")
            } finally {
                connection.disconnect()
            }
        }
    }

    // Identity YouTube gives to a visitor, it keeps the recommendations of a guest the same between launches
    suspend fun fetchVisitorData(locale: YouTubeLocale): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val fromWorker = runCatching { parseWorkerVisitorData(get(VisitorDataUrl, locale)) }.getOrNull()
            fromWorker
                ?: VisitorDataPattern.find(get(MusicOrigin, locale))?.groupValues?.get(1)
                ?: throw InnerTubeException("YouTube did not return visitor data")
        }
    }

    // The part of every body that tells who is asking, in which language and from where
    private fun contextFor(session: YouTubeSession, profile: ClientProfile = ClientProfile()): JSONObject =
        JSONObject()
            .put(
                "client",
                JSONObject()
                    .put("clientName", profile.name)
                    .put("clientVersion", profile.version)
                    .put("gl", session.locale.country)
                    .put("hl", session.locale.hl)
                    .apply { session.visitorData?.let { put("visitorData", it) } }
            )
            .put("request", JSONObject().put("useSsl", true).put("internalExperimentFlags", JSONArray()))
            .put("user", JSONObject().put("lockedSafetyMode", false))

    // Posts the body and gives the text of the answer, trying again when YouTube is busy
    private suspend fun post(url: String, session: YouTubeSession, body: String, profile: ClientProfile = ClientProfile()): String {
        var wait = FirstDelayMillis
        var attempt = 1

        while (true) {
            try {
                val connection = URL(url).openConnection() as HttpURLConnection
                try {
                    connection.requestMethod = "POST"
                    connection.doOutput = true
                    connection.connectTimeout = TimeoutMillis
                    connection.readTimeout = TimeoutMillis
                    headersFor(session, profile).forEach { (name, value) -> connection.setRequestProperty(name, value) }
                    connection.outputStream.use { it.write(body.toByteArray()) }

                    val status = connection.responseCode
                    if (status == HttpOk) return connection.inputStream.bufferedReader().use { it.readText() }
                    if (status !in TransientStatus || attempt >= Attempts) {
                        // What YouTube says about the refusal helps to tell why
                        val reason = connection.errorStream?.bufferedReader()?.use { it.readText() }?.take(ErrorBodyMax).orEmpty()
                        throw InnerTubeException("YouTube answered $status $reason".trim())
                    }
                } finally {
                    connection.disconnect()
                }
            } catch (failure: InnerTubeException) {
                throw failure
            } catch (failure: IOException) {
                if (attempt >= Attempts) throw failure
            }

            delay(wait)
            wait *= 2
            attempt++
        }
    }

    // Text of a page
    private fun get(url: String, locale: YouTubeLocale): String {
        val connection = URL(url).openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = TimeoutMillis
            connection.readTimeout = TimeoutMillis
            connection.setRequestProperty("User-Agent", WebUserAgent)
            connection.setRequestProperty("Accept-Language", locale.acceptLanguage)
            if (connection.responseCode != HttpOk) throw InnerTubeException("YouTube answered ${connection.responseCode}")

            return connection.inputStream.bufferedReader().use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }

    // Headers of the web player, with the cookies and the signature when the user is signed in
    private fun headersFor(session: YouTubeSession, profile: ClientProfile = ClientProfile()): Map<String, String> {
        val headers = linkedMapOf(
            "Content-Type" to "application/json",
            "Accept" to "application/json",
            "User-Agent" to profile.userAgent,
            "X-Goog-Api-Format-Version" to "1",
            "X-YouTube-Client-Name" to profile.id,
            "X-YouTube-Client-Version" to profile.version,
            "Accept-Language" to session.locale.acceptLanguage,
            "Cookie" to cookiesWithLocale(session.cookies, session.locale)
        )
        if (profile.sendOrigin) {
            headers["Origin"] = MusicOrigin
            headers["X-Origin"] = MusicOrigin
            headers["Referer"] = "$MusicOrigin/"
        }
        if (profile.sendVisitor) session.visitorData?.let { headers["X-Goog-Visitor-Id"] = it }

        val signature = session.cookies?.takeIf { it.isNotBlank() && profile.sendSignature }?.let { signatureFor(it) }
        if (signature != null) {
            headers["X-Goog-AuthUser"] = "0"
            headers["Authorization"] = "SAPISIDHASH $signature"
        }

        return headers
    }

    // The visitor data sits at a fixed position of the service worker answer, after its first line
    private fun parseWorkerVisitorData(text: String): String? =
        JSONArray(text.substringAfter('\n').trimStart())
            .getJSONArray(0).getJSONArray(2).getJSONArray(0).getJSONArray(0)
            .getString(VisitorDataPosition)
            .takeIf { it.isNotBlank() }
}

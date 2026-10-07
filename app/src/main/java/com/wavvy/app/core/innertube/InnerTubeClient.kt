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
private const val FirstDelayMillis = 500L
private val TransientStatus = setOf(408, 425, 429, 500, 502, 503, 504)

// Where the guest identity of YouTube comes from, the service worker data first and the page of the player after it
private const val VisitorDataUrl = "https://www.youtube.com/sw.js_data"
private val VisitorDataPattern = Regex("\"(?:VISITOR_DATA|visitorData)\"\\s*:\\s*\"([^\"]+)\"")
private const val VisitorDataPosition = 13

// The answer of YouTube is not what was expected
class InnerTubeException(message: String) : IOException(message)

// Requests to the YouTube Music API, as the web player makes them
object InnerTubeClient {
    // Home and other pages that are opened by an id, a filter of the page or the continuation of a previous answer
    suspend fun browse(
        session: YouTubeSession,
        browseId: String? = null,
        params: String? = null,
        continuation: String? = null
    ): Result<JSONObject> = withContext(Dispatchers.IO) {
        runCatching {
            val body = JSONObject()
                .put("context", contextFor(session))
                .apply {
                    browseId?.let { put("browseId", it) }
                    params?.let { put("params", it) }
                    continuation?.let { put("continuation", it) }
                }

            JSONObject(post("$MusicApi/browse?prettyPrint=false", session, body.toString()))
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
    private fun contextFor(session: YouTubeSession): JSONObject =
        JSONObject()
            .put(
                "client",
                JSONObject()
                    .put("clientName", ClientName)
                    .put("clientVersion", ClientVersion)
                    .put("gl", session.locale.country)
                    .put("hl", session.locale.hl)
                    .apply { session.visitorData?.let { put("visitorData", it) } }
            )
            .put("request", JSONObject().put("useSsl", true).put("internalExperimentFlags", JSONArray()))
            .put("user", JSONObject().put("lockedSafetyMode", false))

    // Posts the body and gives the text of the answer, trying again when YouTube is busy
    private suspend fun post(url: String, session: YouTubeSession, body: String): String {
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
                    headersFor(session).forEach { (name, value) -> connection.setRequestProperty(name, value) }
                    connection.outputStream.use { it.write(body.toByteArray()) }

                    val status = connection.responseCode
                    if (status == HttpOk) return connection.inputStream.bufferedReader().use { it.readText() }
                    if (status !in TransientStatus || attempt >= Attempts) throw InnerTubeException("YouTube answered $status")
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
    private fun headersFor(session: YouTubeSession): Map<String, String> {
        val headers = linkedMapOf(
            "Content-Type" to "application/json",
            "Accept" to "application/json",
            "User-Agent" to WebUserAgent,
            "X-Goog-Api-Format-Version" to "1",
            "X-YouTube-Client-Name" to ClientId,
            "X-YouTube-Client-Version" to ClientVersion,
            "Origin" to MusicOrigin,
            "X-Origin" to MusicOrigin,
            "Referer" to "$MusicOrigin/",
            "Accept-Language" to session.locale.acceptLanguage,
            "Cookie" to cookiesWithLocale(session.cookies, session.locale)
        )
        session.visitorData?.let { headers["X-Goog-Visitor-Id"] = it }

        val signature = session.cookies?.takeIf { it.isNotBlank() }?.let { signatureFor(it) }
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

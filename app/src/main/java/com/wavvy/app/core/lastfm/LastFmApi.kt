package com.wavvy.app.core.lastfm

// Hashing and the web
import java.io.IOException
import java.security.MessageDigest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.Request
import org.json.JSONObject
// Project resources
import com.wavvy.app.BuildConfig
import com.wavvy.app.core.network.WavvyProxy

// Where the API of Last.fm is, and how the app says who it is
private const val ApiAddress = "https://ws.audioscrobbler.com/2.0/"
private const val UserAgent = "Wavvy (https://github.com/Zev-Kaapor/Wavvy)"

// The codes of Last.fm that decide what is done next, the key of the session is no good, or the service is busy or down
const val LastFmBadSession = 9
private val LastFmRetryCodes = setOf(11, 16, 29)

// Last.fm takes this many songs in one call
const val LastFmBatchSize = 50

// Why Last.fm did not accept a call, with the code it gave
class LastFmException(val code: Int, message: String) : Exception(message) {
    // The ones that can work later, as the service coming back
    val isTemporary: Boolean get() = code in LastFmRetryCodes
}

// The account of the user at Last.fm, its name and the key that stands for the login
data class LastFmSession(val name: String, val key: String)

// A song that was listened to, when it began in seconds, and the length of it in seconds or zero when it is not known
data class Scrobble(val artist: String, val title: String, val album: String?, val durationSec: Int, val timestamp: Long)

// The calls to Last.fm, each one signed with the secret of the app as its API asks
object LastFmApi {
    // The keys come with the build, an app made without them has no Last.fm
    val isAvailable: Boolean get() = BuildConfig.LASTFM_API_KEY.isNotEmpty() && BuildConfig.LASTFM_SECRET.isNotEmpty()

    // Logs in with the name and the password of the user, which go only to Last.fm and are not kept
    suspend fun login(username: String, password: String): LastFmSession {
        val answer = call("auth.getMobileSession", null, mapOf("username" to username, "password" to password))
        val session = answer.getJSONObject("session")
        return LastFmSession(session.getString("name"), session.getString("key"))
    }

    // Tells what plays now, it does not count as a listening
    suspend fun nowPlaying(session: LastFmSession, song: Scrobble) {
        call("track.updateNowPlaying", session.key, details(song, null))
    }

    // Counts the songs as listened, up to the size of a batch
    suspend fun scrobble(session: LastFmSession, songs: List<Scrobble>) {
        if (songs.isEmpty()) return
        val params = HashMap<String, String>()
        songs.take(LastFmBatchSize).forEachIndexed { index, song -> params += details(song, index) }
        call("track.scrobble", session.key, params)
    }

    // The fields of a song, the ones of a batch carry their place
    private fun details(song: Scrobble, index: Int?): Map<String, String> {
        val suffix = index?.let { "[$it]" }.orEmpty()
        return buildMap {
            put("artist$suffix", song.artist)
            put("track$suffix", song.title)
            song.album?.takeIf { it.isNotBlank() }?.let { put("album$suffix", it) }
            if (song.durationSec > 0) put("duration$suffix", song.durationSec.toString())
            if (index != null) put("timestamp$suffix", song.timestamp.toString())
        }
    }

    // The fields in the order of their names with the secret at the end, hashed, as Last.fm checks them
    private fun signature(params: Map<String, String>): String {
        val text = params.toSortedMap().entries.joinToString("") { it.key + it.value } + BuildConfig.LASTFM_SECRET
        return MessageDigest.getInstance("MD5").digest(text.toByteArray()).joinToString("") { "%02x".format(it) }
    }

    // One signed call, a network failure comes as IOException and an answer of Last.fm that says no as LastFmException
    private suspend fun call(method: String, sessionKey: String?, extra: Map<String, String>): JSONObject = withContext(Dispatchers.IO) {
        val params = HashMap<String, String>(extra)
        params["method"] = method
        params["api_key"] = BuildConfig.LASTFM_API_KEY
        sessionKey?.let { params["sk"] = it }

        val body = FormBody.Builder().apply {
            params.forEach { (key, value) -> add(key, value) }
            add("api_sig", signature(params))
            add("format", "json")
        }.build()
        val request = Request.Builder().url(ApiAddress).header("User-Agent", UserAgent).post(body).build()

        WavvyProxy.client.newCall(request).execute().use { response ->
            val text = response.body.string()
            val json = runCatching { JSONObject(text) }.getOrNull() ?: throw IOException("Last.fm answered with something that is not understood")
            if (json.has("error")) throw LastFmException(json.optInt("error"), json.optString("message"))
            json
        }
    }
}

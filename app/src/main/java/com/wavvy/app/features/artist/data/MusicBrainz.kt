package com.wavvy.app.features.artist.data

// Android and Java networking
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
// Coroutines
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
// JSON
import org.json.JSONObject
// JSON helpers of the YouTube Music answers
import com.wavvy.app.core.innertube.arrayAt
import com.wavvy.app.core.innertube.objectAt
import com.wavvy.app.core.innertube.objects
import com.wavvy.app.core.innertube.stringAt

// Address of the open database of artists, and who is asking, as it asks of every app
private const val BaseUrl = "https://musicbrainz.org/ws/2"
private const val UserAgent = "Wavvy ( https://github.com/Zev-Kaapor/Wavvy )"
private const val TimeoutMillis = 15_000
private const val HttpOk = 200

// The database allows one request each second, a little more is kept between them
private const val RequestGapMillis = 1_100L

// Page of YouTube where a channel lives, which MusicBrainz keeps as a link of the artist
private const val ChannelUrl = "https://www.youtube.com/channel/"

// How well a name has to match to be taken as the artist when no link says so, out of one hundred
private const val MinimumScore = 95

// How many genres are kept
private const val MaxGenres = 8

// The kinds of links that are shown, the other ones are records of databases
private val LinkTypes = setOf(
    "official homepage", "social network", "free streaming", "streaming", "bandcamp", "last.fm", "lyrics", "discogs", "allmusic", "wikipedia"
)

// The kind of link that is the own site of the artist
private const val WebsiteType = "official homepage"

// Names of the sites that are written in their own way
private val SiteNames = mapOf(
    "open.spotify.com" to "Spotify",
    "spotify.com" to "Spotify",
    "music.apple.com" to "Apple Music",
    "itunes.apple.com" to "Apple Music",
    "x.com" to "X",
    "twitter.com" to "X",
    "last.fm" to "Last.fm",
    "allmusic.com" to "AllMusic",
    "soundcloud.com" to "SoundCloud",
    "tiktok.com" to "TikTok",
    "music.youtube.com" to "YouTube Music",
    "youtube.com" to "YouTube"
)

// Reads the artist from MusicBrainz, first by the channel of YouTube that the database knows, then by the name when it matches exactly
class MusicBrainz {
    private val gate = Mutex()

    // The profile of an artist, null when the database has nobody that is surely the artist
    suspend fun profile(channelIds: List<String>, name: String): Result<ArtistProfile?> = runCatching {
        val id = channelIds.firstNotNullOfOrNull { byChannel(it) } ?: byName(name) ?: return@runCatching null
        parse(get("artist/$id?inc=genres+tags+url-rels&fmt=json"))
    }

    // The artist linked to a channel, nobody when the database has no such link
    private suspend fun byChannel(channelId: String): String? {
        val resource = URLEncoder.encode(ChannelUrl + channelId, "UTF-8")
        val answer = runCatching { get("url?resource=$resource&inc=artist-rels&fmt=json") }.getOrNull() ?: return null

        return answer.arrayAt("relations").objects().firstNotNullOfOrNull { it.stringAt("artist", "id") }
    }

    // The artist with exactly this name that the database is surest of
    private suspend fun byName(name: String): String? {
        val query = URLEncoder.encode("artist:\"$name\"", "UTF-8")

        return get("artist?query=$query&limit=5&fmt=json").arrayAt("artists").objects()
            .filter { it.optInt("score") >= MinimumScore && it.optString("name").equals(name, ignoreCase = true) }
            .maxByOrNull { it.optInt("score") }
            ?.stringAt("id")
    }

    // What the database says, the genres by how many people voted for them and the links that are worth showing
    private fun parse(artist: JSONObject): ArtistProfile {
        val genres = artist.arrayAt("genres").objects()
            .sortedByDescending { it.optInt("count") }
            .mapNotNull { it.stringAt("name") }
            .ifEmpty { artist.arrayAt("tags").objects().sortedByDescending { it.optInt("count") }.mapNotNull { it.stringAt("name") } }
            .take(MaxGenres)

        val links = artist.arrayAt("relations").objects()
            .filter { it.optString("type") in LinkTypes }
            .mapNotNull { relation ->
                relation.stringAt("url", "resource")?.let { ArtistLink(platformOf(it), it, isWebsite = relation.optString("type") == WebsiteType) }
            }
            .distinctBy { if (it.isWebsite) it.url else it.platform }

        return ArtistProfile(
            type = artist.stringAt("type"),
            begin = artist.stringAt("life-span", "begin"),
            end = artist.stringAt("life-span", "end"),
            countryCode = artist.stringAt("country") ?: artist.arrayAt("area", "iso-3166-1-codes")?.optString(0)?.takeIf { it.isNotBlank() },
            city = artist.stringAt("begin-area", "name"),
            genres = genres,
            links = links
        )
    }

    // The name of the site of an address
    private fun platformOf(url: String): String {
        val host = runCatching { URL(url).host.removePrefix("www.") }.getOrDefault(url)

        return SiteNames[host] ?: host.substringBeforeLast(".").substringAfterLast(".").replaceFirstChar { it.uppercase() }
    }

    // One request at a time, with the gap the database asks for between them
    private suspend fun get(path: String): JSONObject = gate.withLock {
        delay(RequestGapMillis)
        withContext(Dispatchers.IO) {
            val connection = URL("$BaseUrl/$path").openConnection() as HttpURLConnection
            try {
                connection.connectTimeout = TimeoutMillis
                connection.readTimeout = TimeoutMillis
                connection.setRequestProperty("User-Agent", UserAgent)
                connection.setRequestProperty("Accept", "application/json")
                if (connection.responseCode != HttpOk) throw IOException("MusicBrainz answered ${connection.responseCode}")

                JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
            } finally {
                connection.disconnect()
            }
        }
    }
}

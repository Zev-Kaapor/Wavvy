package com.wavvy.app.features.playlist.data

// Android context
import android.content.Context
// JSON
import java.util.concurrent.ConcurrentHashMap
import org.json.JSONArray
import org.json.JSONObject
// Coroutines and reactive flows
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
// Project resources
import com.wavvy.app.core.innertube.InnerTubeClient
import com.wavvy.app.core.innertube.arrayAt
import com.wavvy.app.core.innertube.currentSession
import com.wavvy.app.core.innertube.findObjects
import com.wavvy.app.core.innertube.objects
import com.wavvy.app.core.innertube.stringAt
import com.wavvy.app.features.collection.data.CollectionKind
import com.wavvy.app.features.collection.data.CollectionRepository
import com.wavvy.app.features.home.data.HomeItemKind
import com.wavvy.app.features.home.data.PlaylistPagePrefix
import com.wavvy.app.features.library.data.LibraryParser
import com.wavvy.app.features.library.data.LibrarySource

// A playlist of the account in the list of where to save, and whether it already has the songs
data class PlaylistOption(
    val id: String,
    val title: String,
    val coverUrl: String?,
    val line: String?,
    val hasSongs: Boolean,
    // How many songs it has, known once the playlist is read
    val songCount: Int? = null
)

// Who can find a new playlist, as YouTube Music names it
enum class PlaylistPrivacy(val status: String) {
    Public("PUBLIC"),
    Unlisted("UNLISTED"),
    Private("PRIVATE")
}

// Runs the changes that go on after the menu that asked them is closed
object PlaylistActions {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
}

// What the playlists of the account have, kept for a few minutes so the sheet that saves can say which already have a song
object PlaylistContents {
    private class Entry(val loadedAt: Long, val songs: Map<String, List<String>>)

    private val entries = ConcurrentHashMap<String, Entry>()

    fun fresh(playlistId: String): Map<String, List<String>>? =
        entries[playlistId]?.takeIf { System.currentTimeMillis() - it.loadedAt < KeptMillis }?.songs

    fun put(playlistId: String, songs: Map<String, List<String>>) {
        entries[playlistId] = Entry(System.currentTimeMillis(), songs)
    }

    fun invalidate(playlistId: String) {
        entries.remove(playlistId)
    }

    private const val KeptMillis = 5 * 60 * 1000L
}

// A change that the lists show at once, before YouTube Music answers
sealed interface PlaylistEvent {
    // A song left a playlist, from its place when it is known
    data class SongRemoved(val playlistId: String, val videoId: String, val setVideoId: String?) : PlaylistEvent

    // A song was liked or lost its like
    data class LikeChanged(val videoId: String, val isLiked: Boolean) : PlaylistEvent
}

// Counts every change to the playlists, so the lists that show them ask again, and tells the ones that can show it without asking
object PlaylistChanges {
    private val mutableVersion = MutableStateFlow(0)
    val version: StateFlow<Int> = mutableVersion.asStateFlow()

    private val mutableEvents = MutableSharedFlow<PlaylistEvent>(extraBufferCapacity = EventsBuffer)
    val events: SharedFlow<PlaylistEvent> = mutableEvents.asSharedFlow()

    fun emit(event: PlaylistEvent) {
        mutableEvents.tryEmit(event)
    }

    fun notifyChanged() {
        mutableVersion.update { it + 1 }
    }
}

// What the playlists of the account ask of YouTube Music, only an account that signed in has them
class PlaylistRepository(context: Context) {
    private val appContext = context.applicationContext

    suspend fun isSignedIn(): Boolean = currentSession(appContext).cookies != null

    // The playlists the songs can go to, the list of the library when YouTube Music does not say them
    suspend fun options(videoIds: List<String>): Result<List<PlaylistOption>> {
        val session = currentSession(appContext)
        val options = InnerTubeClient.addToPlaylistOptions(session, videoIds).getOrNull()?.let(::optionsOf).orEmpty()
        if (options.isNotEmpty()) return Result.success(options)

        return InnerTubeClient.browse(session, browseId = LibrarySource.Playlists.browseId).mapCatching { response ->
            LibraryParser.parse(response).items
                .filter { it.kind == HomeItemKind.Playlist && it.id !in FixedPlaylists }
                .map { PlaylistOption(id = it.id, title = it.title, coverUrl = it.thumbnailUrl, line = it.countText, hasSongs = false) }
        }
    }

    suspend fun add(playlistId: String, videoIds: List<String>): Result<Unit> =
        edit(playlistId, videoIds.map { JSONObject().put("action", "ACTION_ADD_VIDEO").put("addedVideoId", it).put("dedupeOption", "DEDUPE_OPTION_DROP_DUPLICATE") })

    // Sends the picture as the cover of a playlist, cut to 16:9 already
    suspend fun setCover(playlistId: String, image: ByteArray): Result<Unit> {
        val session = currentSession(appContext)
        return InnerTubeClient.uploadPlaylistImage(session, image).mapCatching { blobId ->
            val action = JSONObject()
                .put("action", "ACTION_SET_CUSTOM_THUMBNAIL")
                .put("addedCustomThumbnail", JSONObject().put("playlistScottyCoverImage", JSONObject().put("encryptedBlobId", blobId)))
            edit(playlistId, listOf(action)).getOrThrow()
        }
    }

    // Takes the songs out of every place they have in the playlist, which YouTube Music asks by the place of each one
    suspend fun remove(playlistId: String, videoIds: List<String>): Result<Unit> {
        val places = songsOf(playlistId).orEmpty()
        val actions = videoIds.flatMap { videoId ->
            val setVideoIds = places[videoId].orEmpty()
            if (setVideoIds.isEmpty()) {
                listOf(JSONObject().put("action", "ACTION_REMOVE_VIDEO").put("removedVideoId", videoId))
            } else {
                setVideoIds.map { JSONObject().put("action", "ACTION_REMOVE_VIDEO").put("removedVideoId", videoId).put("setVideoId", it) }
            }
        }
        return edit(playlistId, actions)
    }

    // The songs of a playlist with the place each one has in it, kept for a while, which is how the app knows what a playlist has
    suspend fun songsOf(playlistId: String): Map<String, List<String>>? {
        val id = playlistId.removePrefix(PlaylistPagePrefix)
        PlaylistContents.fresh(id)?.let { return it }

        val collections = CollectionRepository(appContext)
        val page = collections.load(CollectionKind.Playlist, id).getOrNull() ?: return null
        var tracks = page.tracks
        var token = page.continuation
        var pages = 0
        while (token != null && pages < MaxPages) {
            val more = collections.more(token).getOrNull() ?: break
            tracks = tracks + more.tracks
            token = more.continuation
            pages++
        }

        return tracks.groupBy({ it.id }, { it.setVideoId }).mapValues { (_, places) -> places.filterNotNull() }.also { PlaylistContents.put(id, it) }
    }

    // Takes out the song from its place in the playlist, or every place of it when the place is not known
    suspend fun removeEntry(playlistId: String, videoId: String, setVideoId: String?): Result<Unit> =
        edit(playlistId, listOf(JSONObject().put("action", "ACTION_REMOVE_VIDEO").put("removedVideoId", videoId).apply { setVideoId?.let { put("setVideoId", it) } }))

    // Changes only what is given, the name, the description and who can find the playlist
    suspend fun update(playlistId: String, title: String?, description: String?, privacy: PlaylistPrivacy?): Result<Unit> {
        val actions = listOfNotNull(
            title?.let { JSONObject().put("action", "ACTION_SET_PLAYLIST_NAME").put("playlistName", it) },
            description?.let { JSONObject().put("action", "ACTION_SET_PLAYLIST_DESCRIPTION").put("playlistDescription", it) },
            privacy?.let { JSONObject().put("action", "ACTION_SET_PLAYLIST_PRIVACY").put("playlistPrivacy", it.status) }
        )
        return if (actions.isEmpty()) Result.success(Unit) else edit(playlistId, actions)
    }

    // The id of the new playlist, empty when the answer does not say it
    suspend fun create(title: String, privacy: PlaylistPrivacy, videoIds: List<String>): Result<String?> =
        InnerTubeClient.createPlaylist(currentSession(appContext), title, privacy.status, videoIds)
            .map { it.stringAt("playlistId") }
            .onSuccess { PlaylistChanges.notifyChanged() }

    suspend fun delete(playlistId: String): Result<Unit> =
        InnerTubeClient.deletePlaylist(currentSession(appContext), playlistId.removePrefix(PlaylistPagePrefix))
            .map { }
            .onSuccess { PlaylistChanges.notifyChanged() }

    private suspend fun edit(playlistId: String, actions: List<JSONObject>): Result<Unit> =
        InnerTubeClient.editPlaylist(currentSession(appContext), playlistId.removePrefix(PlaylistPagePrefix), actions)
            .mapCatching { response -> check(response.optString("status") != "STATUS_FAILED") { "The playlist did not change" } }
            .onSuccess {
                PlaylistContents.invalidate(playlistId.removePrefix(PlaylistPagePrefix))
                PlaylistChanges.notifyChanged()
            }

    // True when the playlist says it has every song asked, in any of the words that YouTube Music uses for it
    private fun hasSongsOf(option: JSONObject): Boolean {
        val contained = option.optString("containsSelectedVideos")
        return contained.equals("ALL", ignoreCase = true) || contained.contains("ALL_", ignoreCase = true) ||
            option.optBoolean("containsVideo") || option.optBoolean("isSelected")
    }

    // The biggest picture kept anywhere in the playlist of the answer, wherever the answer wraps it
    private fun thumbnailOf(node: JSONObject): String? {
        node.keys().forEach { key ->
            val value = node.opt(key)
            val found = when {
                key == "thumbnails" && value is JSONArray -> value.optJSONObject(value.length() - 1)?.optString("url")?.takeIf { it.isNotBlank() }
                value is JSONObject -> thumbnailOf(value)
                value is JSONArray -> (0 until value.length()).firstNotNullOfOrNull { index -> value.optJSONObject(index)?.let(::thumbnailOf) }
                else -> null
            }
            if (found != null) return found
        }
        return null
    }

    // Each playlist of the answer, with the check of YouTube Music when it already has every song
    private fun optionsOf(response: JSONObject): List<PlaylistOption> =
        response.findObjects("playlistAddToOptionRenderer").mapNotNull { option ->
            val id = option.stringAt("playlistId") ?: return@mapNotNull null
            val title = option.stringAt("title", "simpleText")
                ?: option.arrayAt("title", "runs").objects().joinToString("") { it.optString("text") }.trim().takeIf { it.isNotEmpty() }
                ?: return@mapNotNull null

            PlaylistOption(
                id = id,
                title = title,
                coverUrl = thumbnailOf(option),
                line = option.stringAt("shortBylineText", "simpleText"),
                hasSongs = hasSongsOf(option)
            )
        }.filter { it.id !in FixedPlaylists }
}

// How many pages of a playlist are read to know its songs, and how many changes wait for a list that is slow to take them
private const val MaxPages = 10
private const val EventsBuffer = 16

// The lists of YouTube Music that are not playlists one can save to, the liked songs and the episodes for later
private val FixedPlaylists = setOf("LM", "SE", "WL")

package com.wavvy.app.core.download

// Android context and files
import android.content.Context
import androidx.compose.runtime.compositionLocalOf
import androidx.core.net.toUri
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.ResolvingDataSource
import androidx.media3.datasource.cache.NoOpCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadManager
import androidx.media3.exoplayer.offline.DownloadNotificationHelper
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.offline.DownloadService
// Files, JSON and streams
import java.io.File
import java.io.IOException
import java.util.concurrent.Executor
import okhttp3.OkHttpClient
import org.json.JSONObject
// Coroutines and reactive flows
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
// Project resources
import com.wavvy.app.core.playback.PlayableTrack
import com.wavvy.app.features.playlist.data.PlaylistRepository
import com.wavvy.app.core.playback.StreamResolver
import com.wavvy.app.R

// Where a download is, as the lists and the menus tell it
enum class DownloadPhase { Queued, Downloading, Completed, Failed }

// Where a song was downloaded from, the playlist, the album or the podcast, which groups it in the list of the downloads
data class DownloadFolder(val id: String, val title: String, val coverUrl: String?)

// The folder of the page that is on screen, so the song that is downloaded from its menu goes to it
val LocalDownloadFolder = compositionLocalOf<DownloadFolder?> { null }

// A song that was asked to be downloaded, with what is needed to list it without the internet
data class DownloadItem(
    val id: String,
    val title: String,
    val artist: String?,
    val artworkUrl: String?,
    val durationMs: Long,
    val phase: DownloadPhase,
    val percent: Int,
    val bytes: Long,
    val startedAt: Long,
    val folderId: String? = null,
    val folderTitle: String? = null,
    val folderCover: String? = null
)

// The downloads of the app, kept in files of the app itself, on the Media3 manager that resumes what was cut and tells the progress
// The player reads the same cache first, so a song that was downloaded plays without asking for a link and without using the internet
@OptIn(UnstableApi::class)
object Downloads {
    @Volatile
    private var appContext: Context? = null

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutableItems = MutableStateFlow<Map<String, DownloadItem>>(emptyMap())
    val items: StateFlow<Map<String, DownloadItem>> = mutableItems.asStateFlow()

    private val databaseProvider by lazy { StandaloneDatabaseProvider(requireContext()) }

    // The files of the downloads, never removed by the system and never cleaned by the app unless the user asks
    val cache: SimpleCache by lazy { SimpleCache(File(requireContext().filesDir, DirectoryName), NoOpCacheEvictor(), databaseProvider) }

    // Finds the link of each song when its download starts, in the best quality there is and without parts, which is what a whole file needs
    private val upstream = ResolvingDataSource.Factory(OkHttpDataSource.Factory(OkHttpClient())) { dataSpec ->
        val mediaId = dataSpec.key ?: throw IOException("No media id")
        val stream = runBlocking(Dispatchers.IO) { StreamResolver.resolve(mediaId, forDownload = true) }
            .getOrElse { throw IOException("Could not resolve the stream", it) }

        // YouTube slows down a request without an end to the speed of the playing, one that asks for the whole file goes at full speed
        val length = stream.contentLengthBytes
        val url = if (length != null && "&range=" !in stream.url) "${stream.url}&range=0-${length - 1}" else stream.url
        dataSpec.withUri(url.toUri()).withRequestHeaders(dataSpec.httpRequestHeaders + stream.headers)
    }

    val manager: DownloadManager by lazy {
        DownloadManager(requireContext(), databaseProvider, cache, upstream, Executor(Runnable::run)).apply {
            maxParallelDownloads = MaxParallel
            addListener(object : DownloadManager.Listener {
                override fun onDownloadChanged(downloadManager: DownloadManager, download: Download, finalException: Exception?) {
                    put(download)
                }

                override fun onDownloadRemoved(downloadManager: DownloadManager, download: Download) {
                    mutableItems.update { it - download.request.id }
                }
            })

            // What was downloaded before the app was opened, and the progress of the ones running, which the manager does not tell by itself
            downloadIndex.getDownloads().use { cursor ->
                while (cursor.moveToNext()) put(cursor.download)
            }
            scope.launch {
                while (true) {
                    delay(ProgressMillis)
                    currentDownloads.forEach(::put)
                }
            }
        }
    }

    // Starts the manager and the cache when the app asks for the first time, so nothing opens before a download or a song is asked
    fun initialize(context: Context) {
        if (appContext == null) appContext = context.applicationContext
        manager
    }

    fun notificationHelper(context: Context): DownloadNotificationHelper = DownloadNotificationHelper(context, ChannelId)

    // The name of a download as the notification says it
    fun titleOf(download: Download): String = JSONObject(String(download.request.data)).optString(TitleKey)

    // Asks a song to be downloaded, it shows as queued at once
    fun enqueue(context: Context, track: PlayableTrack, folder: DownloadFolder? = null) {
        initialize(context)

        // A song that is here or on its way also goes to the folder of the page, and the cover of the folder is kept, whatever else is done
        folder?.let { DownloadFolders.add(context, track.id, listOf(it)) }
        scope.launch { DownloadArt.ensure(context, folder?.coverUrl) }

        val current = mutableItems.value[track.id]
        if (current?.phase == DownloadPhase.Completed || current?.phase == DownloadPhase.Downloading) return

        val data = JSONObject()
            .put(TitleKey, track.title)
            .put(ArtistKey, track.artist)
            .put(ArtworkKey, track.artworkUrl)
            .put(DurationKey, track.durationMs)
            .apply {
                folder?.let {
                    put(FolderIdKey, it.id)
                    put(FolderTitleKey, it.title)
                    put(FolderCoverKey, it.coverUrl)
                }
            }
        val request = DownloadRequest.Builder(track.id, track.id.toUri())
            .setCustomCacheKey(track.id)
            .setData(data.toString().toByteArray())
            .build()

        mutableItems.update { it + (track.id to track.toQueued(folder)) }

        // The cover of the song is kept with it, and the playlists that have the song are looked for
        scope.launch {
            DownloadArt.ensure(context, track.artworkUrl)
            fileInPlaylists(context, listOf(track.id))
        }
        DownloadService.sendAddDownload(context, WavvyDownloadService::class.java, request, false)
    }

    // Asks several songs at once, the ones that are here or on their way are left as they are
    fun enqueueAll(context: Context, tracks: List<PlayableTrack>, folder: DownloadFolder? = null) {
        tracks.forEach { enqueue(context, it, folder) }
    }

    // Puts the songs in the folders of the playlists of the account that have them, once for a whole group so the playlists are read only once
    suspend fun fileInPlaylists(context: Context, ids: List<String>) {
        PlaylistRepository(context).playlistsWith(ids).forEach { (songId, folders) ->
            DownloadFolders.add(context, songId, folders)
            folders.forEach { DownloadArt.ensure(context, it.coverUrl) }
        }
    }

    // Looks for the playlists of the songs that were downloaded and are in no folder yet, only once for each one in the run of the app
    suspend fun fileUnfiled(context: Context) {
        initialize(context)
        val filed = DownloadFolders.map.value
        val ids = mutableItems.value.values.filter { it.phase == DownloadPhase.Completed && filed[it.id].isNullOrEmpty() && it.id !in looked }.map { it.id }
        if (ids.isEmpty()) return

        looked += ids
        fileInPlaylists(context, ids)
    }

    private val looked = mutableSetOf<String>()

    // Takes several songs out at once
    fun removeAll(context: Context, ids: List<String>) {
        ids.forEach { remove(context, it) }
    }

    // Takes a song out, whether it is still coming or already here, which frees its space
    fun remove(context: Context, id: String) {
        initialize(context)
        mutableItems.update { it - id }
        DownloadFolders.remove(context, listOf(id))
        DownloadService.sendRemoveDownload(context, WavvyDownloadService::class.java, id, false)

        // The pictures that no download uses any more go away
        scope.launch { DownloadArt.prune(context, picturesInUse()) }
    }

    // The pictures of what is downloaded, which are the ones that stay
    private fun picturesInUse(): Set<String> =
        mutableItems.value.values.flatMap { listOfNotNull(it.artworkUrl, it.folderCover) }.toSet() +
            DownloadFolders.map.value.values.flatten().mapNotNull { it.coverUrl }

    // Saves the pictures of the downloads that were made before the pictures were kept
    suspend fun backfillArt(context: Context) {
        picturesInUse().forEach { DownloadArt.ensure(context, it) }
    }

    private fun put(download: Download) {
        val data = runCatching { JSONObject(String(download.request.data)) }.getOrDefault(JSONObject())

        // A song downloaded when the folder was kept with the request goes to the file of the folders
        data.optString(FolderIdKey).takeIf { it.isNotEmpty() && it != NullText }?.let { id ->
            DownloadFolders.add(requireContext(), download.request.id, listOf(DownloadFolder(id, data.optString(FolderTitleKey), data.optString(FolderCoverKey).takeIf { it.isNotEmpty() && it != NullText })))
        }
        val phase = when (download.state) {
            Download.STATE_COMPLETED -> DownloadPhase.Completed
            Download.STATE_DOWNLOADING -> DownloadPhase.Downloading
            Download.STATE_FAILED -> DownloadPhase.Failed
            Download.STATE_REMOVING -> return
            else -> DownloadPhase.Queued
        }

        mutableItems.update {
            it + (download.request.id to DownloadItem(
                id = download.request.id,
                title = data.optString(TitleKey),
                artist = data.optString(ArtistKey).takeIf { artist -> artist.isNotEmpty() && artist != NullText },
                artworkUrl = data.optString(ArtworkKey).takeIf { url -> url.isNotEmpty() && url != NullText },
                durationMs = data.optLong(DurationKey),
                phase = phase,
                percent = download.percentDownloaded.toInt().coerceIn(0, MaxPercent),
                bytes = download.bytesDownloaded,
                startedAt = download.startTimeMs,
                folderId = data.optString(FolderIdKey).takeIf { id -> id.isNotEmpty() && id != NullText },
                folderTitle = data.optString(FolderTitleKey).takeIf { title -> title.isNotEmpty() && title != NullText },
                folderCover = data.optString(FolderCoverKey).takeIf { cover -> cover.isNotEmpty() && cover != NullText }
            ))
        }
    }

    private fun PlayableTrack.toQueued(folder: DownloadFolder?) =
        DownloadItem(id, title, artist, artworkUrl, durationMs, DownloadPhase.Queued, 0, 0L, System.currentTimeMillis(), folder?.id, folder?.title, folder?.coverUrl)

    private fun requireContext(): Context = requireNotNull(appContext) { "Downloads is not initialized" }

    // The folder of the files, the channel of the notification, the keys of what is kept with each request and the pace of the progress
    private const val DirectoryName = "downloads"
    const val ChannelId = "downloads"
    private const val TitleKey = "title"
    private const val ArtistKey = "artist"
    private const val ArtworkKey = "artwork"
    private const val DurationKey = "duration"
    private const val FolderIdKey = "folderId"
    private const val FolderTitleKey = "folderTitle"
    private const val FolderCoverKey = "folderCover"
    private const val NullText = "null"
    private const val MaxParallel = 2
    private const val ProgressMillis = 1000L
    private const val MaxPercent = 100
}

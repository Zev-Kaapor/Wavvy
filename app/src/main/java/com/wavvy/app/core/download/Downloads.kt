package com.wavvy.app.core.download

// Android context and files
import java.util.concurrent.ConcurrentHashMap
import androidx.media3.datasource.HttpDataSource
import android.annotation.SuppressLint
import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.compositionLocalOf
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.net.toUri
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.cache.NoOpCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadManager
import androidx.media3.exoplayer.offline.DownloadNotificationHelper
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.offline.DownloadService
import androidx.media3.exoplayer.scheduler.Requirements
// Files, JSON and streams
import java.io.File
import java.io.IOException
import java.util.concurrent.Executor
import com.wavvy.app.core.network.WavvyProxy
import org.json.JSONObject
// Coroutines and reactive flows
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
// Project resources
import com.wavvy.app.core.designsystem.components.Toasts
import com.wavvy.app.core.playback.PlayableTrack
import com.wavvy.app.core.playback.ResolvedStream
import com.wavvy.app.features.notifications.data.ReleaseNotifier
import com.wavvy.app.features.playlist.data.PlaylistRepository
import com.wavvy.app.features.settings.data.DownloadSettings
import com.wavvy.app.features.settings.data.SettingsStore
import com.wavvy.app.core.playback.StreamResolver
import com.wavvy.app.R

// Where a download is, as the lists and the menus tell it
enum class DownloadPhase { Queued, Downloading, Paused, Completed, Failed }

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

    // Asks the main screen to ask for the permission of the notifications, which the progress of the downloads is shown in on Android 13 and newer
    private val mutablePermissionAsks = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val permissionAsks: SharedFlow<Unit> = mutablePermissionAsks.asSharedFlow()

    @Volatile
    private var isAskingPermission = false

    fun permissionAnswered() {
        isAskingPermission = false
    }

    // Asked once for a group of songs, when the permission is not given yet and a screen is there to ask it, the songs download whether it is allowed or not
    private fun askForNotifications(context: Context) {
        if (isAskingPermission || ReleaseNotifier.canPost(context) || mutablePermissionAsks.subscriptionCount.value == 0) return
        isAskingPermission = true
        mutablePermissionAsks.tryEmit(Unit)
    }

    // True while a song is all downloaded, it only moves when this very song changes, so a list does not draw again with the progress of the others
    fun isCompleted(id: String): Flow<Boolean> = items.map { it[id]?.phase == DownloadPhase.Completed }.distinctUntilChanged()

    private val databaseProvider by lazy { StandaloneDatabaseProvider(requireContext()) }

    // The files of the downloads, never removed by the system and never cleaned by the app unless the user asks
    val cache: SimpleCache by lazy { SimpleCache(File(requireContext().filesDir, DirectoryName), NoOpCacheEvictor(), databaseProvider) }

    // The link of each song, found when its download starts and kept until it is about to expire, since each piece of a file asks for it
    private class DownloadLink(val stream: ResolvedStream, val validUntil: Long)
    private val links = ConcurrentHashMap<String, DownloadLink>()

    private fun linkOf(mediaId: String): ResolvedStream {
        val now = System.currentTimeMillis()
        links[mediaId]?.takeIf { it.validUntil > now }?.let { return it.stream }

        // In the best quality there is and without parts, which is what a whole file needs
        val stream = runBlocking(Dispatchers.IO) { StreamResolver.resolve(mediaId, forDownload = true, downloadQuality = settings.quality.audio) }
            .getOrElse { throw IOException("Could not resolve the stream", it) }
        lastClient[mediaId] = stream.clientName
        links[mediaId] = DownloadLink(stream, now + stream.expiresInSeconds * MillisPerSecond / LinkLifeDivisor)
        return stream
    }

    // The file is read in pieces, YouTube slows down a request that asks for a long file to the speed of the playing
    private val upstream = DataSource.Factory { PieceDownloadDataSource(OkHttpDataSource.Factory(WavvyProxy.client).createDataSource(), ::linkOf) }

    val manager: DownloadManager by lazy {
        DownloadManager(requireContext(), databaseProvider, cache, upstream, Executor(Runnable::run)).apply {
            maxParallelDownloads = MaxParallel
            addListener(object : DownloadManager.Listener {
                override fun onDownloadChanged(downloadManager: DownloadManager, download: Download, finalException: Exception?) {
                    put(download)
                    when (download.state) {
                        Download.STATE_FAILED -> onFailed(download.request.id, finalException)
                        Download.STATE_COMPLETED -> autoRetries.remove(download.request.id)
                    }
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
            scope.launch {
                delay(StartRetryMillis)
                restart(mutableItems.value.values.filter { it.phase == DownloadPhase.Failed }.map { it.id })
            }
        }
    }

    // Starts the manager and the cache when the app asks for the first time, so nothing opens before a download or a song is asked
    fun initialize(context: Context) {
        if (appContext == null) appContext = context.applicationContext
        manager
        followSettings(context)
        followRun(context)
    }

    // What the user chose in the settings of the downloads, kept up to date while the app runs
    @Volatile
    private var settings = DownloadSettings()
    private var isFollowing = false

    // Reads the settings once and then each time they change, Wi-Fi only becomes a requirement of the manager, the rest is read when a song is asked
    @Synchronized
    private fun followSettings(context: Context) {
        if (isFollowing) return
        isFollowing = true

        val store = SettingsStore(context)
        scope.launch {
            store.downloads.collect { chosen ->
                settings = chosen
                Handler(Looper.getMainLooper()).post {
                    manager.requirements = Requirements(if (chosen.wifiOnly) Requirements.NETWORK_UNMETERED else Requirements.NETWORK)
                }
            }
        }
    }

    fun notificationHelper(context: Context): DownloadNotificationHelper = DownloadNotificationHelper(context, ChannelId)

    // How far the run of downloads that is going on has come, the songs that are done of all of them, how much of all of them is done, and whether what is left is held
    class RunProgress(val done: Int, val total: Int, val percent: Int, val isPaused: Boolean)

    // The songs of the run, so the notification can say how many of them are done
    private val runSongs: MutableSet<String> = ConcurrentHashMap.newKeySet()

    // Null when nothing of the run is coming or held, a song that is asked then starts a new run
    fun runProgress(): RunProgress? {
        val items = mutableItems.value
        val songs = runSongs.mapNotNull { items[it] }
        val isComing = songs.any { it.phase == DownloadPhase.Queued || it.phase == DownloadPhase.Downloading }
        val isHeld = songs.any { it.phase == DownloadPhase.Paused }
        if (!isComing && !isHeld) return null

        val percent = songs.sumOf { if (it.phase == DownloadPhase.Completed) MaxPercent else it.percent } / songs.size
        return RunProgress(songs.count { it.phase == DownloadPhase.Completed }, songs.size, percent, isPaused = !isComing)
    }

    // The notification of the run, how many songs are done of all of them, with the bar and the percent, and it says when the run is held
    fun runNotification(context: Context, progress: RunProgress): Notification {
        val launch = context.packageManager.getLaunchIntentForPackage(context.packageName)
        val opens = PendingIntent.getActivity(context, 0, launch, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val title = context.getString(if (progress.isPaused) R.string.download_notification_paused else R.string.download_notification_title, progress.done, progress.total)

        return NotificationCompat.Builder(context, ChannelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(context.getString(R.string.download_notification_percent, progress.percent))
            .setProgress(MaxPercent, progress.percent, false)
            .setOngoing(!progress.isPaused)
            .setOnlyAlertOnce(true)
            .setContentIntent(opens)
            .build()
    }

    // While every song of the run is held the service has nothing to show and goes away, so the notification that says it is held is posted here
    private var isPausedShown = false
    private var isFollowingRun = false

    @Synchronized
    private fun followRun(context: Context) {
        if (isFollowingRun) return
        isFollowingRun = true

        scope.launch {
            items.collect { postPaused(context) }
        }
    }

    // The permission is checked by canPost before anything is shown
    @SuppressLint("MissingPermission")
    private fun postPaused(context: Context) {
        val manager = NotificationManagerCompat.from(context)
        val progress = runProgress()?.takeIf { it.isPaused }
        if (progress == null) {
            if (isPausedShown) {
                manager.cancel(PausedNotificationId)
                isPausedShown = false
            }
            return
        }
        if (!ReleaseNotifier.canPost(context)) return

        manager.createNotificationChannel(
            NotificationChannelCompat.Builder(ChannelId, NotificationManagerCompat.IMPORTANCE_LOW)
                .setName(context.getString(R.string.download_channel))
                .build()
        )
        manager.notify(PausedNotificationId, runNotification(context, progress))
        isPausedShown = true
    }

    // The name of a download as the notification says it
    fun titleOf(download: Download): String = JSONObject(String(download.request.data)).optString(TitleKey)

    // Asks a song to be downloaded, it shows as queued at once
    fun enqueue(context: Context, track: PlayableTrack, folder: DownloadFolder? = null) {
        initialize(context)
        askForNotifications(context)

        // A song that is here or on its way also goes to the folder of the page, and the cover of the folder is kept, whatever else is done
        folder?.let { DownloadFolders.add(context, track.id, listOf(it)) }
        scope.launch { DownloadArt.ensure(context, folder?.coverUrl) }

        val current = mutableItems.value[track.id]
        if (current?.phase == DownloadPhase.Completed || current?.phase == DownloadPhase.Downloading) return

        // No more are started once the space the user allowed is used
        val limit = settings.limit.bytes
        if (limit != null && cache.cacheSpace >= limit) {
            warnLimit(context)
            return
        }

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

        // A song asked when nothing is coming or held starts a new run
        if (runProgress() == null) runSongs.clear()
        runSongs += track.id
        mutableItems.update { it + (track.id to track.toQueued(folder)) }

        // The cover of the song is kept with it, and the playlists that have the song are looked for
        scope.launch {
            DownloadArt.ensure(context, track.artworkUrl)
            fileInPlaylists(context, listOf(track.id))
        }
        DownloadService.sendAddDownload(context, WavvyDownloadService::class.java, request, false)
    }

    // The client that gave the link of each song, and how many times each one was tried again by itself
    private val lastClient = ConcurrentHashMap<String, String>()
    private val autoRetries = ConcurrentHashMap<String, Int>()

    // A download failed, a link that YouTube refused leaves out the client that gave it, and then it is tried again by itself a few times, a little later each time
    private fun onFailed(id: String, failure: Exception?) {
        if (isRefusedLink(failure)) {
            links.remove(id)
            lastClient[id]?.let { StreamResolver.markClientFailed(id, it) }
            scope.launch {
                if (runCatching { StreamResolver.refreshAfterStreamRejection() }.getOrDefault(false)) StreamResolver.clearClientFailures()
            }
        }

        val tries = autoRetries[id] ?: 0
        if (tries >= MaxAutoRetries) return
        autoRetries[id] = tries + 1
        scope.launch {
            delay(RetryDelayMillis * (tries + 1))
            restart(listOf(id))
        }
    }

    // True when YouTube answered that the link is not good any more, as it does when the link expired
    private fun isRefusedLink(failure: Throwable?): Boolean {
        var cause = failure
        while (cause != null) {
            if (cause is HttpDataSource.InvalidResponseCodeException && (cause.responseCode == HttpForbidden || cause.responseCode == HttpGone)) return true
            cause = cause.cause
        }
        return false
    }

    // The user asks the failed downloads to go again, which gives each one its tries back
    fun retry(context: Context, ids: List<String>) {
        initialize(context)
        ids.forEach { autoRetries.remove(it) }
        restart(ids)
    }

    fun retryFailed(context: Context) {
        retry(context, mutableItems.value.values.filter { it.phase == DownloadPhase.Failed }.map { it.id })
    }

    // Holds the songs that are coming, they keep what they have downloaded and nothing is removed, so the internet is left to the rest
    fun pause(context: Context, ids: List<String>) {
        initialize(context)
        val coming = ids.filter { mutableItems.value[it]?.phase.let { phase -> phase == DownloadPhase.Queued || phase == DownloadPhase.Downloading } }
        setPhase(coming, DownloadPhase.Paused)
        coming.forEach { DownloadService.sendSetStopReason(context, WavvyDownloadService::class.java, it, PausedReason, false) }
    }

    // Lets the held songs go on from where they stopped, they show as waiting at once
    fun resume(context: Context, ids: List<String>) {
        initialize(context)
        val held = ids.filter { mutableItems.value[it]?.phase == DownloadPhase.Paused }
        setPhase(held, DownloadPhase.Queued)
        held.forEach { DownloadService.sendSetStopReason(context, WavvyDownloadService::class.java, it, Download.STOP_REASON_NONE, false) }
    }

    // The lists show the new phase of the songs at once, before the manager tells it
    private fun setPhase(ids: List<String>, phase: DownloadPhase) {
        mutableItems.update { current -> current + ids.mapNotNull { id -> current[id]?.let { id to it.copy(phase = phase) } } }
    }

    // Asks the manager again for the downloads that failed, from the request that is kept for each, they show as waiting at once
    private fun restart(ids: List<String>) {
        if (ids.isEmpty()) return

        scope.launch {
            ids.forEach { id ->
                val download = manager.downloadIndex.getDownload(id) ?: return@forEach
                if (download.state != Download.STATE_FAILED) return@forEach

                mutableItems.update { current -> current[id]?.let { current + (id to it.copy(phase = DownloadPhase.Queued, percent = 0)) } ?: current }
                DownloadService.sendAddDownload(requireContext(), WavvyDownloadService::class.java, download.request, false)
            }
        }
    }

    // Tells the space is full, once for a whole group of songs
    private var lastLimitWarning = 0L

    private fun warnLimit(context: Context) {
        val now = System.currentTimeMillis()
        if (now - lastLimitWarning < LimitWarningMillis) return
        lastLimitWarning = now
        Toasts.show(context.getString(R.string.download_limit_reached), isError = true)
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
            Download.STATE_STOPPED -> DownloadPhase.Paused
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
    private const val PausedNotificationId = 3
    private const val TitleKey = "title"
    private const val ArtistKey = "artist"
    private const val ArtworkKey = "artwork"
    private const val DurationKey = "duration"
    private const val FolderIdKey = "folderId"
    private const val FolderTitleKey = "folderTitle"
    private const val FolderCoverKey = "folderCover"
    private const val NullText = "null"
    private const val MaxParallel = 2
    private const val PausedReason = 1
    private const val ProgressMillis = 1000L
    private const val MillisPerSecond = 1000L

    // A link is kept for half of what it lives, so a piece never asks with one that is about to expire
    private const val LinkLifeDivisor = 2
    private const val LimitWarningMillis = 3000L

    // How many times a download that failed is tried again by itself, how long it waits for the first one and how long after the app opens, and the answers that say the link is no good
    private const val MaxAutoRetries = 3
    private const val RetryDelayMillis = 5000L
    private const val StartRetryMillis = 4000L
    private const val HttpForbidden = 403
    private const val HttpGone = 410
    private const val MaxPercent = 100
}

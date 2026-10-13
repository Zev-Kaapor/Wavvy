package com.wavvy.app.core.navigation

// Android context and links
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.core.net.toUri
// Reactive flows
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
// Project resources
import com.wavvy.app.R
import com.wavvy.app.core.backup.BackupInbox
import com.wavvy.app.core.discord.DiscordAuth
import com.wavvy.app.core.discord.DiscordHost
import com.wavvy.app.core.discord.DiscordScheme
import com.wavvy.app.core.designsystem.components.Toasts
import com.wavvy.app.core.playback.PlayerConnection
import com.wavvy.app.core.playback.RadioQueue

// What a file that another app hands over starts with
private const val ContentScheme = "content"
private const val FileScheme = "file"

// Hosts of the links the app opens, the short one of YouTube is only for videos
private const val ShortHost = "youtu.be"
private val LongHosts = setOf("music.youtube.com", "www.youtube.com", "youtube.com", "m.youtube.com")

// Parts of the links, the pages of a video and of a list, and the prefixes of the ids of the pages in /browse
private const val WatchPath = "watch"
private const val PlaylistPath = "playlist"
private const val ChannelPath = "channel"
private const val BrowsePath = "browse"
private const val VideoQuery = "v"
private const val ListQuery = "list"
private const val AlbumPrefix = "MPRE"
private const val ArtistPrefix = "UC"
private const val PodcastPrefix = "MPSP"
private const val PlaylistPrefix = "VL"

// The page or the song a link leads to
private sealed interface LinkTarget {
    data class Song(val id: String) : LinkTarget
    data class Playlist(val id: String) : LinkTarget
    data class Album(val id: String) : LinkTarget
    data class Artist(val id: String) : LinkTarget
    data class Podcast(val id: String) : LinkTarget
}

// Opens the links of YouTube and YouTube Music that the system hands to the app, once the main screen is there to show them
object LinkOpener {
    private val mutablePending = MutableStateFlow<Uri?>(null)

    // The link that waits for the main screen, it stays until it is opened
    val pending: StateFlow<Uri?> = mutablePending.asStateFlow()

    // Keeps the link of an intent the system sent to open the app, any other intent is ignored
    fun offer(intent: Intent?) {
        if (intent?.action != Intent.ACTION_VIEW) return
        val data = intent.data ?: return

        // The way back of the login with Discord is for the login, not for the pages
        if (data.scheme == DiscordScheme && data.host == DiscordHost) {
            DiscordAuth.offerCallback(data)
            return
        }

        // A file of the device or of another app is a backup, a link of the web is for the pages
        if (data.scheme == ContentScheme || data.scheme == FileScheme) BackupInbox.offer(data) else mutablePending.value = data
    }

    // Opens the song or the page of a link, a link the app does not know says so
    suspend fun open(context: Context, link: Uri) {
        // The link is taken away only when the work is done, the screen that waits for it starts again when it changes
        val isOpened = try {
            when (val target = targetOf(link)) {
                is LinkTarget.Song -> playSong(context, target.id)
                is LinkTarget.Playlist -> ItemNavigator.openPlaylist(target.id)
                is LinkTarget.Album -> ItemNavigator.openAlbum(target.id)
                is LinkTarget.Artist -> ItemNavigator.openArtist(target.id)
                is LinkTarget.Podcast -> ItemNavigator.openPodcast(target.id)
                null -> false
            }
        } finally {
            mutablePending.value = null
        }
        if (!isOpened) Toasts.show(context.getString(R.string.link_error), isError = true)
    }

    // Opens the settings of the system where the user lets the app open the links, a phone that has no such screen opens the page of the app
    fun openSystemSettings(context: Context) {
        val address = "package:${context.packageName}".toUri()
        val opens = listOf(Settings.ACTION_APP_OPEN_BY_DEFAULT_SETTINGS, Settings.ACTION_APPLICATION_DETAILS_SETTINGS).any { action ->
            runCatching { context.startActivity(Intent(action, address).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }.isSuccess
        }
        if (!opens) Toasts.show(context.getString(R.string.settings_links_error), isError = true)
    }

    // Plays a song of a link, the first of its radio is the song itself
    private suspend fun playSong(context: Context, id: String): Boolean {
        val page = RadioQueue.start(context, id).getOrNull() ?: return false
        val track = page.tracks.firstOrNull { it.id == id } ?: page.tracks.firstOrNull() ?: return false

        PlayerConnection.play(context, track)
        return true
    }

    // What a link leads to, null for the ones the app does not open
    private fun targetOf(link: Uri): LinkTarget? {
        val host = link.host?.lowercase()
        val segments = link.pathSegments

        if (host == ShortHost) return segments.firstOrNull()?.let { LinkTarget.Song(it) }
        if (host !in LongHosts) return null

        return when (segments.firstOrNull()) {
            WatchPath -> link.getQueryParameter(VideoQuery)?.let { LinkTarget.Song(it) }
                ?: link.getQueryParameter(ListQuery)?.let { LinkTarget.Playlist(it) }
            PlaylistPath -> link.getQueryParameter(ListQuery)?.let { LinkTarget.Playlist(it) }
            ChannelPath -> segments.getOrNull(1)?.let { LinkTarget.Artist(it) }
            BrowsePath -> segments.getOrNull(1)?.let { id ->
                when {
                    id.startsWith(AlbumPrefix) -> LinkTarget.Album(id)
                    id.startsWith(ArtistPrefix) -> LinkTarget.Artist(id)
                    id.startsWith(PodcastPrefix) -> LinkTarget.Podcast(id)
                    id.startsWith(PlaylistPrefix) -> LinkTarget.Playlist(id.removePrefix(PlaylistPrefix))
                    else -> null
                }
            }
            else -> null
        }
    }
}

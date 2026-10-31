package com.wavvy.app.features.menu

// Android and Compose layouts
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
// Material 3 components
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
// Compose state and runtime
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
// UI utilities
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
// Coroutines and reactive flows
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
// Project resources
import com.wavvy.app.R
import com.wavvy.app.features.like.data.Likes
import com.wavvy.app.core.designsystem.components.LocalSheetClose
import com.wavvy.app.core.designsystem.components.WavvySheet
import com.wavvy.app.core.designsystem.icons.WavvyIcons
import com.wavvy.app.core.designsystem.theme.WavvyTheme
import com.wavvy.app.core.history.PlayHistory
import com.wavvy.app.core.innertube.InnerTubeClient
import com.wavvy.app.core.innertube.currentSession
import com.wavvy.app.core.playback.SleepTimer
import com.wavvy.app.core.playback.PlayerConnection
import com.wavvy.app.core.playback.RadioQueue
import com.wavvy.app.core.designsystem.components.OverlaySheet
import com.wavvy.app.features.collection.ui.showCollectionMenu
import com.wavvy.app.features.player.ui.components.ArtistPickerSheet
import com.wavvy.app.features.podcast.ui.PodcastMenu
import com.wavvy.app.features.podcast.data.EpisodesLater
import com.wavvy.app.features.home.data.HomeItem
import com.wavvy.app.features.home.data.HomeItemKind
import com.wavvy.app.features.home.ui.toPlayableTrack
import com.wavvy.app.core.device.isDeviceTrack
import com.wavvy.app.core.radio.isRadioTrack
import com.wavvy.app.core.download.DownloadFolder
import com.wavvy.app.core.download.DownloadPhase
import com.wavvy.app.core.download.Downloads
import com.wavvy.app.core.navigation.ItemNavigator
import com.wavvy.app.features.home.ui.components.itemSubtitle
import com.wavvy.app.features.like.ui.rememberLikeState
import com.wavvy.app.features.player.ui.components.SleepTimerSheet
import com.wavvy.app.features.player.ui.components.formatTime
import com.wavvy.app.features.playlist.data.PlaylistActions
import com.wavvy.app.features.playlist.data.PlaylistChanges
import com.wavvy.app.features.playlist.data.PlaylistEvent
import com.wavvy.app.features.playlist.data.PlaylistRepository
import com.wavvy.app.features.playlist.ui.SaveToPlaylist
import com.wavvy.app.features.player.ui.components.TrackCover
import com.wavvy.app.features.player.ui.components.shareSong
import com.wavvy.app.core.designsystem.components.Toasts

// The playlist of the account a list of songs belongs to, given by its page so the menu of a song can take it out
val LocalEditablePlaylist = compositionLocalOf<String?> { null }

// How often the time left of the sleep timer is read, and the milliseconds in a second
private const val SleepTickMillis = 1000L
private const val MillisPerSecond = 1000L

// The song whose menu is open, held outside the screens so any card can open it and the sheet can cover the whole window
object ItemMenu {
    private val mutableItem = MutableStateFlow<HomeItem?>(null)
    val item: StateFlow<HomeItem?> = mutableItem.asStateFlow()

    // The playlist of the account the song was opened from, which it can be taken out of
    private val mutablePlaylistId = MutableStateFlow<String?>(null)
    val playlistId: StateFlow<String?> = mutablePlaylistId.asStateFlow()

    // The page the song was opened from, where a download of it is kept
    private val mutableFolder = MutableStateFlow<DownloadFolder?>(null)
    val folder: StateFlow<DownloadFolder?> = mutableFolder.asStateFlow()

    // True when the song was opened from the list of the downloads, where taking its download out takes it out of the list
    private val mutableClosesOnRemoval = MutableStateFlow(false)
    val closesOnDownloadRemoval: StateFlow<Boolean> = mutableClosesOnRemoval.asStateFlow()

    // What takes the song out of the history, set when the song was opened from the history
    private val mutableRemoveFromHistory = MutableStateFlow<(() -> Unit)?>(null)
    val removeFromHistory: StateFlow<(() -> Unit)?> = mutableRemoveFromHistory.asStateFlow()

    // True when the song was opened from the list of the songs of the library, which it can be taken out of
    private val mutableRemovesFromLibrary = MutableStateFlow(false)
    val removesFromLibrary: StateFlow<Boolean> = mutableRemovesFromLibrary.asStateFlow()

    // Opens the menu of any card, a song, an episode, an artist, an album, a playlist or a podcast
    fun show(item: HomeItem, playlistId: String? = null, folder: DownloadFolder? = null, closesOnDownloadRemoval: Boolean = false, removeFromHistory: (() -> Unit)? = null, removesFromLibrary: Boolean = false) {
        mutableRemovesFromLibrary.value = removesFromLibrary
        mutablePlaylistId.value = playlistId
        mutableFolder.value = folder
        mutableClosesOnRemoval.value = closesOnDownloadRemoval
        mutableRemoveFromHistory.value = removeFromHistory
        mutableItem.value = item
    }

    fun dismiss() {
        mutableItem.value = null
    }
}

// Menu of a song of the Home, drawn over the whole screen, play next, add to the queue, save to a playlist, share and pin to the speed dial
@Composable
fun ItemMenuHost() {
    val item by ItemMenu.item.collectAsState()
    val current = item ?: return

    // An artist has its own menu
    if (current.kind == HomeItemKind.Artist) {
        ArtistMenu(current)
        return
    }

    // An album or a playlist has the menu of its page and a podcast the one of its own, which are drawn by their hosts, so this one gives its place to them
    if (current.kind == HomeItemKind.Album || current.kind == HomeItemKind.Playlist || current.kind == HomeItemKind.Podcast) {
        val context = LocalContext.current
        LaunchedEffect(current) {
            ItemMenu.dismiss()
            if (current.kind == HomeItemKind.Podcast) {
                OverlaySheet.show {
                    PodcastMenu(
                        id = current.id,
                        title = current.title,
                        author = current.author ?: current.lineText,
                        coverUrl = current.thumbnailUrl,
                        onSearch = null,
                        onDismiss = OverlaySheet::dismiss
                    )
                }
            } else {
                showCollectionMenu(context, current)
            }
        }
        return
    }
    val track = current.toPlayableTrack() ?: return
    val playlistId by ItemMenu.playlistId.collectAsState()
    val downloadFolder by ItemMenu.folder.collectAsState()
    val closesOnRemoval by ItemMenu.closesOnDownloadRemoval.collectAsState()
    val removeFromHistory by ItemMenu.removeFromHistory.collectAsState()
    val removesFromLibrary by ItemMenu.removesFromLibrary.collectAsState()
    val like = rememberLikeState(track.id)
    val downloads by Downloads.items.collectAsState()

    val context = LocalContext.current
    val resources = LocalResources.current
    val scope = rememberCoroutineScope()
    val pinnedIds by remember { PlayHistory.pinned(context).map { songs -> songs.map { it.id }.toSet() } }.collectAsState(initial = emptySet())
    val isPinned = track.id in pinnedIds

    // The album of a song comes with its links, asked when the menu opens so its name shows under its line and a tap opens it at once
    val links by produceState(RadioQueue.cachedLinks(track.id), track.id) {
        if (value == null && current.kind == HomeItemKind.Song && !isDeviceTrack(track.id)) value = RadioQueue.links(context, track.id).getOrNull()
    }

    // An episode can be kept for later in the account, what is kept is read again when the menu opens, none while signed out
    val isEpisode = current.kind == HomeItemKind.Episode
    val laterIds by EpisodesLater.kept.collectAsState()
    LaunchedEffect(track.id) {
        if (isEpisode) EpisodesLater.refresh(context)
    }

    WavvySheet(onDismiss = ItemMenu::dismiss) {
        // The menu stays open for what changes something in it, and slides out only when the screen goes on to another one
        val closeSheet = LocalSheetClose.current

        // A station is live and has no place in a queue and a file of the phone has no radio and no playlist of the account
        val isRadio = isRadioTrack(track.id)
        val isDevice = isDeviceTrack(track.id)

        // The song, its cover, its name and the three most used options
        val head: @Composable () -> Unit = {
            // The cover, the name and who made it, in the color of the accent, and the like on the end
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = ItemMenuDimens.HeaderVertical)
            ) {
                TrackCover(track = track, size = ItemMenuDimens.HeaderCover, modifier = Modifier.clip(RoundedCornerShape(ItemMenuDimens.HeaderCoverCorner)))
                Column(modifier = Modifier.weight(1f).padding(start = ItemMenuDimens.HeaderGap)) {
                    Text(
                        text = track.title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE)
                    )
                    (track.artist ?: itemSubtitle(current))?.let { line ->
                        Text(
                            text = line,
                            style = MaterialTheme.typography.bodyMedium,
                            color = WavvyTheme.colors.playerAccent,
                            maxLines = 1,
                            modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE)
                        )
                    }
                }
                if (!isDeviceTrack(track.id)) {
                    IconButton(onClick = like.toggle) {
                        Icon(
                            imageVector = if (like.isLiked) WavvyIcons.ThumbUpFilled else WavvyIcons.ThumbUp,
                            contentDescription = stringResource(R.string.player_favorite),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // The three most used options side by side, an episode has them as cards in the order of YouTube Music
            if (!isEpisode && (!isRadio || !isDevice)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(ItemMenuDimens.CardTileGap),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = ItemMenuDimens.CardGroupGap - ItemMenuDimens.CardGap, bottom = ItemMenuDimens.CardGroupGap - ItemMenuDimens.CardGap)
                ) {
                    if (!isRadio) {
                        MenuCardTile(WavvyIcons.PlaylistPlay, stringResource(R.string.queue_play_next), Modifier.weight(1f)) {
                            if (PlayerConnection.playNextTrack(context, track)) Toasts.show(resources.getString(R.string.menu_playing_next))
                        }
                    }
                    if (!isDevice) {
                        MenuCardTile(WavvyIcons.PlaylistAdd, stringResource(R.string.playlist_save), Modifier.weight(1f)) {
                            closeSheet()
                            SaveToPlaylist.show(listOf(track.id))
                        }
                        MenuCardTile(WavvyIcons.Share, stringResource(R.string.player_share), Modifier.weight(1f)) {
                            shareSong(context, track.id)
                        }
                    }
                }
            }
        }

        // The other options, one card under the other
        val options: @Composable () -> Unit = {

            // An episode starts with the episodes for later and the play next, as YouTube Music has them
            if (isEpisode) {
                laterIds?.let { kept ->
                    val inLater = track.id in kept
                    MenuCard(if (inLater) WavvyIcons.Delete else WavvyIcons.AddCircle, stringResource(if (inLater) R.string.menu_later_remove else R.string.menu_later_add)) {
                        PlaylistActions.scope.launch { EpisodesLater.toggle(context, track.id) }
                    }
                }
                MenuCard(WavvyIcons.PlaylistPlay, stringResource(R.string.queue_play_next)) {
                    if (PlayerConnection.playNextTrack(context, track)) Toasts.show(resources.getString(R.string.menu_playing_next))
                }
            }

            // The song plays alone and the queue goes on with its radio
            if (current.kind == HomeItemKind.Song && !isDevice && !isRadio) {
                MenuCard(WavvyIcons.Mix, stringResource(R.string.menu_start_radio)) {
                    closeSheet()
                    PlayerConnection.play(context, track)
                }
            }
            if (!isRadio) {
                MenuCard(WavvyIcons.QueueMusic, stringResource(R.string.menu_add_to_queue)) {
                    if (PlayerConnection.addTrackToQueue(context, track)) Toasts.show(resources.getString(R.string.menu_added_to_queue))
                }
            }
            // The sleep timer, only in the menu of the song that plays, with what it is waiting for when it is on
            val playing by PlayerConnection.track.collectAsState()
            if (playing?.id == track.id) {
                val sleep by SleepTimer.state.collectAsState()
                val left by produceState(initialValue = 0L, sleep) {
                    while (sleep.endsAtMs != null) {
                        value = SleepTimer.leftMs(sleep) ?: 0L
                        delay(SleepTickMillis)
                    }
                }
                MenuCard(
                    WavvyIcons.Timer,
                    when {
                        sleep.atEndOfSong -> stringResource(R.string.menu_sleep_timer_left, stringResource(R.string.sleep_until_end))
                        sleep.endsAtMs != null -> stringResource(R.string.menu_sleep_timer_left, formatTime(left / MillisPerSecond))
                        else -> stringResource(R.string.menu_sleep_timer)
                    }
                ) {
                    closeSheet()
                    SleepTimerSheet.show(context)
                }
            }

            // A file of the phone is not in YouTube Music, so it has no download, no page to share and no pin
            if (!isDevice) {
                // Download, with the percent while it comes, and taking it out once it is here or when it failed
                val download = downloads[track.id]
                val isRunning = download != null && (download.phase == DownloadPhase.Queued || download.phase == DownloadPhase.Downloading)
                MenuCard(
                    if (download?.phase == DownloadPhase.Completed) WavvyIcons.Delete else WavvyIcons.Download,
                    when {
                        download == null -> stringResource(R.string.download_do)
                        isRunning -> stringResource(R.string.download_cancel, download.percent)
                        download.phase == DownloadPhase.Completed -> stringResource(R.string.download_remove)
                        else -> stringResource(R.string.download_retry)
                    }
                ) {
                    if (download == null || download.phase == DownloadPhase.Failed) {
                        Downloads.enqueue(context, track, downloadFolder)
                    } else {
                        Downloads.remove(context, track.id)
                        // The song left the list it was opened from, so the menu leaves with it
                        if (closesOnRemoval) closeSheet()
                    }
                }
                // An episode saves and shares after the download, as YouTube Music has it
                if (isEpisode) {
                    MenuCard(WavvyIcons.PlaylistAdd, stringResource(R.string.playlist_save)) {
                        closeSheet()
                        SaveToPlaylist.show(listOf(track.id))
                    }
                    MenuCard(WavvyIcons.Share, stringResource(R.string.player_share)) {
                        shareSong(context, track.id)
                    }
                }
                playlistId?.let { id ->
                    MenuCard(WavvyIcons.Delete, stringResource(R.string.playlist_remove_song)) {
                        closeSheet()
                        // Kept apart from the menu, which leaves the screen before the answer comes
                        PlaylistChanges.emit(PlaylistEvent.SongRemoved(id, track.id, current.setVideoId))
                        PlaylistActions.scope.launch {
                            val result = PlaylistRepository(context).removeEntry(id, track.id, current.setVideoId)
                            val message = if (result.isSuccess) R.string.playlist_song_removed else R.string.playlist_action_error
                            Toasts.show(resources.getString(message), isError = result.isFailure)
                            // A refusal brings the song back
                            if (result.isFailure) PlaylistChanges.notifyChanged()
                        }
                    }
                }
                // A song opened from the list of the songs of the library can be taken out of it
                if (removesFromLibrary) {
                    MenuCard(WavvyIcons.Delete, stringResource(R.string.library_remove_song)) {
                        closeSheet()
                        Likes.removeAll(context, listOf(current))
                    }
                }
                // A song opened from the history can be taken out of it
                removeFromHistory?.let { remove ->
                    MenuCard(WavvyIcons.Delete, stringResource(R.string.history_remove)) {
                        closeSheet()
                        remove()
                    }
                }
                // The album and the artists of a song come from its links, asked now when they are not at hand, and the menu leaves when the page opens
                if (current.kind == HomeItemKind.Song) {
                    MenuCard(WavvyIcons.Person, stringResource(R.string.menu_go_to_artist), subtitle = track.artist) {
                        scope.launch {
                            val artists = (RadioQueue.cachedLinks(track.id) ?: RadioQueue.links(context, track.id).getOrNull())?.artists.orEmpty().filter { it.second.isNotEmpty() }
                            when {
                                artists.isEmpty() -> Toasts.show(resources.getString(R.string.menu_link_error), isError = true)
                                artists.size == 1 -> {
                                    closeSheet()
                                    ItemNavigator.openArtist(artists.first().second)
                                }
                                // Two or more artists are listed to pick from
                                else -> {
                                    closeSheet()
                                    OverlaySheet.show {
                                        ArtistPickerSheet(
                                            artists = artists,
                                            onArtistClick = { id ->
                                                OverlaySheet.dismiss()
                                                ItemNavigator.openArtist(id)
                                            },
                                            onDismiss = OverlaySheet::dismiss
                                        )
                                    }
                                }
                            }
                        }
                    }
                    MenuCard(WavvyIcons.Album, stringResource(R.string.menu_go_to_album), subtitle = links?.albumName) {
                        scope.launch {
                            val albumId = (RadioQueue.cachedLinks(track.id) ?: RadioQueue.links(context, track.id).getOrNull())?.albumId
                            if (albumId.isNullOrEmpty()) {
                                Toasts.show(resources.getString(R.string.menu_link_error), isError = true)
                            } else {
                                closeSheet()
                                ItemNavigator.openAlbum(albumId)
                            }
                        }
                    }
                }
                current.podcastId?.takeIf { current.kind == HomeItemKind.Episode }?.let { podcastId ->
                    MenuCard(WavvyIcons.Podcasts, stringResource(R.string.menu_go_to_podcast), subtitle = current.author) {
                        closeSheet()
                        ItemNavigator.openPodcast(podcastId)
                    }
                }
                // Who sang, wrote and published the song, in a sheet of its own
                if (current.kind == HomeItemKind.Song && !isRadio) {
                    MenuCard(WavvyIcons.Group, stringResource(R.string.menu_credits)) {
                        closeSheet()
                        // The arrow of the credits brings this menu back
                        OverlaySheet.show {
                            SongCreditsSheet(
                                track = track,
                                onDismiss = OverlaySheet::dismiss,
                                onBack = { ItemMenu.show(current, playlistId, downloadFolder, closesOnRemoval, removeFromHistory, removesFromLibrary) }
                            )
                        }
                    }
                }
                // Tells YouTube Music to recommend less like this, only when it sent the way to say it
                current.notInterestedToken?.let { token ->
                    MenuCard(WavvyIcons.Block, stringResource(R.string.menu_not_interested)) {
                        closeSheet()
                        PlaylistActions.scope.launch {
                            val result = InnerTubeClient.feedback(currentSession(context), listOf(token))
                            val message = if (result.isSuccess) R.string.menu_not_interested_done else R.string.menu_not_interested_error
                            Toasts.show(resources.getString(message), isError = result.isFailure)
                        }
                    }
                }

                // Pinning to the speed dial comes last, a file of the phone is not in YouTube Music and has no pin
                MenuCard(WavvyIcons.Pin, stringResource(if (isPinned) R.string.menu_unpin else R.string.menu_pin)) {
                    scope.launch(Dispatchers.IO) {
                        if (isPinned) {
                            PlayHistory.unpin(context, track.id)
                        } else {
                            PlayHistory.pin(context, track.id, track.title, track.artist, track.artworkUrl, track.durationMs)
                        }
                    }
                }
            }
        }

        // Each option is a rounded card as the old Wavvy drew them, the sheet keeps its own drag
        CardMenuLayout(head = head, options = options)
    }
}

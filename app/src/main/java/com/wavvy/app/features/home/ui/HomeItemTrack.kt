package com.wavvy.app.features.home.ui

// Android permissions and context
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
// Compose state and runtime
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
// Project resources
import com.wavvy.app.core.innertube.resize
import com.wavvy.app.core.navigation.ItemNavigator
import com.wavvy.app.core.playback.PlayableTrack
import com.wavvy.app.core.playback.PlayerConnection
import com.wavvy.app.features.home.data.HomeItem
import com.wavvy.app.features.home.data.HomeItemKind
import com.wavvy.app.features.home.ui.components.HomeDimens
import com.wavvy.app.features.home.ui.components.isVideo

// Milliseconds in a second, for the length of a song in the queue
private const val MillisPerSecond = 1000L

// What a tap on a card does, songs and episodes play and start their radio, albums and playlists open their page, the other cards do nothing until they have pages
// The media notification needs a permission on Android 13 and newer, the song plays whether it is allowed or not
@Composable
fun rememberItemPlayer(): (HomeItem) -> Unit {
    val context = LocalContext.current
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}

    return remember(context) {
        { item ->
            val track = item.toPlayableTrack()
            if (track == null) {
                ItemNavigator.open(item)
            } else {
                if (needsNotificationPermission(context)) notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                PlayerConnection.play(context, track)
            }
        }
    }
}

// Plays a list of songs, from the one at the start place and shuffled or not when asked, the ones that cannot play are left out of the list
@Composable
fun rememberListPlayer(): (List<HomeItem>, Int, Boolean?) -> Unit {
    val context = LocalContext.current
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}

    return remember(context) {
        { items, startIndex, shuffle ->
            val tapped = items.getOrNull(startIndex)?.id
            val tracks = items.mapNotNull { it.toPlayableTrack() }

            if (tracks.isNotEmpty()) {
                if (needsNotificationPermission(context)) notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                PlayerConnection.playAll(context, tracks, tracks.indexOfFirst { it.id == tapped }.coerceAtLeast(0), shuffle)
            }
        }
    }
}

// True on Android 13 and newer while the notifications are not allowed yet
private fun needsNotificationPermission(context: Context): Boolean =
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED

// A song or an episode as a track for the player, empty for the cards that open a page
fun HomeItem.toPlayableTrack(): PlayableTrack? {
    if (kind != HomeItemKind.Song && kind != HomeItemKind.Episode) return null

    return PlayableTrack(
        id = id,
        title = title,
        artist = artists.joinToString(", ").ifEmpty { author.orEmpty() }.ifEmpty { null },
        artworkUrl = thumbnailUrl?.resize(HomeDimens.CoverRequestSize, HomeDimens.CoverRequestSize),
        durationMs = durationSeconds?.let { it * MillisPerSecond } ?: 0L,
        isVideo = isVideo
    )
}

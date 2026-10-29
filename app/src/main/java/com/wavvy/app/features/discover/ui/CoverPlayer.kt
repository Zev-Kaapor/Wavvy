package com.wavvy.app.features.discover.ui

// Android context
import android.content.Context
// Compose state and runtime
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
// Coroutines
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
// Project resources
import com.wavvy.app.R
import com.wavvy.app.core.designsystem.components.Toasts
import com.wavvy.app.core.playback.PlayerConnection
import com.wavvy.app.features.collection.data.CollectionKind
import com.wavvy.app.features.collection.data.CollectionRepository
import com.wavvy.app.features.home.data.HomeItem
import com.wavvy.app.features.home.data.HomeItemKind
import com.wavvy.app.features.home.ui.toPlayableTrack

// Plays a playlist or an album from its cover, its songs are asked first and the one that is being asked is known, so its button can show it
@Stable
internal class CoverPlayer(private val context: Context, private val scope: CoroutineScope) {
    var playingId by mutableStateOf<String?>(null)
        private set

    fun play(item: HomeItem) {
        if (playingId != null) return

        playingId = item.id
        scope.launch {
            val kind = if (item.kind == HomeItemKind.Album) CollectionKind.Album else CollectionKind.Playlist
            val tracks = CollectionRepository(context).load(kind, item.id).getOrNull()?.tracks.orEmpty().mapNotNull { it.toPlayableTrack() }
            if (tracks.isEmpty()) Toasts.show(context.getString(R.string.explore_play_error), isError = true) else PlayerConnection.playAll(context, tracks)
            playingId = null
        }
    }
}

@Composable
internal fun rememberCoverPlayer(): CoverPlayer {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    return remember(context) { CoverPlayer(context, scope) }
}

package com.wavvy.app.features.podcast.data

// Android context
import android.content.Context
// Coroutines and reactive flows
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
// Project resources
import com.wavvy.app.R
import com.wavvy.app.core.designsystem.components.Toasts
import com.wavvy.app.features.playlist.data.PlaylistRepository

// The episodes kept for later in the account, which YouTube Music keeps as a playlist of its own and shows in the library,
// held here so the button of an episode and its menu always say the same
object EpisodesLater {
    private const val PlaylistId = "SE"

    private val mutableKept = MutableStateFlow<Set<String>?>(null)

    // The ids of the episodes kept for later, none until they are read and while signed out
    val kept: StateFlow<Set<String>?> = mutableKept.asStateFlow()

    // Reads them from the account
    suspend fun refresh(context: Context) {
        val playlists = PlaylistRepository(context)
        mutableKept.value = if (playlists.isSignedIn()) playlists.songsOf(PlaylistId)?.keys.orEmpty() else null
    }

    // Puts the episode in or takes it out, at once on the screen and then in the account, and says so, a refusal puts it back as it was
    suspend fun toggle(context: Context, id: String) {
        val isKept = mutableKept.value?.contains(id) == true
        mutableKept.update { kept -> kept?.let { if (isKept) it - id else it + id } }

        val playlists = PlaylistRepository(context)
        val result = if (isKept) playlists.remove(PlaylistId, listOf(id)) else playlists.add(PlaylistId, listOf(id))
        val message = when {
            result.isFailure -> R.string.playlist_action_error
            isKept -> R.string.menu_later_removed
            else -> R.string.menu_later_added
        }
        Toasts.show(context.getString(message), isError = result.isFailure)
        if (result.isFailure) mutableKept.update { kept -> kept?.let { if (isKept) it + id else it - id } }
    }
}

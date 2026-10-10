package com.wavvy.app.features.like.data

// Android context
import android.content.Context
import android.widget.Toast
// JSON
import org.json.JSONObject
// Coroutines and reactive flows
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
// Project resources
import com.wavvy.app.R
import com.wavvy.app.core.innertube.InnerTubeClient
import com.wavvy.app.core.innertube.currentSession
import com.wavvy.app.core.innertube.findObjects
import com.wavvy.app.core.innertube.stringAt
import com.wavvy.app.features.playlist.data.PlaylistChanges
import com.wavvy.app.features.playlist.data.PlaylistEvent

// What the account likes, a like puts the song in the liked music of the account, which is the Songs list of the library
object Likes {
    private val mutableLiked = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val liked: StateFlow<Map<String, Boolean>> = mutableLiked.asStateFlow()

    // The like goes on after the menu that asked for it is closed
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    // Asks if the account likes the song, once, a guest likes nothing
    suspend fun load(context: Context, videoId: String) {
        if (videoId in mutableLiked.value) return

        val session = currentSession(context)
        if (session.cookies == null) return

        InnerTubeClient.next(session, videoId).onSuccess { response ->
            val button = response.findObjects("likeButtonRenderer").let { buttons ->
                buttons.firstOrNull { it.stringAt("target", "videoId") == videoId } ?: buttons.firstOrNull()
            } ?: return@onSuccess
            val status = button.optString("likeStatus").takeIf { it.isNotEmpty() } ?: return@onSuccess
            mutableLiked.update { it + (videoId to (status == LikeStatusLike)) }
        }
    }

    // Likes the song, or takes the like away when it was there, and says what happened
    fun toggle(context: Context, videoId: String) {
        val appContext = context.applicationContext

        scope.launch {
            val session = currentSession(appContext)
            if (session.cookies == null) {
                toast(appContext, R.string.like_sign_in)
                return@launch
            }

            load(appContext, videoId)
            val wasLiked = mutableLiked.value[videoId] == true

            // The button and the lists change before the answer comes
            mutableLiked.update { it + (videoId to !wasLiked) }
            PlaylistChanges.emit(PlaylistEvent.LikeChanged(videoId, !wasLiked))
            toast(appContext, if (wasLiked) R.string.like_removed else R.string.like_added)

            InnerTubeClient.rate(session, videoId, liked = !wasLiked)
                .onSuccess { PlaylistChanges.notifyChanged() }
                .onFailure {
                    mutableLiked.update { it + (videoId to wasLiked) }
                    PlaylistChanges.notifyChanged()
                    toast(appContext, R.string.like_error)
                }
        }
    }

    private fun toast(context: Context, message: Int) {
        Toast.makeText(context, context.getString(message), Toast.LENGTH_SHORT).show()
    }
}

// How YouTube Music names a song that is liked
private const val LikeStatusLike = "LIKE"

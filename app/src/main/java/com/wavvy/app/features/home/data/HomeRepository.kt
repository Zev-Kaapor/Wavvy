package com.wavvy.app.features.home.data

// Android context and web storage
import android.content.Context
import android.webkit.CookieManager
// Coroutines and reactive flows
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
// Project resources
import com.wavvy.app.core.innertube.InnerTubeClient
import com.wavvy.app.core.innertube.MusicOrigin
import com.wavvy.app.core.innertube.VisitorStore
import com.wavvy.app.core.innertube.YouTubeSession
import com.wavvy.app.core.innertube.deviceLocale
import com.wavvy.app.features.auth.data.Entry
import com.wavvy.app.features.auth.data.EntryStore

// Pages of YouTube Music that are the Home and the playlists of the account
private const val HomeBrowseId = "FEmusic_home"
private const val AccountPlaylistsBrowseId = "FEmusic_liked_playlists"

// Playlist of the account that holds the episodes saved for later, which is not a playlist of music
private const val EpisodesForLaterId = "SE"

// Asks YouTube Music for the Home, as a guest or with the account of the user when there is one
class HomeRepository(context: Context) {
    private val appContext = context.applicationContext
    private val entryStore = EntryStore(appContext)
    private val visitorStore = VisitorStore(appContext)

    // The Home, the Home under a filter, or the next page of one of them
    suspend fun load(params: String? = null, continuation: String? = null): Result<HomePage> {
        val session = currentSession()
        val answer = InnerTubeClient.browse(
            session = session,
            browseId = if (continuation == null) HomeBrowseId else null,
            params = params,
            continuation = continuation
        )

        return answer.mapCatching { response ->
            if (continuation == null) HomeParser.parseFirstPage(response) else HomeParser.parseContinuation(response)
        }
    }

    // Playlists of the account of the user, empty for a guest
    suspend fun accountPlaylists(): Result<List<HomeItem>> {
        if (entryStore.entry.first() != Entry.Google) return Result.success(emptyList())

        return InnerTubeClient.browse(session = currentSession(), browseId = AccountPlaylistsBrowseId)
            .mapCatching { response -> HomeParser.parseLibraryItems(response).filter { it.id != EpisodesForLaterId } }
    }

    // Who is asking, the cookies are only sent when the user signed in with Google
    private suspend fun currentSession(): YouTubeSession {
        val locale = deviceLocale()
        val cookies = if (entryStore.entry.first() == Entry.Google) {
            withContext(Dispatchers.Main) { CookieManager.getInstance().getCookie(MusicOrigin) }
        } else {
            null
        }

        return YouTubeSession(cookies = cookies, visitorData = visitorStore.get(locale), locale = locale)
    }
}

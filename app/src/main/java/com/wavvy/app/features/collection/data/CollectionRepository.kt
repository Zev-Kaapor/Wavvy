package com.wavvy.app.features.collection.data

// Android context
import android.content.Context
// Project resources
import com.wavvy.app.core.innertube.InnerTubeClient
import com.wavvy.app.core.innertube.currentSession
import com.wavvy.app.features.home.data.PlaylistPagePrefix

// What an album or a playlist page asks of YouTube Music
class CollectionRepository(context: Context) {
    private val appContext = context.applicationContext

    // The page of an album, by its page id, or of a playlist, by its id
    suspend fun load(kind: CollectionKind, id: String): Result<CollectionPage> {
        val browseId = if (kind == CollectionKind.Playlist) PlaylistPagePrefix + id else id

        return InnerTubeClient.browse(currentSession(appContext), browseId = browseId)
            .mapCatching { CollectionParser.parsePage(it, kind) ?: throw IllegalStateException("The page has no header") }
    }

    // The next songs of a long playlist
    suspend fun more(continuation: String): Result<CollectionMore> =
        InnerTubeClient.browse(currentSession(appContext), continuation = continuation).mapCatching(CollectionParser::parseMore)
}

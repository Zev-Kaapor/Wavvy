package com.wavvy.app.features.podcast.data

// Android context
import android.content.Context
// Project resources
import com.wavvy.app.core.innertube.InnerTubeClient
import com.wavvy.app.core.innertube.currentSession

// What the page of a podcast asks of YouTube Music
class PodcastRepository(context: Context) {
    private val appContext = context.applicationContext

    // The page of a podcast, by its id
    suspend fun load(id: String): Result<PodcastPage> =
        InnerTubeClient.browse(currentSession(appContext), browseId = id)
            .mapCatching { PodcastParser.parse(it) ?: throw IllegalStateException("The page has no header") }

    // The episodes under a filter or an order, or the next ones of a list, both are asked with a token
    suspend fun episodes(token: String): Result<PodcastEpisodes> =
        InnerTubeClient.browse(currentSession(appContext), continuation = token).map(PodcastParser::parseEpisodes)
}

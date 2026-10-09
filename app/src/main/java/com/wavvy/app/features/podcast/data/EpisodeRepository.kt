package com.wavvy.app.features.podcast.data

// Android context
import android.content.Context
// Project resources
import com.wavvy.app.core.innertube.InnerTubeClient
import com.wavvy.app.core.innertube.currentSession

// What the page of an episode asks of YouTube Music
class EpisodeRepository(context: Context) {
    private val appContext = context.applicationContext

    suspend fun load(id: String): Result<EpisodePage> =
        InnerTubeClient.browse(currentSession(appContext), browseId = id)
            .mapCatching { EpisodeParser.parse(it) ?: throw IllegalStateException("The page has no header") }
}

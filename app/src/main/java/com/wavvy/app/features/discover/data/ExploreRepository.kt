package com.wavvy.app.features.discover.data

// Android context
import android.content.Context
// Project resources
import com.wavvy.app.core.innertube.InnerTubeClient
import com.wavvy.app.core.innertube.currentSession

// What a page opened from the Explore tab asks of YouTube Music, by its id and the parameters its button brings
class ExploreRepository(context: Context) {
    private val appContext = context.applicationContext

    // The country is the code of the one picked on the charts, the page chooses by itself without it
    suspend fun load(browseId: String, params: String?, country: String? = null): Result<ExplorePage> =
        InnerTubeClient.browse(currentSession(appContext), browseId = browseId, params = params, selectedValues = country?.let { listOf(it) })
            .mapCatching { DiscoverParser.parseExplorePage(it) ?: throw IllegalStateException("The page has nothing to show") }
}

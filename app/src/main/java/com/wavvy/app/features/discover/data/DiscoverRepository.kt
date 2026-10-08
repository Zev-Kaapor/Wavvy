package com.wavvy.app.features.discover.data

// Android context
import android.content.Context
// Project resources
import com.wavvy.app.core.innertube.InnerTubeClient
import com.wavvy.app.core.innertube.currentSession

// Page that YouTube Music keeps for exploring, in the language and the country of the device
private const val ExploreId = "FEmusic_explore"

// What the Discover tab asks of YouTube Music
class DiscoverRepository(context: Context) {
    private val appContext = context.applicationContext

    suspend fun load(): Result<DiscoverPage> =
        InnerTubeClient.browse(currentSession(appContext), browseId = ExploreId)
            .mapCatching { DiscoverParser.parse(it) ?: throw IllegalStateException("The page has nothing to show") }
}

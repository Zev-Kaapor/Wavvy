package com.wavvy.app.features.library.data

// Android context
import android.content.Context
// Project resources
import com.wavvy.app.core.innertube.InnerTubeClient
import com.wavvy.app.core.innertube.currentSession

// What the library asks of YouTube Music, a guest has none
class LibraryRepository(context: Context) {
    private val appContext = context.applicationContext

    // The first page of a list
    suspend fun load(source: LibrarySource): Result<LibraryPage> =
        InnerTubeClient.browse(currentSession(appContext), browseId = source.browseId).map(LibraryParser::parse)

    // The next page of a list
    suspend fun more(token: String): Result<LibraryPage> =
        InnerTubeClient.browse(currentSession(appContext), continuation = token).map(LibraryParser::parse)

}

package com.wavvy.app.features.artist.data

// Android context
import android.content.Context
import java.util.Calendar
import java.util.concurrent.ConcurrentHashMap
// Project resources
import com.wavvy.app.core.innertube.InnerTubeClient
import com.wavvy.app.core.innertube.currentSession
import com.wavvy.app.features.collection.data.CollectionKind
import com.wavvy.app.features.collection.data.CollectionRepository
import com.wavvy.app.features.home.data.ChannelPrefix
import com.wavvy.app.features.home.data.PlaylistPagePrefix

// How many top songs the page shows at most, the page of YouTube Music brings only the first few and the rest come from the list behind its title
private const val TopSongsMax = 20

// Artists already summarized
private val summaries = ConcurrentHashMap<String, ArtistSummary>()

// What the page of an artist asks of YouTube Music
class ArtistRepository(context: Context) {
    private val appContext = context.applicationContext
    private val musicBrainz = MusicBrainz()

    // The page of an artist, by its channel id
    suspend fun load(id: String): Result<ArtistPage> {
        val response = InnerTubeClient.browse(currentSession(appContext), browseId = id).getOrElse { return Result.failure(it) }

        val page = ArtistParser.parse(response, Calendar.getInstance().get(Calendar.YEAR))
            ?: return Result.failure(IllegalStateException("The page has no header"))
        return Result.success(withMoreSongs(page))
    }

    // The photo, the name and the subscribers of an artist, without the rest of the page, kept for the next time
    suspend fun summary(id: String): Result<ArtistSummary> {
        summaries[id]?.let { return Result.success(it) }

        return InnerTubeClient.browse(currentSession(appContext), browseId = id).mapCatching { response ->
            val page = ArtistParser.parse(response, Calendar.getInstance().get(Calendar.YEAR))
                ?: throw IllegalStateException("The page has no header")
            ArtistSummary(name = page.name, photoUrl = page.bannerUrl, subscribers = page.subscription?.countText)
                .also { summaries[id] = it }
        }
    }

    // What MusicBrainz knows about the artist, found by the channels of YouTube it keeps and then by the name
    suspend fun profile(channelIds: List<String>, name: String): Result<ArtistProfile?> =
        musicBrainz.profile(channelIds.filter { it.startsWith(ChannelPrefix) }.distinct(), name)

    // Subscribes the account to the artist or takes the subscription away
    suspend fun setSubscribed(subscription: ArtistSubscription, subscribe: Boolean): Result<Unit> =
        InnerTubeClient.subscription(currentSession(appContext), subscription.channelId, subscribe, subscription.subscribeParams.takeIf { subscribe })
            .map { }

    // The top songs of the page followed by the ones of the list behind the title of the shelf, which has more of them
    private suspend fun withMoreSongs(page: ArtistPage): ArtistPage {
        val shelf = page.sections.firstOrNull()?.takeIf { it.items == page.topSongs } ?: return page
        val listId = shelf.link?.browseId?.takeIf { it.startsWith(PlaylistPagePrefix) }?.removePrefix(PlaylistPagePrefix) ?: return page
        val more = CollectionRepository(appContext).load(CollectionKind.Playlist, listId).getOrNull() ?: return page

        val known = page.topSongs.map { it.id }.toSet()
        val songs = (page.topSongs + more.tracks.filter { it.id !in known }).take(TopSongsMax)

        return page.copy(topSongs = songs, sections = listOf(shelf.copy(items = songs)) + page.sections.drop(1))
    }
}

package com.wavvy.app.features.artist.data

// JSON helpers of the YouTube Music answers
import com.wavvy.app.core.innertube.arrayAt
import com.wavvy.app.core.innertube.objectAt
import com.wavvy.app.core.innertube.objects
import com.wavvy.app.core.innertube.stringAt
// JSON
import org.json.JSONArray
import org.json.JSONObject
// Project resources
import com.wavvy.app.features.collection.data.CollectionParser
import com.wavvy.app.features.home.data.ChannelPrefix
import com.wavvy.app.features.home.data.HomeItem
import com.wavvy.app.features.home.data.HomeItemKind
import com.wavvy.app.features.home.data.HomeLink
import com.wavvy.app.features.home.data.HomeParser
import com.wavvy.app.features.home.data.HomeSection
import com.wavvy.app.features.home.data.Separator

// Turns the page of an artist into the same shelves and cards the Home and the search use
object ArtistParser {
    fun parse(response: JSONObject, currentYear: Int): ArtistPage? {
        val header = response.objectAt("header", "musicImmersiveHeaderRenderer")
            ?: response.objectAt("header", "musicVisualHeaderRenderer")
            ?: return null

        val blocks = response.arrayAt("contents", "singleColumnBrowseResultsRenderer", "tabs").objects().firstOrNull()
            ?.arrayAt("tabRenderer", "content", "sectionListRenderer", "contents").objects()

        // The top songs are a list of rows, the other shelves are carousels
        val songsShelf = blocks.firstNotNullOfOrNull { it.objectAt("musicShelfRenderer") }
        val topSongs = CollectionParser.tracksOf(songsShelf?.arrayAt("contents").objects(), null, emptyList())

        val topLink = songsShelf?.objectAt("title", "runs", 0, "navigationEndpoint", "browseEndpoint")
            ?: songsShelf?.objectAt("bottomEndpoint", "browseEndpoint")
        val topSection = songsShelf?.stringAt("title", "runs", 0, "text")
            ?.takeIf { topSongs.isNotEmpty() }
            ?.let { title ->
                HomeSection(
                    title = title,
                    label = null,
                    thumbnailUrl = null,
                    link = topLink?.stringAt("browseId")?.let { HomeLink(browseId = it, params = topLink.stringAt("params"), isArtist = false) },
                    items = topSongs
                )
            }

        // A shelf that points to the same artist, such as the live performances, would open the page again, so it does not link
        val carousels = HomeParser.parseSections(JSONArray(blocks.filter { it.has("musicCarouselShelfRenderer") }))
            .map { if (it.link?.browseId?.startsWith(ChannelPrefix) == true) it.copy(link = null) else it }
        val about = blocks.firstNotNullOfOrNull { it.objectAt("musicDescriptionShelfRenderer") }

        return ArtistPage(
            name = header.stringAt("title", "runs", 0, "text") ?: return null,
            bannerUrl = HomeParser.coverOf(header.objectAt("thumbnail")) ?: HomeParser.coverOf(header.objectAt("foregroundThumbnail")),
            monthlyListeners = header.stringAt("monthlyListenerCount", "runs", 0, "text"),
            subscription = subscriptionOf(header),
            featured = featuredOf(carousels, currentYear),
            topSongs = topSongs,
            sections = listOfNotNull(topSection) + carousels,
            views = about?.stringAt("subheader", "runs", 0, "text"),
            description = about?.arrayAt("description", "runs").objects().joinToString("") { it.optString("text") }.ifBlank { null }
                ?: header.arrayAt("description", "runs").objects().joinToString("") { it.optString("text") }.ifBlank { null }
        )
    }

    // The button that subscribes, with the state of the account
    private fun subscriptionOf(header: JSONObject): ArtistSubscription? {
        val button = header.objectAt("subscriptionButton", "subscribeButtonRenderer") ?: return null

        return ArtistSubscription(
            channelId = button.stringAt("channelId") ?: return null,
            isSubscribed = button.optBoolean("subscribed"),
            countText = button.stringAt("subscriberCountText", "runs", 0, "text"),
            subscribedLabel = button.stringAt("subscribedButtonText", "runs", 0, "text"),
            unsubscribedLabel = button.stringAt("unsubscribedButtonText", "runs", 0, "text"),
            subscribeParams = button.arrayAt("serviceEndpoints").objects()
                .firstNotNullOfOrNull { it.stringAt("subscribeEndpoint", "params") }
        )
    }

    // The page of the web does not bring the card of the new release that the app of YouTube Music shows, so it is made from the shelves
    // The first card of a shelf of albums or singles is the newest, and it counts as new while it is from this year, a single or an EP before an album
    private fun featuredOf(sections: List<HomeSection>, currentYear: Int): ArtistFeatured? =
        sections.mapNotNull { section -> section.items.firstOrNull()?.takeIf { it.kind == HomeItemKind.Album } }
            .mapNotNull { release ->
                val groups = release.countText?.split(Separator)?.map { it.trim() } ?: return@mapNotNull null
                val year = groups.lastOrNull()?.toIntOrNull()?.takeIf { it == currentYear } ?: return@mapNotNull null
                val type = groups.first().lowercase()
                val kind = when {
                    "single" in type -> ReleaseKind.Single
                    type == "ep" -> ReleaseKind.Ep
                    else -> ReleaseKind.Album
                }
                ArtistFeatured(kind, release)
            }
            .let { candidates -> candidates.firstOrNull { it.kind != ReleaseKind.Album } ?: candidates.firstOrNull() }
}

package com.wavvy.app.features.search.data

// JSON helpers of the YouTube Music answers
import com.wavvy.app.core.innertube.arrayAt
import com.wavvy.app.core.innertube.objectAt
import com.wavvy.app.core.innertube.objects
import com.wavvy.app.core.innertube.stringAt
// JSON
import org.json.JSONObject
// Project resources
import com.wavvy.app.features.home.data.ChannelPrefix
import com.wavvy.app.features.home.data.HomeItem
import com.wavvy.app.features.home.data.HomeItemKind
import com.wavvy.app.features.home.data.HomeParser
import com.wavvy.app.features.home.data.PageAlbum
import com.wavvy.app.features.home.data.PageArtist
import com.wavvy.app.features.home.data.PageAudiobook
import com.wavvy.app.features.home.data.PageChannel
import com.wavvy.app.features.home.data.PageLibraryArtist
import com.wavvy.app.features.home.data.PagePlaylist
import com.wavvy.app.features.home.data.PagePodcast
import com.wavvy.app.features.home.data.PlaylistPagePrefix

// Kind of a video that is an episode of a podcast, the others are songs and videos
private const val VideoTypeEpisode = "MUSIC_VIDEO_TYPE_PODCAST_EPISODE"

// Turns the answers of the search of YouTube Music into cards of the Home, so the same covers, rows and menus show them
// The line under a title of the search of everything starts with the word of the type, such as Song or Album, in the language of the request
object SearchParser {
    // A page of results, the first one of a search or one asked after another with the same filter
    fun parsePage(response: JSONObject, category: SearchCategory): SearchPage {
        val blocks = response.objectAt(
            "contents", "tabbedSearchResultsRenderer", "tabs", 0, "tabRenderer", "content", "sectionListRenderer"
        )?.arrayAt("contents").objects()

        // The rows come in a shelf when the search is filtered and in sections of one row when it is not
        val rows = blocks.flatMap { block ->
            block.arrayAt("musicShelfRenderer", "contents").objects() + block.arrayAt("itemSectionRenderer", "contents").objects()
        }
        val hasTypeWord = category == SearchCategory.All

        return SearchPage(
            topMatch = blocks.firstNotNullOfOrNull { it.objectAt("musicCardShelfRenderer") }?.let(::parseTopCard),
            items = rows.mapNotNull { it.objectAt("musicResponsiveListItemRenderer")?.let { row -> parseRow(row, hasTypeWord) } },
            continuation = blocks.firstNotNullOfOrNull { it.objectAt("musicShelfRenderer") }?.let(::continuationOf)
        )
    }

    // The next page of a filter, only rows and a new continuation
    fun parseContinuation(response: JSONObject): SearchPage {
        val shelf = response.objectAt("continuationContents", "musicShelfContinuation")

        return SearchPage(
            topMatch = null,
            items = shelf?.arrayAt("contents").objects()
                .mapNotNull { it.objectAt("musicResponsiveListItemRenderer")?.let { row -> parseRow(row, hasTypeWord = false) } },
            continuation = shelf?.let(::continuationOf)
        )
    }

    // The words and the matches under the field, in the order YouTube Music sends them
    fun parseSuggestions(response: JSONObject): List<SearchSuggestion> =
        response.arrayAt("contents").objects().flatMap { section ->
            section.arrayAt("searchSuggestionsSectionRenderer", "contents").objects().mapNotNull { entry ->
                // The searches of the account come in their own renderer with the token that removes them
                val history = entry.objectAt("historySuggestionRenderer")
                val renderer = history ?: entry.objectAt("searchSuggestionRenderer")
                val words = renderer?.arrayAt("suggestion", "runs").objects()
                    .joinToString("") { it.optString("text") }
                    .takeIf { it.isNotBlank() }

                when {
                    words != null -> SearchSuggestion.Words(
                        text = words,
                        feedbackToken = history?.stringAt("serviceEndpoint", "feedbackEndpoint", "feedbackToken"),
                        isHistory = history != null
                    )
                    else -> entry.objectAt("musicResponsiveListItemRenderer")
                        ?.let { parseRow(it, hasTypeWord = true) }
                        ?.let { SearchSuggestion.Match(it) }
                }
            }
        }

    // Token for the next page, empty on the last one
    private fun continuationOf(shelf: JSONObject): String? =
        shelf.arrayAt("continuations").objects().firstNotNullOfOrNull { it.stringAt("nextContinuationData", "continuation") }

    // The card that opens the results of everything, it is the best match for what was typed
    private fun parseTopCard(card: JSONObject): HomeItem? {
        val tap = card.objectAt("onTap")
        val button = card.arrayAt("buttons").objects().firstNotNullOfOrNull {
            it.stringAt("buttonRenderer", "command", "watchEndpoint", "videoId")
        }
        val browse = tap?.objectAt("browseEndpoint")

        return build(
            title = card.stringAt("title", "runs", 0, "text") ?: return null,
            cover = HomeParser.coverOf(card.objectAt("thumbnail")),
            pageType = browse?.let(HomeParser::pageTypeOf),
            browseId = browse?.stringAt("browseId"),
            videoId = tap?.stringAt("watchEndpoint", "videoId") ?: button,
            videoType = null,
            groups = HomeParser.groupsOf(card.arrayAt("subtitle", "runs").objects()).drop(1),
            isExplicit = false,
            playlistId = null
        )
    }

    // A row with the cover, the title and the line under it
    private fun parseRow(row: JSONObject, hasTypeWord: Boolean): HomeItem? {
        val columns = row.arrayAt("flexColumns").objects()
        val first = columns.getOrNull(0)?.objectAt("musicResponsiveListItemFlexColumnRenderer", "text")
        val runs = columns.getOrNull(1)?.arrayAt("musicResponsiveListItemFlexColumnRenderer", "text", "runs").objects()
        val browse = row.objectAt("navigationEndpoint", "browseEndpoint")
        val playEndpoint = row.objectAt(
            "overlay", "musicItemThumbnailOverlayRenderer", "content", "musicPlayButtonRenderer", "playNavigationEndpoint"
        )

        return build(
            title = first?.stringAt("runs", 0, "text") ?: return null,
            cover = HomeParser.coverOf(row.objectAt("thumbnail")),
            pageType = browse?.let(HomeParser::pageTypeOf),
            browseId = browse?.stringAt("browseId"),
            videoId = row.stringAt("playlistItemData", "videoId")
                ?: first.stringAt("runs", 0, "navigationEndpoint", "watchEndpoint", "videoId")
                ?: row.stringAt("navigationEndpoint", "watchEndpoint", "videoId")
                ?: playEndpoint?.stringAt("watchEndpoint", "videoId"),
            videoType = playEndpoint?.stringAt("watchEndpoint", "watchEndpointMusicSupportedConfigs", "watchEndpointMusicConfig", "musicVideoType"),
            groups = HomeParser.groupsOf(runs).let { groups -> if (hasTypeWord && groups.firstOrNull().isTypeWord()) groups.drop(1) else groups },
            isExplicit = HomeParser.hasExplicitBadge(row.arrayAt("badges")),
            playlistId = playEndpoint?.stringAt("watchPlaylistEndpoint", "playlistId")
        )
    }

    // The word of the type is plain text, so a first group with a link is an artist and stays, as in the rows that leave the word out
    private fun List<JSONObject>?.isTypeWord(): Boolean = this != null && none { it.has("navigationEndpoint") }

    // The card of what a result is, by the page it opens or the video it plays and not by the words, so it works in any language
    private fun build(
        title: String,
        cover: String?,
        pageType: String?,
        browseId: String?,
        videoId: String?,
        videoType: String?,
        groups: List<List<JSONObject>>,
        isExplicit: Boolean,
        playlistId: String?
    ): HomeItem? {
        val allRuns = groups.flatten()
        val lastGroupText = groups.lastOrNull()?.firstOrNull()?.optString("text")?.trim()?.takeIf { it.isNotEmpty() }

        return when {
            pageType == PageAlbum || pageType == PageAudiobook -> HomeItem(
                kind = HomeItemKind.Album,
                id = browseId ?: return null,
                title = title,
                thumbnailUrl = cover,
                artists = HomeParser.artistsOf(allRuns),
                isExplicit = isExplicit,
                playlistId = playlistId
            )

            pageType == PagePlaylist -> HomeItem(
                kind = HomeItemKind.Playlist,
                id = (browseId ?: return null).removePrefix(PlaylistPagePrefix),
                title = title,
                thumbnailUrl = cover,
                author = HomeParser.ownerOf(groups.firstOrNull().orEmpty()),
                // The last group of a playlist says how many songs it has or how many times it was seen
                countText = lastGroupText.takeIf { groups.size > 1 }
            )

            pageType == PageArtist || pageType == PageLibraryArtist || pageType == PageChannel ||
                (pageType == null && browseId?.startsWith(ChannelPrefix) == true) -> HomeItem(
                kind = HomeItemKind.Artist,
                id = browseId ?: return null,
                title = title,
                thumbnailUrl = cover,
                // The line of an artist is how many people listen to it, the Home never fills this for artists
                countText = lastGroupText
            )

            pageType == PagePodcast -> HomeItem(
                kind = HomeItemKind.Podcast,
                id = browseId ?: return null,
                title = title,
                thumbnailUrl = cover,
                author = HomeParser.ownerOf(groups.firstOrNull().orEmpty())
            )

            videoId != null && videoType == VideoTypeEpisode -> HomeItem(
                kind = HomeItemKind.Episode,
                id = videoId,
                title = title,
                thumbnailUrl = cover,
                // The line of an episode is the day it came out and the name of its podcast
                author = lastGroupText.takeIf { groups.size > 1 },
                isExplicit = isExplicit
            )

            videoId != null -> HomeItem(
                kind = HomeItemKind.Song,
                id = videoId,
                title = title,
                thumbnailUrl = cover,
                artists = HomeParser.artistsOf(allRuns),
                durationSeconds = lastGroupText?.let(HomeParser::parseTime),
                isExplicit = isExplicit
            )

            else -> null
        }
    }
}

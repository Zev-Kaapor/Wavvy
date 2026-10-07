package com.wavvy.app.features.home.data

// JSON helpers of the YouTube Music answers
import com.wavvy.app.core.innertube.arrayAt
import com.wavvy.app.core.innertube.at
import com.wavvy.app.core.innertube.objectAt
import com.wavvy.app.core.innertube.objects
import com.wavvy.app.core.innertube.stringAt
// JSON
import org.json.JSONArray
import org.json.JSONObject

// Page types that tell what a card opens
internal const val PageAlbum = "MUSIC_PAGE_TYPE_ALBUM"
internal const val PageAudiobook = "MUSIC_PAGE_TYPE_AUDIOBOOK"
internal const val PagePlaylist = "MUSIC_PAGE_TYPE_PLAYLIST"
internal const val PageArtist = "MUSIC_PAGE_TYPE_ARTIST"
internal const val PageLibraryArtist = "MUSIC_PAGE_TYPE_LIBRARY_ARTIST"
internal const val PagePodcast = "MUSIC_PAGE_TYPE_PODCAST_SHOW_DETAIL_PAGE"
internal const val PageEpisode = "MUSIC_PAGE_TYPE_NON_MUSIC_AUDIO_TRACK_PAGE"
internal const val PageChannel = "MUSIC_PAGE_TYPE_USER_CHANNEL"

// Prefix of the ids of channels, which count as artists when the link has no page type
internal const val ChannelPrefix = "UC"

// What separates the groups of a line
internal const val Separator = "•"

// Prefix of the page of a playlist, which is not part of its id
internal const val PlaylistPagePrefix = "VL"

// Badge of a track with explicit lyrics
internal const val ExplicitBadge = "MUSIC_EXPLICIT_BADGE"

// Seconds in a minute and in an hour, to read durations such as 3:45 or 1:02:30
private const val SecondsPerMinute = 60
private const val SecondsPerHour = 3600

// Where the covers sit in a card, under a name that depends on the kind of cover
private val CoverPaths = listOf(
    arrayOf<Any>("musicThumbnailRenderer", "thumbnail", "thumbnails"),
    arrayOf<Any>("musicAnimatedThumbnailRenderer", "backupRenderer", "thumbnail", "thumbnails"),
    arrayOf<Any>("croppedSquareThumbnailRenderer", "thumbnail", "thumbnails")
)

// Turns the answers of YouTube Music into the Home the way Metrolist reads them, whatever is not understood is left out
object HomeParser {
    // First page, with the filters, the shelves and the continuation
    fun parseFirstPage(response: JSONObject): HomePage {
        val list = response.objectAt(
            "contents", "singleColumnBrowseResultsRenderer", "tabs", 0, "tabRenderer", "content", "sectionListRenderer"
        )

        return HomePage(
            filters = list?.arrayAt("header", "chipCloudRenderer", "chips").objects().mapNotNull(::parseFilter),
            sections = parseSections(list?.arrayAt("contents")),
            continuation = list?.let(::continuationOf)
        )
    }

    // Next pages, only shelves and a new continuation
    fun parseContinuation(response: JSONObject): HomePage {
        val list = response.objectAt("continuationContents", "sectionListContinuation")

        return HomePage(
            filters = emptyList(),
            sections = parseSections(list?.arrayAt("contents")),
            continuation = list?.let(::continuationOf)
        )
    }

    // Playlists of a library page, as the ones of the account, with how many songs each has
    fun parseLibraryItems(response: JSONObject): List<HomeItem> {
        val list = response.objectAt(
            "contents", "singleColumnBrowseResultsRenderer", "tabs", 0, "tabRenderer", "content", "sectionListRenderer"
        ) ?: response.objectAt(
            "contents", "twoColumnBrowseResultsRenderer", "tabs", 0, "tabRenderer", "content", "sectionListRenderer"
        )
        val grid = list?.arrayAt("contents").objects().firstNotNullOfOrNull { block ->
            block.objectAt("gridRenderer")
                ?: block.arrayAt("itemSectionRenderer", "contents").objects().firstNotNullOfOrNull { it.objectAt("gridRenderer") }
        }

        return grid?.arrayAt("items").objects().mapNotNull { item ->
            val row = item.objectAt("musicTwoRowItemRenderer") ?: return@mapNotNull null
            parseTwoRow(row)?.let { parsed ->
                if (parsed.kind == HomeItemKind.Playlist) parsed.copy(countText = lastText(row.arrayAt("subtitle", "runs"))) else parsed
            }
        }
    }

    // Token for the next page, empty on the last one
    private fun continuationOf(list: JSONObject): String? =
        list.arrayAt("continuations").objects().firstNotNullOfOrNull {
            it.stringAt("nextContinuationData", "continuation") ?: it.stringAt("nextRadioContinuationData", "continuation")
        }

    // A chip with its text and the parameters that ask for the Home under it, the close button has neither
    private fun parseFilter(chip: JSONObject): HomeFilter? {
        val renderer = chip.objectAt("chipCloudChipRenderer") ?: return null

        return HomeFilter(
            title = renderer.stringAt("text", "runs", 0, "text") ?: return null,
            params = renderer.stringAt("navigationEndpoint", "browseEndpoint", "params") ?: return null
        )
    }

    // The shelves of the page, other kinds of blocks are not part of the Home
    internal fun parseSections(contents: JSONArray?): List<HomeSection> =
        contents.objects().mapNotNull { content ->
            val shelf = content.objectAt("musicCarouselShelfRenderer") ?: content.objectAt("musicImmersiveCarouselShelfRenderer")
            shelf?.let(::parseSection)
        }

    // A shelf needs a title and at least one card, the cards come grouped by layout as Metrolist reads them
    private fun parseSection(shelf: JSONObject): HomeSection? {
        val header = shelf.objectAt("header", "musicCarouselShelfBasicHeaderRenderer")
            ?: shelf.objectAt("header", "musicImmersiveCarouselShelfBasicHeaderRenderer")
        val title = header?.stringAt("title", "runs", 0, "text") ?: return null
        val contents = shelf.arrayAt("contents").objects()

        val items = contents.mapNotNull { it.objectAt("musicTwoRowItemRenderer")?.let(::parseTwoRow) } +
            contents.mapNotNull { it.objectAt("musicMultiRowListItemRenderer")?.let(::parseEpisodeRow) } +
            contents.mapNotNull { it.objectAt("musicResponsiveListItemRenderer")?.let(::parseSongRow) }
        if (items.isEmpty()) return null

        // The title opens a page only when its button goes to one, the play all button of a shelf of songs is not a page
        val more = header.objectAt("moreContentButton", "buttonRenderer", "navigationEndpoint", "browseEndpoint")

        return HomeSection(
            title = title,
            label = header.stringAt("strapline", "runs", 0, "text"),
            thumbnailUrl = coverOf(header.objectAt("thumbnail")),
            link = more?.stringAt("browseId")?.let { browseId ->
                HomeLink(browseId = browseId, params = more.stringAt("params"), isArtist = pageTypeOf(more) == PageArtist)
            },
            items = items
        )
    }

    // Card with a cover and two lines, used by songs, albums, playlists, artists, podcasts and episodes
    internal fun parseTwoRow(row: JSONObject): HomeItem? {
        val title = row.stringAt("title", "runs", 0, "text") ?: return null
        val runs = row.arrayAt("subtitle", "runs").objects()
        val thumbnail = coverOf(row.objectAt("thumbnailRenderer"))
        val explicit = hasExplicitBadge(row.arrayAt("subtitleBadges"))
        val endpoint = row.objectAt("navigationEndpoint")
        val browse = endpoint?.objectAt("browseEndpoint")
        val browseId = browse?.stringAt("browseId")
        val pageType = browse?.let(::pageTypeOf)
        val playEndpoint = row.objectAt(
            "thumbnailOverlay", "musicItemThumbnailOverlayRenderer", "content", "musicPlayButtonRenderer", "playNavigationEndpoint"
        )

        return when {
            endpoint?.objectAt("watchEndpoint") != null -> HomeItem(
                kind = HomeItemKind.Song,
                id = endpoint.stringAt("watchEndpoint", "videoId") ?: return null,
                title = title,
                thumbnailUrl = thumbnail,
                artists = artistsOf(runs),
                isExplicit = explicit
            )

            pageType == PageAlbum || pageType == PageAudiobook -> HomeItem(
                kind = HomeItemKind.Album,
                id = browseId ?: return null,
                title = title,
                thumbnailUrl = thumbnail,
                artists = artistsOf(runs),
                // An album with no artist in its line, as on the page of an artist, shows its kind and year
                countText = if (artistsOf(runs).isEmpty()) groupsOf(runs).joinToString(" $Separator ") { group -> group.joinToString("") { it.optString("text") } }.ifBlank { null } else null,
                isExplicit = explicit,
                playlistId = playEndpoint?.stringAt("watchPlaylistEndpoint", "playlistId")
            )

            pageType == PagePlaylist -> HomeItem(
                kind = HomeItemKind.Playlist,
                id = (browseId ?: return null).removePrefix(PlaylistPagePrefix),
                title = title,
                thumbnailUrl = thumbnail,
                author = ownerOf(runs),
                // The line of a playlist of an artist ends with its views, as in Playlist, Ari Abdul, 46K views
                countText = groupsOf(runs).getOrNull(2)?.joinToString("") { it.optString("text") }?.takeIf { it.isNotBlank() }
            )

            pageType == PageArtist || pageType == PageLibraryArtist -> HomeItem(
                kind = HomeItemKind.Artist,
                id = browseId ?: return null,
                // The name of an artist is the last piece of its title
                title = row.arrayAt("title", "runs").objects().lastOrNull()?.optString("text")?.takeIf { it.isNotBlank() } ?: title,
                thumbnailUrl = thumbnail
            )

            pageType == PagePodcast -> HomeItem(
                kind = HomeItemKind.Podcast,
                id = browseId ?: return null,
                title = title,
                thumbnailUrl = thumbnail,
                author = podcastAuthorOf(runs)
            )

            pageType == PageEpisode -> HomeItem(
                kind = HomeItemKind.Episode,
                id = playEndpoint?.stringAt("watchEndpoint", "videoId") ?: return null,
                title = title,
                thumbnailUrl = thumbnail,
                author = artistsOf(runs).firstOrNull(),
                durationSeconds = groupsOf(runs).lastOrNull()?.firstOrNull()?.optString("text")?.let(::parseTime),
                isExplicit = explicit
            )

            else -> null
        }
    }

    // Song as a row of a list, as the quick picks come, the second column has the artists, the album and the length
    private fun parseSongRow(row: JSONObject): HomeItem? {
        val columns = row.arrayAt("flexColumns").objects()
        val endpoint = row.objectAt("navigationEndpoint")
        val playEndpoint = row.objectAt(
            "overlay", "musicItemThumbnailOverlayRenderer", "content", "musicPlayButtonRenderer", "playNavigationEndpoint"
        )
        val isSong = endpoint == null || endpoint.has("watchEndpoint") || endpoint.has("watchPlaylistEndpoint") ||
            playEndpoint?.has("watchEndpoint") == true
        if (!isSong) return null

        val first = columns.getOrNull(0)?.objectAt("musicResponsiveListItemFlexColumnRenderer", "text")
        val secondLine = columns.getOrNull(1)?.arrayAt("musicResponsiveListItemFlexColumnRenderer", "text", "runs") ?: return null
        val groups = groupsOf(secondLine.objects())

        return HomeItem(
            kind = HomeItemKind.Song,
            id = row.stringAt("playlistItemData", "videoId")
                ?: first?.stringAt("runs", 0, "navigationEndpoint", "watchEndpoint", "videoId")
                ?: playEndpoint?.stringAt("watchEndpoint", "videoId")
                ?: return null,
            title = first?.stringAt("runs", 0, "text") ?: return null,
            thumbnailUrl = coverOf(row.objectAt("thumbnail")) ?: return null,
            artists = artistsOf(groups.getOrNull(0).orEmpty()),
            durationSeconds = groups.lastOrNull()?.firstOrNull()?.optString("text")?.let(::parseTime),
            isExplicit = hasExplicitBadge(row.arrayAt("badges"))
        )
    }

    // Episode of a podcast as a row, its length is the last group of its line when there is one
    private fun parseEpisodeRow(row: JSONObject): HomeItem? =
        HomeItem(
            kind = HomeItemKind.Episode,
            id = row.stringAt("onTap", "watchEndpoint", "videoId") ?: return null,
            title = row.stringAt("title", "runs", 0, "text") ?: return null,
            thumbnailUrl = coverOf(row.objectAt("thumbnail")) ?: return null,
            durationSeconds = groupsOf(row.arrayAt("subtitle", "runs").objects()).lastOrNull()?.firstOrNull()
                ?.optString("text")?.let(::parseTime)
        )

    // Names of the pieces that link to the page of an artist or of a channel
    internal fun artistsOf(runs: List<JSONObject>): List<String> =
        runs.mapNotNull { run ->
            val browse = run.objectAt("navigationEndpoint", "browseEndpoint") ?: return@mapNotNull null
            val type = pageTypeOf(browse)
            val isArtist = type == PageArtist || type == PageLibraryArtist || type == PageChannel ||
                (type == null && browse.optString("browseId").startsWith(ChannelPrefix))
            run.optString("text").trim().takeIf { isArtist && it.isNotEmpty() }
        }

    // Owner of a playlist, the first artist or, without one, the first piece that is not a link
    internal fun ownerOf(runs: List<JSONObject>): String? =
        artistsOf(runs).firstOrNull()
            ?: groupsOf(runs).firstOrNull()?.firstOrNull()
                ?.takeIf { !it.has("navigationEndpoint") }
                ?.optString("text")?.trim()?.takeIf { it.isNotEmpty() }

    // Author of a podcast, an artist, the piece that links to a podcast, or the first piece that is not a link
    private fun podcastAuthorOf(runs: List<JSONObject>): String? =
        artistsOf(runs).firstOrNull()
            ?: runs.firstOrNull { run ->
                run.optString("text").isNotBlank() &&
                    run.objectAt("navigationEndpoint", "browseEndpoint")?.let(::pageTypeOf) == PagePodcast
            }?.optString("text")?.trim()
            ?: ownerOf(runs)

    // Pieces of a line split into groups by the bullets between them
    internal fun groupsOf(runs: List<JSONObject>): List<List<JSONObject>> {
        val groups = mutableListOf(mutableListOf<JSONObject>())
        runs.forEach { run ->
            if (run.optString("text").trim() == Separator) groups.add(mutableListOf()) else groups.last().add(run)
        }

        return groups
    }

    // Text of the last piece of a line
    internal fun lastText(runs: JSONArray?): String? = runs.objects().lastOrNull()?.optString("text")?.takeIf { it.isNotBlank() }

    // Page type of a link
    internal fun pageTypeOf(browse: JSONObject): String? =
        browse.stringAt("browseEndpointContextSupportedConfigs", "browseEndpointContextMusicConfig", "pageType")

    // A duration such as 3:45, 3.45 or 1:02:30, empty when the text is not one
    internal fun parseTime(text: String): Int? {
        val parts = text.trim().split(Regex("[:.,]")).map { it.toIntOrNull() ?: return null }

        return when (parts.size) {
            2 -> parts[0] * SecondsPerMinute + parts[1]
            3 -> parts[0] * SecondsPerHour + parts[1] * SecondsPerMinute + parts[2]
            else -> null
        }
    }

    // True when one of the badges of a card says explicit
    internal fun hasExplicitBadge(badges: JSONArray?): Boolean =
        badges.objects().any { it.stringAt("musicInlineBadgeRenderer", "icon", "iconType") == ExplicitBadge }

    // The largest picture of a cover
    internal fun coverOf(renderer: JSONObject?): String? =
        CoverPaths.firstNotNullOfOrNull { path ->
            val thumbnails = renderer?.at(*path) as? JSONArray
            thumbnails?.optJSONObject(thumbnails.length() - 1)?.optString("url")?.takeIf { it.isNotBlank() }
        }
}

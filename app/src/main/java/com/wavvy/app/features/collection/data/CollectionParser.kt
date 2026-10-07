package com.wavvy.app.features.collection.data

// JSON helpers of the YouTube Music answers
import com.wavvy.app.core.innertube.arrayAt
import com.wavvy.app.core.innertube.objectAt
import com.wavvy.app.core.innertube.objects
import com.wavvy.app.core.innertube.stringAt
// JSON
import org.json.JSONArray
import org.json.JSONObject
// Project resources
import com.wavvy.app.features.home.data.HomeItem
import com.wavvy.app.features.home.data.HomeItemKind
import com.wavvy.app.features.home.data.HomeParser

// Turns the page of an album or of a playlist into the same cards of songs the Home and the search use
object CollectionParser {
    // The header and the songs of the first page
    fun parsePage(response: JSONObject, kind: CollectionKind): CollectionPage? {
        val two = response.objectAt("contents", "twoColumnBrowseResultsRenderer") ?: return null
        val first = two.arrayAt("tabs").objects().firstOrNull()
            ?.arrayAt("tabRenderer", "content", "sectionListRenderer", "contents").objects().firstOrNull()
        val header = first?.objectAt("musicResponsiveHeaderRenderer")
            ?: first?.objectAt("musicEditablePlaylistDetailHeaderRenderer", "header", "musicResponsiveHeaderRenderer")
            ?: return null

        val cover = HomeParser.coverOf(header.objectAt("thumbnail"))
        val strapline = header.arrayAt("straplineTextOne", "runs").objects()
        val albumArtists = HomeParser.artistsOf(strapline)
        val facepile = header.stringAt("facepile", "avatarStackViewModel", "text", "content")

        val blocks = two.arrayAt("secondaryContents", "sectionListRenderer", "contents")
        val shelf = blocks.objects().firstOrNull()
        val rows = (shelf?.objectAt("musicPlaylistShelfRenderer") ?: shelf?.objectAt("musicShelfRenderer"))
            ?.arrayAt("contents").objects()

        // An album belongs to its artists, a playlist to whoever made it
        val owner = if (kind == CollectionKind.Album) {
            albumArtists.joinToString(", ").ifEmpty { null }
        } else {
            facepile ?: HomeParser.ownerOf(strapline)
        }

        return CollectionPage(
            kind = kind,
            title = header.stringAt("title", "runs", 0, "text") ?: return null,
            subtitle = header.arrayAt("subtitle", "runs").objects().joinToString("") { it.optString("text") }.ifBlank { null },
            owner = owner,
            ownerPhotoUrl = HomeParser.coverOf(header.objectAt("straplineThumbnail"))
                ?: header.stringAt("facepile", "avatarStackViewModel", "avatars", 0, "avatarViewModel", "image", "sources", 0, "url"),
            details = header.arrayAt("secondSubtitle", "runs").objects().joinToString("") { it.optString("text") }.ifBlank { null },
            description = header.arrayAt("description", "musicDescriptionShelfRenderer", "description", "runs").objects()
                .joinToString("") { it.optString("text") }.ifBlank { null },
            thumbnailUrl = cover,
            tracks = tracksOf(rows, cover, albumArtists),
            continuation = continuationOf(rows),
            sections = HomeParser.parseSections(blocks?.let { JSONArray((0 until it.length()).drop(1).mapNotNull { index -> it.optJSONObject(index) }) })
        )
    }

    // The next songs of a long playlist
    fun parseMore(response: JSONObject): CollectionMore {
        val rows = response.arrayAt("onResponseReceivedActions").objects().firstNotNullOfOrNull {
            it.arrayAt("appendContinuationItemsAction", "continuationItems")
        }.objects()

        return CollectionMore(tracks = tracksOf(rows, null, emptyList()), continuation = continuationOf(rows))
    }

    // The songs among the rows, the rows that cannot play have no video and are left out
    internal fun tracksOf(rows: List<JSONObject>, albumCover: String?, albumArtists: List<String>): List<HomeItem> =
        rows.mapNotNull { it.objectAt("musicResponsiveListItemRenderer")?.let { row -> trackOf(row, albumCover, albumArtists) } }

    // One song of the list, a song of an album has no cover of its own and takes the cover of the album
    private fun trackOf(row: JSONObject, albumCover: String?, albumArtists: List<String>): HomeItem? {
        val columns = row.arrayAt("flexColumns").objects()
        val title = columns.getOrNull(0)?.objectAt("musicResponsiveListItemFlexColumnRenderer", "text")
        val artistRuns = columns.getOrNull(1)?.arrayAt("musicResponsiveListItemFlexColumnRenderer", "text", "runs").objects()
        val play = row.objectAt("overlay", "musicItemThumbnailOverlayRenderer", "content", "musicPlayButtonRenderer", "playNavigationEndpoint")

        val videoId = row.stringAt("playlistItemData", "videoId")
            ?: title?.stringAt("runs", 0, "navigationEndpoint", "watchEndpoint", "videoId")
            ?: play?.stringAt("watchEndpoint", "videoId")
            ?: return null

        return HomeItem(
            kind = HomeItemKind.Song,
            id = videoId,
            title = title?.stringAt("runs", 0, "text") ?: return null,
            thumbnailUrl = HomeParser.coverOf(row.objectAt("thumbnail")) ?: albumCover,
            artists = HomeParser.artistsOf(artistRuns).ifEmpty { albumArtists },
            durationSeconds = row.arrayAt("fixedColumns").objects().firstOrNull()
                ?.stringAt("musicResponsiveListItemFixedColumnRenderer", "text", "runs", 0, "text")?.let(HomeParser::parseTime),
            isExplicit = HomeParser.hasExplicitBadge(row.arrayAt("badges")),
            countText = playsOf(columns.getOrNull(2))
        )
    }

    // How many times a song of an album was played, the third column of a playlist has the name of the album, which is a link
    private fun playsOf(column: JSONObject?): String? =
        column?.arrayAt("musicResponsiveListItemFlexColumnRenderer", "text", "runs").objects()
            .firstOrNull { !it.has("navigationEndpoint") }?.optString("text")?.takeIf { it.isNotBlank() }

    // The token that asks for the next songs, it comes as the last row of the list
    private fun continuationOf(rows: List<JSONObject>): String? =
        rows.lastOrNull()?.stringAt("continuationItemRenderer", "continuationEndpoint", "continuationCommand", "token")
}

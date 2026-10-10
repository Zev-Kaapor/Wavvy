package com.wavvy.app.features.library.data

// JSON helpers of the YouTube Music answers
import com.wavvy.app.core.innertube.arrayAt
import com.wavvy.app.core.innertube.findObjects
import com.wavvy.app.core.innertube.objectAt
import com.wavvy.app.core.innertube.objects
import com.wavvy.app.core.innertube.stringAt
// JSON
import org.json.JSONObject
// Project resources
import com.wavvy.app.features.collection.data.CollectionParser
import com.wavvy.app.features.home.data.HomeItem
import com.wavvy.app.features.home.data.HomeItemKind
import com.wavvy.app.features.home.data.HomeParser
import com.wavvy.app.features.home.data.PageArtist

// Turns the lists of the library of YouTube Music into the same cards the rest of the app draws, the list may be a grid of covers or rows
object LibraryParser {
    // A list of the library, the first page or the next ones of it
    fun parse(response: JSONObject): LibraryPage {
        val next = response.objectAt("continuationContents")
        val container = next?.objectAt("gridContinuation")
            ?: next?.objectAt("musicShelfContinuation")
            ?: response.findObjects("gridRenderer").firstOrNull()
            ?: response.findObjects("musicShelfRenderer").firstOrNull()
            ?: response.findObjects("musicPlaylistShelfRenderer").firstOrNull()

        val entries = container?.arrayAt("items").objects() + container?.arrayAt("contents").objects()
        val continuation = container?.arrayAt("continuations").objects()
            .firstNotNullOfOrNull { it.stringAt("nextContinuationData", "continuation") }

        return LibraryPage(items = entries.mapNotNull(::itemOf), continuation = continuation, sortOptions = sortOptionsOf(response))
    }

    // The orders of the button above the list, only when more than one of them can be asked
    internal fun sortOptionsOf(response: JSONObject): List<LibrarySortOption> {
        val options = response.findObjects("musicMultiSelectMenuItemRenderer").mapNotNull { option ->
            val title = option.arrayAt("title", "runs").objects().joinToString("") { it.optString("text") }.trim().takeIf { it.isNotEmpty() }
                ?: return@mapNotNull null
            val token = option.findObjects("reloadContinuationData").firstOrNull()?.stringAt("continuation")

            LibrarySortOption(title = title, token = token, isSelected = option.optBoolean("isSelected"))
        }

        return options.takeIf { list -> list.size > 1 && list.any { it.token != null } }.orEmpty()
    }

    // One entry of the list, a cover with two lines or a row
    private fun itemOf(entry: JSONObject): HomeItem? {
        entry.objectAt("musicTwoRowItemRenderer")?.let { row ->
            val item = HomeParser.parseTwoRow(row) ?: return null
            // The line under the cover is kept as YouTube Music writes it, with the kind of the item when the list says it
            return item.takeIf { it.kind != HomeItemKind.Playlist }
                ?: item.copy(countText = row.arrayAt("subtitle", "runs").objects().lastOrNull()?.optString("text")?.takeIf { it.isNotBlank() })
        }

        val row = entry.objectAt("musicResponsiveListItemRenderer") ?: return null
        val browse = row.objectAt("navigationEndpoint", "browseEndpoint")

        // A row that opens the page of an artist
        if (browse != null && HomeParser.pageTypeOf(browse) == PageArtist) {
            return HomeItem(
                kind = HomeItemKind.Artist,
                id = browse.stringAt("browseId") ?: return null,
                title = row.stringAt("flexColumns", 0, "musicResponsiveListItemFlexColumnRenderer", "text", "runs", 0, "text") ?: return null,
                thumbnailUrl = HomeParser.coverOf(row.objectAt("thumbnail")),
                countText = secondLineOf(row),
                lineText = secondLineOf(row)
            )
        }

        // Any other row is a song
        return CollectionParser.tracksOf(listOf(entry), null, emptyList()).firstOrNull()?.copy(lineText = secondLineOf(row))
    }

    // The line under the title of a row, as it comes
    private fun secondLineOf(row: JSONObject): String? =
        row.arrayAt("flexColumns").objects().getOrNull(1)
            ?.arrayAt("musicResponsiveListItemFlexColumnRenderer", "text", "runs").objects()
            .joinToString("") { it.optString("text") }.trim().takeIf { it.isNotEmpty() }
}

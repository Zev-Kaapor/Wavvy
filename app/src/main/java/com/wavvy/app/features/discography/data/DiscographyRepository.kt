package com.wavvy.app.features.discography.data

// Android context
import android.content.Context
// JSON helpers of the YouTube Music answers
import com.wavvy.app.core.innertube.arrayAt
import com.wavvy.app.core.innertube.objectAt
import com.wavvy.app.core.innertube.objects
import com.wavvy.app.core.innertube.stringAt
// JSON
import org.json.JSONObject
// Project resources
import com.wavvy.app.core.innertube.InnerTubeClient
import com.wavvy.app.core.innertube.currentSession
import com.wavvy.app.features.home.data.HomeItem
import com.wavvy.app.features.home.data.HomeParser

// A filter of the list, such as albums or singles, the token asks the list again under that filter
data class DiscographyChip(
    val title: String,
    val isSelected: Boolean,
    val token: String?
)

// The covers of a list and the filters it has
data class DiscographyPage(
    val chips: List<DiscographyChip>,
    val items: List<HomeItem>
)

// The full list behind a shelf of an artist, such as all the albums or all the singles, a grid of cards with filters on top
class DiscographyRepository(context: Context) {
    private val appContext = context.applicationContext

    // The list a shelf points to
    suspend fun load(browseId: String, params: String?): Result<DiscographyPage> =
        InnerTubeClient.browse(currentSession(appContext), browseId = browseId, params = params).mapCatching { response ->
            val list = response.arrayAt("contents", "singleColumnBrowseResultsRenderer", "tabs").objects().firstOrNull()
                ?.objectAt("tabRenderer", "content", "sectionListRenderer")
            pageOf(list)
        }

    // The same list under another filter
    suspend fun reload(token: String): Result<DiscographyPage> =
        InnerTubeClient.browse(currentSession(appContext), continuation = token).mapCatching { response ->
            pageOf(response.objectAt("continuationContents", "sectionListContinuation"))
        }

    // The cards and the filters of a list of sections, which is the page itself or the answer of a filter
    private fun pageOf(list: JSONObject?): DiscographyPage {
        val chips = list?.arrayAt("header", "musicSideAlignedItemRenderer", "startItems").objects()
            .flatMap { it.arrayAt("chipCloudRenderer", "chips").objects() }
            .mapNotNull { chip ->
                val body = chip.objectAt("chipCloudChipRenderer") ?: return@mapNotNull null
                DiscographyChip(
                    title = body.stringAt("text", "runs", 0, "text") ?: return@mapNotNull null,
                    isSelected = body.optBoolean("isSelected"),
                    token = body.stringAt("navigationEndpoint", "browseSectionListReloadEndpoint", "continuation", "reloadContinuationData", "continuation")
                )
            }

        val items = list?.arrayAt("contents").objects()
            .flatMap { it.objectAt("gridRenderer")?.arrayAt("items").objects() }
            .mapNotNull { it.objectAt("musicTwoRowItemRenderer")?.let(HomeParser::parseTwoRow) }
            .distinctBy { it.id }

        return DiscographyPage(chips = chips, items = items)
    }
}

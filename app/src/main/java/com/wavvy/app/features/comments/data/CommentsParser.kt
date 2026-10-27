package com.wavvy.app.features.comments.data

// Android encoding
import android.net.Uri
import android.util.Base64
// JSON helpers of the YouTube Music answers
import com.wavvy.app.core.innertube.arrayAt
import com.wavvy.app.core.innertube.findObjects
import com.wavvy.app.core.innertube.objectAt
import com.wavvy.app.core.innertube.objects
import com.wavvy.app.core.innertube.stringAt
// JSON
import org.json.JSONObject

// The panel of the page of an album the comments open in
private const val CommentsPanelTag = "engagement-panel-comments-section"

// The state of a comment the artist gave a heart
private const val HeartedState = "TOOLBAR_HEART_STATE_HEARTED"

// Turns the comments the Android app of YouTube Music is given into the comments of the sheet
object CommentsParser {
    // The token that asks the comments of an album, it comes with the page of the album as the Android app sees it
    fun threadTokenOf(response: JSONObject): String? =
        response.arrayAt("panels").objects()
            .mapNotNull { it.objectAt("engagementPanelSectionListRenderer") }
            .firstOrNull { it.stringAt("identifier", "tag") == CommentsPanelTag }
            ?.stringAt("content", "sectionListRenderer", "continuations", 0, "reloadContinuationData", "continuation")

    // The comments of a page in their order, each with what asks its replies, and the token of the next page
    // The comments come as entities beside the list, in the order of the list
    fun parsePage(response: JSONObject): CommentPage {
        val payloads = response.arrayAt("frameworkUpdates", "entityBatchUpdate", "mutations").objects().mapNotNull { it.objectAt("payload") }
        val list = response.objectAt("continuationContents") ?: response.objectAt("contents")

        // What asks the replies of each comment, and how many there are, by the id of the comment
        val replies = payloads.mapNotNull { it.objectAt("commentSurfaceEntityPayload") }.mapNotNull { surface ->
            val extras = surface.objectAt("viewAllRepliesCommand", "innertubeCommand", "showEngagementPanelEndpoint", "engagementPanelExtras", "commentRepliesEngagementPanelExtras")
            val id = surface.stringAt("key")?.let(::commentIdOf)
            val feedId = extras?.stringAt("commentFeedId")
            val params = extras?.stringAt("params")
            if (id == null || feedId == null || params == null) null else id to (surface.stringAt("viewAllRepliesText") to CommentReplies(feedId, params))
        }.toMap()

        // Whether the artist gave each comment a heart, by the key of the state of its buttons
        val hearts = payloads.mapNotNull { it.objectAt("engagementToolbarStateEntityPayload") }
            .mapNotNull { state -> state.stringAt("key")?.let { it to (state.stringAt("heartState") == HeartedState) } }
            .toMap()

        val comments = payloads.mapNotNull { it.objectAt("commentEntityPayload") }.mapNotNull { entity ->
            val properties = entity.objectAt("properties") ?: return@mapNotNull null
            val id = properties.stringAt("commentId") ?: return@mapNotNull null
            val author = entity.objectAt("author")
            val reply = replies[id]

            Comment(
                id = id,
                author = author?.stringAt("displayName").orEmpty(),
                authorPhotoUrl = author?.stringAt("avatarThumbnailUrl"),
                authorChannelId = author?.stringAt("channelId"),
                isArtist = author?.optBoolean("isArtist") == true,
                text = properties.stringAt("content", "content") ?: return@mapNotNull null,
                publishedTime = properties.stringAt("publishedTime").orEmpty(),
                likeCount = entity.stringAt("toolbar", "likeCountNotliked")?.trim().orEmpty(),
                repliesText = reply?.first,
                replies = reply?.second,
                isReply = properties.optInt("replyLevel") > 0,
                isCreator = author?.optBoolean("isCreator") == true,
                isVerified = author?.optBoolean("isVerified") == true,
                isHearted = properties.stringAt("toolbarStateKey")?.let { hearts[it] } == true,
                heartText = entity.stringAt("toolbar", "heartActiveTooltip")
            )
        }

        return CommentPage(
            comments = comments,
            continuation = list?.findObjects("nextContinuationData")?.firstNotNullOfOrNull { it.stringAt("continuation") }
        )
    }

    // The id of the comment a surface belongs to, its key packs the id, a slash and a number
    private fun commentIdOf(key: String): String? = runCatching {
        val bytes = Base64.decode(Uri.decode(key).replace('-', '+').replace('_', '/'), Base64.DEFAULT)
        String(bytes, 2, bytes[1].toInt(), Charsets.ISO_8859_1).substringBefore('/')
    }.getOrNull()
}

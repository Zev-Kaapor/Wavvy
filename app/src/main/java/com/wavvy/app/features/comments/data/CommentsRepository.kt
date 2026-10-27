package com.wavvy.app.features.comments.data

// Android context
import android.content.Context
// JSON
import org.json.JSONObject
// Concurrency
import java.util.concurrent.ConcurrentHashMap
// Project resources
import com.wavvy.app.core.innertube.InnerTubeClient
import com.wavvy.app.core.innertube.YouTubeSession
import com.wavvy.app.core.innertube.currentSession

// The tokens of the comments of the albums already opened, kept while the app runs so the page of the album is asked only once
private val threadTokens = ConcurrentHashMap<String, String>()

// What the sheet of comments asks of YouTube Music, through the Android app, the only one the comments of an album come to
class CommentsRepository(context: Context) {
    private val appContext = context.applicationContext

    // The token of the comments of an album, it comes with the page of the album as the Android app sees it
    suspend fun threadToken(albumId: String): Result<String> {
        threadTokens[albumId]?.let { return Result.success(it) }

        return InnerTubeClient.appBrowse(guest(), browseId = albumId)
            .mapCatching { CommentsParser.threadTokenOf(it) ?: throw IllegalStateException("The album has no comments") }
            .onSuccess { threadTokens[albumId] = it }
    }

    // The first comments in the order that was chosen
    suspend fun comments(token: String, sort: CommentSort): Result<CommentPage> =
        InnerTubeClient.appBrowse(guest(), continuation = token, formData = sortOf(sort)).mapCatching(CommentsParser::parsePage)

    // The next comments, or the next replies
    suspend fun more(continuation: String): Result<CommentPage> =
        InnerTubeClient.appBrowse(guest(), continuation = continuation).mapCatching(CommentsParser::parsePage)

    // The replies of a comment, the comment itself comes first
    suspend fun replies(replies: CommentReplies): Result<CommentPage> =
        InnerTubeClient.appBrowse(guest(), browseId = replies.feedId, params = replies.params).mapCatching(CommentsParser::parsePage)

    // The Android app only takes the Google account of the device and refuses the cookies of the sign in, so the comments are asked as a guest
    private suspend fun guest(): YouTubeSession = currentSession(appContext).copy(cookies = null)

    // The order of the comments as the chips of the sheet send it
    private fun sortOf(sort: CommentSort): JSONObject =
        JSONObject().put("commentsFilterFormData", JSONObject().put("commentsFilter", JSONObject().put("sortBy", sort.value)))
}

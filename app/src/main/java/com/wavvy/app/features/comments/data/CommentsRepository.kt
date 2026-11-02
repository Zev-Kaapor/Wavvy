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

// The tokens of the comments of the albums and of the songs already opened, kept while the app runs so the page is asked only once
private val threadTokens = ConcurrentHashMap<String, String>()

// What the sheet of comments asks of YouTube Music, through the Android app, the only one the comments of an album and of a song come to
class CommentsRepository(context: Context) {
    private val appContext = context.applicationContext

    // The token of the comments of an album or of a song, it comes with the page of the album or with the page of the song in the player, as the Android app sees them
    suspend fun threadToken(id: String, isSong: Boolean = false): Result<String> {
        threadTokens[id]?.let { return Result.success(it) }

        val page = if (isSong) InnerTubeClient.appNext(guest(), id) else InnerTubeClient.appBrowse(guest(), browseId = id)
        return page
            .mapCatching { CommentsParser.threadTokenOf(it) ?: throw IllegalStateException("It has no comments") }
            .onSuccess { threadTokens[id] = it }
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

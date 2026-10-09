package com.wavvy.app.core.history

// Android context
import android.content.Context
// Project resources
import com.wavvy.app.core.innertube.InnerTubeClient
import com.wavvy.app.core.innertube.currentSession

// How long a listening must be to have its place kept, ten minutes, shorter ones are songs and start over
private const val EpisodeMinimumMillis = 10 * 60 * 1000L
private const val MillisPerSecond = 1000L

// Tells YouTube Music what was listened to here, so it shows in the history of the account, nothing is sent for a guest
object YouTubeHistory {
    // Registers a listening of the video, a failure is ignored since the history of the device already has it
    suspend fun report(context: Context, videoId: String) {
        val session = currentSession(context)
        if (session.cookies.isNullOrBlank()) return

        InnerTubeClient.registerPlayback(session, videoId)
    }

    // Tells where a long listening stands, only for what is long enough to be an episode, which is what resumes where it stopped
    suspend fun reportPosition(context: Context, videoId: String, positionMs: Long, lengthMs: Long) {
        // A length that is not known yet counts as long when the place is already far, since a song is never listened to that far in one go
        val isLong = if (lengthMs > 0L) lengthMs >= EpisodeMinimumMillis else positionMs >= EpisodeMinimumMillis
        if (!isLong || positionMs <= 0L) return

        val session = currentSession(context)
        if (session.cookies.isNullOrBlank()) return

        InnerTubeClient.reportWatchTime(session, videoId, positionMs / MillisPerSecond, maxOf(lengthMs, positionMs) / MillisPerSecond)
    }
}

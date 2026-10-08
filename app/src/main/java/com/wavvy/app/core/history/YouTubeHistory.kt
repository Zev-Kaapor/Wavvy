package com.wavvy.app.core.history

// Android context
import android.content.Context
// Project resources
import com.wavvy.app.core.innertube.InnerTubeClient
import com.wavvy.app.core.innertube.currentSession

// Tells YouTube Music what was listened to here, so it shows in the history of the account, nothing is sent for a guest
object YouTubeHistory {
    // Registers a listening of the video, a failure is ignored since the history of the device already has it
    suspend fun report(context: Context, videoId: String) {
        val session = currentSession(context)
        if (session.cookies.isNullOrBlank()) return

        InnerTubeClient.registerPlayback(session, videoId)
    }
}

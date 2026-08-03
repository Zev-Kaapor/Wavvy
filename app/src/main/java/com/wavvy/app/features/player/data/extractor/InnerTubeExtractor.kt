package com.wavvy.app.features.player.data.extractor

// Android log utilities
import android.util.Log
// Domain models
import com.wavvy.app.features.player.ui.components.QueueSong

// InnerTube extractor integration
object InnerTubeExtractor {

    // Fetch queue sequence from InnerTube client
    fun fetchQueue(videoId: String, limit: Int): List<QueueSong> {
        try {
            val (innertubeSongs, _) = InnerTubeClient.fetchNextQueue(
                videoId = videoId,
                excludeVideoId = videoId
            )
            if (innertubeSongs.isNotEmpty()) {
                val filtered = innertubeSongs
                    .filterVideoSongs(disableVideos = true)
                    .filterShortsAndPodcasts()
                    .filter { it.durationSeconds < 600L }

                if (filtered.isNotEmpty()) {
                    return filtered.shuffled().take(limit)
                }
            }
        } catch (_: Exception) {
            Log.e("WavvyExtractor", "InnerTube extraction failed")
        }
        return emptyList()
    }
}

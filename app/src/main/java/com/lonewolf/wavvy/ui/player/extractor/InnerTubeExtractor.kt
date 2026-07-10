package com.lonewolf.wavvy.ui.player.extractor

// Project models
import com.lonewolf.wavvy.ui.player.components.QueueSong

// InnerTube extractor integration
object InnerTubeExtractor {

    // Fetch queue sequence from InnerTube client
    fun fetchQueue(videoId: String, limit: Int): List<QueueSong> {
        try {
            val (innertubeSongs, _) = InnerTubeClient.fetchNextQueue(videoId)
            if (innertubeSongs.isNotEmpty()) {
                val filtered = innertubeSongs
                    .filterVideoSongs(disableVideos = true)
                    .filterShortsAndPodcasts()
                    .filter { it.durationSeconds < 600L }

                if (filtered.isNotEmpty()) {
                    return filtered.take(limit)
                }
            }
        } catch (_: Exception) {
            android.util.Log.e("WavvyExtractor", "InnerTube extraction failed, bypassing to NewPipe")
        }
        return emptyList()
    }
}

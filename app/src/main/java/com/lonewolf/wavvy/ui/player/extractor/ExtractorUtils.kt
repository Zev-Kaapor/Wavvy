package com.lonewolf.wavvy.ui.player.extractor

// Project models
import com.lonewolf.wavvy.ui.player.components.QueueSong
import java.util.Locale

// Content filters for video tracks
fun List<QueueSong>.filterVideoSongs(disableVideos: Boolean = false): List<QueueSong> {
    return if (disableVideos) filterNot { it.isVideoSong } else this
}

// Content filters for shorts and podcasts
fun List<QueueSong>.filterShortsAndPodcasts(): List<QueueSong> {
    return filterNot { it.isEpisode || it.isPodcast || it.id.startsWith("SS") }
}

// Format raw duration seconds into timestamp string
fun formatDuration(seconds: Long): String {
    val hrs = seconds / 3600
    val mins = (seconds % 3600) / 60
    val secs = seconds % 60
    return if (hrs > 0) {
        String.format(Locale.US, "%02d:%02d:%02d", hrs, mins, secs)
    } else {
        String.format(Locale.US, "%02d:%02d", mins, secs)
    }
}

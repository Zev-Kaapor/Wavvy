package com.lonewolf.wavvy.ui.player.extractor

// Third-party extractors (NewPipe wrappers)
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.stream.StreamInfo
import org.schabi.newpipe.extractor.stream.StreamInfoItem
// Project models
import com.lonewolf.wavvy.ui.player.components.QueueSong

// NewPipe extractor integration
object NewPipeExtractor {

    @Volatile
    var isReady = false
        private set

    @Volatile
    private var isInitializing = false

    // Primary extractor init
    fun init() {
        if (isReady || isInitializing) return
        isInitializing = true
        try {
            NewPipeDownloader.init()
            NewPipe.init(NewPipeDownloader.getInstance())
            isReady = true
        } catch (_: Exception) {
            // ignore init errors here
        } finally {
            isInitializing = false
        }
    }

    // Try NewPipe link extraction
    fun extractUrl(videoId: String): String? {
        return try {
            val url = "https://music.youtube.com/watch?v=$videoId"
            val streamInfo = StreamInfo.getInfo(org.schabi.newpipe.extractor.ServiceList.YouTube, url)

            // Prefer audioStreams if present
            val audioStreams = try {
                streamInfo.audioStreams
            } catch (_: Throwable) {
                null
            }

            if (!audioStreams.isNullOrEmpty()) {
                val best = audioStreams.maxByOrNull { it.averageBitrate }
                best?.url
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    // Existing queue extraction via NewPipe
    fun fetchQueue(videoId: String, limit: Int): List<QueueSong> {
        if (!isReady) {
            init()
        }

        try {
            val url = "https://music.youtube.com/watch?v=$videoId"
            val streamInfo = StreamInfo.getInfo(org.schabi.newpipe.extractor.ServiceList.YouTube, url)
            val relatedItems = streamInfo.relatedItems ?: emptyList()

            val primaryQueue = relatedItems.filterIsInstance<StreamInfoItem>()
                .mapNotNull { item: StreamInfoItem ->
                    val extractedId = item.url?.substringAfter("v=", "") ?: ""
                    if (extractedId.isBlank()) return@mapNotNull null

                    val duration = item.duration
                    val realThumb = item.thumbnails.maxByOrNull { it.height }?.url ?: ""

                    QueueSong(
                        id = extractedId,
                        title = item.name ?: "",
                        artist = item.uploaderName ?: "",
                        imageUrl = realThumb,
                        durationSeconds = duration,
                        isVideoSong = false,
                        isEpisode = false,
                        isPodcast = false
                    )
                }

            if (primaryQueue.isNotEmpty()) {
                val filtered = primaryQueue
                    .filterVideoSongs(disableVideos = true)
                    .filterShortsAndPodcasts()
                    .filter { it.durationSeconds < 600L }

                return filtered.take(limit)
            }
        } catch (_: Exception) {
            android.util.Log.e("WavvyExtractor", "NewPipe queue extraction failed")
        }
        return emptyList()
    }
}

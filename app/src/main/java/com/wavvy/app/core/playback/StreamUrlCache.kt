package com.wavvy.app.core.playback

// Android utilities
import androidx.annotation.OptIn
import androidx.core.net.toUri
// Media3 data
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSpec

// Request for the audio of a video turned into a request for its link, cut in pieces when YouTube asks for that, adapted from Metrolist (GPL-3.0)
@OptIn(UnstableApi::class)
internal fun DataSpec.withResolvedStream(stream: ResolvedStream): DataSpec {
    val resolved = withUri(stream.url.toUri()).withRequestHeaders(httpRequestHeaders + stream.headers)
    if ((!stream.requireBoundedRange && !stream.useRangeChunks) || stream.rangeChunkSizeBytes <= 0L) return resolved

    val boundedLength = if (length == C.LENGTH_UNSET.toLong()) stream.rangeChunkSizeBytes else minOf(length, stream.rangeChunkSizeBytes)
    return resolved.subrange(0, boundedLength)
}

// Links already found, kept until they expire so the pieces of the same song do not ask for a new one, adapted from Metrolist (GPL-3.0)
internal class StreamUrlCache(private val maxEntries: Int = DefaultMaxEntries) {
    private class Entry(
        val stream: ResolvedStream,
        val expiresAtMillis: Long
    )

    // Least recently used order, the oldest leaves when it is full
    private val entries = object : LinkedHashMap<String, Entry>(0, LoadFactor, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Entry>): Boolean = size > maxEntries
    }

    // Link of a video while it is still valid
    operator fun get(mediaId: String): ResolvedStream? =
        synchronized(entries) {
            val entry = entries[mediaId] ?: return@synchronized null
            if (entry.expiresAtMillis <= System.currentTimeMillis()) {
                entries.remove(mediaId)
                null
            } else {
                entry.stream
            }
        }

    // Keeps a link for as long as YouTube says it lasts
    fun put(mediaId: String, stream: ResolvedStream) {
        val ttlMillis = stream.expiresInSeconds.coerceAtLeast(0).toLong() * MillisPerSecond
        synchronized(entries) { entries[mediaId] = Entry(stream, System.currentTimeMillis() + ttlMillis) }
    }

    // Forgets the link of a video, after YouTube refused it
    fun invalidate(mediaId: String) {
        synchronized(entries) { entries.remove(mediaId) }
    }

    private companion object {
        const val DefaultMaxEntries = 500
        const val LoadFactor = 0.75f
        const val MillisPerSecond = 1000L
    }
}

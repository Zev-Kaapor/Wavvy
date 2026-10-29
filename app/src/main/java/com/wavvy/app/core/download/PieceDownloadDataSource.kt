package com.wavvy.app.core.download

// Android and media
import android.net.Uri
import androidx.core.net.toUri
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.BaseDataSource
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import java.io.IOException
// Project resources
import com.wavvy.app.core.playback.ResolvedStream

// Reads a whole file from YouTube as a single stream, but asks it in pieces, each a request of its own,
// YouTube slows down a request that goes on for long to the speed of the playing, which makes a file of hours take hours to come
@UnstableApi
class PieceDownloadDataSource(
    private val http: DataSource,
    private val linkOf: (String) -> ResolvedStream
) : BaseDataSource(true) {
    private var spec: DataSpec? = null
    private var mediaId = ""
    private var position = 0L
    private var end = 0L
    private var pieceEnd = 0L
    private var isPieceOpen = false
    private var isWhole = false
    private var isOpen = false

    override fun open(dataSpec: DataSpec): Long {
        spec = dataSpec
        mediaId = dataSpec.key ?: throw IOException("No media id")
        transferInitializing(dataSpec)

        val stream = linkOf(mediaId)
        val length = stream.contentLengthBytes

        // A link that already asks a range, or a file of an unknown size, goes in a single request as it is
        isWhole = length == null || "&range=" in stream.url
        val opened = if (isWhole) {
            http.open(dataSpec.withUri(stream.url.toUri()).withRequestHeaders(dataSpec.httpRequestHeaders + stream.headers))
        } else {
            position = dataSpec.position
            end = if (dataSpec.length == C.LENGTH_UNSET.toLong()) length!! else minOf(length!!, position + dataSpec.length)
            end - position
        }

        isOpen = true
        transferStarted(dataSpec)
        return opened
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        if (length == 0) return 0

        if (isWhole) {
            val read = http.read(buffer, offset, length)
            if (read > 0) bytesTransferred(read)
            return read
        }

        if (position >= end) return C.RESULT_END_OF_INPUT
        if (!isPieceOpen) openPiece()

        val read = http.read(buffer, offset, minOf(length.toLong(), pieceEnd - position).toInt())
        if (read == C.RESULT_END_OF_INPUT) throw IOException("A piece of the file ended before its end")

        position += read
        bytesTransferred(read)
        if (position >= pieceEnd) closePiece()
        return read
    }

    // Asks the next piece with the link of the song, which is kept until it is about to expire, so each piece does not look for it again
    private fun openPiece() {
        val stream = linkOf(mediaId)
        pieceEnd = minOf(position + PieceBytes, end)

        val piece = DataSpec.Builder()
            .setUri("${stream.url}&range=$position-${pieceEnd - 1}".toUri())
            .setHttpRequestHeaders(spec?.httpRequestHeaders.orEmpty() + stream.headers)
            .build()
        http.open(piece)
        isPieceOpen = true
    }

    private fun closePiece() {
        if (isPieceOpen) http.close()
        isPieceOpen = false
    }

    override fun getUri(): Uri? = spec?.uri

    override fun close() {
        try {
            if (isWhole) http.close() else closePiece()
        } finally {
            if (isOpen) {
                isOpen = false
                transferEnded()
            }
        }
    }

    private companion object {
        // How much each piece asks, big enough for the request to cost little and small enough to go at full speed
        const val PieceBytes = 8L * 1024 * 1024
    }
}

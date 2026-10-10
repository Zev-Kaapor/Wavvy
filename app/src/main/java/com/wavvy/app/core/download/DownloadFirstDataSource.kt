package com.wavvy.app.core.download

// Android media
import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.TransferListener
import androidx.media3.datasource.cache.Cache
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.ContentMetadata

// Reads a song from the downloads only when the whole of it is there, in any other case it goes to the internet and never touches the files of the downloads
// A song that is half downloaded has the bytes of the quality of the download, which are not the ones of the quality that is playing, so mixing them breaks the sound
@OptIn(UnstableApi::class)
internal class DownloadFirstDataSource(
    private val cache: Cache,
    private val fromCache: DataSource,
    private val fromNetwork: DataSource
) : DataSource {
    private var current: DataSource? = null

    override fun addTransferListener(transferListener: TransferListener) {
        fromCache.addTransferListener(transferListener)
        fromNetwork.addTransferListener(transferListener)
    }

    override fun open(dataSpec: DataSpec): Long {
        val key = dataSpec.key
        val length = key?.let { ContentMetadata.getContentLength(cache.getContentMetadata(it)) } ?: C.LENGTH_UNSET.toLong()
        val isDownloaded = key != null && length > 0L && cache.isCached(key, 0L, length)

        return (if (isDownloaded) fromCache else fromNetwork).also { current = it }.open(dataSpec)
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int = checkNotNull(current).read(buffer, offset, length)

    override fun getUri(): Uri? = current?.uri

    override fun getResponseHeaders(): Map<String, List<String>> = current?.responseHeaders.orEmpty()

    override fun close() {
        current?.close()
        current = null
    }

    companion object {
        // The source of the player, the files of the downloads when the song is all there and the internet when it is not
        fun factory(cache: Cache, network: DataSource.Factory): DataSource.Factory {
            val cached = CacheDataSource.Factory()
                .setCache(cache)
                .setCacheWriteDataSinkFactory(null)
                .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
                .setUpstreamDataSourceFactory(network)

            return DataSource.Factory { DownloadFirstDataSource(cache, cached.createDataSource(), network.createDataSource()) }
        }
    }
}

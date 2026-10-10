package com.wavvy.app.core.download

// Android context and files
import android.content.Context
import android.net.Uri
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
// Coroutines and reactive flows
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import com.wavvy.app.core.innertube.imageBase
import com.wavvy.app.core.innertube.resize

// The pictures of the downloads, kept in files of the app with the songs, so the covers show without the internet and are never lost
// The picture is a file named by its address, shared by every song that has the same one, and the ones nobody uses any more are deleted
object DownloadArt {
    private val mutableVersion = MutableStateFlow(0)

    // Goes up whenever a picture is saved, so the lists that show them draw again
    val version: StateFlow<Int> = mutableVersion.asStateFlow()

    // The address to show for a picture, the file when it is saved and the address itself while it is not
    fun localOrRemote(context: Context, url: String?): String? {
        if (url == null) return null
        val file = fileOf(context, url)
        return if (file.exists() && file.length() > 0L) Uri.fromFile(file).toString() else url
    }

    // Saves the picture when it is not yet, a failure leaves the address to be shown from the internet as before
    suspend fun ensure(context: Context, url: String?) {
        if (url == null || !url.startsWith(HttpPrefix)) return
        val file = fileOf(context, url)
        if (file.exists() && file.length() > 0L) return

        withContext(Dispatchers.IO) {
            runCatching {
                // Saved big enough for the biggest cover of the app, the smaller ones are cut from it
                val connection = URL(url.resize(ArtSize, ArtSize)).openConnection() as HttpURLConnection
                try {
                    connection.connectTimeout = TimeoutMillis
                    connection.readTimeout = TimeoutMillis
                    if (connection.responseCode != HttpOk) return@runCatching
                    val temporary = File(file.parentFile, file.name + TemporarySuffix)
                    connection.inputStream.use { input -> temporary.outputStream().use { output -> input.copyTo(output) } }
                    if (temporary.renameTo(file)) mutableVersion.update { it + 1 }
                } finally {
                    connection.disconnect()
                }
            }
        }
    }

    // Deletes the pictures that no download uses any more
    fun prune(context: Context, inUse: Set<String>) {
        val keep = inUse.map { nameOf(it) }.toSet()
        directory(context).listFiles()?.filter { it.name !in keep }?.forEach { it.delete() }
    }

    private fun fileOf(context: Context, url: String) = File(directory(context), nameOf(url))

    private fun directory(context: Context) = File(context.applicationContext.filesDir, DirectoryName).also { it.mkdirs() }

    private fun nameOf(url: String): String =
        MessageDigest.getInstance("SHA-256").digest(url.imageBase().toByteArray()).joinToString("") { "%02x".format(it) }.take(NameLength)

    private const val ArtSize = 720
    private const val DirectoryName = "download_art"
    private const val TemporarySuffix = ".part"
    private const val HttpPrefix = "http"
    private const val HttpOk = 200
    private const val TimeoutMillis = 15_000
    private const val NameLength = 32
}

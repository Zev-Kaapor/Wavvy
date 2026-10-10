package com.wavvy.app.core.innertube

// Android context and network
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
// Files and hashes
import java.io.File
import java.security.MessageDigest

// The last answer of each page of YouTube Music, kept in files so the library and the pages that were opened can be navigated without the internet
// A page is asked again whenever there is a connection, and only when there is none or the request fails the kept answer is used
object OfflineCache {
    @Volatile
    private var directory: File? = null

    @Volatile
    private var connectivity: ConnectivityManager? = null

    // Keeps the folder and the way to ask the network, called once when the app starts
    fun initialize(context: Context) {
        if (directory != null) return

        val app = context.applicationContext
        directory = File(app.filesDir, DirectoryName).also { it.mkdirs() }
        connectivity = app.getSystemService(ConnectivityManager::class.java)
    }

    // True when the device has a network that reaches the internet
    fun isOnline(): Boolean {
        val manager = connectivity ?: return true
        val capabilities = manager.getNetworkCapabilities(manager.activeNetwork) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    // The name of the file of a page, the same for the same page and the same account language
    fun keyOf(vararg parts: String?): String =
        MessageDigest.getInstance("SHA-256")
            .digest(parts.joinToString("|") { it.orEmpty() }.toByteArray())
            .joinToString("") { "%02x".format(it) }

    fun get(key: String): String? = directory?.let { File(it, key) }?.takeIf { it.exists() }?.readText()

    fun put(key: String, text: String) {
        val folder = directory ?: return
        runCatching {
            File(folder, key).writeText(text)
            trim(folder)
        }
    }

    // Takes everything out, when the account leaves, so the next one never sees the pages of the first
    fun clear() {
        directory?.listFiles()?.forEach { it.delete() }
    }

    // The oldest answers leave when the folder is too big
    private fun trim(folder: File) {
        val files = folder.listFiles() ?: return
        var total = files.sumOf { it.length() }
        if (total <= MaxBytes) return

        files.sortedBy { it.lastModified() }.forEach { file ->
            if (total <= MaxBytes) return
            total -= file.length()
            file.delete()
        }
    }

    private const val DirectoryName = "pages"
    private const val MaxBytes = 60L * 1024 * 1024
}

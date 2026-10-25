package com.wavvy.app.features.updates.data

// Network and JSON
import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONObject
// Coroutines
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// A file of a release that can be downloaded, its name, where it is and how big it is
data class UpdateAsset(val name: String, val url: String, val sizeBytes: Long)

// One thing that changed, its title and the description under it when the commit had one
data class UpdateNote(val title: String, val details: String?)

// A release of the app, its version, what changed in it, when it came out, the page of it and the file that fits the device
data class UpdateInfo(
    val version: String,
    val notes: List<UpdateNote>,
    val publishedAt: String?,
    val pageUrl: String,
    val asset: UpdateAsset?
)

// Asks GitHub for the last release of the app and tells if it is newer than the one installed
object UpdateRepository {
    // The last release, with the file of the processor of the device or the one that works on any device
    suspend fun latest(abis: List<String>): Result<UpdateInfo> = withContext(Dispatchers.IO) {
        runCatching {
            val connection = URL(LatestUrl).openConnection() as HttpURLConnection
            try {
                connection.connectTimeout = TimeoutMillis
                connection.readTimeout = TimeoutMillis
                connection.setRequestProperty("Accept", "application/vnd.github+json")
                connection.setRequestProperty("User-Agent", UserAgent)
                if (connection.responseCode != HttpOk) throw IllegalStateException("GitHub answered ${connection.responseCode}")

                parse(JSONObject(connection.inputStream.bufferedReader().use { it.readText() }), abis)
            } finally {
                connection.disconnect()
            }
        }
    }

    // True when the first version is bigger than the second, compared number by number as in 0.11.0 and 0.9.2
    fun isNewer(latest: String, current: String): Boolean {
        val a = numbersOf(latest)
        val b = numbersOf(current)
        for (index in 0 until maxOf(a.size, b.size)) {
            val x = a.getOrElse(index) { 0 }
            val y = b.getOrElse(index) { 0 }
            if (x != y) return x > y
        }
        return false
    }

    private fun numbersOf(version: String): List<Int> = version.trim().removePrefix("v").split('.').map { it.takeWhile(Char::isDigit).toIntOrNull() ?: 0 }

    private fun parse(json: JSONObject, abis: List<String>): UpdateInfo {
        val assets = json.optJSONArray("assets")?.let { array ->
            (0 until array.length()).mapNotNull { index ->
                val asset = array.optJSONObject(index) ?: return@mapNotNull null
                val name = asset.optString("name")
                val url = asset.optString("browser_download_url")
                if (name.endsWith(".apk") && url.isNotEmpty()) UpdateAsset(name, url, asset.optLong("size")) else null
            }
        }.orEmpty()

        // The file of the first processor of the device that has one, and the one for any device when none fits
        val asset = abis.firstNotNullOfOrNull { abi -> assets.firstOrNull { it.name.contains("-$abi") } } ?: assets.firstOrNull { it.name.contains(UniversalMark) }

        return UpdateInfo(
            version = json.optString("tag_name").removePrefix("v"),
            notes = notesOf(json.optString("body")),
            publishedAt = json.optString("published_at").takeIf { it.isNotEmpty() },
            pageUrl = json.optString("html_url"),
            asset = asset
        )
    }

    // The lines of what changed, which come after its title in the text of the release, without the mark of the list, the type of the commit, its code and the version it ends with
    // A line that starts the list is a title, the lines indented under it are its description
    private fun notesOf(body: String): List<UpdateNote> {
        val lines = body.lines()
        val start = lines.indexOfFirst { it.trim().startsWith(NotesTitle) }
        if (start < 0) return emptyList()

        val notes = mutableListOf<UpdateNote>()
        for (line in lines.drop(start + 1)) {
            val text = line.trim()
            if (text.startsWith("#") || text.startsWith("**")) break

            if (line.startsWith("- ")) {
                val title = line.removePrefix("- ").replace(CommitCode, "").replace(CommitType, "").replace(VersionTail, "").trim()
                if (title.isNotEmpty()) notes += UpdateNote(title, null)
            } else if (line.startsWith("  ") && text.isNotEmpty() && notes.isNotEmpty()) {
                val last = notes.removeAt(notes.lastIndex)
                notes += last.copy(details = listOfNotNull(last.details, text).joinToString(" "))
            }
        }
        return notes
    }

    private val CommitCode = Regex("\\s*\\(`[0-9a-f]+`\\)\\s*$")
    private val CommitType = Regex("^[a-z]+(\\([^)]*\\))?!?:\\s*")
    private val VersionTail = Regex(",?\\s*version\\s+[0-9.]+\\s*$")
    private const val NotesTitle = "### What's changed"
    private const val LatestUrl = "https://api.github.com/repos/Zev-Kaapor/Wavvy/releases/latest"
    private const val UniversalMark = "universal"
    private const val UserAgent = "Wavvy"
    private const val TimeoutMillis = 20_000
    private const val HttpOk = 200
}

package com.wavvy.app.features.auth.data

// Android context and DataStore
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore

// Domain models
import com.wavvy.app.features.auth.models.AccountData
import com.wavvy.app.features.home.models.QuickPick
import com.wavvy.app.features.home.ui.components.ForgottenTrack

// Coroutines and reactive flows
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

// JSON utilities
import org.json.JSONArray
import org.json.JSONObject

// Network and security utilities
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

// DataStore preferences keys
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "auth_session")
private val SessionTokenKey = stringPreferencesKey("session_token")

private val VisitorDataKey = stringPreferencesKey("guest_visitor_data")

// Authentication repository implementation
class AuthRepositoryImpl(
    private val context: Context
) : AuthRepository {

    val savedAccountsManager = SavedAccountsManager(context)

    // Visitor data persistence
    private suspend fun getVisitorData(): String? {
        return context.dataStore.data
            .map { preferences -> preferences[VisitorDataKey] }
            .firstOrNull()
    }

    private suspend fun saveVisitorData(visitorData: String) {
        if (visitorData.isBlank()) return
        context.dataStore.edit { preferences ->
            preferences[VisitorDataKey] = visitorData
        }
    }

    // Session management
    override suspend fun signInWithGoogle(cookies: String): Result<Unit> {
        return try {
            context.dataStore.edit { preferences ->
                preferences[SessionTokenKey] = cookies
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getSessionToken(): String? {
        return context.dataStore.data
            .map { preferences -> preferences[SessionTokenKey] }
            .firstOrNull()
    }

    override suspend fun logout() {
        context.dataStore.edit { preferences ->
            preferences.remove(SessionTokenKey)
        }
    }

    // Network security helpers
    private fun generateSapiSidHash(sapisid: String): String {
        val timestamp = (System.currentTimeMillis() / 1000).toString()
        val message = "$timestamp $sapisid https://music.youtube.com"

        val digest = MessageDigest.getInstance("SHA-1")
        val hashBytes = digest.digest(message.toByteArray(Charsets.UTF_8))
        val hashString = hashBytes.joinToString("") { "%02x".format(it) }

        return "${timestamp}_${hashString}"
    }

    private fun extractCookieValue(cookieString: String, key: String): String? {
        val pattern = "$key=([^;]+)".toRegex()
        return pattern.find(cookieString)?.groupValues?.get(1)
    }

    // Account details fetching
    override suspend fun fetchAuthenticatedAccountDetails(): AccountData? =
        withContext(Dispatchers.IO) {
            fetchAccountDetailsWithCookies(getAuthCookie())
        }

    suspend fun fetchAccountDetailsWithCookies(sessionCookie: String?): AccountData? =
        withContext(Dispatchers.IO) {
            try {
                val url = URL("https://music.youtube.com/youtubei/v1/account/account_menu")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.doOutput = true

                connection.setRequestProperty("Content-Type", "application/json")
                connection.setRequestProperty(
                    "User-Agent",
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64)"
                )
                connection.setRequestProperty("X-Origin", "https://music.youtube.com")

                if (!sessionCookie.isNullOrEmpty()) {
                    connection.setRequestProperty("Cookie", sessionCookie)

                    val sapisid = extractCookieValue(sessionCookie, "SAPISID")
                        ?: extractCookieValue(sessionCookie, "__Secure-1PAPISID")
                        ?: extractCookieValue(sessionCookie, "__Secure-3PAPISID")

                    if (!sapisid.isNullOrEmpty()) {
                        val hash = generateSapiSidHash(sapisid)
                        connection.setRequestProperty("Authorization", "SAPISIDHASH $hash")
                    }
                }

                val visitorData = getVisitorData()
                val payload = JSONObject().apply {
                    put("context", JSONObject().apply {
                        put("client", JSONObject().apply {
                            put("clientName", "WEB_REMIX")
                            put("clientVersion", "1.20260615.01.00")
                            if (!visitorData.isNullOrEmpty()) {
                                put("visitorData", visitorData)
                            }
                        })
                    })
                }

                OutputStreamWriter(connection.outputStream).use { writer ->
                    writer.write(payload.toString())
                    writer.flush()
                }

                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    val responseString =
                        connection.inputStream.bufferedReader().use { it.readText() }
                    val rootJson = JSONObject(responseString)

                    val responseVisitorData =
                        rootJson.optJSONObject("responseContext")?.optString("visitorData")
                    if (!responseVisitorData.isNullOrBlank()) {
                        saveVisitorData(responseVisitorData)
                    }

                    val actions = rootJson.getJSONArray("actions")
                    val openPopupAction = actions.getJSONObject(0).getJSONObject("openPopupAction")
                    val multiPageMenuRenderer = openPopupAction.getJSONObject("popup")
                        .getJSONObject("multiPageMenuRenderer")
                    val header = multiPageMenuRenderer.optJSONObject("header")

                    val activeAccountHeaderRenderer =
                        header?.optJSONObject("activeAccountHeaderRenderer")
                    if (activeAccountHeaderRenderer != null) {
                        val nameRuns = activeAccountHeaderRenderer.getJSONObject("accountName")
                            .getJSONArray("runs")
                        val extractedName = nameRuns.getJSONObject(0).getString("text")

                        val handleObj = activeAccountHeaderRenderer.optJSONObject("channelHandle")
                        val handleRuns = handleObj?.optJSONArray("runs")
                        val extractedHandle = if (handleRuns != null && handleRuns.length() > 0) {
                            handleRuns.getJSONObject(0).getString("text")
                        } else ""

                        val photoObject = activeAccountHeaderRenderer.optJSONObject("accountPhoto")
                        val thumbnails = photoObject?.optJSONArray("thumbnails")
                        val extractedPhotoUrl = if (thumbnails != null && thumbnails.length() > 0) {
                            thumbnails.getJSONObject(0).getString("url")
                        } else null

                        val accountData = AccountData(
                            name = extractedName,
                            handle = extractedHandle,
                            pictureUrl = extractedPhotoUrl
                        )

                        // Cached account details storage
                        context.dataStore.edit { prefs ->
                            prefs[stringPreferencesKey("cached_account_name")] = extractedName
                            prefs[stringPreferencesKey("cached_account_handle")] = extractedHandle
                            prefs[stringPreferencesKey("cached_account_picture")] = extractedPhotoUrl ?: ""
                        }

                        return@withContext accountData
                    }
                }

                null
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }

    private fun getAuthCookie(): String? = runBlocking {
        return@runBlocking getSessionToken()
    }

    // Recommendations and history fetching
    override suspend fun fetchQuickPicks(): List<QuickPick> = withContext(Dispatchers.IO) {
        fetchQuickPicksWithCookies(getAuthCookie())
    }

    override suspend fun fetchForgottenFavorites(): List<ForgottenTrack> = withContext(Dispatchers.IO) {
        emptyList()
    }

    // Genre-based search
    override suspend fun fetchSongsByGenre(genre: String): List<QuickPick> = withContext(Dispatchers.IO) {
        fetchGuestSongHits(getAuthCookie(), genre)
    }

    suspend fun fetchQuickPicksWithCookies(sessionCookie: String?): List<QuickPick> =
        withContext(Dispatchers.IO) {
            try {
                val url = URL("https://music.youtube.com/youtubei/v1/browse")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.doOutput = true

                connection.setRequestProperty("Content-Type", "application/json")
                connection.setRequestProperty(
                    "User-Agent",
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64)"
                )
                connection.setRequestProperty("X-Origin", "https://music.youtube.com")

                if (!sessionCookie.isNullOrEmpty()) {
                    connection.setRequestProperty("Cookie", sessionCookie)

                    val sapisid = extractCookieValue(sessionCookie, "SAPISID")
                        ?: extractCookieValue(sessionCookie, "__Secure-1PAPISID")
                        ?: extractCookieValue(sessionCookie, "__Secure-3PAPISID")

                    if (!sapisid.isNullOrEmpty()) {
                        val hash = generateSapiSidHash(sapisid)
                        connection.setRequestProperty("Authorization", "SAPISIDHASH $hash")
                    }
                }

                val visitorData = getVisitorData()
                val clientObj = JSONObject().apply {
                    put("clientName", "WEB_REMIX")
                    put("clientVersion", "1.20260615.01.00")
                    put("hl", "pt")
                    put("gl", "BR")
                    if (!visitorData.isNullOrEmpty()) {
                        put("visitorData", visitorData)
                    }
                }

                val payload = JSONObject().apply {
                    put("browseId", "FEmusic_home")
                    put("context", JSONObject().apply {
                        put("client", clientObj)
                    })
                }

                OutputStreamWriter(connection.outputStream).use { writer ->
                    writer.write(payload.toString())
                    writer.flush()
                }

                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    val responseString =
                        connection.inputStream.bufferedReader().use { it.readText() }
                    val rootJson = JSONObject(responseString)

                    val responseVisitorData =
                        rootJson.optJSONObject("responseContext")?.optString("visitorData")
                    if (!responseVisitorData.isNullOrBlank()) {
                        saveVisitorData(responseVisitorData)
                    }

                    val homePicks = parseQuickPicksFromHome(rootJson)
                    val validSongs = homePicks.filter { isRealSongVideoId(it.videoId) }

                    if (validSongs.size >= 4) {
                        return@withContext validSongs
                    } else {
                        // Unauthenticated search fallback
                        val guestHits = fetchGuestSongHits(sessionCookie)
                        if (guestHits.isNotEmpty()) {
                            return@withContext guestHits
                        }
                        return@withContext validSongs
                    }
                }
                fetchGuestSongHits(sessionCookie)
            } catch (e: Exception) {
                e.printStackTrace()
                fetchGuestSongHits(sessionCookie)
            }
        }

    // Video ID validation helper
    private fun isRealSongVideoId(id: String?): Boolean {
        if (id.isNullOrBlank()) return false
        if (id.length != 11) return false
        if (id.startsWith("MPREb") || id.startsWith("VL") || id.startsWith("RD") || id.startsWith("PL") || id.startsWith("UC")) {
            return false
        }
        return true
    }

    // Guest search request handler
    private suspend fun fetchGuestSongHits(sessionCookie: String?, query: String = "music"): List<QuickPick> =
        withContext(Dispatchers.IO) {
            try {
                val url = URL("https://music.youtube.com/youtubei/v1/search")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.doOutput = true

                connection.setRequestProperty("Content-Type", "application/json")
                connection.setRequestProperty(
                    "User-Agent",
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64)"
                )
                connection.setRequestProperty("X-Origin", "https://music.youtube.com")

                if (!sessionCookie.isNullOrEmpty()) {
                    connection.setRequestProperty("Cookie", sessionCookie)
                }

                val visitorData = getVisitorData()
                val clientObj = JSONObject().apply {
                    put("clientName", "WEB_REMIX")
                    put("clientVersion", "1.20260615.01.00")
                    put("hl", "pt")
                    put("gl", "BR")
                    if (!visitorData.isNullOrEmpty()) {
                        put("visitorData", visitorData)
                    }
                }

                // Song filter payload setup
                val payload = JSONObject().apply {
                    put("query", query)
                    put("params", "EgWKAQIIAWoQEAMQBBAJEAoQCRAKEAYQAQ==")
                    put("context", JSONObject().apply {
                        put("client", clientObj)
                    })
                }

                OutputStreamWriter(connection.outputStream).use { writer ->
                    writer.write(payload.toString())
                    writer.flush()
                }

                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    val responseString =
                        connection.inputStream.bufferedReader().use { it.readText() }
                    val rootJson = JSONObject(responseString)

                    val responseVisitorData =
                        rootJson.optJSONObject("responseContext")?.optString("visitorData")
                    if (!responseVisitorData.isNullOrBlank()) {
                        saveVisitorData(responseVisitorData)
                    }

                    return@withContext parseSongsFromSearch(rootJson)
                }
                emptyList()
            } catch (e: Exception) {
                e.printStackTrace()
                emptyList()
            }
        }

    // Search JSON parser
    private fun parseSongsFromSearch(rootJson: JSONObject): List<QuickPick> {
        val quickPicks = mutableListOf<QuickPick>()

        val contents = rootJson.optJSONObject("contents")
            ?.optJSONObject("tabbedSearchResultsRenderer")
            ?.optJSONArray("tabs")
            ?.optJSONObject(0)
            ?.optJSONObject("tabRenderer")
            ?.optJSONObject("content")
            ?.optJSONObject("sectionListRenderer")
            ?.optJSONArray("contents") ?: return emptyList()

        for (i in 0 until contents.length()) {
            val section = contents.optJSONObject(i) ?: continue
            val musicShelf = section.optJSONObject("musicShelfRenderer") ?: continue

            val items = musicShelf.optJSONArray("contents") ?: continue
            for (j in 0 until items.length()) {
                val itemObj = items.optJSONObject(j) ?: continue
                val responsiveItem = itemObj.optJSONObject("musicResponsiveListItemRenderer")
                    ?: continue

                parseQuickPickItem(responsiveItem)?.let { pick ->
                    if (isRealSongVideoId(pick.videoId)) {
                        quickPicks.add(pick)
                    }
                }
            }
        }

        return quickPicks
    }

    // Home feed JSON parser
    private fun parseQuickPicksFromHome(rootJson: JSONObject): List<QuickPick> {
        val sectionListContents = findSectionListContents(rootJson) ?: return emptyList()

        var targetShelf: JSONObject? = null

        // Section header lookup
        for (i in 0 until sectionListContents.length()) {
            val section = sectionListContents.optJSONObject(i) ?: continue
            val shelf = section.optJSONObject("musicCarouselShelfRenderer") ?: continue

            val headerTitle = shelf.optJSONObject("header")
                ?.optJSONObject("musicCarouselShelfBasicHeaderRenderer")
                ?.optJSONObject("title")
                ?.optJSONArray("runs")
                ?.let { runs ->
                    (0 until runs.length()).joinToString("") {
                        runs.getJSONObject(it).optString("text")
                    }
                }
                ?: continue

            if (headerTitle.contains("Escolha a dedo", ignoreCase = true) ||
                headerTitle.contains("Escolhas rápidas", ignoreCase = true) ||
                headerTitle.contains("Quick picks", ignoreCase = true) ||
                headerTitle.contains("Hand-Picked", ignoreCase = true)
            ) {
                targetShelf = shelf
                break
            }
        }

        // Section fallback lookup
        if (targetShelf == null) {
            for (i in 0 until sectionListContents.length()) {
                val section = sectionListContents.optJSONObject(i) ?: continue
                val shelf = section.optJSONObject("musicCarouselShelfRenderer") ?: continue
                val items = shelf.optJSONArray("contents") ?: continue
                if (items.length() > 0) {
                    targetShelf = shelf
                    break
                }
            }
        }

        val shelf = targetShelf ?: return emptyList()
        val items = shelf.optJSONArray("contents") ?: return emptyList()
        val quickPicks = mutableListOf<QuickPick>()

        for (j in 0 until items.length()) {
            val itemObj = items.optJSONObject(j) ?: continue

            val responsiveItem = itemObj.optJSONObject("musicResponsiveListItemRenderer")
            if (responsiveItem != null) {
                parseQuickPickItem(responsiveItem)?.let { quickPicks.add(it) }
                continue
            }

            val twoRowItem = itemObj.optJSONObject("musicTwoRowItemRenderer")
            if (twoRowItem != null) {
                parseTwoRowItem(twoRowItem)?.let { quickPicks.add(it) }
                continue
            }
        }

        return quickPicks
    }

    // Section list contents lookup
    private fun findSectionListContents(rootJson: JSONObject): JSONArray? {
        val tabContent = rootJson.optJSONObject("contents")
            ?.optJSONObject("singleColumnBrowseResultsRenderer")
            ?.optJSONArray("tabs")
            ?.optJSONObject(0)
            ?.optJSONObject("tabRenderer")
            ?.optJSONObject("content")
            ?.optJSONObject("sectionListRenderer")
            ?.optJSONArray("contents")

        if (tabContent != null) return tabContent

        return rootJson.optJSONObject("continuationContents")
            ?.optJSONObject("sectionListContinuation")
            ?.optJSONArray("contents")
    }

    // Single item JSON parser
    private fun parseQuickPickItem(item: JSONObject): QuickPick? {
        val videoId = item.optJSONObject("playlistItemData")?.optString("videoId")
            ?: item.optJSONArray("flexColumns")
                ?.optJSONObject(0)
                ?.optJSONObject("musicResponsiveListItemFlexColumnRenderer")
                ?.optJSONObject("text")
                ?.optJSONArray("runs")
                ?.optJSONObject(0)
                ?.optJSONObject("navigationEndpoint")
                ?.optJSONObject("watchEndpoint")
                ?.optString("videoId")
            ?: item.optJSONObject("overlay")
                ?.optJSONObject("musicItemThumbnailOverlayRenderer")
                ?.optJSONObject("content")
                ?.optJSONObject("musicPlayButtonRenderer")
                ?.optJSONObject("playNavigationEndpoint")
                ?.optJSONObject("watchEndpoint")
                ?.optString("videoId")

        if (!isRealSongVideoId(videoId)) return null

        val flexColumns = item.optJSONArray("flexColumns") ?: return null
        if (flexColumns.length() < 1) return null

        val titleRuns = flexColumns.optJSONObject(0)
            ?.optJSONObject("musicResponsiveListItemFlexColumnRenderer")
            ?.optJSONObject("text")
            ?.optJSONArray("runs")
        val title = titleRuns?.optJSONObject(0)?.optString("text") ?: return null

        val artists = mutableListOf<String>()
        if (flexColumns.length() >= 2) {
            val subtitleRuns = flexColumns.optJSONObject(1)
                ?.optJSONObject("musicResponsiveListItemFlexColumnRenderer")
                ?.optJSONObject("text")
                ?.optJSONArray("runs")

            if (subtitleRuns != null) {
                val groups = splitRunnsBySeparator(subtitleRuns)

                groups.firstOrNull()?.let { artistRuns ->
                    artistRuns
                        .filterIndexed { index, _ -> index % 2 == 0 }
                        .forEach { run ->
                            val artistText = run.optString("text").trim()
                            if (artistText.isNotEmpty() && run.has("navigationEndpoint")) {
                                artists.add(artistText)
                            }
                        }
                }
            }
        }

        val artist = artists.firstOrNull() ?: ""

        val thumbnails = item.optJSONObject("thumbnail")
            ?.optJSONObject("musicThumbnailRenderer")
            ?.optJSONObject("thumbnail")
            ?.optJSONArray("thumbnails")
        val thumbnailUrl = thumbnails?.let { array ->
            if (array.length() > 0) array.optJSONObject(array.length() - 1)
                ?.optString("url") else null
        }

        return QuickPick(
            videoId = videoId!!,
            title = title,
            artist = artist,
            artists = if (artists.isEmpty()) listOf(artist) else artists,
            thumbnailUrl = thumbnailUrl
        )
    }

    // Alternative item format JSON parser
    private fun parseTwoRowItem(item: JSONObject): QuickPick? {
        val titleRuns = item.optJSONObject("title")?.optJSONArray("runs")
        val title = titleRuns?.optJSONObject(0)?.optString("text") ?: return null

        val playButtonEndpoint = item.optJSONObject("thumbnailOverlay")
            ?.optJSONObject("musicItemThumbnailOverlayRenderer")
            ?.optJSONObject("content")
            ?.optJSONObject("musicPlayButtonRenderer")
            ?.optJSONObject("playNavigationEndpoint")

        val navEndpoint = playButtonEndpoint ?: item.optJSONObject("navigationEndpoint")

        val videoId = navEndpoint?.optJSONObject("watchEndpoint")?.optString("videoId")
        if (!isRealSongVideoId(videoId)) return null

        val subtitleRuns = item.optJSONObject("subtitle")?.optJSONArray("runs")
        val artists = mutableListOf<String>()
        if (subtitleRuns != null) {
            for (k in 0 until subtitleRuns.length()) {
                val text = subtitleRuns.optJSONObject(k)?.optString("text", "")?.trim() ?: continue
                if (text.isNotEmpty() && text != "•" && text != "Álbum" && text != "Single" && text != "EP") {
                    artists.add(text)
                }
            }
        }
        val artist = artists.firstOrNull() ?: ""

        val thumbnails = item.optJSONObject("thumbnailRenderer")
            ?.optJSONObject("musicThumbnailRenderer")
            ?.optJSONObject("thumbnail")
            ?.optJSONArray("thumbnails")

        val thumbnailUrl = thumbnails?.let { array ->
            if (array.length() > 0) array.optJSONObject(array.length() - 1)?.optString("url") else null
        }

        return QuickPick(
            videoId = videoId!!,
            title = title,
            artist = artist,
            artists = if (artists.isEmpty()) listOf(artist) else artists,
            thumbnailUrl = thumbnailUrl
        )
    }

    // JSON array separator parsing helper
    private fun splitRunnsBySeparator(runs: JSONArray): List<List<JSONObject>> {
        val result = mutableListOf<List<JSONObject>>()
        var tmp = mutableListOf<JSONObject>()

        for (i in 0 until runs.length()) {
            val run = runs.getJSONObject(i)
            val text = run.optString("text", "").trim()

            if (text == "•") {
                result.add(tmp)
                tmp = mutableListOf()
            } else {
                tmp.add(run)
            }
        }
        result.add(tmp)
        return result
    }
}

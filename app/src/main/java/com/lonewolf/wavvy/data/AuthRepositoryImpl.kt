package com.lonewolf.wavvy.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.lonewolf.wavvy.data.models.AccountData
import com.lonewolf.wavvy.data.models.QuickPick
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

// Storage extension metadata delegation
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "auth_session")
private val SessionTokenKey = stringPreferencesKey("session_token")

// Data layer
class AuthRepositoryImpl(
    private val context: Context
) : AuthRepository {

    val savedAccountsManager = SavedAccountsManager(context)

    // Session management logic goes here
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

    // Generate dynamic security hash required for innerTube private endpoints
    private fun generateSapiSidHash(sapisid: String, origin: String): String {
        val timestamp = (System.currentTimeMillis() / 1000).toString()
        val message = "$timestamp $sapisid $origin"

        val digest = MessageDigest.getInstance("SHA-1")
        val hashBytes = digest.digest(message.toByteArray(Charsets.UTF_8))
        val hashString = hashBytes.joinToString("") { "%02x".format(it) }

        return "${timestamp}_${hashString}"
    }

    // Extract specific token field values from structural header text
    private fun extractCookieValue(cookieString: String, key: String): String? {
        val pattern = "$key=([^;]+)".toRegex()
        return pattern.find(cookieString)?.groupValues?.get(1)
    }

    // Extract real account user details from InnerTube natively
    override suspend fun fetchAuthenticatedAccountDetails(): AccountData? = withContext(Dispatchers.IO) {
        fetchAccountDetailsWithCookies(getAuthCookie())
    }

    // Fetch account details using explicit cookie string
    suspend fun fetchAccountDetailsWithCookies(sessionCookie: String?): AccountData? = withContext(Dispatchers.IO) {
        try {
            val url = URL("https://music.youtube.com/youtubei/v1/account/account_menu")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.doOutput = true

            // Network setup configuration headers
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
            connection.setRequestProperty("X-Origin", "https://music.youtube.com")

            // Inject authorization cookie and calculate required hash tokens
            if (!sessionCookie.isNullOrEmpty()) {
                connection.setRequestProperty("Cookie", sessionCookie)

                val sapisid = extractCookieValue(sessionCookie, "SAPISID")
                    ?: extractCookieValue(sessionCookie, "__Secure-1PAPISID")
                    ?: extractCookieValue(sessionCookie, "__Secure-3PAPISID")

                if (!sapisid.isNullOrEmpty()) {
                    val hash = generateSapiSidHash(sapisid, "https://music.youtube.com")
                    connection.setRequestProperty("Authorization", "SAPISIDHASH $hash")
                }
            }

            // Minimal payload body structure matching InnerTube specifications
            val payload = JSONObject().apply {
                put("context", JSONObject().apply {
                    put("client", JSONObject().apply {
                        put("clientName", "WEB_REMIX")
                        put("clientVersion", "1.20260615.01.00")
                    })
                })
            }

            OutputStreamWriter(connection.outputStream).use { writer ->
                writer.write(payload.toString())
                writer.flush()
            }

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val responseString = connection.inputStream.bufferedReader().use { it.readText() }
                val rootJson = JSONObject(responseString)

                // Pure JSON structural tree traversal
                val actions = rootJson.getJSONArray("actions")
                val openPopupAction = actions.getJSONObject(0).getJSONObject("openPopupAction")
                val multiPageMenuRenderer = openPopupAction.getJSONObject("popup").getJSONObject("multiPageMenuRenderer")
                val header = multiPageMenuRenderer.optJSONObject("header")

                val activeAccountHeaderRenderer = header?.optJSONObject("activeAccountHeaderRenderer")
                if (activeAccountHeaderRenderer != null) {
                    // Extract runs content block text arrays
                    val nameRuns = activeAccountHeaderRenderer.getJSONObject("accountName").getJSONArray("runs")
                    val extractedName = nameRuns.getJSONObject(0).getString("text")

                    // Extract unique profile handle identifier
                    val handleObj = activeAccountHeaderRenderer.optJSONObject("channelHandle")
                    val handleRuns = handleObj?.optJSONArray("runs")
                    val extractedHandle = if (handleRuns != null && handleRuns.length() > 0) {
                        handleRuns.getJSONObject(0).getString("text")
                    } else ""

                    // Profile thumbnail processing resolution extraction
                    val photoObject = activeAccountHeaderRenderer.optJSONObject("accountPhoto")
                    val thumbnails = photoObject?.optJSONArray("thumbnails")
                    val extractedPhotoUrl = if (thumbnails != null && thumbnails.length() > 0) {
                        thumbnails.getJSONObject(0).getString("url")
                    } else null

                    return@withContext AccountData(
                        name = extractedName,
                        handle = extractedHandle,
                        pictureUrl = extractedPhotoUrl
                    )
                }
            }
            null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // Session retrieval abstraction helper
    private fun getAuthCookie(): String? = runBlocking {
        return@runBlocking getSessionToken()
    }

    // Remote data tracking for home screen selections
    override suspend fun fetchQuickPicks(): List<QuickPick> = withContext(Dispatchers.IO) {
        fetchQuickPicksWithCookies(getAuthCookie())
    }

    // Fallback data tracking for unauthenticated contexts
    suspend fun fetchQuickPicksWithCookies(sessionCookie: String?): List<QuickPick> = withContext(Dispatchers.IO) {
        try {
            val url = URL("https://music.youtube.com/youtubei/v1/browse")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.doOutput = true

            // Network setup configuration headers
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
            connection.setRequestProperty("X-Origin", "https://music.youtube.com")

            // Inject authorization cookie and calculate required hash tokens
            if (!sessionCookie.isNullOrEmpty()) {
                connection.setRequestProperty("Cookie", sessionCookie)

                val sapisid = extractCookieValue(sessionCookie, "SAPISID")
                    ?: extractCookieValue(sessionCookie, "__Secure-1PAPISID")
                    ?: extractCookieValue(sessionCookie, "__Secure-3PAPISID")

                if (!sapisid.isNullOrEmpty()) {
                    val hash = generateSapiSidHash(sapisid, "https://music.youtube.com")
                    connection.setRequestProperty("Authorization", "SAPISIDHASH $hash")
                }
            }

            // Minimal payload body structure matching InnerTube specifications
            val payload = JSONObject().apply {
                put("browseId", "FEmusic_home")
                put("context", JSONObject().apply {
                    put("client", JSONObject().apply {
                        put("clientName", "WEB_REMIX")
                        put("clientVersion", "1.20260615.01.00")
                        put("hl", "pt")
                        put("gl", "BR")
                    })
                })
            }

            OutputStreamWriter(connection.outputStream).use { writer ->
                writer.write(payload.toString())
                writer.flush()
            }

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val responseString = connection.inputStream.bufferedReader().use { it.readText() }
                val rootJson = JSONObject(responseString)
                return@withContext parseQuickPicksFromHome(rootJson)
            }
            emptyList()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    // Parse specific responsive target container from response node tree
    private fun parseQuickPicksFromHome(rootJson: JSONObject): List<QuickPick> {
        val sectionListContents = findSectionListContents(rootJson) ?: return emptyList()

        for (i in 0 until sectionListContents.length()) {
            val section = sectionListContents.optJSONObject(i) ?: continue
            val shelf = section.optJSONObject("musicCarouselShelfRenderer") ?: continue

            val headerTitle = shelf.optJSONObject("header")
                ?.optJSONObject("musicCarouselShelfBasicHeaderRenderer")
                ?.optJSONObject("title")
                ?.optJSONArray("runs")
                ?.let { runs -> (0 until runs.length()).joinToString("") { runs.getJSONObject(it).optString("text") } }
                ?: continue

            // Target matching descriptor node matching localization settings
            if (!headerTitle.equals("Escolha a dedo", ignoreCase = true)) continue

            val items = shelf.optJSONArray("contents") ?: continue
            val quickPicks = mutableListOf<QuickPick>()

            for (j in 0 until items.length()) {
                val itemRenderer = items.optJSONObject(j)?.optJSONObject("musicResponsiveListItemRenderer") ?: continue
                parseQuickPickItem(itemRenderer)?.let { quickPicks.add(it) }
            }

            return quickPicks
        }

        return emptyList()
    }

    // Traverse structure recursively to process alternative continuation mappings
    private fun findSectionListContents(rootJson: JSONObject): org.json.JSONArray? {
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

    // Extract track identifier metadata parameters from response object
    private fun parseQuickPickItem(item: JSONObject): QuickPick? {
        val videoId = item.optJSONObject("playlistItemData")?.optString("videoId")
            ?: return null

        val flexColumns = item.optJSONArray("flexColumns") ?: return null
        if (flexColumns.length() < 2) return null

        // Primary flex column mapping for title identification
        val titleRuns = flexColumns.optJSONObject(0)
            ?.optJSONObject("musicResponsiveListItemFlexColumnRenderer")
            ?.optJSONObject("text")
            ?.optJSONArray("runs")
        val title = titleRuns?.optJSONObject(0)?.optString("text") ?: return null

        // Secondary flex column mapping for structural artist identification
        val subtitleRuns = flexColumns.optJSONObject(1)
            ?.optJSONObject("musicResponsiveListItemFlexColumnRenderer")
            ?.optJSONObject("text")
            ?.optJSONArray("runs")

        // Extract all artists
        val artists = mutableListOf<String>()
        if (subtitleRuns != null) {
            for (k in 0 until subtitleRuns.length()) {
                val runObj = subtitleRuns.optJSONObject(k) ?: continue
                val rawText = runObj.optString("text") ?: continue
                val artistText = rawText.trim()

                if (artistText.isNotEmpty()) {
                    // Define connectors to be strictly ignored
                    val connectors = listOf(",", "•", "e", "&", "and")

                    // Filter formatting nodes and trailing metadata fields
                    val isConnector = connectors.contains(artistText.lowercase())
                    val isMetadata = artistText.contains("Tocou") || artistText.contains("vezes")

                    if (!isConnector && !isMetadata) {
                        artists.add(artistText)
                    }
                }
            }
        }

        // Extract primary artist (first run)
        val artist = artists.firstOrNull() ?: ""

        // Thumbnail component selector resolving highest available capacity
        val thumbnails = item.optJSONObject("thumbnail")
            ?.optJSONObject("musicThumbnailRenderer")
            ?.optJSONObject("thumbnail")
            ?.optJSONArray("thumbnails")
        val thumbnailUrl = thumbnails?.let { array ->
            if (array.length() > 0) array.optJSONObject(array.length() - 1)?.optString("url") else null
        }

        return QuickPick(
            videoId = videoId,
            title = title,
            artist = artist,
            artists = if (artists.isEmpty()) listOf(artist) else artists,
            thumbnailUrl = thumbnailUrl
        )
    }
}

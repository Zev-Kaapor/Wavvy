package com.lonewolf.wavvy.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.lonewolf.wavvy.data.models.AccountData
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
        try {
            val url = URL("https://music.youtube.com/youtubei/v1/account/account_menu")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.doOutput = true

            // Network setup configuration headers
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
            connection.setRequestProperty("X-Origin", "https://music.youtube.com")

            // Inject authorization cookie from session storage and calculate required hash tokens
            val sessionCookie = getAuthCookie()
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
}

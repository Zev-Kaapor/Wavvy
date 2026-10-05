package com.wavvy.app.features.auth.data

// Coroutines
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
// JSON and files
import org.json.JSONObject
import java.io.File
// Network and security utilities
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

// Name and photo of the account that signed in
data class AccountInfo(val name: String, val photoUrl: String?)

// YouTube Music and the values the requests to its account menu carry
const val MusicOrigin = "https://music.youtube.com"
private const val AccountMenuUrl = "$MusicOrigin/youtubei/v1/account/account_menu"
private const val ClientName = "WEB_REMIX"
private const val ClientVersion = "1.20260615.01.00"
private const val UserAgent = "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"
private const val AuthUser = "0"
private const val TimeoutMillis = 15_000
private const val HttpOk = 200

// Reads who is signed in from the session cookies, and downloads the photo
object AccountClient {
    // The account menu has the photo of the active account, the YouTube Music channel when there is one
    suspend fun fetchAccount(cookies: String): Result<AccountInfo> = withContext(Dispatchers.IO) {
        runCatching {
            val connection = URL(AccountMenuUrl).openConnection() as HttpURLConnection
            try {
                connection.requestMethod = "POST"
                connection.doOutput = true
                connection.connectTimeout = TimeoutMillis
                connection.readTimeout = TimeoutMillis
                connection.setRequestProperty("Content-Type", "application/json")
                connection.setRequestProperty("User-Agent", UserAgent)
                connection.setRequestProperty("Origin", MusicOrigin)
                connection.setRequestProperty("X-Origin", MusicOrigin)
                connection.setRequestProperty("X-Goog-AuthUser", AuthUser)
                connection.setRequestProperty("Cookie", cookies)
                signatureFor(cookies)?.let { connection.setRequestProperty("Authorization", "SAPISIDHASH $it") }

                val body = JSONObject().put(
                    "context",
                    JSONObject().put(
                        "client",
                        JSONObject().put("clientName", ClientName).put("clientVersion", ClientVersion)
                    )
                )
                connection.outputStream.use { it.write(body.toString().toByteArray()) }

                check(connection.responseCode == HttpOk) { "Account menu answered ${connection.responseCode}" }
                parseAccount(connection.inputStream.bufferedReader().use { it.readText() })
            } finally {
                connection.disconnect()
            }
        }
    }

    // Saves the photo of the account in the file
    suspend fun downloadPhoto(photoUrl: String, target: File): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            require(photoUrl.startsWith("https://")) { "Photo is not served over https" }
            val connection = URL(photoUrl).openConnection() as HttpURLConnection
            try {
                connection.connectTimeout = TimeoutMillis
                connection.readTimeout = TimeoutMillis
                check(connection.responseCode == HttpOk) { "Photo answered ${connection.responseCode}" }
                connection.inputStream.use { input -> target.outputStream().use { input.copyTo(it) } }
                Unit
            } finally {
                connection.disconnect()
            }
        }
    }

    // Reads the name and the largest photo from the active account header
    private fun parseAccount(json: String): AccountInfo {
        val header = JSONObject(json)
            .getJSONArray("actions").getJSONObject(0)
            .getJSONObject("openPopupAction").getJSONObject("popup")
            .getJSONObject("multiPageMenuRenderer").getJSONObject("header")
            .getJSONObject("activeAccountHeaderRenderer")

        val name = header.getJSONObject("accountName").getJSONArray("runs").getJSONObject(0).getString("text")
        val photos = header.optJSONObject("accountPhoto")?.optJSONArray("thumbnails")
        val photoUrl = photos?.takeIf { it.length() > 0 }?.getJSONObject(photos.length() - 1)?.getString("url")

        return AccountInfo(name = name, photoUrl = photoUrl)
    }

    // Signature the requests of the signed in web player carry, made from the session cookie
    private fun signatureFor(cookies: String): String? {
        val sapisid = cookieValue(cookies, "SAPISID") ?: cookieValue(cookies, "__Secure-3PAPISID") ?: return null
        val timestamp = System.currentTimeMillis() / 1000
        val digest = MessageDigest.getInstance("SHA-1").digest("$timestamp $sapisid $MusicOrigin".toByteArray())

        return "${timestamp}_${digest.joinToString("") { "%02x".format(it) }}"
    }

    private fun cookieValue(cookies: String, key: String): String? =
        Regex("(?:^|;\\s*)$key=([^;]+)").find(cookies)?.groupValues?.get(1)
}

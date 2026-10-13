package com.wavvy.app.core.discord

// Android context, intents and encoding
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Base64
import androidx.browser.customtabs.CustomTabsClient
import androidx.browser.customtabs.CustomTabsIntent
import androidx.browser.customtabs.CustomTabsServiceConnection
import androidx.core.net.toUri
// Security and time
import java.security.MessageDigest
import java.security.SecureRandom
// Coroutines and reactive flows
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
// The web
import okhttp3.FormBody
import okhttp3.Request
import org.json.JSONObject
// Project resources
import com.wavvy.app.core.network.WavvyProxy

// The name of the file where the tokens are kept, also named in the rules that leave it out of the backups
const val DiscordStoreName = "discord"

// The application of Wavvy in the Developer Portal of Discord, the address Discord sends the user back to and what the login asks for
const val DiscordAppId = "1559613183189061722"
const val DiscordScheme = "wavvy"
const val DiscordHost = "discord"
private const val RedirectAddress = "$DiscordScheme://$DiscordHost/callback"
private const val Scopes = "openid sdk.social_layer_presence"
private const val AuthorizeAddress = "https://discord.com/oauth2/authorize"
private const val TokenAddress = "https://discord.com/api/v10/oauth2/token"
private const val RevokeAddress = "https://discord.com/api/v10/oauth2/token/revoke"

// How many random bytes make the secret and the state of a login, and how long before the end a token is renewed
private const val SecretBytes = 32
private const val RenewEarlyMillis = 60_000L
private const val MillisPerSecond = 1000L

// How much of a link that came back wrong is shown in the notice, it holds no token
private const val LinkLogLength = 160

// Why the login with Discord did not finish
class DiscordAuthException(message: String) : Exception(message)

// The login with Discord in the browser, with a proof key so no secret of the app is needed, and the tokens that come out of it
object DiscordAuth {
    private val mutableCallback = MutableStateFlow<Uri?>(null)
    private var connection: CustomTabsServiceConnection? = null

    // The link Discord sent the user back with, it waits here until the screen that finishes the login takes it
    val callback: StateFlow<Uri?> = mutableCallback.asStateFlow()

    fun offerCallback(link: Uri) {
        mutableCallback.value = link
    }

    fun clearCallback() {
        mutableCallback.value = null
    }

    // Wakes the browser while the user reads the warning, so the page of Discord opens sooner
    fun warmUp(context: Context) {
        if (connection != null) return
        val app = context.applicationContext
        val browser = CustomTabsClient.getPackageName(app, null) ?: return
        val service = object : CustomTabsServiceConnection() {
            override fun onCustomTabsServiceConnected(name: ComponentName, client: CustomTabsClient) {
                client.warmup(0L)
            }

            override fun onServiceDisconnected(name: ComponentName) {}
        }
        connection = service
        if (!CustomTabsClient.bindCustomTabsService(app, browser, service)) connection = null
    }

    // Lets the browser sleep again
    fun coolDown(context: Context) {
        connection?.let { runCatching { context.applicationContext.unbindService(it) } }
        connection = null
    }

    // Opens the page of Discord where the user agrees, the secret that proves it is the same login is kept for the way back
    suspend fun start(context: Context) {
        val verifier = randomText()
        val state = randomText()
        DiscordStore(context).saveLogin(verifier, state)

        val challenge = Base64.encodeToString(MessageDigest.getInstance("SHA-256").digest(verifier.toByteArray()), UrlSafe)
        val address = AuthorizeAddress.toUri().buildUpon()
            .appendQueryParameter("response_type", "code")
            .appendQueryParameter("client_id", DiscordAppId)
            .appendQueryParameter("scope", Scopes)
            .appendQueryParameter("redirect_uri", RedirectAddress)
            .appendQueryParameter("state", state)
            .appendQueryParameter("code_challenge", challenge)
            .appendQueryParameter("code_challenge_method", "S256")
            .appendQueryParameter("prompt", "consent")
            .build()

        CustomTabsIntent.Builder().build().apply { intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }.launchUrl(context, address)
        coolDown(context)
    }

    // Trades the code that came back for the tokens, a link that is not the one of this login is refused
    suspend fun complete(context: Context, link: Uri): Result<Unit> = runCatching<Unit> {
        val store = DiscordStore(context)
        val (verifier, state) = store.takeLogin() ?: throw DiscordAuthException("No login is open")
        if (link.getQueryParameter("state") != state) throw DiscordAuthException("The state does not match")
        // A refusal comes back in the place of the code, with the reason Discord gives
        link.getQueryParameter("error")?.let { error ->
            throw DiscordAuthException("$error ${link.getQueryParameter("error_description").orEmpty()}".trim())
        }
        val code = link.getQueryParameter("code") ?: throw DiscordAuthException("Discord sent no code, the link was ${link.toString().take(LinkLogLength)}")

        val form = FormBody.Builder()
            .add("client_id", DiscordAppId)
            .add("grant_type", "authorization_code")
            .add("code", code)
            .add("redirect_uri", RedirectAddress)
            .add("code_verifier", verifier)
            .build()
        store.saveTokens(requestTokens(form))
    }.onFailure { if (it is CancellationException) throw it }

    // A key that works, renewed when it is about to stop, null when there is no login or it cannot be renewed
    suspend fun accessToken(context: Context): String? {
        val store = DiscordStore(context)
        val tokens = store.tokens() ?: return null
        if (tokens.expiresAt - System.currentTimeMillis() > RenewEarlyMillis) return tokens.access
        if (tokens.refresh.isEmpty()) return null

        return runCatching {
            val form = FormBody.Builder()
                .add("client_id", DiscordAppId)
                .add("grant_type", "refresh_token")
                .add("refresh_token", tokens.refresh)
                .build()
            requestTokens(form).also { store.saveTokens(it) }.access
        }.getOrNull()
    }

    // Takes the login away from this phone and tells Discord to stop honoring the tokens, the second part is only tried
    suspend fun signOut(context: Context) {
        val store = DiscordStore(context)
        val tokens = store.tokens()
        store.clear()
        if (tokens == null) return

        runCatching {
            withContext(Dispatchers.IO) {
                val form = FormBody.Builder().add("client_id", DiscordAppId).add("token", tokens.access).build()
                WavvyProxy.client.newCall(Request.Builder().url(RevokeAddress).post(form).build()).execute().close()
            }
        }
    }

    private suspend fun requestTokens(form: FormBody): DiscordTokens = withContext(Dispatchers.IO) {
        val request = Request.Builder().url(TokenAddress).post(form).build()
        WavvyProxy.client.newCall(request).execute().use { response ->
            val body = response.body.string()
            if (!response.isSuccessful) {
                // What Discord says is why it refused, it never carries a token
                val reason = runCatching { JSONObject(body).let { it.optString("error") + " " + it.optString("error_description") }.trim() }.getOrDefault("")
                throw DiscordAuthException("Discord answered ${response.code} $reason".trim())
            }

            val json = JSONObject(body)
            DiscordTokens(
                access = json.getString("access_token"),
                refresh = json.optString("refresh_token"),
                expiresAt = System.currentTimeMillis() + json.optLong("expires_in") * MillisPerSecond
            )
        }
    }

    private fun randomText(): String = Base64.encodeToString(ByteArray(SecretBytes).also { SecureRandom().nextBytes(it) }, UrlSafe)

    private const val UrlSafe = Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP
}

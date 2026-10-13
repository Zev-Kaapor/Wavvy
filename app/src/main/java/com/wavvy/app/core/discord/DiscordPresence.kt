package com.wavvy.app.core.discord

// Android context
import android.content.Context
// Coroutines and reactive flows
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
// The web
import kotlin.random.Random
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
// Project resources
import com.wavvy.app.R
import com.wavvy.app.core.network.WavvyProxy
import com.wavvy.app.core.playback.PlayableTrack
import com.wavvy.app.core.playback.PlayerConnection

// What the gateway needs to send an activity, and the address of a song to open from the button
private const val ExternalAssetsAddress = "https://discord.com/api/v9/applications/$DiscordAppId/external-assets"
private const val SongAddress = "https://music.youtube.com/watch?v="
private const val ProjectAddress = "https://github.com/Zev-Kaapor/Wavvy"
private const val DefaultDetails = "{title}"
private const val DefaultState = "{artist}"
private const val AssetPrefix = "mp:"

// The close codes that decide what is done next, the token is not good or the app is not allowed to connect
private const val CloseBadToken = 4004
private const val CloseNotAllowed = 4014

// The wait before a connection that dropped is tried again, it doubles each time up to a limit, with some chance added
private const val RetryBaseMillis = 2_000L
private const val RetryMaxMillis = 60_000L
private const val RetryJitterMillis = 1_000L

// Shows what plays on the profile of the Discord of the user, while they are logged in
object DiscordPresence : DiscordGatewayListener {
    private val scope = MainScope()
    private var appContext: Context? = null
    private var gateway: DiscordGateway? = null
    private var retryJob: Job? = null
    private var pushJob: Job? = null
    private var attempts = 0

    // Pictures already sent to Discord, so a song is asked once
    private val assets = HashMap<String, String>()

    // Follows the login and the player, called once when the app opens
    fun start(context: Context) {
        if (appContext != null) return
        val app = context.applicationContext
        appContext = app

        val store = DiscordStore(app)
        scope.launch {
            combine(store.isConnected, store.settings) { isConnected, settings -> isConnected && settings.isEnabled }
                .distinctUntilChanged()
                .collect { if (it) connect() else disconnect() }
        }
        scope.launch { store.settings.collect { push() } }
        scope.launch {
            combine(PlayerConnection.track, PlayerConnection.isPlaying) { track, isPlaying -> track to isPlaying }.collect { push() }
        }
        scope.launch { PlayerConnection.seeks.collect { push() } }
    }

    private fun connect() {
        disconnect()
        gateway = DiscordGateway(scope, this).also { open(it) }
    }

    private fun disconnect() {
        retryJob?.cancel()
        pushJob?.cancel()
        gateway?.close()
        gateway = null
        attempts = 0
    }

    // Asks for a good key and opens, a login that cannot be renewed is left for the user to do again
    private fun open(target: DiscordGateway) {
        val app = appContext ?: return
        retryJob = scope.launch {
            val token = DiscordAuth.accessToken(app) ?: return@launch
            if (gateway === target) target.open(token)
        }
    }

    override fun onReady() {
        attempts = 0
        push()
    }

    override fun onClosed(code: Int) {
        val target = gateway ?: return
        when (code) {
            CloseNotAllowed -> return
            CloseBadToken -> target.forget()
        }

        val wait = minOf(RetryBaseMillis shl attempts.coerceAtMost(MaxDoublings), RetryMaxMillis) + Random.nextLong(RetryJitterMillis)
        attempts++
        retryJob = scope.launch {
            delay(wait)
            if (gateway === target) open(target)
        }
    }

    // Sends what plays, or an empty list when nothing does, the last request wins
    private fun push() {
        val target = gateway?.takeIf { it.isReady } ?: return
        val app = appContext ?: return
        val track = PlayerConnection.track.value?.takeIf { PlayerConnection.isPlaying.value }

        pushJob?.cancel()
        pushJob = scope.launch {
            val settings = DiscordStore(app).settings.first()
            val activities = JSONArray()
            if (track != null) {
                val token = DiscordAuth.accessToken(app)
                val image = token?.takeIf { settings.showCover }?.let { resolveImage(it, track.artworkUrl) }
                activities.put(activity(app, settings, track, image))
            }
            val data = JSONObject().put("since", JSONObject.NULL).put("activities", activities).put("status", settings.status.wire).put("afk", false)
            target.send(JSONObject().put("op", OpPresence).put("d", data))
        }
    }

    // Puts the song in the places a text marks for it
    private fun fill(text: String, track: PlayableTrack): String =
        text.replace("{title}", track.title).replace("{artist}", track.artist.orEmpty()).replace("{id}", track.id)

    private fun activity(context: Context, settings: DiscordSettings, track: PlayableTrack, image: String?): JSONObject {
        val start = System.currentTimeMillis() - PlayerConnection.positionMs
        val timestamps = JSONObject().put("start", start)
        PlayerConnection.durationMs.takeIf { it > 0 }?.let { timestamps.put("end", start + it) }

        val activity = JSONObject()
            .put("application_id", DiscordAppId)
            .put("name", settings.name.ifBlank { context.getString(R.string.app_name) }.let { fill(it, track) })
            .put("type", settings.kind.wire)
            .put("details", fill(settings.details.ifBlank { DefaultDetails }, track))
            .put("timestamps", timestamps)
        fill(settings.state.ifBlank { DefaultState }, track).takeIf { it.isNotBlank() }?.let { activity.put("state", it) }

        // Each button has a label and an address, the empty ones are the defaults
        val buttons = listOf(
            settings.button1 to (context.getString(R.string.integrations_discord_button_listen) to SongAddress + "{id}"),
            settings.button2 to (context.getString(R.string.integrations_discord_button_project) to ProjectAddress)
        ).filter { it.first.isEnabled }.map { (button, default) ->
            fill(button.label.ifBlank { default.first }, track) to fill(button.url.ifBlank { default.second }, track)
        }
        if (buttons.isNotEmpty()) {
            activity.put("buttons", JSONArray(buttons.map { it.first }))
            activity.put("metadata", JSONObject().put("button_urls", JSONArray(buttons.map { it.second })))
        }
        image?.let { activity.put("assets", JSONObject().put("large_image", it)) }
        return activity
    }

    // Discord takes a picture from the internet only after it is handed over, what it answers is the name to use
    private suspend fun resolveImage(token: String, url: String?): String? {
        if (url == null) return null
        assets[url]?.let { return it }

        return withContext(Dispatchers.IO) {
            runCatching {
                val body = JSONObject().put("urls", JSONArray().put(url)).toString().toRequestBody("application/json".toMediaType())
                val request = Request.Builder().url(ExternalAssetsAddress).header("Authorization", "Bearer $token").post(body).build()
                WavvyProxy.client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@use null
                    JSONArray(response.body.string()).optJSONObject(0)?.optString("external_asset_path")?.takeIf { it.isNotEmpty() }
                }
            }.getOrNull()?.let { AssetPrefix + it }?.also { assets[url] = it }
        }
    }

    // How many times the wait doubles, more would only pass the limit
    private const val MaxDoublings = 5

    // The message that sets the activity
    private const val OpPresence = 3
}

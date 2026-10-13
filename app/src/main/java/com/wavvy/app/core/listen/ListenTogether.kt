package com.wavvy.app.core.listen

// Android context
import android.content.Context
import androidx.annotation.StringRes
// Coroutines and reactive flows
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
// The web
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONArray
import org.json.JSONObject
// Project resources
import com.wavvy.app.R
import com.wavvy.app.core.designsystem.components.Toasts
import com.wavvy.app.core.network.WavvyProxy
import com.wavvy.app.core.playback.ControlAction
import com.wavvy.app.core.playback.ControlGate
import com.wavvy.app.core.playback.PlayableTrack
import com.wavvy.app.core.playback.PlayerConnection

// Someone in the room
data class RoomPerson(val id: String, val name: String, val isHost: Boolean)

// Where the connection to the room is
enum class ListenStatus { Connecting, Connected, Reconnecting, Closed, Failed }

// The room the user is in, null when there is none
data class RoomState(
    val code: String,
    val name: String?,
    val isHost: Boolean,
    val people: List<RoomPerson>,
    val status: ListenStatus,
    // Why the room is over, as the server says it, when it closed
    val reason: String? = null
)

// Something a guest asked the host to do, which the host accepts or not
data class GuestRequest(
    val requestId: String,
    val fromId: String,
    val fromName: String,
    val action: String,
    val fraction: Float?,
    val track: PlayableTrack?,
    val tracks: List<PlayableTrack>
)

// Why the room could not be made or joined, as the server says it
class ListenError(val reason: String) : Exception(reason)

private const val NormalClosure = 1000
private const val BadRequest = 400
private const val Forbidden = 403
private const val NotFound = 404
private const val MillisPerSecond = 1000L
private const val JsonType = "application/json; charset=UTF-8"
private const val CreateAddress = "$ListenServerAddress/rooms"
private const val ConnectAddress = "$ListenServerAddress/ws"

// Listening together, the host tells where it is in the song and the guests follow it, each phone plays the song by itself
object ListenTogether {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val client: OkHttpClient by lazy { WavvyProxy.client.newBuilder().pingInterval(ListenKeepAliveSeconds, TimeUnit.SECONDS).build() }

    private val mutableRoom = MutableStateFlow<RoomState?>(null)
    val room: StateFlow<RoomState?> = mutableRoom.asStateFlow()

    // The requests of the guests that wait for the host, and whether the host takes all of them without asking
    private val mutableRequests = MutableStateFlow<List<GuestRequest>>(emptyList())
    val requests: StateFlow<List<GuestRequest>> = mutableRequests.asStateFlow()
    private val mutableAutoAccept = MutableStateFlow(false)
    val autoAccept: StateFlow<Boolean> = mutableAutoAccept.asStateFlow()
    private val mutableAutoDecline = MutableStateFlow(false)
    val autoDecline: StateFlow<Boolean> = mutableAutoDecline.asStateFlow()

    // True after a guest enters, until the notice of how the app works in a room was read
    private val mutableNotice = MutableStateFlow(false)
    val notice: StateFlow<Boolean> = mutableNotice.asStateFlow()

    private var lastRequestAt = 0L

    // How to come back to the room when the connection is lost, the address with its credentials, and how many tries were made
    private var connectAddress: String? = null
    private var reconnectAttempts = 0
    private var reconnectJob: Job? = null

    init {
        PlayerConnection.controlGate = ControlGate(::intercept)
    }

    private var socket: WebSocket? = null
    private var appContext: Context? = null
    private var jobs = emptyList<Job>()

    // The difference between the clock of the server and the one of the phone, from the answer that came back the fastest
    private var clockOffsetMs = 0L
    private var bestRoundTripMs = Long.MAX_VALUE

    // Makes a room and connects to it as the host
    suspend fun create(context: Context, roomName: String?, hostName: String): Result<String> = runCatching {
        val body = JSONObject().put("hostName", hostName).apply { if (!roomName.isNullOrBlank()) put("name", roomName) }
        val request = Request.Builder().url(CreateAddress).post(body.toString().toRequestBody(JsonType.toMediaType())).build()

        val answer = withContext(Dispatchers.IO) {
            client.newCall(request).execute().use { response ->
                val text = response.body.string()
                if (!response.isSuccessful) throw ListenError(JSONObject(text.ifBlank { "{}" }).optString("error", "unavailable"))
                JSONObject(text)
            }
        }

        val code = answer.getString("code")
        connect(context, code, "token=${answer.getString("hostToken")}", isHost = true)
        code
    }

    // Connects to a room as a guest with the code that was given
    fun join(context: Context, code: String, name: String): Result<Unit> = runCatching {
        val clean = code.uppercase().filter { it.isLetterOrDigit() }
        if (clean.length != ListenCodeLength) throw ListenError("room_not_found")
        connect(context, clean, "name=${java.net.URLEncoder.encode(name, "UTF-8")}", isHost = false)
    }

    // Leaves the room, the host leaving closes it for everyone
    fun leave() {
        reconnectJob?.cancel()
        reconnectJob = null
        socket?.send(JSONObject().put("t", "leave").toString())
        socket?.close(NormalClosure, "left")
        socket = null
        jobs.forEach { it.cancel() }
        jobs = emptyList()
        mutableRoom.value = null
    }

    // Taking all the requests and declining all of them exclude each other, and the ones that wait get the answer
    fun setAutoAccept(value: Boolean) {
        mutableAutoAccept.value = value
        if (value) {
            mutableAutoDecline.value = false
            mutableRequests.value.forEach { decide(it, accepted = true) }
        }
    }

    fun setAutoDecline(value: Boolean) {
        mutableAutoDecline.value = value
        if (value) {
            mutableAutoAccept.value = false
            mutableRequests.value.forEach { decide(it, accepted = false) }
        }
    }

    fun dismissNotice() {
        mutableNotice.value = false
    }

    private fun connect(context: Context, code: String, credentials: String, isHost: Boolean) {
        close()
        mutableRequests.value = emptyList()
        mutableNotice.value = false
        mutableAutoAccept.value = false
        mutableAutoDecline.value = false
        appContext = context.applicationContext
        clockOffsetMs = 0L
        bestRoundTripMs = Long.MAX_VALUE
        mutableRoom.value = RoomState(code, null, isHost, emptyList(), ListenStatus.Connecting)

        connectAddress = "$ConnectAddress?code=$code&$credentials"
        reconnectAttempts = 0
        openSocket()
    }

    private fun openSocket() {
        val address = connectAddress ?: return
        socket = client.newWebSocket(Request.Builder().url(address).build(), Listener())
    }

    private fun close() {
        reconnectJob?.cancel()
        reconnectJob = null
        jobs.forEach { it.cancel() }
        jobs = emptyList()
        socket?.cancel()
        socket = null
    }

    // What the server says arrives here, on a thread of the connection, and goes to the main one
    private class Listener : WebSocketListener() {
        override fun onMessage(webSocket: WebSocket, text: String) {
            scope.launch { if (socket === webSocket) handle(JSONObject(text)) }
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            // A close the server chose to give, such as a second phone of the host taking its place, is the end, any other one is a loss to recover from
            scope.launch {
                if (socket !== webSocket) return@launch
                if (reason == "replaced" || reason == "too_many_messages" || reason == "message_too_big") finish(ListenStatus.Closed, reason) else lost(null)
            }
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            val reason = when (response?.code) {
                NotFound -> "room_not_found"
                Forbidden -> "room_full"
                BadRequest -> "name_required"
                else -> null
            }
            scope.launch { if (socket === webSocket) lost(reason) }
        }
    }

    // The connection was lost, a reason from the server that will not change is the end, anything else is tried again a few times
    private fun lost(reason: String?) {
        jobs.forEach { it.cancel() }
        jobs = emptyList()
        socket = null

        val current = mutableRoom.value ?: return
        if (reason != null || reconnectAttempts >= ListenReconnectAttempts || current.status == ListenStatus.Closed) {
            finish(ListenStatus.Failed, reason)
            return
        }

        reconnectAttempts++
        mutableRoom.value = current.copy(status = ListenStatus.Reconnecting)
        val wait = minOf(ListenReconnectFirstMillis shl (reconnectAttempts - 1), ListenReconnectLongestMillis)
        reconnectJob = scope.launch {
            delay(wait)
            openSocket()
        }
    }

    // The connection is over, the room stays on screen as closed so the user can see why
    private fun finish(status: ListenStatus, reason: String?) {
        jobs.forEach { it.cancel() }
        jobs = emptyList()
        socket = null
        mutableRoom.value = mutableRoom.value?.let { if (it.status == ListenStatus.Closed) it else it.copy(status = status, reason = it.reason ?: reason) }
    }

    private fun handle(message: JSONObject) {
        when (message.optString("t")) {
            "welcome" -> {
                clockOffsetMs = message.optLong("serverAt") - System.currentTimeMillis()
                val current = mutableRoom.value ?: return
                val isBack = current.status == ListenStatus.Reconnecting
                reconnectAttempts = 0
                mutableRoom.value = current.copy(
                    code = message.optString("code", current.code),
                    name = message.optString("name").takeIf { it.isNotEmpty() && it != "null" },
                    isHost = message.optBoolean("isHost"),
                    people = readPeople(message.optJSONArray("people")),
                    status = ListenStatus.Connected
                )
                startClock()
                if (message.optBoolean("isHost")) {
                    startHost()
                } else {
                    if (!isBack) mutableNotice.value = true
                    message.optJSONObject("playback")?.let(::follow)
                }
            }

            "people" -> mutableRoom.value = mutableRoom.value?.copy(people = readPeople(message.optJSONArray("people")))
            "playback" -> if (mutableRoom.value?.isHost == false) message.optJSONObject("playback")?.let(::follow)
            "pong" -> readClock(message)
            "request" -> receiveRequest(message)
            "decision" -> tell(if (message.optBoolean("accepted")) R.string.listen_request_accepted else R.string.listen_request_declined, isError = !message.optBoolean("accepted"))
            "denied" -> tell(
                if (message.optString("reason") == "cooldown") R.string.listen_request_wait else R.string.listen_request_no_host,
                isError = true,
                seconds = (message.optLong("waitMs") + MillisPerSecond - 1) / MillisPerSecond
            )
            "closed" -> {
                mutableRoom.value = mutableRoom.value?.copy(status = ListenStatus.Closed, reason = message.optString("reason"))
                close()
            }
        }
    }

    // A guest asks something of the player, which goes to the host in place of the player, nothing happens on this phone until the host says yes
    private fun intercept(action: ControlAction): Boolean {
        val current = mutableRoom.value ?: return false
        if (current.isHost || current.status == ListenStatus.Closed || current.status == ListenStatus.Failed) return false

        // Nothing can be asked while the connection is down
        if (current.status == ListenStatus.Reconnecting) {
            tell(R.string.listen_reconnecting, isError = true)
            return true
        }

        val message = JSONObject().put("t", "request")
        when (action) {
            ControlAction.Blocked -> {
                tell(R.string.listen_request_blocked, isError = true)
                return true
            }
            is ControlAction.PlayPause -> message.put("action", if (action.shouldPlay) "play" else "pause")
            is ControlAction.Seek -> message.put("action", "seek").put("fraction", action.fraction.toDouble())
            ControlAction.Next -> message.put("action", "next")
            ControlAction.Previous -> message.put("action", "previous")
            is ControlAction.PlayTrack -> message.put("action", "playTrack").put("track", writeTrack(action.track))
            is ControlAction.Enqueue -> message.put("action", if (action.afterCurrent) "playNext" else "enqueue").put("tracks", JSONArray(action.tracks.take(ListenRequestTracks).map(::writeTrack)))
        }

        // A guest asks again only after a few seconds, the server keeps the same rule
        val now = System.currentTimeMillis()
        val wait = lastRequestAt + ListenRequestCooldownMillis - now
        if (wait > 0) {
            tell(R.string.listen_request_wait, isError = true, seconds = (wait + MillisPerSecond - 1) / MillisPerSecond)
            return true
        }

        lastRequestAt = now
        socket?.send(message.put("requestId", java.util.UUID.randomUUID().toString()).toString())
        tell(R.string.listen_request_sent, isError = false)
        return true
    }

    // What a guest asked arrives at the host, which takes it at once when it was told to take all of them
    private fun receiveRequest(message: JSONObject) {
        if (mutableRoom.value?.isHost != true) return
        val request = GuestRequest(
            requestId = message.optString("requestId"),
            fromId = message.optString("from"),
            fromName = message.optString("name"),
            action = message.optString("action"),
            fraction = if (message.isNull("fraction")) null else message.optDouble("fraction").toFloat(),
            track = readTrack(message.optJSONObject("track")),
            tracks = message.optJSONArray("tracks")?.let { array -> (0 until array.length()).mapNotNull { readTrack(array.optJSONObject(it)) } }.orEmpty()
        )

        mutableRequests.value = mutableRequests.value + request
        when {
            mutableAutoDecline.value -> decide(request, accepted = false)
            mutableAutoAccept.value -> decide(request, accepted = true)
        }
    }

    // The host answers a request, and does what was asked when it said yes
    fun decide(request: GuestRequest, accepted: Boolean) {
        if (request !in mutableRequests.value) return
        mutableRequests.value = mutableRequests.value - request
        socket?.send(JSONObject().put("t", "decision").put("requestId", request.requestId).put("to", request.fromId).put("accepted", accepted).toString())
        if (accepted) execute(request)
    }

    private fun execute(request: GuestRequest) {
        val context = appContext ?: return
        val track = request.track
        when (request.action) {
            "play" -> PlayerConnection.setPlaying(true)
            "pause" -> PlayerConnection.setPlaying(false)
            "next" -> PlayerConnection.skipToNext()
            "previous" -> PlayerConnection.skipToPrevious()
            "seek" -> request.fraction?.let(PlayerConnection::seekTo)
            "playTrack" -> track?.let { PlayerConnection.play(context, it) }
            "enqueue" -> PlayerConnection.enqueueAll(context, request.tracks, afterCurrent = false)
            "playNext" -> PlayerConnection.enqueueAll(context, request.tracks, afterCurrent = true)
        }
    }

    // A short message on the screen, with how many seconds to wait when it asks for it
    private fun tell(@StringRes text: Int, isError: Boolean, seconds: Long = 0L) {
        val context = appContext ?: return
        Toasts.show(if (seconds > 0) context.getString(text, seconds) else context.getString(text), isError = isError)
    }

    private fun readPeople(array: JSONArray?): List<RoomPerson> =
        (0 until (array?.length() ?: 0)).mapNotNull { index ->
            array?.optJSONObject(index)?.let { RoomPerson(it.optString("id"), it.optString("name"), it.optBoolean("isHost")) }
        }

    // The clock of the phone is compared with the one of the server by the time a message takes to go and come back
    private fun startClock() {
        jobs += scope.launch {
            while (true) {
                socket?.send(JSONObject().put("t", "ping").put("at", System.currentTimeMillis()).toString())
                delay(TimeUnit.SECONDS.toMillis(ListenClockSeconds))
            }
        }
    }

    private fun readClock(message: JSONObject) {
        val now = System.currentTimeMillis()
        val roundTrip = now - message.optLong("at")
        if (roundTrip in 0 until bestRoundTripMs) {
            bestRoundTripMs = roundTrip
            clockOffsetMs = message.optLong("serverAt") - (message.optLong("at") + roundTrip / 2)
        }
    }

    private fun serverNow(): Long = System.currentTimeMillis() + clockOffsetMs

    // The host tells what plays when something changes, when it jumps in the song, and every few seconds so the guests do not drift
    @OptIn(FlowPreview::class)
    private fun startHost() {
        val changes = combine(PlayerConnection.track, PlayerConnection.isPlaying, PlayerConnection.queue, PlayerConnection.currentIndex) { _, _, _, _ -> }
        val beat = flow { while (true) { emit(Unit); delay(TimeUnit.SECONDS.toMillis(ListenHeartbeatSeconds)) } }

        jobs += scope.launch {
            merge(changes, PlayerConnection.seeks, beat).debounce(ListenDebounceMillis).collect { sendPlayback() }
        }
    }

    private fun sendPlayback() {
        val track = PlayerConnection.track.value
        val queue = PlayerConnection.queue.value
        val current = PlayerConnection.currentIndex.value

        // What comes after the song that plays, in the order it plays
        val upcoming = queue.dropWhile { it.index != current }.drop(1).take(ListenQueueShared).map { it.track }
        val message = JSONObject()
            .put("t", "playback")
            .put("isPlaying", PlayerConnection.wantsToPlay)
            .put("positionMs", PlayerConnection.positionMs)
            .put("queue", JSONArray(upcoming.map(::writeTrack)))
        if (track != null) message.put("track", writeTrack(track))

        socket?.send(message.toString())
    }

    private fun writeTrack(track: PlayableTrack): JSONObject =
        JSONObject()
            .put("id", track.id)
            .put("title", track.title)
            .put("artist", track.artist.orEmpty())
            .put("durationMs", track.durationMs)
            .apply { track.artworkUrl?.takeIf { it.startsWith("https://") }?.let { put("thumbnail", it) } }

    private fun readTrack(json: JSONObject?): PlayableTrack? {
        val id = json?.optString("id").orEmpty()
        if (id.isEmpty()) return null
        return PlayableTrack(
            id = id,
            title = json?.optString("title").orEmpty(),
            artist = json?.optString("artist")?.takeIf { it.isNotEmpty() },
            artworkUrl = json?.optString("thumbnail")?.takeIf { it.isNotEmpty() && it != "null" },
            durationMs = json?.optLong("durationMs") ?: 0L
        )
    }

    // A guest does what the host says, opens the song if it is another one, plays or pauses with it and jumps when it drifted too far
    private fun follow(playback: JSONObject) {
        val context = appContext ?: return
        val track = readTrack(playback.optJSONObject("track"))
        if (track == null) {
            PlayerConnection.stop()
            return
        }

        val isPlaying = playback.optBoolean("isPlaying")
        val saidAt = playback.optLong("serverAt")
        val position = playback.optLong("positionMs")

        // Where the host is now, a song that plays has moved on since the host said it
        val expected = if (isPlaying) position + (serverNow() - saidAt).coerceAtLeast(0L) else position

        if (PlayerConnection.track.value?.id != track.id) {
            val queue = playback.optJSONArray("queue")
            val upcoming = (0 until (queue?.length() ?: 0)).mapNotNull { readTrack(queue?.optJSONObject(it)) }
            PlayerConnection.playShared(context, listOf(track) + upcoming, expected, isPlaying)
            return
        }

        if (PlayerConnection.wantsToPlay != isPlaying) PlayerConnection.setPlaying(isPlaying)
        if (kotlin.math.abs(PlayerConnection.positionMs - expected) > ListenDriftMillis) PlayerConnection.seekToMs(expected)
    }
}

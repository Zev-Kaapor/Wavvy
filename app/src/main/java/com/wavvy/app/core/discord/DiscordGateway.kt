package com.wavvy.app.core.discord

// Coroutines
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
// The web
import java.util.concurrent.TimeUnit
import kotlin.random.Random
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
// Project resources
import com.wavvy.app.core.network.WavvyProxy

// Where the gateway of Discord is, and the numbers of the messages it speaks
private const val GatewayAddress = "wss://gateway.discord.gg/?v=10&encoding=json"
private const val OpDispatch = 0
private const val OpHeartbeat = 1
private const val OpIdentify = 2
private const val OpResume = 6
private const val OpReconnect = 7
private const val OpInvalidSession = 9
private const val OpHello = 10
private const val OpAck = 11

// The code that closes a connection to be opened again, and the one of a normal end
private const val CloseAgain = 4000
private const val CloseNormal = 1000

// How the app tells Discord who it is, as itself
private const val ClientName = "Wavvy"

// What the connection to the gateway tells the one that uses it
interface DiscordGatewayListener {
    // The gateway took the login, activities can be sent
    fun onReady()

    // The connection is over, with the code it ended with
    fun onClosed(code: Int)
}

// A connection to the gateway of Discord, which keeps itself alive and can pick up where it stopped when it drops
class DiscordGateway(private val scope: CoroutineScope, private val listener: DiscordGatewayListener) {
    private val client: OkHttpClient by lazy { WavvyProxy.client.newBuilder().readTimeout(0, TimeUnit.MILLISECONDS).pingInterval(0, TimeUnit.MILLISECONDS).build() }

    private var socket: WebSocket? = null
    private var heartbeat: Job? = null
    private var token = ""
    private var sessionId: String? = null
    private var resumeAddress: String? = null
    private var sequence: Int? = null
    private var hasAck = true

    // True once the gateway took the login
    var isReady = false
        private set

    // Opens the connection, it picks up the old session when there is one
    fun open(token: String) {
        this.token = token
        isReady = false
        val request = Request.Builder().url(resumeAddress?.takeIf { sessionId != null } ?: GatewayAddress).build()
        socket = client.newWebSocket(request, Receiver())
    }

    fun send(frame: JSONObject): Boolean = isReady && socket?.send(frame.toString()) == true

    fun close() {
        heartbeat?.cancel()
        isReady = false
        socket?.close(CloseNormal, null)
        socket = null
    }

    // Forgets the session, the next connection starts from the login
    fun forget() {
        sessionId = null
        resumeAddress = null
        sequence = null
    }

    // What arrives on the thread of the connection goes to the main one, and a connection that was already replaced is ignored
    private inner class Receiver : WebSocketListener() {
        override fun onMessage(webSocket: WebSocket, text: String) {
            scope.launch { if (socket === webSocket) handle(JSONObject(text)) }
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            scope.launch { if (socket === webSocket) ended(code) }
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            scope.launch { if (socket === webSocket) ended(CloseAgain) }
        }
    }

    private fun ended(code: Int) {
        heartbeat?.cancel()
        isReady = false
        socket = null
        listener.onClosed(code)
    }

    private fun handle(message: JSONObject) {
        if (!message.isNull("s")) sequence = message.optInt("s")

        when (message.optInt("op", -1)) {
            OpHello -> {
                startHeartbeat(message.getJSONObject("d").getLong("heartbeat_interval"))
                sendRaw(if (sessionId != null && sequence != null) resumeFrame() else identifyFrame())
            }
            OpAck -> hasAck = true
            OpHeartbeat -> sendRaw(heartbeatFrame())
            OpReconnect -> socket?.close(CloseAgain, null)
            OpInvalidSession -> {
                if (!message.optBoolean("d")) forget()
                socket?.close(CloseAgain, null)
            }
            OpDispatch -> when (message.optString("t")) {
                "READY" -> {
                    val data = message.getJSONObject("d")
                    sessionId = data.optString("session_id").takeIf { it.isNotEmpty() }
                    resumeAddress = data.optString("resume_gateway_url").takeIf { it.isNotEmpty() }?.let { "$it/?v=10&encoding=json" }
                    isReady = true
                    listener.onReady()
                }
                "RESUMED" -> {
                    isReady = true
                    listener.onReady()
                }
            }
        }
    }

    // The messages that open the session go out before it is ready, so they skip the check
    private fun sendRaw(frame: JSONObject) {
        socket?.send(frame.toString())
    }

    // A beat at a random moment of the first interval, then one every interval, a beat that nobody answered ends the connection
    private fun startHeartbeat(intervalMs: Long) {
        heartbeat?.cancel()
        hasAck = true
        heartbeat = scope.launch {
            delay((intervalMs * Random.nextDouble()).toLong())
            while (true) {
                if (!hasAck) {
                    socket?.close(CloseAgain, null)
                    return@launch
                }
                hasAck = false
                sendRaw(heartbeatFrame())
                delay(intervalMs)
            }
        }
    }

    private fun heartbeatFrame() = JSONObject().put("op", OpHeartbeat).put("d", sequence ?: JSONObject.NULL)

    private fun identifyFrame(): JSONObject {
        val properties = JSONObject().put("os", "android").put("browser", ClientName).put("device", ClientName)
        val data = JSONObject().put("token", "Bearer $token").put("intents", 0).put("properties", properties).put("compress", false)
        return JSONObject().put("op", OpIdentify).put("d", data)
    }

    private fun resumeFrame(): JSONObject {
        val data = JSONObject().put("token", "Bearer $token").put("session_id", sessionId).put("seq", sequence)
        return JSONObject().put("op", OpResume).put("d", data)
    }
}

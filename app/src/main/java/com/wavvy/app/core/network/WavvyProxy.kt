package com.wavvy.app.core.network

// Android context
import android.content.Context
// Network
import java.net.Authenticator as SystemAuthenticator
import java.net.InetSocketAddress
import java.net.PasswordAuthentication
import java.net.Proxy
import java.net.ProxySelector
import java.net.SocketAddress
import java.net.URI
import okhttp3.Authenticator
import okhttp3.Credentials
import okhttp3.OkHttpClient
// Coroutines
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
// Project resources
import com.wavvy.app.features.settings.data.ProxyKind
import com.wavvy.app.features.settings.data.ProxySettings
import com.wavvy.app.features.settings.data.SettingsStore

// The header an HTTP proxy asks the login in, and the hosts that never go through the proxy
private const val ProxyAuthorization = "Proxy-Authorization"
private val LocalHosts = setOf("localhost", "::1")
private const val LoopbackPrefix = "127."
private const val SchemeSeparator = "://"
private const val PortSeparator = ':'
private const val MaxPort = 65535

// The proxy the user chose, one for the whole app, every connection asks it what to do so a change counts at once for the new ones
object WavvyProxy {
    @Volatile
    private var settings = ProxySettings()

    // What the system did before the proxy took its place, used for everything the proxy does not cover
    private var fallback: ProxySelector? = null
    private var isInstalled = false

    // Gives the proxy to the connections of the system and to the clients of the app, called once when the app starts
    @Synchronized
    fun install(context: Context) {
        if (isInstalled) return
        isInstalled = true

        val store = SettingsStore(context)
        settings = runBlocking { store.proxy.first() }
        fallback = ProxySelector.getDefault()
        ProxySelector.setDefault(selector)

        // The login of a proxy that the system connections are asked for, the clients of the app answer it on their own
        SystemAuthenticator.setDefault(object : SystemAuthenticator() {
            override fun getPasswordAuthentication(): PasswordAuthentication? =
                settings.takeIf { it.enabled && it.username.isNotEmpty() && requestorType == RequestorType.PROXY }
                    ?.let { PasswordAuthentication(it.username, it.password.toCharArray()) }
        })

        // A change of the choice is kept, and the connections that are open go away so the next ones follow it
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            store.proxy.distinctUntilChanged().collect {
                settings = it
                client.connectionPool.evictAll()
            }
        }
    }

    // The client of the app for what goes through OkHttp, the player, the downloads and the libraries, it shares its connections with the ones built from it
    val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .proxySelector(selector)
            .proxyAuthenticator(Authenticator { _, response ->
                // An HTTP proxy that asked for the login gets it once, a second ask means the login is wrong
                settings.takeIf { it.enabled && it.kind == ProxyKind.Http && it.username.isNotEmpty() && response.request.header(ProxyAuthorization) == null }
                    ?.let { response.request.newBuilder().header(ProxyAuthorization, Credentials.basic(it.username, it.password)).build() }
            })
            .build()
    }

    // The host and the port of what the user typed, null when it is not a host with a port
    fun parseAddress(text: String): Pair<String, Int>? {
        val address = text.trim().substringAfter(SchemeSeparator).trimEnd('/')
        val host = address.substringBeforeLast(PortSeparator, missingDelimiterValue = "").trim()
        val port = address.substringAfterLast(PortSeparator, missingDelimiterValue = "").toIntOrNull()

        return if (host.isNotEmpty() && port != null && port in 1..MaxPort) host to port else null
    }

    // The proxy that is on, null while it is off or its address is not good
    private fun active(): Proxy? {
        val current = settings
        if (!current.enabled) return null

        val (host, port) = parseAddress(current.address) ?: return null
        return Proxy(current.kind.type, InetSocketAddress.createUnresolved(host, port))
    }

    // Tells each connection to go through the proxy, except the ones to this very device
    private val selector = object : ProxySelector() {
        override fun select(uri: URI?): List<Proxy> {
            val proxy = active()
            val host = uri?.host?.lowercase()
            val isLocal = host == null || host in LocalHosts || host.startsWith(LoopbackPrefix)

            return if (proxy != null && !isLocal) listOf(proxy) else fallback?.select(uri) ?: listOf(Proxy.NO_PROXY)
        }

        override fun connectFailed(uri: URI?, address: SocketAddress?, failure: java.io.IOException?) {
            fallback?.connectFailed(uri, address, failure)
        }
    }
}

package com.wavvy.app.core.cast

// Android context
import android.content.Context
// Reactive flows
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
// Google Cast and the routes of the media
import androidx.mediarouter.media.MediaRouter
import com.google.android.gms.cast.framework.CastContext
import com.google.android.gms.cast.framework.CastSession
import com.google.android.gms.cast.framework.SessionManagerListener

// A device that can play what the app sends, with the name it gives itself and whether the app is connected to it
data class CastDevice(val id: String, val name: String, val description: String?, val isConnected: Boolean)

// The way to the TVs and speakers of the house, it finds the devices, connects to one and tells which one is connected
// It must be started and used from the main thread
object CastController {
    private var castContext: CastContext? = null
    private var router: MediaRouter? = null
    private var isSearching = false

    private val mutableIsAvailable = MutableStateFlow(false)
    private val mutableDevices = MutableStateFlow<List<CastDevice>>(emptyList())
    private val mutableConnectedName = MutableStateFlow<String?>(null)

    // False when the phone has no Google services, where there is nothing to show
    val isAvailable: StateFlow<Boolean> = mutableIsAvailable.asStateFlow()

    // The devices found while the search is on
    val devices: StateFlow<List<CastDevice>> = mutableDevices.asStateFlow()

    // The name of the device the app is connected to, none when it is not
    val connectedName: StateFlow<String?> = mutableConnectedName.asStateFlow()

    // Starts the framework, once, a phone that cannot run it leaves the buttons out
    fun init(context: Context) {
        if (castContext != null) return
        val app = context.applicationContext

        runCatching { CastContext.getSharedInstance(app) }.onSuccess { shared ->
            castContext = shared
            router = MediaRouter.getInstance(app)
            shared.sessionManager.addSessionManagerListener(sessionListener, CastSession::class.java)
            mutableConnectedName.value = shared.sessionManager.currentCastSession?.castDevice?.friendlyName
            mutableIsAvailable.value = true
        }
    }

    // Looks for devices until it is stopped, it costs battery, so it only runs while the list is on the screen
    fun startSearch() {
        val selector = castContext?.mergedSelector ?: return
        if (isSearching) return

        isSearching = true
        router?.addCallback(selector, routeCallback, MediaRouter.CALLBACK_FLAG_REQUEST_DISCOVERY)
        refresh()
    }

    fun stopSearch() {
        if (!isSearching) return

        isSearching = false
        router?.removeCallback(routeCallback)
    }

    // Starts the search again from nothing, for a device that did not show up
    fun restartSearch() {
        stopSearch()
        startSearch()
    }

    // Connects to a device of the list
    fun connect(device: CastDevice) {
        val target = router?.routes?.firstOrNull { it.id == device.id } ?: return
        router?.selectRoute(target)
    }

    // Ends the session, the sound goes back to the phone
    fun disconnect() {
        castContext?.sessionManager?.endCurrentSession(true)
    }

    // The devices of the router that play what the app sends, the selector only matches those of Cast, so the speaker of the phone is not among them
    private fun refresh() {
        val current = router ?: return
        val selector = castContext?.mergedSelector ?: return
        val selectedId = current.selectedRoute.id

        mutableDevices.value = current.routes
            .filter { !it.isDefault && it.isEnabled && it.matchesSelector(selector) }
            .map { CastDevice(id = it.id, name = it.name, description = it.description, isConnected = it.id == selectedId) }
    }

    private val routeCallback = object : MediaRouter.Callback() {
        override fun onRouteAdded(router: MediaRouter, route: MediaRouter.RouteInfo) = refresh()
        override fun onRouteRemoved(router: MediaRouter, route: MediaRouter.RouteInfo) = refresh()
        override fun onRouteChanged(router: MediaRouter, route: MediaRouter.RouteInfo) = refresh()
        override fun onRouteSelected(router: MediaRouter, route: MediaRouter.RouteInfo) = refresh()
        override fun onRouteUnselected(router: MediaRouter, route: MediaRouter.RouteInfo) = refresh()
    }

    private val sessionListener = object : SessionManagerListener<CastSession> {
        override fun onSessionStarted(session: CastSession, sessionId: String) = connected(session)
        override fun onSessionResumed(session: CastSession, wasSuspended: Boolean) = connected(session)
        override fun onSessionEnded(session: CastSession, error: Int) = disconnected()
        override fun onSessionStartFailed(session: CastSession, error: Int) = disconnected()
        override fun onSessionResumeFailed(session: CastSession, error: Int) = disconnected()
        override fun onSessionStarting(session: CastSession) = Unit
        override fun onSessionEnding(session: CastSession) = Unit
        override fun onSessionResuming(session: CastSession, sessionId: String) = Unit
        override fun onSessionSuspended(session: CastSession, reason: Int) = Unit
    }

    private fun connected(session: CastSession) {
        mutableConnectedName.value = session.castDevice?.friendlyName
        refresh()
    }

    private fun disconnected() {
        mutableConnectedName.value = null
        refresh()
    }
}

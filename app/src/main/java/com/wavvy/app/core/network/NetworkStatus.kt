package com.wavvy.app.core.network

// Android networking and threads
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Handler
import android.os.Looper
// Compose state and runtime
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
// UI utilities
import androidx.compose.ui.platform.LocalContext

// How the device reaches the internet, a connection that cannot reach it counts as none
enum class Connection { Wifi, Cellular, Offline }

// The connection of the device, which changes on the screen as it changes on the device
@Composable
fun rememberConnection(): Connection {
    val context = LocalContext.current
    var connection by remember { mutableStateOf(currentConnection(context)) }

    DisposableEffect(context) {
        val manager = context.getSystemService(ConnectivityManager::class.java)
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
                connection = capabilities.toConnection()
            }

            override fun onLost(network: Network) {
                connection = Connection.Offline
            }
        }

        // Answers come on the main thread, so the screen can read them as they are
        manager.registerDefaultNetworkCallback(callback, Handler(Looper.getMainLooper()))
        onDispose { manager.unregisterNetworkCallback(callback) }
    }

    return connection
}

// The connection that is in use now
private fun currentConnection(context: Context): Connection {
    val manager = context.getSystemService(ConnectivityManager::class.java)
    val capabilities = manager.getNetworkCapabilities(manager.activeNetwork) ?: return Connection.Offline

    return capabilities.toConnection()
}

// Wi-Fi, cable and anything else that works count as Wi-Fi, mobile data is its own, one that does not reach the internet is none
private fun NetworkCapabilities.toConnection(): Connection = when {
    !hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) -> Connection.Offline
    hasTransport(NetworkCapabilities.TRANSPORT_WIFI) || hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> Connection.Wifi
    hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> Connection.Cellular
    else -> Connection.Wifi
}

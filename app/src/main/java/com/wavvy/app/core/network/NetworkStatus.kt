package com.wavvy.app.core.network

// Android networking and threads
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
// Compose state and runtime
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
// UI utilities
import androidx.compose.ui.platform.LocalContext
// Coroutines
import kotlinx.coroutines.delay

// How the device reaches the internet and how well, a connection that cannot reach it counts as none and one that reaches it badly or by fits is weak
enum class Connection { Wifi, Cellular, WifiWeak, CellularWeak, Offline }

// A Wi-Fi at this signal or lower, in dBm, and a link at this speed or lower, in kbit/s, are weak
private const val WeakWifiSignal = -75
private const val SlowLinkKbps = 2_500

// A connection that changes this many times in this window is oscillating, and stays weak for the hold after the last change
private const val OscillationChanges = 3
private const val OscillationWindowMillis = 60_000L
private const val OscillationHoldMillis = 30_000L

// The connection of the device, which changes on the screen as it changes on the device
@Composable
fun rememberConnection(): Connection {
    val context = LocalContext.current
    var connection by remember { mutableStateOf(currentConnection(context)) }
    var isOscillating by remember { mutableStateOf(false) }
    var oscillatingUntil by remember { mutableLongStateOf(0L) }
    val changes = remember { ArrayDeque<Long>() }

    DisposableEffect(context) {
        val manager = context.getSystemService(ConnectivityManager::class.java)

        // Counts the changes of the last minute, enough of them mean the connection comes and goes
        fun change(next: Connection) {
            if (next != connection) {
                val now = SystemClock.elapsedRealtime()
                changes.addLast(now)
                while (changes.isNotEmpty() && now - changes.first() > OscillationWindowMillis) changes.removeFirst()
                if (changes.size >= OscillationChanges) oscillatingUntil = now + OscillationHoldMillis
            }
            connection = next
        }

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
                change(capabilities.toConnection())
            }

            override fun onLost(network: Network) {
                change(Connection.Offline)
            }
        }

        // Answers come on the main thread, so the screen can read them as they are
        manager.registerDefaultNetworkCallback(callback, Handler(Looper.getMainLooper()))
        onDispose { manager.unregisterNetworkCallback(callback) }
    }

    // The weak mark stays for the hold and then goes, unless the connection changes again
    LaunchedEffect(oscillatingUntil) {
        val wait = oscillatingUntil - SystemClock.elapsedRealtime()
        if (wait > 0) {
            isOscillating = true
            delay(wait)
        }
        isOscillating = false
    }

    return if (isOscillating) connection.weakened() else connection
}

// The connection that is in use now
private fun currentConnection(context: Context): Connection {
    val manager = context.getSystemService(ConnectivityManager::class.java)
    val capabilities = manager.getNetworkCapabilities(manager.activeNetwork) ?: return Connection.Offline

    return capabilities.toConnection()
}

// A connection that works but oscillates, none is still none
private fun Connection.weakened(): Connection = when (this) {
    Connection.Wifi -> Connection.WifiWeak
    Connection.Cellular -> Connection.CellularWeak
    else -> this
}

// Wi-Fi, cable and anything else that works count as Wi-Fi, mobile data is its own, one that does not reach the internet is none
private fun NetworkCapabilities.toConnection(): Connection {
    if (!hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)) return Connection.Offline

    val isWeak = isWeak()
    return when {
        hasTransport(NetworkCapabilities.TRANSPORT_WIFI) || hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) ->
            if (isWeak) Connection.WifiWeak else Connection.Wifi
        hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ->
            if (isWeak) Connection.CellularWeak else Connection.Cellular
        else -> if (isWeak) Connection.WifiWeak else Connection.Wifi
    }
}

// A Wi-Fi with a weak signal, or a link that is slow, such as the old mobile networks, the system says zero when it does not know the speed
private fun NetworkCapabilities.isWeak(): Boolean {
    val isWeakSignal = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
        hasTransport(NetworkCapabilities.TRANSPORT_WIFI) &&
        signalStrength != NetworkCapabilities.SIGNAL_STRENGTH_UNSPECIFIED &&
        signalStrength <= WeakWifiSignal
    val isSlow = linkDownstreamBandwidthKbps in 1..SlowLinkKbps

    return isWeakSignal || isSlow
}

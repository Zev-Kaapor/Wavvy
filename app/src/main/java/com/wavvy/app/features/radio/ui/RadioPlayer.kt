package com.wavvy.app.features.radio.ui

// Android context
import android.content.Context
// Compose state and runtime
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
// Coroutines
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
// Project resources
import com.wavvy.app.R
import com.wavvy.app.core.designsystem.components.Toasts
import com.wavvy.app.core.playback.PlayerConnection
import com.wavvy.app.core.radio.RadioLibrary
import com.wavvy.app.core.radio.RadioStation
import com.wavvy.app.core.radio.RadiosReader

// Plays a station, its sound is asked first and the one that is being asked is known, so its row can show it, a station that played goes to the ones listened to lately
@Stable
internal class RadioPlayer(private val context: Context, private val scope: CoroutineScope) {
    var opening by mutableStateOf<String?>(null)
        private set

    fun play(station: RadioStation) {
        if (opening != null) return

        opening = station.id
        scope.launch {
            RadiosReader.stream(context, station)
                .onSuccess {
                    PlayerConnection.playStation(context, station, it)
                    RadioLibrary.played(context, station)
                }
                .onFailure { Toasts.show(context.getString(R.string.radios_open_failed), subtitle = it.message, isError = true) }
            opening = null
        }
    }
}

@Composable
internal fun rememberRadioPlayer(): RadioPlayer {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    return remember(context) { RadioPlayer(context, scope) }
}

package com.wavvy.app.core.playback

// Android clock
import android.os.SystemClock
// Reactive flows
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

// Milliseconds in a minute
private const val MillisPerMinute = 60_000L

// What the sleep timer is waiting for, a moment of the clock of the phone, or the end of the song that plays
data class SleepState(val endsAtMs: Long? = null, val atEndOfSong: Boolean = false) {
    val isActive: Boolean get() = endsAtMs != null || atEndOfSong
}

// The sleep timer, the screens ask it and the playback service watches it and stops the music
// The clock counts the time the phone spends asleep too, and does not follow the changes of the hour
object SleepTimer {
    private val mutableState = MutableStateFlow(SleepState())
    val state: StateFlow<SleepState> = mutableState.asStateFlow()

    // Stops the music after some minutes
    fun startIn(minutes: Int) {
        mutableState.value = SleepState(endsAtMs = SystemClock.elapsedRealtime() + minutes * MillisPerMinute)
    }

    // Stops the music when the song that plays ends
    fun startAtEndOfSong() {
        mutableState.value = SleepState(atEndOfSong = true)
    }

    fun cancel() {
        mutableState.value = SleepState()
    }

    // Milliseconds left for a timer of minutes, none for the one that waits for the end of the song
    fun leftMs(state: SleepState): Long? = state.endsAtMs?.let { (it - SystemClock.elapsedRealtime()).coerceAtLeast(0L) }
}

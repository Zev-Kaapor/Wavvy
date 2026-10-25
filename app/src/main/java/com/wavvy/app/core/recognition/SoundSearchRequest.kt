package com.wavvy.app.core.recognition

// Reactive flows
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

// What the search by sound found outside the app, such as in the tile of the quick settings, it waits for the Search to take it and search it
object SoundSearchRequest {
    private val mutablePending = MutableStateFlow<String?>(null)

    // The name of the song that was found, until the Search takes it
    val pending: StateFlow<String?> = mutablePending.asStateFlow()

    fun offer(query: String) {
        mutablePending.value = query
    }

    // The Search took it, so it is not searched again when the screen is drawn again
    fun take() {
        mutablePending.value = null
    }
}

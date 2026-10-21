package com.wavvy.app.features.library.ui

// Reactive flows
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

// The group of the library that another screen asked to open, such as the playlists from the Home, waiting for the library to show it
object LibraryRequest {
    private val mutablePending = MutableStateFlow<LibraryGroup?>(null)
    val pending: StateFlow<LibraryGroup?> = mutablePending.asStateFlow()

    fun open(group: LibraryGroup) {
        mutablePending.value = group
    }

    // The library showed the group, so it is not asked again
    fun clear() {
        mutablePending.value = null
    }
}

package com.wavvy.app.features.settings.data

// Android context
import android.content.Context
// Coroutines
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

// What the user chose for the content, read at once by the code that filters the lists, which cannot wait for the store
object ContentPrefs {
    // True when videos show in the Home and in the queue
    @Volatile
    var showVideos = false
        private set

    private var isFollowing = false

    // Reads the choice once and then each time it changes, called when the app starts
    @Synchronized
    fun follow(context: Context) {
        if (isFollowing) return
        isFollowing = true

        val store = SettingsStore(context)
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            store.content.collect { showVideos = it.showVideos }
        }
    }
}

package com.wavvy.app.features.settings.data

// Android context
import android.content.Context
// Coroutines
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

// What the user chose for the privacy, read at once by the code that counts a listening, which cannot wait for the store
object PrivacyPrefs {
    // True while what is listened to is not kept in the history
    @Volatile
    var pauseHistory = false
        private set

    // True when what is listened to also goes to the history of the account
    @Volatile
    var syncToAccount = true
        private set

    private var isFollowing = false

    // Reads the choices once and then each time they change, called when the player starts
    @Synchronized
    fun follow(context: Context) {
        if (isFollowing) return
        isFollowing = true

        val store = SettingsStore(context)
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            store.privacy.collect {
                pauseHistory = it.pauseHistory
                syncToAccount = it.syncToAccount
            }
        }
    }
}

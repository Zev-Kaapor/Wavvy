package com.wavvy.app.core.backup

// Files and reactive flows
import android.net.Uri
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

// The backup file that the user opened from another app, such as the files or the messages, waiting for the screen of the backup to ask its password
object BackupInbox {
    private val mutablePending = MutableStateFlow<Uri?>(null)
    val pending: StateFlow<Uri?> = mutablePending.asStateFlow()

    fun offer(file: Uri) {
        mutablePending.value = file
    }

    // The screen took the file, so it is not offered again
    fun clear() {
        mutablePending.value = null
    }
}

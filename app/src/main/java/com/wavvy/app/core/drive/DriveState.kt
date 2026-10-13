package com.wavvy.app.core.drive

// Android context
import android.content.Context
// Coroutines and reactive flows
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first

// What is known of the Drive of the user, not connected, being looked at, connected with no backup, connected with one, or the Drive could not be reached
sealed interface DriveStatus {
    data object Disconnected : DriveStatus
    data object Checking : DriveStatus
    data object Empty : DriveStatus
    data class Found(val backup: RemoteBackup) : DriveStatus
    data object Failed : DriveStatus
}

// The state of the Drive shared by the screens of the backup, so the dot on the card and the screen of the Drive tell the same
object DriveState {
    private val mutableStatus = MutableStateFlow<DriveStatus>(DriveStatus.Disconnected)
    val status: StateFlow<DriveStatus> = mutableStatus.asStateFlow()

    // Looks at the Drive again, a connected Drive that asks the user once more counts as an error, the screen of the Drive is where it is fixed
    suspend fun refresh(context: Context) {
        if (!DriveStore(context).isConnected.first()) {
            mutableStatus.value = DriveStatus.Disconnected
            return
        }

        mutableStatus.value = DriveStatus.Checking
        mutableStatus.value = runCatching { DriveAuth.authorize(context) }.fold(
            onSuccess = { access -> if (access is DriveAccess.Ready) look(access.token) else DriveStatus.Failed },
            onFailure = { DriveStatus.Failed }
        )
    }

    // Looks for the backup with a key that was just given
    suspend fun use(token: String) {
        mutableStatus.value = DriveStatus.Checking
        mutableStatus.value = look(token)
    }

    fun fail() {
        mutableStatus.value = DriveStatus.Failed
    }

    fun disconnect() {
        mutableStatus.value = DriveStatus.Disconnected
    }

    private suspend fun look(token: String): DriveStatus =
        runCatching { DriveClient.find(token) }.fold(
            onSuccess = { found -> if (found != null) DriveStatus.Found(found) else DriveStatus.Empty },
            onFailure = { DriveStatus.Failed }
        )
}

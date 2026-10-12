package com.wavvy.app.features.updates.data

// Android receiver and installer
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import androidx.core.content.IntentCompat
import com.wavvy.app.R

// Receives what the installer of the system answers, asks the user to confirm when it needs to and tells the updater when it failed
class UpdateInstallReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)) {
            // The system asks the user, its question is opened over the app
            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                IntentCompat.getParcelableExtra(intent, Intent.EXTRA_INTENT, Intent::class.java)
                    ?.let { context.startActivity(it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
            }

            // The app is replaced and closes by itself
            PackageInstaller.STATUS_SUCCESS -> Unit

            // The user closed the question of the system, the file is still there to install
            PackageInstaller.STATUS_FAILURE_ABORTED -> Updates.cancelInstall()

            // The installed app was signed with another key, such as one built by Android Studio, so the system does not replace it
            PackageInstaller.STATUS_FAILURE_CONFLICT, PackageInstaller.STATUS_FAILURE_INCOMPATIBLE ->
                Updates.failInstall(null, R.string.updates_error_signature)

            else -> Updates.failInstall(null)
        }
    }
}

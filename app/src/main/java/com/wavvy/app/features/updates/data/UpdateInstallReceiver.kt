package com.wavvy.app.features.updates.data

// Android receiver and installer
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import androidx.core.content.IntentCompat

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

            else -> Updates.failInstall(null)
        }
    }
}

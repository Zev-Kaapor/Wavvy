package com.wavvy.app.features.notifications.data

// Android context
import android.content.Context
// Background work
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

// Name of the work, and how often it runs
private const val WorkName = "release_watcher"
private const val RepeatHours = 12L

// The check of the artists followed, done in the background so the news come without opening the app
class ReleaseWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        ReleaseNotifier.post(applicationContext, ReleaseWatcher.check(applicationContext))
        return Result.success()
    }
}

// Puts the check to run at the pace the user chose while there is a connection, or takes it away when the user turned it off
object ReleaseWork {
    // The first time after the app opens keeps the check that exists, so opening the app never delays it, a change of the settings replaces it
    fun apply(context: Context, enabled: Boolean, hours: Long = RepeatHours, keepExisting: Boolean = false) {
        val manager = WorkManager.getInstance(context)
        if (!enabled) {
            manager.cancelUniqueWork(WorkName)
            return
        }

        val request = PeriodicWorkRequestBuilder<ReleaseWorker>(hours, TimeUnit.HOURS)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()

        manager.enqueueUniquePeriodicWork(WorkName, if (keepExisting) ExistingPeriodicWorkPolicy.KEEP else ExistingPeriodicWorkPolicy.UPDATE, request)
    }
}

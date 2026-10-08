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

// Puts the check to run every twelve hours while there is a connection
object ReleaseWork {
    // Scheduling it again keeps the one that exists, so it can be called every time the app opens
    fun schedule(context: Context) {
        val request = PeriodicWorkRequestBuilder<ReleaseWorker>(RepeatHours, TimeUnit.HOURS)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(WorkName, ExistingPeriodicWorkPolicy.KEEP, request)
    }
}

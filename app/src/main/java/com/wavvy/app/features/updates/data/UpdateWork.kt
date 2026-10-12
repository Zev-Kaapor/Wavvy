package com.wavvy.app.features.updates.data

// Android context, permissions and notifications
import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
// Background work
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.first
// Project resources
import com.wavvy.app.R
import com.wavvy.app.features.settings.data.SettingsStore

// Name of the work, the channel of the notification and the notification itself
private const val WorkName = "update_check"
private const val ChannelId = "app_updates"
private const val NotificationId = 2002

// The check of the last release done in the background, it tells with a notification when there is a newer version, unless the user paused it
class UpdateWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val settings = SettingsStore(applicationContext)
        if (settings.updates.first().paused) return Result.success()

        UpdateRepository.latest(Build.SUPPORTED_ABIS.toList()).onSuccess { info ->
            if (UpdateRepository.isNewer(info.version, Updates.installedVersion(applicationContext)) && settings.dismissedUpdate.first() != info.version) {
                UpdateNotifier.post(applicationContext, info.version)
            }
        }
        return Result.success()
    }
}

// Puts the check to run at the pace the user chose, or takes it away when the updates are paused
object UpdateWork {
    // The first time after the app opens keeps the check that exists, so opening the app never delays it, a change of the settings replaces it
    fun apply(context: Context, isPaused: Boolean, hours: Long, keepExisting: Boolean) {
        val manager = WorkManager.getInstance(context)
        if (isPaused) {
            manager.cancelUniqueWork(WorkName)
            return
        }

        val request = PeriodicWorkRequestBuilder<UpdateWorker>(hours, TimeUnit.HOURS)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()

        manager.enqueueUniquePeriodicWork(WorkName, if (keepExisting) ExistingPeriodicWorkPolicy.KEEP else ExistingPeriodicWorkPolicy.UPDATE, request)
    }
}

// Tells the user about a newer version with a system notification, a tap opens the app
object UpdateNotifier {
    // The permission is checked before anything is shown
    @SuppressLint("MissingPermission")
    fun post(context: Context, version: String) {
        val canPost = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        if (!canPost) return

        val manager = NotificationManagerCompat.from(context)
        manager.createNotificationChannel(
            NotificationChannelCompat.Builder(ChannelId, NotificationManagerCompat.IMPORTANCE_DEFAULT)
                .setName(context.getString(R.string.updates_channel))
                .build()
        )

        val launch = context.packageManager.getLaunchIntentForPackage(context.packageName)
        val opens = PendingIntent.getActivity(context, 0, launch, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

        manager.notify(
            NotificationId,
            NotificationCompat.Builder(context, ChannelId)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(context.getString(R.string.updates_notification_title))
                .setContentText(context.getString(R.string.updates_notification_text, version))
                .setContentIntent(opens)
                .setAutoCancel(true)
                .build()
        )
    }
}

package com.wavvy.app.features.recap.data

// Android context, permissions and notifications
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.net.toUri
// Background work
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
// Time and flows
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
// Project resources
import com.wavvy.app.R
import com.wavvy.app.core.navigation.RecapLink
import com.wavvy.app.features.notifications.data.ReleaseNotifier
import com.wavvy.app.features.settings.data.SettingsStore

// Channel and number of the notification of the retrospective, and the name of the work that watches for its day
private const val ChannelId = "recap"
private const val NotificationId = 2002
private const val WorkName = "recap_notice"
private const val RepeatHours = 24L

// The months when the retrospective of the year is told about, and the hour of the day it is told at
private val NoticeMonths = setOf(12)
private const val NoticeHour = 9

// The retrospective of the year that is ready: it is told about once, on the day, with a notification of the system, and stays in the Activity until it is deleted
object RecapNotice {
    // The retrospective that is waiting to be seen, and if it was already read in the Activity
    data class Notice(val year: Int, val isRead: Boolean, val at: Long)

    // The year of the retrospective told about, none out of its months
    private fun year(today: LocalDate = LocalDate.now()): Int? = today.year.takeIf { today.monthValue in NoticeMonths }

    // The notice while there is one and it was not deleted
    fun notice(store: SettingsStore): Flow<Notice?> =
        combine(store.recapRead, store.recapDismissed) { read, dismissed ->
            val year = year()
            if (year == null || dismissed == year) null else Notice(year, read == year, LocalDate.of(year, NoticeMonths.min(), 1).atTime(NoticeHour, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli())
        }

    // How many of them wait to be read, for the number on the bell
    fun unread(context: Context): Flow<Int> = notice(SettingsStore(context)).map { if (it != null && !it.isRead) 1 else 0 }

    // The Activity was left, so the notice was seen
    suspend fun markRead(context: Context) {
        year()?.let { SettingsStore(context).setRecapRead(it) }
    }

    suspend fun dismiss(context: Context, year: Int) {
        SettingsStore(context).setRecapDismissed(year)
    }

    // Puts the check to run once a day, at the hour of the notice, so the notification comes on the day even when the app is closed
    fun schedule(context: Context) {
        val now = LocalDateTime.now()
        var next = now.toLocalDate().atTime(NoticeHour, 0)
        if (!next.isAfter(now)) next = next.plusDays(1)

        val request = PeriodicWorkRequestBuilder<RecapNoticeWorker>(RepeatHours, TimeUnit.HOURS)
            .setInitialDelay(Duration.between(now, next).toMillis(), TimeUnit.MILLISECONDS)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(WorkName, ExistingPeriodicWorkPolicy.KEEP, request)
    }

    // Tells about the retrospective once for each year, and not when the user already saw or deleted it or turned the notifications off
    suspend fun announce(context: Context) {
        val year = year() ?: return
        val store = SettingsStore(context)
        if (store.recapNotified.first() == year) return
        store.setRecapNotified(year)

        if (!store.notifications.first().enabled || store.recapRead.first() == year || store.recapDismissed.first() == year) return
        post(context, year)
    }

    // The permission is checked by canPost before anything is shown
    @SuppressLint("MissingPermission")
    private fun post(context: Context, year: Int) {
        if (!ReleaseNotifier.canPost(context)) return

        val manager = NotificationManagerCompat.from(context)
        manager.createNotificationChannel(
            NotificationChannelCompat.Builder(ChannelId, NotificationManagerCompat.IMPORTANCE_DEFAULT)
                .setName(context.getString(R.string.recap_notice_channel))
                .build()
        )

        // A tap opens the app on the retrospective
        val launch = context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
            action = Intent.ACTION_VIEW
            data = RecapLink.toUri()
        }
        val opens = PendingIntent.getActivity(context, 0, launch, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

        val notification = NotificationCompat.Builder(context, ChannelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.recap_notice_title, year))
            .setContentText(context.getString(R.string.recap_notice_text))
            .setContentIntent(opens)
            .setAutoCancel(true)
            .build()

        manager.notify(NotificationId, notification)
    }
}

// The daily check for the day of the retrospective
class RecapNoticeWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        RecapNotice.announce(applicationContext)
        return Result.success()
    }
}

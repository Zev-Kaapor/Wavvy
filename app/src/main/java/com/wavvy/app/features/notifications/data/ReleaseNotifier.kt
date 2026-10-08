package com.wavvy.app.features.notifications.data

// Android context, permissions and notifications
import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
// Project resources
import com.wavvy.app.R
import com.wavvy.app.core.history.ReleaseEntity

// Channel of the notifications of new releases, and the notification they are shown as
private const val ChannelId = "new_releases"
private const val NotificationId = 2001

// The sentence of a release of an artist, as the Activity writes it, with the kind of the release in its words
fun ReleaseEntity.sentenceRes(): Int =
    when (kind?.trim()?.lowercase()) {
        "single" -> R.string.notifications_released_single
        "ep" -> R.string.notifications_released_ep
        else -> R.string.notifications_released_album
    }

// Tells the user about the new releases of the artists followed, with a system notification
object ReleaseNotifier {
    // One notification for the news found in a check, the sentence of the release when there is one and the list of them when there are more
    // The permission is checked by canPost before anything is shown
    @SuppressLint("MissingPermission")
    fun post(context: Context, releases: List<ReleaseEntity>) {
        if (releases.isEmpty() || !canPost(context)) return

        val manager = NotificationManagerCompat.from(context)
        manager.createNotificationChannel(
            NotificationChannelCompat.Builder(ChannelId, NotificationManagerCompat.IMPORTANCE_DEFAULT)
                .setName(context.getString(R.string.notifications_channel))
                .build()
        )

        val lines = releases.map { context.getString(it.sentenceRes(), it.artistName, it.title) }
        val title = if (releases.size == 1) context.getString(R.string.app_name) else context.resources.getString(R.string.notifications_many, releases.size)

        // A tap opens the app
        val launch = context.packageManager.getLaunchIntentForPackage(context.packageName)
        val opens = PendingIntent.getActivity(context, 0, launch, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

        val notification = NotificationCompat.Builder(context, ChannelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(lines.first())
            .setStyle(NotificationCompat.BigTextStyle().bigText(lines.joinToString("\n")))
            .setContentIntent(opens)
            .setAutoCancel(true)
            .build()

        manager.notify(NotificationId, notification)
    }

    // Android 13 and newer need the permission, without it the news only show in the Activity
    private fun canPost(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
}

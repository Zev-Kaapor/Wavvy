package com.wavvy.app.core.download

// Android notification
import android.app.Notification
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadManager
import androidx.media3.exoplayer.offline.DownloadService
import androidx.media3.exoplayer.scheduler.Scheduler
// Project resources
import com.wavvy.app.R

// Keeps the downloads going while the app is closed, with the notification that shows what is coming
@UnstableApi
class WavvyDownloadService : DownloadService(
    NotificationId,
    ForegroundUpdateMillis,
    Downloads.ChannelId,
    R.string.download_channel,
    0
) {
    override fun getDownloadManager(): DownloadManager {
        Downloads.initialize(this)
        return Downloads.manager
    }

    // The downloads wait for the app to be opened again after the device restarts
    override fun getScheduler(): Scheduler? = null

    override fun getForegroundNotification(downloads: MutableList<Download>, notMetRequirements: Int): Notification {
        val message = if (downloads.size == 1) {
            Downloads.titleOf(downloads[0])
        } else {
            resources.getQuantityString(R.plurals.download_notification_songs, downloads.size, downloads.size)
        }

        return Downloads.notificationHelper(this).buildProgressNotification(this, R.drawable.ic_notification, null, message, downloads, notMetRequirements)
    }

    private companion object {
        const val NotificationId = 2

        // How often the notification of the progress is drawn again
        const val ForegroundUpdateMillis = 1000L
    }
}

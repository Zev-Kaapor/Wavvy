package com.wavvy.app.core.drive

// Android context
import android.content.Context
import androidx.annotation.StringRes
// Background work
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.security.MessageDigest
import java.util.concurrent.TimeUnit
// Coroutines
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
// Project resources
import com.wavvy.app.R
import com.wavvy.app.core.backup.BackupCrypto
import com.wavvy.app.core.backup.BackupData
import com.wavvy.app.core.backup.BackupSection

// Name of the work
private const val WorkName = "drive_backup"

// How often the app saves to the Drive, a backup has to be fresh, so the longest wait is a day
enum class DriveInterval(val hours: Long, @StringRes val titleRes: Int) {
    TwoHours(2, R.string.backup_drive_interval_2),
    SixHours(6, R.string.backup_drive_interval_6),
    TwelveHours(12, R.string.backup_drive_interval_12),
    OneDay(24, R.string.backup_drive_interval_24)
}

// The key of the part of the file that holds the data, so the time it was made does not count as a change
private const val SectionsKey = "sections"

// The save to the Drive, done in the background on a network without a data plan so the backup follows the phone without the user asking
class DriveWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result =
        runCatching { DriveBackup.saveByItself(applicationContext) }.fold(onSuccess = { Result.success() }, onFailure = { Result.retry() })
}

// What the automatic save does, which never replaces a backup that has a password or one this phone has not seen
object DriveBackup {
    suspend fun saveByItself(context: Context) {
        val store = DriveStore(context)
        if (!store.isConnected.first() || !store.autoSave.first() || store.isLocked.first()) return

        // A key that needs the user again is left for the screen of the Drive to fix
        val token = (DriveAuth.authorize(context) as? DriveAccess.Ready)?.token ?: return

        val payload = withContext(Dispatchers.IO) { BackupData.collect(context, BackupSection.entries.toSet()) }
        if (BackupData.available(payload).values.all { it == 0 }) return

        val hash = sha256(payload.getJSONObject(SectionsKey).toString())
        if (hash == store.lastHash.first()) return

        val remote = DriveClient.find(token)
        if (remote != null && remote.modifiedAt.toEpochMilli() > store.syncedAt.first()) return

        val bytes = withContext(Dispatchers.Default) { BackupCrypto.encrypt(payload.toString().toByteArray(Charsets.UTF_8), null) }
        DriveClient.save(token, bytes, remote?.id)
        store.setSyncedAt(System.currentTimeMillis())
        store.setLastHash(hash)
        DriveState.use(token)
    }

    private fun sha256(text: String): String =
        MessageDigest.getInstance("SHA-256").digest(text.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }
}

// Puts the save to run at the pace the user chose while the Drive is connected and the user wants it, or takes it away
object DriveWork {
    fun apply(context: Context, enabled: Boolean, hours: Long, keepExisting: Boolean = false) {
        val manager = WorkManager.getInstance(context)
        if (!enabled) {
            manager.cancelUniqueWork(WorkName)
            return
        }

        val request = PeriodicWorkRequestBuilder<DriveWorker>(hours, TimeUnit.HOURS)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.UNMETERED).build())
            .build()

        manager.enqueueUniquePeriodicWork(WorkName, if (keepExisting) ExistingPeriodicWorkPolicy.KEEP else ExistingPeriodicWorkPolicy.UPDATE, request)
    }
}

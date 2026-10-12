package com.wavvy.app.features.updates.data

// Android context, installer and settings of the system
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.annotation.StringRes
// Files and network
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
// Coroutines and reactive flows
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
// Project resources
import com.wavvy.app.R
import com.wavvy.app.features.settings.data.SettingsStore

// What the updater is doing, waiting, looking, knowing the app is the newest, knowing there is a newer one, downloading it, having it ready, asking the system to install it or failing
sealed interface UpdateState {
    data object Idle : UpdateState
    data object Checking : UpdateState
    data class UpToDate(val info: UpdateInfo) : UpdateState
    data class Available(val info: UpdateInfo) : UpdateState
    data class Downloading(val info: UpdateInfo, val bytes: Long, val total: Long) : UpdateState {
        val percent: Int get() = if (total > 0) ((bytes * PercentTotal) / total).toInt().coerceIn(0, PercentTotal) else 0
    }
    data class Ready(val info: UpdateInfo, val file: File) : UpdateState
    data class Installing(val info: UpdateInfo) : UpdateState
    data class NeedsPermission(val info: UpdateInfo, val file: File) : UpdateState
    data class Failed(@StringRes val messageRes: Int, val info: UpdateInfo? = null) : UpdateState
}

private const val PercentTotal = 100

// The updater of the app, it looks at the last release on GitHub, downloads the file that fits the device and gives it to the installer of the system
object Updates {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var job: Job? = null

    private val mutableState = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val state: StateFlow<UpdateState> = mutableState.asStateFlow()

    // The release that is asked about when the app opens, it stays until the user answers or says later
    private val mutablePrompt = MutableStateFlow<UpdateInfo?>(null)
    val prompt: StateFlow<UpdateInfo?> = mutablePrompt.asStateFlow()

    fun installedVersion(context: Context): String = context.packageManager.getPackageInfo(context.packageName, 0).versionName.orEmpty()

    // Looks at the last release, a check that the user asked shows what it found and one that was not asked only asks when there is something new
    fun check(context: Context, isAsked: Boolean = true) {
        if (job?.isActive == true) return
        val app = context.applicationContext
        if (isAsked) mutableState.value = UpdateState.Checking

        job = scope.launch {
            SettingsStore(app).setLastUpdateCheck(System.currentTimeMillis())
            UpdateRepository.latest(Build.SUPPORTED_ABIS.toList())
                .onSuccess { info ->
                    if (UpdateRepository.isNewer(info.version, installedVersion(app))) {
                        // A file that was downloaded before is used again
                        val file = info.asset?.let { fileOf(app, it) }?.takeIf { it.exists() && it.length() == info.asset.sizeBytes }
                        mutableState.value = if (file != null) UpdateState.Ready(info, file) else UpdateState.Available(info)

                        // The question when the app opens, once for each version the user did not answer
                        if (!isAsked && SettingsStore(app).dismissedUpdate.first() != info.version) mutablePrompt.value = info
                    } else if (isAsked) {
                        mutableState.value = UpdateState.UpToDate(info)
                    }
                }
                .onFailure { if (isAsked) mutableState.value = UpdateState.Failed(R.string.updates_error_check) }
        }
    }

    // Says the question of the update was answered with later, so it is not asked again for this version
    fun postpone(context: Context) {
        val info = mutablePrompt.value ?: return
        mutablePrompt.value = null
        scope.launch { SettingsStore(context.applicationContext).setDismissedUpdate(info.version) }
    }

    // Takes the question away, the user went to the screen of the updates
    fun clearPrompt() {
        mutablePrompt.value = null
    }

    // Downloads the file of the release, the progress shows as it comes and a cancel takes the part that came away
    fun download(context: Context) {
        val current = mutableState.value
        val info = (current as? UpdateState.Available)?.info ?: (current as? UpdateState.Failed)?.info ?: return
        val asset = info.asset ?: run {
            mutableState.value = UpdateState.Failed(R.string.updates_error_no_file, info)
            return
        }
        val app = context.applicationContext
        val target = fileOf(app, asset)

        job?.cancel()
        mutableState.value = UpdateState.Downloading(info, 0L, asset.sizeBytes)
        job = scope.launch {
            val temporary = File(target.parentFile, target.name + ".part")
            try {
                val connection = URL(asset.url).openConnection() as HttpURLConnection
                try {
                    connection.connectTimeout = TimeoutMillis
                    connection.readTimeout = TimeoutMillis
                    if (connection.responseCode != HttpOk) throw IllegalStateException("GitHub answered ${connection.responseCode}")

                    val total = connection.contentLengthLong.takeIf { it > 0 } ?: asset.sizeBytes
                    var bytes = 0L
                    var lastUpdate = 0L
                    connection.inputStream.use { input ->
                        temporary.outputStream().use { output ->
                            val buffer = ByteArray(BufferSize)
                            while (isActive) {
                                val read = input.read(buffer)
                                if (read < 0) break
                                output.write(buffer, 0, read)
                                bytes += read

                                // The screen is told a few times in a second, not on every piece
                                val now = System.currentTimeMillis()
                                if (now - lastUpdate >= ProgressMillis) {
                                    lastUpdate = now
                                    mutableState.value = UpdateState.Downloading(info, bytes, total)
                                }
                            }
                        }
                    }
                } finally {
                    connection.disconnect()
                }

                if (!isActive) return@launch
                temporary.renameTo(target)
                mutableState.value = UpdateState.Ready(info, target)
            } catch (failure: Exception) {
                temporary.delete()
                if (isActive) mutableState.value = UpdateState.Failed(R.string.updates_error_download, info)
            }
        }
    }

    // Stops a download that is going and goes back to the release that was found
    fun cancelDownload() {
        val current = mutableState.value as? UpdateState.Downloading ?: return
        job?.cancel()
        mutableState.value = UpdateState.Available(current.info)
    }

    // Gives the file to the installer of the system, which asks the user to confirm, the app must be allowed to install apps first
    fun install(context: Context) {
        val current = mutableState.value
        val (info, file) = when (current) {
            is UpdateState.Ready -> current.info to current.file
            is UpdateState.NeedsPermission -> current.info to current.file
            else -> return
        }
        val app = context.applicationContext

        if (!app.packageManager.canRequestPackageInstalls()) {
            mutableState.value = UpdateState.NeedsPermission(info, file)
            app.startActivity(
                Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${app.packageName}")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
            return
        }

        mutableState.value = UpdateState.Installing(info)
        scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val installer = app.packageManager.packageInstaller
                    val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL).apply { setAppPackageName(app.packageName) }
                    val id = installer.createSession(params)
                    installer.openSession(id).use { session ->
                        session.openWrite(SessionFileName, 0, file.length()).use { output ->
                            file.inputStream().use { it.copyTo(output) }
                            session.fsync(output)
                        }

                        val receiver = PendingIntent.getBroadcast(
                            app, id, Intent(app, UpdateInstallReceiver::class.java),
                            PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                        )
                        session.commit(receiver.intentSender)
                    }
                }
            } catch (failure: Exception) {
                mutableState.value = UpdateState.Failed(R.string.updates_error_install, info)
            }
        }
    }

    // The installer said the file could not be installed
    fun failInstall(info: UpdateInfo?) {
        mutableState.value = UpdateState.Failed(R.string.updates_error_install, info ?: (mutableState.value as? UpdateState.Installing)?.info)
    }

    // The file of a release, in the folder of the app that the system can clean
    private fun fileOf(context: Context, asset: UpdateAsset): File = File(File(context.cacheDir, FolderName).also { it.mkdirs() }, asset.name)

    private const val FolderName = "updates"
    private const val SessionFileName = "wavvy"
    private const val BufferSize = 64 * 1024
    private const val ProgressMillis = 250L
    private const val TimeoutMillis = 30_000
    private const val HttpOk = 200
}

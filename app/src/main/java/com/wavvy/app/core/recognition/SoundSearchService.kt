package com.wavvy.app.core.recognition

// Android service, windows and intents
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
// Project resources
import com.wavvy.app.MainActivity
import com.wavvy.app.R
import com.wavvy.app.core.designsystem.theme.WavvyMotion
import com.wavvy.app.core.designsystem.theme.WavvyTheme
import com.wavvy.app.core.language.AppLanguage
import com.wavvy.app.features.search.ui.components.SoundGlow
import com.wavvy.app.features.search.ui.components.SoundPill
import com.wavvy.app.features.search.ui.components.SoundSearch
import com.wavvy.app.features.search.ui.components.SoundPhase
import com.wavvy.app.features.search.ui.components.SoundSearchRunner
import com.wavvy.app.features.settings.data.SettingsStore

// Channel and notification of the service while it listens
private const val ChannelId = "sound_search"
private const val NotificationId = 2002

// The search by sound over whatever app is open, drawn as windows of the system and not as a screen of the app, so the app under it does not pause
// One window has the light of the bottom edge and takes no touch, the other has the pill and takes the touches on it
class SoundSearchService : Service(), SavedStateRegistryOwner {
    private val registry = LifecycleRegistry(this)
    private val savedState = SavedStateRegistryController.create(this)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var runner: SoundSearchRunner? = null
    private val windows = mutableListOf<View>()
    private var isShown by mutableStateOf(true)

    override val lifecycle: Lifecycle get() = registry
    override val savedStateRegistry: SavedStateRegistry get() = savedState.savedStateRegistry

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(AppLanguage.wrap(newBase))
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        savedState.performAttach()
        savedState.performRestore(null)
        registry.currentState = Lifecycle.State.RESUMED
    }

    // A second start while it is open listens again
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // The microphone is only given to a service that starts while a screen of the app is in front, so it is promoted at once
        if (runCatching { startAsForeground() }.isFailure) {
            stopSelf()
            return START_NOT_STICKY
        }
        isListening.value = true
        if (windows.isEmpty()) show() else runner?.listen()
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        isListening.value = false
        windows.forEach { runCatching { getSystemService(WindowManager::class.java).removeView(it) } }
        windows.clear()
        runner?.cancel()
        scope.cancel()
        registry.currentState = Lifecycle.State.DESTROYED
        super.onDestroy()
    }

    // The notification that the microphone is in use
    private fun startAsForeground() {
        NotificationManagerCompat.from(this).createNotificationChannel(
            NotificationChannelCompat.Builder(ChannelId, NotificationManagerCompat.IMPORTANCE_LOW)
                .setName(getString(R.string.search_sound_channel))
                .build()
        )
        val notification = NotificationCompat.Builder(this, ChannelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(getString(R.string.search_sound_listening))
            .setOngoing(true)
            .build()
        ServiceCompat.startForeground(
            this,
            NotificationId,
            notification,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE else 0
        )
    }

    // Draws the light over the whole screen and the pill at its bottom, both with the state of the same search
    private fun show() {
        val store = SettingsStore(this)
        val firstTheme = runBlocking { store.themeMode.first() }
        val search = SoundSearchRunner(this, scope, onFound = ::openSearch).also { runner = it }
        val manager = getSystemService(WindowManager::class.java)

        fun window(content: @androidx.compose.runtime.Composable (SoundSearch) -> Unit): ComposeView =
            ComposeView(this).apply {
                setViewTreeLifecycleOwner(this@SoundSearchService)
                setViewTreeSavedStateRegistryOwner(this@SoundSearchService)
                setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
                setContent {
                    val themeMode by store.themeMode.collectAsState(initial = firstTheme)
                    WavvyTheme(themeMode = themeMode) {
                        content(
                            SoundSearch(
                                phase = search.phase,
                                level = search.level,
                                isRefused = search.isRefused(),
                                listenAgain = search::listen,
                                openSettings = search::openSettings
                            )
                        )
                    }
                }
            }

        // The light, over everything including the bars of the system, and no touch is taken
        val glow = window { SoundGlow(search = it, isShown = isShown, modifier = Modifier.fillMaxSize()) }
        manager.addView(
            glow,
            layout(
                height = manager.maximumWindowMetrics.bounds.height(),
                flags = WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
            )
        )

        // The pill, as high as it is, at the bottom, and only the pill takes touches
        val pill = window { SoundPill(search = it, isShown = isShown, onClose = ::leave) }
        manager.addView(
            pill,
            layout(height = WindowManager.LayoutParams.WRAP_CONTENT, flags = WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
        )
        windows += listOf(glow, pill)
        search.listen()

        // A search that found nothing goes away by itself after a moment, unless it is tried again
        scope.launch {
            snapshotFlow { search.phase }.collectLatest { phase ->
                if (phase is SoundPhase.Failed) {
                    delay(WavvyMotion.SoundFailedHoldMillis.toLong())
                    leave()
                }
            }
        }
    }

    // The window of an overlay that never takes the focus, so the keyboard and the keys stay with the app under it, drawn by the graphics chip as a screen is
    private fun layout(height: Int, flags: Int) = WindowManager.LayoutParams(
        WindowManager.LayoutParams.MATCH_PARENT,
        height,
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
        flags or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
        PixelFormat.TRANSLUCENT
    ).apply {
        gravity = Gravity.BOTTOM
        layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
    }

    // The app comes to the front with the name of the song, and the service is done
    private fun openSearch(query: String) {
        SoundSearchRequest.offer(query)
        startActivity(Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP))
        leave()
    }

    // The light and the pill fade out and then the service is done
    private fun leave() {
        isShown = false
        runner?.cancel()
        scope.launch {
            delay(WavvyMotion.SoundShowMillis.toLong())
            stopSelf()
        }
    }

    companion object {
        // True while the service is on the air, the screen that started it leaves when it is
        val isListening = MutableStateFlow(false)

        // The windows over the other apps need the permission of the user to draw over them
        fun canShow(context: Context): Boolean = Settings.canDrawOverlays(context)
    }
}

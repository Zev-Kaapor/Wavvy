package com.wavvy.app

// Android activity components
import android.content.Intent
import android.os.Build
import android.content.Context
import android.os.Bundle
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import com.wavvy.app.core.network.WavvyProxy
import com.wavvy.app.core.download.DownloadArtInterceptor
import com.wavvy.app.core.language.AppLanguage
import com.wavvy.app.core.download.Downloads
import com.wavvy.app.core.drive.DriveStore
import com.wavvy.app.core.drive.DriveWork
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.lifecycleScope
// Window styling
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
// Coroutines
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.launch
// Project resources
import com.wavvy.app.core.designsystem.theme.WavvyTheme
import com.wavvy.app.core.navigation.LinkOpener
import com.wavvy.app.core.navigation.MainTab
import com.wavvy.app.core.navigation.WavvyApp
import com.wavvy.app.core.innertube.OfflineCache
import com.wavvy.app.features.settings.data.ContentPrefs
import com.wavvy.app.features.settings.data.PrivacyPrefs
import com.wavvy.app.features.settings.data.SettingsStore
import com.wavvy.app.features.updates.data.UpdateWork
import com.wavvy.app.features.updates.data.Updates
import com.wavvy.app.core.playback.StreamResolver
import com.wavvy.app.features.notifications.data.ReleaseWork

// Wait after the start before the playback engine warms up, so the Home loads first
private const val PrewarmDelayMs = 2500L

// Single activity of the app, immersive with the content under the cutout
class MainActivity : ComponentActivity() {
    // The language the user picked, for the versions of Android that do not keep it themselves
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(AppLanguage.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setupImmersiveMode()
        warmUpPlayback(isFirstStart = savedInstanceState == null)

        // A link that opened the app waits for the main screen, a rebuilt activity does not open it again
        if (savedInstanceState == null) LinkOpener.offer(intent)

        // The check of the new releases and the theme follow what the user chose in the settings
        val store = SettingsStore(this)
        lifecycleScope.launch {
            var isFirst = true
            store.notifications.distinctUntilChanged().collect {
                ReleaseWork.apply(this@MainActivity, it.enabled, it.interval.hours, keepExisting = isFirst)
                isFirst = false
            }
        }
        val firstTheme = runBlocking { store.themeMode.first() }

        // The tab the user chose to open on, read before the screen is drawn so the app opens straight on it
        val firstTab = runBlocking { store.defaultTab.first() }.let { name -> MainTab.entries.firstOrNull { it.name == name } ?: MainTab.HOME }

        // The save to the Drive runs at the pace the user chose while the Drive is connected and the user turned it on
        val drive = DriveStore(this)
        lifecycleScope.launch {
            var isFirst = true
            combine(drive.isConnected, drive.autoSave, drive.autoHours) { isConnected, isAuto, hours -> (isConnected && isAuto) to hours }.distinctUntilChanged().collect { (isOn, hours) ->
                DriveWork.apply(this@MainActivity, isOn, hours, keepExisting = isFirst)
                isFirst = false
            }
        }

        // The screen is hidden from screenshots and from the list of recent apps while the user asked for it
        lifecycleScope.launch {
            store.privacy.map { it.blockScreenshots }.distinctUntilChanged().collect { isBlocked ->
                if (isBlocked) window.addFlags(WindowManager.LayoutParams.FLAG_SECURE) else window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
            }
        }

        // The check for a newer version runs at the pace the user chose, and once when the app opens if it is due
        lifecycleScope.launch {
            var isFirst = true
            store.updates.distinctUntilChanged().collect {
                UpdateWork.apply(this@MainActivity, it.paused, it.interval.hours, keepExisting = isFirst)
                isFirst = false
            }
        }
        lifecycleScope.launch {
            val settings = store.updates.first()
            val isDue = System.currentTimeMillis() - store.lastUpdateCheck.first() >= settings.interval.hours * MillisPerHour
            if (!settings.paused && isDue) Updates.check(this@MainActivity, isAsked = false)
        }

        // The pictures saved with the downloads are used before the ones of the internet, in every place that shows a picture
        SingletonImageLoader.setSafe { context -> ImageLoader.Builder(context).components {
                add(OkHttpNetworkFetcherFactory(callFactory = { WavvyProxy.client }))
                add(DownloadArtInterceptor)
            }.build() }

        setContent {
            val themeMode by store.themeMode.collectAsState(initial = firstTheme)

            WavvyTheme(themeMode = themeMode) {
                WavvyApp(startTab = firstTab)
            }
        }
    }

    // A link that comes while the app is open
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        LinkOpener.offer(intent)
    }

    // The system bars come back on a swipe, so they are hidden again when the window regains focus
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) setupImmersiveMode()
    }

    // Loads the player config, the cipher and the token page off the path of the first song, as Metrolist does
    private fun warmUpPlayback(isFirstStart: Boolean) {
        StreamResolver.initialize(this)
        OfflineCache.initialize(this)
        ContentPrefs.follow(this)
        PrivacyPrefs.follow(this)
        if (!isFirstStart) return

        lifecycleScope.launch(Dispatchers.IO) {
            delay(PrewarmDelayMs)
            runCatching { StreamResolver.prewarm() }

            // The downloads are read at the start, with or without internet, so the lists can show which songs are downloaded
            Downloads.initialize(this@MainActivity)

            // The pictures of the downloads that are not saved yet are saved while there is internet
            if (OfflineCache.isOnline()) Downloads.backfillArt(this@MainActivity)
        }
    }

    // Content draws under the cutout and the system bars stay hidden until a swipe
    private fun setupImmersiveMode() {
        WindowCompat.setDecorFitsSystemWindows(window, false)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }

        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.hide(WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }
}

// Milliseconds in an hour, to tell when the check for updates is due
private const val MillisPerHour = 60L * 60L * 1000L

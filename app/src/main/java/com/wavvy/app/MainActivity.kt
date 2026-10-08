package com.wavvy.app

// Android activity components
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
// Window styling
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
// Coroutines
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
// Project resources
import com.wavvy.app.core.designsystem.theme.WavvyTheme
import com.wavvy.app.core.navigation.WavvyApp
import com.wavvy.app.core.playback.StreamResolver
import com.wavvy.app.features.notifications.data.ReleaseWork

// Wait after the start before the playback engine warms up, so the Home loads first
private const val PrewarmDelayMs = 2500L

// Single activity of the app, immersive with the content under the cutout
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setupImmersiveMode()
        warmUpPlayback(isFirstStart = savedInstanceState == null)
        ReleaseWork.schedule(this)

        setContent {
            WavvyTheme {
                WavvyApp()
            }
        }
    }

    // The system bars come back on a swipe, so they are hidden again when the window regains focus
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) setupImmersiveMode()
    }

    // Loads the player config, the cipher and the token page off the path of the first song, as Metrolist does
    private fun warmUpPlayback(isFirstStart: Boolean) {
        StreamResolver.initialize(this)
        if (!isFirstStart) return

        lifecycleScope.launch(Dispatchers.IO) {
            delay(PrewarmDelayMs)
            runCatching { StreamResolver.prewarm() }
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

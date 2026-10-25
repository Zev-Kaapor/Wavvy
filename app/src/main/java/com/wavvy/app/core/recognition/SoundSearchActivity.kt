package com.wavvy.app.core.recognition

// Android activity components
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.runBlocking
// Project resources
import com.wavvy.app.MainActivity
import com.wavvy.app.core.designsystem.theme.WavvyMotion
import com.wavvy.app.core.designsystem.theme.WavvyTheme
import com.wavvy.app.core.language.AppLanguage
import com.wavvy.app.features.search.ui.components.SoundOverlay
import com.wavvy.app.features.search.ui.components.SoundPhase
import com.wavvy.app.features.search.ui.components.rememberSoundSearch
import com.wavvy.app.features.settings.data.SettingsStore

// How long this screen waits for the service before it leaves anyway
private const val RelayMillis = 1500L

// The search by sound over whatever app is open, the tile of the quick settings opens it, it listens on top and when the song is found the app opens on its search
// The window is only as tall as the light and the pill, so the touches on the rest of the screen go to the app that is open
class SoundSearchActivity : ComponentActivity() {
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(AppLanguage.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // With everything allowed this screen only starts the service, which draws over the apps without pausing them
        if (SoundSearchService.canShow(this) && MusicRecognizer.hasPermission(this)) {
            relayToService()
            return
        }

        // At the bottom, across the screen, with no background, and touches outside it go through
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        window.setGravity(Gravity.BOTTOM)
        window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        window.addFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
        window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)

        val store = SettingsStore(this)
        val firstTheme = runBlocking { store.themeMode.first() }
        setContent {
            val themeMode by store.themeMode.collectAsState(initial = firstTheme)

            WavvyTheme(themeMode = themeMode) {
                val search = rememberSoundSearch(onFound = ::openSearch)
                SoundOverlay(search = search, onClose = ::finish)

                // A search that found nothing goes away by itself after a moment, unless it is tried again
                LaunchedEffect(search.phase) {
                    if (search.phase is SoundPhase.Failed) {
                        delay(WavvyMotion.SoundFailedHoldMillis.toLong())
                        finish()
                    }
                }
            }
        }
    }

    // The microphone is only given to a service that starts while a screen of the app is in front, so this one stays until the service is listening
    private fun relayToService() {
        startForegroundService(Intent(this, SoundSearchService::class.java))
        lifecycleScope.launch {
            withTimeoutOrNull(RelayMillis) { SoundSearchService.isListening.first { it } }
            finish()
        }
    }

    // The app comes to the front with the name of the song, and this screen is done
    private fun openSearch(query: String) {
        SoundSearchRequest.offer(query)
        startActivity(Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP))
        finish()
    }
}

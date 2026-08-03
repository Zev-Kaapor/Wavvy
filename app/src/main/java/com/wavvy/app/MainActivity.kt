package com.wavvy.app

// Android activity foundations
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
// Core window styling utilities
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
// Project components
import com.wavvy.app.core.navigation.MainScreen
import com.wavvy.app.core.designsystem.theme.WavvyTheme
import com.wavvy.app.core.designsystem.bottomsheet.ProvideMenuState
import com.wavvy.app.core.data.local.SettingsStorage

// Infrastructure setup components
class MainActivity : ComponentActivity() {

    // Infrastructure lifecycle management
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setupImmersiveMode()

        setContent {
            val settingsStorage = remember { SettingsStorage(this) }
            var currentTheme by remember { mutableStateOf(settingsStorage.getThemeMode()) }
            var currentDefaultTab by remember { mutableStateOf(settingsStorage.getDefaultTab()) }

            WavvyTheme(themeMode = currentTheme) {
                ProvideMenuState {
                    MainScreen(
                        currentTheme = currentTheme,
                        onThemeChange = { mode ->
                            currentTheme = mode
                            settingsStorage.saveThemeMode(mode)
                        },
                        currentDefaultTab = currentDefaultTab,
                        onDefaultTabChange = { tab ->
                            currentDefaultTab = tab
                            settingsStorage.saveDefaultTab(tab)
                        }
                    )
                }
            }
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            setupImmersiveMode()
        }
    }

    private fun setupImmersiveMode() {
        WindowCompat.setDecorFitsSystemWindows(window, false)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }

        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.hide(WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }
}

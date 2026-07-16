package com.lonewolf.wavvy

// Android system structure and packages
import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
// Android activity foundations
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
// Compose state management
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
// Core window styling utilities
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
// Project data components
import com.lonewolf.wavvy.data.SettingsStorage
// Project UI components and frameworks
import com.lonewolf.wavvy.ui.navigation.DefaultTab
import com.lonewolf.wavvy.ui.navigation.MainScreen
import com.lonewolf.wavvy.ui.theme.ThemeMode
import com.lonewolf.wavvy.ui.theme.WavvyTheme

// Infrastructure setup components
class MainActivity : ComponentActivity() {

    // Storage driver reference
    private lateinit var settingsStorage: SettingsStorage

    // Theme state management
    private var currentThemeMode by mutableStateOf(ThemeMode.SYSTEM)

    // Startup tab preference state management
    private var currentDefaultTab by mutableStateOf(DefaultTab.HOME)

    // Infrastructure lifecycle management
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Local storage initialization
        settingsStorage = SettingsStorage(this)

        // Load persisted preferences on startup
        loadSavedSettings()

        // System UI edge-to-edge support
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            ),
            navigationBarStyle = SystemBarStyle.auto(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            )
        )

        // Immersive mode configuration
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.hide(WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        // Display cutout management
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }

        // Deep link intent processing
        handleIntent(intent)

        setContent {
            // Main app theme wrapper
            WavvyTheme(themeMode = currentThemeMode) {
                // Entry point screen
                MainScreen(
                    currentTheme = currentThemeMode,
                    onThemeChange = { newMode ->
                        currentThemeMode = newMode
                        settingsStorage.saveString(SettingsStorage.KEY_THEME_MODE, newMode.name)
                    },
                    currentDefaultTab = currentDefaultTab,
                    onDefaultTabChange = { newTab ->
                        currentDefaultTab = newTab
                        settingsStorage.saveString(SettingsStorage.KEY_DEFAULT_TAB, newTab.name)
                    }
                )
            }
        }
    }

    // Deep link intent processing
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    // Load configurations from settings storage wrapper
    private fun loadSavedSettings() {
        try {
            val savedThemeStr = settingsStorage.getString(SettingsStorage.KEY_THEME_MODE, ThemeMode.SYSTEM.name)
            currentThemeMode = ThemeMode.valueOf(savedThemeStr)
        } catch (_: Exception) {
            currentThemeMode = ThemeMode.SYSTEM
        }

        try {
            val savedTabStr = settingsStorage.getString(SettingsStorage.KEY_DEFAULT_TAB, DefaultTab.HOME.name)
            currentDefaultTab = DefaultTab.valueOf(savedTabStr)
        } catch (_: Exception) {
            currentDefaultTab = DefaultTab.HOME
        }
    }

    // Authentication token extraction
    private fun handleIntent(intent: Intent?) {
        intent?.data?.let { uri ->
            if (uri.scheme == "https" && uri.host == "localhost" && uri.path == "/oauth2redirect") {
                val fragment = uri.fragment
                val idToken = fragment?.split("&")
                    ?.find { it.startsWith("id_token=") }
                    ?.substringAfter("id_token=")

                if (idToken != null) {
                    // Auth logic
                }
            }
        }
    }
}

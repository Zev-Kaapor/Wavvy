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
// Core window styling utilities
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
// Project UI components and frameworks
import com.lonewolf.wavvy.ui.navigation.MainScreen
import com.lonewolf.wavvy.ui.theme.WavvyTheme

// Infrastructure setup components
class MainActivity : ComponentActivity() {

    // Infrastructure lifecycle management
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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
        controller?.hide(WindowInsetsCompat.Type.systemBars())
        controller?.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        // Display cutout management
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }

        // Deep link intent processing
        handleIntent(intent)

        setContent {
            // Main app theme wrapper
            WavvyTheme {
                // Entry point screen
                MainScreen()
            }
        }
    }

    // Deep link intent processing
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
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
                }
            }
        }
    }
}

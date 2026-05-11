package com.lonewolf.wavvy

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.lonewolf.wavvy.ui.navigation.MainScreen
import com.lonewolf.wavvy.ui.theme.WavvyTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // System UI edge-to-edge support
        enableEdgeToEdge()

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode =
                android.view.WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
        0
        setContent {
            // Main app theme wrapper
            WavvyTheme {
                // Entry point screen
                MainScreen()
            }
        }
    }
}

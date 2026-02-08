package com.lonewolf.wavvy

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.lonewolf.wavvy.ui.home.HomeScreen
import com.lonewolf.wavvy.ui.theme.WavvyTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // System UI edge-to-edge support
        enableEdgeToEdge()

        setContent {
            // Main app theme wrapper
            WavvyTheme {
                // Entry point screen
                HomeScreen()
            }
        }
    }
}

package com.lonewolf.wavvy

// Android foundational frameworks
import android.app.Application
// Project dynamic extraction frameworks
import com.lonewolf.wavvy.ui.player.ExtractorHelper
// Coroutines orchestration and async runtimes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

// Application startup orchestration container
class WavvyApplication : Application() {

    // Global application initialization layer
    override fun onCreate() {
        super.onCreate()
        initializeExtractor()
    }

    // Dynamic stream resolution preloading logic
    private fun initializeExtractor() {
        CoroutineScope(Dispatchers.IO).launch {
            ExtractorHelper.initExtractor(this@WavvyApplication)
        }
    }
}

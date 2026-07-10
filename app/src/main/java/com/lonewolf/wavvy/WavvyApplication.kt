package com.lonewolf.wavvy

// Android foundational frameworks
import android.app.Application
// Project dynamic extraction frameworks
import com.lonewolf.wavvy.ui.player.extractor.ExtractorHelper
// Coroutines orchestration and async runtimes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

// Application startup orchestration container
class WavvyApplication : Application() {

    // Scope tied to the Application lifecycle instead of a detached one
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Global application initialization layer
    override fun onCreate() {
        super.onCreate()
        initializeExtractor()
    }

    // Dynamic stream resolution preloading logic
    private fun initializeExtractor() {
        applicationScope.launch {
            ExtractorHelper.initExtractor()
        }
    }
}

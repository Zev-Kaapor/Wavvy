package com.wavvy.app

// Android foundational frameworks
import android.app.Application

// Dependency injection with Koin
import com.wavvy.app.core.di.appModule
import com.wavvy.app.features.player.data.extractor.ExtractorHelper
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

// Coroutines orchestration and async runtimes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

// Application startup orchestration container
class WavvyApplication : Application() {

    // Scope tied to the Application lifecycle instead of a detached one
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Global application initialization layer
    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidLogger()
            androidContext(this@WavvyApplication)
            modules(appModule)
        }

        // Warm up the stream extractor
        ExtractorHelper.initExtractor()
    }
}

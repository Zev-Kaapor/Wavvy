package com.wavvy.app

// Android application
import android.app.Application
// Project resources
import com.wavvy.app.core.network.WavvyProxy

// The application of the app, it starts before the screens, the player and the background work, so all of them find the proxy ready
class WavvyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        WavvyProxy.install(this)
    }
}

package com.wavvy.app.features.auth.ui.viewmodel

// Android utilities
import android.content.Context
import android.webkit.CookieManager
import android.webkit.WebStorage
import androidx.core.net.toUri

// Authentication manager
class AuthManager(
    private val context: Context
) {

    private companion object {
        const val REDIRECT_URI = "https://music.youtube.com"
        const val AUTH_ENDPOINT = "https://accounts.google.com/ServiceLogin"
    }

    // Build auth URL
    fun buildAuthUrl(mode: String): String {
        val locale = context.resources.configuration.locales[0].toLanguageTag()
        return "https://accounts.google.com/ServiceLogin?service=youtube&passive=true&continue=https%3A%2F%2Fmusic.youtube.com%2F&hl=$locale"
    }

    // Redirect URI
    fun getRedirectUri(): String = REDIRECT_URI

    // Clear session
    fun clearSession() {
        val cookieManager = CookieManager.getInstance()
        cookieManager.removeAllCookies(null)
        cookieManager.removeSessionCookies(null)
        cookieManager.flush()
        WebStorage.getInstance().deleteAllData()
    }
}

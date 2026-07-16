package com.lonewolf.wavvy.data

import android.content.Context
import android.content.SharedPreferences

// Storage driver for local settings persistence
class SettingsStorage(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("wavvy_settings_prefs", Context.MODE_PRIVATE)

    companion object {
        const val KEY_THEME_MODE = "pref_theme_mode"
        const val KEY_DEFAULT_TAB = "pref_default_tab"
        const val KEY_QUEUE_LIMIT = "pref_queue_limit"
        const val KEY_DOWNLOAD_WIFI_ONLY = "pref_download_wifi_only"
        const val KEY_HIGH_QUALITY_AUDIO = "pref_high_quality_audio"

        // Privacy and history control keys
        const val KEY_PAUSE_PLAYBACK_HISTORY = "pref_pause_playback_history"
        const val KEY_PAUSE_SEARCH_HISTORY = "pref_pause_search_history"
        const val KEY_DISABLE_SCREENSHOTS = "pref_disable_screenshots"
    }

    // Save a string preference
    fun saveString(key: String, value: String) {
        prefs.edit().putString(key, value).apply()
    }

    // Retrieve a string preference
    fun getString(key: String, defaultValue: String): String {
        return prefs.getString(key, defaultValue) ?: defaultValue
    }

    // Save a boolean preference
    fun saveBoolean(key: String, value: Boolean) {
        prefs.edit().putBoolean(key, value).apply()
    }

    // Retrieve a boolean preference
    fun getBoolean(key: String, defaultValue: Boolean): Boolean {
        return prefs.getBoolean(key, defaultValue)
    }

    // Save an integer preference
    fun saveInt(key: String, value: Int) {
        prefs.edit().putInt(key, value).apply()
    }

    // Retrieve an integer preference
    fun getInt(key: String, defaultValue: Int): Int {
        return prefs.getInt(key, defaultValue)
    }

    // Export all current shared preferences as a raw JSON or XML representation if needed for backups
    fun getAllSettings(): Map<String, *> {
        return prefs.all
    }

    // Bulk import settings mapping from local backups
    fun importSettings(settingsMap: Map<String, *>) {
        val editor = prefs.edit()
        settingsMap.forEach { (key, value) ->
            when (value) {
                is Boolean -> editor.putBoolean(key, value)
                is String -> editor.putString(key, value)
                is Int -> editor.putInt(key, value)
                is Float -> editor.putFloat(key, value)
                is Long -> editor.putLong(key, value)
            }
        }
        editor.apply()
    }
}

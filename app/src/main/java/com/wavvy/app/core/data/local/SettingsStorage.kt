package com.wavvy.app.core.data.local

// Android and core frameworks
import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
// Project models
import com.wavvy.app.core.designsystem.theme.ThemeMode
import com.wavvy.app.core.navigation.DefaultTab

// Storage driver for local settings persistence
class SettingsStorage(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("wavvy_settings_prefs", Context.MODE_PRIVATE)

    companion object {
        const val KEY_THEME_MODE = "pref_theme_mode"
        const val KEY_DEFAULT_TAB = "pref_default_tab"
        const val KEY_DOWNLOAD_WIFI_ONLY = "pref_download_wifi_only"
        const val KEY_PAUSE_PLAYBACK_HISTORY = "pref_pause_playback_history"
        const val KEY_QUEUE_LIMIT = "pref_queue_limit"
        const val KEY_GUEST_NAME = "pref_guest_name"
        const val KEY_IS_GUEST_ACTIVE = "pref_is_guest_active"
        const val KEY_ACTIVE_GUEST_ID = "pref_active_guest_id"
        const val KEY_GUEST_NAME_IS_CUSTOM = "pref_guest_name_is_custom"
        const val KEY_QUICK_PICKS_SOURCE = "pref_quick_picks_source"
        const val KEY_KWORB_SCOPE = "pref_kworb_scope"
        const val KEY_KWORB_COUNTRY = "pref_kworb_country"
        const val KEY_KWORB_PERIOD = "pref_kworb_period"
    }

    // String operations
    fun saveString(key: String, value: String) {
        prefs.edit { putString(key, value) }
    }

    // String retrieval operations
    fun getString(key: String, defaultValue: String): String {
        return prefs.getString(key, defaultValue) ?: defaultValue
    }

    // Theme mode persistence
    fun saveThemeMode(theme: ThemeMode) {
        saveString(KEY_THEME_MODE, theme.name)
    }

    // Theme mode retrieval
    fun getThemeMode(defaultValue: ThemeMode = ThemeMode.SYSTEM): ThemeMode {
        val storedValue = getString(KEY_THEME_MODE, defaultValue.name)
        return runCatching { ThemeMode.valueOf(storedValue) }.getOrDefault(defaultValue)
    }

    // Default tab persistence
    fun saveDefaultTab(tab: DefaultTab) {
        saveString(KEY_DEFAULT_TAB, tab.name)
    }

    // Default tab retrieval
    fun getDefaultTab(defaultValue: DefaultTab = DefaultTab.HOME): DefaultTab {
        val storedValue = getString(KEY_DEFAULT_TAB, defaultValue.name)
        return runCatching { DefaultTab.valueOf(storedValue) }.getOrDefault(defaultValue)
    }

    // Guest display name persistence
    fun saveGuestName(name: String) {
        saveString(KEY_GUEST_NAME, name)
        saveBoolean(KEY_IS_GUEST_ACTIVE, true)
    }

    // Guest display name retrieval
    fun getGuestName(defaultValue: String = ""): String {
        return getString(KEY_GUEST_NAME, defaultValue)
    }

    // Guest active session state
    fun setGuestActive(active: Boolean) {
        saveBoolean(KEY_IS_GUEST_ACTIVE, active)
    }

    fun isGuestActive(): Boolean {
        return getBoolean(KEY_IS_GUEST_ACTIVE, false)
    }

    // Which saved guest profile is currently active
    fun saveActiveGuestId(id: String?) {
        if (id.isNullOrBlank()) {
            prefs.edit { remove(KEY_ACTIVE_GUEST_ID) }
        } else {
            saveString(KEY_ACTIVE_GUEST_ID, id)
        }
    }

    fun getActiveGuestId(): String? {
        return getString(KEY_ACTIVE_GUEST_ID, "").ifBlank { null }
    }

    // Whether the active guest's name was typed by the user (vs auto-generated)
    fun saveGuestNameIsCustom(isCustom: Boolean) {
        saveBoolean(KEY_GUEST_NAME_IS_CUSTOM, isCustom)
    }

    fun isGuestNameCustom(): Boolean {
        return getBoolean(KEY_GUEST_NAME_IS_CUSTOM, false)
    }

    // Boolean operations
    fun saveBoolean(key: String, value: Boolean) {
        prefs.edit { putBoolean(key, value) }
    }

    // Boolean retrieval operations
    fun getBoolean(key: String, defaultValue: Boolean): Boolean {
        return prefs.getBoolean(key, defaultValue)
    }

    // Int operations
    fun saveInt(key: String, value: Int) {
        prefs.edit { putInt(key, value) }
    }

    // Int retrieval operations
    fun getInt(key: String, defaultValue: Int): Int {
        return prefs.getInt(key, defaultValue)
    }

    // Queue limit persistence
    fun saveQueueLimit(limit: Int) {
        saveInt(KEY_QUEUE_LIMIT, limit)
    }

    // Queue limit retrieval
    fun getQueueLimit(defaultValue: Int = 100): Int {
        return getInt(KEY_QUEUE_LIMIT, defaultValue)
    }

    // Raw preference structure exporter
    fun getAllSettings(): Map<String, *> {
        return prefs.all
    }

    // Data insertion operations
    fun importSettings(settingsMap: Map<String, *>) {
        prefs.edit {
            for ((key, value) in settingsMap) {
                when (value) {
                    is Boolean -> putBoolean(key, value)
                    is String -> putString(key, value)
                }
            }
        }
    }
}

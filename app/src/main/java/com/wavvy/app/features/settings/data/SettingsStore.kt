package com.wavvy.app.features.settings.data

// Android context, resources and DataStore
import android.content.Context
import androidx.annotation.StringRes
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
// Coroutines and reactive flows
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
// Audio
import com.metrolist.innertubex.extraction.AudioQuality
// Project resources
import com.wavvy.app.R
import com.wavvy.app.core.designsystem.theme.ThemeMode

// How good the audio of a download is, the best there is, the one the network allows or the lightest
enum class DownloadQuality(val audio: AudioQuality, @StringRes val titleRes: Int) {
    Highest(AudioQuality.HIGH, R.string.settings_downloads_quality_highest),
    Standard(AudioQuality.AUTO, R.string.settings_downloads_quality_standard),
    Low(AudioQuality.LOW, R.string.settings_downloads_quality_low)
}

// How much space the downloads can take before no more are started, none means no limit
enum class DownloadLimit(val gigabytes: Int?, @StringRes val titleRes: Int) {
    Unlimited(null, R.string.settings_downloads_limit_none),
    One(1, R.string.settings_downloads_limit_1),
    Two(2, R.string.settings_downloads_limit_2),
    Five(5, R.string.settings_downloads_limit_5),
    Ten(10, R.string.settings_downloads_limit_10),
    Twenty(20, R.string.settings_downloads_limit_20);

    val bytes: Long? get() = gigabytes?.let { it * BytesPerGigabyte }

    private companion object {
        const val BytesPerGigabyte = 1024L * 1024L * 1024L
    }
}

// What the settings of the downloads are
data class DownloadSettings(
    val wifiOnly: Boolean = false,
    val quality: DownloadQuality = DownloadQuality.Highest,
    val limit: DownloadLimit = DownloadLimit.Unlimited
)

// What the settings of the content are
data class ContentSettings(
    // True when videos show in the Home and in the queue, which keep to music otherwise
    val showVideos: Boolean = false
)

// How often the artists followed are looked at for new releases
enum class ReleaseInterval(val hours: Long, @StringRes val titleRes: Int) {
    SixHours(6, R.string.settings_notifications_interval_6),
    TwelveHours(12, R.string.settings_notifications_interval_12),
    OneDay(24, R.string.settings_notifications_interval_24),
    TwoDays(48, R.string.settings_notifications_interval_48)
}

// What the settings of the notifications are
data class NotificationSettings(
    val enabled: Boolean = true,
    val interval: ReleaseInterval = ReleaseInterval.TwelveHours
)

// How often the app looks for a newer version
enum class UpdateInterval(val hours: Long, @StringRes val titleRes: Int) {
    SixHours(6, R.string.updates_interval_6),
    TwelveHours(12, R.string.updates_interval_12),
    OneDay(24, R.string.updates_interval_24),
    OneWeek(168, R.string.updates_interval_168)
}

// What the settings of the updates are
data class UpdateSettings(
    // True while the app does not look for newer versions by itself
    val paused: Boolean = false,
    val interval: UpdateInterval = UpdateInterval.OneDay
)

// What the settings of the privacy are
data class PrivacySettings(
    // True while what is listened to is not kept in the history
    val pauseHistory: Boolean = false,
    // True when what is listened to also goes to the history of the account
    val syncToAccount: Boolean = true
)

// DataStore file and keys of the settings
private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")
private val DownloadWifiOnlyKey = booleanPreferencesKey("download_wifi_only")
private val DownloadQualityKey = stringPreferencesKey("download_quality")
private val DownloadLimitKey = stringPreferencesKey("download_limit")
private val ShowVideosKey = booleanPreferencesKey("show_videos")
private val PauseHistoryKey = booleanPreferencesKey("pause_history")
private val UpdatesPausedKey = booleanPreferencesKey("updates_paused")
private val UpdateIntervalKey = stringPreferencesKey("update_interval")
private val LastUpdateCheckKey = longPreferencesKey("last_update_check")
private val DismissedUpdateKey = stringPreferencesKey("dismissed_update")
private val ReleasesEnabledKey = booleanPreferencesKey("releases_enabled")
private val ReleaseIntervalKey = stringPreferencesKey("release_interval")
private val ThemeModeKey = stringPreferencesKey("theme_mode")
private val SyncToAccountKey = booleanPreferencesKey("sync_to_account")

// Keeps what the user chose in the settings, on the device
class SettingsStore(context: Context) {
    private val dataStore = context.applicationContext.settingsDataStore

    val downloads: Flow<DownloadSettings> = dataStore.data.map { preferences ->
        DownloadSettings(
            wifiOnly = preferences[DownloadWifiOnlyKey] ?: false,
            quality = DownloadQuality.entries.firstOrNull { it.name == preferences[DownloadQualityKey] } ?: DownloadQuality.Highest,
            limit = DownloadLimit.entries.firstOrNull { it.name == preferences[DownloadLimitKey] } ?: DownloadLimit.Unlimited
        )
    }

    val content: Flow<ContentSettings> = dataStore.data.map { preferences ->
        ContentSettings(showVideos = preferences[ShowVideosKey] ?: false)
    }

    val privacy: Flow<PrivacySettings> = dataStore.data.map { preferences ->
        PrivacySettings(
            pauseHistory = preferences[PauseHistoryKey] ?: false,
            syncToAccount = preferences[SyncToAccountKey] ?: true
        )
    }

    val notifications: Flow<NotificationSettings> = dataStore.data.map { preferences ->
        NotificationSettings(
            enabled = preferences[ReleasesEnabledKey] ?: true,
            interval = ReleaseInterval.entries.firstOrNull { it.name == preferences[ReleaseIntervalKey] } ?: ReleaseInterval.TwelveHours
        )
    }

    val themeMode: Flow<ThemeMode> = dataStore.data.map { preferences ->
        ThemeMode.entries.firstOrNull { it.name == preferences[ThemeModeKey] } ?: ThemeMode.SYSTEM
    }

    suspend fun setReleasesEnabled(value: Boolean) {
        dataStore.edit { it[ReleasesEnabledKey] = value }
    }

    suspend fun setReleaseInterval(value: ReleaseInterval) {
        dataStore.edit { it[ReleaseIntervalKey] = value.name }
    }

    suspend fun setThemeMode(value: ThemeMode) {
        dataStore.edit { it[ThemeModeKey] = value.name }
    }

    val updates: Flow<UpdateSettings> = dataStore.data.map { preferences ->
        UpdateSettings(
            paused = preferences[UpdatesPausedKey] ?: false,
            interval = UpdateInterval.entries.firstOrNull { it.name == preferences[UpdateIntervalKey] } ?: UpdateInterval.OneDay
        )
    }

    // When the last check was made, and the version the user said later to, so it is not asked again
    val lastUpdateCheck: Flow<Long> = dataStore.data.map { it[LastUpdateCheckKey] ?: 0L }
    val dismissedUpdate: Flow<String?> = dataStore.data.map { it[DismissedUpdateKey] }

    suspend fun setUpdatesPaused(value: Boolean) {
        dataStore.edit { it[UpdatesPausedKey] = value }
    }

    suspend fun setUpdateInterval(value: UpdateInterval) {
        dataStore.edit { it[UpdateIntervalKey] = value.name }
    }

    suspend fun setLastUpdateCheck(value: Long) {
        dataStore.edit { it[LastUpdateCheckKey] = value }
    }

    suspend fun setDismissedUpdate(value: String) {
        dataStore.edit { it[DismissedUpdateKey] = value }
    }

    suspend fun setPauseHistory(value: Boolean) {
        dataStore.edit { it[PauseHistoryKey] = value }
    }

    suspend fun setSyncToAccount(value: Boolean) {
        dataStore.edit { it[SyncToAccountKey] = value }
    }

    suspend fun setShowVideos(value: Boolean) {
        dataStore.edit { it[ShowVideosKey] = value }
    }

    suspend fun setDownloadWifiOnly(value: Boolean) {
        dataStore.edit { it[DownloadWifiOnlyKey] = value }
    }

    suspend fun setDownloadQuality(value: DownloadQuality) {
        dataStore.edit { it[DownloadQualityKey] = value.name }
    }

    suspend fun setDownloadLimit(value: DownloadLimit) {
        dataStore.edit { it[DownloadLimitKey] = value.name }
    }
}

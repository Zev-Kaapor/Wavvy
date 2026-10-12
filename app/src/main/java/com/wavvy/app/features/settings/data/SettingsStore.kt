package com.wavvy.app.features.settings.data

// Android context, resources and DataStore
import java.net.Proxy
import android.content.Context
import androidx.annotation.StringRes
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
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
    val showVideos: Boolean = false,
    // True when songs and albums marked explicit stay out of the Home and the search
    val hideExplicit: Boolean = false,
    // True when the playlist of Shorts stays out of the library
    val hideShorts: Boolean = false,
    // The two letters of the country the content comes from, none follows the device
    val country: String? = null
)

// The lengths of the crossfade that can be chosen, in milliseconds
val CrossfadeDurations = listOf(250, 500, 750, 1000, 2000, 3000, 4000, 5000, 6000, 7000, 8000, 9000, 10000, 11000, 12000)

// What the settings of the crossfade are, how long it lasts and whether it works when the user changes the song and when a song ends
data class CrossfadeSettings(
    val enabled: Boolean = false,
    val durationMs: Int = 5000,
    val onSkip: Boolean = true,
    val onAutoAdvance: Boolean = true
)

// The kinds of proxy, with the name the proxies are known by and what the system calls it
enum class ProxyKind(val label: String, val type: Proxy.Type) {
    Http("HTTP", Proxy.Type.HTTP),
    Socks("SOCKS", Proxy.Type.SOCKS)
}

// What the settings of the proxy are, the address is the host and the port as the user typed them
data class ProxySettings(
    val enabled: Boolean = false,
    val kind: ProxyKind = ProxyKind.Http,
    val address: String = "",
    val username: String = "",
    val password: String = ""
)

// What the settings of the looks of the library are, the lists YouTube Music makes by itself that the user does not want to see
data class LibrarySettings(
    val hideLiked: Boolean = false,
    val hideEpisodes: Boolean = false,
    val hideWatchLater: Boolean = false
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
    val syncToAccount: Boolean = true,
    // True while the searches are not kept on the device nor in the account
    val pauseSearchHistory: Boolean = false,
    // True when the system is asked not to allow screenshots and recordings of the app
    val blockScreenshots: Boolean = false
)

// DataStore file and keys of the settings
private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")
private val DownloadWifiOnlyKey = booleanPreferencesKey("download_wifi_only")
private val DownloadQualityKey = stringPreferencesKey("download_quality")
private val DownloadLimitKey = stringPreferencesKey("download_limit")
private val ShowVideosKey = booleanPreferencesKey("show_videos")
private val HideExplicitKey = booleanPreferencesKey("hide_explicit")
private val HideShortsKey = booleanPreferencesKey("hide_shorts")
private val CountryKey = stringPreferencesKey("content_country")
private val HideLikedKey = booleanPreferencesKey("hide_liked")
private val HideEpisodesKey = booleanPreferencesKey("hide_episodes")
private val HideWatchLaterKey = booleanPreferencesKey("hide_watch_later")
private val PauseSearchHistoryKey = booleanPreferencesKey("pause_search_history")
private val BlockScreenshotsKey = booleanPreferencesKey("block_screenshots")
private val CrossfadeEnabledKey = booleanPreferencesKey("crossfade_enabled")
private val CrossfadeDurationKey = intPreferencesKey("crossfade_duration")
private val CrossfadeOnSkipKey = booleanPreferencesKey("crossfade_on_skip")
private val CrossfadeOnAutoKey = booleanPreferencesKey("crossfade_on_auto")
private val ProxyEnabledKey = booleanPreferencesKey("proxy_enabled")
private val ProxyKindKey = stringPreferencesKey("proxy_kind")
private val ProxyAddressKey = stringPreferencesKey("proxy_address")
private val ProxyUsernameKey = stringPreferencesKey("proxy_username")
private val ProxyPasswordKey = stringPreferencesKey("proxy_password")
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
        ContentSettings(
            showVideos = preferences[ShowVideosKey] ?: false,
            hideExplicit = preferences[HideExplicitKey] ?: false,
            hideShorts = preferences[HideShortsKey] ?: false,
            country = preferences[CountryKey]
        )
    }

    val crossfade: Flow<CrossfadeSettings> = dataStore.data.map { preferences ->
        CrossfadeSettings(
            enabled = preferences[CrossfadeEnabledKey] ?: false,
            durationMs = preferences[CrossfadeDurationKey]?.takeIf { it in CrossfadeDurations } ?: CrossfadeSettings().durationMs,
            onSkip = preferences[CrossfadeOnSkipKey] ?: true,
            onAutoAdvance = preferences[CrossfadeOnAutoKey] ?: true
        )
    }

    val proxy: Flow<ProxySettings> = dataStore.data.map { preferences ->
        ProxySettings(
            enabled = preferences[ProxyEnabledKey] ?: false,
            kind = ProxyKind.entries.firstOrNull { it.name == preferences[ProxyKindKey] } ?: ProxyKind.Http,
            address = preferences[ProxyAddressKey].orEmpty(),
            username = preferences[ProxyUsernameKey].orEmpty(),
            password = preferences[ProxyPasswordKey].orEmpty()
        )
    }

    val library: Flow<LibrarySettings> = dataStore.data.map { preferences ->
        LibrarySettings(
            hideLiked = preferences[HideLikedKey] ?: false,
            hideEpisodes = preferences[HideEpisodesKey] ?: false,
            hideWatchLater = preferences[HideWatchLaterKey] ?: false
        )
    }

    val privacy: Flow<PrivacySettings> = dataStore.data.map { preferences ->
        PrivacySettings(
            pauseHistory = preferences[PauseHistoryKey] ?: false,
            syncToAccount = preferences[SyncToAccountKey] ?: true,
            pauseSearchHistory = preferences[PauseSearchHistoryKey] ?: false,
            blockScreenshots = preferences[BlockScreenshotsKey] ?: false
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

    suspend fun setHideExplicit(value: Boolean) {
        dataStore.edit { it[HideExplicitKey] = value }
    }

    suspend fun setHideShorts(value: Boolean) {
        dataStore.edit { it[HideShortsKey] = value }
    }

    // A null country goes back to the one of the device
    suspend fun setCountry(value: String?) {
        dataStore.edit { if (value == null) it.remove(CountryKey) else it[CountryKey] = value }
    }

    suspend fun setCrossfadeEnabled(value: Boolean) {
        dataStore.edit { it[CrossfadeEnabledKey] = value }
    }

    suspend fun setCrossfadeDuration(value: Int) {
        dataStore.edit { it[CrossfadeDurationKey] = value }
    }

    suspend fun setCrossfadeOnSkip(value: Boolean) {
        dataStore.edit { it[CrossfadeOnSkipKey] = value }
    }

    suspend fun setCrossfadeOnAutoAdvance(value: Boolean) {
        dataStore.edit { it[CrossfadeOnAutoKey] = value }
    }

    suspend fun setProxy(value: ProxySettings) {
        dataStore.edit {
            it[ProxyEnabledKey] = value.enabled
            it[ProxyKindKey] = value.kind.name
            it[ProxyAddressKey] = value.address
            it[ProxyUsernameKey] = value.username
            it[ProxyPasswordKey] = value.password
        }
    }

    suspend fun setProxyEnabled(value: Boolean) {
        dataStore.edit { it[ProxyEnabledKey] = value }
    }

    suspend fun setHideLiked(value: Boolean) {
        dataStore.edit { it[HideLikedKey] = value }
    }

    suspend fun setHideEpisodes(value: Boolean) {
        dataStore.edit { it[HideEpisodesKey] = value }
    }

    suspend fun setHideWatchLater(value: Boolean) {
        dataStore.edit { it[HideWatchLaterKey] = value }
    }

    suspend fun setPauseHistory(value: Boolean) {
        dataStore.edit { it[PauseHistoryKey] = value }
    }

    suspend fun setPauseSearchHistory(value: Boolean) {
        dataStore.edit { it[PauseSearchHistoryKey] = value }
    }

    suspend fun setBlockScreenshots(value: Boolean) {
        dataStore.edit { it[BlockScreenshotsKey] = value }
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

package com.wavvy.app.features.settings.data

// Android context, resources and DataStore
import java.net.Proxy
import org.json.JSONObject
import com.wavvy.app.core.backup.PrefsSnapshot
import com.wavvy.app.core.playback.SponsorCategory
import android.content.Context
import androidx.annotation.StringRes
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
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

// How good the audio is, for what is played and for what is downloaded, the best format there is, a lighter one that plays anywhere or the lightest
// YouTube Music offers Opus of about 150 kbps, AAC of about 130 kbps and AAC of about 50 kbps
enum class AudioLevel(val audio: AudioQuality, @StringRes val titleRes: Int, @StringRes val detailRes: Int) {
    Highest(AudioQuality.HIGH, R.string.settings_audio_high, R.string.settings_audio_high_detail),
    Standard(AudioQuality.MP4, R.string.settings_audio_medium, R.string.settings_audio_medium_detail),
    Low(AudioQuality.LOW, R.string.settings_audio_low, R.string.settings_audio_low_detail),

    // Only for what is played, the best on Wi-Fi and the lightest on mobile data
    Automatic(AudioQuality.AUTO, R.string.settings_audio_auto, R.string.settings_audio_auto_detail)
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
    val quality: AudioLevel = AudioLevel.Highest,
    val limit: DownloadLimit = DownloadLimit.Unlimited
)

// What the settings of the content are
data class ContentSettings(
    // True when videos show in the Home, which keeps to music otherwise
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

// How loud the songs are brought to, the louder the level the more the quiet songs are raised, adapted from Metrolist (GPL-3.0)
enum class LoudnessLevel(val targetLufs: Float, @StringRes val titleRes: Int) {
    Aggressive(-7f, R.string.settings_loudness_aggressive),
    Loud(-11f, R.string.settings_loudness_loud),
    Balanced(-14f, R.string.settings_loudness_balanced),
    Quiet(-19f, R.string.settings_loudness_quiet)
}

// What the settings of the playback are besides the crossfade
data class PlaybackSettings(
    // True when the silent parts of the songs are skipped
    val skipSilence: Boolean = false,
    // True when every song is brought to the same loudness
    val normalizeAudio: Boolean = false,
    // How loud the songs are brought to when they are normalized
    val loudness: LoudnessLevel = LoudnessLevel.Balanced,
    // How good the audio of what is played is
    val quality: AudioLevel = AudioLevel.Highest,
    // True when the queue, the song and the place in it come back when the app opens again
    val persistentQueue: Boolean = true,
    // True when removing the app from the recent apps stops the music even while it plays
    val stopOnTaskRemoved: Boolean = false,
    // True when the music pauses as the volume goes down to nothing
    val pauseWhenMuted: Boolean = true,
    // True when the music goes on as a Bluetooth device connects
    val resumeOnBluetooth: Boolean = false,
    // True when the screen stays on while the full player is open
    val keepScreenOn: Boolean = false
)

// The days a schedule of the sleep timer can run on, the last one is the days the user picked
enum class SleepRepeat(@StringRes val titleRes: Int, val days: Set<Int>?) {
    Daily(R.string.settings_sleep_repeat_daily, (1..7).toSet()),
    Weekdays(R.string.settings_sleep_repeat_weekdays, (1..5).toSet()),
    Weekends(R.string.settings_sleep_repeat_weekends, setOf(6, 7)),
    Custom(R.string.settings_sleep_repeat_custom, null)
}

// The lengths of the sleep timer that start by themselves, in minutes
val SleepAutoMinutes = listOf(15, 30, 45, 60, 90, 120)

// What the settings of the sleep timer are, how it ends and when it starts by itself, the days are counted as 1 for Monday to 7 for Sunday
data class SleepSettings(
    val fadeOut: Boolean = true,
    val finishSong: Boolean = false,
    val auto: Boolean = false,
    val minutes: Int = 30,
    val repeat: SleepRepeat = SleepRepeat.Daily,
    val customDays: Set<Int> = (1..7).toSet(),
    val startMinute: Int = 22 * 60,
    val endMinute: Int = 6 * 60
) {
    // The days the schedule runs on
    val days: Set<Int> get() = repeat.days ?: customDays
}

// What the settings of SponsorBlock are, and the kinds of part that are skipped
data class SponsorBlockSettings(
    val enabled: Boolean = false,
    val categories: Set<SponsorCategory> = SponsorCategory.entries.filter { it.isDefault }.toSet()
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
    TwoHours(2, R.string.settings_notifications_interval_2),
    SixHours(6, R.string.settings_notifications_interval_6),
    TwelveHours(12, R.string.settings_notifications_interval_12),
    OneDay(24, R.string.settings_notifications_interval_24),
    TwoDays(48, R.string.settings_notifications_interval_48)
}

// What the settings of the notifications are
data class NotificationSettings(
    val enabled: Boolean = true,
    val interval: ReleaseInterval = ReleaseInterval.SixHours
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
private val SkipSilenceKey = booleanPreferencesKey("playback_skip_silence")
private val NormalizeAudioKey = booleanPreferencesKey("playback_normalize_audio")
private val LoudnessLevelKey = stringPreferencesKey("playback_loudness_level")
private val PlaybackQualityKey = stringPreferencesKey("playback_quality")
private val PersistentQueueKey = booleanPreferencesKey("playback_persistent_queue")
private val StopOnTaskRemovedKey = booleanPreferencesKey("playback_stop_on_task_removed")
private val PauseWhenMutedKey = booleanPreferencesKey("playback_pause_when_muted")
private val ResumeOnBluetoothKey = booleanPreferencesKey("playback_resume_on_bluetooth")
private val KeepScreenOnKey = booleanPreferencesKey("playback_keep_screen_on")
private val SleepFadeOutKey = booleanPreferencesKey("sleep_fade_out")
private val SleepFinishSongKey = booleanPreferencesKey("sleep_finish_song")
private val SleepAutoKey = booleanPreferencesKey("sleep_auto")
private val SleepMinutesKey = intPreferencesKey("sleep_minutes")
private val SleepRepeatKey = stringPreferencesKey("sleep_repeat")
private val SleepDaysKey = stringSetPreferencesKey("sleep_days")
private val SleepStartKey = intPreferencesKey("sleep_start_minute")
private val SleepEndKey = intPreferencesKey("sleep_end_minute")
private val DefaultTabKey = stringPreferencesKey("default_tab")
private val MirrorSaveKey = booleanPreferencesKey("playlist_mirror_save")
private val CrossfadeOnSkipKey = booleanPreferencesKey("crossfade_on_skip")
private val CrossfadeOnAutoKey = booleanPreferencesKey("crossfade_on_auto")
private val SponsorEnabledKey = booleanPreferencesKey("sponsorblock_enabled")
private val SponsorCategoriesKey = stringSetPreferencesKey("sponsorblock_categories")
private val ProfileShowActivityKey = booleanPreferencesKey("profile_show_activity")
private val ProfileShowSongsKey = booleanPreferencesKey("profile_show_songs")
private val ProfileShowArtistsKey = booleanPreferencesKey("profile_show_artists")
private val ProfileShowPlaylistsKey = booleanPreferencesKey("profile_show_playlists")
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

// The keys that stay out of the backup, when the last look for updates was made and which version was put off
private val BackupSkipped = setOf("last_update_check", "dismissed_update")

// Keeps what the user chose in the settings, on the device
class SettingsStore(context: Context) {
    private val dataStore = context.applicationContext.settingsDataStore

    // The choices for the backup, without what only belongs to this moment of the app
    suspend fun backup(): JSONObject = PrefsSnapshot.read(dataStore, BackupSkipped)

    suspend fun restore(json: JSONObject) = PrefsSnapshot.write(dataStore, json, BackupSkipped)

    val downloads: Flow<DownloadSettings> = dataStore.data.map { preferences ->
        DownloadSettings(
            wifiOnly = preferences[DownloadWifiOnlyKey] ?: false,
            quality = AudioLevel.entries.firstOrNull { it.name == preferences[DownloadQualityKey] } ?: AudioLevel.Highest,
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

    val playback: Flow<PlaybackSettings> = dataStore.data.map { preferences ->
        val defaults = PlaybackSettings()
        PlaybackSettings(
            skipSilence = preferences[SkipSilenceKey] ?: defaults.skipSilence,
            normalizeAudio = preferences[NormalizeAudioKey] ?: defaults.normalizeAudio,
            loudness = LoudnessLevel.entries.firstOrNull { it.name == preferences[LoudnessLevelKey] } ?: defaults.loudness,
            quality = AudioLevel.entries.firstOrNull { it.name == preferences[PlaybackQualityKey] } ?: defaults.quality,
            persistentQueue = preferences[PersistentQueueKey] ?: defaults.persistentQueue,
            stopOnTaskRemoved = preferences[StopOnTaskRemovedKey] ?: defaults.stopOnTaskRemoved,
            pauseWhenMuted = preferences[PauseWhenMutedKey] ?: defaults.pauseWhenMuted,
            resumeOnBluetooth = preferences[ResumeOnBluetoothKey] ?: defaults.resumeOnBluetooth,
            keepScreenOn = preferences[KeepScreenOnKey] ?: defaults.keepScreenOn
        )
    }

    val sleep: Flow<SleepSettings> = dataStore.data.map { preferences ->
        val defaults = SleepSettings()
        SleepSettings(
            fadeOut = preferences[SleepFadeOutKey] ?: defaults.fadeOut,
            finishSong = preferences[SleepFinishSongKey] ?: defaults.finishSong,
            auto = preferences[SleepAutoKey] ?: defaults.auto,
            minutes = preferences[SleepMinutesKey]?.takeIf { it in SleepAutoMinutes } ?: defaults.minutes,
            repeat = SleepRepeat.entries.firstOrNull { it.name == preferences[SleepRepeatKey] } ?: defaults.repeat,
            customDays = preferences[SleepDaysKey]?.mapNotNull { it.toIntOrNull() }?.toSet()?.takeIf { it.isNotEmpty() } ?: defaults.customDays,
            startMinute = preferences[SleepStartKey] ?: defaults.startMinute,
            endMinute = preferences[SleepEndKey] ?: defaults.endMinute
        )
    }

    // True when several songs saved to a playlist go in the reverse order, so a playlist that shows the newest first ends up like the queue
    val mirrorSave: Flow<Boolean> = dataStore.data.map { it[MirrorSaveKey] ?: false }

    // The name of the tab the app opens on, as the tab is named, read once when the app starts
    val defaultTab: Flow<String?> = dataStore.data.map { it[DefaultTabKey] }

    val sponsorBlock: Flow<SponsorBlockSettings> = dataStore.data.map { preferences ->
        SponsorBlockSettings(
            enabled = preferences[SponsorEnabledKey] ?: false,
            categories = preferences[SponsorCategoriesKey]?.let { names -> SponsorCategory.entries.filter { it.name in names }.toSet() }
                ?: SponsorBlockSettings().categories
        )
    }

    // Whether the profile shows each of its parts, or keeps them private
    val profileShowActivity: Flow<Boolean> = dataStore.data.map { it[ProfileShowActivityKey] ?: true }
    val profileShowSongs: Flow<Boolean> = dataStore.data.map { it[ProfileShowSongsKey] ?: true }
    val profileShowArtists: Flow<Boolean> = dataStore.data.map { it[ProfileShowArtistsKey] ?: true }
    val profileShowPlaylists: Flow<Boolean> = dataStore.data.map { it[ProfileShowPlaylistsKey] ?: true }

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
            interval = ReleaseInterval.entries.firstOrNull { it.name == preferences[ReleaseIntervalKey] } ?: ReleaseInterval.SixHours
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

    suspend fun setPlayback(value: PlaybackSettings) {
        dataStore.edit {
            it[SkipSilenceKey] = value.skipSilence
            it[NormalizeAudioKey] = value.normalizeAudio
            it[LoudnessLevelKey] = value.loudness.name
            it[PlaybackQualityKey] = value.quality.name
            it[PersistentQueueKey] = value.persistentQueue
            it[StopOnTaskRemovedKey] = value.stopOnTaskRemoved
            it[PauseWhenMutedKey] = value.pauseWhenMuted
            it[ResumeOnBluetoothKey] = value.resumeOnBluetooth
            it[KeepScreenOnKey] = value.keepScreenOn
        }
    }

    suspend fun setSleep(value: SleepSettings) {
        dataStore.edit {
            it[SleepFadeOutKey] = value.fadeOut
            it[SleepFinishSongKey] = value.finishSong
            it[SleepAutoKey] = value.auto
            it[SleepMinutesKey] = value.minutes
            it[SleepRepeatKey] = value.repeat.name
            it[SleepDaysKey] = value.customDays.map { day -> day.toString() }.toSet()
            it[SleepStartKey] = value.startMinute
            it[SleepEndKey] = value.endMinute
        }
    }

    suspend fun setMirrorSave(value: Boolean) {
        dataStore.edit { it[MirrorSaveKey] = value }
    }

    suspend fun setDefaultTab(name: String) {
        dataStore.edit { it[DefaultTabKey] = name }
    }

    suspend fun setSponsorBlockEnabled(value: Boolean) {
        dataStore.edit { it[SponsorEnabledKey] = value }
    }

    // Turns one kind of part on or off, the ones the user never touched start from the kinds that are on by default
    suspend fun setSponsorCategory(category: SponsorCategory, isOn: Boolean) {
        dataStore.edit { preferences ->
            val names = preferences[SponsorCategoriesKey] ?: SponsorCategory.entries.filter { it.isDefault }.map { it.name }.toSet()
            preferences[SponsorCategoriesKey] = if (isOn) names + category.name else names - category.name
        }
    }

    suspend fun setProfileShowActivity(value: Boolean) {
        dataStore.edit { it[ProfileShowActivityKey] = value }
    }

    suspend fun setProfileShowSongs(value: Boolean) {
        dataStore.edit { it[ProfileShowSongsKey] = value }
    }

    suspend fun setProfileShowArtists(value: Boolean) {
        dataStore.edit { it[ProfileShowArtistsKey] = value }
    }

    suspend fun setProfileShowPlaylists(value: Boolean) {
        dataStore.edit { it[ProfileShowPlaylistsKey] = value }
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

    suspend fun setDownloadQuality(value: AudioLevel) {
        dataStore.edit { it[DownloadQualityKey] = value.name }
    }

    suspend fun setDownloadLimit(value: DownloadLimit) {
        dataStore.edit { it[DownloadLimitKey] = value.name }
    }
}

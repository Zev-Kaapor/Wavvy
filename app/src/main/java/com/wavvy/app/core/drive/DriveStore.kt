package com.wavvy.app.core.drive

// Android context and DataStore
import android.content.Context
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

// DataStore file and key, apart from the settings so a backup never brings the connection of another phone
private val Context.driveDataStore: DataStore<Preferences> by preferencesDataStore(name = "drive")
private val ConnectedKey = booleanPreferencesKey("connected")
private val SyncedKey = longPreferencesKey("synced_at")
private val AutoKey = booleanPreferencesKey("auto_save")
private val LockedKey = booleanPreferencesKey("locked_backup")
private val HashKey = stringPreferencesKey("last_hash")
private val HoursKey = longPreferencesKey("auto_hours")
private val OfferedKey = booleanPreferencesKey("offered")

// Keeps whether the user connected the Drive on this phone and when it last met the backup
class DriveStore(context: Context) {
    private val dataStore = context.applicationContext.driveDataStore

    val isConnected: Flow<Boolean> = dataStore.data.map { it[ConnectedKey] ?: false }

    suspend fun setConnected(value: Boolean) {
        dataStore.edit { it[ConnectedKey] = value }
    }

    // When this phone last saved to the Drive or restored from it, zero if it never did
    val syncedAt: Flow<Long> = dataStore.data.map { it[SyncedKey] ?: 0L }

    suspend fun setSyncedAt(value: Long) {
        dataStore.edit { it[SyncedKey] = value }
    }

    // Whether the app saves to the Drive by itself
    val autoSave: Flow<Boolean> = dataStore.data.map { it[AutoKey] ?: false }

    suspend fun setAutoSave(value: Boolean) {
        dataStore.edit { it[AutoKey] = value }
    }

    // Whether the backup that is in the Drive has a password, which the automatic save never replaces by one without it
    val isLocked: Flow<Boolean> = dataStore.data.map { it[LockedKey] ?: false }

    suspend fun setLocked(value: Boolean) {
        dataStore.edit { it[LockedKey] = value }
    }

    // How many hours between the saves, six unless the user chose another
    val autoHours: Flow<Long> = dataStore.data.map { it[HoursKey] ?: DriveInterval.SixHours.hours }

    suspend fun setAutoHours(value: Long) {
        dataStore.edit { it[HoursKey] = value }
    }

    // Whether the user was already asked, once, to connect the Drive after signing in
    val wasOffered: Flow<Boolean> = dataStore.data.map { it[OfferedKey] ?: false }

    suspend fun setOffered(value: Boolean) {
        dataStore.edit { it[OfferedKey] = value }
    }

    // The fingerprint of what was saved last, so nothing is sent again while nothing changed
    val lastHash: Flow<String?> = dataStore.data.map { it[HashKey] }

    suspend fun setLastHash(value: String?) {
        dataStore.edit { if (value == null) it.remove(HashKey) else it[HashKey] = value }
    }
}

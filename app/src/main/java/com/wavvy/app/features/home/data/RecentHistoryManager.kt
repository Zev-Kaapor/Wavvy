package com.wavvy.app.features.home.data

// Android DataStore preferences
import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
// JSON serialization
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
// Project imports
import com.wavvy.app.core.data.local.SettingsStorage
import com.wavvy.app.features.home.ui.components.RecentTrack
// Coroutines reactive flows
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map

// DataStore delegation
private val Context.recentStore by preferencesDataStore(name = "recent_history_pref")

class RecentHistoryManager(private val context: Context) {

    private companion object {
        const val MAX_ITEMS = 50
        const val ANONYMOUS_OWNER = "anonymous"
    }

    private val gson = Gson()
    private val activeOwnerId = MutableStateFlow(ANONYMOUS_OWNER)

    // Switch whose recent tracks are being read/written (account handle or guest id)
    fun setActiveOwner(ownerId: String?) {
        activeOwnerId.value = ownerId?.takeIf { it.isNotBlank() } ?: ANONYMOUS_OWNER
    }

    private fun keyFor(owner: String) = stringPreferencesKey("recent_tracks_key_$owner")

    // Recent tracks flow for whichever owner is currently active
    @OptIn(ExperimentalCoroutinesApi::class)
    val recentTracks: Flow<List<RecentTrack>> = activeOwnerId.flatMapLatest { owner ->
        context.recentStore.data.map { preferences ->
            val json = preferences[keyFor(owner)] ?: return@map emptyList()
            val type = object : TypeToken<List<RecentTrack>>() {}.type
            try {
                gson.fromJson<List<RecentTrack>>(json, type)
                    ?.filter { it.id.isNotBlank() }
                    ?: emptyList()
            } catch (_: Exception) {
                emptyList()
            }
        }
    }

    // Save or update track for the current owner
    suspend fun saveTrack(track: RecentTrack) {
        val storage = SettingsStorage(context)
        val isPaused = storage.getBoolean(SettingsStorage.KEY_PAUSE_PLAYBACK_HISTORY, false)
        if (isPaused) return

        val key = keyFor(activeOwnerId.value)
        context.recentStore.edit { preferences ->
            val json = preferences[key]
            val type = object : TypeToken<List<RecentTrack>>() {}.type

            val currentList: List<RecentTrack> = if (!json.isNullOrBlank()) {
                try {
                    gson.fromJson(json, type) ?: emptyList()
                } catch (_: Exception) {
                    emptyList()
                }
            } else {
                emptyList()
            }

            val updated = currentList.toMutableList().apply {
                removeAll { it.id == track.id }
                add(0, track)
                if (size > MAX_ITEMS) removeAt(size - 1)
            }

            preferences[key] = gson.toJson(updated)
        }
    }

    // Remove specific item for the current owner
    suspend fun removeItem(trackId: String) {
        val key = keyFor(activeOwnerId.value)
        context.recentStore.edit { preferences ->
            val json = preferences[key] ?: return@edit
            val type = object : TypeToken<List<RecentTrack>>() {}.type
            try {
                val currentList: List<RecentTrack> = gson.fromJson(json, type) ?: return@edit
                val updated = currentList.filter { it.id != trackId }
                preferences[key] = gson.toJson(updated)
            } catch (_: Exception) {
                // Keep current state on failure
            }
        }
    }

    // Clear all history for the current owner
    suspend fun clearAll() {
        val key = keyFor(activeOwnerId.value)
        context.recentStore.edit { preferences ->
            preferences.remove(key)
        }
    }
}

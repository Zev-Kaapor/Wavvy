package com.lonewolf.wavvy.data

// Datastore and preferences
import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
// Serialization
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.lonewolf.wavvy.ui.home.components.RecentTrack
// Coroutines and flows
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Datastore delegation
private val Context.recentStore by preferencesDataStore(name = "recent_history_pref")

class RecentHistoryManager(private val context: Context) {

    // Internal constants
    companion object {
        private val RECENT_KEY = stringPreferencesKey("recent_tracks_key")
        private const val MAX_ITEMS = 50
    }

    private val gson = Gson()

    // Recent tracks flow
    val recentTracks: Flow<List<RecentTrack>> = context.recentStore.data.map { preferences ->
        val json = preferences[RECENT_KEY] ?: return@map emptyList()
        val type = object : TypeToken<List<RecentTrack>>() {}.type
        try {
            gson.fromJson<List<RecentTrack>>(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    // Save or update recent track
    suspend fun saveTrack(track: RecentTrack) {
        context.recentStore.edit { preferences ->
            val json = preferences[RECENT_KEY]
            val type = object : TypeToken<List<RecentTrack>>() {}.type

            val currentList: List<RecentTrack> = if (!json.isNullOrBlank()) {
                try {
                    gson.fromJson(json, type) ?: emptyList()
                } catch (e: Exception) {
                    emptyList()
                }
            } else {
                emptyList()
            }

            // List update logic
            val updated = currentList.toMutableList().apply {
                removeAll { it.id == track.id }
                add(0, track)
                if (size > MAX_ITEMS) removeAt(size - 1)
            }

            preferences[RECENT_KEY] = gson.toJson(updated)
        }
    }

    // Remove specific item
    suspend fun removeItem(trackId: String) {
        context.recentStore.edit { preferences ->
            val json = preferences[RECENT_KEY] ?: return@edit
            val type = object : TypeToken<List<RecentTrack>>() {}.type
            try {
                val currentList: List<RecentTrack> = gson.fromJson(json, type) ?: return@edit
                val updated = currentList.filter { it.id != trackId }
                preferences[RECENT_KEY] = gson.toJson(updated)
            } catch (e: Exception) {
                // Keep current state on failure
            }
        }
    }

    // Clear all history
    suspend fun clearAll() {
        context.recentStore.edit { preferences ->
            preferences.remove(RECENT_KEY)
        }
    }
}

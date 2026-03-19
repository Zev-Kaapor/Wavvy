package com.lonewolf.wavvy.data

// Datastore and preferences
import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
// Coroutines and flows
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Datastore delegation
private val Context.historyStore by preferencesDataStore(name = "search_history_pref")

class SearchHistoryManager(private val context: Context) {

    // Internal constants
    private companion object {
        val HISTORY_KEY = stringSetPreferencesKey("history_items_key")
        const val MAX_ITEMS = 50
    }

    // Search history flow
    val history: Flow<List<String>> = context.historyStore.data.map { preferences ->
        preferences[HISTORY_KEY]?.toList()?.reversed() ?: emptyList()
    }

    // Save or update query
    suspend fun saveSearch(query: String) {
        if (query.isBlank()) return

        context.historyStore.edit { preferences ->
            val current = preferences[HISTORY_KEY] ?: emptySet()

            // List update logic
            val updated = current.toMutableList().apply {
                remove(query)
                add(query)
                if (size > MAX_ITEMS) removeAt(0)
            }

            preferences[HISTORY_KEY] = updated.toSet()
        }
    }

    // Remove specific item
    suspend fun removeItem(query: String) {
        context.historyStore.edit { preferences ->
            val current = preferences[HISTORY_KEY] ?: return@edit
            preferences[HISTORY_KEY] = current.filter { it != query }.toSet()
        }
    }

    // Clear all history
    suspend fun clearAll() {
        context.historyStore.edit { it.clear() }
    }
}

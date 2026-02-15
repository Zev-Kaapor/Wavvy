package com.lonewolf.wavvy.data

// DataStore and preferences
import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
// Coroutines and flows
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Datastore delegate
val Context.dataStore by preferencesDataStore(name = SearchHistoryManager.DATASTORE_NAME)

class SearchHistoryManager(private val context: Context) {

    // Get search history flow
    val history: Flow<List<String>> = context.dataStore.data.map { preferences ->
        preferences[HISTORY_KEY]?.toList()?.reversed() ?: emptyList()
    }

    // Persist new search query
    suspend fun saveSearch(query: String) {
        context.dataStore.edit { preferences ->
            val currentHistory = preferences[HISTORY_KEY] ?: emptySet()

            // Move existing query to top
            val filteredHistory = currentHistory.filter { it != query }

            // Add new query and apply limit
            val updatedHistory = (filteredHistory + query).takeLast(HISTORY_LIMIT).toSet()

            preferences[HISTORY_KEY] = updatedHistory
        }
    }

    // Remove single history item
    suspend fun removeItem(query: String) {
        context.dataStore.edit { preferences ->
            val currentHistory = preferences[HISTORY_KEY] ?: emptySet()
            preferences[HISTORY_KEY] = currentHistory.filter { it != query }.toSet()
        }
    }

    // Clear all history
    suspend fun clearAll() {
        context.dataStore.edit { it.clear() }
    }

    // Centralized configuration and keys
    companion object {
        const val DATASTORE_NAME = "search_history_pref"
        const val HISTORY_LIMIT = 30
        private val HISTORY_KEY = stringSetPreferencesKey("history_items_key")
    }
}

package com.lonewolf.wavvy.data

// DataStore and preferences
import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
// Coroutines and flows
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// DataStore delegation
private val Context.historyStore by preferencesDataStore(name = SearchHistoryConstants.NAME)

// Search configuration
object SearchHistoryConstants {
    const val NAME = "search_history_pref"
    const val LIMIT = 30
    val KEY = stringSetPreferencesKey("history_items_key")
}

class SearchHistoryManager(private val context: Context) {

    // Search history flow
    val history: Flow<List<String>> = context.historyStore.data.map { preferences ->
        preferences[SearchHistoryConstants.KEY]?.toList()?.reversed() ?: emptyList()
    }

    // Save or update query
    suspend fun saveSearch(query: String) {
        context.historyStore.edit { preferences ->
            val current = preferences[SearchHistoryConstants.KEY] ?: emptySet()

            val updated = (current.filter { it != query } + query)
                .takeLast(SearchHistoryConstants.LIMIT)
                .toSet()

            preferences[SearchHistoryConstants.KEY] = updated
        }
    }

    // Remove item
    suspend fun removeItem(query: String) {
        context.historyStore.edit { preferences ->
            val current = preferences[SearchHistoryConstants.KEY] ?: emptySet()
            preferences[SearchHistoryConstants.KEY] = current.filter { it != query }.toSet()
        }
    }

    // Clear history
    suspend fun clearAll() {
        context.historyStore.edit { it.clear() }
    }
}

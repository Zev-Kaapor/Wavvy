package com.wavvy.app.features.library.data

// Android context and DataStore
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
// Coroutines and reactive flows
import kotlinx.coroutines.flow.first

// DataStore file and keys for the last choice of the library
private val Context.libraryDataStore: DataStore<Preferences> by preferencesDataStore(name = "library")
private val GroupKey = stringPreferencesKey("group")
private val SourceKey = stringPreferencesKey("source")

// Remembers the button and the list that were on, so the library opens the same way next time
class LibraryStore(context: Context) {
    private val dataStore = context.applicationContext.libraryDataStore

    // The names of the button and of the list, empty when none was chosen
    suspend fun read(): Pair<String?, String?> {
        val preferences = dataStore.data.first()
        return preferences[GroupKey] to preferences[SourceKey]
    }

    suspend fun save(group: String?, source: String?) {
        dataStore.edit { preferences ->
            if (group == null) preferences.remove(GroupKey) else preferences[GroupKey] = group
            if (source == null) preferences.remove(SourceKey) else preferences[SourceKey] = source
        }
    }
}

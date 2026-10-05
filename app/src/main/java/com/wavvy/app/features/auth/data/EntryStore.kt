package com.wavvy.app.features.auth.data

// Android context and DataStore
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
// Coroutines and reactive flows
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// How the user came into the app
enum class Entry { Guest, Google }

// DataStore file and key for the entry
private val Context.entryDataStore: DataStore<Preferences> by preferencesDataStore(name = "entry")
private val EntryKey = stringPreferencesKey("entry")

// Remembers how the user came in, so the welcome and the login only show the first time
class EntryStore(context: Context) {
    private val dataStore = context.applicationContext.entryDataStore

    // Empty until the user has chosen a way in
    val entry: Flow<Entry?> = dataStore.data.map { preferences ->
        Entry.entries.firstOrNull { it.name == preferences[EntryKey] }
    }

    suspend fun save(entry: Entry) {
        dataStore.edit { preferences -> preferences[EntryKey] = entry.name }
    }
}

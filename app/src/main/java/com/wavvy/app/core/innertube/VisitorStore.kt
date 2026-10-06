package com.wavvy.app.core.innertube

// Android context and DataStore
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
// Coroutines and reactive flows
import kotlinx.coroutines.flow.first

// DataStore file and key for the visitor identity
private val Context.innerTubeDataStore: DataStore<Preferences> by preferencesDataStore(name = "innertube")
private val VisitorDataKey = stringPreferencesKey("visitor_data")

// Keeps the identity YouTube gave to this device, so the recommendations of a guest do not start over at each launch
class VisitorStore(context: Context) {
    private val dataStore = context.applicationContext.innerTubeDataStore

    // The saved identity, or a new one asked from YouTube and saved, empty when there is none and YouTube cannot be reached
    suspend fun get(locale: YouTubeLocale): String? {
        dataStore.data.first()[VisitorDataKey]?.let { return it }

        return InnerTubeClient.fetchVisitorData(locale).getOrNull()?.also { visitorData ->
            dataStore.edit { it[VisitorDataKey] = visitorData }
        }
    }
}

package com.wavvy.app.core.lastfm

// Android context and DataStore
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
// Coroutines and reactive flows
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
// Data
import org.json.JSONArray
import org.json.JSONObject

// The name of the file, apart from the settings and left out of the backups of the system and of the app, since the key of the session is a key to the account
const val LastFmStoreName = "lastfm"

// DataStore file and keys
private val Context.lastFmDataStore: DataStore<Preferences> by preferencesDataStore(name = LastFmStoreName)
private val NameKey = stringPreferencesKey("session_name")
private val SessionKey = stringPreferencesKey("session_key")
private val EnabledKey = booleanPreferencesKey("scrobble_enabled")
private val NowPlayingKey = booleanPreferencesKey("now_playing_enabled")
private val QueueKey = stringPreferencesKey("pending_queue")

// How many listenings wait for the network at most, the oldest go first when there are more
private const val QueueLimit = 200

// What the user chose to send
data class LastFmSettings(val isEnabled: Boolean = true, val showNowPlaying: Boolean = true)

// Keeps the login of Last.fm of the user on this phone, what to send, and the listenings that could not be sent yet
class LastFmStore(context: Context) {
    private val dataStore = context.applicationContext.lastFmDataStore

    val session: Flow<LastFmSession?> = dataStore.data.map { preferences ->
        val key = preferences[SessionKey] ?: return@map null
        LastFmSession(preferences[NameKey].orEmpty(), key)
    }

    val isConnected: Flow<Boolean> = dataStore.data.map { it[SessionKey] != null }

    val settings: Flow<LastFmSettings> = dataStore.data.map { preferences ->
        LastFmSettings(preferences[EnabledKey] ?: true, preferences[NowPlayingKey] ?: true)
    }

    suspend fun saveSession(session: LastFmSession) {
        dataStore.edit {
            it[NameKey] = session.name
            it[SessionKey] = session.key
        }
    }

    // Signing out takes the login and the listenings that wait away, what the user chose to send stays
    suspend fun clear() {
        dataStore.edit {
            it.remove(NameKey)
            it.remove(SessionKey)
            it.remove(QueueKey)
        }
    }

    suspend fun update(change: (LastFmSettings) -> LastFmSettings) {
        val next = change(settings.first())
        dataStore.edit {
            it[EnabledKey] = next.isEnabled
            it[NowPlayingKey] = next.showNowPlaying
        }
    }

    // The listenings that waited for the network, in the order they happened
    suspend fun queue(): List<Scrobble> {
        val text = dataStore.data.first()[QueueKey] ?: return emptyList()
        val array = runCatching { JSONArray(text) }.getOrNull() ?: return emptyList()
        return (0 until array.length()).mapNotNull { index ->
            val item = array.optJSONObject(index) ?: return@mapNotNull null
            Scrobble(item.optString("artist"), item.optString("title"), item.optString("album").ifEmpty { null }, item.optInt("duration"), item.optLong("timestamp"))
        }
    }

    suspend fun saveQueue(songs: List<Scrobble>) {
        val array = JSONArray()
        songs.takeLast(QueueLimit).forEach { song ->
            array.put(
                JSONObject()
                    .put("artist", song.artist)
                    .put("title", song.title)
                    .put("album", song.album.orEmpty())
                    .put("duration", song.durationSec)
                    .put("timestamp", song.timestamp)
            )
        }
        dataStore.edit { if (songs.isEmpty()) it.remove(QueueKey) else it[QueueKey] = array.toString() }
    }
}

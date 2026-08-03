package com.wavvy.app.core.data.local

// Android DataStore preferences
import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
// JSON serialization
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
// Coroutines reactive flows
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map

// DataStore delegation
private val Context.chartResolutionStore by preferencesDataStore(name = "chart_resolution_cache")
private val CacheKey = stringPreferencesKey("resolved_entries")

// Cached name-to-videoId resolution entry
private data class CachedResolution(
    val videoId: String,
    val resolvedAt: Long
)

// Maps chart entry names to resolved videoIds, avoiding repeat searches
class ChartResolutionCache(private val context: Context) {

    private companion object {
        val TTL_MS = timeUnitDays(14)
        fun timeUnitDays(days: Long) = days * 24 * 60 * 60 * 1000L
    }

    private val gson = Gson()

    // Normalize artist/title into a stable cache key
    fun buildKey(artist: String?, title: String): String {
        val combined = "${artist.orEmpty()} $title".lowercase().trim()
        return combined.replace(Regex("\\s+"), " ")
    }

    // Look up a cached videoId, ignoring expired entries
    suspend fun get(key: String): String? {
        val map = readMap()
        val cached = map[key] ?: return null
        val isExpired = System.currentTimeMillis() - cached.resolvedAt > TTL_MS
        return if (isExpired) null else cached.videoId
    }

    // Store a resolved videoId for a key
    suspend fun put(key: String, videoId: String) {
        val map = readMap().toMutableMap()
        map[key] = CachedResolution(videoId, System.currentTimeMillis())
        writeMap(map)
    }

    // Read the full cache map from storage
    private suspend fun readMap(): Map<String, CachedResolution> {
        val json = context.chartResolutionStore.data
            .map { it[CacheKey] }
            .firstOrNull() ?: return emptyMap()

        val type = object : TypeToken<Map<String, CachedResolution>>() {}.type
        return try {
            gson.fromJson(json, type) ?: emptyMap()
        } catch (_: Exception) {
            emptyMap()
        }
    }

    // Persist the full cache map to storage
    private suspend fun writeMap(map: Map<String, CachedResolution>) {
        context.chartResolutionStore.edit { preferences ->
            preferences[CacheKey] = gson.toJson(map)
        }
    }
}

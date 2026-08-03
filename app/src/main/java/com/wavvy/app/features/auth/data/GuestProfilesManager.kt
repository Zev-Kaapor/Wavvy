package com.wavvy.app.features.auth.data

// Android DataStore preferences
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
// Coroutines reactive flows
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
// JSON serialization
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

// DataStore keys
private val Context.guestProfilesDataStore: DataStore<Preferences> by preferencesDataStore(name = "guest_profiles")
private val GuestProfilesKey = stringPreferencesKey("guest_profiles_list")
private val GuestProfileCounterKey = intPreferencesKey("guest_profile_counter")

// Guest profile model
data class GuestProfile(
    val id: String,
    val name: String,
    val isAutoNamed: Boolean,
    val lastUsedAt: Long
)

// Guest profiles manager
class GuestProfilesManager(private val context: Context) {

    // Get guest profiles
    suspend fun getGuestProfiles(): List<GuestProfile> {
        val raw = context.guestProfilesDataStore.data
            .map { it[GuestProfilesKey] }
            .firstOrNull() ?: return emptyList()

        val arr = try {
            JSONArray(raw)
        } catch (_: Exception) {
            return emptyList()
        }

        val result = mutableListOf<GuestProfile>()
        for (i in 0 until arr.length()) {
            try {
                val obj = arr.getJSONObject(i)
                result.add(
                    GuestProfile(
                        id = obj.getString("id"),
                        name = obj.getString("name"),
                        isAutoNamed = obj.optBoolean("isAutoNamed", false),
                        lastUsedAt = obj.getLong("lastUsedAt")
                    )
                )
            } catch (_: Exception) {
            }
        }
        return result.sortedByDescending { it.lastUsedAt }
    }

    // Create profile
    suspend fun createProfile(name: String): GuestProfile {
        val isAutoNamed = name.isBlank()
        val finalName = name.ifBlank { nextDefaultName() }
        val profile = GuestProfile(
            id = UUID.randomUUID().toString(),
            name = finalName,
            isAutoNamed = isAutoNamed,
            lastUsedAt = System.currentTimeMillis()
        )
        persist(getGuestProfiles() + profile)
        return profile
    }

    // Touch profile
    suspend fun touchProfile(id: String) {
        val updated = getGuestProfiles().map {
            if (it.id == id) it.copy(lastUsedAt = System.currentTimeMillis()) else it
        }
        persist(updated)
    }

    // Remove profile
    suspend fun removeProfile(id: String) {
        persist(getGuestProfiles().filter { it.id != id })
    }

    // Next default name
    private suspend fun nextDefaultName(): String {
        val next = (context.guestProfilesDataStore.data
            .map { it[GuestProfileCounterKey] }
            .firstOrNull() ?: 0) + 1

        context.guestProfilesDataStore.edit { it[GuestProfileCounterKey] = next }
        return "Usuário_$next"
    }

    // Persist profiles
    private suspend fun persist(profiles: List<GuestProfile>) {
        val arr = JSONArray()
        for (profile in profiles) {
            arr.put(JSONObject().apply {
                put("id", profile.id)
                put("name", profile.name)
                put("isAutoNamed", profile.isAutoNamed)
                put("lastUsedAt", profile.lastUsedAt)
            })
        }
        context.guestProfilesDataStore.edit { it[GuestProfilesKey] = arr.toString() }
    }
}

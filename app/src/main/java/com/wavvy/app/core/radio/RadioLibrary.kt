package com.wavvy.app.core.radio

// Android context
import android.content.Context
// Coroutines and reactive flows
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
// JSON
import org.json.JSONArray
import org.json.JSONObject
// Project resources
import com.wavvy.app.features.settings.data.SettingsStore

// The stations the user keeps, the favorites and the ones that were listened to lately, kept on the device with the settings,
// so they go in the backup of the Drive with them
object RadioLibrary {
    // How many stations of the ones listened to lately are kept
    private const val MaxRecents = 12

    fun favorites(context: Context): Flow<List<RadioStation>> = SettingsStore(context).radioFavorites

    fun recents(context: Context): Flow<List<RadioStation>> = SettingsStore(context).radioRecents

    // Puts the station in the favorites at the start or takes it out
    suspend fun toggleFavorite(context: Context, station: RadioStation) {
        val store = SettingsStore(context)
        val current = store.radioFavorites.first()
        store.setRadioFavorites(if (current.any { it.id == station.id }) current.filterNot { it.id == station.id } else listOf(station) + current)
    }

    // A station that was played goes to the start of the ones listened to lately, once
    suspend fun played(context: Context, station: RadioStation) {
        val store = SettingsStore(context)
        store.setRadioRecents((listOf(station) + store.radioRecents.first().filterNot { it.id == station.id }).take(MaxRecents))
    }
}

// The stations as they are kept, a list of what the site gave of each
fun radioStationsToJson(stations: List<RadioStation>): String =
    JSONArray(stations.map { station ->
        JSONObject()
            .put("id", station.id)
            .put("name", station.name)
            .put("url", station.url)
            .put("logo", station.logoUrl ?: "")
            .put("place", station.place ?: "")
            .put("segments", JSONArray(station.segments))
            .put("onAir", station.onAir ?: "")
    }).toString()

// The stations from what is kept, none when nothing is kept or it cannot be read
fun radioStationsOf(json: String?): List<RadioStation> = runCatching {
    val array = JSONArray(json ?: return emptyList())
    (0 until array.length()).map { index ->
        val item = array.getJSONObject(index)
        RadioStation(
            id = item.getString("id"),
            name = item.getString("name"),
            url = item.getString("url"),
            logoUrl = item.optString("logo").ifEmpty { null },
            place = item.optString("place").ifEmpty { null },
            segments = item.optJSONArray("segments")?.let { list -> (0 until list.length()).map(list::getString) }.orEmpty(),
            onAir = item.optString("onAir").ifEmpty { null }
        )
    }
}.getOrDefault(emptyList())

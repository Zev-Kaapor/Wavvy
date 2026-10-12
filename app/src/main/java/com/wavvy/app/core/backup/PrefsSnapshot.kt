package com.wavvy.app.core.backup

// DataStore
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
// Coroutines and JSON
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject

// The names of the kinds of value a setting can have, as they are written in the backup
private const val TypeKey = "t"
private const val ValueKey = "v"
private const val BooleanType = "boolean"
private const val IntType = "int"
private const val LongType = "long"
private const val FloatType = "float"
private const val DoubleType = "double"
private const val StringType = "string"
private const val StringSetType = "strings"

// Turns the choices kept in a DataStore into JSON and back, each with the kind of its value, so nothing is lost on the way
object PrefsSnapshot {
    // Everything in the store but the keys that stay out of the backup
    suspend fun read(store: DataStore<Preferences>, skip: Set<String> = emptySet()): JSONObject {
        val out = JSONObject()

        store.data.first().asMap().forEach { (key, value) ->
            if (key.name in skip) return@forEach

            val (type, json) = when (value) {
                is Boolean -> BooleanType to value
                is Int -> IntType to value
                is Long -> LongType to value
                is Float -> FloatType to value.toDouble()
                is Double -> DoubleType to value
                is String -> StringType to value
                is Set<*> -> StringSetType to JSONArray(value.map { it.toString() })
                else -> return@forEach
            }
            out.put(key.name, JSONObject().put(TypeKey, type).put(ValueKey, json))
        }
        return out
    }

    // Puts the values of the backup in the store, the ones that are in it now and are not in the backup are taken away, except the keys that stay out
    suspend fun write(store: DataStore<Preferences>, json: JSONObject, skip: Set<String> = emptySet()) {
        store.edit { preferences ->
            preferences.asMap().keys.filter { it.name !in skip }.forEach { preferences.remove(it) }

            json.keys().forEach { name ->
                if (name in skip) return@forEach
                val entry = json.optJSONObject(name) ?: return@forEach

                when (entry.optString(TypeKey)) {
                    BooleanType -> preferences[booleanPreferencesKey(name)] = entry.getBoolean(ValueKey)
                    IntType -> preferences[intPreferencesKey(name)] = entry.getInt(ValueKey)
                    LongType -> preferences[longPreferencesKey(name)] = entry.getLong(ValueKey)
                    FloatType -> preferences[floatPreferencesKey(name)] = entry.getDouble(ValueKey).toFloat()
                    DoubleType -> preferences[doublePreferencesKey(name)] = entry.getDouble(ValueKey)
                    StringType -> preferences[stringPreferencesKey(name)] = entry.getString(ValueKey)
                    StringSetType -> {
                        val array = entry.getJSONArray(ValueKey)
                        preferences[stringSetPreferencesKey(name)] = (0 until array.length()).map { array.getString(it) }.toSet()
                    }
                }
            }
        }
    }
}

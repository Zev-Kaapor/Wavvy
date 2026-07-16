package com.lonewolf.wavvy.data

// Android core
import android.content.Context
import android.net.Uri
// Date and time utilities
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
// JSON serialization
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader

// Helper utility for importing and exporting local settings
object SettingsBackupHelper {

    // Generates a standardized backup filename containing date and time
    fun generateBackupFileName(): String {
        val current = LocalDateTime.now()
        val formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmm")
        val formattedDate = current.format(formatter)
        return "Wavvy_backup-$formattedDate.json"
    }

    // Serializes current preferences to JSON string
    fun exportSettingsToJson(context: Context): String? {
        return try {
            val storage = SettingsStorage(context)
            val settingsMap = storage.getAllSettings()
            val jsonObject = JSONObject()

            settingsMap.forEach { (key, value) ->
                jsonObject.put(key, value)
            }
            jsonObject.toString(4)
        } catch (_: Exception) {
            null
        }
    }

    // Parses JSON string back into the Shared Preferences
    fun importSettingsFromJson(context: Context, jsonString: String): Boolean {
        return try {
            val jsonObject = JSONObject(jsonString)
            val settingsMap = mutableMapOf<String, Any>()
            val keys = jsonObject.keys()

            while (keys.hasNext()) {
                val key = keys.next()
                val value = jsonObject.get(key)
                settingsMap[key] = value
            }

            val storage = SettingsStorage(context)
            storage.importSettings(settingsMap)
            true
        } catch (_: Exception) {
            false
        }
    }

    // Writes data content to a specific Android document URI
    fun writeTextToUri(context: Context, uri: Uri, text: String): Boolean {
        return try {
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.write(text.toByteArray())
            }
            true
        } catch (_: Exception) {
            false
        }
    }

    // Reads data content from a specific Android document URI
    fun readTextFromUri(context: Context, uri: Uri): String? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    reader.readText()
                }
            }
        } catch (_: Exception) {
            null
        }
    }
}

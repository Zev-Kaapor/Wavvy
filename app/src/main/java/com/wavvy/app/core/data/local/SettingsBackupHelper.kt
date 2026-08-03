package com.wavvy.app.core.data.local

// Android and platform utilities
import android.content.Context
import android.net.Uri
// JSON structures
import org.json.JSONObject
// IO and date libraries
import java.io.BufferedReader
import java.io.InputStreamReader
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

// Backup file name generator and stream operations
object SettingsBackupHelper {

    // Backup filename generation
    fun generateBackupFileName(): String {
        val current = LocalDateTime.now()
        val formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmm")
        val formattedDate = current.format(formatter)
        return "Wavvy_backup-$formattedDate.json"
    }

    // Export local preferences to text formatting
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

    // Import formatting conversion to storage
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

    // File writing framework
    fun writeTextToUri(context: Context, uri: Uri, text: String): Boolean {
        return try {
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.write(text.toByteArray(Charsets.UTF_8))
            }
            true
        } catch (_: Exception) {
            false
        }
    }

    // File reading framework
    fun readTextFromUri(context: Context, uri: Uri): String? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8)).use { reader ->
                    reader.readText()
                }
            }
        } catch (_: Exception) {
            null
        }
    }
}

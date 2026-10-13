package com.wavvy.app.core.drive

// Android text and time
import android.net.Uri
import java.io.IOException
import java.time.Instant
// Coroutines and the web
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
// Project resources
import com.wavvy.app.core.network.WavvyProxy

// The backup that is in the Drive of the user, when it was changed and how big it is
data class RemoteBackup(val id: String, val modifiedAt: Instant, val sizeBytes: Long)

// The addresses of the Drive, the folder that is private to the app and the name of the file that is kept in it
private const val FilesAddress = "https://www.googleapis.com/drive/v3/files"
private const val AppFolder = "appDataFolder"
private const val BackupName = "wavvy-backup.wvy"
private const val UploadAddress = "https://www.googleapis.com/upload/drive/v3/files"
private const val JsonType = "application/json; charset=UTF-8"
private const val FileType = "application/octet-stream"
private const val RelatedType = "multipart/related"
private const val Fields = "id,modifiedTime,size"

// Talks to the private folder of the app in the Drive of the user, through the same connection as the rest of the app so the proxy is kept
object DriveClient {
    // The backup of the app, null when there is none yet
    suspend fun find(token: String): RemoteBackup? = withContext(Dispatchers.IO) {
        val address = Uri.parse(FilesAddress).buildUpon()
            .appendQueryParameter("spaces", AppFolder)
            .appendQueryParameter("q", "name = '$BackupName' and trashed = false")
            .appendQueryParameter("orderBy", "modifiedTime desc")
            .appendQueryParameter("fields", "files(id,modifiedTime,size)")
            .build()
            .toString()

        val request = Request.Builder().url(address).header("Authorization", "Bearer $token").build()
        val body = WavvyProxy.client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("Drive answered ${response.code}")
            response.body.string()
        }

        val file = JSONObject(body).optJSONArray("files")?.optJSONObject(0) ?: return@withContext null
        parse(file)
    }

    // Sends the file, a new one in the private folder or in place of the one that is there
    suspend fun save(token: String, bytes: ByteArray, existingId: String?): RemoteBackup = withContext(Dispatchers.IO) {
        val metadata = JSONObject().put("name", BackupName).apply { if (existingId == null) put("parents", JSONArray().put(AppFolder)) }
        val body = MultipartBody.Builder()
            .setType(RelatedType.toMediaType())
            .addPart(metadata.toString().toRequestBody(JsonType.toMediaType()))
            .addPart(bytes.toRequestBody(FileType.toMediaType()))
            .build()

        val address = "$UploadAddress${existingId?.let { "/$it" }.orEmpty()}?uploadType=multipart&fields=$Fields"
        val request = Request.Builder().url(address).header("Authorization", "Bearer $token").let { builder ->
            if (existingId == null) builder.post(body) else builder.patch(body)
        }.build()

        val answer = WavvyProxy.client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("Drive answered ${response.code}")
            response.body.string()
        }
        parse(JSONObject(answer))
    }

    // The bytes of the backup that is in the Drive
    suspend fun read(token: String, id: String): ByteArray = withContext(Dispatchers.IO) {
        val request = Request.Builder().url("$FilesAddress/$id?alt=media").header("Authorization", "Bearer $token").build()
        WavvyProxy.client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("Drive answered ${response.code}")
            response.body.bytes()
        }
    }

    private fun parse(file: JSONObject) = RemoteBackup(
        id = file.getString("id"),
        modifiedAt = Instant.parse(file.getString("modifiedTime")),
        sizeBytes = file.optString("size").toLongOrNull() ?: 0L
    )
}

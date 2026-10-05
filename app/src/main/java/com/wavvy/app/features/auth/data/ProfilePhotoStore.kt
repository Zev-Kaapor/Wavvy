package com.wavvy.app.features.auth.data

// Android context and images
import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
// Coroutines and files
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

// Files of the photo, its address and the copy being downloaded, in the private storage of the app
private const val PhotoFileName = "profile_photo.jpg"
private const val UrlFileName = "profile_photo.url"
private const val TempFileName = "profile_photo.tmp"

// Keeps the profile photo on the device, so it shows even without a connection
object ProfilePhotoStore {
    private fun photoFile(context: Context) = File(context.applicationContext.filesDir, PhotoFileName)
    private fun urlFile(context: Context) = File(context.applicationContext.filesDir, UrlFileName)
    private fun tempFile(context: Context) = File(context.applicationContext.filesDir, TempFileName)

    // Empty when there is no photo or it cannot be read
    suspend fun load(context: Context): ImageBitmap? = withContext(Dispatchers.IO) {
        photoFile(context).takeIf { it.exists() }?.let { BitmapFactory.decodeFile(it.path)?.asImageBitmap() }
    }

    // Gives true when the photo on the device changed, and downloads again only when the address is new
    suspend fun save(context: Context, photoUrl: String?): Result<Boolean> = withContext(Dispatchers.IO) {
        runCatching {
            val photo = photoFile(context)
            val url = urlFile(context)

            when {
                // The account has no photo, so the old one goes away
                photoUrl == null -> photo.delete() or url.delete()
                photo.exists() && url.exists() && url.readText() == photoUrl -> false
                else -> {
                    val temp = tempFile(context)
                    AccountClient.downloadPhoto(photoUrl, temp).getOrThrow()
                    check(temp.renameTo(photo)) { "Photo could not be saved" }
                    url.writeText(photoUrl)
                    true
                }
            }
        }
    }
}

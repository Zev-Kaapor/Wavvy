package com.wavvy.app.core.device

// Android context, permissions and the media of the phone
import android.Manifest
import android.app.RecoverableSecurityException
import android.content.ContentUris
import android.content.Context
import android.content.IntentSender
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.core.content.ContextCompat
// Coroutines
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// A song file of the phone, its id is the address of the file in the media of the phone, which is also what the player opens
// The folder is the path of the place of the file, as the phone writes it
data class DeviceSong(
    val id: String,
    val title: String,
    val artist: String?,
    val artworkUrl: String,
    val durationMs: Long,
    val sizeBytes: Long,
    val addedAt: Long,
    val folder: String
)

// What the media of the phone writes when it does not know the artist, and the start of the address of the covers of the albums
private const val UnknownArtist = "<unknown>"
private const val AlbumArtAddress = "content://media/external/audio/albumart"
private const val ContentPrefix = "content://"
private const val MillisPerSecond = 1000L
private const val PathSeparator = '/'

// True when a song is a file of the phone, which has no page, no like and no radio of YouTube Music
fun isDeviceTrack(id: String?): Boolean = id?.startsWith(ContentPrefix) == true

// What happened when files were asked to be deleted, they are gone, the system has to ask the user first, or they could not be deleted
sealed interface DeleteResult {
    data object Done : DeleteResult
    data class Ask(val question: IntentSender, val remaining: List<String>) : DeleteResult
    data object Failed : DeleteResult
}

// The music that is on the phone, read from its media with the permission of the user
object DeviceMusic {
    // The permission to read the audio files, which has a new name from Android 13
    val permission: String
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) Manifest.permission.READ_MEDIA_AUDIO else Manifest.permission.READ_EXTERNAL_STORAGE

    fun hasPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

    // The songs of the phone, the recordings, the ringtones and the alarms are not music and stay out
    suspend fun songs(context: Context): List<DeviceSong> = withContext(Dispatchers.IO) {
        if (!hasPermission(context)) return@withContext emptyList()

        val songs = mutableListOf<DeviceSong>()

        // The place of the file has its own column from Android 10, before it is part of the path of the file
        val isPathSplit = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
        val pathColumn = if (isPathSplit) MediaStore.Audio.Media.RELATIVE_PATH else MediaStore.Audio.Media.DATA
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.DATE_ADDED,
            pathColumn
        )

        runCatching {
            context.contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                "${MediaStore.Audio.Media.IS_MUSIC} != 0 AND ${MediaStore.Audio.Media.DURATION} > 0",
                null,
                "${MediaStore.Audio.Media.DATE_ADDED} DESC"
            )?.use { cursor ->
                val id = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val title = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val name = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
                val artist = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val album = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
                val duration = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val size = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
                val added = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
                val path = cursor.getColumnIndexOrThrow(pathColumn)

                while (cursor.moveToNext()) {
                    val place = cursor.getString(path).orEmpty()
                    songs += DeviceSong(
                        id = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, cursor.getLong(id)).toString(),
                        title = cursor.getString(title).orEmpty().ifBlank { cursor.getString(name).orEmpty().substringBeforeLast('.') },
                        artist = cursor.getString(artist)?.takeIf { it.isNotBlank() && it != UnknownArtist },
                        artworkUrl = ContentUris.withAppendedId(Uri.parse(AlbumArtAddress), cursor.getLong(album)).toString(),
                        durationMs = cursor.getLong(duration),
                        sizeBytes = cursor.getLong(size),
                        addedAt = cursor.getLong(added) * MillisPerSecond,
                        folder = (if (isPathSplit) place else place.substringBeforeLast(PathSeparator)).trimEnd(PathSeparator)
                    )
                }
            }
        }
        songs
    }

    // From Android 11 the system itself asks the user to delete the files, and gives the question to show
    fun deleteQuestion(context: Context, ids: List<String>): IntentSender? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) MediaStore.createDeleteRequest(context.contentResolver, ids.map(Uri::parse)).intentSender else null

    // Before Android 11 the files are deleted here, Android 10 asks the user about each file that is not the app's own, and tells the question to show and the files that are left
    suspend fun delete(context: Context, ids: List<String>): DeleteResult = withContext(Dispatchers.IO) {
        ids.forEachIndexed { index, id ->
            try {
                context.contentResolver.delete(Uri.parse(id), null, null)
            } catch (failure: SecurityException) {
                val question = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) (failure as? RecoverableSecurityException)?.userAction?.actionIntent?.intentSender else null
                return@withContext if (question != null) DeleteResult.Ask(question, ids.drop(index)) else DeleteResult.Failed
            }
        }
        DeleteResult.Done
    }
}

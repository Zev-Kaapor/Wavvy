package com.wavvy.app.features.playlist.data

// Android context and pictures
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
// Coroutines
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
// Streams
import java.io.ByteArrayOutputStream

// Turns the picture that was chosen into the cover of a playlist, cut to the 16:9 shape that YouTube Music keeps
object PlaylistCover {
    // The shape of the cover, and the width and the quality of the picture that is sent
    const val RatioWidth = 16f
    const val RatioHeight = 9f
    private const val SentWidth = 1280
    private const val Quality = 90
    private const val ReadWidth = 2048

    // The bytes of the picture cut in the middle to 16:9, empty when the picture cannot be read
    suspend fun encode(context: Context, uri: Uri): ByteArray? = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver

        // Reads it smaller first, a photo of the camera is far bigger than what the cover needs
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@withContext null

        var sample = 1
        while (bounds.outWidth / (sample * 2) >= ReadWidth) sample *= 2
        val source = resolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
        } ?: return@withContext null

        // The biggest 16:9 piece from the middle
        val wideEnough = source.width * RatioHeight >= source.height * RatioWidth
        val cutWidth = if (wideEnough) (source.height * RatioWidth / RatioHeight).toInt() else source.width
        val cutHeight = if (wideEnough) source.height else (source.width * RatioHeight / RatioWidth).toInt()
        val cut = Bitmap.createBitmap(source, (source.width - cutWidth) / 2, (source.height - cutHeight) / 2, cutWidth, cutHeight)
        val sent = if (cut.width > SentWidth) Bitmap.createScaledBitmap(cut, SentWidth, (SentWidth * RatioHeight / RatioWidth).toInt(), true) else cut

        ByteArrayOutputStream().use { output ->
            sent.compress(Bitmap.CompressFormat.JPEG, Quality, output)
            output.toByteArray()
        }
    }
}

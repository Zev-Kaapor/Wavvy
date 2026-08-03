package com.wavvy.app.features.player.models

// Media3
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.core.net.toUri
// Project models
import com.wavvy.app.features.player.ui.components.QueueSong

// Map UI tracking structures into framework native media layers
fun QueueSong.toMediaItem(audioUrl: String? = null): MediaItem {
    val metadata = MediaMetadata.Builder()
        .setTitle(title)
        .setArtist(artist)
        .setDisplayTitle(title)
        .setSubtitle(artist)
        .setArtworkUri(if (imageUrl.isNotBlank()) imageUrl.toUri() else null)
        .build()

    val resolvedUri = if (!audioUrl.isNullOrBlank()) audioUrl else "https://music.youtube.com/watch?v=$id"

    return MediaItem.Builder()
        .setMediaId(id)
        .setUri(resolvedUri)
        .setMediaMetadata(metadata)
        .build()
}

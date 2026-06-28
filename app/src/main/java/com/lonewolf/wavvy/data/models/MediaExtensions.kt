package com.lonewolf.wavvy.data.models

import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.lonewolf.wavvy.ui.player.components.QueueSong

// Map UI tracking structures into framework native media layers
fun QueueSong.toMediaItem(audioUrl: String? = null): MediaItem {
    val metadata = MediaMetadata.Builder()
        .setTitle(title ?: "")
        .setArtist(artist ?: "")
        .setDisplayTitle(title ?: "")
        .setSubtitle(artist ?: "")
        .setArtworkUri(if (!imageUrl.isNullOrBlank()) Uri.parse(imageUrl) else null)
        .build()

    val resolvedUri = if (!audioUrl.isNullOrBlank()) audioUrl else "https://music.youtube.com/watch?v=$id"

    return MediaItem.Builder()
        .setMediaId(id)
        .setUri(resolvedUri)
        .setMediaMetadata(metadata)
        .build()
}

package com.lonewolf.wavvy.data.models

import java.io.Serializable

// Track metadata representation structures
data class Track(
    val id: String,
    val title: String,
    val artist: String,
    val audioUrl: String,
    val thumbnailUrl: String?,
    val durationMs: Long = 0L
) : Serializable

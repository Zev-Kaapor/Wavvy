package com.wavvy.app.features.home.models

// Compose runtime stability annotations
import androidx.compose.runtime.Immutable

// Quick picks section entity representation
@Immutable
data class QuickPick(
    val videoId: String,
    val title: String,
    val artist: String,
    val artists: List<String>,
    val thumbnailUrl: String?
)

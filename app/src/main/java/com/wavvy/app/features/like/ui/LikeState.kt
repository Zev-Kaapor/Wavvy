package com.wavvy.app.features.like.ui

// Compose state and runtime
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
// UI utilities
import androidx.compose.ui.platform.LocalContext
// Project resources
import com.wavvy.app.features.like.data.Likes

// Whether the account likes a song and what the button of it does
class LikeState(val isLiked: Boolean, val toggle: () -> Unit)

// The like of a song, asked once when it first shows and kept up to date as it changes anywhere
@Composable
fun rememberLikeState(videoId: String): LikeState {
    val context = LocalContext.current
    val liked by Likes.liked.collectAsState()

    LaunchedEffect(videoId) { Likes.load(context, videoId) }

    return LikeState(isLiked = liked[videoId] == true, toggle = { Likes.toggle(context, videoId) })
}

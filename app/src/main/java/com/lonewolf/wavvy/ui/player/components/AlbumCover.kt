package com.lonewolf.wavvy.ui.player.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import coil.size.Size

@Composable
fun AlbumCover(
    progress: Float,
    screenWidth: Dp,
    imageUrl: String?
) {
    val context = LocalContext.current

    // Fade levels
    val fadeMultiplier = 0.40f

    // Animation values
    val currentSize = lerp(52.dp, screenWidth, progress)
    val offsetX = lerp(12.dp, 0.dp, progress)
    val offsetY = lerp(6.dp, 90.dp, progress)
    val coverRoundness = lerp(12.dp, 8.dp, progress)

    Box(modifier = Modifier.fillMaxSize()) {

        // Background Layer
        if (progress > 0.01f && !imageUrl.isNullOrEmpty()) {
            Box(modifier = Modifier.fillMaxSize()) {
                SubcomposeAsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(imageUrl)
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .scale(1.0f)
                        .blur(100.dp)
                        .alpha(progress)
                )

                // High visibility gradient
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                0.2f to Color.White.copy(alpha = 0.1f),
                                0.5f to Color.Transparent,
                                0.8f to Color.Black.copy(alpha = 0.6f),
                                1.0f to Color.Black.copy(alpha = 0.9f) // Fixed: Added 'f'
                            )
                        )
                )
            }
        }

        // Foreground Layer: Main Artwork
        Box(
            modifier = Modifier
                .offset(offsetX, offsetY)
                .size(currentSize)
                .graphicsLayer {
                    // Control main cover transparency here
                    alpha = 1.0f
                    compositingStrategy = CompositingStrategy.Offscreen
                }
                .drawWithContent {
                    drawContent()

                    if (progress > 0.5f) {
                        val animProgress = (progress - 0.5f) * 2f
                        val dynamicFade = fadeMultiplier * animProgress

                        // Top and Bottom fade
                        drawRect(
                            brush = Brush.verticalGradient(
                                0f to Color.Transparent,
                                dynamicFade to Color.Black,
                                (1f - dynamicFade) to Color.Black,
                                1f to Color.Transparent
                            ),
                            blendMode = BlendMode.DstIn
                        )
                    }
                }
                .clip(RoundedCornerShape(coverRoundness)),
            contentAlignment = Alignment.Center
        ) {
            if (imageUrl.isNullOrEmpty()) {
                AlbumPlaceholder()
            } else {
                SubcomposeAsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(imageUrl)
                        .crossfade(true)
                        .size(Size.ORIGINAL)
                        .build(),
                    contentDescription = "Main Cover",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                    loading = { AlbumPlaceholder() },
                    error = { AlbumPlaceholder() }
                )
            }
        }
    }
}

@Composable
private fun AlbumPlaceholder() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.DarkGray),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.MusicNote,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(0.4f),
            tint = Color.White.copy(alpha = 0.2f)
        )
    }
}

package com.lonewolf.wavvy.ui.player.components

// Compose foundation and layout
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
// Icons and Material 3
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
// State and UI tools
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size as ComposeSize
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
// Image loading (Coil)
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import coil.size.Size

// Dynamic album cover with transition animations
@Composable
fun AlbumCover(
    progress: Float,
    songProgress: Float,
    screenWidth: Dp,
    imageUrl: String?
) {
    val context = LocalContext.current
    val neonColor = MaterialTheme.colorScheme.tertiary
    val fadeMultiplier = 0.40f

    // Animated spatial values
    val currentSize = lerp(44.dp, screenWidth, progress)
    val offsetX = lerp(16.dp, 0.dp, progress)
    val offsetY = lerp(10.dp, 90.dp, progress)
    val coverRoundness = lerp(22.dp, 8.dp, progress)

    Box(modifier = Modifier.fillMaxSize()) {
        // Dynamic background blur layer
        if (progress > 0.01f && !imageUrl.isNullOrEmpty()) {
            Box(modifier = Modifier.fillMaxSize()) {
                SubcomposeAsyncImage(
                    model = ImageRequest.Builder(context).data(imageUrl).build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .scale(1.0f)
                        .blur(100.dp)
                        .alpha(progress)
                )

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                0.2f to Color.White.copy(alpha = 0.1f),
                                0.5f to Color.Transparent,
                                0.8f to Color.Black.copy(alpha = 0.6f),
                                1.0f to Color.Black.copy(alpha = 0.9f)
                            )
                        )
                )
            }
        }

        // Main artwork container
        Box(
            modifier = Modifier
                .offset(offsetX, offsetY)
                .size(currentSize),
            contentAlignment = Alignment.Center
        ) {
            // Mini-player progress ring
            if (progress < 0.1f) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokeWidth = 2.dp.toPx()
                    val extraPadding = 3.dp.toPx()
                    val arcSize = ComposeSize(
                        width = size.width + (extraPadding * 2),
                        height = size.height + (extraPadding * 2)
                    )
                    val arcTopLeft = Offset(-extraPadding, -extraPadding)

                    drawCircle(
                        color = Color.White.copy(alpha = 0.1f),
                        radius = (size.width / 2) + extraPadding,
                        style = Stroke(width = strokeWidth)
                    )

                    drawArc(
                        color = neonColor,
                        startAngle = -90f,
                        sweepAngle = 360f * songProgress,
                        useCenter = false,
                        topLeft = arcTopLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                }
            }

            // Cover image with dynamic fade effects
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                    .drawWithContent {
                        drawContent()
                        if (progress > 0.5f) {
                            val animProgress = (progress - 0.5f) * 2f
                            val dynamicFade = fadeMultiplier * animProgress
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
}

// Default placeholder for missing artwork
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
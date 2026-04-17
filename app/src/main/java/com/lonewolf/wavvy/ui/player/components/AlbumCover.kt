package com.lonewolf.wavvy.ui.player.components

// Compose animation and core
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
// Foundation and layout
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
import androidx.compose.runtime.getValue
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
// Project resources
import com.lonewolf.wavvy.ui.theme.WavvyTheme

// Dynamic album cover with transition animations
@Composable
fun AlbumCover(
    progress: Float,
    songProgress: Float,
    screenWidth: Dp,
    imageUrl: String?,
    showFrontCard: Boolean = true,
    isLandscape: Boolean = false
) {
    val context = LocalContext.current
    // Circular progress bar in the mini player
    val isDark = isSystemInDarkTheme()
    val neonColor = if (isDark) MaterialTheme.colorScheme.tertiary else Color.Black
    val fadeMultiplier = 0.40f

    // Animated spatial values
    val miniSize = 44.dp
    val expandedSize = if (isLandscape) 280.dp else screenWidth
    val currentSize = lerp(miniSize, expandedSize, progress)

    val miniOffsetX = 16.dp
    val expandedOffsetX = if (isLandscape) 40.dp else 0.dp
    val offsetX = lerp(miniOffsetX, expandedOffsetX, progress)

    val miniOffsetY = 10.dp
    val expandedOffsetY = if (isLandscape) 40.dp else 90.dp
    val offsetY = lerp(miniOffsetY, expandedOffsetY, progress)

    val coverRoundness = lerp(22.dp, 16.dp, progress)

    // Fade animation for lyrics mode synchronization
    val lyricsTransitionAlpha by animateFloatAsState(
        targetValue = if (showFrontCard || progress < 0.5f) 1f else 0f,
        animationSpec = if (progress < 0.5f) tween(0) else tween(400),
        label = "LyricsSyncFade"
    )

    Box(modifier = Modifier.fillMaxSize()) {
        // Background blur layer
        Box(modifier = Modifier.fillMaxSize().alpha(progress)) {
            if (!imageUrl.isNullOrEmpty()) {
                SubcomposeAsyncImage(
                    model = ImageRequest.Builder(context).data(imageUrl).build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .scale(1.0f)
                        .blur(100.dp)
                )
            } else {
                // Background remains black when there's no image
                Box(modifier = Modifier.fillMaxSize().background(Color.Black))
            }

            // Blurred background gradient overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0.0f to Color.Black.copy(alpha = 0.5f),
                            0.2f to Color.Black.copy(alpha = 0.15f),
                            0.4f to Color.Transparent,
                            0.55f to Color.Transparent,
                            0.68f to Color.Black.copy(alpha = 0.45f),
                            0.85f to Color.Black.copy(alpha = 0.85f),
                            1.0f to Color.Black
                        )
                    )
            )
        }

        // Main artwork container
        Box(
            modifier = Modifier
                .offset(offsetX, offsetY)
                .size(currentSize)
                .alpha(lyricsTransitionAlpha),
            contentAlignment = Alignment.Center
        ) {
            // Mini-player progress ring with smooth transition
            val ringAlpha = (1f - (progress / 0.2f)).coerceIn(0f, 1f)

            if (ringAlpha > 0f) {
                val trackColor = if (isDark) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f * ringAlpha) else Color.Black.copy(alpha = 0.1f * ringAlpha)
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokeWidth = 2.dp.toPx()
                    val extraPadding = 3.dp.toPx()
                    val arcSize = ComposeSize(
                        width = size.width + (extraPadding * 2),
                        height = size.height + (extraPadding * 2)
                    )
                    val arcTopLeft = Offset(-extraPadding, -extraPadding)

                    // Track circle
                    drawCircle(
                        color = trackColor,
                        radius = (size.width / 2) + extraPadding,
                        style = Stroke(width = strokeWidth)
                    )

                    // Progress arc
                    drawArc(
                        color = neonColor.copy(alpha = ringAlpha),
                        startAngle = -90f,
                        sweepAngle = 360f * songProgress,
                        useCenter = false,
                        topLeft = arcTopLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                }
            }

            // Cover image
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                    .drawWithContent {
                        drawContent()
                        // Fade in/out mask
                        if (progress > 0.5f) {
                            val animProgress = (progress - 0.5f) * 2f
                            val dynamicFade = fadeMultiplier * animProgress
                            
                            if (isLandscape) {
                                // Horizontal fade for landscape
                                drawRect(
                                    brush = Brush.horizontalGradient(
                                        0f to Color.Transparent,
                                        dynamicFade to Color.Black,
                                        (1f - dynamicFade) to Color.Black,
                                        1f to Color.Transparent
                                    ),
                                    blendMode = BlendMode.DstIn
                                )
                                // Combine with vertical fade for a smooth "vignette" effect
                                drawRect(
                                    brush = Brush.verticalGradient(
                                        0f to Color.Transparent,
                                        dynamicFade to Color.Black,
                                        (1f - dynamicFade) to Color.Black,
                                        1f to Color.Transparent
                                    ),
                                    blendMode = BlendMode.DstIn
                                )
                            } else {
                                // Original vertical fade for portrait
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
    WavvyTheme(darkTheme = true) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.MusicNote,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(0.4f),
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
            )
        }
    }
}

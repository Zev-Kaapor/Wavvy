package com.wavvy.app.features.player.ui.components

// Compose layouts and foundations
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
// Material 3 components
import androidx.compose.material3.Icon
// Compose state and runtime
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
// UI styling and utilities
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.lerp
// Image loading
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import coil3.request.transformations
// Math
import kotlin.math.roundToInt
// Project resources
import com.wavvy.app.core.designsystem.components.VideoSquareCrop
import com.wavvy.app.core.designsystem.icons.WavvyIcons
import com.wavvy.app.core.designsystem.theme.WavvyTheme
import com.wavvy.app.core.innertube.isVideoThumbnail
import com.wavvy.app.core.innertube.largestVideoThumbnail
import com.wavvy.app.core.innertube.resize

// Arc where the progress ring starts, at the top
private const val RingStartAngle = -90f

// Mask that keeps the middle of the open cover and clears its edges
private val MaskKeep = Color.Black
private val MaskClear = Color.Transparent

// Cover that grows from the round one of the pill to the big one of the open player, over the blurred backdrop, as in the old Wavvy
// The opening is read only while measuring and drawing, so the cover follows the finger without being built again
@Composable
fun AlbumCover(
    progress: () -> Float,
    coverAlpha: () -> Float,
    imageUrl: String?,
    songProgress: () -> Float,
    screenWidth: Dp,
    screenHeight: Dp,
    isLandscape: Boolean
) {
    val context = LocalContext.current
    val colors = WavvyTheme.colors

    // Videos come wide, so their largest picture is asked and cut square, and the given one is used when the largest does not exist
    val isVideoPicture = imageUrl?.isVideoThumbnail() == true
    var useGivenPicture by remember(imageUrl) { mutableStateOf(false) }

    // The cover is always asked at full size, never at the size of the pill it starts in
    val coverRequest = remember(imageUrl, useGivenPicture) {
        imageUrl?.takeIf { it.isNotEmpty() }?.let { url ->
            val largest = if (useGivenPicture) null else url.largestVideoThumbnail()
            ImageRequest.Builder(context)
                .data(largest ?: url.resize(PlayerDimens.CoverRequestSize, PlayerDimens.CoverRequestSize))
                .size(PlayerDimens.CoverRequestSize)
                .crossfade(true)
                .apply { if (isVideoPicture) transformations(VideoSquareCrop) }
                .build()
        }
    }
    val backdropRequest = remember(imageUrl) {
        imageUrl?.takeIf { it.isNotEmpty() }?.let { url ->
            val blur = BackdropBlur(PlayerDimens.BackdropBlurRadius, PlayerDimens.BackdropBlurPasses)
            ImageRequest.Builder(context)
                .data(url.resize(PlayerDimens.BackdropRequestSize, PlayerDimens.BackdropRequestSize))
                .size(PlayerDimens.BackdropRequestSize)
                .transformations(if (isVideoPicture) listOf(VideoSquareCrop, blur) else listOf(blur))
                .build()
        }
    }

    // Size and place of the open cover
    val expandedSize = when {
        isLandscape -> PlayerDimens.ExpandedCoverLandscape
        screenHeight < PlayerDimens.ShortScreenHeight -> screenWidth * PlayerDimens.ShortScreenCoverFraction
        else -> screenWidth
    }
    val expandedX = if (isLandscape) PlayerDimens.CoverLandscapeOffset else (screenWidth - expandedSize) / 2
    val expandedY = if (isLandscape) PlayerDimens.CoverLandscapeOffset else screenHeight * PlayerDimens.ExpandedCoverTopFraction

    Box(modifier = Modifier.fillMaxSize()) {
        Backdrop(request = backdropRequest, progress = progress)

        Box(
            modifier = Modifier
                .layout { measurable, constraints ->
                    val opening = progress()
                    val side = lerp(PlayerDimens.MiniCover, expandedSize, opening).roundToPx()
                    val placeable = measurable.measure(Constraints.fixed(side, side))
                    layout(constraints.maxWidth, constraints.maxHeight) {
                        placeable.place(
                            lerp(PlayerDimens.MiniCoverStart, expandedX, opening).roundToPx(),
                            lerp(PlayerDimens.MiniCoverTop, expandedY, opening).roundToPx()
                        )
                    }
                }
                // Ring of the pill, gone early in the opening, its song progress read while drawing
                .drawBehind {
                    val ringAlpha = (1f - progress() / PlayerDimens.RingFadeEnd).coerceIn(0f, 1f)
                    if (ringAlpha > 0f) {
                        val strokeWidth = PlayerDimens.RingStroke.toPx()
                        val gap = PlayerDimens.RingGap.toPx()
                        drawCircle(
                            color = colors.playerRingTrack.copy(alpha = colors.playerRingTrack.alpha * ringAlpha),
                            radius = size.width / 2 + gap,
                            style = Stroke(width = strokeWidth)
                        )
                        drawArc(
                            color = colors.playerRing.copy(alpha = ringAlpha),
                            startAngle = RingStartAngle,
                            sweepAngle = PlayerDimens.FullTurn * songProgress(),
                            useCenter = false,
                            topLeft = Offset(-gap, -gap),
                            size = Size(size.width + gap * 2, size.height + gap * 2),
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )
                    }
                }
                .graphicsLayer {
                    val opening = progress()
                    // The cover fades out while the lyrics are open
                    alpha = coverAlpha()
                    shape = RoundedCornerShape(lerp(PlayerDimens.MiniCoverCorner, PlayerDimens.ExpandedCoverCorner, opening))
                    clip = true
                    // The fade of the edges needs its own layer, only while it shows
                    compositingStrategy = if (opening > PlayerDimens.CoverFadeStart) CompositingStrategy.Offscreen else CompositingStrategy.Auto
                }
                .drawWithContent {
                    drawContent()
                    val opening = progress()
                    if (opening > PlayerDimens.CoverFadeStart) {
                        val fade = PlayerDimens.CoverFadeStrength * (opening - PlayerDimens.CoverFadeStart) / (1f - PlayerDimens.CoverFadeStart)
                        val mask = arrayOf(0f to MaskClear, fade to MaskKeep, (1f - fade) to MaskKeep, 1f to MaskClear)
                        if (isLandscape) drawRect(brush = Brush.horizontalGradient(*mask), blendMode = BlendMode.DstIn)
                        drawRect(brush = Brush.verticalGradient(*mask), blendMode = BlendMode.DstIn)
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            // The note shows under the picture until it arrives
            AlbumPlaceholder()
            if (coverRequest != null) {
                AsyncImage(
                    model = coverRequest,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    filterQuality = FilterQuality.High,
                    onError = { if (isVideoPicture) useGivenPicture = true },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

// Tiny blurred cover stretched behind the whole open player, with a dark fade so the controls stay readable
@Composable
private fun Backdrop(request: ImageRequest?, progress: () -> Float) {
    val shade = WavvyTheme.colors.tileScrim
    val shadeBrush = remember(shade) {
        Brush.verticalGradient(*PlayerDimens.BackdropShade.map { (stop, strength) -> stop to shade.copy(alpha = strength) }.toTypedArray())
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { alpha = progress() }
            .background(shade)
    ) {
        if (request != null) {
            AsyncImage(
                model = request,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                filterQuality = FilterQuality.High,
                modifier = Modifier.fillMaxSize()
            )
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(shadeBrush)
        )
    }
}

// Dark square with a faint note, shown while there is no picture
@Composable
internal fun AlbumPlaceholder() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(WavvyTheme.colors.coverPlaceholder),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = WavvyIcons.MusicNote,
            contentDescription = null,
            tint = WavvyTheme.colors.onMedia.copy(alpha = PlayerDimens.PlaceholderIconAlpha),
            modifier = Modifier.fillMaxSize(PlayerDimens.PlaceholderIconFraction)
        )
    }
}

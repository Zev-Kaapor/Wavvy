package com.lonewolf.wavvy.ui.player.components

// Compose animation mechanics
import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
// Foundation and layout
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
// Material 3 and icons
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
// State and UI tools
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
// Project resources
import com.lonewolf.wavvy.R
import com.lonewolf.wavvy.ui.theme.Poppins

// Collapsed mini-player view integrated with NavBar
@SuppressLint("UseOfNonLambdaOffsetOverload")
@Composable
fun MiniPlayerContent(
    isExpanded: Boolean,
    songTitle: String,
    artistNames: List<String>,
    screenWidth: Dp,
    springSpec: AnimationSpec<Dp>,
    modifier: Modifier = Modifier,
    progress: Float = 0f,
    isLoading: Boolean = false
) {
    // Theme and visibility states
    val isDark = isSystemInDarkTheme()
    val baseColor = if (isDark) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary
    val animatedAlpha = (1f - (progress * 5f)).coerceIn(0f, 1f)

    // Artist names formatting logic
    val fallbackArtist = stringResource(R.string.default_artist_name)
    val displayArtists = remember(artistNames) {
        val filtered = artistNames.map { it.trim() }.filter { it.isNotBlank() }
        if (filtered.isNotEmpty()) filtered.joinToString(", ") else fallbackArtist
    }

    // Dynamic color interpolation
    val buttonTint = lerp(baseColor, baseColor.copy(alpha = 0f), (progress * 4f).coerceIn(0f, 1f))
    val buttonBgColor = lerp(baseColor.copy(alpha = 0.08f), Color.Transparent, (progress * 4f).coerceIn(0f, 1f))

    // Layout transformation animations
    val textOffsetX by animateDpAsState(
        targetValue = if (isExpanded) 0.dp else 72.dp,
        animationSpec = springSpec,
        label = "TextX"
    )
    val titleFontSize by animateFloatAsState(
        targetValue = if (isExpanded) 0f else 14f,
        animationSpec = tween(300),
        label = "FontSize"
    )

    // Control elements animations
    val buttonSize by animateDpAsState(
        targetValue = if (isExpanded) 0.dp else 40.dp,
        animationSpec = springSpec,
        label = "BtnSize"
    )
    val iconSize by animateDpAsState(
        targetValue = if (isExpanded) 0.dp else 24.dp,
        animationSpec = springSpec,
        label = "IconSize"
    )

    // Dynamic horizontal button offset matching filled NavBar width
    val buttonOffsetX by animateDpAsState(
        targetValue = if (isExpanded) 0.dp else screenWidth - 64.dp,
        animationSpec = springSpec,
        label = "BtnX"
    )

    // Visualization colors
    val trackColor = baseColor.copy(alpha = 0.15f * animatedAlpha)
    val indicatorColor = baseColor.copy(alpha = animatedAlpha)

    // Smooth visibility wrapper
    AnimatedVisibility(
        visible = !isExpanded,
        enter = fadeIn(tween(300)),
        exit = fadeOut(tween(250))
    ) {
        // Main container with top-only rounding for docking
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp)
                .clip(RoundedCornerShape(topStart = 40.dp, topEnd = 40.dp))
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.95f))
                .border(
                    width = 0.5.dp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                    shape = RoundedCornerShape(topStart = 40.dp, topEnd = 40.dp)
                )
                .graphicsLayer {
                    compositingStrategy = CompositingStrategy.Offscreen
                    shadowElevation = 0f
                    ambientShadowColor = Color.Transparent
                    spotShadowColor = Color.Transparent
                },
            color = Color.Transparent,
            tonalElevation = 0.dp,
            shadowElevation = 0.dp
        ) {
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .padding(vertical = 4.dp)
            ) {
                // Mini Album Art + Circular Progress
                Box(
                    modifier = Modifier
                        .padding(start = 16.dp)
                        .size(44.dp)
                        .align(Alignment.CenterStart),
                    contentAlignment = Alignment.Center
                ) {
                    // Progress visualization
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawCircle(trackColor, style = Stroke(width = 2.dp.toPx()))
                        drawArc(
                            color = indicatorColor,
                            startAngle = -90f,
                            sweepAngle = 360f * 0.3f,
                            useCenter = false,
                            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }

                    // Inner album cover placeholder
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f * animatedAlpha),
                                shape = CircleShape
                            )
                    )
                }

                // Track information with fade-out edge
                val infoWidth = (buttonOffsetX - textOffsetX - 16.dp).coerceAtLeast(0.dp)
                Column(
                    modifier = Modifier
                        .offset(textOffsetX, 12.dp)
                        .widthIn(max = infoWidth)
                        .drawWithContent {
                            drawContent()
                            drawRect(
                                brush = Brush.horizontalGradient(
                                    0.85f to Color.Black,
                                    1f to Color.Transparent
                                ),
                                blendMode = BlendMode.DstIn
                            )
                        },
                    horizontalAlignment = Alignment.Start
                ) {
                    // Song title
                    Text(
                        text = songTitle,
                        color = baseColor.copy(alpha = animatedAlpha),
                        fontSize = titleFontSize.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = Poppins,
                        maxLines = 1,
                        modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE),
                        textAlign = TextAlign.Start
                    )
                    // Artist name
                    Text(
                        text = displayArtists,
                        fontSize = 11.sp,
                        color = baseColor.copy(alpha = 0.6f * animatedAlpha),
                        fontWeight = FontWeight.Bold,
                        fontFamily = Poppins,
                        maxLines = 1,
                        modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE),
                        textAlign = TextAlign.Start
                    )
                }

                // Quick play control
                Box(
                    modifier = Modifier
                        .offset(buttonOffsetX, 12.dp)
                        .size(buttonSize)
                        .background(
                            color = buttonBgColor,
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    MorphingLoadingIcon(
                        size = iconSize,
                        color = buttonTint,
                        strokeWidth = iconSize * 0.09f,
                        isLoading = isLoading
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = stringResource(R.string.cd_play_pause),
                            tint = buttonTint,
                            modifier = Modifier.size(iconSize)
                        )
                    }
                }
            }
        }
    }
}

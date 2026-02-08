package com.lonewolf.wavvy.ui.player.components

// Compose animation mechanics
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
// Foundation and layout
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
// Material 3 and icons
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
// State and UI tools
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
// Project resources
import com.lonewolf.wavvy.R
import com.lonewolf.wavvy.ui.theme.Poppins

// Collapsed mini-player view with smooth transitions
@Composable
fun MiniPlayerContent(
    isExpanded: Boolean,
    songTitle: String,
    artistName: String,
    screenWidth: Dp,
    springSpec: AnimationSpec<Dp>,
    modifier: Modifier = Modifier,
    progress: Float = 0.3f
) {
    // Text transformation animations
    val textOffsetX by animateDpAsState(
        targetValue = if (isExpanded) 0.dp else 76.dp,
        animationSpec = springSpec,
        label = "TextX"
    )
    val titleFontSize by animateFloatAsState(
        targetValue = if (isExpanded) 0f else 14f,
        animationSpec = tween(300),
        label = "FontSize"
    )

    // Play button animations
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
    val buttonOffsetX by animateDpAsState(
        targetValue = if (isExpanded) 0.dp else (screenWidth * 0.92f) - 52.dp,
        animationSpec = springSpec,
        label = "BtnX"
    )

    // Circular progress colors
    val trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f)
    val indicatorColor = MaterialTheme.colorScheme.tertiary

    // Visibility transition
    AnimatedVisibility(
        visible = !isExpanded,
        enter = fadeIn(tween(300)),
        exit = fadeOut(tween(250))
    ) {
        Box(modifier = modifier.fillMaxSize()) {

            // Mini Album Art + Circular Progress
            Box(
                modifier = Modifier
                    .padding(start = 12.dp)
                    .size(48.dp)
                    .align(Alignment.CenterStart),
                contentAlignment = Alignment.Center
            ) {
                // Progress visualization
                Canvas(modifier = Modifier.fillMaxSize()) {
                    // Outer circle (The one that was disappearing)
                    drawCircle(
                        color = trackColor,
                        style = Stroke(width = 2.dp.toPx())
                    )
                    // Active progress arc
                    drawArc(
                        color = indicatorColor,
                        startAngle = -90f,
                        sweepAngle = 360f * progress,
                        useCenter = false,
                        style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                    )
                }

                // Inner album cover placeholder
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = CircleShape
                        )
                )
            }

            // Track information
            Column(
                modifier = Modifier
                    .offset(textOffsetX, 12.dp)
                    .width(200.dp),
                horizontalAlignment = Alignment.Start
            ) {
                // Song title
                Text(
                    text = songTitle,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = titleFontSize.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = Poppins,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Start
                )
                // Artist name
                Text(
                    text = artistName,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.tertiary,
                    fontWeight = FontWeight.Bold,
                    fontFamily = Poppins,
                    textAlign = TextAlign.Start
                )
            }

            // Quick play control
            Box(
                modifier = Modifier
                    .offset(buttonOffsetX, 12.dp)
                    .size(buttonSize)
                    .background(
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = stringResource(R.string.cd_play_pause),
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(iconSize)
                )
            }
        }
    }
}

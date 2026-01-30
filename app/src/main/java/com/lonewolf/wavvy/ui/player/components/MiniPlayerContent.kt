package com.lonewolf.wavvy.ui.player.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lonewolf.wavvy.R
import com.lonewolf.wavvy.ui.theme.Poppins

@Composable
fun MiniPlayerContent(
    isExpanded: Boolean,
    songTitle: String,
    artistName: String,
    screenWidth: Dp,
    springSpec: AnimationSpec<Dp>,
    modifier: Modifier = Modifier
) {
    // Animações do texto
    val textOffsetX by animateDpAsState(
        targetValue = if (isExpanded) 0.dp else 76.dp,
        animationSpec = springSpec
    )
    val textOffsetY by animateDpAsState(
        targetValue = 12.dp,
        animationSpec = springSpec
    )
    val titleFontSize by animateFloatAsState(
        targetValue = if (isExpanded) 0f else 14f,
        animationSpec = tween(durationMillis = 300)
    )

    // Animações do botão
    val buttonSize by animateDpAsState(
        targetValue = if (isExpanded) 0.dp else 40.dp,
        animationSpec = springSpec
    )
    val iconSize by animateDpAsState(
        targetValue = if (isExpanded) 0.dp else 24.dp,
        animationSpec = springSpec
    )
    val buttonOffsetX by animateDpAsState(
        targetValue = if (isExpanded) 0.dp else (screenWidth * 0.92f) - 52.dp,
        animationSpec = springSpec
    )
    val buttonOffsetY by animateDpAsState(
        targetValue = 12.dp,
        animationSpec = springSpec
    )

    val isDark = isSystemInDarkTheme()

    AnimatedVisibility(
        visible = !isExpanded,
        enter = fadeIn(animationSpec = tween(300)),
        exit = fadeOut(animationSpec = tween(250))
    ) {
        Box(modifier = modifier.fillMaxSize()) {
            // Título e artista
            Column(
                modifier = Modifier
                    .offset(textOffsetX, textOffsetY)
                    .width(200.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    songTitle,
                    color = if (isDark) Color(0xFFF8F9FA) else Color(0xFF0E0E16),
                    fontSize = titleFontSize.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = Poppins,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Start,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    artistName,
                    fontSize = 11.sp,
                    color = if (isDark) Color(0xFF00E5FF) else Color(0xFF00838F),
                    fontWeight = FontWeight.Bold,
                    fontFamily = Poppins,
                    textAlign = TextAlign.Start,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Ícone de play
            Box(
                modifier = Modifier
                    .offset(buttonOffsetX, buttonOffsetY)
                    .size(buttonSize)
                    .background(
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                        CircleShape
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

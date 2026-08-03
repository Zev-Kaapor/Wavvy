package com.wavvy.app.features.player.ui

// Compose animation
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
// Compose foundation and layouts
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
// Material 3 components
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
// UI utilities and graphics
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
// Project components and theme
import com.wavvy.app.core.designsystem.theme.Poppins
import com.wavvy.app.features.player.ui.components.LyricsAlignment
import com.wavvy.app.features.player.ui.components.LyricsView

// Player lyrics overlay layout
@Composable
fun PlayerLyricsOverlay(
    isLyricsActive: Boolean,
    progress: Float,
    isLandscape: Boolean,
    currentProgress: Float,
    trackDuration: Long,
    songTitle: String,
    cleanArtistName: String,
    onSeek: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    AnimatedVisibility(
        visible = isLyricsActive && progress >= 0.8f,
        enter = fadeIn(tween(600)),
        exit = fadeOut(tween(600))
    ) {
        val lyricsModifier = if (isLandscape) {
            Modifier
                .fillMaxSize()
                .padding(bottom = 100.dp)
        } else {
            Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .padding(top = 40.dp, bottom = 320.dp)
        }

        Box(
            modifier = lyricsModifier
                .pointerInput(Unit) {
                    detectDragGestures { change, _ -> change.consume() }
                }
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                )
        ) {
            Box(modifier = Modifier.padding(top = 80.dp)) {
                LyricsView(
                    lyrics = null,
                    translation = null,
                    isSynced = true,
                    currentPosition = (currentProgress * trackDuration).toLong(),
                    onSeek = onSeek,
                    alignment = LyricsAlignment.CENTER,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 64.dp)
                    .padding(top = if (isLandscape) 20.dp else 0.dp)
                    .align(Alignment.TopCenter),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = songTitle,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontFamily = Poppins,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    ),
                    color = Color.White,
                    maxLines = 1,
                    modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE)
                )
                Text(
                    text = cleanArtistName,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = Poppins,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp
                    ),
                    color = Color.White.copy(alpha = 0.7f),
                    maxLines = 1,
                    modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE)
                )
            }
        }
    }
}

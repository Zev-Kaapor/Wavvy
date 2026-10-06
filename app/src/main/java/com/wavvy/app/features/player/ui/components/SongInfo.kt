package com.wavvy.app.features.player.ui.components

// Compose layouts and foundations
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
// Material 3 components
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
// Compose state and runtime
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
// UI styling and utilities
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp as lerpColor
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.lerp
// Math
import kotlin.math.roundToInt
// Project resources
import com.wavvy.app.core.designsystem.icons.WavvyIcons
import com.wavvy.app.core.designsystem.theme.WavvyTheme

// Where the title and the artist scale from, the start of their line
private val StartOrigin = TransformOrigin(0f, 0.5f)

// Title and artist of the song, small in the pill and growing into the open player with a shadow and the cyan artist, as in the old Wavvy
// Places, sizes and colors follow the opening while measuring and drawing, the shadow and the weight change in a few steps
@Composable
fun SongInfo(
    title: String,
    artist: String,
    progress: () -> Float,
    screenWidth: Dp,
    isLandscape: Boolean,
    modifier: Modifier = Modifier
) {
    val colors = WavvyTheme.colors
    val onSurface = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant

    // The opening in a few steps, for what cannot change on every frame
    val step by remember(progress) { derivedStateOf { (progress() * PlayerDimens.TextSteps).roundToInt() / PlayerDimens.TextSteps.toFloat() } }
    val textShadow = Shadow(
        color = colors.textShadow.copy(alpha = PlayerDimens.TextShadowAlpha * step),
        offset = Offset.Zero,
        blurRadius = PlayerDimens.TextShadowBlur * step
    )

    val isSmallScreen = screenWidth < PlayerDimens.SmallScreenWidth
    val titleSize = if (isSmallScreen && !isLandscape) PlayerDimens.TitleSizeSmall else PlayerDimens.TitleSize
    val artistSize = if (isSmallScreen || isLandscape) PlayerDimens.ArtistSizeSmall else PlayerDimens.ArtistSize

    // Laid out at the size of the open player and scaled down in the pill
    val titleMini = PlayerDimens.MiniTitleSize / titleSize.value
    val artistMini = PlayerDimens.MiniArtistSize / artistSize.value
    val spacingStart = if (isLandscape) PlayerDimens.InfoSpacingLandscape else PlayerDimens.InfoSpacing
    val spacingEnd = if (isLandscape) PlayerDimens.InfoSpacingLandscapeExpanded else PlayerDimens.InfoSpacingExpanded
    val lineWidth = if (isLandscape) 1f else PlayerDimens.InfoWidthFraction

    // The player keeps its look whatever the system font size is
    CompositionLocalProvider(LocalDensity provides Density(density = LocalDensity.current.density, fontScale = 1f)) {
        Column(modifier = modifier, horizontalAlignment = Alignment.Start) {
            BasicText(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontSize = titleSize, shadow = textShadow),
                color = { lerpColor(onSurface, colors.onMedia, progress()) },
                maxLines = 1,
                modifier = Modifier
                    .fillMaxWidth(lineWidth)
                    // Room above the title grows while opening
                    .layout { measurable, constraints ->
                        val top = (PlayerDimens.TitleTopExpanded * progress()).roundToPx()
                        val placeable = measurable.measure(constraints)
                        layout(placeable.width, placeable.height + top) { placeable.place(0, top) }
                    }
                    .marquee()
                    .graphicsLayer {
                        val scale = titleMini + progress() * (1f - titleMini)
                        scaleX = scale
                        scaleY = scale
                        transformOrigin = StartOrigin
                    }
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth(lineWidth)
                    // Space between the title and the artist, tight in the pill and open in the player
                    .layout { measurable, constraints ->
                        val spacing = lerp(spacingStart, spacingEnd, progress()).roundToPx()
                        val placeable = measurable.measure(constraints)
                        layout(placeable.width, (placeable.height + spacing).coerceAtLeast(0)) { placeable.place(0, spacing) }
                    }
            ) {
                Icon(
                    imageVector = WavvyIcons.Person,
                    contentDescription = null,
                    tint = lerpColor(onSurfaceVariant, colors.playerAccent, step),
                    modifier = Modifier
                        .layout { measurable, _ ->
                            val side = lerp(PlayerDimens.ArtistIcon, PlayerDimens.ArtistIconExpanded, progress()).roundToPx()
                            val placeable = measurable.measure(Constraints.fixed(side, side))
                            layout(side, side) { placeable.place(0, 0) }
                        }
                        .graphicsLayer {
                            val opening = progress()
                            alpha = if (opening < PlayerDimens.CollapsedThreshold) PlayerDimens.ArtistIconAlpha else opening
                        }
                        .padding(end = PlayerDimens.ArtistIconEnd)
                )

                BasicText(
                    text = artist,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = artistSize,
                        fontWeight = if (step > PlayerDimens.ArtistBoldStart) FontWeight.SemiBold else FontWeight.Medium,
                        shadow = textShadow
                    ),
                    color = { lerpColor(onSurfaceVariant, colors.playerAccent, progress()) },
                    maxLines = 1,
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .marquee()
                        .graphicsLayer {
                            val scale = artistMini + progress() * (1f - artistMini)
                            scaleX = scale
                            scaleY = scale
                            transformOrigin = StartOrigin
                        }
                )
            }
        }
    }
}

// A name that does not fit slides once, after a wait
private fun Modifier.marquee(): Modifier =
    basicMarquee(
        iterations = PlayerDimens.MarqueeIterations,
        initialDelayMillis = PlayerDimens.MarqueeDelayMillis,
        velocity = PlayerDimens.MarqueeVelocity
    )

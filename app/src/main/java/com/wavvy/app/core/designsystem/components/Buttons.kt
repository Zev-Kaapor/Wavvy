package com.wavvy.app.core.designsystem.components

// Compose layouts and foundations
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
// Material 3 components
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
// Compose state and runtime
import androidx.compose.runtime.Composable
// UI styling and utilities
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
// Project resources
import com.wavvy.app.core.designsystem.theme.WavvyTheme

// Main pill button filled with a blue that fades to a darker blue
@Composable
fun GradientButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leading: (@Composable () -> Unit)? = null
) {
    val colors = WavvyTheme.colors
    val brush = Brush.horizontalGradient(listOf(colors.buttonStart, colors.buttonEnd))

    PillButton(
        text = text,
        textColor = colors.onAccentFill,
        onClick = onClick,
        modifier = modifier.background(brush, CircleShape),
        leading = leading
    )
}

// Secondary pill button on a quiet surface
@Composable
fun QuietButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leading: (@Composable () -> Unit)? = null
) {
    PillButton(
        text = text,
        textColor = MaterialTheme.colorScheme.onBackground,
        onClick = onClick,
        modifier = modifier.background(WavvyTheme.colors.elevated, CircleShape),
        leading = leading
    )
}

// Pill with a centered label, an optional icon at the start and no touch ripple, the caller gives the background
@Composable
private fun PillButton(
    text: String,
    textColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leading: (@Composable () -> Unit)? = null
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(WavvyTheme.dimens.buttonHeight)
            .clip(CircleShape)
            .clickableNoIndication(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        // Optional icon at the start, the label stays centered
        if (leading != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = WavvyTheme.dimens.spaceSmall)
            ) {
                leading()
            }
        }

        Text(
            text = text,
            style = MaterialTheme.typography.titleSmall,
            color = textColor,
            textAlign = TextAlign.Center
        )
    }
}

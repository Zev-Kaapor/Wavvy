package com.wavvy.app.core.designsystem.components

// Material 3 components
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
// Compose state and lifecycle hooks
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
// UI styling and utilities
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
// Project resources
import com.wavvy.app.core.designsystem.theme.Poppins

// Content section title text
@Composable
fun SectionTitle(
    text: String,
    modifier: Modifier = Modifier
) {
    val typography = MaterialTheme.typography
    val color = MaterialTheme.colorScheme.onBackground

    val sectionStyle = remember(typography) {
        typography.titleLarge.copy(
            fontFamily = Poppins,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 20.sp,
            letterSpacing = (-0.5).sp
        )
    }

    Text(
        text = text,
        style = sectionStyle,
        color = color,
        modifier = modifier.padding(horizontal = 16.dp, vertical = 12.dp)
    )
}

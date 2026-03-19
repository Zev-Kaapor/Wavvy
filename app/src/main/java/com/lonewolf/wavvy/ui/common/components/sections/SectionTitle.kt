package com.lonewolf.wavvy.ui.common.components.sections

// Material 3 and layout
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
// Project resources
import com.lonewolf.wavvy.ui.theme.Poppins

// Standard header for content sections
@Composable
fun SectionTitle(
    text: String,
    modifier: Modifier = Modifier
) {
    // Access typography context
    val typography = MaterialTheme.typography
    val color = MaterialTheme.colorScheme.onBackground

    // Memoize style using the current typography as key
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

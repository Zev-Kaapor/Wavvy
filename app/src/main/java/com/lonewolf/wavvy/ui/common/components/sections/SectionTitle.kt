package com.lonewolf.wavvy.ui.common.components.sections

// Material 3 and layout foundations
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
// Project resources
import com.lonewolf.wavvy.ui.theme.Poppins

// Standard section headline
@Composable
fun SectionTitle(
    text: String,
    modifier: Modifier = Modifier
) {
    // Section title
    Text(
        text = text,
        style = MaterialTheme.typography.titleLarge.copy(
            fontFamily = Poppins,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 20.sp,
            letterSpacing = (-0.5).sp
        ),
        color = MaterialTheme.colorScheme.onBackground,
        modifier = modifier.padding(horizontal = 16.dp, vertical = 12.dp)
    )
}

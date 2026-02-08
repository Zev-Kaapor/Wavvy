package com.lonewolf.wavvy.ui.home.components

// UI framework and layout
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
// Material 3 components
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
// State management
import androidx.compose.runtime.*
// UI utilities
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
// Project resources
import com.lonewolf.wavvy.R
import com.lonewolf.wavvy.ui.theme.Poppins

// Horizontal filter selection pills
@Composable
fun FilterPills(
    modifier: Modifier = Modifier,
    onFilterSelected: (Int) -> Unit = {}
) {
    // Filter options
    val filters = listOf(
        R.string.filter_focus,
        R.string.filter_workout,
        R.string.filter_car,
        R.string.filter_relax,
        R.string.filter_party,
        R.string.filter_travel
    )

    // Selection state
    var selectedFilterResId by remember { mutableIntStateOf(0) }

    // Horizontal filter list
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(
            items = filters,
            key = { it },
            contentType = { "filter_pill" }
        ) { filterResId ->
            val isSelected = selectedFilterResId == filterResId

            // Filter pill item
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        if (isSelected)
                            MaterialTheme.colorScheme.tertiary
                        else
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                    )
                    .clickable {
                        selectedFilterResId = if (isSelected) 0 else filterResId
                        onFilterSelected(selectedFilterResId)
                    }
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            ) {
                // Pill label
                Text(
                    text = stringResource(filterResId),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = Poppins,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    ),
                    color = if (isSelected)
                        MaterialTheme.colorScheme.onTertiary
                    else
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                )
            }
        }
    }
}

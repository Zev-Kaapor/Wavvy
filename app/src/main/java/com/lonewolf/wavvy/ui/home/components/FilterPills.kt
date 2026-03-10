package com.lonewolf.wavvy.ui.home.components

// UI framework and layout
import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
// Project resources
import com.lonewolf.wavvy.R
import com.lonewolf.wavvy.ui.theme.Poppins
import com.lonewolf.wavvy.ui.theme.accentCyan

// Horizontal filter selection pills with persistent state in ViewModel
@SuppressLint("LocalContextResourcesRead")
@Composable
fun FilterPills(
    availableFilters: List<String>,
    selectedFilter: String,
    onFilterSelected: (String) -> Unit,
    onInitializeFilters: (List<String>) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isDark = isSystemInDarkTheme()

    // Initialize filters once if they are empty
    LaunchedEffect(Unit) {
        if (availableFilters.isEmpty()) {
            val newFilters = context.resources.getStringArray(R.array.filter_moods)
                .toList()
                .shuffled()
                .take(5)
            onInitializeFilters(newFilters)
        }
    }

    // Horizontal filter list
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(
            items = availableFilters,
            key = { it },
            contentType = { "filter_pill" }
        ) { filterText ->
            val isSelected = selectedFilter == filterText

            // Unified color logic
            val containerColor = if (isSelected) {
                if (isDark) MaterialTheme.accentCyan else Color.Black
            } else {
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
            }

            val contentColor = if (isSelected) {
                if (isDark) Color.Black else Color.White
            } else {
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            }

            // Filter pill item
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(containerColor)
                    .clickable {
                        onFilterSelected(if (isSelected) "" else filterText)
                    }
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                // Pill label
                Text(
                    text = filterText,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = Poppins,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    ),
                    color = contentColor
                )
            }
        }
    }
}

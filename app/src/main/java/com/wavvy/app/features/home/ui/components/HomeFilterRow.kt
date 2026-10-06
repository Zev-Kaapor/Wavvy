package com.wavvy.app.features.home.ui.components

// Compose layouts and foundations
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
// Material 3 components
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
// Compose state and runtime
import androidx.compose.runtime.Composable
// UI utilities
import androidx.compose.ui.Modifier
// Project resources
import com.wavvy.app.features.home.data.HomeFilter

// Filters of the top of the Home as chips, the same row Metrolist (GPL-3.0) draws
@Composable
fun HomeFilterRow(
    filters: List<HomeFilter>,
    selected: HomeFilter?,
    onSelect: (HomeFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Horizontal))
    ) {
        Spacer(modifier = Modifier.width(HomeDimens.FilterStart))

        filters.forEach { filter ->
            FilterChip(
                label = { Text(text = filter.title, style = MaterialTheme.typography.bodyMedium.merge(HomeType.Filter)) },
                selected = filter == selected,
                colors = FilterChipDefaults.filterChipColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                onClick = { onSelect(filter) },
                shape = RoundedCornerShape(HomeDimens.FilterCorner),
                border = null
            )

            Spacer(modifier = Modifier.width(HomeDimens.FilterSpacing))
        }
    }
}

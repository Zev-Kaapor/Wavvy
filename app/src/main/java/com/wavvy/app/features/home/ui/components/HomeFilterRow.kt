package com.wavvy.app.features.home.ui.components

// Compose layouts and foundations
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
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
import com.wavvy.app.core.designsystem.theme.WavvyTheme
import com.wavvy.app.features.home.data.HomeFilter
import com.wavvy.app.features.library.ui.FilterChips

// Filters of the top of the Home as chips, the same row Metrolist (GPL-3.0) draws, the one that is on goes to the start, and a tap on it turns it off
@Composable
fun HomeFilterRow(
    filters: List<HomeFilter>,
    selected: HomeFilter?,
    onSelect: (HomeFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    // A faint veil lets the glow of the top show through, and the selected one is solid like the buttons of the library
    val onBackground = MaterialTheme.colorScheme.onBackground
    val colors = FilterChipDefaults.filterChipColors(
        containerColor = WavvyTheme.colors.filterChip,
        labelColor = onBackground,
        selectedContainerColor = onBackground,
        selectedLabelColor = MaterialTheme.colorScheme.background
    )

    FilterChips(
        options = filters,
        selected = selected,
        id = { it.title },
        gap = HomeDimens.FilterSpacing,
        onClear = { selected?.let(onSelect) },
        hasClear = false,
        contentPadding = PaddingValues(start = HomeDimens.FilterStart),
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Horizontal))
    ) { filter, isOn ->
        FilterChip(
            label = { Text(text = filter.title, style = MaterialTheme.typography.bodyMedium.merge(HomeType.Filter)) },
            selected = isOn,
            colors = colors,
            onClick = { onSelect(filter) },
            shape = RoundedCornerShape(HomeDimens.FilterCorner),
            border = null
        )
    }
}

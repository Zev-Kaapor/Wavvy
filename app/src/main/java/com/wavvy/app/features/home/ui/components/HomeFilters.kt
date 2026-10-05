package com.wavvy.app.features.home.ui.components

// Compose layouts and foundations
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
// Material 3 components
import androidx.compose.material3.MaterialTheme
// Compose state and runtime
import androidx.compose.runtime.Composable
// UI utilities
import androidx.compose.ui.Modifier
// Project resources
import com.wavvy.app.core.designsystem.components.skeleton
import com.wavvy.app.core.designsystem.theme.WavvyTheme

// Filter row shown while the filters do not arrive, empty pills that pulse
@Composable
fun HomeFiltersPlaceholder(modifier: Modifier = Modifier) {
    val dimens = WavvyTheme.dimens

    LazyRow(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)),
        contentPadding = PaddingValues(horizontal = dimens.screenPadding),
        horizontalArrangement = Arrangement.spacedBy(dimens.spaceSmall),
        userScrollEnabled = false
    ) {
        items(HomeDimens.FilterPlaceholderWidths) { width ->
            Box(
                modifier = Modifier
                    .size(width = width, height = HomeDimens.FilterHeight)
                    .skeleton(MaterialTheme.shapes.medium)
            )
        }
    }
}

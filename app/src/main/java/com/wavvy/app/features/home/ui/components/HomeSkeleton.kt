package com.wavvy.app.features.home.ui.components

// Compose layouts and foundations
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
// Material 3 components
import androidx.compose.material3.MaterialTheme
// Compose state and runtime
import androidx.compose.runtime.Composable
// UI utilities
import androidx.compose.ui.Modifier
// Project resources
import com.wavvy.app.core.designsystem.components.SkeletonHost
import com.wavvy.app.core.designsystem.components.skeleton
import com.wavvy.app.core.designsystem.theme.WavvyTheme

// Placeholder of the Home while nothing has arrived, the filters and a few shelves of covers in the sizes of the real ones
@Composable
fun HomeSkeleton(
    modifier: Modifier = Modifier,
    showFilters: Boolean = true
) {
    val dimens = WavvyTheme.dimens

    SkeletonHost(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Horizontal))
    ) {
        Column {
            // Filters, left out when the real ones are already on the screen
            if (showFilters) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = HomeDimens.FilterStart, vertical = dimens.spaceSmall),
                    horizontalArrangement = Arrangement.spacedBy(HomeDimens.FilterSpacing),
                    userScrollEnabled = false
                ) {
                    items(HomeDimens.FilterPlaceholderWidths) { width ->
                        Box(
                            modifier = Modifier
                                .size(width = width, height = HomeDimens.FilterHeight)
                                .skeleton(RoundedCornerShape(HomeDimens.FilterCorner))
                        )
                    }
                }
            }

            repeat(HomeDimens.SkeletonShelves) {
                // Shelf title
                Box(
                    modifier = Modifier
                        .padding(HomeDimens.TitlePadding)
                        .size(width = HomeDimens.ShelfTitleWidth, height = HomeDimens.ShelfTitleHeight)
                        .skeleton(MaterialTheme.shapes.extraSmall)
                )

                // Covers with two lines of text under each
                LazyRow(userScrollEnabled = false) {
                    items(HomeDimens.SkeletonCovers) {
                        CoverSkeleton()
                    }
                }
            }
        }
    }
}

// Square cover with a title line and a shorter line under it
@Composable
private fun CoverSkeleton(modifier: Modifier = Modifier) {
    val dimens = WavvyTheme.dimens

    Column(
        modifier = modifier
            .padding(HomeDimens.GridPadding)
            .width(HomeDimens.GridCover)
    ) {
        Box(
            modifier = Modifier
                .size(HomeDimens.GridCover)
                .skeleton(RoundedCornerShape(HomeDimens.CoverCorner))
        )

        Spacer(modifier = Modifier.height(HomeDimens.GridTextGap))

        Box(
            modifier = Modifier
                .fillMaxWidth(HomeDimens.CoverTitleFraction)
                .height(HomeDimens.CoverLineHeight)
                .skeleton(MaterialTheme.shapes.extraSmall)
        )

        Spacer(modifier = Modifier.height(dimens.spaceExtraSmall))

        Box(
            modifier = Modifier
                .fillMaxWidth(HomeDimens.CoverSubtitleFraction)
                .height(HomeDimens.CoverLineHeight)
                .skeleton(MaterialTheme.shapes.extraSmall)
        )
    }
}

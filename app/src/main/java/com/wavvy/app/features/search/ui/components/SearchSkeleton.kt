package com.wavvy.app.features.search.ui.components

// Compose layouts and foundations
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
// Material 3 components
import androidx.compose.material3.MaterialTheme
// Compose state and runtime
import androidx.compose.runtime.Composable
// UI styling and utilities
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
// Project resources
import com.wavvy.app.core.designsystem.components.SkeletonHost
import com.wavvy.app.core.designsystem.components.skeleton
import com.wavvy.app.core.designsystem.theme.WavvyTheme
import com.wavvy.app.features.home.ui.components.HomeDimens
import com.wavvy.app.features.search.ui.SearchDimens

// Placeholder of the results, rows with a cover and two lines of text in the size of the real ones
@Composable
fun SearchSkeleton(modifier: Modifier = Modifier) {
    val dimens = WavvyTheme.dimens

    SkeletonHost(modifier = modifier.fillMaxWidth()) {
        repeat(SearchDimens.SkeletonRows) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .height(HomeDimens.ListHeight)
                    .padding(horizontal = HomeDimens.ListPadding)
                    .padding(horizontal = dimens.spaceSmall)
            ) {
                Box(
                    modifier = Modifier
                        .padding(HomeDimens.ListCoverPadding)
                        .size(HomeDimens.ListCover)
                        .skeleton(RoundedCornerShape(HomeDimens.CoverCorner))
                )

                Column(modifier = Modifier.padding(horizontal = HomeDimens.ListTextPadding)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(SearchDimens.SkeletonTitleFraction)
                            .height(SearchDimens.SkeletonLine)
                            .skeleton(MaterialTheme.shapes.extraSmall)
                    )

                    Spacer(modifier = Modifier.height(dimens.spaceExtraSmall))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth(SearchDimens.SkeletonSubtitleFraction)
                            .height(SearchDimens.SkeletonLine)
                            .skeleton(MaterialTheme.shapes.extraSmall)
                    )
                }
            }
        }
    }
}

package com.wavvy.app.features.discover.ui

// Compose layouts and foundations
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
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
import com.wavvy.app.features.home.ui.components.HomeDimens

// Placeholder of the Explore tab while nothing has arrived, the big buttons, a row of covers, the moods and a row of videos in the places of the real ones
@Composable
fun DiscoverSkeleton(modifier: Modifier = Modifier) {
    SkeletonHost(modifier = modifier.fillMaxWidth()) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val side = WindowInsets.systemBars.only(WindowInsetsSides.Horizontal).asPaddingValues()
            val cover = coverWidth(maxWidth)

            Column(modifier = Modifier.padding(side)) {
                // Big buttons, two by two
                Column(
                    verticalArrangement = Arrangement.spacedBy(DiscoverDimens.Gap),
                    modifier = Modifier.padding(horizontal = DiscoverDimens.Side, vertical = WavvyTheme.dimens.spaceSmall)
                ) {
                    repeat(ShortcutRows) {
                        Row(horizontalArrangement = Arrangement.spacedBy(DiscoverDimens.Gap)) {
                            repeat(ShortcutColumns) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(DiscoverDimens.ShortcutHeight)
                                        .skeleton(RoundedCornerShape(DiscoverDimens.ShortcutCorner))
                                )
                            }
                        }
                    }
                }

                // Covers with two lines under each
                TitleSkeleton()
                Row(horizontalArrangement = Arrangement.spacedBy(DiscoverDimens.Gap), modifier = Modifier.padding(horizontal = DiscoverDimens.Side)) {
                    repeat(DiscoverDimens.CoverColumns + 1) {
                        Column(modifier = Modifier.width(cover)) {
                            Box(
                                modifier = Modifier
                                    .size(cover)
                                    .skeleton(RoundedCornerShape(DiscoverDimens.CoverCorner))
                            )
                            Spacer(modifier = Modifier.height(DiscoverDimens.CardTextGap))
                            LineSkeleton(HomeDimens.CoverTitleFraction)
                            Spacer(modifier = Modifier.height(WavvyTheme.dimens.spaceExtraSmall))
                            LineSkeleton(HomeDimens.CoverSubtitleFraction)
                        }
                    }
                }

                // Moods and genres in rows of two
                TitleSkeleton()
                Column(
                    verticalArrangement = Arrangement.spacedBy(DiscoverDimens.Gap),
                    modifier = Modifier.padding(horizontal = DiscoverDimens.Side)
                ) {
                    repeat(DiscoverDimens.MoodRows) {
                        Row(horizontalArrangement = Arrangement.spacedBy(DiscoverDimens.Gap)) {
                            repeat(DiscoverDimens.CoverColumns) {
                                Box(
                                    modifier = Modifier
                                        .width(cover)
                                        .height(DiscoverDimens.MoodHeight)
                                        .skeleton(RoundedCornerShape(DiscoverDimens.MoodCorner))
                                )
                            }
                        }
                    }
                }

                // Wide card of a video
                TitleSkeleton()
                Box(
                    modifier = Modifier
                        .padding(horizontal = DiscoverDimens.Side)
                        .fillMaxWidth()
                        .aspectRatio(WideAspectRatio)
                        .skeleton(RoundedCornerShape(DiscoverDimens.WideCorner))
                )
            }
        }
    }
}

// Title of a shelf
@Composable
private fun TitleSkeleton() {
    Box(
        modifier = Modifier
            .padding(horizontal = DiscoverDimens.Side, vertical = DiscoverDimens.TitleVertical)
            .size(width = HomeDimens.ShelfTitleWidth, height = HomeDimens.ShelfTitleHeight)
            .skeleton(MaterialTheme.shapes.extraSmall)
    )
}

// A line of text under a card, a fraction of the width of the card
@Composable
private fun LineSkeleton(fraction: Float) {
    Box(
        modifier = Modifier
            .fillMaxWidth(fraction)
            .height(HomeDimens.CoverLineHeight)
            .skeleton(MaterialTheme.shapes.extraSmall)
    )
}

// Rows of big buttons
private const val ShortcutRows = 2

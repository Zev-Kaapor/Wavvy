package com.wavvy.app.features.radio.ui

// Compose layouts and foundations
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
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
import com.wavvy.app.features.discover.ui.DiscoverDimens

// The room of a list of stations while it is asked, the line with how many there are and the rows as they are drawn, the logo, the name, the place, what is on air and the place of the menu
@Composable
internal fun StationsSkeleton(modifier: Modifier = Modifier) {
    val dimens = WavvyTheme.dimens
    val shape = MaterialTheme.shapes.extraSmall

    SkeletonHost(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .padding(horizontal = dimens.screenPadding, vertical = RadiosDimens.CountVertical)
                .size(width = RadiosDimens.SkeletonCountWidth, height = RadiosDimens.SkeletonLineHeight)
                .skeleton(shape)
        )

        repeat(RadiosDimens.SkeletonRows) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(start = dimens.screenPadding, top = RadiosDimens.RowVertical, bottom = RadiosDimens.RowVertical)
            ) {
                Box(modifier = Modifier.size(RadiosDimens.Logo).skeleton(RoundedCornerShape(RadiosDimens.LogoCorner)))

                Column(verticalArrangement = Arrangement.spacedBy(dimens.spaceExtraSmall), modifier = Modifier.weight(1f).padding(start = dimens.spaceMedium)) {
                    Box(modifier = Modifier.fillMaxWidth(RadiosDimens.SkeletonNameFraction).height(RadiosDimens.SkeletonNameHeight).skeleton(shape))
                    Box(modifier = Modifier.fillMaxWidth(RadiosDimens.SkeletonPlaceFraction).height(RadiosDimens.SkeletonLineHeight).skeleton(shape))
                    Box(modifier = Modifier.fillMaxWidth(RadiosDimens.SkeletonOnAirFraction).height(RadiosDimens.SkeletonLineHeight).skeleton(shape))
                }

                // Where the three dots are, so the rows do not change their width when the real ones come
                Box(modifier = Modifier.padding(horizontal = RadiosDimens.SkeletonMenuGap).size(RadiosDimens.SkeletonMenu).skeleton(CircleShape))
            }
        }
    }
}

// The room of a list of places while it is asked, the wide button that opens every station, the title of a group and the buttons with a stripe in as many columns as fit
@Composable
internal fun PlacesSkeleton(modifier: Modifier = Modifier) {
    val shape = MaterialTheme.shapes.extraSmall

    SkeletonHost(modifier = modifier.fillMaxWidth()) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
                .padding(horizontal = DiscoverDimens.Side, vertical = RadiosDimens.SectionVertical)
        ) {
            val columns = ((maxWidth + DiscoverDimens.Gap) / (DiscoverDimens.MoodMinWidth + DiscoverDimens.Gap)).toInt().coerceAtLeast(2)

            Column(verticalArrangement = Arrangement.spacedBy(DiscoverDimens.Gap)) {
                Box(modifier = Modifier.fillMaxWidth().height(DiscoverDimens.MoodHeight).skeleton(RoundedCornerShape(DiscoverDimens.MoodCorner)))

                Box(modifier = Modifier.padding(top = RadiosDimens.SectionVertical).size(width = DiscoverDimens.SkeletonShelfTitleWidth, height = DiscoverDimens.SkeletonLine).skeleton(shape))

                repeat(RadiosDimens.SkeletonPlaceRows) {
                    Row(horizontalArrangement = Arrangement.spacedBy(DiscoverDimens.Gap)) {
                        repeat(columns) {
                            Box(modifier = Modifier.weight(1f).height(DiscoverDimens.MoodHeight).skeleton(RoundedCornerShape(DiscoverDimens.MoodCorner)))
                        }
                    }
                }
            }
        }
    }
}

package com.wavvy.app.core.navigation

// Compose layouts and foundations
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
// Compose state and runtime
import androidx.compose.runtime.Composable
// UI styling and utilities
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
// Project resources
import com.wavvy.app.core.designsystem.theme.WavvyTheme

// Rail on the left with only the icons of the main destinations stacked, used when the screen is wider than tall
@Composable
fun WavvyNavRail(
    selected: MainTab,
    onSelect: (MainTab) -> Unit,
    modifier: Modifier = Modifier
) {
    // The camera cutout takes the first part of the rail, so the icons never go under it
    val cutout = WindowInsets.safeDrawing.asPaddingValues().calculateStartPadding(LocalLayoutDirection.current)
    val iconsInset = maxOf(cutout - NavBarDimens.RailCameraReduction, NavBarDimens.NoInset)
    val railWidth = if (cutout > NavBarDimens.NoInset) {
        NavBarDimens.RailIconAreaWidth + iconsInset
    } else {
        NavBarDimens.RailDefaultWidth
    }

    Box(
        modifier = modifier
            .fillMaxHeight()
            .width(railWidth)
            .background(WavvyTheme.colors.navBar),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .padding(start = iconsInset)
                .width(NavBarDimens.RailIconAreaWidth),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(NavBarDimens.RailItemSpacing, Alignment.CenterVertically)
        ) {
            MainTab.entries.forEach { tab ->
                NavBarItem(
                    tab = tab,
                    selected = tab == selected,
                    onClick = { onSelect(tab) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = NavBarDimens.RailItemHeight),
                    showLabel = false
                )
            }
        }
    }
}

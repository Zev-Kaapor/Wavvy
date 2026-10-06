package com.wavvy.app.features.player.ui.components

// Compose layouts and foundations
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
// Compose state and runtime
import androidx.compose.runtime.Composable
// UI utilities
import androidx.compose.ui.unit.Dp

// Space under the toolbar of the open player, a fixed margin with gesture navigation and above the buttons otherwise
@Composable
fun toolbarBottomPadding(): Dp {
    val navigation = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    return if (navigation <= PlayerDimens.GestureNavMaxInset) PlayerDimens.ToolbarBottomGesture else navigation + PlayerDimens.ToolbarBottomInsetGap
}

// Height the toolbar takes from the bottom, with its space under it
@Composable
fun toolbarReservedHeight(): Dp = toolbarBottomPadding() + PlayerDimens.ToolbarHeight

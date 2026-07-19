package com.lonewolf.wavvy.ui.common.components

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun rememberWavvyBottomSheetState(
    skipPartiallyExpanded: Boolean = false,
    initialValue: SheetValue = SheetValue.Hidden,
): SheetState {
    val density = LocalDensity.current
    return remember(density, skipPartiallyExpanded, initialValue) {
        SheetState(
            skipPartiallyExpanded = skipPartiallyExpanded,
            positionalThreshold = { with(density) { 56.dp.toPx() } },
            velocityThreshold = { with(density) { 125.dp.toPx() } },
            initialValue = initialValue,
        )
    }
}

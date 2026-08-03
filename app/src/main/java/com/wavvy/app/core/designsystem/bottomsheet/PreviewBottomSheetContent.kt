package com.wavvy.app.core.designsystem.bottomsheet

// Compose layouts and foundations
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
// UI styling and utilities
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

// Bottom-sheet preview placeholder layout
@Composable
fun PreviewBottomSheetContent(
    onOptionsClicked: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOptionsClicked() }
            .padding(24.dp)
    )
}

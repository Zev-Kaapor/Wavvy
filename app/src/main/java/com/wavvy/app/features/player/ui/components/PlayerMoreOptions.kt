package com.wavvy.app.features.player.ui.components

// Compose foundation and layout
import android.annotation.SuppressLint
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
// Project resources
import com.wavvy.app.core.designsystem.bottomsheet.SequentialBottomSheet

@SuppressLint("ConfigurationScreenWidthHeight")
@Composable
fun PlayerMoreOptions(
    onDismiss: () -> Unit,
    content: @Composable () -> Unit
) {
    SequentialBottomSheet(onDismiss = onDismiss) {
        content()
    }
}

// Preview configuration for design system validation
@Preview(showBackground = true)
@Composable
fun PlayerMoreOptionsPreview() {
    PlayerMoreOptions(
        onDismiss = {}
    ) {
        // Preview content space
    }
}

package com.wavvy.app.features.home.ui.components

// Compose layouts and foundations
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
// Material 3 components
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
// Compose state and runtime
import androidx.compose.runtime.Composable
// UI styling and utilities
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
// Project resources
import com.wavvy.app.core.designsystem.components.GradientButton
import com.wavvy.app.core.designsystem.theme.WavvyTheme

// Short message in the middle of the Home, with a button when there is something to try again
@Composable
fun HomeMessage(
    text: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: () -> Unit = {}
) {
    val dimens = WavvyTheme.dimens

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier
                .widthIn(max = HomeDimens.MessageMaxWidth)
                .padding(horizontal = dimens.screenPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(dimens.spaceLarge)
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            if (actionLabel != null) {
                GradientButton(text = actionLabel, onClick = onAction)
            }
        }
    }
}

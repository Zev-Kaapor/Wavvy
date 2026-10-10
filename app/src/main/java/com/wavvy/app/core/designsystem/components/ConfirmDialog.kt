package com.wavvy.app.core.designsystem.components

// Compose layouts and foundations
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
// Material 3 components
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
// Compose state and runtime
import androidx.compose.runtime.Composable
// UI styling and utilities
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

// A question with two buttons of the same width side by side, the one that cancels and the one that goes on, which stands out
// It is white on the dark theme and dark on the light one, the same as the play button
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfirmDialog(
    title: String,
    text: String,
    confirmLabel: String,
    cancelLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    BasicAlertDialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(ConfirmCorner),
            color = MaterialTheme.colorScheme.surfaceContainerHigh
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(ConfirmPadding)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = ConfirmTextTop)
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(ConfirmButtonGap),
                    modifier = Modifier.fillMaxWidth().padding(top = ConfirmButtonsTop)
                ) {
                    FilledTonalButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text(text = cancelLabel, maxLines = 1)
                    }
                    Button(
                        onClick = onConfirm,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.onBackground,
                            contentColor = MaterialTheme.colorScheme.background
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(text = confirmLabel, maxLines = 1)
                    }
                }
            }
        }
    }
}

// The corners of the box, the room inside it and between its parts
private val ConfirmCorner = 28.dp
private val ConfirmPadding = 24.dp
private val ConfirmTextTop = 12.dp
private val ConfirmButtonsTop = 24.dp
private val ConfirmButtonGap = 12.dp

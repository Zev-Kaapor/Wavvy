package com.wavvy.app.features.settings.ui.components

// Compose foundation and layout
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
// Material 3 components
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
// Project resources
import com.wavvy.app.core.designsystem.theme.Poppins

// Single choice item shown inside the dialog
data class SettingsSelectionOption(
    val key: String,
    val label: String
)

// Reusable radio list dialog for settings pickers
@Composable
fun SettingsSelectionDialog(
    title: String,
    options: List<SettingsSelectionOption>,
    selectedKey: String,
    onOptionSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = title,
                fontFamily = Poppins,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        },
        text = {
            LazyColumn {
                items(options, key = { it.key }) { option ->
                    val isSelected = option.key == selectedKey
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = isSelected,
                                onClick = {
                                    onOptionSelected(option.key)
                                    onDismiss()
                                }
                            )
                            .padding(vertical = 4.dp)
                    ) {
                        RadioButton(selected = isSelected, onClick = {
                            onOptionSelected(option.key)
                            onDismiss()
                        })
                        Text(
                            text = option.label,
                            fontFamily = Poppins,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Fechar", fontFamily = Poppins)
            }
        }
    )
}

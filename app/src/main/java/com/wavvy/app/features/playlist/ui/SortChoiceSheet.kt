package com.wavvy.app.features.playlist.ui

// Compose layouts and foundations
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
// Material 3 components
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
// Compose state and runtime
import androidx.compose.runtime.Composable
// UI styling and utilities
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
// Project resources
import com.wavvy.app.core.designsystem.components.LocalSheetClose
import com.wavvy.app.core.designsystem.components.WavvySheet
import com.wavvy.app.core.designsystem.icons.WavvyIcons
import com.wavvy.app.features.menu.ItemMenuDimens

// One way to put the songs in order, the words, if it is the one in use and what it does when it is chosen
class SortChoice(val title: String, val isSelected: Boolean, val onSelect: () -> Unit)

// The ways to put the songs of a page in order, the one in use has a check
@Composable
fun SortChoiceSheet(title: String, choices: List<SortChoice>, onDismiss: () -> Unit) {
    WavvySheet(onDismiss = onDismiss) {
        val closeSheet = LocalSheetClose.current

        Column(modifier = Modifier.verticalScroll(rememberScrollState()).padding(bottom = ItemMenuDimens.Bottom)) {
            SheetHeader(title = title)

            choices.forEach { choice ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(ItemMenuDimens.RowHeight)
                        .clickable {
                            closeSheet()
                            choice.onSelect()
                        }
                        .padding(horizontal = ItemMenuDimens.Side)
                ) {
                    Box(modifier = Modifier.width(ItemMenuDimens.RowIcon + ItemMenuDimens.RowTextStart)) {
                        if (choice.isSelected) {
                            Icon(imageVector = WavvyIcons.Check, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                    Text(text = choice.title, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                }
            }

        }
    }
}

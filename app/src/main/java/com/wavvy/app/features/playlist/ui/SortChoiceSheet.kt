package com.wavvy.app.features.playlist.ui

// Android configuration
import android.content.res.Configuration
// Compose layouts and foundations
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
// Material 3 components
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
// Compose state and runtime
import androidx.compose.runtime.Composable
// UI styling and utilities
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
// Project resources
import com.wavvy.app.core.designsystem.components.LocalSheetClose
import com.wavvy.app.core.designsystem.components.ScrollIndicatorWidth
import com.wavvy.app.core.designsystem.components.WavvySheet
import com.wavvy.app.core.designsystem.components.scrollIndicator
import com.wavvy.app.core.designsystem.icons.WavvyIcons
import com.wavvy.app.core.designsystem.theme.WavvyTheme
import com.wavvy.app.features.menu.ItemMenuDimens

// One way to choose, the words, if it is the one in use and what it does when it is chosen
class SortChoice(val title: String, val isSelected: Boolean, val onSelect: () -> Unit)

// The choices of a setting or of the order of a page in a card, the one in use has its name in bold and a check at its end
@Composable
fun SortChoiceSheet(title: String, choices: List<SortChoice>, onDismiss: () -> Unit) {
    WavvySheet(onDismiss = onDismiss) {
        val closeSheet = LocalSheetClose.current
        val scroll = rememberScrollState()

        // The name of the choice on top
        val head: @Composable () -> Unit = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = ItemMenuDimens.CreditsTitleStart, vertical = ItemMenuDimens.HeaderVertical)
            )
        }

        // The choices, a line between two of them
        val options: @Composable () -> Unit = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(ItemMenuDimens.CardCorner))
                    .background(WavvyTheme.colors.menuCard)
            ) {
                choices.forEachIndexed { index, choice ->
                    if (index > 0) {
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = ItemMenuDimens.CardRowHorizontal),
                            thickness = WavvyTheme.dimens.hairline,
                            color = MaterialTheme.colorScheme.outlineVariant
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(ItemMenuDimens.CardRowTextStart),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                closeSheet()
                                choice.onSelect()
                            }
                            .padding(horizontal = ItemMenuDimens.CardRowHorizontal, vertical = ItemMenuDimens.CardRowVertical)
                    ) {
                        Text(
                            text = choice.title,
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = if (choice.isSelected) FontWeight.Bold else FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                        if (choice.isSelected) {
                            Icon(
                                imageVector = WavvyIcons.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(ItemMenuDimens.CardRowIcon)
                            )
                        }
                    }
                }
            }
        }

        // Lying down the screen is too short for the choices, the name stays on the start and the choices scroll on the end
        if (LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(ItemMenuDimens.CardGroupGap),
                modifier = Modifier.padding(start = ItemMenuDimens.CardsSide, end = ItemMenuDimens.CardsSide, bottom = ItemMenuDimens.LandscapeBottom)
            ) {
                Column(modifier = Modifier.weight(ItemMenuDimens.LandscapeHeadWeight).align(Alignment.CenterVertically)) { head() }
                Column(
                    modifier = Modifier
                        .weight(1f - ItemMenuDimens.LandscapeHeadWeight)
                        .scrollIndicator(scroll)
                        .verticalScroll(scroll)
                        .padding(end = ScrollIndicatorWidth + ItemMenuDimens.LandscapeIndicatorGap)
                ) { options() }
            }
        } else {
            // The bar on the end tells there are more choices below, as with the many countries
            Column(
                modifier = Modifier
                    .scrollIndicator(scroll)
                    .verticalScroll(scroll)
                    .padding(start = ItemMenuDimens.CardsSide, end = ItemMenuDimens.CardsSide, bottom = ItemMenuDimens.Bottom)
            ) {
                head()
                Spacer(modifier = Modifier.height(ItemMenuDimens.HeaderVertical))
                options()
            }
        }
    }
}

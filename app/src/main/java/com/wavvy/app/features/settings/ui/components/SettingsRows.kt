package com.wavvy.app.features.settings.ui.components

// Compose layouts and foundations
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
// Material 3 components
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
// Compose state and runtime
import androidx.compose.runtime.Composable
// UI styling and utilities
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
// Project resources
import com.wavvy.app.R
import com.wavvy.app.core.designsystem.icons.WavvyIcons
import com.wavvy.app.core.designsystem.theme.WavvyTheme
import com.wavvy.app.features.home.ui.components.HomeType
import com.wavvy.app.features.player.ui.LocalMiniPlayerInset
import com.wavvy.app.features.settings.ui.SettingsDimens
import com.wavvy.app.features.settings.ui.SettingsType

// The screen of a section, the arrow and the name on top and the cards of its settings under them
@Composable
fun SettingsPage(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val dimens = WavvyTheme.dimens

    // Solid, so the screen under it does not show through while the page slides
    Column(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))
                .padding(horizontal = dimens.spaceSmall)
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = WavvyIcons.Back,
                    contentDescription = stringResource(R.string.cd_back),
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }

            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge.merge(HomeType.SectionTitle),
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(start = dimens.spaceSmall)
            )
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(SettingsDimens.GroupGap),
            modifier = Modifier
                .weight(1f)
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
                .verticalScroll(rememberScrollState())
                .padding(
                    PaddingValues(
                        start = SettingsDimens.Side,
                        end = SettingsDimens.Side,
                        top = SettingsDimens.ListTop,
                        bottom = LocalMiniPlayerInset.current + SettingsDimens.GroupGap
                    )
                ),
            content = content
        )
    }
}

// The card that holds the rows of a group, with a line between two of them
@Composable
fun SettingsCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        shape = RoundedCornerShape(SettingsDimens.CardCorner),
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(content = content)
    }
}

// The line between two rows of a card
@Composable
fun SettingsDivider() {
    HorizontalDivider(modifier = Modifier.padding(horizontal = SettingsDimens.RowPadding), color = MaterialTheme.colorScheme.outlineVariant)
}

// A row with a name, a line under it and a switch at its end, a tap anywhere on it turns it
@Composable
fun SettingsSwitchRow(title: String, subtitle: String?, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = SettingsDimens.RowHeight)
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = SettingsDimens.RowPadding, vertical = SettingsDimens.RowVertical)
    ) {
        RowTexts(title = title, subtitle = subtitle, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

// A row with a name and what is chosen under it, which opens the choices when tapped
@Composable
fun SettingsChoiceRow(title: String, value: String, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = SettingsDimens.RowHeight)
            .clickable(onClick = onClick)
            .padding(horizontal = SettingsDimens.RowPadding, vertical = SettingsDimens.RowVertical)
    ) {
        RowTexts(title = title, subtitle = value.takeIf { it.isNotEmpty() }, modifier = Modifier.weight(1f))
    }
}

// A row with a name and a line under it and an arrow at its end, that opens another screen when tapped
@Composable
fun SettingsNavigateRow(title: String, subtitle: String?, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = SettingsDimens.RowHeight)
            .clickable(onClick = onClick)
            .padding(horizontal = SettingsDimens.RowPadding, vertical = SettingsDimens.RowVertical)
    ) {
        RowTexts(title = title, subtitle = subtitle, modifier = Modifier.weight(1f))
        Icon(
            imageVector = WavvyIcons.NavigateNext,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(SettingsDimens.RowArrow)
        )
    }
}

// A row with a name and a line under it that does something when tapped
@Composable
fun SettingsActionRow(title: String, subtitle: String?, onClick: () -> Unit) {
    SettingsChoiceRow(title = title, value = subtitle.orEmpty(), onClick = onClick)
}

// A row that only tells something, with the value at its end
@Composable
fun SettingsInfoRow(title: String, value: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = SettingsDimens.RowHeight)
            .padding(horizontal = SettingsDimens.RowPadding, vertical = SettingsDimens.RowVertical)
    ) {
        RowTexts(title = title, subtitle = null, modifier = Modifier.weight(1f))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.merge(SettingsType.RowSubtitle),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// The name of a row and the line under it
@Composable
private fun RowTexts(title: String, subtitle: String?, modifier: Modifier = Modifier) {
    Column(modifier = modifier.padding(end = SettingsDimens.RowIconGap)) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge.merge(SettingsType.RowTitle),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        subtitle?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodyMedium.merge(SettingsType.RowSubtitle),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

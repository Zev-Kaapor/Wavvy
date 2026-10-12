package com.wavvy.app.features.settings.ui

// Android version
import android.os.Build
// Compose layouts and foundations
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
// Material 3 components
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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

// The settings, the groups of sections in cards, each section with its icon, its name and the line that says what is in it
// The sections open their own screens as they are built, until then they only stand in their places
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    onSectionClick: (SettingsSection) -> Unit = {}
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
                text = stringResource(R.string.profile_settings),
                style = MaterialTheme.typography.titleLarge.merge(HomeType.SectionTitle),
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(start = dimens.spaceSmall)
            )
        }

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(SettingsDimens.GroupGap),
            modifier = Modifier
                .weight(1f)
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)),
            contentPadding = PaddingValues(
                start = SettingsDimens.Side,
                end = SettingsDimens.Side,
                top = SettingsDimens.ListTop,
                bottom = LocalMiniPlayerInset.current + SettingsDimens.GroupGap
            )
        ) {
            items(SettingsGroup.entries, key = { it.name }) { group ->
                GroupCard(group = group, onSectionClick = onSectionClick)
            }
        }
    }
}

// The name of a group over its card, and the card with its sections one under the other
@Composable
private fun GroupCard(group: SettingsGroup, onSectionClick: (SettingsSection) -> Unit) {
    Column {
        Text(
            text = stringResource(group.titleRes),
            style = MaterialTheme.typography.labelLarge.merge(SettingsType.GroupTitle),
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.padding(start = SettingsDimens.GroupTitleStart, bottom = SettingsDimens.GroupTitleBottom)
        )

        Surface(
            shape = RoundedCornerShape(SettingsDimens.CardCorner),
            color = MaterialTheme.colorScheme.surfaceContainer
        ) {
            Column {
                // The system only lets an app ask for the links from Android 12
                val sections = group.sections.filter { it != SettingsSection.Links || Build.VERSION.SDK_INT >= Build.VERSION_CODES.S }
                sections.forEachIndexed { index, section ->
                    if (index > 0) {
                        HorizontalDivider(
                            modifier = Modifier.padding(start = SettingsDimens.DividerStart),
                            color = MaterialTheme.colorScheme.outlineVariant
                        )
                    }
                    SectionRow(section = section, onClick = { onSectionClick(section) })
                }
            }
        }
    }
}

// A section, its icon, its name, the line under it and the arrow at the end
@Composable
private fun SectionRow(section: SettingsSection, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .height(SettingsDimens.RowHeight)
            .clickable(onClick = onClick)
            .padding(horizontal = SettingsDimens.RowPadding)
    ) {
        Icon(
            imageVector = section.icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(SettingsDimens.RowIcon)
        )

        Column(modifier = Modifier.weight(1f).padding(horizontal = SettingsDimens.RowIconGap)) {
            Text(
                text = stringResource(section.titleRes),
                style = MaterialTheme.typography.bodyLarge.merge(SettingsType.RowTitle),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = stringResource(section.subtitleRes),
                style = MaterialTheme.typography.bodyMedium.merge(SettingsType.RowSubtitle),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Icon(
            imageVector = WavvyIcons.NavigateNext,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(SettingsDimens.RowArrow)
        )
    }
}

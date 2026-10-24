package com.wavvy.app.features.profile.ui

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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
// UI styling and utilities
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import kotlinx.coroutines.launch
// Project resources
import com.wavvy.app.R
import com.wavvy.app.core.designsystem.components.ScrollIndicatorWidth
import com.wavvy.app.core.designsystem.components.WavvySheet
import com.wavvy.app.core.designsystem.components.WavvySwitch
import com.wavvy.app.core.designsystem.components.scrollIndicator
import com.wavvy.app.core.designsystem.icons.WavvyIcons
import com.wavvy.app.core.designsystem.theme.WavvyTheme
import com.wavvy.app.features.menu.ItemMenuDimens
import com.wavvy.app.features.settings.data.SettingsStore

// What the profile shows, a sheet with a row for each part and a switch that takes effect at once
@Composable
fun ProfileVisibilitySheet(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val store = remember { SettingsStore(context) }
    val showActivity by store.profileShowActivity.collectAsState(initial = true)
    val showSongs by store.profileShowSongs.collectAsState(initial = true)
    val showArtists by store.profileShowArtists.collectAsState(initial = true)
    val showPlaylists by store.profileShowPlaylists.collectAsState(initial = true)

    // The words on top and the card with a row for each part
    val head: @Composable () -> Unit = {
        Column(modifier = Modifier.padding(horizontal = ItemMenuDimens.CreditsTitleStart, vertical = ItemMenuDimens.HeaderVertical)) {
            Text(
                text = stringResource(R.string.profile_visibility_title),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = stringResource(R.string.profile_visibility_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
    val options: @Composable () -> Unit = {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(ItemMenuDimens.CardCorner))
                .background(WavvyTheme.colors.menuCard)
        ) {
            VisibilityRow(WavvyIcons.Headphones, R.string.profile_visibility_activity, R.string.profile_visibility_activity_hint, showActivity) { scope.launch { store.setProfileShowActivity(it) } }
            VisibilityDivider()
            VisibilityRow(WavvyIcons.MusicNote, R.string.profile_visibility_songs, R.string.profile_visibility_songs_hint, showSongs) { scope.launch { store.setProfileShowSongs(it) } }
            VisibilityDivider()
            VisibilityRow(WavvyIcons.Mic, R.string.profile_visibility_artists, R.string.profile_visibility_artists_hint, showArtists) { scope.launch { store.setProfileShowArtists(it) } }
            VisibilityDivider()
            VisibilityRow(WavvyIcons.QueueMusic, R.string.profile_visibility_playlists, R.string.profile_visibility_playlists_hint, showPlaylists) { scope.launch { store.setProfileShowPlaylists(it) } }
        }
    }

    WavvySheet(onDismiss = onDismiss) {
        // Lying down the screen is too short for the rows, the words stay on the start and the rows scroll on the end
        if (LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(ItemMenuDimens.CardGroupGap),
                modifier = Modifier.padding(start = ItemMenuDimens.CardsSide, end = ItemMenuDimens.CardsSide, bottom = ItemMenuDimens.LandscapeBottom)
            ) {
                Column(modifier = Modifier.weight(ItemMenuDimens.LandscapeHeadWeight).align(Alignment.CenterVertically)) { head() }

                // The bar on the end tells there are more rows below
                val scroll = rememberScrollState()
                Column(
                    modifier = Modifier
                        .weight(1f - ItemMenuDimens.LandscapeHeadWeight)
                        .scrollIndicator(scroll)
                        .verticalScroll(scroll)
                        .padding(end = ScrollIndicatorWidth + ItemMenuDimens.LandscapeIndicatorGap)
                ) { options() }
            }
        } else {
            Column(modifier = Modifier.padding(start = ItemMenuDimens.CardsSide, end = ItemMenuDimens.CardsSide, bottom = ItemMenuDimens.Bottom)) {
                head()
                Spacer(modifier = Modifier.height(ItemMenuDimens.HeaderVertical))
                options()
            }
        }
    }
}

// A part of the profile with its icon, its name, a line about it and the switch at the end, a tap anywhere on the row turns it
@Composable
private fun VisibilityRow(icon: ImageVector, title: Int, hint: Int, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onChange(!checked) }
            .padding(horizontal = ItemMenuDimens.CardRowHorizontal, vertical = ItemMenuDimens.CardRowVertical)
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(ItemMenuDimens.CardRowIcon))
        Column(modifier = Modifier.weight(1f).padding(start = ItemMenuDimens.CardRowTextStart)) {
            Text(
                text = stringResource(title),
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
            Text(
                text = stringResource(hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        WavvySwitch(checked = checked, onCheckedChange = onChange)
    }
}

// The line between two rows, as long as the words and not the icon
@Composable
private fun VisibilityDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = ItemMenuDimens.CardRowHorizontal + ItemMenuDimens.CardRowIcon + ItemMenuDimens.CardRowTextStart, end = ItemMenuDimens.CardRowHorizontal),
        thickness = WavvyTheme.dimens.hairline,
        color = MaterialTheme.colorScheme.outlineVariant
    )
}

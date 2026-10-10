package com.wavvy.app.features.podcast.ui

// Compose layouts and foundations
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
// Material 3 components
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
// Coroutines
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
// Project resources
import com.wavvy.app.R
import com.wavvy.app.core.designsystem.components.WavvySheet
import com.wavvy.app.core.designsystem.icons.WavvyIcons
import com.wavvy.app.core.history.PlayHistory
import com.wavvy.app.features.home.data.HomeItemKind
import com.wavvy.app.features.menu.ItemMenuDimens
import com.wavvy.app.features.player.ui.components.MenuAction

// The three dots of a podcast, its name on top with the close button, the search among its episodes when it has the page open and the pin to the speed dial
@Composable
internal fun PodcastMenu(
    id: String,
    title: String,
    author: String?,
    coverUrl: String?,
    onSearch: (() -> Unit)?,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val pinnedIds by remember { PlayHistory.pinned(context).map { pinned -> pinned.map { it.id }.toSet() } }.collectAsState(initial = emptySet())
    val isPinned = id in pinnedIds

    WavvySheet(onDismiss = onDismiss) {
        Column(modifier = Modifier.padding(bottom = ItemMenuDimens.Bottom)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = ItemMenuDimens.Side, end = ItemMenuDimens.CloseEnd, top = ItemMenuDimens.HeaderTop, bottom = ItemMenuDimens.HeaderBottom)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = WavvyIcons.Close,
                        contentDescription = stringResource(R.string.cd_close),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // The search is only on the page of the podcast
            onSearch?.let {
                MenuAction(WavvyIcons.Search, stringResource(R.string.podcast_search)) {
                    onDismiss()
                    it()
                }
            }
            MenuAction(WavvyIcons.Pin, stringResource(if (isPinned) R.string.menu_unpin else R.string.menu_pin)) {
                scope.launch(Dispatchers.IO) {
                    if (isPinned) {
                        PlayHistory.unpin(context, id)
                    } else {
                        PlayHistory.pin(context, id, title, author, coverUrl, durationMs = 0L, kind = HomeItemKind.Podcast.name)
                    }
                }
                onDismiss()
            }
        }
    }
}

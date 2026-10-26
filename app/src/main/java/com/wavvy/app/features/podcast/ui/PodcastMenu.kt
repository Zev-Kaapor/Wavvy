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
import com.wavvy.app.core.designsystem.components.LocalSheetClose
import com.wavvy.app.core.designsystem.components.WavvySheet
import com.wavvy.app.core.designsystem.icons.WavvyIcons
import com.wavvy.app.core.history.PlayHistory
import com.wavvy.app.features.home.data.HomeItemKind
import com.wavvy.app.features.menu.CardMenuHead
import com.wavvy.app.features.menu.CardMenuLayout
import com.wavvy.app.features.menu.MenuCard

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
        val closeSheet = LocalSheetClose.current

        CardMenuLayout(
            head = { CardMenuHead(title = title, subtitle = author, coverUrl = coverUrl) },
            options = {
                // The search is only on the page of the podcast
                onSearch?.let {
                    MenuCard(WavvyIcons.Search, stringResource(R.string.podcast_search)) {
                        closeSheet()
                        it()
                    }
                }
                MenuCard(WavvyIcons.Pin, stringResource(if (isPinned) R.string.menu_unpin else R.string.menu_pin)) {
                    scope.launch(Dispatchers.IO) {
                        if (isPinned) {
                            PlayHistory.unpin(context, id)
                        } else {
                            PlayHistory.pin(context, id, title, author, coverUrl, durationMs = 0L, kind = HomeItemKind.Podcast.name)
                        }
                    }
                    closeSheet()
                }
            }
        )
    }
}

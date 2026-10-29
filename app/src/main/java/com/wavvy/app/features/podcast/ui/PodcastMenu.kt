package com.wavvy.app.features.podcast.ui

// Compose layouts and foundations
// Material 3 components
// Compose state and runtime
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
// UI styling and utilities
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
// Coroutines
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
// Project resources
import com.wavvy.app.R
import com.wavvy.app.core.designsystem.components.LocalSheetClose
import com.wavvy.app.core.designsystem.components.Toasts
import com.wavvy.app.core.designsystem.components.WavvySheet
import com.wavvy.app.core.designsystem.icons.WavvyIcons
import com.wavvy.app.core.history.PlayHistory
import com.wavvy.app.features.home.data.HomeItemKind
import com.wavvy.app.features.menu.CardMenuHead
import com.wavvy.app.features.menu.CardMenuLayout
import com.wavvy.app.features.menu.MenuCard
import com.wavvy.app.features.podcast.data.PodcastAutoDownloads
import com.wavvy.app.features.settings.data.AutoPodcast
import com.wavvy.app.features.settings.data.SettingsStore

// The three dots of a podcast, its name on top with the close button, the search among its episodes when it has the page open, the automatic downloads and the pin to the speed dial
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
    val autoIds by remember { SettingsStore(context).autoPodcasts.map { podcasts -> podcasts.map { it.id }.toSet() } }.collectAsState(initial = emptySet())
    val isAuto = id in autoIds
    val resources = LocalResources.current

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
                // The newest episodes download by themselves, at once and each time the app opens, as YouTube Music has it
                MenuCard(WavvyIcons.Download, stringResource(if (isAuto) R.string.podcast_auto_off else R.string.podcast_auto_on)) {
                    PodcastAutoDownloads.set(context, AutoPodcast(id, title, coverUrl), isOn = !isAuto)
                    Toasts.show(if (isAuto) resources.getString(R.string.podcast_auto_off_done) else resources.getString(R.string.podcast_auto_on_done, PodcastAutoDownloads.NewestEpisodes))
                    closeSheet()
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

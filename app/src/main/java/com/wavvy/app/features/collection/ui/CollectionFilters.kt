package com.wavvy.app.features.collection.ui

// Compose layouts and foundations
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.LazyRow
// Material 3 components
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
// Compose state and runtime
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
// UI styling and utilities
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
// Project resources
import com.wavvy.app.R
import com.wavvy.app.core.designsystem.icons.WavvyIcons
import com.wavvy.app.core.designsystem.theme.WavvyTheme
import com.wavvy.app.core.designsystem.theme.WavvyMotion
import com.wavvy.app.features.home.data.HomeItem
import com.wavvy.app.features.home.ui.components.isVideo
import com.wavvy.app.features.library.ui.Chip
import com.wavvy.app.features.podcast.ui.PodcastSearchField

// What narrows the songs of a page, the words of the search and the three buttons that are on or off
data class CollectionFilters(
    val query: String? = null,
    val isDownloaded: Boolean = false,
    val isSongsOnly: Boolean = false,
    val isClean: Boolean = false
) {
    // True when anything narrows the list, so an empty answer is known to come from it
    val isActive: Boolean
        get() = !query.isNullOrBlank() || isDownloaded || isSongsOnly || isClean

    // The songs that pass every filter that is on
    fun apply(songs: List<HomeItem>, isDownloadedSong: (String) -> Boolean): List<HomeItem> {
        val words = query?.trim().orEmpty()

        return songs.filter { song ->
            (words.isEmpty() || song.title.contains(words, ignoreCase = true) || song.artists.any { it.contains(words, ignoreCase = true) }) &&
                (!isDownloaded || isDownloadedSong(song.id)) &&
                (!isSongsOnly || !song.isVideo) &&
                (!isClean || !song.isExplicit)
        }
    }
}

// The row over the songs, the three buttons and the search icon at the end
// The field grows from the icon to the whole width while the buttons fade, and goes back the same way, as the search of the library does
@Composable
internal fun CollectionFilterRow(
    filters: CollectionFilters,
    onChange: (CollectionFilters) -> Unit
) {
    val isSearching = filters.query != null
    val progress by animateFloatAsState(
        targetValue = if (isSearching) 1f else 0f,
        animationSpec = tween(WavvyMotion.ExpandMillis),
        label = "CollectionSearch"
    )

    Box {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.alpha(1f - progress)
        ) {
            LazyRow(
                contentPadding = PaddingValues(start = WavvyTheme.dimens.spaceLarge, top = WavvyTheme.dimens.spaceSmall, bottom = WavvyTheme.dimens.spaceSmall),
                horizontalArrangement = Arrangement.spacedBy(WavvyTheme.dimens.spaceSmall),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                item(key = "downloaded") { Chip(stringResource(R.string.collection_filter_downloaded), filters.isDownloaded) { onChange(filters.copy(isDownloaded = !filters.isDownloaded)) } }
                item(key = "songs") { Chip(stringResource(R.string.collection_filter_songs), filters.isSongsOnly) { onChange(filters.copy(isSongsOnly = !filters.isSongsOnly)) } }
                item(key = "clean") { Chip(stringResource(R.string.collection_filter_clean), filters.isClean) { onChange(filters.copy(isClean = !filters.isClean)) } }
            }

            IconButton(onClick = { if (!isSearching) onChange(filters.copy(query = "")) }) {
                Icon(
                    imageVector = WavvyIcons.Search,
                    contentDescription = stringResource(R.string.collection_search_hint),
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
        }

        if (progress > 0f) {
            PodcastSearchField(
                query = filters.query.orEmpty(),
                onQueryChange = { onChange(filters.copy(query = it)) },
                onClose = { onChange(filters.copy(query = null)) },
                hint = stringResource(R.string.collection_search_hint),
                progress = progress
            )
        }
    }
}

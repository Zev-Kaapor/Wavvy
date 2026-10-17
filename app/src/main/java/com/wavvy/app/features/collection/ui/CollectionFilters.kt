package com.wavvy.app.features.collection.ui

// Compose layouts and foundations
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
// Material 3 components
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
// Compose state and runtime
import androidx.compose.runtime.Composable
// UI styling and utilities
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
// Project resources
import com.wavvy.app.R
import com.wavvy.app.core.designsystem.icons.WavvyIcons
import com.wavvy.app.core.designsystem.theme.WavvyTheme
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

// The row over the songs, a search icon and the three buttons, the field takes the place of the row while the search is open
@Composable
internal fun CollectionFilterRow(
    filters: CollectionFilters,
    onChange: (CollectionFilters) -> Unit
) {
    if (filters.query != null) {
        PodcastSearchField(
            query = filters.query,
            onQueryChange = { onChange(filters.copy(query = it)) },
            onClose = { onChange(filters.copy(query = null)) },
            hint = stringResource(R.string.collection_search_hint)
        )
        return
    }

    LazyRow(
        contentPadding = PaddingValues(horizontal = WavvyTheme.dimens.spaceLarge, vertical = WavvyTheme.dimens.spaceSmall),
        horizontalArrangement = Arrangement.spacedBy(WavvyTheme.dimens.spaceSmall),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        item(key = "search") {
            Icon(
                imageVector = WavvyIcons.Search,
                contentDescription = stringResource(R.string.collection_search_hint),
                tint = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier
                    .padding(end = WavvyTheme.dimens.spaceSmall)
                    .size(24.dp)
                    .clickable { onChange(filters.copy(query = "")) }
            )
        }
        item(key = "downloaded") { Chip(stringResource(R.string.collection_filter_downloaded), filters.isDownloaded) { onChange(filters.copy(isDownloaded = !filters.isDownloaded)) } }
        item(key = "songs") { Chip(stringResource(R.string.collection_filter_songs), filters.isSongsOnly) { onChange(filters.copy(isSongsOnly = !filters.isSongsOnly)) } }
        item(key = "clean") { Chip(stringResource(R.string.collection_filter_clean), filters.isClean) { onChange(filters.copy(isClean = !filters.isClean)) } }
    }
}

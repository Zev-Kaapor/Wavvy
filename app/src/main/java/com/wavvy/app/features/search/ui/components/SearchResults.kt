package com.wavvy.app.features.search.ui.components

// Compose layouts and foundations
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
// Material 3 components
import androidx.compose.material3.CircularProgressIndicator
// Compose state and runtime
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
// UI styling and utilities
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
// Project resources
import com.wavvy.app.R
import com.wavvy.app.features.home.data.HomeFilter
import com.wavvy.app.features.home.data.HomeItem
import com.wavvy.app.features.home.data.HomeItemKind
import com.wavvy.app.features.home.ui.components.HomeFilterRow
import com.wavvy.app.features.home.ui.components.HomeListItem
import com.wavvy.app.features.home.ui.components.HomeMessage
import com.wavvy.app.features.home.ui.components.HomeSectionTitle
import com.wavvy.app.features.home.ui.components.isVideo
import com.wavvy.app.features.player.ui.LocalMiniPlayerInset
import com.wavvy.app.features.search.data.SearchCategory
import com.wavvy.app.features.search.ui.SearchDimens
import com.wavvy.app.features.search.ui.SearchStatus
import com.wavvy.app.features.search.ui.SearchUiState

// Results of a search, the filters on top and under them the best result with a few rows of each kind or one whole kind with more rows as the list goes down
@Composable
fun SearchResults(
    state: SearchUiState,
    onCategoryClick: (SearchCategory) -> Unit,
    onLoadMore: () -> Unit,
    onRetry: () -> Unit,
    onItemClick: (HomeItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    // Filters as the chips of the Home, the selected one is the kind that is shown
    val filters = SearchCategory.entries.map { HomeFilter(title = categoryTitle(it), params = it.name) }
    val selected = filters.first { it.params == state.category.name }

    // True when the last rows on the screen are close to the end of the list
    val nearEnd by remember {
        derivedStateOf {
            val info = listState.layoutInfo
            val last = info.visibleItemsInfo.lastOrNull()?.index ?: 0
            last >= info.totalItemsCount - SearchDimens.LoadMoreThreshold
        }
    }
    LaunchedEffect(nearEnd, state.continuation, state.items.size) {
        if (nearEnd) onLoadMore()
    }

    // A new search or a new filter starts at the top
    LaunchedEffect(state.submitted, state.category) {
        listState.scrollToItem(0)
    }

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = LocalMiniPlayerInset.current)
    ) {
        item(key = "filters") {
            HomeFilterRow(
                filters = filters,
                selected = selected,
                onSelect = { onCategoryClick(SearchCategory.valueOf(it.params)) },
                modifier = Modifier.padding(top = SearchDimens.ChipsTop, bottom = SearchDimens.ChipsBottom)
            )
        }

        when {
            state.status == SearchStatus.Loading -> item(key = "skeleton") { SearchSkeleton() }

            state.status == SearchStatus.Error -> item(key = "error") {
                HomeMessage(
                    text = stringResource(R.string.search_error),
                    modifier = Modifier.fillParentMaxSize(),
                    actionLabel = stringResource(R.string.home_retry),
                    onAction = onRetry
                )
            }

            state.items.isEmpty() && state.topMatch == null -> item(key = "empty") {
                HomeMessage(text = stringResource(R.string.search_empty), modifier = Modifier.fillParentMaxSize())
            }

            state.category == SearchCategory.All -> everything(state, onCategoryClick, onItemClick)

            else -> {
                items(state.items, key = { it.id }) { item ->
                    HomeListItem(item = item, onClick = { onItemClick(item) })
                }

                if (state.isLoadingMore) {
                    item(key = "more") {
                        Box(modifier = Modifier.fillMaxWidth().padding(SearchDimens.MorePadding), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(modifier = Modifier.size(SearchDimens.MoreSpinner))
                        }
                    }
                }
            }
        }
    }
}

// The best result and then a few rows of each kind, its title opens the whole kind
private fun LazyListScope.everything(
    state: SearchUiState,
    onCategoryClick: (SearchCategory) -> Unit,
    onItemClick: (HomeItem) -> Unit
) {
    state.topMatch?.let { match ->
        item(key = "top_title") { HomeSectionTitle(title = stringResource(R.string.search_best_result)) }
        item(key = "top_${match.id}") { HomeListItem(item = match, onClick = { onItemClick(match) }) }
    }

    sections(state.items).forEach { (category, rows) ->
        item(key = "title_${category.name}") {
            HomeSectionTitle(title = categoryTitle(category), onClick = { onCategoryClick(category) })
        }

        items(rows.take(SearchDimens.SectionPreviewRows), key = { "${category.name}_${it.id}" }) { item ->
            HomeListItem(item = item, onClick = { onItemClick(item) })
        }
    }
}

// What the search of everything brought, split by kind in the order the filters have, the kinds without rows are left out
private fun sections(items: List<HomeItem>): List<Pair<SearchCategory, List<HomeItem>>> {
    val byCategory = items.groupBy { categoryOf(it) }

    return SearchCategory.entries.mapNotNull { category ->
        byCategory[category]?.let { category to it }
    }
}

// The filter that holds a kind of result, a video is a song with a picture
private fun categoryOf(item: HomeItem): SearchCategory = when (item.kind) {
    HomeItemKind.Song -> if (item.isVideo) SearchCategory.Videos else SearchCategory.Songs
    HomeItemKind.Album -> SearchCategory.Albums
    HomeItemKind.Artist -> SearchCategory.Artists
    HomeItemKind.Playlist -> SearchCategory.CommunityPlaylists
    HomeItemKind.Podcast -> SearchCategory.Podcasts
    HomeItemKind.Episode -> SearchCategory.Episodes
}

// Name of a filter in the language of the app
@Composable
fun categoryTitle(category: SearchCategory): String = stringResource(
    when (category) {
        SearchCategory.All -> R.string.search_category_all
        SearchCategory.Artists -> R.string.search_category_artists
        SearchCategory.Songs -> R.string.search_category_songs
        SearchCategory.Videos -> R.string.search_category_videos
        SearchCategory.CommunityPlaylists -> R.string.search_category_community_playlists
        SearchCategory.Episodes -> R.string.search_category_episodes
        SearchCategory.Albums -> R.string.search_category_albums
        SearchCategory.Profiles -> R.string.search_category_profiles
        SearchCategory.FeaturedPlaylists -> R.string.search_category_featured_playlists
        SearchCategory.Podcasts -> R.string.search_category_podcasts
    }
)

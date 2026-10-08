package com.wavvy.app.features.home.ui

// Compose layouts and foundations
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
// Material 3 components
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
// Compose state and runtime
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
// UI utilities
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
// Android utilities and lifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
// Project resources
import com.wavvy.app.R
import com.wavvy.app.core.designsystem.icons.WavvyIcons
import com.wavvy.app.core.designsystem.theme.WavvyTheme
import com.wavvy.app.core.navigation.ItemNavigator
import com.wavvy.app.features.home.data.HomeFilter
import com.wavvy.app.features.home.data.HomeItem
import com.wavvy.app.features.home.data.HomeItemKind
import com.wavvy.app.features.home.data.HomeSection
import com.wavvy.app.features.home.ui.components.HomeCoverRow
import com.wavvy.app.features.home.ui.components.HomeDimens
import com.wavvy.app.features.home.ui.components.HomeFilterRow
import com.wavvy.app.features.home.ui.components.HomeHeader
import com.wavvy.app.features.home.ui.components.HomeMessage
import com.wavvy.app.features.home.ui.components.HomeRecentRow
import com.wavvy.app.features.home.ui.components.HomeSectionTitle
import com.wavvy.app.features.home.ui.components.HomeShelf
import com.wavvy.app.features.home.ui.components.HomeSkeleton
import com.wavvy.app.features.home.ui.components.HomeSpeedDial
import com.wavvy.app.features.home.ui.components.LocalPinnedIds
import com.wavvy.app.features.player.ui.LocalMiniPlayerInset
import com.wavvy.app.features.profile.ui.LocalProfile

// Home tab, the header and what YouTube Music brings for this user, whatever does not come does not show
@Composable
fun HomeScreen(
    onProfileClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()

    // Songs and episodes play, the other cards do nothing until they have pages
    val onItemClick = rememberItemPlayer()

    // Back first leaves the selected filter, and only then leaves the app
    BackHandler(enabled = state.selectedFilter != null) {
        state.selectedFilter?.let(viewModel::selectFilter)
    }

    Column(modifier = modifier.fillMaxSize()) {
        // The bell does nothing yet
        HomeHeader(onNotificationsClick = { ItemNavigator.openNotifications() }, onProfileClick = onProfileClick)

        when {
            // Nothing to show yet, not even the filters
            state.status == HomeStatus.Loading && state.filters.isEmpty() -> HomeSkeleton(modifier = Modifier.weight(1f))

            state.status == HomeStatus.Error -> HomeMessage(
                text = stringResource(R.string.home_error),
                modifier = Modifier.weight(1f),
                actionLabel = stringResource(R.string.home_retry),
                onAction = viewModel::load
            )

            else -> {
                // Every cover below can show the pin of a pinned song
                val pinnedIds = remember(state.pinned) { state.pinned.map { it.id }.toSet() }
                CompositionLocalProvider(LocalPinnedIds provides pinnedIds) {
                    HomeContent(
                        state = state,
                        onFilterClick = viewModel::selectFilter,
                        onLoadMore = viewModel::loadMore,
                        onRefresh = viewModel::refresh,
                        onItemClick = onItemClick,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

// Filters and shelves in a list that refreshes when pulled down and asks for more shelves near its end
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeContent(
    state: HomeUiState,
    onFilterClick: (HomeFilter) -> Unit,
    onLoadMore: () -> Unit,
    onRefresh: () -> Unit,
    onItemClick: (HomeItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val dimens = WavvyTheme.dimens
    val listState = rememberLazyListState()
    val playList = rememberListPlayer()

    // What the user pinned, what was listened to the longest and then the items of the shelves, without repeats
    // The speed dial is left out when a filter is selected
    val speedDial = remember(state.sections, state.selectedFilter, state.keepListening, state.pinned) {
        if (state.selectedFilter != null) {
            emptyList()
        } else {
            (state.pinned + state.keepListening + state.sections.flatMap { it.items }).distinctBy { it.id }.take(HomeDimens.SpeedDialMaxItems)
        }
    }

    // True when the last cards on the screen are close to the end of the list
    val nearEnd by remember {
        derivedStateOf {
            val info = listState.layoutInfo
            val last = info.visibleItemsInfo.lastOrNull()?.index ?: 0
            last >= info.totalItemsCount - HomeDimens.LoadMoreThreshold
        }
    }
    LaunchedEffect(nearEnd, state.continuation, state.sections.size) {
        if (nearEnd) onLoadMore()
    }

    PullToRefreshBox(
        isRefreshing = state.isRefreshing,
        onRefresh = onRefresh,
        modifier = modifier
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            // The mini player covers the end of the list, which gets room for it
            contentPadding = PaddingValues(bottom = LocalMiniPlayerInset.current)
        ) {
            if (state.filters.isNotEmpty()) {
                item(key = "filters") {
                    HomeFilterRow(filters = state.filters, selected = state.selectedFilter, onSelect = onFilterClick)
                }
            }

            when {
                state.status == HomeStatus.Loading -> item(key = "skeleton") {
                    HomeSkeleton(showFilters = false)
                }

                state.sections.isEmpty() -> item(key = "empty") {
                    HomeMessage(text = stringResource(R.string.home_empty), modifier = Modifier.height(HomeDimens.EmptyHeight))
                }

                else -> {
                    if (speedDial.isNotEmpty()) {
                        item(key = "speed_dial") {
                            Column {
                                HomeSectionTitle(title = stringResource(R.string.home_speed_dial))
                                HomeSpeedDial(
                                    items = speedDial,
                                    onItemClick = onItemClick,
                                    // The die plays every song of the speed dial shuffled, from a random one, and with no song it opens a random card
                                    onRandomClick = {
                                        val songs = speedDial.filter { it.kind == HomeItemKind.Song }
                                        if (songs.isNotEmpty()) playList(songs, songs.indices.random(), true) else speedDial.randomOrNull()?.let(onItemClick)
                                    }
                                )
                            }
                        }
                    }

                    // What was listened to on this device, left out while a filter is selected like the speed dial
                    if (state.selectedFilter == null && state.recent.isNotEmpty()) {
                        item(key = "recent") {
                            Column {
                                HomeSectionTitle(title = stringResource(R.string.home_recent))
                                HomeRecentRow(items = state.recent, onItemClick = onItemClick)
                            }
                        }
                    }

                    // Songs like the ones listened to lately and the ones that were forgotten, hidden until there is something to show
                    if (state.selectedFilter == null && state.quickPicks.isNotEmpty()) {
                        item(key = "quick_picks") {
                            HomeShelf(
                                section = HomeSection(
                                    title = stringResource(R.string.home_quick_picks),
                                    label = null,
                                    thumbnailUrl = null,
                                    link = null,
                                    items = state.quickPicks
                                ),
                                onItemClick = onItemClick
                            )
                        }
                    }

                    if (state.selectedFilter == null && state.forgotten.isNotEmpty()) {
                        item(key = "forgotten") {
                            Column {
                                HomeSectionTitle(title = stringResource(R.string.home_forgotten))
                                HomeCoverRow(items = state.forgotten, onItemClick = onItemClick)
                            }
                        }
                    }

                    if (state.accountPlaylists.isNotEmpty()) {
                        item(key = "account_playlists") { AccountPlaylists(items = state.accountPlaylists, onItemClick = onItemClick) }
                    }

                    itemsIndexed(state.sections, key = { index, _ -> "section_$index" }) { _, section ->
                        HomeShelf(section = section, onItemClick = onItemClick)
                    }
                }
            }

            // Room under the last shelf
            item(key = "end") { Spacer(modifier = Modifier.height(dimens.spaceExtraLarge)) }
        }
    }
}

// Playlists of the account under the name and the photo of the account, as Metrolist (GPL-3.0) shows them
@Composable
private fun AccountPlaylists(
    items: List<HomeItem>,
    onItemClick: (HomeItem) -> Unit
) {
    val profile = LocalProfile.current

    Column {
        // The title does nothing until there is a page of the account
        HomeSectionTitle(
            label = stringResource(R.string.home_mixes),
            title = profile.name.orEmpty(),
            thumbnail = {
                val photo = profile.photo
                if (photo != null) {
                    Image(
                        bitmap = photo,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(HomeDimens.TitlePhoto)
                            .clip(CircleShape)
                    )
                } else {
                    Icon(
                        imageVector = WavvyIcons.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.size(HomeDimens.TitlePhoto)
                    )
                }
            },
            onClick = {}
        )

        HomeCoverRow(items = items, onItemClick = onItemClick)
    }
}

package com.wavvy.app.features.radio.ui

// Android text encoding
import android.net.Uri
// Compose layouts and foundations
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
// Material 3 components
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
// Compose state and runtime
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
// UI styling and utilities
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
// Image loading
import coil3.compose.AsyncImage
// Project resources
import com.wavvy.app.R
import com.wavvy.app.core.designsystem.icons.WavvyIcons
import com.wavvy.app.core.designsystem.theme.WavvyTheme
import com.wavvy.app.core.navigation.ItemNavigator
import com.wavvy.app.core.radio.RadioStation
import com.wavvy.app.core.radio.RadiosBrowse
import com.wavvy.app.core.radio.RadiosCountriesPath
import com.wavvy.app.core.radio.RadiosEntry
import com.wavvy.app.core.radio.allStationsPath
import com.wavvy.app.core.radio.plain
import com.wavvy.app.core.radio.userRadiosCountry
import com.wavvy.app.features.home.data.Separator
import com.wavvy.app.features.home.ui.components.HomeMessage
import com.wavvy.app.features.player.ui.LocalMiniPlayerInset
import com.wavvy.app.features.profile.ui.RowsSkeleton
import com.wavvy.app.features.search.ui.components.SearchBar

// The radios, a search by name, city, state or country, and under it the same four ways to walk through the places, from the country of the user down to the stations
@Composable
fun RadiosScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var filter by rememberSaveable { mutableStateOf(RadiosFilter.Name) }
    var query by rememberSaveable { mutableStateOf("") }
    // What was searched, which the lists follow, the field changes with every letter and the lists wait for the search
    var submitted by rememberSaveable { mutableStateOf("") }
    // The country of the user in the site, nothing while it is looked for and an empty list when it could not be found
    val country by produceState<List<RadiosEntry>?>(initialValue = null) { value = listOfNotNull(userRadiosCountry(context)) }

    Column(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        SearchBar(
            query = query,
            onQueryChange = {
                query = it
                if (it.isEmpty()) submitted = ""
            },
            onSearch = { submitted = query.trim() },
            onBack = onBack,
            onFocusChange = {},
            hint = stringResource(R.string.radios_search_hint)
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(RadiosDimens.ChipsGap),
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = WavvyTheme.dimens.screenPadding, vertical = RadiosDimens.ChipsVertical)
        ) {
            RadiosFilter.entries.forEach { option ->
                FilterChip(selected = filter == option, onClick = { filter = option }, label = { Text(text = stringResource(option.titleRes)) })
            }
        }

        val countryPath = country?.firstOrNull()?.path
        when {
            filter == RadiosFilter.Country && submitted.isEmpty() -> PlacesList(path = RadiosCountriesPath) { listOf(RadiosSection(null, it.countries)) }

            submitted.isNotEmpty() && filter != RadiosFilter.State ->
                StationsList(path = "/busca?q=${Uri.encode(submitted)}&qfilter=${filter.wire}")

            country == null -> RowsSkeleton()

            countryPath == null -> HomeMessage(text = stringResource(R.string.home_error), modifier = Modifier.weight(1f))

            // The site also lists the states that only contain the words, so the name has to be the same, or the words have to start it
            filter == RadiosFilter.State -> PlacesList(path = countryPath.orEmpty()) { browse ->
                val wanted = submitted.plain()
                val states = if (wanted.isEmpty()) browse.states else browse.states.filter { it.title.plain().substringBefore("(").trim().let { name -> name == wanted || name.startsWith(wanted) } }
                listOf(RadiosSection(null, states))
            }

            filter == RadiosFilter.City -> PlacesList(path = countryPath.orEmpty()) { listOf(RadiosSection(null, it.cities)) }

            else -> StationsList(path = countryPath.orEmpty().allStationsPath())
        }
    }
}

// A group of places under a title, which is none when the page has only one
data class RadiosSection(val titleRes: Int?, val entries: List<RadiosEntry>)

// The places of a page, each one opens the page that lists what is inside it
@Composable
internal fun PlacesList(path: String, sections: (RadiosBrowse) -> List<RadiosSection>) {
    val viewModel: RadiosPlacesViewModel = viewModel(key = "places:$path")
    val state by viewModel.state.collectAsState()
    LaunchedEffect(path) { viewModel.load(path) }

    val browse = state.browse
    when {
        browse == null && state.isLoading -> RowsSkeleton()

        browse == null -> HomeMessage(
            text = state.error?.takeIf { it.isNotEmpty() } ?: stringResource(R.string.home_error),
            actionLabel = stringResource(R.string.home_retry),
            onAction = viewModel::retry
        )

        else -> {
            val groups = sections(browse).filter { it.entries.isNotEmpty() }
            if (groups.isEmpty()) {
                HomeMessage(text = stringResource(R.string.radios_empty))
            } else {
                LazyColumn(contentPadding = PaddingValues(bottom = miniPlayerRoom())) {
                    groups.forEach { group ->
                        group.titleRes?.let { title ->
                            item(key = "title_$title") {
                                Text(
                                    text = stringResource(title),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onBackground,
                                    modifier = Modifier.padding(horizontal = WavvyTheme.dimens.screenPadding, vertical = RadiosDimens.SectionVertical)
                                )
                            }
                        }
                        items(group.entries, key = { it.path }) { entry -> EntryRow(entry) }
                    }
                }
            }
        }
    }
}

// A place, with an arrow that says it opens more
@Composable
private fun EntryRow(entry: RadiosEntry) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
            .clickable { ItemNavigator.openRadiosPlace(entry.title, entry.path) }
            .height(RadiosDimens.EntryHeight)
            .padding(horizontal = WavvyTheme.dimens.screenPadding)
    ) {
        Text(
            text = entry.title,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Icon(
            imageVector = WavvyIcons.NavigateNext,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(RadiosDimens.EntryArrow)
        )
    }
}

// The stations of a page of the site, read page by page as the list is scrolled, a tap plays the station live
@Composable
internal fun StationsList(path: String, filter: String = "") {
    val viewModel: RadiosViewModel = viewModel(key = "stations:$path")
    val state by viewModel.state.collectAsState()
    val listState = rememberLazyListState()
    LaunchedEffect(path) { viewModel.start(path) }

    // What was typed narrows the stations that are loaded, by their name, their place or their segment
    val wanted = filter.plain()
    val stations = if (wanted.isEmpty()) state.stations else state.stations.filter { station ->
        (listOf(station.name, station.place.orEmpty()) + station.segments).any { it.plain().contains(wanted) }
    }

    // The next page is asked when the end of the list is near
    LaunchedEffect(listState, stations.size) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0 }
            .collect { last -> if (last >= stations.size - RadiosDimens.LoadMoreMargin) viewModel.loadMore() }
    }

    // While something is typed the pages are read one after the other, up to a limit, so what is looked for is found
    LaunchedEffect(wanted, state.stations.size, state.isLoading) {
        if (wanted.isNotEmpty() && !state.isLoading && state.stations.size < minOf(state.total, RadiosDimens.FilterScanLimit)) viewModel.loadMore()
    }

    when {
        stations.isEmpty() && (state.isLoading || (wanted.isNotEmpty() && state.stations.size < minOf(state.total, RadiosDimens.FilterScanLimit))) -> RowsSkeleton()

        stations.isEmpty() && state.error == null -> HomeMessage(text = stringResource(R.string.radios_empty))

        stations.isEmpty() -> HomeMessage(
            text = state.error?.takeIf { it.isNotEmpty() } ?: stringResource(R.string.home_error),
            actionLabel = stringResource(R.string.home_retry),
            onAction = viewModel::loadMore
        )

        else -> LazyColumn(
            state = listState,
            modifier = Modifier.windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)),
            contentPadding = PaddingValues(bottom = miniPlayerRoom())
        ) {
            items(stations, key = { it.id }) { station ->
                StationRow(station = station, isOpening = state.opening == station.id, onClick = { viewModel.play(station) })
            }

            if (state.isLoading) {
                item(key = "more") {
                    Box(modifier = Modifier.fillMaxWidth().padding(vertical = RadiosDimens.FooterVertical), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(modifier = Modifier.size(RadiosDimens.Spinner), strokeWidth = RadiosDimens.SpinnerStroke)
                    }
                }
            }
        }
    }
}

// The mini player covers the end of a list, with a little more room so it does not touch the last row
@Composable
private fun miniPlayerRoom() = LocalMiniPlayerInset.current + if (LocalMiniPlayerInset.current > 0.dp) RadiosDimens.MiniPlayerClearance else 0.dp

// A station, its logo, its name, where it is and what it plays now, with a spinner while it opens
@Composable
private fun StationRow(station: RadioStation, isOpening: Boolean, onClick: () -> Unit) {
    val dimens = WavvyTheme.dimens
    val place = (listOfNotNull(station.place) + station.segments.take(1)).joinToString(" $Separator ")

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = dimens.screenPadding, vertical = RadiosDimens.RowVertical)
    ) {
        AsyncImage(
            model = station.logoUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(RadiosDimens.Logo)
                .clip(RoundedCornerShape(RadiosDimens.LogoCorner))
                .background(WavvyTheme.colors.chip)
        )

        Column(verticalArrangement = Arrangement.spacedBy(dimens.spaceExtraSmall), modifier = Modifier.weight(1f).padding(start = dimens.spaceMedium)) {
            Text(
                text = station.name,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (place.isNotEmpty()) {
                Text(
                    text = place,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            station.onAir?.let { onAir ->
                Text(
                    text = onAir,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        if (isOpening) {
            CircularProgressIndicator(
                modifier = Modifier.padding(start = dimens.spaceMedium).size(RadiosDimens.Spinner),
                strokeWidth = RadiosDimens.SpinnerStroke
            )
        }
    }
}

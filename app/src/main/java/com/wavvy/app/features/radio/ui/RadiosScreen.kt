package com.wavvy.app.features.radio.ui

// Android address and text
import android.net.Uri
import java.text.Collator
// Compose layouts and foundations
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
// Material 3 components
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
// Compose state and runtime
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
// UI styling and utilities
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
// Lifecycle and images
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
// Project resources
import com.wavvy.app.R
import com.wavvy.app.core.designsystem.components.OverlaySheet
import com.wavvy.app.core.designsystem.components.rememberBackdropFade
import com.wavvy.app.core.designsystem.icons.WavvyIcons
import com.wavvy.app.core.designsystem.theme.WavvyMotion
import com.wavvy.app.core.designsystem.theme.WavvyTheme
import com.wavvy.app.core.navigation.ItemNavigator
import com.wavvy.app.core.playback.PlayerConnection
import com.wavvy.app.core.radio.RadioIdPrefix
import com.wavvy.app.core.radio.RadioLibrary
import com.wavvy.app.core.radio.RadioSegment
import com.wavvy.app.core.radio.RadioStation
import com.wavvy.app.core.radio.RadiosBrowse
import com.wavvy.app.core.radio.RadiosEntry
import com.wavvy.app.core.radio.RadiosReader
import com.wavvy.app.core.radio.allStationsPath
import com.wavvy.app.core.radio.isRadioTrack
import com.wavvy.app.core.radio.plain
import com.wavvy.app.core.radio.userRadiosCountry
import com.wavvy.app.features.discover.ui.DiscoverDimens
import com.wavvy.app.features.discover.ui.DiscoverShelfTitle
import com.wavvy.app.features.discover.ui.ExploreBar
import com.wavvy.app.features.discover.ui.StripeButton
import com.wavvy.app.features.home.data.Separator
import com.wavvy.app.features.home.ui.components.HomeMessage
import com.wavvy.app.features.player.ui.LocalMiniPlayerInset
import com.wavvy.app.features.podcast.ui.HubBackdrop
import com.wavvy.app.features.search.ui.components.SearchBar

// The radios, the one that plays now, the favorites and the ones listened to lately, the genres as buttons with a stripe and the places, the search opens over all of it
@Composable
fun RadiosScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val listState = rememberLazyListState()
    val player = rememberRadioPlayer()
    val favorites by remember { RadioLibrary.favorites(context) }.collectAsState(initial = emptyList())
    val recents by remember { RadioLibrary.recents(context) }.collectAsState(initial = emptyList())
    val playing by PlayerConnection.track.collectAsState()
    val isPlaying by PlayerConnection.isPlaying.collectAsState()

    // The country of the user in the site, nothing while it is looked for
    val country by produceState<RadiosEntry?>(initialValue = null) { value = userRadiosCountry(context) }

    // The search, what is typed and what was searched, which the list follows, the field changes with every letter and the list waits for the search
    var isSearching by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    var submitted by rememberSaveable { mutableStateOf("") }
    BackHandler(enabled = isSearching) {
        isSearching = false
        query = ""
        submitted = ""
    }

    val station = playing?.takeIf { isRadioTrack(it.id) }

    // The last one that played, which is still drawn while the card closes
    val lastStation = remember { mutableStateOf(station) }
    SideEffect { if (station != null) lastStation.value = station }
    val playingId = station?.id?.removePrefix(RadioIdPrefix)

    // How tall the top of the page is, the card of the one that plays and the places, so the color goes as that part scrolls away, as the charts have it
    var nowHeight by remember { mutableIntStateOf(0) }
    var placesHeight by remember { mutableIntStateOf(0) }
    val backdropFade = rememberBackdropFade(listState) { (nowHeight + placesHeight) * DiscoverDimens.HubBackdropFade }

    // The arrow and the title stay in place, the back of the bar comes in as the page goes under it
    val barScroll = with(LocalDensity.current) { DiscoverDimens.HubBarScroll.toPx() }
    val barAlpha by remember {
        derivedStateOf { if (listState.firstVisibleItemIndex > 0) 1f else (listState.firstVisibleItemScrollOffset / barScroll).coerceIn(0f, 1f) }
    }
    val topInset = WindowInsets.safeDrawing.only(WindowInsetsSides.Top).asPaddingValues().calculateTopPadding()

    // Solid, so the screen under it does not show through while the page slides
    Box(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        HubBackdrop(
            coverUrl = station?.artworkUrl ?: favorites.firstOrNull()?.largeLogoUrl ?: recents.firstOrNull()?.largeLogoUrl,
            accent = WavvyTheme.colors.playerAccent,
            fade = backdropFade,
            modifier = Modifier.align(Alignment.TopStart)
        )

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = topInset + DiscoverDimens.BarHeight + DiscoverDimens.HubBarGap, bottom = miniPlayerRoom())
        ) {
            // Always on the list, so the first item is never pushed away and the color of the top does not go off, it opens and closes with an animation
            item(key = "now") {
                AnimatedVisibility(
                    visible = station != null,
                    modifier = Modifier.onSizeChanged { nowHeight = it.height },
                    enter = fadeIn(tween(WavvyMotion.ExpandMillis)) + expandVertically(tween(WavvyMotion.ExpandMillis)),
                    exit = fadeOut(tween(WavvyMotion.ExpandMillis)) + shrinkVertically(tween(WavvyMotion.ExpandMillis))
                ) {
                    val shown = station ?: lastStation.value
                    shown?.let { NowPlaying(name = it.title, place = it.artist, logoUrl = it.artworkUrl, isPlaying = isPlaying, onClick = PlayerConnection::togglePlayPause) }
                }
            }

            // Always on the list, even before the country is known, so what comes after it never pushes the page down to the genres
            item(key = "places") { PlacesSection(country = country, modifier = Modifier.onSizeChanged { placesHeight = it.height }) }

            if (favorites.isNotEmpty()) {
                item(key = "favorites") { StationShelf(title = stringResource(R.string.radios_favorites), stations = favorites, playingId = playingId, player = player, modifier = Modifier.animateItem()) }
            }
            if (recents.isNotEmpty()) {
                item(key = "recents") { StationShelf(title = stringResource(R.string.radios_recents), stations = recents, playingId = playingId, player = player, modifier = Modifier.animateItem()) }
            }

            item(key = "genres") { GenreSection(modifier = Modifier.animateItem()) }
        }

        ExploreBar(
            title = stringResource(R.string.radios_page_title),
            titleAlpha = 1f,
            onBack = onBack,
            backgroundAlpha = barAlpha,
            trailing = {
                IconButton(onClick = { isSearching = true }) {
                    Icon(imageVector = WavvyIcons.Search, contentDescription = stringResource(R.string.radios_search_hint), tint = MaterialTheme.colorScheme.onBackground)
                }
            }
        )

        if (isSearching) {
            Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                SearchBar(
                    query = query,
                    onQueryChange = {
                        query = it
                        if (it.isEmpty()) submitted = ""
                    },
                    onSearch = { submitted = query.trim() },
                    onBack = {
                        isSearching = false
                        query = ""
                        submitted = ""
                    },
                    onFocusChange = {},
                    hint = stringResource(R.string.radios_search_hint)
                )

                RadiosSearch(query = query, submitted = submitted, countryPath = country?.path)
            }
        }
    }
}

// The station that plays now, its logo, the words that say it is live, its name and where it is, and the button that pauses it and plays it again
@Composable
private fun NowPlaying(name: String, place: String?, logoUrl: String?, isPlaying: Boolean, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
            .padding(horizontal = DiscoverDimens.Side)
            .padding(bottom = RadiosDimens.SectionBottom)
            .fillMaxWidth()
            .clip(RoundedCornerShape(RadiosDimens.NowCorner))
            .background(WavvyTheme.colors.menuCard)
            .padding(RadiosDimens.NowPadding)
    ) {
        AsyncImage(
            model = logoUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(RadiosDimens.NowLogo)
                .clip(RoundedCornerShape(RadiosDimens.LogoCorner))
                .background(WavvyTheme.colors.chip)
        )

        Column(modifier = Modifier.weight(1f).padding(horizontal = RadiosDimens.NowPadding)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(RadiosDimens.LiveDot).clip(CircleShape).background(WavvyTheme.colors.playerAccent))
                Text(
                    text = stringResource(R.string.radios_now_playing),
                    style = MaterialTheme.typography.labelMedium,
                    color = WavvyTheme.colors.playerAccent,
                    modifier = Modifier.padding(start = RadiosDimens.LiveGap)
                )
            }
            Text(
                text = name,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            place?.let {
                Text(text = it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(RadiosDimens.NowButton)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.onBackground)
                .clickable(onClick = onClick)
        ) {
            Icon(
                imageVector = if (isPlaying) WavvyIcons.Pause else WavvyIcons.PlayArrow,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.background,
                modifier = Modifier.size(RadiosDimens.NowButtonIcon)
            )
        }
    }
}

// A row of stations that slides sideways, a tap plays the station and a long press opens its menu
@Composable
private fun StationShelf(title: String, stations: List<RadioStation>, playingId: String?, player: RadioPlayer, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth().padding(bottom = RadiosDimens.SectionBottom)) {
        DiscoverShelfTitle(title = title, link = null)

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(DiscoverDimens.Gap),
            contentPadding = PaddingValues(horizontal = DiscoverDimens.Side),
            modifier = Modifier.windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
        ) {
            items(stations, key = { it.id }) { station ->
                StationCard(station = station, isCurrent = station.id == playingId, isOpening = player.opening == station.id, onClick = { player.play(station) })
            }
        }
    }
}

// A station as a card, the logo as big as the card, the name in up to two lines and where it is, the mark of the one that plays now
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun StationCard(station: RadioStation, isCurrent: Boolean, isOpening: Boolean, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(RadiosDimens.CardWidth)
            .combinedClickable(onClick = onClick, onLongClick = { OverlaySheet.show { RadioMenu(station = station, onDismiss = OverlaySheet::dismiss) } })
    ) {
        Box(
            modifier = Modifier
                .size(RadiosDimens.CardWidth)
                .clip(RoundedCornerShape(RadiosDimens.CardCorner))
                .background(WavvyTheme.colors.chip)
        ) {
            AsyncImage(model = station.logoUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())

            if (isOpening) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center).size(RadiosDimens.Spinner), strokeWidth = RadiosDimens.SpinnerStroke)
            } else if (isCurrent) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(RadiosDimens.CardBadgeInset)
                        .size(RadiosDimens.CardBadge)
                        .clip(CircleShape)
                        .background(WavvyTheme.colors.playerAccent)
                ) {
                    Icon(imageVector = WavvyIcons.Radio, contentDescription = null, tint = MaterialTheme.colorScheme.background, modifier = Modifier.size(RadiosDimens.CardBadgeIcon))
                }
            }
        }

        Text(
            text = station.name,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = if (isCurrent) WavvyTheme.colors.playerAccent else MaterialTheme.colorScheme.onBackground,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = RadiosDimens.CardTextGap)
        )
        station.place?.let {
            Text(text = it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

// All the genres as buttons in as many columns as fit, in the order of the names in the language of the app
@Composable
private fun GenreSection(modifier: Modifier = Modifier) {
    val genres = rememberRadioGenres()

    Column(modifier = modifier.fillMaxWidth().padding(bottom = RadiosDimens.SectionBottom)) {
        DiscoverShelfTitle(title = stringResource(R.string.radios_genres), link = null)

        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val columns = ((maxWidth - DiscoverDimens.Side * 2 + DiscoverDimens.Gap) / (DiscoverDimens.MoodMinWidth + DiscoverDimens.Gap)).toInt().coerceAtLeast(2)

            Column(
                verticalArrangement = Arrangement.spacedBy(DiscoverDimens.Gap),
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
                    .padding(horizontal = DiscoverDimens.Side)
            ) {
                genres.sorted.chunked(columns).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(DiscoverDimens.Gap)) {
                        row.forEach { (segment, title) -> GenreButton(segment = segment, title = title, modifier = Modifier.weight(1f)) }
                        // A row with fewer buttons leaves the room of the others
                        if (row.size < columns) Spacer(modifier = Modifier.weight((columns - row.size).toFloat()))
                    }
                }
            }
        }
    }
}

// A genre, its stripe has the color the genre always has, a tap opens the stations of it
@Composable
private fun GenreButton(segment: RadioSegment, title: String, modifier: Modifier = Modifier) {
    StripeButton(title = title, color = RadiosColors.genre(segment.id), onClick = { ItemNavigator.openRadiosPlace(title, segment.path) }, modifier = modifier)
}

// The places of the site as two big cards side by side, half of the width each, the states and the cities of the country of the user and every station of it
@Composable
private fun PlacesSection(country: RadiosEntry?, modifier: Modifier = Modifier) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(DiscoverDimens.Gap),
        modifier = modifier
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
            .padding(horizontal = DiscoverDimens.Side)
            .padding(bottom = RadiosDimens.SectionBottom)
    ) {
        PlaceCard(
            icon = WavvyIcons.Public,
            title = stringResource(R.string.radios_places_button),
            subtitle = country?.title.orEmpty(),
            onClick = { country?.let { ItemNavigator.openRadiosPlace(it.title, it.path) } },
            modifier = Modifier.weight(1f)
        )
        PlaceCard(
            icon = WavvyIcons.Radio,
            title = stringResource(R.string.radios_all_button),
            subtitle = country?.title.orEmpty(),
            onClick = { country?.let { ItemNavigator.openRadiosPlace(it.title, it.path.allStationsPath()) } },
            modifier = Modifier.weight(1f)
        )
    }
}

// A big card with an icon in the color of the accent, what it opens and where
@Composable
private fun PlaceCard(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        verticalArrangement = Arrangement.SpaceBetween,
        modifier = modifier
            .height(RadiosDimens.PlaceHeight)
            .clip(RoundedCornerShape(RadiosDimens.PlaceCorner))
            .background(WavvyTheme.colors.menuCard)
            .clickable(onClick = onClick)
            .padding(RadiosDimens.PlacePadding)
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = WavvyTheme.colors.playerAccent, modifier = Modifier.size(RadiosDimens.PlaceIcon))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

// What the search finds, the genres and the places that have the words as buttons that slide, and under them the stations of the site that have them in the name
@Composable
private fun RadiosSearch(query: String, submitted: String, countryPath: String?) {
    val context = LocalContext.current
    val wanted = query.plain()

    // The states and the cities of the country, which are looked at here without asking the site again
    val browse by produceState<RadiosBrowse?>(initialValue = null, countryPath) { value = countryPath?.let { RadiosReader.browse(context, it).getOrNull() } }
    val allGenres = rememberRadioGenres()
    val genres = remember(wanted, allGenres) { if (wanted.isEmpty()) emptyList() else allGenres.sorted.filter { (_, title) -> title.plain().contains(wanted) }.take(RadiosDimens.MaxMatches) }
    val places = remember(wanted, browse) {
        if (wanted.isEmpty()) emptyList() else (browse?.states.orEmpty() + browse?.cities.orEmpty()).filter { it.title.plain().contains(wanted) }.take(RadiosDimens.MaxMatches)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        if (genres.isNotEmpty() || places.isNotEmpty()) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(DiscoverDimens.Gap),
                contentPadding = PaddingValues(horizontal = DiscoverDimens.Side, vertical = RadiosDimens.MatchesVertical),
                modifier = Modifier.windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
            ) {
                items(genres, key = { (segment, _) -> "genre_${segment.id}" }) { (segment, title) -> GenreButton(segment = segment, title = title, modifier = Modifier.width(RadiosDimens.MatchWidth)) }
                items(places, key = { "place_${it.path}" }) { place ->
                    StripeButton(
                        title = place.title,
                        color = RadiosColors.Match,
                        onClick = { ItemNavigator.openRadiosPlace(place.title, place.path) },
                        modifier = Modifier.width(RadiosDimens.MatchWidth)
                    )
                }
            }
        }

        Box(modifier = Modifier.weight(1f)) {
            if (submitted.isNotEmpty()) {
                StationsList(path = "/busca?q=${Uri.encode(submitted)}&qfilter=nome")
            } else {
                HomeMessage(text = stringResource(R.string.radios_search_tip))
            }
        }
    }
}

// How the places are compared, by the rules of the language of the app
@Composable
private fun rememberPlacesCollator(): Collator {
    val locale = LocalConfiguration.current.locales[0]
    return remember(locale) { Collator.getInstance(locale).apply { strength = Collator.PRIMARY } }
}

// The name of a place without the letters of a state after it, so Sergipe (SE) is put where Sergipe goes
private fun placeKey(title: String): String = title.substringBefore("(").trim()

// A group of places under a title, which is none when the page has only one, the color of the stripe of their buttons, and whether each one takes the whole width
data class RadiosSection(val titleRes: Int?, val entries: List<RadiosEntry>, val color: Long, val isFull: Boolean = false)

// The places of a page as buttons with a stripe in as many columns as fit, each one opens the page that lists what is inside it
@Composable
internal fun PlacesList(path: String, sections: (RadiosBrowse) -> List<RadiosSection>) {
    val viewModel: RadiosPlacesViewModel = viewModel(key = "places:$path")
    val state by viewModel.state.collectAsState()
    LaunchedEffect(path) { viewModel.load(path) }

    val browse = state.browse
    when {
        browse == null && state.isLoading -> PlacesSkeleton()

        browse == null -> HomeMessage(
            text = state.error?.takeIf { it.isNotEmpty() } ?: stringResource(R.string.home_error),
            actionLabel = stringResource(R.string.home_retry),
            onAction = viewModel::retry
        )

        else -> {
            // The states, the capitals and the cities in the order of their names in the language of the app, the link to every station stays where it is
            val collator = rememberPlacesCollator()
            val groups = sections(browse).filter { it.entries.isNotEmpty() }.map { group ->
                if (group.isFull) group else group.copy(entries = group.entries.sortedWith { a, b -> collator.compare(placeKey(a.title), placeKey(b.title)) })
            }
            if (groups.isEmpty()) {
                HomeMessage(text = stringResource(R.string.radios_empty))
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(DiscoverDimens.MoodMinWidth),
                    horizontalArrangement = Arrangement.spacedBy(DiscoverDimens.Gap),
                    verticalArrangement = Arrangement.spacedBy(DiscoverDimens.Gap),
                    contentPadding = PaddingValues(start = DiscoverDimens.Side, end = DiscoverDimens.Side, top = RadiosDimens.SectionVertical, bottom = miniPlayerRoom()),
                    modifier = Modifier.windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
                ) {
                    groups.forEach { group ->
                        group.titleRes?.let { title ->
                            item(key = "title_$title", span = { GridItemSpan(maxLineSpan) }) {
                                Text(
                                    text = stringResource(title),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onBackground,
                                    modifier = Modifier.padding(top = RadiosDimens.SectionVertical)
                                )
                            }
                        }
                        gridItems(group.entries, key = { it.path }, span = { if (group.isFull) GridItemSpan(maxLineSpan) else GridItemSpan(1) }) { entry ->
                            StripeButton(title = entry.title, color = group.color, onClick = { ItemNavigator.openRadiosPlace(entry.title, entry.path) })
                        }
                    }
                }
            }
        }
    }
}

// The stations of a page of the site, read page by page as the list is scrolled, a tap plays the station live
@Composable
internal fun StationsList(path: String, filter: String = "") {
    val context = LocalContext.current
    val viewModel: RadiosViewModel = viewModel(key = "stations:$path")
    val state by viewModel.state.collectAsState()
    val listState = rememberLazyListState()
    val player = rememberRadioPlayer()
    val playing by PlayerConnection.track.collectAsState()
    val isPlaying by PlayerConnection.isPlaying.collectAsState()
    val favorites by remember { RadioLibrary.favorites(context) }.collectAsState(initial = emptyList())
    val favoriteIds = remember(favorites) { favorites.map { it.id }.toSet() }
    LaunchedEffect(path) { viewModel.start(path) }

    // What was typed narrows the stations that are loaded, by their name, their place or their segment
    val wanted = filter.plain()
    val genres = rememberRadioGenres()
    val found = if (wanted.isEmpty()) state.stations else state.stations.filter { station ->
        (listOf(station.name, station.place.orEmpty()) + station.segments + station.segments.map(genres::titleOf)).any { it.plain().contains(wanted) }
    }
    val isWhole = state.pages == 1 && state.total > 0 && state.stations.size >= state.total
    val stations = rememberSortedStations(found, isWhole)

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
        stations.isEmpty() && (state.isLoading || (wanted.isNotEmpty() && state.stations.size < minOf(state.total, RadiosDimens.FilterScanLimit))) -> StationsSkeleton()

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
            // How many stations the list has, which tells the user how far the list goes
            if (wanted.isEmpty() && state.total > 0) {
                item(key = "count") {
                    Text(
                        text = pluralStringResource(R.plurals.radios_count, state.total, state.total),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = WavvyTheme.dimens.screenPadding, vertical = RadiosDimens.CountVertical)
                    )
                }
            }

            items(stations, key = { it.id }) { station ->
                StationRow(
                    modifier = Modifier.animateItem(fadeInSpec = tween(WavvyMotion.ScreenFadeMillis), placementSpec = null, fadeOutSpec = null),
                    station = station,
                    isCurrent = playing?.id == RadioIdPrefix + station.id,
                    isPlaying = isPlaying,
                    isOpening = player.opening == station.id,
                    isFavorite = station.id in favoriteIds,
                    genre = station.segments.firstOrNull()?.let(genres::titleOf),
                    onClick = { player.play(station) },
                    onMenu = { OverlaySheet.show { RadioMenu(station = station, onDismiss = OverlaySheet::dismiss) } }
                )
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

// A station, its logo, its name with a heart when it is a favorite, where it is and what it plays now, the one that plays is in the color of the accent, with the menu at the end
@Composable
private fun StationRow(
    modifier: Modifier,
    station: RadioStation,
    isCurrent: Boolean,
    isPlaying: Boolean,
    isOpening: Boolean,
    isFavorite: Boolean,
    genre: String?,
    onClick: () -> Unit,
    onMenu: () -> Unit
) {
    val dimens = WavvyTheme.dimens
    val place = listOfNotNull(station.place, genre).joinToString(" $Separator ")
    val accent = WavvyTheme.colors.playerAccent

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(start = dimens.screenPadding, top = RadiosDimens.RowVertical, bottom = RadiosDimens.RowVertical)
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
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = station.name,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Normal),
                    color = if (isCurrent) accent else MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (isFavorite) {
                    Icon(
                        imageVector = WavvyIcons.Favorite,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.padding(start = RadiosDimens.LiveGap).size(RadiosDimens.FavoriteMark)
                    )
                }
            }
            if (place.isNotEmpty()) {
                Text(text = place, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            station.onAir?.let { onAir ->
                Text(
                    text = stringResource(R.string.radios_on_air, onAir),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        when {
            isOpening -> CircularProgressIndicator(
                modifier = Modifier.padding(horizontal = dimens.spaceMedium).size(RadiosDimens.Spinner),
                strokeWidth = RadiosDimens.SpinnerStroke
            )
            isCurrent && isPlaying -> Icon(imageVector = WavvyIcons.Radio, contentDescription = null, tint = accent, modifier = Modifier.padding(horizontal = dimens.spaceMedium).size(RadiosDimens.FavoriteMark))
        }

        IconButton(onClick = onMenu) {
            Icon(imageVector = WavvyIcons.MoreVertical, contentDescription = stringResource(R.string.cd_more), tint = MaterialTheme.colorScheme.onBackground)
        }
    }
}

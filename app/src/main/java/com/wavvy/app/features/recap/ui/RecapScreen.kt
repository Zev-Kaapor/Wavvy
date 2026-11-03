package com.wavvy.app.features.recap.ui

// Animation
// Compose layouts and foundations
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
// Material 3 components
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
// Compose state and runtime
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
// UI styling and utilities
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
// Time
import java.time.LocalDate
import java.time.Month
import java.time.format.TextStyle
import java.util.Locale
// Project resources
import com.wavvy.app.R
import com.wavvy.app.core.designsystem.components.OverlaySheet
import com.wavvy.app.core.designsystem.icons.WavvyIcons
import com.wavvy.app.core.designsystem.theme.WavvyTheme
import com.wavvy.app.core.history.TopSong
import com.wavvy.app.core.navigation.ItemNavigator
import com.wavvy.app.features.home.data.HomeItem
import com.wavvy.app.features.home.data.HomeItemKind
import com.wavvy.app.features.home.ui.components.HomeListItem
import com.wavvy.app.features.home.ui.rememberListPlayer
import com.wavvy.app.features.library.ui.Chip
import com.wavvy.app.features.player.ui.LocalMiniPlayerInset
import com.wavvy.app.features.profile.ui.PlayCountBadge
import com.wavvy.app.features.profile.ui.ProfileRanking
import com.wavvy.app.features.profile.ui.RowsSkeleton
import com.wavvy.app.features.profile.ui.Section
import com.wavvy.app.features.profile.ui.TopArtist
import com.wavvy.app.features.profile.ui.durationText
import com.wavvy.app.features.recap.data.Recap
import com.wavvy.app.features.recap.data.RecapPart
import com.wavvy.app.features.recap.data.RecapRepository
import com.wavvy.app.features.search.data.SearchRepository

// Milliseconds in an hour
private const val MillisPerHour = 3_600_000L

// All the numbers of the year of listening in a page to scroll, the hours, the months, the artists, the songs, the genres and the albums
@Composable
fun RecapDetailsScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val dimens = WavvyTheme.dimens
    val playList = rememberListPlayer()
    var year by rememberSaveable { mutableIntStateOf(LocalDate.now().year) }
    val years by produceState(initialValue = emptyList<Int>()) { value = RecapRepository.years(context) }
    // What counts changes here, so the numbers are worked out again
    var reload by rememberSaveable { mutableIntStateOf(0) }
    var part by rememberSaveable { mutableStateOf(RecapPart.Year) }
    val recap by produceState<Recap?>(initialValue = null, year, part, reload) {
        value = RecapRepository.load(context, year, part)
    }

    // The pictures of the artists come after the list, the ones already known show at once
    val artists by produceState(initialValue = emptyList<TopArtist>(), recap) {
        val first = recap?.artists?.take(RecapDimens.FullCount)?.let { ProfileRanking.withKnownPhotos(context, it) } ?: return@produceState
        value = first
        val looked = ProfileRanking.withPhotos(context, SearchRepository(context), first.filterNot { it.isLookedUp })
        value = first.map { artist -> looked.firstOrNull { it.name == artist.name } ?: artist }
    }

    var isArtistsOpen by rememberSaveable { mutableStateOf(false) }
    var isSongsOpen by rememberSaveable { mutableStateOf(false) }
    var isAlbumsOpen by rememberSaveable { mutableStateOf(false) }

    LazyColumn(
        contentPadding = PaddingValues(bottom = LocalMiniPlayerInset.current + RecapDimens.ScreenBottom),
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        item(key = "bar") {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))
                    .padding(horizontal = dimens.spaceSmall)
            ) {
                IconButton(onClick = onBack) {
                    Icon(imageVector = WavvyIcons.Back, contentDescription = stringResource(R.string.cd_back), tint = MaterialTheme.colorScheme.onBackground)
                }
                Text(
                    text = stringResource(R.string.recap_title),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // The years there is something of, only when there is more than one
        if (years.size > 1) {
            item(key = "years") {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(dimens.spaceSmall),
                    contentPadding = PaddingValues(horizontal = dimens.screenPadding),
                    modifier = Modifier.padding(top = RecapDimens.SectionGap)
                ) {
                    items(years) { option -> Chip(text = option.toString(), isOn = option == year) { year = option } }
                }
            }
        }

        // The stretch of the year, the whole of it or half of it or a quarter
        item(key = "parts") {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(dimens.spaceSmall),
                contentPadding = PaddingValues(horizontal = dimens.screenPadding),
                modifier = Modifier.padding(top = RecapDimens.SectionGap)
            ) {
                items(RecapPart.entries) { option -> Chip(text = stringResource(option.labelRes), isOn = option == part) { part = option } }
            }
        }

        // What counts in the numbers can be changed from here, and the same year can be seen as a story
        item(key = "filter") {
            Row(modifier = Modifier.padding(horizontal = dimens.spaceSmall)) {
                TextButton(onClick = { OverlaySheet.show { RecapFilterSheet(year = year, onChanged = { reload++ }, onDismiss = OverlaySheet::dismiss) } }) {
                    Text(stringResource(R.string.recap_filter_button))
                }
                TextButton(onClick = { ItemNavigator.openRecap(year, part.name) }) { Text(stringResource(R.string.recap_story_button)) }
            }
        }

        val current = recap
        if (current == null) {
            item(key = "loading") { RowsSkeleton(rows = RecapDimens.PreviewCount) }
        } else if (current.isEmpty) {
            item(key = "empty") {
                Text(
                    text = stringResource(R.string.recap_empty, current.year),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = dimens.screenPadding, vertical = RecapDimens.HeroTop * 2)
                )
            }
        } else {
            item(key = "hero") { Hero(current) }
            item(key = "months") {
                Section(title = stringResource(R.string.recap_months)) { Months(current) }
            }

            // The artists, the first ones and then up to the full list
            if (artists.isNotEmpty()) {
                item(key = "artists") {
                    Section(title = stringResource(R.string.recap_top_artists)) {
                        val shown = artists.take(if (isArtistsOpen) RecapDimens.FullCount else RecapDimens.PreviewCount)
                        Column {
                            shown.forEachIndexed { index, artist ->
                                val item = HomeItem(kind = HomeItemKind.Artist, id = artist.id ?: artist.name, title = artist.name, thumbnailUrl = artist.picture)
                                HomeListItem(
                                    item = item,
                                    onClick = { ItemNavigator.open(item) },
                                    rank = index + 1,
                                    trailing = { PlayCountBadge(artist.plays) },
                                    accent = durationText(artist.totalMs)
                                )
                            }
                            MoreButton(isOpen = isArtistsOpen, hasMore = artists.size > RecapDimens.PreviewCount) { isArtistsOpen = !isArtistsOpen }
                        }
                    }
                }
            }

            item(key = "songs") {
                Section(title = stringResource(R.string.recap_top_songs)) {
                    val rows = current.songs.take(RecapDimens.FullCount).map { it.toItem() }
                    val shown = rows.take(if (isSongsOpen) RecapDimens.FullCount else RecapDimens.PreviewCount)
                    Column {
                        shown.forEachIndexed { index, item ->
                            val top = current.songs[index]
                            HomeListItem(
                                item = item,
                                onClick = { playList(rows, index, null) },
                                rank = index + 1,
                                trailing = { PlayCountBadge(top.plays) },
                                accent = durationText(top.total)
                            )
                        }
                        MoreButton(isOpen = isSongsOpen, hasMore = rows.size > RecapDimens.PreviewCount) { isSongsOpen = !isSongsOpen }
                    }
                }
            }

            if (current.genres.isNotEmpty()) {
                item(key = "genres") {
                    Section(title = stringResource(R.string.recap_genres)) {
                        val top = current.genres.take(RecapDimens.PreviewCount)
                        val peak = top.first().totalMs.coerceAtLeast(1L)
                        Column(
                            verticalArrangement = Arrangement.spacedBy(RecapDimens.GenreGap),
                            modifier = Modifier.padding(horizontal = dimens.screenPadding, vertical = RecapDimens.SectionGap)
                        ) {
                            top.forEach { genre ->
                                Column(verticalArrangement = Arrangement.spacedBy(RecapDimens.GenreBarGap)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = genre.name.replaceFirstChar { it.titlecase(Locale.getDefault()) },
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = MaterialTheme.colorScheme.onBackground,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Text(
                                            text = durationText(genre.totalMs),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(RecapDimens.GenreBar)
                                            .clip(RoundedCornerShape(RecapDimens.GenreBarCorner))
                                            .background(MaterialTheme.colorScheme.onBackground.copy(alpha = RecapDimens.GenreTrackAlpha))
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxHeight()
                                                .fillMaxWidth(genre.totalMs.toFloat() / peak)
                                                .background(MaterialTheme.colorScheme.primary)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (current.albums.isNotEmpty()) {
                item(key = "albums") {
                    Section(title = stringResource(R.string.recap_albums)) {
                        val shown = current.albums.take(if (isAlbumsOpen) RecapDimens.FullCount else RecapDimens.PreviewCount)
                        Column {
                            shown.forEachIndexed { index, album ->
                                val item = HomeItem(kind = HomeItemKind.Album, id = album.id, title = album.name, thumbnailUrl = album.cover)
                                HomeListItem(
                                    item = item,
                                    onClick = { ItemNavigator.openAlbum(album.id) },
                                    rank = index + 1,
                                    trailing = { PlayCountBadge(album.plays) },
                                    accent = durationText(album.totalMs)
                                )
                            }
                            MoreButton(isOpen = isAlbumsOpen, hasMore = current.albums.size > RecapDimens.PreviewCount) { isAlbumsOpen = !isAlbumsOpen }
                        }
                    }
                }
            }

            // Genres and albums are written down while the songs are listened to, so a year with few of them says so
            if (current.genres.isEmpty() && current.albums.isEmpty()) {
                item(key = "hint") {
                    Text(
                        text = stringResource(R.string.recap_hint),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = dimens.screenPadding, vertical = RecapDimens.StatTop)
                    )
                }
            }
        }
    }
}

// The hours of the year, big, with the numbers that come with them
@Composable
private fun Hero(recap: Recap) {
    val dimens = WavvyTheme.dimens
    val hours = (recap.totalMs / MillisPerHour).toInt()

    Column(modifier = Modifier.padding(horizontal = dimens.screenPadding).padding(top = RecapDimens.HeroTop)) {
        Text(
            text = hours.toString(),
            style = MaterialTheme.typography.displayLarge.copy(fontSize = RecapDimens.HeroNumberSize, lineHeight = RecapDimens.HeroNumberLine, fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = pluralStringResource(R.plurals.recap_hours, hours, recap.year),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(top = RecapDimens.HeroCaptionGap)
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(RecapDimens.StatGap),
            modifier = Modifier.padding(top = RecapDimens.StatTop)
        ) {
            Stat(recap.listens, R.string.recap_stat_plays)
            Stat(recap.songs.size, R.string.recap_stat_songs)
            Stat(recap.artists.size, R.string.recap_stat_artists)
            Stat(recap.days, R.string.recap_stat_days)
        }
    }
}

// One number of the year with its name under it
@Composable
private fun Stat(value: Int, label: Int) {
    Column {
        Text(text = value.toString(), style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onBackground)
        Text(text = stringResource(label), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

// A bar for each month, the biggest one in the color of the app, and a line that names it
@Composable
private fun Months(recap: Recap) {
    val dimens = WavvyTheme.dimens
    val locale = Locale.getDefault()
    val peak = recap.months.max().coerceAtLeast(1L)
    val peakIndex = recap.months.indexOf(recap.months.max())

    Column(modifier = Modifier.padding(horizontal = dimens.screenPadding, vertical = RecapDimens.SectionGap)) {
        Row(modifier = Modifier.fillMaxWidth().height(RecapDimens.ChartHeight), verticalAlignment = Alignment.Bottom) {
            recap.months.forEachIndexed { index, total ->
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f).fillMaxHeight()) {
                    Box(contentAlignment = Alignment.BottomCenter, modifier = Modifier.weight(1f).fillMaxWidth()) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight((total.toFloat() / peak).coerceAtLeast(RecapDimens.BarMinFraction))
                                .fillMaxWidth(RecapDimens.BarWidthFraction)
                                .clip(RoundedCornerShape(RecapDimens.BarCorner))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = if (index == peakIndex) 1f else RecapDimens.BarAlpha))
                        )
                    }
                    Spacer(Modifier.height(RecapDimens.BarLabelGap))
                    Text(
                        text = Month.of(index + 1).getDisplayName(TextStyle.NARROW, locale).uppercase(locale),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Text(
            text = stringResource(R.string.recap_peak, Month.of(peakIndex + 1).getDisplayName(TextStyle.FULL_STANDALONE, locale).replaceFirstChar { it.titlecase(locale) }, durationText(peak)),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = RecapDimens.PeakTop)
        )
    }
}

// Opens the rest of a list and closes it again, nothing when the list has no more than the first places
@Composable
private fun MoreButton(isOpen: Boolean, hasMore: Boolean, onClick: () -> Unit) {
    if (!hasMore) return

    TextButton(onClick = onClick, modifier = Modifier.padding(start = WavvyTheme.dimens.spaceSmall, top = RecapDimens.MoreTop)) {
        Text(stringResource(if (isOpen) R.string.recap_less else R.string.recap_more))
    }
}

// A song of the ranking as a row of the lists
private fun TopSong.toItem(): HomeItem =
    HomeItem(
        kind = HomeItemKind.Song,
        id = song.id,
        title = song.title,
        thumbnailUrl = song.artworkUrl,
        artists = listOfNotNull(song.artist)
    )

// The card of the profile that leads to the retrospective
@Composable
fun RecapCard(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val dimens = WavvyTheme.dimens

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .padding(horizontal = dimens.screenPadding)
            .fillMaxWidth()
            .clip(RoundedCornerShape(RecapDimens.ProfileCardCorner))
            .background(MaterialTheme.colorScheme.primary.copy(alpha = RecapDimens.ProfileCardAlpha))
            .clickable(onClick = onClick)
            .padding(RecapDimens.ProfileCardPadding)
    ) {
        Icon(imageVector = WavvyIcons.History, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.width(RecapDimens.ProfileCardIcon).height(RecapDimens.ProfileCardIcon))
        Column(modifier = Modifier.weight(1f).padding(horizontal = RecapDimens.ProfileCardGap)) {
            Text(text = stringResource(R.string.recap_title), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold), color = MaterialTheme.colorScheme.onBackground)
            Text(text = stringResource(R.string.recap_card_subtitle), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(imageVector = WavvyIcons.NavigateNext, contentDescription = null, tint = MaterialTheme.colorScheme.onBackground)
    }
}

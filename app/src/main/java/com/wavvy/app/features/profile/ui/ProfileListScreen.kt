package com.wavvy.app.features.profile.ui

// Compose layouts and foundations
import androidx.compose.animation.core.tween
import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
// Material 3 components
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
// Compose state and runtime
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.produceState
// UI styling and utilities
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
// Project resources
import com.wavvy.app.R
import com.wavvy.app.core.designsystem.icons.WavvyIcons
import com.wavvy.app.core.designsystem.theme.WavvyTheme
import com.wavvy.app.core.designsystem.theme.WavvyMotion
import com.wavvy.app.core.history.TopSong
import com.wavvy.app.core.navigation.ItemNavigator
import com.wavvy.app.features.discography.data.DiscographyRepository
import com.wavvy.app.features.artist.data.ArtistRepository
import com.wavvy.app.features.home.data.HomeItem
import com.wavvy.app.features.home.data.HomeItemKind
import com.wavvy.app.features.home.ui.components.HomeListItem
import com.wavvy.app.features.home.ui.rememberListPlayer
import com.wavvy.app.features.player.ui.LocalMiniPlayerInset
import com.wavvy.app.features.search.data.SearchRepository

// The parts of the profile that open as a full list
enum class ProfileList(@StringRes val titleRes: Int) {
    Songs(R.string.profile_top_songs),
    Artists(R.string.profile_top_artists),
    Playlists(R.string.profile_playlists)
}

// How many songs and artists the full lists show
private const val FullCount = 50

// The full list behind a part of the profile, with the place of each one and how it moved since the period before
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileListScreen(list: ProfileList, period: ProfilePeriod?, onBack: () -> Unit, modifier: Modifier = Modifier, channel: String? = null, section: Int = 0) {
    val context = LocalContext.current
    val playList = rememberListPlayer()
    // A pull of the finger asks the list again, the mark stays until the answer comes
    var refreshKey by remember { mutableIntStateOf(0) }
    var isRefreshing by remember { mutableStateOf(false) }
    var isLoading by remember(list, period) { mutableStateOf(true) }
    val songs by produceState(initialValue = emptyList<RankedSong>(), list, period, refreshKey) {
        if (list == ProfileList.Songs) {
            value = ProfileRanking.songs(context, period, FullCount)
            isRefreshing = false
            isLoading = false
        }
    }
    val artists by produceState(initialValue = emptyList<TopArtist>(), list, period, refreshKey) {
        if (list == ProfileList.Artists) {
            val first = ProfileRanking.withKnownPhotos(context, ProfileRanking.artists(context, period).take(FullCount))
            value = first
            value = ProfileRanking.withPhotos(context, SearchRepository(context), first.filterNot { it.isLookedUp }).let { looked -> first.map { artist -> looked.firstOrNull { it.name == artist.name } ?: artist } }
            isRefreshing = false
            isLoading = false
        }
    }
    val playlists by produceState(initialValue = emptyList<HomeItem>(), list, refreshKey) {
        if (list == ProfileList.Playlists) {
            value = if (channel == null) ProfileRanking.playlists(context) else channelShelf(context, channel, section)
            isRefreshing = false
            isLoading = false
        }
    }

    // Each row with how many times and how long it was listened to, none for a playlist
    val shown = when (list) {
        ProfileList.Songs -> songs.map { Triple(it.top.toItem(trend = it.trend), it.top.plays, it.top.total) }
        ProfileList.Artists -> artists.map { Triple(it.toItem(), it.plays, it.totalMs) }
        ProfileList.Playlists -> playlists.map { Triple(it, null, null) }
    }
    val items = shown.map { it.first }

    val refreshState = rememberPullToRefreshState()
    PullToRefreshBox(
        state = refreshState,
        isRefreshing = isRefreshing,
        onRefresh = {
            isRefreshing = true
            refreshKey++
        },
        indicator = {
            PullToRefreshDefaults.Indicator(
                state = refreshState,
                isRefreshing = isRefreshing,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top))
                    .padding(top = ProfileDimens.RefreshTop)
            )
        },
        modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
    ) {
        LazyColumn(contentPadding = PaddingValues(bottom = LocalMiniPlayerInset.current + ProfileDimens.ScreenBottom)) {
            item(key = "bar") {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))
                        .padding(horizontal = WavvyTheme.dimens.spaceSmall)
                ) {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = WavvyIcons.Back, contentDescription = stringResource(R.string.cd_back), tint = MaterialTheme.colorScheme.onBackground)
                    }
                    Text(
                        text = stringResource(list.titleRes),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Two artists can lead to the same page, so the place is part of the key
            // The placeholder fades out as the rows fade in
            if (isLoading && shown.isEmpty()) item(key = "loading") { Box(modifier = Modifier.animateItem(fadeInSpec = null, placementSpec = null, fadeOutSpec = tween(WavvyMotion.ScreenFadeMillis))) { RowsSkeleton(rows = ProfileDimens.SkeletonListRows) } }

            itemsIndexed(shown, key = { index, row -> "${row.first.id}#$index" }) { index, (item, plays, time) ->
                HomeListItem(
                    modifier = Modifier.animateItem(fadeInSpec = tween(WavvyMotion.ScreenFadeMillis), placementSpec = null, fadeOutSpec = null),
                    item = item,
                    onClick = {
                        when (list) {
                            ProfileList.Songs -> playList(items, index, null)
                            else -> ItemNavigator.open(item)
                        }
                    },
                    rank = if (list == ProfileList.Playlists) null else index + 1,
                    trailing = plays?.let { { PlayCountBadge(it) } },
                    accent = time?.let { durationText(it) }
                )
            }
        }
    }
}

// Every playlist of a shelf of the page of a person, the whole list behind its title when it has one, or the ones the shelf brought
private suspend fun channelShelf(context: Context, channel: String, section: Int): List<HomeItem> {
    val shelf = ArtistRepository(context).load(channel).getOrNull()?.sections?.getOrNull(section) ?: return emptyList()
    val link = shelf.link ?: return shelf.items
    return DiscographyRepository(context).load(link.browseId, link.params).getOrNull()?.items?.takeIf { it.isNotEmpty() } ?: shelf.items
}

// How many times a song or an artist was listened to, on a soft tint of the page that is lighter on the dark theme and darker on the light one
@Composable
internal fun PlayCountBadge(plays: Int) {
    Text(
        text = pluralStringResource(R.plurals.profile_plays, plays, plays),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onBackground,
        maxLines = 1,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .widthIn(min = ProfileDimens.CountMinWidth)
            .clip(RoundedCornerShape(ProfileDimens.CountCorner))
            .background(MaterialTheme.colorScheme.onBackground.copy(alpha = ProfileDimens.CountAlpha))
            .padding(horizontal = ProfileDimens.CountPaddingHorizontal, vertical = ProfileDimens.CountPaddingVertical)
    )
}

// A song of the ranking as a row of the lists
private fun TopSong.toItem(trend: String): HomeItem =
    HomeItem(
        kind = HomeItemKind.Song,
        id = song.id,
        title = song.title,
        thumbnailUrl = song.artworkUrl,
        artists = listOfNotNull(song.artist),
        trend = trend
    )

// An artist of the ranking as a row of the lists
private fun TopArtist.toItem(): HomeItem =
    HomeItem(
        kind = HomeItemKind.Artist,
        id = id ?: name,
        title = name,
        thumbnailUrl = picture,
        trend = trend
    )

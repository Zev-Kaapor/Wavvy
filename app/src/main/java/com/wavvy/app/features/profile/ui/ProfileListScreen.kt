package com.wavvy.app.features.profile.ui

// Compose layouts and foundations
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
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
// Material 3 components
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
// Compose state and runtime
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
// UI styling and utilities
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
// Project resources
import com.wavvy.app.R
import com.wavvy.app.core.designsystem.icons.WavvyIcons
import com.wavvy.app.core.designsystem.theme.WavvyTheme
import com.wavvy.app.core.history.TopSong
import com.wavvy.app.core.navigation.ItemNavigator
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
@Composable
fun ProfileListScreen(list: ProfileList, period: ProfilePeriod?, onBack: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val playList = rememberListPlayer()
    val songs by produceState(initialValue = emptyList<RankedSong>(), list, period) {
        if (list == ProfileList.Songs) value = ProfileRanking.songs(context, period, FullCount)
    }
    val artists by produceState(initialValue = emptyList<TopArtist>(), list, period) {
        if (list == ProfileList.Artists) value = ProfileRanking.withPhotos(SearchRepository(context), ProfileRanking.artists(context, period).take(FullCount))
    }
    val playlists by produceState(initialValue = emptyList<HomeItem>(), list) {
        if (list == ProfileList.Playlists) value = ProfileRanking.playlists(context)
    }
    val shown = when (list) {
        ProfileList.Songs -> songs.map { it.top.toItem(plays = it.top.plays, trend = it.trend) }
        ProfileList.Artists -> artists.map { it.toItem() }
        ProfileList.Playlists -> playlists
    }

    Box(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
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

            itemsIndexed(shown, key = { _, item -> item.id }) { index, item ->
                HomeListItem(
                    item = item,
                    onClick = {
                        when (list) {
                            ProfileList.Songs -> playList(shown, index, null)
                            else -> ItemNavigator.open(item)
                        }
                    },
                    rank = if (list == ProfileList.Playlists) null else index + 1
                )
            }
        }
    }
}

// A song of the ranking as a row of the lists, with how many times it was played under its name
@Composable
private fun TopSong.toItem(plays: Int, trend: String): HomeItem {
    val playsText = pluralStringResource(R.plurals.profile_plays, plays, plays)
    return HomeItem(
        kind = HomeItemKind.Song,
        id = song.id,
        title = song.title,
        thumbnailUrl = song.artworkUrl,
        artists = listOfNotNull(song.artist),
        lineText = listOfNotNull(song.artist, playsText).joinToString(" • "),
        trend = trend
    )
}

// An artist of the ranking as a row of the lists, with the time listened under its name
@Composable
private fun TopArtist.toItem(): HomeItem =
    HomeItem(
        kind = HomeItemKind.Artist,
        id = id ?: name,
        title = name,
        thumbnailUrl = photoUrl ?: cover,
        lineText = pluralStringResource(R.plurals.profile_plays, plays, plays),
        trend = trend
    )

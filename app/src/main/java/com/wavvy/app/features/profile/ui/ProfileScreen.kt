package com.wavvy.app.features.profile.ui

// Android context and intents
import android.content.Context
import android.content.Intent
import android.net.Uri
// Compose layouts and foundations
import androidx.compose.animation.core.tween
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
// Material 3 components
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
// Compose state and runtime
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
// UI styling and utilities
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
// Pictures and coroutines
import coil3.compose.AsyncImage
// Project resources
import com.wavvy.app.features.home.ui.components.HomeDimens
import com.wavvy.app.R
import com.wavvy.app.core.history.ListenStats
import com.wavvy.app.core.history.TopSong
import com.wavvy.app.core.designsystem.components.OverlaySheet
import com.wavvy.app.core.designsystem.components.SkeletonHost
import com.wavvy.app.core.designsystem.components.clickableNoIndication
import com.wavvy.app.core.designsystem.components.skeleton
import com.wavvy.app.core.designsystem.icons.WavvyIcons
import com.wavvy.app.core.designsystem.theme.WavvyTheme
import com.wavvy.app.core.designsystem.theme.WavvyMotion
import com.wavvy.app.core.navigation.ItemNavigator
import com.wavvy.app.features.home.data.HomeItem
import com.wavvy.app.features.home.data.HomeItemKind
import com.wavvy.app.features.home.ui.components.HomeListItem
import com.wavvy.app.features.home.ui.components.HomeType
import com.wavvy.app.features.home.ui.rememberListPlayer
import com.wavvy.app.features.library.ui.Chip
import com.wavvy.app.features.player.ui.LocalMiniPlayerInset
import com.wavvy.app.features.profile.ui.components.ProfileAvatar
import com.wavvy.app.features.settings.data.SettingsStore

// The addresses a profile is shared with, where the channel is edited, and the milliseconds in a minute and in an hour
private const val MusicChannelAddress = "https://music.youtube.com/channel/"
private const val ChannelAddress = "https://www.youtube.com/"
private const val StudioAddress = "https://studio.youtube.com/channel/"
private const val StudioProfilePath = "/editing/profile"
private const val MillisPerMinute = 60_000L
private const val MinutesPerHour = 60L
private const val HoursPerDay = 24L
private const val DaysPerYear = 365L

// How many lines of the words of the channel show before the more
private const val AboutClosedLines = 1

// The profile of the user, a banner with the photo, the words of the channel, the numbers of what was listened to, the artists and the songs listened to the most, the playlists and what was played lately
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(onBack: () -> Unit, modifier: Modifier = Modifier, viewModel: ProfileViewModel = viewModel()) {
    val context = LocalContext.current
    val dimens = WavvyTheme.dimens
    val profile = LocalProfile.current
    val state by viewModel.state.collectAsState()
    val playList = rememberListPlayer()
    val store = remember { SettingsStore(context) }
    val showActivity by store.profileShowActivity.collectAsState(initial = true)
    val showSongs by store.profileShowSongs.collectAsState(initial = true)
    val showArtists by store.profileShowArtists.collectAsState(initial = true)
    val showPlaylists by store.profileShowPlaylists.collectAsState(initial = true)
    var isEditingChannel by remember { mutableStateOf(false) }

    // Coming back from editing the channel asks for it again, so what changed there shows here
    val lifecycleOwner = LocalView.current.findViewTreeLifecycleOwner()
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && isEditingChannel) {
                isEditingChannel = false
                viewModel.syncChannel()
            }
        }
        lifecycleOwner?.lifecycle?.addObserver(observer)
        onDispose { lifecycleOwner?.lifecycle?.removeObserver(observer) }
    }

    // Each song with its plays and time, taken together so they always agree with each other
    val songRows = state.topSongs.map { it.top.toItem(trend = it.trend) }
    val songPlays = state.topSongs.map { it.top.plays }
    val songTimes = state.topSongs.map { durationText(it.top.total) }
    val periodName = state.period?.name

    // The first time the numbers are worked out the whole page is blocks, so it never shows in pieces
    val isFirstLoad = state.isLoading && state.stats == null

    // The link of the profile in YouTube Music, or of the channel when its id is not known yet, none for a guest
    val shareLink = when {
        !profile.isSignedIn -> null
        state.channelId != null -> "$MusicChannelAddress${state.channelId}"
        profile.handle != null -> "$ChannelAddress${profile.handle}"
        else -> null
    }

    val refreshState = rememberPullToRefreshState()
    PullToRefreshBox(
        state = refreshState,
        isRefreshing = state.isRefreshing,
        onRefresh = viewModel::refresh,
        indicator = {
            PullToRefreshDefaults.Indicator(
                state = refreshState,
                isRefreshing = state.isRefreshing,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top))
                    .padding(top = ProfileDimens.RefreshTop)
            )
        },
        modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
    ) {
        Crossfade(targetState = isFirstLoad, animationSpec = tween(WavvyMotion.ScreenFadeMillis), label = "profileLoad") { isPlaceholder ->
            if (isPlaceholder) {
                ProfileSkeleton(onBack = onBack, actions = listOfNotNull(WavvyIcons.Edit.takeIf { profile.isSignedIn }, WavvyIcons.ShareArrow.takeIf { shareLink != null }, WavvyIcons.Settings))
            } else LazyColumn(contentPadding = PaddingValues(bottom = LocalMiniPlayerInset.current + ProfileDimens.ScreenBottom)) {
                item(key = "header") { Header(profile = profile, bannerUrl = state.bannerUrl, stats = state.stats, artistCount = state.artistCount, showStats = showActivity, shareLink = shareLink, onShare = { shareLink?.let { link -> shareProfile(context, link) } }, onEdit = if (profile.isSignedIn) ({ isEditingChannel = true; openStudio(context, state.channelId) }) else null, onSettings = { OverlaySheet.show { ProfileVisibilitySheet(onDismiss = { OverlaySheet.dismiss() }) } }, onBack = onBack) }

                // A few words about the user
                if (!state.about.isNullOrBlank()) {
                    item(key = "about") { About(text = state.about.orEmpty()) }
                }

                // The period and the activity, or the note that the user kept it private
                item(key = "activity") {
                    Column(verticalArrangement = Arrangement.spacedBy(dimens.spaceMedium), modifier = Modifier.padding(top = if (state.about.isNullOrBlank()) ProfileDimens.SectionTop else ProfileDimens.AboutGap)) {
                        if (showActivity || showSongs || showArtists) {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(ProfileDimens.ChipGap),
                                contentPadding = PaddingValues(horizontal = dimens.screenPadding)
                            ) {
                                items(ProfilePeriod.entries) { period ->
                                    Chip(text = stringResource(period.titleRes), isOn = period == state.period) { viewModel.selectPeriod(period) }
                                }
                            }
                        }

                        if (showActivity) ActivityCard(state = state) else Section(title = stringResource(R.string.profile_activity)) { PrivateNote() }
                    }
                }

                // The songs listened to the most, the first ones, the rest opens by the arrow
                if (!showSongs || state.isLoading || songRows.isNotEmpty()) {
                    item(key = "songs") {
                        Section(
                            title = stringResource(R.string.profile_top_songs),
                            onMore = if (showSongs) ({ ItemNavigator.openProfileList(ProfileList.Songs.name, periodName) }) else null
                        ) {
                            if (showSongs) {
                                Column {
                                    songRows.forEachIndexed { index, item ->
                                        HomeListItem(item = item, onClick = { playList(songRows, index, null) }, rank = index + 1, trailing = { PlayCountBadge(songPlays.getOrElse(index) { 0 }) }, accent = songTimes.getOrNull(index))
                                    }
                                }
                            } else {
                                PrivateNote()
                            }
                        }
                    }
                }

                // The artists listened to the most
                if (!showArtists || state.isLoading || state.topArtists.isNotEmpty()) {
                    item(key = "artists") {
                        Section(
                            title = stringResource(R.string.profile_top_artists),
                            onMore = if (showArtists) ({ ItemNavigator.openProfileList(ProfileList.Artists.name, periodName) }) else null
                        ) {
                            if (showArtists) ArtistCarousel(artists = state.topArtists) else PrivateNote()
                        }
                    }
                }

                // The playlists, the first ones, or the note that the user kept them private
                if (!showPlaylists || state.playlists.isNotEmpty()) {
                    item(key = "playlists") {
                        Section(
                            title = stringResource(R.string.profile_playlists),
                            onMore = if (showPlaylists && state.playlists.size > PlaylistPreview) ({ ItemNavigator.openProfileList(ProfileList.Playlists.name, null) }) else null
                        ) {
                            if (showPlaylists) Carousel(items = state.playlists.take(PlaylistPreview), onClick = { ItemNavigator.open(it) }) else PrivateNote()
                        }
                    }
                }
            }
        }
    }
}

// The rows of songs while they load, a cover and two lines each, with the light that sweeps over them
@Composable
internal fun RowsSkeleton(rows: Int = ProfileDimens.SkeletonSongs) {
    SkeletonHost(modifier = Modifier.padding(horizontal = WavvyTheme.dimens.screenPadding)) {
        repeat(rows) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = ProfileDimens.SkeletonLineGap)) {
                Box(modifier = Modifier.size(ProfileDimens.SkeletonCover).skeleton(RoundedCornerShape(ProfileDimens.CoverCorner)))
                Column(verticalArrangement = Arrangement.spacedBy(ProfileDimens.SkeletonLineGap), modifier = Modifier.padding(start = WavvyTheme.dimens.spaceMedium)) {
                    Box(modifier = Modifier.width(ProfileDimens.SkeletonTitleWidth).height(ProfileDimens.SkeletonLineHeight).skeleton())
                    Box(modifier = Modifier.width(ProfileDimens.SkeletonLineWidth).height(ProfileDimens.SkeletonLineHeight).skeleton())
                }
            }
        }
    }
}

// The whole page of a profile while it loads, laid out as the page itself, what frames it is real, the round buttons, the places of the songs, the arrows of the titles, the dots of the card and the menus of the rows, and what it shows is gray blocks in their places
// The top is the same on every profile and the body is the one of the page, the one of the user when none is given
// The arrow back is the only button that works, so the page can still be left, the others only stand in their places
@Composable
internal fun ProfileSkeleton(onBack: () -> Unit, actions: List<ImageVector>, body: @Composable () -> Unit = { UserBodySkeleton() }) {
    val dimens = WavvyTheme.dimens
    val block = WavvyTheme.colors.skeleton
    val background = MaterialTheme.colorScheme.background

    SkeletonHost(modifier = Modifier.fillMaxSize()) {
        // The banner with the round buttons, the name, the user name and the numbers at the start and the round photo at the end
        Box(modifier = Modifier.fillMaxWidth().height(ProfileDimens.BannerHeight)) {
            Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(block, background))))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))
                    .padding(horizontal = dimens.screenPadding, vertical = dimens.spaceSmall)
            ) {
                RoundAction(icon = WavvyIcons.Back, description = stringResource(R.string.cd_back), onClick = onBack)
                Box(modifier = Modifier.weight(1f))
                Row(horizontalArrangement = Arrangement.spacedBy(ProfileDimens.RoundButtonGap)) {
                    actions.forEach { icon -> RoundAction(icon = icon, description = null, onClick = {}) }
                }
            }
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(dimens.spaceMedium),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
                    .padding(horizontal = dimens.screenPadding)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(ProfileDimens.SkeletonLineGap), modifier = Modifier.weight(1f)) {
                    Box(modifier = Modifier.width(ProfileDimens.SkeletonNameWidth).height(ProfileDimens.SkeletonNameHeight).skeleton())
                    Box(modifier = Modifier.width(ProfileDimens.SkeletonLineWidth).height(ProfileDimens.SkeletonLineHeight).skeleton())
                    Row(horizontalArrangement = Arrangement.spacedBy(ProfileDimens.StatGap), modifier = Modifier.padding(top = ProfileDimens.StatTop)) {
                        repeat(ProfileDimens.SkeletonStats) {
                            Column(verticalArrangement = Arrangement.spacedBy(ProfileDimens.SkeletonLineGap)) {
                                Box(modifier = Modifier.width(ProfileDimens.SkeletonStatWidth).height(ProfileDimens.SkeletonStatHeight).skeleton())
                                Box(modifier = Modifier.width(ProfileDimens.SkeletonStatLabelWidth).height(ProfileDimens.SkeletonLineHeight).skeleton())
                            }
                        }
                    }
                }
                RingedBlock(size = ProfileDimens.BigAvatar)
            }
        }
        body()
    }
}

// The body of the profile of the user while it loads, the words of the channel, the chips, the card of the artist heard the most, the songs and the artists
@Composable
private fun UserBodySkeleton() {
    val dimens = WavvyTheme.dimens

    Column {
        Column(modifier = Modifier.padding(horizontal = dimens.screenPadding)) {
            // The words of the channel, with the place of the first sign
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(dimens.spaceSmall), modifier = Modifier.padding(top = ProfileDimens.AboutGap)) {
                Box(modifier = Modifier.size(ProfileDimens.SkeletonAboutDot).skeleton(CircleShape))
                Box(modifier = Modifier.width(ProfileDimens.SkeletonAboutWidth).height(ProfileDimens.SkeletonLineHeight).skeleton())
            }

            // The chips of the periods
            Row(horizontalArrangement = Arrangement.spacedBy(ProfileDimens.ChipGap), modifier = Modifier.padding(top = ProfileDimens.AboutGap)) {
                ProfileDimens.SkeletonChipWidths.forEach { chipWidth ->
                    Box(modifier = Modifier.weight(chipWidth.value).height(ProfileDimens.SkeletonChipHeight).skeleton(RoundedCornerShape(ProfileDimens.CoverCorner)))
                }
            }

            // The card of the artist heard the most, its picture, its three lines and the dots of its pages
            Box(
                modifier = Modifier
                    .padding(top = dimens.spaceMedium)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(ProfileDimens.TileCorner))
                    .background(MaterialTheme.colorScheme.surfaceContainer)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(ProfileDimens.TopArtistGap), modifier = Modifier.padding(ProfileDimens.TilePadding)) {
                    Box(modifier = Modifier.size(ProfileDimens.TopArtistPhoto).skeleton(RoundedCornerShape(ProfileDimens.CoverCorner)))
                    Column(verticalArrangement = Arrangement.spacedBy(ProfileDimens.SkeletonLineGap)) {
                        ProfileDimens.SkeletonCardLineWidths.forEach { lineWidth ->
                            Box(modifier = Modifier.width(lineWidth).height(ProfileDimens.SkeletonLineHeight).skeleton())
                        }
                    }
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(ProfileDimens.PagerDotGap),
                    modifier = Modifier.align(Alignment.BottomEnd).padding(end = ProfileDimens.PagerDotInset, bottom = ProfileDimens.PagerDotInset)
                ) {
                    repeat(ProfileDimens.TopArtistPages) {
                        Box(modifier = Modifier.size(ProfileDimens.PagerDot).clip(CircleShape).background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = ProfileDimens.PagerDotAlpha)))
                    }
                }
            }

            // The songs, each with its place, its cover, its lines, its count and its menu
            SectionTitleSkeleton()
            repeat(ProfileDimens.SkeletonSongs) { index ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().height(HomeDimens.ListHeight)) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(HomeDimens.RankWidth)) {
                        Text(text = (index + 1).toString(), style = MaterialTheme.typography.bodyMedium.merge(HomeType.Rank), color = MaterialTheme.colorScheme.secondary)
                        Box(modifier = Modifier.size(HomeDimens.TrendMark).clip(CircleShape).background(MaterialTheme.colorScheme.secondary))
                    }
                    Box(modifier = Modifier.padding(HomeDimens.ListCoverPadding).size(HomeDimens.ListCover).skeleton(RoundedCornerShape(ProfileDimens.CoverCorner)))
                    Column(verticalArrangement = Arrangement.spacedBy(ProfileDimens.SkeletonLineGap), modifier = Modifier.weight(1f).padding(horizontal = HomeDimens.ListTextPadding)) {
                        ProfileDimens.SkeletonRowLineWidths.forEach { lineWidth ->
                            Box(modifier = Modifier.width(lineWidth).height(ProfileDimens.SkeletonLineHeight).skeleton())
                        }
                    }
                    Box(modifier = Modifier.width(ProfileDimens.CountMinWidth).height(ProfileDimens.SkeletonCountHeight).skeleton(CircleShape))
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(ProfileDimens.SkeletonMoreButton)) {
                        Icon(imageVector = WavvyIcons.MoreVertical, contentDescription = null, tint = MaterialTheme.colorScheme.onBackground)
                    }
                }
            }

            // The artists
            SectionTitleSkeleton()
        }
        Row(horizontalArrangement = Arrangement.spacedBy(ProfileDimens.CarouselGap), modifier = Modifier.padding(horizontal = dimens.screenPadding)) {
            repeat(ProfileDimens.SkeletonArtists) { RingedBlock(size = ProfileDimens.ArtistSize) }
        }
    }
}

// The title of a part while it loads, a block where the words go and the real arrow at its end when the part has one
@Composable
internal fun SectionTitleSkeleton(hasArrow: Boolean = true) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(top = ProfileDimens.SectionTop + ProfileDimens.CaptionTop, bottom = ProfileDimens.CaptionTop)) {
        Box(modifier = Modifier.weight(1f)) {
            Box(modifier = Modifier.width(ProfileDimens.SkeletonSectionTitleWidth).height(ProfileDimens.SkeletonSectionTitleHeight).skeleton())
        }
        if (hasArrow) Icon(imageVector = WavvyIcons.NavigateNext, contentDescription = null, tint = MaterialTheme.colorScheme.onBackground)
    }
}

// A round block with a ring around it, the place of a round photo
@Composable
private fun RingedBlock(size: Dp) {
    Box(
        modifier = Modifier
            .size(size)
            .border(ProfileDimens.SkeletonRing, WavvyTheme.colors.skeleton, CircleShape)
            .padding(ProfileDimens.SkeletonRing + ProfileDimens.AvatarRingGap)
            .skeleton(CircleShape)
    )
}

// A song of the profile as a row of the lists, with how it moved
private fun TopSong.toItem(trend: String): HomeItem =
    HomeItem(
        kind = HomeItemKind.Song,
        id = song.id,
        title = song.title,
        thumbnailUrl = song.artworkUrl,
        artists = listOfNotNull(song.artist),
        trend = trend
    )

// The time written in the biggest unit that fits, from minutes to years
@Composable
internal fun durationText(totalMs: Long): String {
    val minutes = totalMs / MillisPerMinute
    val hours = minutes / MinutesPerHour
    val days = hours / HoursPerDay
    return when {
        days >= DaysPerYear -> stringResource(R.string.profile_time_years, days / DaysPerYear, days % DaysPerYear)
        days >= 1 -> stringResource(R.string.profile_time_days, days, hours % HoursPerDay)
        hours >= 1 -> stringResource(R.string.profile_time_hours, hours, minutes % MinutesPerHour)
        else -> stringResource(R.string.profile_time_minutes, minutes)
    }
}

// Opens YouTube Studio on the profile of the channel, where the banner, the photo and the words are changed
private fun openStudio(context: Context, channelId: String?) {
    val address = if (channelId != null) "$StudioAddress$channelId$StudioProfilePath" else StudioAddress
    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(address))) }
}

// Opens the sharing sheet of the system with the link of the profile
private fun shareProfile(context: Context, link: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, link)
    }
    context.startActivity(Intent.createChooser(intent, context.getString(R.string.player_share_via)))
}

// The artists heard the most, a card each that slides sideways, with the dots of the pages under them
@Composable
private fun ActivityCard(state: ProfileUiState) {
    val artists = state.topArtists.take(ProfileDimens.TopArtistPages)
    if (artists.isEmpty()) return

    val labels = listOf(R.string.profile_top_artist, R.string.profile_top_artist_second, R.string.profile_top_artist_third)
    val pagerState = rememberPagerState(pageCount = { artists.size })

    Box {
        HorizontalPager(
            state = pagerState,
            contentPadding = PaddingValues(horizontal = WavvyTheme.dimens.screenPadding),
            pageSpacing = ProfileDimens.TileGap,
            modifier = Modifier.fillMaxWidth()
        ) { page ->
            TopArtistCard(artist = artists[page], label = stringResource(labels[page]))
        }

        // The dots sit on the corner of the card, in a neutral color
        if (pagerState.pageCount > 1) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(ProfileDimens.PagerDotGap),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = WavvyTheme.dimens.screenPadding + ProfileDimens.PagerDotInset, bottom = ProfileDimens.PagerDotInset)
            ) {
                repeat(pagerState.pageCount) { page ->
                    val color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (pagerState.currentPage == page) 1f else ProfileDimens.PagerDotAlpha)
                    Box(modifier = Modifier.clip(CircleShape).background(color).size(ProfileDimens.PagerDot))
                }
            }
        }
    }
}

// An artist with the picture, what place it holds and how many times it was played, a tap opens the artist
@Composable
private fun TopArtistCard(artist: TopArtist, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(ProfileDimens.TopArtistGap),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(ProfileDimens.TileCorner))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .clickable(enabled = artist.id != null) { artist.id?.let(ItemNavigator::openArtist) }
            .padding(ProfileDimens.TilePadding)
    ) {
        AsyncImage(
            model = artist.photoUrl ?: artist.cover,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(ProfileDimens.TopArtistPhoto).clip(RoundedCornerShape(ProfileDimens.CoverCorner)).background(MaterialTheme.colorScheme.surfaceContainerHigh)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(text = label.uppercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            Text(
                text = artist.name,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(text = pluralStringResource(R.plurals.profile_plays, artist.plays, artist.plays), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
        }
    }
}

// A number with what it counts under it, at the start of the column
@Composable
internal fun ActivityStat(value: String, label: String) {
    Column {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge.copy(lineHeight = ProfileDimens.StatNumberLineHeight, fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(text = label, style = MaterialTheme.typography.bodyMedium.copy(lineHeight = ProfileDimens.StatLabelLineHeight), color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
    }
}

// The banner of the channel with the name and the user name at its start and the round photo at its end, and the round buttons on top
@Composable
private fun Header(profile: Profile, bannerUrl: String?, stats: ListenStats?, artistCount: Int, showStats: Boolean, shareLink: String?, onShare: () -> Unit, onEdit: (() -> Unit)?, onSettings: () -> Unit, onBack: () -> Unit) {
    val dimens = WavvyTheme.dimens
    val colors = WavvyTheme.colors
    val background = MaterialTheme.colorScheme.background

    Box(modifier = Modifier.fillMaxWidth().height(ProfileDimens.BannerHeight)) {
        // The banner of the channel, or the colors of the app when there is none, and a fade into the background of the screen
        Box(
            modifier = Modifier.fillMaxSize().background(
                Brush.verticalGradient(listOf(colors.gradientStart.copy(alpha = ProfileDimens.PlainBannerAlpha), colors.gradientEnd.copy(alpha = ProfileDimens.PlainBannerAlpha)))
            )
        )
        if (bannerUrl != null) {
            AsyncImage(model = bannerUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        }
        Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, background))))

        // The round buttons on top, the arrow back at the start and the pencil, the share and the gear at the end
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))
                .padding(horizontal = dimens.screenPadding, vertical = dimens.spaceSmall)
        ) {
            RoundAction(icon = WavvyIcons.Back, description = stringResource(R.string.cd_back), onClick = onBack)
            Box(modifier = Modifier.weight(1f))
            Row(horizontalArrangement = Arrangement.spacedBy(ProfileDimens.RoundButtonGap)) {
                if (onEdit != null) RoundAction(icon = WavvyIcons.Edit, description = stringResource(R.string.profile_edit_channel), onClick = onEdit)
                if (shareLink != null) RoundAction(icon = WavvyIcons.ShareArrow, description = stringResource(R.string.profile_share), onClick = onShare)
                RoundAction(icon = WavvyIcons.Settings, description = stringResource(R.string.profile_page_settings), onClick = onSettings)
            }
        }

        // The name and the user name at the start, the round photo at the end
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(dimens.spaceMedium),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
                .padding(horizontal = dimens.screenPadding)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (profile.isSignedIn) profile.name.orEmpty() else stringResource(R.string.profile_guest),
                    style = MaterialTheme.typography.headlineMedium.copy(fontSize = ProfileDimens.NameSize, lineHeight = ProfileDimens.NameLineHeight, fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.offset(y = ProfileDimens.NameDrop)
                )
                val subtitle = if (profile.isSignedIn) profile.handle else stringResource(R.string.profile_guest_hint)
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // The time, the songs and the artists listened to, where a social profile has its numbers
                if (showStats) {
                    Row(horizontalArrangement = Arrangement.spacedBy(ProfileDimens.StatGap), modifier = Modifier.padding(top = ProfileDimens.StatTop)) {
                        ActivityStat(value = durationText(stats?.totalMs ?: 0L), label = stringResource(R.string.profile_stats_time))
                        ActivityStat(value = (stats?.songs ?: 0).toString(), label = stringResource(R.string.profile_stats_songs))
                        ActivityStat(value = artistCount.toString(), label = stringResource(R.string.profile_stats_artists))
                    }
                }
            }
            ProfileAvatar(photo = profile.photo, size = ProfileDimens.BigAvatar)
        }
    }
}

// A round button over the banner, dark and see-through so its icon shows over any picture
@Composable
internal fun RoundAction(icon: ImageVector, description: String?, onClick: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(ProfileDimens.RoundButton)
            .clip(CircleShape)
            .background(WavvyTheme.colors.tileScrim.copy(alpha = ProfileDimens.RoundButtonAlpha))
            .clickable(onClick = onClick)
    ) {
        Icon(imageVector = icon, contentDescription = description, tint = Color.White, modifier = Modifier.size(ProfileDimens.RoundButtonIcon))
    }
}

// The words of the channel, the first line with the more in color at its end, a tap opens all of them
@Composable
internal fun About(text: String) {
    var isOpen by remember(text) { mutableStateOf(false) }
    var isCut by remember(text) { mutableStateOf(false) }
    // Closed it shows the first line only, so the button stays right after its words and not at the end of the width, the rest of the lines come with it
    val firstLine = remember(text) { text.lineSequence().first().trim() }
    val hasMoreLines = remember(text) { text.lineSequence().drop(1).any { it.isNotBlank() } }
    val style = MaterialTheme.typography.bodyMedium
    val toggle = @Composable { label: Int ->
        Text(text = stringResource(label), style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold), color = MaterialTheme.colorScheme.primary)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (isCut || hasMoreLines || isOpen) Modifier.clickableNoIndication { isOpen = !isOpen } else Modifier)
            .padding(start = WavvyTheme.dimens.screenPadding, end = WavvyTheme.dimens.screenPadding, top = ProfileDimens.AboutGap)
    ) {
        if (isOpen) {
            Text(text = text, style = style, color = MaterialTheme.colorScheme.onSurfaceVariant)
            toggle(R.string.profile_about_less)
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (hasMoreLines) "$firstLine…" else firstLine,
                    style = style,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = AboutClosedLines,
                    overflow = TextOverflow.Ellipsis,
                    onTextLayout = { isCut = it.hasVisualOverflow },
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (isCut || hasMoreLines) {
                    Spacer(modifier = Modifier.width(ProfileDimens.AboutToggleGap))
                    toggle(R.string.profile_about_more)
                }
            }
        }
    }
}

// The line that takes the place of a part the user kept private, or of a part there is nothing to show of
@Composable
internal fun PrivateNote(textRes: Int = R.string.profile_private) {
    Text(
        text = stringResource(textRes),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.fillMaxWidth().padding(horizontal = WavvyTheme.dimens.screenPadding, vertical = ProfileDimens.CaptionTop)
    )
}

// The title of a part of the profile with its content under it, an arrow at the end of the title opens the whole list
@Composable
internal fun Section(title: String, onMore: (() -> Unit)? = null, content: @Composable () -> Unit) {
    Column(modifier = Modifier.padding(top = ProfileDimens.SectionTop)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().then(if (onMore != null) Modifier.clickable(onClick = onMore) else Modifier)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge.merge(HomeType.SectionTitle),
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f).padding(horizontal = WavvyTheme.dimens.screenPadding, vertical = ProfileDimens.CaptionTop)
            )
            if (onMore != null) {
                Icon(
                    imageVector = WavvyIcons.NavigateNext,
                    contentDescription = stringResource(R.string.profile_see_all),
                    tint = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(end = WavvyTheme.dimens.screenPadding)
                )
            }
        }
        content()
    }
}

// The round pictures of the artists with their names and the time listened, a tap opens the artist
@Composable
private fun ArtistCarousel(artists: List<TopArtist>) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(ProfileDimens.CarouselGap),
        contentPadding = PaddingValues(horizontal = WavvyTheme.dimens.screenPadding)
    ) {
        items(artists, key = { it.name }) { artist ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .width(ProfileDimens.ArtistSize)
                    .clip(RoundedCornerShape(ProfileDimens.CoverCorner))
                    .clickable(enabled = artist.id != null) { artist.id?.let(ItemNavigator::openArtist) }
            ) {
                AsyncImage(
                    model = artist.photoUrl ?: artist.cover,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(ProfileDimens.ArtistSize).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceContainer)
                )
                Text(
                    text = artist.name,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = ProfileDimens.CaptionTop)
                )
                Text(
                    text = pluralStringResource(R.plurals.profile_plays, artist.plays, artist.plays),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
        }
    }
}

// A row of covers that slides sideways, each with its name, a tap opens or plays it
@Composable
internal fun Carousel(items: List<HomeItem>, onClick: (HomeItem) -> Unit) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(ProfileDimens.CarouselGap),
        contentPadding = PaddingValues(horizontal = WavvyTheme.dimens.screenPadding)
    ) {
        items(items, key = { it.id }) { item ->
            Column(
                modifier = Modifier
                    .width(ProfileDimens.CoverSize)
                    .clip(RoundedCornerShape(ProfileDimens.CoverCorner))
                    .clickable { onClick(item) }
            ) {
                AsyncImage(
                    model = item.thumbnailUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(ProfileDimens.CoverSize)
                        .clip(RoundedCornerShape(ProfileDimens.CoverCorner))
                        .background(MaterialTheme.colorScheme.surfaceContainer)
                )
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = ProfileDimens.CaptionTop)
                )
            }
        }
    }
}

// How many playlists show before the arrow that opens all of them
internal const val PlaylistPreview = 5

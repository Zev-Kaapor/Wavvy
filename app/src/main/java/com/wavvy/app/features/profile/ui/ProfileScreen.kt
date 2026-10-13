package com.wavvy.app.features.profile.ui

// Android context and intents
import android.content.Context
import android.content.Intent
// Compose layouts and foundations
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
// Material 3 components
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
// Compose state and runtime
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
// UI styling and utilities
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.viewmodel.compose.viewModel
// Pictures and coroutines
import coil3.compose.AsyncImage
import kotlinx.coroutines.launch
// Project resources
import com.wavvy.app.R
import com.wavvy.app.core.history.TopSong
import com.wavvy.app.core.designsystem.icons.WavvyIcons
import com.wavvy.app.core.designsystem.theme.WavvyTheme
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
import com.wavvy.app.features.settings.ui.components.SettingsSwitchRow

// The addresses a profile is shared with, and the milliseconds in a minute and in an hour
private const val MusicChannelAddress = "https://music.youtube.com/channel/"
private const val ChannelAddress = "https://www.youtube.com/"
private const val MillisPerMinute = 60_000L
private const val MinutesPerHour = 60L
private const val HoursPerDay = 24L
private const val DaysPerYear = 365L

// The profile of the user, a banner with the photo, a few words about the user, the numbers of what was listened to, the artists and the songs listened to the most, the playlists and what was played lately
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(onBack: () -> Unit, modifier: Modifier = Modifier, viewModel: ProfileViewModel = viewModel()) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val dimens = WavvyTheme.dimens
    val profile = LocalProfile.current
    val state by viewModel.state.collectAsState()
    val playList = rememberListPlayer()
    val store = remember { SettingsStore(context) }
    val about by store.profileAbout.collectAsState(initial = "")
    val showActivity by store.profileShowActivity.collectAsState(initial = true)
    val showSongs by store.profileShowSongs.collectAsState(initial = true)
    val showArtists by store.profileShowArtists.collectAsState(initial = true)
    val showPlaylists by store.profileShowPlaylists.collectAsState(initial = true)
    var isEditing by remember { mutableStateOf(false) }
    var isConfiguring by remember { mutableStateOf(false) }

    if (isConfiguring) {
        PrivacyDialog(
            shown = listOf(showActivity, showSongs, showArtists, showPlaylists),
            onChange = { index, value ->
                scope.launch {
                    when (index) {
                        0 -> store.setProfileShowActivity(value)
                        1 -> store.setProfileShowSongs(value)
                        2 -> store.setProfileShowArtists(value)
                        else -> store.setProfileShowPlaylists(value)
                    }
                }
            },
            onDismiss = { isConfiguring = false }
        )
    }

    if (isEditing) {
        AboutDialog(
            initial = about,
            onSave = {
                isEditing = false
                scope.launch { store.setProfileAbout(it) }
            },
            onDismiss = { isEditing = false }
        )
    }

    val songRows = state.topSongs.map { it.top.toItem(trend = it.trend) }
    val periodName = state.period?.name
    val songTimes = state.topSongs.map { durationText(it.top.total) }

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
        LazyColumn(contentPadding = PaddingValues(bottom = LocalMiniPlayerInset.current + ProfileDimens.ScreenBottom)) {
            item(key = "header") { Header(profile = profile, bannerUrl = state.bannerUrl, shareLink = shareLink, onShare = { shareLink?.let { link -> shareProfile(context, link) } }, onEdit = { isEditing = true }, onSettings = { isConfiguring = true }, onBack = onBack) }

            // A few words about the user
            if (about.isNotBlank()) {
                item(key = "about") { About(text = about) }
            }

            // The period and the activity, or the note that the user kept it private
            item(key = "activity") {
                Column(verticalArrangement = Arrangement.spacedBy(dimens.spaceMedium), modifier = Modifier.padding(top = ProfileDimens.SectionTop)) {
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
            if (!showSongs || songRows.isNotEmpty()) {
                item(key = "songs") {
                    Section(
                        title = stringResource(R.string.profile_top_songs),
                        onMore = if (showSongs) ({ ItemNavigator.openProfileList(ProfileList.Songs.name, periodName) }) else null
                    ) {
                        if (showSongs) {
                            Column {
                                songRows.forEachIndexed { index, item ->
                                    HomeListItem(item = item, onClick = { playList(songRows, index, null) }, rank = index + 1, trailing = { PlayCountBadge(state.topSongs[index].top.plays) }, accent = songTimes[index])
                                }
                            }
                        } else {
                            PrivateNote()
                        }
                    }
                }
            }

            // The artists listened to the most
            if (!showArtists || state.topArtists.isNotEmpty()) {
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

// Opens the sharing sheet of the system with the link of the profile
private fun shareProfile(context: Context, link: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, link)
    }
    context.startActivity(Intent.createChooser(intent, context.getString(R.string.player_share_via)))
}

// The three numbers of the activity and the artist heard the most, in one card
@Composable
private fun ActivityCard(state: ProfileUiState) {
    val stats = state.stats
    val top = state.topArtists.firstOrNull()

    Column(
        verticalArrangement = Arrangement.spacedBy(ProfileDimens.TileGap),
        modifier = Modifier
            .padding(horizontal = WavvyTheme.dimens.screenPadding)
            .fillMaxWidth()
            .clip(RoundedCornerShape(ProfileDimens.TileCorner))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(ProfileDimens.TilePadding)
    ) {
        Text(text = stringResource(R.string.profile_activity), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onBackground)
        Row(modifier = Modifier.fillMaxWidth()) {
            ActivityStat(icon = WavvyIcons.Headphones, value = durationText(stats?.totalMs ?: 0L), label = stringResource(R.string.profile_stats_time), modifier = Modifier.weight(1f))
            ActivityStat(icon = WavvyIcons.MusicNote, value = (stats?.songs ?: 0).toString(), label = stringResource(R.string.profile_stats_songs), modifier = Modifier.weight(1f))
            ActivityStat(icon = WavvyIcons.Mic, value = state.artistCount.toString(), label = stringResource(R.string.profile_stats_artists), modifier = Modifier.weight(1f))
        }

        if (top != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(ProfileDimens.TopArtistGap),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(ProfileDimens.CoverCorner))
                    .clickable(enabled = top.id != null) { top.id?.let(ItemNavigator::openArtist) }
            ) {
                AsyncImage(
                    model = top.photoUrl ?: top.cover,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(ProfileDimens.TopArtistPhoto).clip(RoundedCornerShape(ProfileDimens.CoverCorner)).background(MaterialTheme.colorScheme.surfaceContainerHigh)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = stringResource(R.string.profile_top_artist).uppercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                    Text(
                        text = top.name,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(text = pluralStringResource(R.plurals.profile_plays, top.plays, top.plays), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                }
            }
        }
    }
}

// A number with a small icon over it and what it counts under it
@Composable
private fun ActivityStat(icon: ImageVector, value: String, label: String, modifier: Modifier = Modifier) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier) {
        Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(ProfileDimens.StatIcon))
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
    }
}

// The banner of the channel, the round photo over it, the name and the user name, with the arrow back, the pencil and the gear on top
@Composable
private fun Header(profile: Profile, bannerUrl: String?, shareLink: String?, onShare: () -> Unit, onEdit: () -> Unit, onSettings: () -> Unit, onBack: () -> Unit) {
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
            Box(modifier = Modifier.weight(1f))
            if (shareLink != null) {
                IconButton(onClick = onShare) {
                    Icon(imageVector = WavvyIcons.ShareArrow, contentDescription = stringResource(R.string.profile_share), tint = MaterialTheme.colorScheme.onBackground)
                }
            }
            IconButton(onClick = onEdit) {
                Icon(imageVector = WavvyIcons.Edit, contentDescription = stringResource(R.string.profile_about_edit), tint = MaterialTheme.colorScheme.onBackground)
            }
            IconButton(onClick = onSettings) {
                Icon(imageVector = WavvyIcons.Settings, contentDescription = stringResource(R.string.profile_page_settings), tint = MaterialTheme.colorScheme.onBackground)
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.align(Alignment.BottomCenter).padding(horizontal = dimens.screenPadding)) {
            ProfileAvatar(photo = profile.photo, size = ProfileDimens.BigAvatar)
            Text(
                text = if (profile.isSignedIn) profile.name.orEmpty() else stringResource(R.string.profile_guest),
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = ProfileDimens.NameTop)
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
        }
    }
}

// A few words about the user
@Composable
private fun About(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onBackground,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().padding(horizontal = WavvyTheme.dimens.screenPadding * 2, vertical = ProfileDimens.NameTop)
    )
}

// The line that takes the place of a part the user kept private
@Composable
private fun PrivateNote() {
    Text(
        text = stringResource(R.string.profile_private),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.fillMaxWidth().padding(horizontal = WavvyTheme.dimens.screenPadding, vertical = ProfileDimens.CaptionTop)
    )
}

// The title of a part of the profile with its content under it, an arrow at the end of the title opens the whole list
@Composable
private fun Section(title: String, onMore: (() -> Unit)? = null, content: @Composable () -> Unit) {
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
private fun Carousel(items: List<HomeItem>, onClick: (HomeItem) -> Unit) {
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

// The box where the user chooses what the profile shows and what stays private, a switch for each part
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PrivacyDialog(shown: List<Boolean>, onChange: (Int, Boolean) -> Unit, onDismiss: () -> Unit) {
    val titles = listOf(R.string.profile_show_activity, R.string.profile_show_songs, R.string.profile_show_artists, R.string.profile_show_playlists)

    BasicAlertDialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(ProfileDimens.DialogCorner), color = MaterialTheme.colorScheme.surfaceContainerHigh) {
            Column(verticalArrangement = Arrangement.spacedBy(ProfileDimens.DialogGap), modifier = Modifier.padding(ProfileDimens.DialogPadding)) {
                Text(text = stringResource(R.string.profile_page_settings), style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface)
                titles.forEachIndexed { index, title ->
                    SettingsSwitchRow(title = stringResource(title), subtitle = null, checked = shown[index], onCheckedChange = { onChange(index, it) })
                }
                FilledTonalButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth().padding(top = ProfileDimens.DialogGap)) {
                    Text(text = stringResource(R.string.profile_settings_done), maxLines = 1)
                }
            }
        }
    }
}

// The box where the few words about the user are written, with how many letters are left
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AboutDialog(initial: String, onSave: (String) -> Unit, onDismiss: () -> Unit) {
    var text by remember { mutableStateOf(initial) }

    BasicAlertDialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(ProfileDimens.DialogCorner), color = MaterialTheme.colorScheme.surfaceContainerHigh) {
            Column(verticalArrangement = Arrangement.spacedBy(ProfileDimens.DialogGap), modifier = Modifier.padding(ProfileDimens.DialogPadding)) {
                Text(text = stringResource(R.string.profile_about_title), style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface)

                TextField(
                    value = text,
                    onValueChange = { if (it.length <= AboutMaxLength) text = it },
                    placeholder = { Text(text = stringResource(R.string.profile_about_hint)) },
                    supportingText = { Text(text = "${text.length}/$AboutMaxLength") },
                    minLines = AboutMinLines,
                    maxLines = AboutMaxLines,
                    keyboardOptions = KeyboardOptions.Default,
                    colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(horizontalArrangement = Arrangement.spacedBy(ProfileDimens.DialogGap), modifier = Modifier.fillMaxWidth().padding(top = ProfileDimens.DialogGap)) {
                    FilledTonalButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text(text = stringResource(R.string.playlist_cancel), maxLines = 1)
                    }
                    Button(
                        onClick = { onSave(text) },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.onBackground, contentColor = MaterialTheme.colorScheme.background),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(text = stringResource(R.string.profile_about_save), maxLines = 1)
                    }
                }
            }
        }
    }
}

// How many letters the words about the user can have, and how many lines the box shows
private const val AboutMaxLength = 200
private const val AboutMinLines = 3
private const val AboutMaxLines = 5

// How many playlists show before the arrow that opens all of them
private const val PlaylistPreview = 5

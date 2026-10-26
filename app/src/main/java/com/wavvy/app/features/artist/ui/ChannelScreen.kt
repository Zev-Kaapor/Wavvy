package com.wavvy.app.features.artist.ui

// Compose layouts and foundations
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
// Material 3 components
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
// Compose state and runtime
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
// UI styling and utilities
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
// Project resources
import com.wavvy.app.R
import com.wavvy.app.core.designsystem.components.skeleton
import com.wavvy.app.core.designsystem.icons.WavvyIcons
import com.wavvy.app.core.designsystem.theme.WavvyMotion
import com.wavvy.app.core.designsystem.theme.WavvyTheme
import com.wavvy.app.core.innertube.resize
import com.wavvy.app.core.navigation.ItemNavigator
import com.wavvy.app.features.artist.data.ArtistPage
import com.wavvy.app.features.collection.ui.shareLink
import com.wavvy.app.features.home.ui.components.HomeMessage
import com.wavvy.app.features.home.ui.rememberItemPlayer
import com.wavvy.app.features.player.ui.LocalMiniPlayerInset
import com.wavvy.app.features.profile.ui.About
import com.wavvy.app.features.profile.ui.ActivityStat
import com.wavvy.app.features.profile.ui.Carousel
import com.wavvy.app.features.profile.ui.PlaylistPreview
import com.wavvy.app.features.profile.ui.PrivateNote
import com.wavvy.app.features.profile.ui.ProfileList
import com.wavvy.app.features.profile.ui.ProfileDimens
import com.wavvy.app.features.profile.ui.ProfileSkeleton
import com.wavvy.app.features.profile.ui.RoundAction
import com.wavvy.app.features.profile.ui.Section
import com.wavvy.app.features.profile.ui.SectionTitleSkeleton

// The page of a person, a channel with no music of its own, a copy of the profile of the user with what can be known of someone else
// The banner with the arrow and the share, the name with the user name and the numbers, and the round photo, then the subscription, the words of the channel, the parts that only the user has with the note that there is nothing to show, and the playlists of the person
// It reads the same page as the one of an artist, and while it loads the whole screen is the placeholder of the profile
@Composable
fun ChannelScreen(
    handle: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ArtistViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val onItemClick = rememberItemPlayer()
    val page = state.page

    // Solid, so the screen under it does not show through while the page slides
    // The placeholder fades into the page
    Crossfade(
        targetState = state.status,
        animationSpec = tween(WavvyMotion.ScreenFadeMillis),
        label = "channelLoad",
        modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
    ) { status ->
        when {
            status == ArtistStatus.Loading -> ProfileSkeleton(onBack = onBack, actions = listOf(WavvyIcons.ShareArrow)) { ChannelBodySkeleton() }

            status == ArtistStatus.Error || page == null -> Column {
                ChannelHeader(page = null, handle = handle, onShare = {}, onBack = onBack)
                HomeMessage(
                    text = stringResource(R.string.artist_error),
                    actionLabel = stringResource(R.string.home_retry),
                    onAction = viewModel::load
                )
            }

            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = LocalMiniPlayerInset.current + ProfileDimens.ScreenBottom)
            ) {
                item(key = "header") { ChannelHeader(page = page, handle = handle, onShare = { shareLink(context, viewModel.shareUrl) }, onBack = onBack) }

                // The subscription with how many follow, the only place the number shows
                page.subscription?.let { subscription ->
                    item(key = "subscribe") {
                        Box(modifier = Modifier.padding(start = WavvyTheme.dimens.screenPadding, end = WavvyTheme.dimens.screenPadding, top = ProfileDimens.AboutGap)) {
                            SubscribeButton(subscription = subscription, onClick = viewModel::toggleSubscription)
                        }
                    }
                }

                // A few words about the person
                page.description?.takeIf { it.isNotBlank() }?.let { description ->
                    item(key = "about") { About(text = description) }
                }

                // What only the user has of itself, the activity, the songs and the artists heard the most, with the note that there is nothing to show
                item(key = "activity") { Section(title = stringResource(R.string.profile_activity)) { PrivateNote(R.string.profile_no_info) } }
                item(key = "songs") { Section(title = stringResource(R.string.profile_top_songs)) { PrivateNote(R.string.profile_no_info) } }
                item(key = "artists") { Section(title = stringResource(R.string.profile_top_artists)) { PrivateNote(R.string.profile_no_info) } }

                // The playlists and the other shelves of the person, the first ones as on the profile of the user, and with more of them the arrow of the title opens the whole list
                itemsIndexed(page.sections, key = { index, _ -> "section_$index" }) { index, section ->
                    Section(
                        title = section.title,
                        onMore = if (section.items.size > PlaylistPreview || section.link != null) ({ ItemNavigator.openProfileList(ProfileList.Playlists.name, null, viewModel.id, index) }) else null
                    ) {
                        Carousel(items = section.items.take(PlaylistPreview), onClick = onItemClick)
                    }
                }
            }
        }
    }
}

// The body of the page of a person while it loads, as the page is drawn, the button of the subscription, the three parts with the line of their note, and the playlists with their arrow, covers and names
@Composable
private fun ChannelBodySkeleton() {
    Column(modifier = Modifier.padding(horizontal = WavvyTheme.dimens.screenPadding)) {
        Box(modifier = Modifier.padding(top = ProfileDimens.AboutGap).width(ArtistDimens.SkeletonSubscribeWidth).height(ArtistDimens.SubscribeHeight).skeleton(CircleShape))
        repeat(ChannelNoteParts) {
            SectionTitleSkeleton(hasArrow = false)
            Box(modifier = Modifier.padding(vertical = ProfileDimens.CaptionTop).width(ProfileDimens.SkeletonNoteWidth).height(ProfileDimens.SkeletonLineHeight).skeleton())
        }
        SectionTitleSkeleton()
        Row(horizontalArrangement = Arrangement.spacedBy(ProfileDimens.CarouselGap)) {
            repeat(ChannelSkeletonCovers) {
                Column(verticalArrangement = Arrangement.spacedBy(ProfileDimens.SkeletonLineGap)) {
                    Box(modifier = Modifier.size(ProfileDimens.CoverSize).skeleton(RoundedCornerShape(ProfileDimens.CoverCorner)))
                    Box(modifier = Modifier.width(ProfileDimens.SkeletonLineWidth).height(ProfileDimens.SkeletonLineHeight).skeleton())
                }
            }
        }
    }
}

// The top of the page as the one of the profile of the user, the banner with a fade into the background, the arrow and the share as round buttons, the name with the user name and the numbers at the start and the round photo at the end
// The numbers are the ones of the profile of the user, which are not known of someone else, so they show a dash, and with no page, when it failed, only the colors of the app and the arrow are drawn
@Composable
private fun ChannelHeader(page: ArtistPage?, handle: String, onShare: () -> Unit, onBack: () -> Unit) {
    val dimens = WavvyTheme.dimens
    val colors = WavvyTheme.colors
    val background = MaterialTheme.colorScheme.background
    // A channel with no banner gives its photo in its place, which is no banner, so the colors of the app are drawn as on the profile of the user
    val banner = page?.bannerUrl?.takeIf { it != page.photoUrl }
    val ring = Brush.linearGradient(
        colors = listOf(colors.gradientStart, colors.gradientMiddle, colors.gradientEnd),
        start = Offset(0f, Float.POSITIVE_INFINITY),
        end = Offset(Float.POSITIVE_INFINITY, 0f)
    )
    val unknown = stringResource(R.string.profile_stat_unknown)

    Box(modifier = Modifier.fillMaxWidth().height(ProfileDimens.BannerHeight)) {
        // The banner of the channel, or the colors of the app when there is none, and a fade into the background of the screen
        Box(
            modifier = Modifier.fillMaxSize().background(
                Brush.verticalGradient(listOf(colors.gradientStart.copy(alpha = ProfileDimens.PlainBannerAlpha), colors.gradientEnd.copy(alpha = ProfileDimens.PlainBannerAlpha)))
            )
        )
        banner?.let {
            AsyncImage(model = it.resize(ChannelBannerWidth, ChannelBannerHeight), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        }
        Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, background))))

        // The round buttons on top, the arrow at the start and the share at the end
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))
                .padding(horizontal = dimens.screenPadding, vertical = dimens.spaceSmall)
        ) {
            RoundAction(icon = WavvyIcons.Back, description = stringResource(R.string.cd_back), onClick = onBack)
            Box(modifier = Modifier.weight(1f))
            if (page != null) RoundAction(icon = WavvyIcons.ShareArrow, description = stringResource(R.string.profile_share), onClick = onShare)
        }

        // The name, the user name and the numbers at the start, the round photo at the end
        if (page != null) {
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
                        text = page.name,
                        style = MaterialTheme.typography.headlineMedium.copy(fontSize = ProfileDimens.NameSize, lineHeight = ProfileDimens.NameLineHeight, fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.offset(y = ProfileDimens.NameDrop)
                    )
                    if (handle.isNotEmpty()) {
                        Text(
                            text = handle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(ProfileDimens.StatGap), modifier = Modifier.padding(top = ProfileDimens.StatTop)) {
                        ActivityStat(value = unknown, label = stringResource(R.string.profile_stats_time))
                        ActivityStat(value = unknown, label = stringResource(R.string.profile_stats_songs))
                        ActivityStat(value = unknown, label = stringResource(R.string.profile_stats_artists))
                    }
                }

                // The round photo with the ring of the app, the silhouette stays under it until it comes
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(ProfileDimens.BigAvatar)
                        .border(ProfileDimens.AvatarRing, ring, CircleShape)
                        .padding(ProfileDimens.AvatarRing + ProfileDimens.AvatarRingGap)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Icon(imageVector = WavvyIcons.Person, contentDescription = null, tint = MaterialTheme.colorScheme.onBackground, modifier = Modifier.size(ProfileDimens.BigAvatar * ProfileDimens.AvatarIconFraction))
                    page.photoUrl?.let { photo ->
                        AsyncImage(model = photo.resize(ChannelPhotoRequest, ChannelPhotoRequest), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                    }
                }
            }
        }
    }
}

// The parts of the profile of the user the page shows with only the note, and the covers of the playlists while it loads
private const val ChannelNoteParts = 3
private const val ChannelSkeletonCovers = 3

// The size asked of the photo, a little more than it is drawn at so it stays sharp, and of the banner
private const val ChannelPhotoRequest = 400
private const val ChannelBannerWidth = 1080
private const val ChannelBannerHeight = 450

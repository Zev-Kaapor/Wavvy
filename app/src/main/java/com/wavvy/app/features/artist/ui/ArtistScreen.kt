package com.wavvy.app.features.artist.ui

// Compose animation
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
// Material 3 components
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
// Compose state and runtime
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
// UI styling and utilities
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.dp
// Lifecycle and image loading
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
// Project resources
import com.wavvy.app.R
import com.wavvy.app.core.designsystem.components.SkeletonHost
import com.wavvy.app.core.designsystem.components.skeleton
import com.wavvy.app.core.designsystem.icons.WavvyIcons
import com.wavvy.app.core.designsystem.theme.WavvyTheme
import com.wavvy.app.core.innertube.resize
import com.wavvy.app.core.navigation.ItemNavigator
import com.wavvy.app.features.artist.data.ArtistFeatured
import com.wavvy.app.features.artist.data.ArtistPage
import com.wavvy.app.features.artist.data.ArtistSubscription
import com.wavvy.app.features.artist.data.ReleaseKind
import com.wavvy.app.features.collection.ui.shareLink
import com.wavvy.app.features.home.ui.components.HomeDimens
import com.wavvy.app.features.home.ui.components.HomeMessage
import com.wavvy.app.features.home.ui.components.HomeShelf
import com.wavvy.app.features.home.ui.components.ItemBadges
import com.wavvy.app.features.home.ui.components.ItemThumbnail
import com.wavvy.app.features.home.ui.rememberItemPlayer
import com.wavvy.app.features.home.ui.rememberListPlayer
import com.wavvy.app.features.player.ui.LocalMiniPlayerInset

// What a link in a description looks like, the last character is not part of it when it is punctuation
private val LinkPattern = Regex("""https?://[^\s]+""")
private const val LinkTrailingPunctuation = ".,;:!?)]}\"'"

// Page of an artist, a picture with the name on top, the buttons that play it and the shelves of YouTube Music, ending with who the artist is
@Composable
fun ArtistScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ArtistViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val playList = rememberListPlayer()
    val onItemClick = rememberItemPlayer()
    val listState = rememberLazyListState()
    val page = state.page

    // The top bar gets a background and the name once the picture is mostly gone
    val threshold = with(LocalDensity.current) { ArtistDimens.BarScrollThreshold.toPx() }
    val isScrolled by remember { derivedStateOf { listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > threshold } }
    val barAlpha by animateFloatAsState(
        targetValue = if (isScrolled) 1f else 0f,
        animationSpec = tween(ArtistDimens.BarFadeMillis),
        label = "ArtistBar"
    )

    // Solid, so the screen under it does not show through while the page slides
    Box(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        when {
            state.status == ArtistStatus.Loading -> ArtistSkeleton()

            state.status == ArtistStatus.Error || page == null -> HomeMessage(
                text = stringResource(R.string.artist_error),
                actionLabel = stringResource(R.string.home_retry),
                onAction = viewModel::load
            )

            else -> LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                // The mini player covers the end of the page, with a little more room so it does not touch the chips
                contentPadding = PaddingValues(
                    bottom = LocalMiniPlayerInset.current + if (LocalMiniPlayerInset.current > 0.dp) ArtistDimens.MiniPlayerClearance else 0.dp
                )
            ) {
                item(key = "hero") { ArtistHero(page) }

                // Who the artist is, right under the name
                page.description?.let { description ->
                    item(key = "description") {
                        ArtistDescription(
                            description = description,
                            translation = state.translation,
                            showTranslation = state.showTranslation,
                            onTranslate = viewModel::toggleTranslation
                        )
                    }
                }

                item(key = "actions") {
                    ArtistActions(
                        subscription = page.subscription,
                        isEnabled = page.topSongs.isNotEmpty(),
                        onSubscribe = viewModel::toggleSubscription,
                        onMix = { page.topSongs.firstOrNull()?.let(onItemClick) },
                        onPlay = { playList(page.topSongs, 0, null) }
                    )
                }

                page.featured?.let { featured ->
                    item(key = "featured") {
                        ArtistFeaturedCard(
                            label = stringResource(
                                when (featured.kind) {
                                    ReleaseKind.Single -> R.string.artist_new_single
                                    ReleaseKind.Ep -> R.string.artist_new_ep
                                    ReleaseKind.Album -> R.string.artist_new_album
                                }
                            ),
                            featured = featured,
                            onClick = { ItemNavigator.open(featured.release, title = featured.release.title) }
                        )
                    }
                }

                itemsIndexed(page.sections, key = { index, _ -> "section_$index" }) { _, section ->
                    HomeShelf(
                        section = section,
                        onItemClick = onItemClick,
                        onSectionClick = { link -> ItemNavigator.openLink(link, filter = section.title, title = page.name) }
                    )
                }

                item(key = "about") { ArtistAbout(page = page, profile = state.profile) }
            }
        }

        ArtistTopBar(
            title = page?.name,
            alpha = barAlpha,
            onBack = onBack,
            onShare = { shareLink(context, viewModel.shareUrl) }
        )
    }
}

// Arrow and share on top of the picture, which get a background with the name when the page scrolls
@Composable
private fun ArtistTopBar(
    title: String?,
    alpha: Float,
    onBack: () -> Unit,
    onShare: () -> Unit
) {
    val dimens = WavvyTheme.dimens

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background.copy(alpha = alpha))
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))
            .padding(horizontal = dimens.spaceSmall)
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = WavvyIcons.Back,
                contentDescription = stringResource(R.string.cd_back),
                tint = MaterialTheme.colorScheme.onBackground
            )
        }

        Text(
            text = title.orEmpty(),
            style = MaterialTheme.typography.titleMedium.merge(ArtistType.Bar),
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .padding(start = dimens.spaceSmall)
                .alpha(alpha)
        )

        IconButton(onClick = onShare) {
            Icon(
                imageVector = WavvyIcons.Share,
                contentDescription = stringResource(R.string.player_share),
                tint = MaterialTheme.colorScheme.onBackground
            )
        }
    }
}

// The picture of the artist fading into the background, with the name and the listeners of the month at its bottom
@Composable
private fun ArtistHero(page: ArtistPage) {
    val dimens = WavvyTheme.dimens
    val background = MaterialTheme.colorScheme.background

    Box(modifier = Modifier.fillMaxWidth().aspectRatio(1f / ArtistDimens.HeroHeightFraction)) {
        // The picture comes cut to the shape of the hero by YouTube, around the face, instead of being stretched
        AsyncImage(
            model = page.bannerUrl?.resize(ArtistDimens.HeroRequestWidth, ArtistDimens.HeroRequestHeight),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // A little dark on top so the arrow can be seen over any picture, and the bottom fades into the page
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Black.copy(alpha = ArtistDimens.HeroTopShade),
                        ArtistDimens.HeroTopShadeEnd to Color.Transparent,
                        1f - ArtistDimens.HeroFadeFraction to Color.Transparent,
                        1f to background
                    )
                )
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(horizontal = dimens.screenPadding)
        ) {
            Text(
                text = page.name,
                style = MaterialTheme.typography.headlineLarge.merge(ArtistType.Name),
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            page.monthlyListeners?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium.merge(ArtistType.Detail),
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }
    }
}

// The subscription of the account on the left, the mix and the play button on the right
@Composable
private fun ArtistActions(
    subscription: ArtistSubscription?,
    isEnabled: Boolean,
    onSubscribe: () -> Unit,
    onMix: () -> Unit,
    onPlay: () -> Unit
) {
    val dimens = WavvyTheme.dimens

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = dimens.screenPadding, vertical = dimens.spaceMedium)
    ) {
        subscription?.let { SubscribeButton(subscription = it, onClick = onSubscribe) }

        Spacer(modifier = Modifier.weight(1f))

        FilledIconButton(
            onClick = onMix,
            enabled = isEnabled,
            modifier = Modifier.size(ArtistDimens.MixSize),
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                contentColor = MaterialTheme.colorScheme.onBackground
            )
        ) {
            Icon(
                imageVector = WavvyIcons.Mix,
                contentDescription = stringResource(R.string.artist_mix),
                modifier = Modifier.size(ArtistDimens.MixIcon)
            )
        }

        Spacer(modifier = Modifier.width(ArtistDimens.ActionGap))

        // The play button is the one that stands out, light on the dark theme and dark on the light one
        FilledIconButton(
            onClick = onPlay,
            enabled = isEnabled,
            modifier = Modifier.size(ArtistDimens.PlaySize),
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = MaterialTheme.colorScheme.onBackground,
                contentColor = MaterialTheme.colorScheme.background
            )
        ) {
            Icon(
                imageVector = WavvyIcons.PlayArrow,
                contentDescription = stringResource(R.string.artist_play),
                modifier = Modifier.size(ArtistDimens.PlayIcon)
            )
        }
    }
}

// Pill with the state of the subscription and how many people are subscribed, as YouTube Music words them
@Composable
private fun SubscribeButton(
    subscription: ArtistSubscription,
    onClick: () -> Unit
) {
    val label = if (subscription.isSubscribed) subscription.subscribedLabel else subscription.unsubscribedLabel

    // Light with dark words while the account is not subscribed, which asks for the tap, and quiet once it is
    val container = if (subscription.isSubscribed) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.onBackground
    val content = if (subscription.isSubscribed) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.background

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .height(ArtistDimens.SubscribeHeight)
            .clip(CircleShape)
            .background(container)
            .clickable(onClick = onClick)
            .padding(horizontal = ArtistDimens.SubscribePadding)
    ) {
        label?.let {
            Text(text = it, style = MaterialTheme.typography.labelMedium, color = content)
        }

        subscription.countText?.let {
            Spacer(modifier = Modifier.width(ArtistDimens.SubscribeGap))
            Text(text = it, style = MaterialTheme.typography.labelMedium, color = content)
        }
    }
}

// The release the artist is promoting, a card with its cover, the line above it and its title
@Composable
private fun ArtistFeaturedCard(
    label: String,
    featured: ArtistFeatured,
    onClick: () -> Unit
) {
    val dimens = WavvyTheme.dimens

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = dimens.screenPadding, vertical = dimens.spaceSmall)
            .clip(RoundedCornerShape(ArtistDimens.FeaturedCorner))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable(onClick = onClick)
            .padding(ArtistDimens.FeaturedPadding)
    ) {
        ItemThumbnail(
            url = featured.release.thumbnailUrl,
            shape = RoundedCornerShape(HomeDimens.CoverCorner),
            modifier = Modifier.size(ArtistDimens.FeaturedCover)
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = ArtistDimens.FeaturedGap)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                ItemBadges(item = featured.release)
                Text(
                    text = featured.release.title,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Icon(
            imageVector = WavvyIcons.NavigateNext,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onBackground
        )
    }
}

// The description of the artist under the name, a few lines with its links, the button that shows the rest and the one that translates it
@Composable
private fun ArtistDescription(
    description: String,
    translation: String?,
    showTranslation: Boolean,
    onTranslate: () -> Unit
) {
    val dimens = WavvyTheme.dimens
    var isOpen by remember { mutableStateOf(false) }
    var isCut by remember { mutableStateOf(false) }
    val shown = if (showTranslation && translation != null) translation else description

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = dimens.screenPadding, end = dimens.spaceSmall, top = ArtistDimens.DescriptionGap)
            .animateContentSize()
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Text(
                text = linked(shown, MaterialTheme.colorScheme.primary),
                style = MaterialTheme.typography.bodyMedium.merge(ArtistType.Detail),
                color = MaterialTheme.colorScheme.secondary,
                maxLines = if (isOpen) Int.MAX_VALUE else ArtistDimens.DescriptionLines,
                overflow = TextOverflow.Ellipsis,
                onTextLayout = { if (!isOpen) isCut = it.hasVisualOverflow },
                modifier = Modifier
                    .weight(1f)
                    .padding(top = ArtistDimens.DescriptionGap)
            )

            // Only when the description is not in the language of the device
            if (translation != null) {
                IconButton(onClick = onTranslate, modifier = Modifier.size(ArtistDimens.TranslateButton)) {
                    Icon(
                        imageVector = WavvyIcons.Translate,
                        contentDescription = stringResource(R.string.artist_translate),
                        tint = if (showTranslation) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.size(ArtistDimens.TranslateIcon)
                    )
                }
            }
        }

        // Only when the text does not fit in the lines it has, or while it is open
        if (isCut || isOpen) {
            Text(
                text = stringResource(if (isOpen) R.string.artist_show_less else R.string.artist_show_more),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clickable { isOpen = !isOpen }
                    .padding(vertical = ArtistDimens.ShowMorePadding)
            )
        }
    }
}

// The text with its web addresses as links that open in the browser
private fun linked(text: String, color: Color) = buildAnnotatedString {
    var last = 0
    LinkPattern.findAll(text).forEach { match ->
        val address = match.value.trimEnd { it in LinkTrailingPunctuation }
        append(text.substring(last, match.range.first))
        withLink(LinkAnnotation.Url(address, TextLinkStyles(SpanStyle(color = color, textDecoration = TextDecoration.Underline)))) {
            append(address)
        }
        last = match.range.first + address.length
    }
    append(text.substring(last))
}

// Placeholder of the page while it loads, each piece where the real one will be, the name and the listeners over the picture,
// the lines of the description, the subscribe button with the mix and the play button at its side, and the first shelf
@Composable
private fun ArtistSkeleton() {
    val dimens = WavvyTheme.dimens

    SkeletonHost(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxWidth().aspectRatio(1f / ArtistDimens.HeroHeightFraction)) {
            Box(modifier = Modifier.fillMaxSize().skeleton(RectangleShape))

            Column(modifier = Modifier.align(Alignment.BottomStart).padding(horizontal = dimens.screenPadding)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(ArtistDimens.SkeletonNameFraction)
                        .height(ArtistDimens.SkeletonName)
                        .skeleton(MaterialTheme.shapes.small)
                )
                Spacer(modifier = Modifier.height(dimens.spaceSmall))
                Box(
                    modifier = Modifier
                        .fillMaxWidth(ArtistDimens.SkeletonListenersFraction)
                        .height(ArtistDimens.SkeletonLine)
                        .skeleton(MaterialTheme.shapes.extraSmall)
                )
            }
        }

        // The lines of the description
        Column(
            verticalArrangement = Arrangement.spacedBy(dimens.spaceSmall),
            modifier = Modifier.padding(horizontal = dimens.screenPadding, vertical = dimens.spaceMedium)
        ) {
            ArtistDimens.SkeletonDescriptionLines.forEach { fraction ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction)
                        .height(ArtistDimens.SkeletonLine)
                        .skeleton(MaterialTheme.shapes.extraSmall)
                )
            }
        }

        // The subscribe button, the mix and the play button, in the sizes of the real ones
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = dimens.screenPadding, vertical = dimens.spaceMedium)
        ) {
            Box(modifier = Modifier.size(ArtistDimens.SkeletonSubscribeWidth, ArtistDimens.SubscribeHeight).skeleton(CircleShape))
            Spacer(modifier = Modifier.weight(1f))
            Box(modifier = Modifier.size(ArtistDimens.MixSize).skeleton(CircleShape))
            Spacer(modifier = Modifier.width(ArtistDimens.ActionGap))
            Box(modifier = Modifier.size(ArtistDimens.PlaySize).skeleton(CircleShape))
        }

        // The title of the first shelf and its rows of songs
        Box(
            modifier = Modifier
                .padding(HomeDimens.TitlePadding)
                .size(HomeDimens.ShelfTitleWidth, HomeDimens.ShelfTitleHeight)
                .skeleton(MaterialTheme.shapes.extraSmall)
        )
        repeat(ArtistDimens.SkeletonSongRows) { SongRowSkeleton() }
    }
}

// A row of a song in the size of the real one, a cover and two lines
@Composable
private fun SongRowSkeleton() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .height(HomeDimens.ListHeight)
            .padding(horizontal = HomeDimens.ListPadding)
            .padding(horizontal = WavvyTheme.dimens.spaceSmall)
    ) {
        Box(
            modifier = Modifier
                .padding(HomeDimens.ListCoverPadding)
                .size(HomeDimens.ListCover)
                .skeleton(RoundedCornerShape(HomeDimens.CoverCorner))
        )

        Column(modifier = Modifier.padding(horizontal = HomeDimens.ListTextPadding)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(ArtistDimens.SkeletonRowTitleFraction)
                    .height(ArtistDimens.SkeletonLine)
                    .skeleton(MaterialTheme.shapes.extraSmall)
            )
            Spacer(modifier = Modifier.height(WavvyTheme.dimens.spaceExtraSmall))
            Box(
                modifier = Modifier
                    .fillMaxWidth(ArtistDimens.SkeletonRowSubtitleFraction)
                    .height(ArtistDimens.SkeletonLine)
                    .skeleton(MaterialTheme.shapes.extraSmall)
            )
        }
    }
}

package com.wavvy.app.features.podcast.ui

// Android context and sharing
import android.content.Context
import android.content.Intent
// Compose layouts and foundations
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
// Material 3 components
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
// Compose state and runtime
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
// UI styling and utilities
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
// Images and lifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
// Project resources
import com.wavvy.app.R
import com.wavvy.app.core.designsystem.icons.WavvyIcons
import com.wavvy.app.core.designsystem.theme.WavvyTheme
import com.wavvy.app.core.innertube.MusicOrigin
import com.wavvy.app.core.navigation.ItemNavigator
import com.wavvy.app.features.home.data.HomeItem
import com.wavvy.app.features.home.data.HomeItemKind
import com.wavvy.app.features.home.ui.components.HomeMessage
import com.wavvy.app.features.home.ui.rememberItemPlayer
import com.wavvy.app.features.menu.ItemMenu
import com.wavvy.app.features.player.ui.LocalMiniPlayerInset
import com.wavvy.app.features.podcast.data.EpisodePage

// The page of an episode, its picture with the length, the name, the buttons, the numbers and the whole description
@Composable
fun EpisodeScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: EpisodeViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()
    val onItemClick = rememberItemPlayer()
    val listState = rememberLazyListState()
    val context = LocalContext.current
    val page = state.page

    // The name of the episode comes into the bar once its own is mostly gone
    val threshold = with(LocalDensity.current) { EpisodeDimens.BarScrollThreshold.toPx() }
    val isScrolled by remember { derivedStateOf { listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > threshold } }
    val barAlpha by animateFloatAsState(
        targetValue = if (isScrolled) 1f else 0f,
        animationSpec = tween(PodcastDimens.BarFadeMillis),
        label = "EpisodeBar"
    )

    val topInset = WindowInsets.safeDrawing.only(WindowInsetsSides.Top).asPaddingValues().calculateTopPadding()

    // Solid, so the screen under it does not show through while the page slides
    Box(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        when {
            state.status == EpisodeStatus.Loading -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }

            state.status == EpisodeStatus.Error || page == null -> HomeMessage(
                text = stringResource(R.string.episode_error),
                actionLabel = stringResource(R.string.home_retry),
                onAction = viewModel::load
            )

            else -> {
                val item = HomeItem(kind = HomeItemKind.Episode, id = page.videoId, title = page.title, thumbnailUrl = page.coverUrl, author = page.showName)

                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        top = topInset + PodcastDimens.BarHeight,
                        bottom = LocalMiniPlayerInset.current + if (LocalMiniPlayerInset.current > 0.dp) PodcastDimens.MiniPlayerClearance else 0.dp
                    )
                ) {
                    item(key = "details") {
                        EpisodeDetails(page = page, item = item, context = context, onPlay = { onItemClick(item) })
                    }

                    item(key = "description") {
                        EpisodeDescription(page = page)
                    }
                }
            }
        }

        EpisodeBar(
            title = page?.title.orEmpty(),
            showName = page?.showName,
            showId = page?.showId,
            scrolledAlpha = barAlpha,
            onBack = onBack
        )
    }
}

// The bar on top, the arrow and the name of the podcast, which tap opens its page, and the name of the episode with a line under it when the page scrolls
@Composable
private fun EpisodeBar(title: String, showName: String?, showId: String?, scrolledAlpha: Float, onBack: () -> Unit) {
    val dimens = WavvyTheme.dimens

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background.copy(alpha = scrolledAlpha))
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))
    ) {
        Box(modifier = Modifier.fillMaxWidth().height(PodcastDimens.BarHeight)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.align(Alignment.CenterStart).padding(horizontal = dimens.spaceSmall)
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = WavvyIcons.Back,
                        contentDescription = stringResource(R.string.cd_back),
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }

                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge.merge(PodcastType.BarTitle),
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .padding(start = dimens.spaceSmall)
                        .graphicsLayer { alpha = scrolledAlpha }
                )
            }

            if (showName != null) {
                Text(
                    text = showName,
                    style = MaterialTheme.typography.bodyMedium.merge(EpisodeType.ShowName),
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .graphicsLayer { alpha = 1f - scrolledAlpha }
                        .clickable(enabled = showId != null) { showId?.let { ItemNavigator.openPodcast(it) } }
                )
            }
        }

        if (scrolledAlpha > 0f) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = scrolledAlpha))
        }
    }
}

// The picture, how long the episode is, its name, the buttons and the numbers
@Composable
private fun EpisodeDetails(page: EpisodePage, item: HomeItem, context: Context, onPlay: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
            .padding(horizontal = PodcastDimens.Side)
    ) {
        AsyncImage(
            model = page.coverUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .padding(top = EpisodeDimens.PictureTop)
                .width(EpisodeDimens.PictureWidth)
                .aspectRatio(EpisodePictureRatio)
                .clip(RoundedCornerShape(EpisodeDimens.PictureCorner))
                .background(MaterialTheme.colorScheme.surfaceContainer)
        )

        page.durationText?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodyMedium.merge(EpisodeType.Length),
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(top = EpisodeDimens.LengthTop)
            )
        }

        Text(
            text = page.title,
            style = MaterialTheme.typography.headlineSmall.merge(EpisodeType.Title),
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = EpisodeDimens.TitleTop)
        )

        // Download and put in the queue of episodes for later, which only stand in their places for now, then play, share and more
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(PodcastDimens.ActionGap),
            modifier = Modifier.padding(top = PodcastDimens.ActionsTop)
        ) {
            RoundButton(WavvyIcons.Download, PodcastDimens.ActionSize) { }
            RoundButton(WavvyIcons.AddCircle, PodcastDimens.ActionSize) { }

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(EpisodeDimens.PlaySize)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onBackground)
                    .clickable(onClick = onPlay)
            ) {
                Icon(
                    imageVector = WavvyIcons.PlayArrow,
                    contentDescription = stringResource(R.string.artist_play),
                    tint = MaterialTheme.colorScheme.background,
                    modifier = Modifier.size(EpisodeDimens.PlayIcon)
                )
            }

            RoundButton(WavvyIcons.Share, PodcastDimens.ActionSize) { shareLink(context, "$MusicOrigin/watch?v=${page.videoId}") }
            RoundButton(WavvyIcons.MoreVertical, PodcastDimens.ActionSize) { ItemMenu.show(item) }
        }

        // The views and the day it came out, the likes are not in what YouTube Music gives
        Row(
            horizontalArrangement = Arrangement.spacedBy(PodcastDimens.ActionGap),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = EpisodeDimens.NumbersTop, bottom = EpisodeDimens.NumbersBottom)
        ) {
            page.views?.let { views ->
                val number = ViewsPattern.find(views)?.groupValues?.get(1)?.trim() ?: views
                NumberTile(value = number, label = stringResource(R.string.episode_views), modifier = Modifier.weight(1f))
            }
            page.age?.let { age ->
                NumberTile(value = age, label = stringResource(R.string.episode_published), modifier = Modifier.weight(1f))
            }
        }
    }
}

// What comes before the word views, in the two languages of the app, so the number stands alone on its tile
private val ViewsPattern = Regex("^(.*?)\\s*(?:de\\s+)?(?:visualiza\\S*|views?)\\s*$", RegexOption.IGNORE_CASE)

// A round button with an icon
@Composable
private fun RoundButton(icon: ImageVector, size: Dp, onClick: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(WavvyTheme.colors.chip)
            .clickable(onClick = onClick)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(PodcastDimens.ActionIcon)
        )
    }
}

// A number in bold with its name under it, on a rounded back
@Composable
private fun NumberTile(value: String, label: String, modifier: Modifier = Modifier) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .height(EpisodeDimens.TileHeight)
            .clip(RoundedCornerShape(EpisodeDimens.TileCorner))
            .background(WavvyTheme.colors.chip)
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium.merge(EpisodeType.TileValue),
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium.merge(EpisodeType.TileLabel),
            color = MaterialTheme.colorScheme.secondary,
            maxLines = 1
        )
    }
}

// The whole description, the pieces that link somewhere in white and the ones with an address open it
@Composable
private fun EpisodeDescription(page: EpisodePage) {
    val link = MaterialTheme.colorScheme.onBackground

    Text(
        text = buildAnnotatedString {
            page.description.forEach { part ->
                when {
                    part.url != null -> withLink(LinkAnnotation.Url(part.url, TextLinkStyles(SpanStyle(color = link)))) { append(part.text) }
                    part.isLink -> withStyle(SpanStyle(color = link)) { append(part.text) }
                    else -> append(part.text)
                }
            }
        },
        style = MaterialTheme.typography.bodyLarge.merge(EpisodeType.Description),
        color = MaterialTheme.colorScheme.secondary,
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
            .padding(horizontal = PodcastDimens.Side)
    )
}

// The shape of the picture of an episode
private const val EpisodePictureRatio = 16f / 9f

// Opens the sharing sheet of the system with the link
private fun shareLink(context: Context, url: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, url)
    }
    context.startActivity(Intent.createChooser(intent, context.getString(R.string.player_share_via)))
}

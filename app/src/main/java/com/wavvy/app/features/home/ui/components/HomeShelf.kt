package com.wavvy.app.features.home.ui.components

// Compose layouts and foundations
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.snapping.SnapLayoutInfoProvider
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
// Material 3 components
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
// Compose state and runtime
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
// UI styling and utilities
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
// Image loading
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.transformations
// Project resources
import com.wavvy.app.R
import com.wavvy.app.core.designsystem.components.PinBadge
import com.wavvy.app.core.designsystem.components.TrendMark
import com.wavvy.app.core.designsystem.components.VideoBadge
import com.wavvy.app.core.designsystem.components.VideoSquareCrop
import com.wavvy.app.core.designsystem.icons.WavvyIcons
import com.wavvy.app.core.designsystem.theme.WavvyTheme
import com.wavvy.app.core.innertube.isVideoThumbnail
import com.wavvy.app.core.innertube.resize
import com.wavvy.app.core.navigation.ItemNavigator
import com.wavvy.app.features.home.data.HomeItem
import com.wavvy.app.features.home.data.HomeItemKind
import com.wavvy.app.features.home.data.HomeLink
import com.wavvy.app.features.home.data.HomeSection
import com.wavvy.app.features.home.ui.rememberListPlayer
import com.wavvy.app.features.menu.ItemMenu
import com.wavvy.app.core.download.LocalDownloadFolder
import com.wavvy.app.features.menu.LocalEditablePlaylist

// Shelf of the Home as Metrolist (GPL-3.0) draws it, a shelf of only songs is a list of rows and the others are covers
@Composable
fun HomeShelf(
    section: HomeSection,
    onItemClick: (HomeItem) -> Unit,
    modifier: Modifier = Modifier,
    onSectionClick: ((HomeLink) -> Unit)? = null,
    isRanked: Boolean = false
) {
    val items = section.items.distinctBy { it.id }
    val hasSongs = items.any { it.kind == HomeItemKind.Song }
    val isSongsOnly = items.isNotEmpty() && items.all { it.kind == HomeItemKind.Song }
    val playList = rememberListPlayer()

    Column(modifier = modifier.fillMaxWidth()) {
        // The title opens the page of the shelf, the play all button plays the songs of the shelf
        HomeSectionTitle(
            title = section.title,
            label = section.label,
            thumbnail = section.thumbnailUrl?.let { url ->
                {
                    AsyncImage(
                        model = url,
                        contentDescription = null,
                        modifier = Modifier
                            .size(HomeDimens.TitlePhoto)
                            .clip(if (section.link?.isArtist == true) CircleShape else RoundedCornerShape(HomeDimens.CoverCorner))
                    )
                }
            },
            onClick = section.link?.let { link -> { if (onSectionClick != null) onSectionClick(link) else ItemNavigator.openLink(link, section.title) } },
            onPlayAllClick = if (hasSongs) ({ playList(items.filter { it.kind == HomeItemKind.Song }, 0, null) }) else null
        )

        if (isSongsOnly) HomeSongGrid(items = items, onItemClick = onItemClick, isRanked = isRanked) else HomeCoverRow(items = items, onItemClick = onItemClick)
    }
}

// Title of a shelf with the small line above it, the photo before it and the arrow when it opens a page
@Composable
fun HomeSectionTitle(
    title: String,
    modifier: Modifier = Modifier,
    label: String? = null,
    thumbnail: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    onPlayAllClick: (() -> Unit)? = null
) {
    val primary = MaterialTheme.colorScheme.primary
    val base = MaterialTheme.typography.bodyMedium

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(HomeDimens.TitleSpacing),
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Horizontal))
            .clickable(enabled = onClick != null) { onClick?.invoke() }
            .padding(HomeDimens.TitlePadding)
    ) {
        thumbnail?.invoke()

        Column(
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.weight(1f)
        ) {
            label?.let { label ->
                Text(
                    text = label,
                    style = base.merge(HomeType.SectionLabel),
                    color = MaterialTheme.colorScheme.onBackground,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Text(
                text = title,
                style = base.merge(HomeType.SectionTitle),
                color = primary,
                overflow = TextOverflow.Ellipsis,
                maxLines = 1
            )
        }

        onPlayAllClick?.let { playAllClick ->
            OutlinedButton(
                onClick = playAllClick,
                shape = RoundedCornerShape(HomeDimens.PlayAllCorner),
                border = BorderStroke(HomeDimens.PlayAllBorder, primary.copy(alpha = HomeDimens.PlayAllBorderAlpha)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = primary),
                contentPadding = PaddingValues(
                    horizontal = HomeDimens.PlayAllPaddingHorizontal,
                    vertical = HomeDimens.PlayAllPaddingVertical
                ),
                modifier = Modifier.height(HomeDimens.PlayAllHeight)
            ) {
                Text(
                    text = stringResource(R.string.home_play_all),
                    style = base.merge(HomeType.PlayAll)
                )
            }
        }

        if (onClick != null) {
            Icon(
                imageVector = WavvyIcons.ArrowForward,
                contentDescription = null,
                tint = primary
            )
        }
    }
}

// Row of cards with a square cover each
@Composable
fun HomeCoverRow(
    items: List<HomeItem>,
    onItemClick: (HomeItem) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier,
        contentPadding = WindowInsets.systemBars.only(WindowInsetsSides.Horizontal).asPaddingValues()
    ) {
        items(items.distinctBy { it.id }, key = { it.id }) { item ->
            HomeGridItem(item = item, onClick = { onItemClick(item) })
        }
    }
}

// Columns of four rows of songs, two columns side by side when the screen is wide enough
@Composable
private fun HomeSongGrid(
    items: List<HomeItem>,
    onItemClick: (HomeItem) -> Unit,
    isRanked: Boolean
) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val widthFactor = if (maxWidth * HomeDimens.SongColumnWideFraction >= HomeDimens.SongColumnMinWidth) {
            HomeDimens.SongColumnWideFraction
        } else {
            HomeDimens.SongColumnNarrowFraction
        }
        val itemWidth = maxWidth * widthFactor

        // A fling stops with a whole column at the start and the next one peeking, as in Metrolist and the old Wavvy
        val gridState = rememberLazyGridState()
        val snapLayout = remember(gridState, widthFactor) {
            columnSnapLayout(gridState) { layoutSize, itemSize -> layoutSize * widthFactor / 2f - itemSize / 2f }
        }

        LazyHorizontalGrid(
            state = gridState,
            rows = GridCells.Fixed(HomeDimens.SongRows),
            flingBehavior = rememberSnapFlingBehavior(snapLayout),
            contentPadding = WindowInsets.systemBars.only(WindowInsetsSides.Horizontal).asPaddingValues(),
            modifier = Modifier
                .fillMaxWidth()
                .height(HomeDimens.ListHeight * HomeDimens.SongRows)
        ) {
            itemsIndexed(items, key = { _, song -> song.id }) { index, song ->
                HomeListItem(item = song, onClick = { onItemClick(song) }, modifier = Modifier.width(itemWidth), rank = if (isRanked) index + 1 else null)
            }
        }
    }
}

// Snap points of a grid that scrolls sideways, adapted from Metrolist (GPL-3.0), a fling goes to the next column in its direction
internal fun columnSnapLayout(
    gridState: LazyGridState,
    positionInLayout: (layoutSize: Float, itemSize: Float) -> Float
): SnapLayoutInfoProvider = object : SnapLayoutInfoProvider {
    override fun calculateApproachOffset(velocity: Float, decayOffset: Float): Float = 0f

    override fun calculateSnapOffset(velocity: Float): Float {
        val layoutInfo = gridState.layoutInfo
        val containerSize = layoutInfo.viewportSize.width - layoutInfo.beforeContentPadding - layoutInfo.afterContentPadding
        var lower = Float.NEGATIVE_INFINITY
        var upper = Float.POSITIVE_INFINITY

        // Nearest snap point behind and ahead of the current position
        layoutInfo.visibleItemsInfo.forEach { item ->
            val distance = item.offset.x - positionInLayout(containerSize.toFloat(), item.size.width.toFloat())
            if (distance <= 0 && distance > lower) lower = distance
            if (distance >= 0 && distance < upper) upper = distance
        }

        return when {
            velocity < 0 -> lower
            velocity > 0 -> upper
            else -> 0f
        }
    }
}

// Card with the cover, the title that slides when it does not fit and the line under it
@Composable
internal fun HomeGridItem(
    item: HomeItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onCoverLoaded: () -> Unit = {}
) {
    val isArtist = item.kind == HomeItemKind.Artist
    val base = MaterialTheme.typography.bodyMedium

    Column(
        modifier = modifier
            // A long press opens the menu of a song
            .combinedClickable(onClick = onClick, onLongClick = { ItemMenu.show(item) })
            .padding(HomeDimens.GridPadding)
            .width(HomeDimens.GridCover)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .height(HomeDimens.GridCover)
                .aspectRatio(1f)
        ) {
            ItemThumbnail(
                url = item.thumbnailUrl,
                shape = coverShape(item),
                isVideo = item.isVideo,
                isPinned = item.id in LocalPinnedIds.current,
                onLoaded = onCoverLoaded
            )

            // Play buttons do nothing until there is a player
            if (item.kind == HomeItemKind.Song) OverlayPlayButton()
            if (item.kind == HomeItemKind.Album) AlbumPlayButton()
        }

        Spacer(modifier = Modifier.height(HomeDimens.GridTextGap))

        Text(
            text = item.title,
            style = base.merge(HomeType.GridTitle),
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = if (isArtist) TextAlign.Center else TextAlign.Start,
            modifier = Modifier
                .basicMarquee()
                .fillMaxWidth()
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            ItemBadges(item = item)

            itemSubtitle(item)?.let { subtitle ->
                Text(
                    text = subtitle,
                    style = base.merge(HomeType.GridSubtitle),
                    color = MaterialTheme.colorScheme.secondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

// Row of a song with its small cover, the title, the line under it and the menu button, a number takes the place of the cover and a rank stands before it
@Composable
internal fun HomeListItem(
    item: HomeItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    number: Int? = null,
    rank: Int? = null
) {
    val subtitle = itemSubtitle(item)
    val base = MaterialTheme.typography.bodyMedium
    val editablePlaylist = LocalEditablePlaylist.current
    val downloadFolder = LocalDownloadFolder.current

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .combinedClickable(onClick = onClick, onLongClick = { ItemMenu.show(item, editablePlaylist, downloadFolder) })
            .height(HomeDimens.ListHeight)
            .padding(horizontal = HomeDimens.ListPadding)
    ) {
        // The place of the song in the list, before the cover
        if (rank != null) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(HomeDimens.RankWidth)) {
                Text(
                    text = rank.toString(),
                    style = base.merge(HomeType.Rank),
                    color = MaterialTheme.colorScheme.secondary,
                    textAlign = TextAlign.Center
                )
                // How the song moved in the chart, when the list is one
                item.trend?.let { TrendMark(it, HomeDimens.TrendMark) }
            }
        }

        Box(
            modifier = Modifier.padding(HomeDimens.ListCoverPadding),
            contentAlignment = Alignment.Center
        ) {
            if (number != null) {
                Text(
                    text = number.toString(),
                    style = base.merge(HomeType.ListSubtitle),
                    color = MaterialTheme.colorScheme.secondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.size(HomeDimens.ListCover).wrapContentHeight(Alignment.CenterVertically)
                )
            } else {
                ItemThumbnail(
                    url = item.thumbnailUrl,
                    shape = coverShape(item),
                    modifier = Modifier.size(HomeDimens.ListCover),
                    isVideo = item.isVideo,
                    isPinned = item.id in LocalPinnedIds.current
                )
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = HomeDimens.ListTextPadding)
        ) {
            Text(
                text = item.title,
                style = base.merge(HomeType.ListTitle),
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                ItemBadges(item = item)

                if (!subtitle.isNullOrEmpty()) {
                    Text(
                        text = subtitle,
                        color = MaterialTheme.colorScheme.secondary,
                        style = base.merge(HomeType.ListSubtitle),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        IconButton(onClick = { ItemMenu.show(item, editablePlaylist, downloadFolder) }) {
            Icon(
                imageVector = WavvyIcons.MoreVertical,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onBackground
            )
        }
    }
}

// Ids of the songs pinned to the speed dial, so any cover of the Home can show the pin without the id going through every card
val LocalPinnedIds = compositionLocalOf { emptySet<String>() }

// Cover of an item, round for an artist, cropped to fill the square, a video cut square and marked with a camera and a pinned song with a pin
@Composable
internal fun ItemThumbnail(
    url: String?,
    shape: Shape,
    modifier: Modifier = Modifier,
    isVideo: Boolean = false,
    isPinned: Boolean = false,
    onLoaded: () -> Unit = {}
) {
    val context = LocalContext.current
    val model = remember(url) {
        url?.let { address ->
            ImageRequest.Builder(context)
                .data(address.resize(HomeDimens.CoverRequestSize, HomeDimens.CoverRequestSize))
                .apply { if (address.isVideoThumbnail()) transformations(VideoSquareCrop) }
                .build()
        }
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxSize()
            .aspectRatio(1f)
            .clip(shape)
    ) {
        AsyncImage(
            model = model,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            // A cover that failed counts as loaded too, so what waits for it never stays hidden
            onSuccess = { onLoaded() },
            onError = { onLoaded() },
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
        )

        CoverBadges(isVideo = isVideo, isPinned = isPinned)
    }
}

// The camera of a video on the top start corner of a cover and the pin of a pinned song on the top end one
@Composable
internal fun BoxScope.CoverBadges(isVideo: Boolean, isPinned: Boolean) {
    if (isVideo) {
        VideoBadge(
            iconSize = HomeDimens.CoverBadgeIcon,
            padding = HomeDimens.CoverBadgePadding,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(HomeDimens.CoverBadgeInset)
        )
    }
    if (isPinned) {
        PinBadge(
            iconSize = HomeDimens.CoverBadgeIcon,
            padding = HomeDimens.CoverBadgePadding,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(HomeDimens.CoverBadgeInset)
        )
    }
}

// A song of the Home that is a video, told by its wide picture since the Home does not say it
internal val HomeItem.isVideo: Boolean
    get() = kind == HomeItemKind.Song && thumbnailUrl?.isVideoThumbnail() == true

// Round play button in the middle of the cover of a song
@Composable
private fun BoxScope.OverlayPlayButton() {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .align(Alignment.Center)
            .size(HomeDimens.PlayButton)
            .clip(CircleShape)
            .background(WavvyTheme.colors.playButton)
    ) {
        Icon(
            imageVector = WavvyIcons.Play,
            contentDescription = null,
            tint = WavvyTheme.colors.onMedia,
            modifier = Modifier.size(HomeDimens.PlayIcon)
        )
    }
}

// Round play button in the bottom end corner of the cover of an album
@Composable
private fun BoxScope.AlbumPlayButton() {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .align(Alignment.BottomEnd)
            .padding(HomeDimens.AlbumPlayInset)
            .size(HomeDimens.PlayButton)
            .clip(CircleShape)
            .background(WavvyTheme.colors.playButton)
            .clickable {}
    ) {
        Icon(
            imageVector = WavvyIcons.Play,
            contentDescription = null,
            tint = WavvyTheme.colors.onMedia,
            modifier = Modifier.size(HomeDimens.AlbumPlayIcon)
        )
    }
}

// Marks before the line of an item, only explicit lyrics for now
@Composable
internal fun RowScope.ItemBadges(item: HomeItem) {
    if (item.isExplicit) {
        Icon(
            imageVector = WavvyIcons.Explicit,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier
                .size(HomeDimens.Badge)
                .padding(end = HomeDimens.BadgeEnd)
        )
    }
}

// Shape of a cover, round for an artist
internal fun coverShape(item: HomeItem): Shape =
    if (item.kind == HomeItemKind.Artist) CircleShape else RoundedCornerShape(HomeDimens.CoverCorner)

// Line under the title, built from the data of the item as Metrolist does, an artist has none unless the item brings how many listen to it
@Composable
internal fun itemSubtitle(item: HomeItem): String? {
    val conjunction = " ${stringResource(R.string.home_and)} "

    return when (item.kind) {
        HomeItemKind.Song -> joinByBullet(item.artists.joinToArtistString(conjunction), makeTimeString(item.durationSeconds), item.countText)
        HomeItemKind.Album -> joinByBullet(item.artists.joinToArtistString(conjunction), item.countText)
        HomeItemKind.Artist -> item.countText
        HomeItemKind.Playlist, HomeItemKind.Podcast -> joinByBullet(item.author, item.countText)
        HomeItemKind.Episode -> joinByBullet(item.author, makeTimeString(item.durationSeconds))
    }
}

// Names of the artists, the last one joined by the conjunction
private fun List<String>.joinToArtistString(conjunction: String): String = when (size) {
    0 -> ""
    1 -> this[0]
    2 -> "${this[0]}$conjunction${this[1]}"
    else -> dropLast(1).joinToString(", ") + "$conjunction${last()}"
}

// Length as minutes and seconds, with hours and days when it has them
private fun makeTimeString(durationSeconds: Int?): String {
    if (durationSeconds == null || durationSeconds < 0) return ""
    var seconds = durationSeconds.toLong()
    val days = seconds / SecondsInDay
    seconds %= SecondsInDay
    val hours = seconds / SecondsInHour
    seconds %= SecondsInHour
    val minutes = seconds / SecondsInMinute
    seconds %= SecondsInMinute

    return when {
        days > 0 -> "%d:%02d:%02d:%02d".format(days, hours, minutes, seconds)
        hours > 0 -> "%d:%02d:%02d".format(hours, minutes, seconds)
        else -> "%d:%02d".format(minutes, seconds)
    }
}

// Parts of a line that are not empty, joined by a bullet
private fun joinByBullet(vararg parts: String?): String = parts.filterNot { it.isNullOrEmpty() }.joinToString(" • ")

// Seconds in a day, an hour and a minute
private const val SecondsInDay = 86400L
private const val SecondsInHour = 3600L
private const val SecondsInMinute = 60L

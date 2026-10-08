package com.wavvy.app.features.discover.ui

// Compose layouts and foundations
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.grid.itemsIndexed as gridItemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
// Material 3 components
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
// Compose state and runtime
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
// UI styling and utilities
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
// Lifecycle and image loading
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
// Project resources
import com.wavvy.app.R
import com.wavvy.app.core.designsystem.icons.WavvyIcons
import com.wavvy.app.core.designsystem.theme.WavvyTheme
import com.wavvy.app.core.innertube.isVideoThumbnail
import com.wavvy.app.core.innertube.resize
import com.wavvy.app.core.navigation.ItemNavigator
import com.wavvy.app.features.discover.data.DiscoverBlock
import com.wavvy.app.features.discover.data.DiscoverMood
import com.wavvy.app.features.discover.data.DiscoverShortcut
import com.wavvy.app.features.home.data.HomeItem
import com.wavvy.app.features.home.data.HomeLink
import com.wavvy.app.features.home.data.HomeSection
import com.wavvy.app.features.home.ui.components.HomeDimens
import com.wavvy.app.features.home.ui.components.HomeHeader
import com.wavvy.app.features.home.ui.components.HomeMessage
import com.wavvy.app.features.home.ui.components.CoverBadges
import com.wavvy.app.features.home.ui.components.ItemBadges
import com.wavvy.app.features.home.ui.components.isVideo
import com.wavvy.app.features.home.ui.components.columnSnapLayout
import com.wavvy.app.features.home.ui.components.itemSubtitle
import com.wavvy.app.features.home.ui.rememberItemPlayer
import com.wavvy.app.features.menu.ItemMenu
import com.wavvy.app.features.player.ui.LocalMiniPlayerInset

// Discover tab, the page that YouTube Music calls Explore, the big buttons and then the shelves of new releases, moods and genres, episodes, what is rising and new videos
@Composable
fun DiscoverScreen(
    onProfileClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DiscoverViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()
    val onItemClick = rememberItemPlayer()
    val page = state.page

    Column(modifier = modifier.fillMaxSize()) {
        HomeHeader(onNotificationsClick = {}, onProfileClick = onProfileClick)

        when {
            state.status == DiscoverStatus.Loading -> DiscoverSkeleton(modifier = Modifier.weight(1f))

            state.status == DiscoverStatus.Error || page == null -> HomeMessage(
                text = stringResource(R.string.home_error),
                modifier = Modifier.weight(1f),
                actionLabel = stringResource(R.string.home_retry),
                onAction = viewModel::load
            )

            else -> LazyColumn(
                modifier = Modifier.weight(1f),
                // The mini player covers the end of the page, with a little more room so it does not touch the last shelf
                contentPadding = PaddingValues(
                    bottom = LocalMiniPlayerInset.current + if (LocalMiniPlayerInset.current > 0.dp) DiscoverDimens.MiniPlayerClearance else 0.dp
                )
            ) {
                item(key = "shortcuts") { DiscoverShortcuts(shortcuts = page.shortcuts) }

                // The buttons, the arrows of the shelves and the moods open their pages as those pages are built
                items(page.blocks.size, key = { "block_$it" }) { index ->
                    when (val block = page.blocks[index]) {
                        is DiscoverBlock.Covers -> DiscoverCoverShelf(section = block.section, onItemClick = onItemClick)
                        is DiscoverBlock.Ranked -> DiscoverRankedShelf(section = block.section, onItemClick = onItemClick)
                        is DiscoverBlock.Wide -> DiscoverWideShelf(section = block.section, onItemClick = onItemClick)
                        is DiscoverBlock.Moods -> DiscoverMoodShelf(title = block.title, link = block.link, moods = block.moods)
                    }
                }

                item(key = "end") { Spacer(modifier = Modifier.height(WavvyTheme.dimens.spaceExtraLarge)) }
            }
        }
    }
}

// The big buttons of the top, two by two, each with its icon over its name
@Composable
private fun DiscoverShortcuts(shortcuts: List<DiscoverShortcut>) {
    Column(
        verticalArrangement = Arrangement.spacedBy(DiscoverDimens.Gap),
        modifier = Modifier
            .fillMaxWidth()
            .padding(WindowInsets.systemBars.only(WindowInsetsSides.Horizontal).asPaddingValues())
            .padding(horizontal = DiscoverDimens.Side, vertical = WavvyTheme.dimens.spaceSmall)
    ) {
        shortcuts.chunked(ShortcutColumns).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(DiscoverDimens.Gap), modifier = Modifier.fillMaxWidth()) {
                row.forEach { shortcut ->
                    ShortcutCard(shortcut = shortcut, modifier = Modifier.weight(1f))
                }
                // A row with one button leaves the space of the other
                if (row.size < ShortcutColumns) Spacer(modifier = Modifier.weight((ShortcutColumns - row.size).toFloat()))
            }
        }
    }
}

// How many big buttons sit side by side
internal const val ShortcutColumns = 2

// One big button, its icon is the one of the kind of page it opens
@Composable
private fun ShortcutCard(shortcut: DiscoverShortcut, modifier: Modifier = Modifier) {
    Column(
        verticalArrangement = Arrangement.SpaceBetween,
        modifier = modifier
            .height(DiscoverDimens.ShortcutHeight)
            .clip(RoundedCornerShape(DiscoverDimens.ShortcutCorner))
            .background(WavvyTheme.colors.chip)
            .clickable { }
            .padding(DiscoverDimens.ShortcutPadding)
    ) {
        Icon(
            imageVector = iconOf(shortcut.icon),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.size(DiscoverDimens.ShortcutIcon)
        )

        Text(
            text = shortcut.title,
            style = MaterialTheme.typography.bodyLarge.merge(DiscoverType.Shortcut),
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.basicMarquee()
        )
    }
}

// The icon of a button by the name YouTube Music gives it, a note for what it does not know
private fun iconOf(name: String?): ImageVector = when (name) {
    "MUSIC_NEW_RELEASE" -> WavvyIcons.Album
    "TRENDING_UP" -> WavvyIcons.TrendingUp
    "STICKER_EMOTICON" -> WavvyIcons.Mood
    "BROADCAST" -> WavvyIcons.Podcasts
    else -> WavvyIcons.MusicNote
}

// Title of a shelf in white with a white arrow when it opens a page, as YouTube Music draws it
@Composable
private fun DiscoverShelfTitle(title: String, link: HomeLink?) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(WindowInsets.systemBars.only(WindowInsetsSides.Horizontal).asPaddingValues())
            .clickable(enabled = link != null) { link?.let { ItemNavigator.openLink(it, title) } }
            .padding(horizontal = DiscoverDimens.Side, vertical = DiscoverDimens.TitleVertical)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge.merge(DiscoverType.Shelf),
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .basicMarquee()
        )

        if (link != null) {
            Icon(
                imageVector = WavvyIcons.NavigateNext,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.size(DiscoverDimens.TitleArrow)
            )
        }
    }
}

// Width of a card of a row that shows two whole ones and the start of a third, from the width that is left after the sides and the gaps
internal fun coverWidth(maxWidth: Dp): Dp =
    (maxWidth - DiscoverDimens.Side * DiscoverDimens.CoverGapsAroundCards) / DiscoverDimens.CoverColumns

// New releases as big covers in a row, with the kind and the artist under each
@Composable
private fun DiscoverCoverShelf(section: HomeSection, onItemClick: (HomeItem) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        DiscoverShelfTitle(title = section.title, link = section.link)

        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val cardWidth = coverWidth(maxWidth)

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(DiscoverDimens.Gap),
                contentPadding = PaddingValues(horizontal = DiscoverDimens.Side)
            ) {
                items(section.items.distinctBy { it.id }, key = { it.id }) { item ->
                    CoverCard(item = item, onClick = { onItemClick(item) }, modifier = Modifier.width(cardWidth))
                }
            }
        }
    }
}

// A cover with its title and, under it, the kind and the artists
@Composable
private fun CoverCard(item: HomeItem, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val line = listOfNotNull(item.typeText, itemSubtitle(item)).joinToString(" • ").ifEmpty { null }

    Column(modifier = modifier.clickable(onClick = onClick)) {
        AsyncImage(
            model = item.thumbnailUrl?.resize(HomeDimens.CoverRequestSize, HomeDimens.CoverRequestSize),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(DiscoverDimens.CoverCorner))
                .background(MaterialTheme.colorScheme.surfaceContainer)
        )

        Spacer(modifier = Modifier.height(DiscoverDimens.CardTextGap))

        CardTexts(title = item.title, line = line, item = item)
    }
}

// The title of a card and its line, with the badge of explicit lyrics before the line
@Composable
private fun CardTexts(title: String, line: String?, item: HomeItem) {
    Text(
        text = title,
        style = MaterialTheme.typography.bodyMedium.merge(DiscoverType.CardTitle),
        color = MaterialTheme.colorScheme.onBackground,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.basicMarquee()
    )

    Row(verticalAlignment = Alignment.CenterVertically) {
        ItemBadges(item = item)

        line?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodyMedium.merge(DiscoverType.CardLine),
                color = MaterialTheme.colorScheme.secondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.basicMarquee()
            )
        }
    }
}

// Moods and genres as buttons with a colored stripe on their side, three rows that slide sideways, each as wide as a cover
@Composable
private fun DiscoverMoodShelf(title: String, link: HomeLink?, moods: List<DiscoverMood>) {
    Column(modifier = Modifier.fillMaxWidth()) {
        DiscoverShelfTitle(title = title, link = link)

        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val buttonWidth = coverWidth(maxWidth)

            LazyHorizontalGrid(
                rows = GridCells.Fixed(DiscoverDimens.MoodRows),
                horizontalArrangement = Arrangement.spacedBy(DiscoverDimens.Gap),
                verticalArrangement = Arrangement.spacedBy(DiscoverDimens.Gap),
                contentPadding = PaddingValues(horizontal = DiscoverDimens.Side),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(DiscoverDimens.MoodHeight * DiscoverDimens.MoodRows + DiscoverDimens.Gap * (DiscoverDimens.MoodRows - 1))
            ) {
                gridItems(moods, key = { it.title + it.params }) { mood -> MoodButton(mood = mood, modifier = Modifier.width(buttonWidth)) }
            }
        }
    }
}

// A mood or a genre
@Composable
private fun MoodButton(mood: DiscoverMood, modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .height(DiscoverDimens.MoodHeight)
            .clip(RoundedCornerShape(DiscoverDimens.MoodCorner))
            .background(WavvyTheme.colors.chip)
            .clickable { }
    ) {
        Box(
            modifier = Modifier
                .width(DiscoverDimens.MoodStripe)
                .fillMaxHeight()
                .background(Color(mood.color))
        )

        Text(
            text = mood.title,
            style = MaterialTheme.typography.bodyLarge.merge(DiscoverType.Mood),
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = WavvyTheme.dimens.spaceMedium)
                .basicMarquee()
        )
    }
}

// Videos and episodes as wide cards, the picture as it is, wider than tall, with the title and the line with the views and the age under it
@Composable
private fun DiscoverWideShelf(section: HomeSection, onItemClick: (HomeItem) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        DiscoverShelfTitle(title = section.title, link = section.link)

        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val cardWidth = maxWidth - DiscoverDimens.WideRemainder
            val cards = section.items.distinctBy { it.id }

            // One card at a time whatever the strength of the swipe, as the speed dial moves its pages
            HorizontalPager(
                state = rememberPagerState(pageCount = { cards.size }),
                pageSize = PageSize.Fixed(cardWidth),
                pageSpacing = DiscoverDimens.Gap,
                contentPadding = PaddingValues(horizontal = DiscoverDimens.Side),
                verticalAlignment = Alignment.Top,
                key = { cards[it].id },
                modifier = Modifier.fillMaxWidth()
            ) { page ->
                val item = cards[page]
                WideCard(item = item, onClick = { onItemClick(item) }, modifier = Modifier.width(cardWidth))
            }
        }
    }
}

// A wide card
@Composable
private fun WideCard(item: HomeItem, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier.clickable(onClick = onClick)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(WideAspectRatio)
                .clip(RoundedCornerShape(DiscoverDimens.WideCorner))
                .background(MaterialTheme.colorScheme.surfaceContainer)
        ) {
            AsyncImage(
                model = item.thumbnailUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            CoverBadges(isVideo = item.isVideo, isPinned = false)
        }

        Spacer(modifier = Modifier.height(DiscoverDimens.CardTextGap))

        CardTexts(title = item.title, line = item.lineText ?: itemSubtitle(item), item = item)
    }
}

// Shape of the picture of a video, sixteen by nine
internal const val WideAspectRatio = 16f / 9f

// What is rising as columns of four songs that slide sideways, each with its place, its picture as it comes, its title and the line with the artist and the views
@Composable
private fun DiscoverRankedShelf(section: HomeSection, onItemClick: (HomeItem) -> Unit) {
    val songs = section.items.distinctBy { it.id }

    Column(modifier = Modifier.fillMaxWidth()) {
        DiscoverShelfTitle(title = section.title, link = section.link)

        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val state = rememberLazyGridState()

            // A fling stops with a whole column at the start, the way the speed dial stops on its tiles
            val snapLayout = remember(state) { columnSnapLayout(state) { _, _ -> 0f } }

            LazyHorizontalGrid(
                state = state,
                rows = GridCells.Fixed(DiscoverDimens.RankedRows),
                flingBehavior = rememberSnapFlingBehavior(snapLayout),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(DiscoverDimens.RankedRowHeight * DiscoverDimens.RankedRows)
            ) {
                gridItemsIndexed(songs, key = { _, song -> song.id }) { index, song ->
                    RankedRow(rank = index + 1, item = song, onClick = { onItemClick(song) }, modifier = Modifier.width(maxWidth))
                }
            }
        }
    }
}

// A song of the ranked list
@Composable
private fun RankedRow(rank: Int, item: HomeItem, onClick: () -> Unit, modifier: Modifier = Modifier) {
    // A picture of a video is wide and the cover of a song is square, both as they come
    val aspect = remember(item.thumbnailUrl) { if (item.thumbnailUrl?.isVideoThumbnail() == true) WideAspectRatio else 1f }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .height(DiscoverDimens.RankedRowHeight)
            .combinedClickable(onClick = onClick, onLongClick = { ItemMenu.show(item) })
    ) {
        Text(
            text = rank.toString(),
            style = MaterialTheme.typography.headlineMedium.merge(DiscoverType.Rank),
            color = MaterialTheme.colorScheme.secondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(DiscoverDimens.RankWidth)
        )

        Box(
            modifier = Modifier
                .width(DiscoverDimens.RankedPicture)
                .aspectRatio(aspect)
                .clip(RoundedCornerShape(DiscoverDimens.RankedCorner))
                .background(MaterialTheme.colorScheme.surfaceContainer)
        ) {
            AsyncImage(
                model = item.thumbnailUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            CoverBadges(isVideo = item.isVideo, isPinned = false)
        }

        Column(
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = WavvyTheme.dimens.spaceMedium)
        ) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.bodyMedium.merge(DiscoverType.CardTitle),
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.basicMarquee()
            )

            (item.lineText ?: itemSubtitle(item))?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium.merge(DiscoverType.CardLine),
                    color = MaterialTheme.colorScheme.secondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.basicMarquee()
                )
            }
        }

        IconButton(onClick = { ItemMenu.show(item) }) {
            Icon(
                imageVector = WavvyIcons.MoreVertical,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onBackground
            )
        }
    }
}

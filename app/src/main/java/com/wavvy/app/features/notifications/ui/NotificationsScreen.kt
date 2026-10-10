package com.wavvy.app.features.notifications.ui

// Compose layouts and foundations
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
// Material 3 components
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
// Images and lifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
// Project resources
import com.wavvy.app.R
import com.wavvy.app.core.designsystem.components.LocalSheetClose
import com.wavvy.app.core.designsystem.components.WavvySheet
import com.wavvy.app.core.designsystem.icons.WavvyIcons
import com.wavvy.app.core.designsystem.theme.DarkColors
import com.wavvy.app.core.designsystem.theme.WavvyTheme
import com.wavvy.app.core.navigation.ItemNavigator
import com.wavvy.app.features.home.data.HomeItem
import com.wavvy.app.features.home.data.HomeItemKind
import com.wavvy.app.features.home.data.HomeLink
import com.wavvy.app.features.home.ui.components.HomeMessage
import com.wavvy.app.features.home.ui.components.HomeType
import com.wavvy.app.features.home.ui.rememberItemPlayer
import com.wavvy.app.features.notifications.data.NotificationItem
import com.wavvy.app.features.player.ui.LocalMiniPlayerInset
import com.wavvy.app.features.player.ui.components.MenuAction

// The activity of the account, the new notifications first and the ones before under them, as the Activity page of YouTube Music
@Composable
fun NotificationsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: NotificationsViewModel = viewModel()
) {
    val items by viewModel.items.collectAsState()
    val onItemClick = rememberItemPlayer()
    val dimens = WavvyTheme.dimens
    var menuFor by remember { mutableStateOf<NotificationItem?>(null) }

    // Leaving the screen is what makes the news read, so the new ones stay marked while they are on the screen
    DisposableEffect(Unit) { onDispose { viewModel.markAllRead() } }

    // Solid, so the screen under it does not show through while the page slides
    Column(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
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
                text = stringResource(R.string.notifications_title),
                style = MaterialTheme.typography.titleLarge.merge(HomeType.SectionTitle),
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(start = dimens.spaceSmall)
            )
        }

        val list = items
        when {
            // Nothing is drawn until the database answers
            list == null -> Box(modifier = Modifier.weight(1f).fillMaxWidth())

            list.isEmpty() -> HomeMessage(
                text = stringResource(R.string.notifications_empty),
                modifier = Modifier.weight(1f)
            )

            else -> {
                val (fresh, before) = list.partition { it.isNew }

                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)),
                    contentPadding = PaddingValues(bottom = LocalMiniPlayerInset.current + dimens.spaceSmall)
                ) {
                    if (fresh.isNotEmpty()) {
                        item(key = "new") { GroupTitle(stringResource(R.string.notifications_new)) }
                        items(fresh, key = { it.id }) { NotificationRow(it, onClick = { open(it, onItemClick) }, onMore = { menuFor = it }) }
                    }
                    if (before.isNotEmpty()) {
                        item(key = "before") { GroupTitle(stringResource(R.string.notifications_before)) }
                        items(before, key = { it.id }) { NotificationRow(it, onClick = { open(it, onItemClick) }, onMore = { menuFor = it }) }
                    }
                }
            }
        }
    }

    menuFor?.let { item ->
        NotificationMenu(
            onDelete = { viewModel.delete(item) },
            onDismiss = { menuFor = null }
        )
    }
}

// Opens what a notification leads to, a page by its id or a video that plays
private fun open(item: NotificationItem, play: (HomeItem) -> Unit) {
    val link = item.link ?: return
    when {
        link.browseId != null -> ItemNavigator.openLink(HomeLink(browseId = link.browseId, params = null, isArtist = false), filter = "")
        link.videoId != null -> play(
            HomeItem(kind = HomeItemKind.Song, id = link.videoId, title = item.plainText, thumbnailUrl = item.coverUrl)
        )
    }
}

// Name of a group of notifications
@Composable
private fun GroupTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyLarge.merge(NotificationsType.Group),
        color = MaterialTheme.colorScheme.secondary,
        modifier = Modifier.padding(horizontal = WavvyTheme.dimens.screenPadding, vertical = WavvyTheme.dimens.spaceSmall)
    )
}

// A notification with the photo of the artist, the message with the names in bold and the time, the cover of the release and the three dots
@Composable
private fun NotificationRow(item: NotificationItem, onClick: () -> Unit, onMore: () -> Unit) {
    val dimens = WavvyTheme.dimens

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(start = dimens.screenPadding, top = NotificationsDimens.RowVertical, bottom = NotificationsDimens.RowVertical)
    ) {
        AsyncImage(
            model = item.avatarUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(NotificationsDimens.Avatar)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainer)
        )

        Column(modifier = Modifier.weight(1f).padding(horizontal = NotificationsDimens.Gap)) {
            Text(
                text = buildAnnotatedString {
                    item.message.forEach { part ->
                        withStyle(SpanStyle(fontWeight = if (part.isBold) FontWeight.Bold else FontWeight.Normal)) { append(part.text) }
                    }
                },
                style = MaterialTheme.typography.bodyLarge.merge(NotificationsType.Message),
                color = MaterialTheme.colorScheme.onBackground
            )
            item.timeText?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium.merge(NotificationsType.Time),
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }

        AsyncImage(
            model = item.coverUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(NotificationsDimens.Cover)
                .clip(RoundedCornerShape(NotificationsDimens.CoverCorner))
                .background(MaterialTheme.colorScheme.surfaceContainer)
        )

        IconButton(onClick = onMore, modifier = Modifier.size(NotificationsDimens.More)) {
            Icon(
                imageVector = WavvyIcons.MoreVertical,
                contentDescription = stringResource(R.string.cd_more),
                tint = MaterialTheme.colorScheme.onBackground
            )
        }
        Spacer(modifier = Modifier.width(dimens.spaceSmall))
    }
}

// What a notification can do, delete it
@Composable
private fun NotificationMenu(
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    DarkColors {
        WavvySheet(onDismiss = onDismiss) {
            val closeSheet = LocalSheetClose.current

            Column(modifier = Modifier.padding(bottom = WavvyTheme.dimens.spaceMedium)) {
                MenuAction(WavvyIcons.Delete, stringResource(R.string.notifications_delete)) {
                    onDelete()
                    closeSheet()
                }
            }
        }
    }
}

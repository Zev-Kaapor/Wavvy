package com.wavvy.app.features.menu

// Android context and sharing
import android.content.Context
import android.content.Intent
// Compose layouts and foundations
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
// Material 3 components
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
// Compose state and runtime
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
// UI styling and utilities
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
// Coroutines
import kotlinx.coroutines.launch
// Project resources
import com.wavvy.app.R
import com.wavvy.app.core.designsystem.components.SkeletonHost
import com.wavvy.app.core.designsystem.components.LocalSheetClose
import com.wavvy.app.core.designsystem.components.WavvySheet
import com.wavvy.app.core.designsystem.components.skeleton
import com.wavvy.app.core.designsystem.icons.WavvyIcons
import com.wavvy.app.core.designsystem.theme.WavvyTheme
import com.wavvy.app.core.innertube.MusicOrigin
import com.wavvy.app.features.artist.data.ArtistPage
import com.wavvy.app.features.artist.data.ArtistRepository
import com.wavvy.app.features.home.data.HomeItem
import com.wavvy.app.features.home.ui.rememberItemPlayer
import com.wavvy.app.features.home.ui.rememberListPlayer

// Menu of an artist of a chart, the name and how many subscribe on top, shuffle, mix and share as buttons and the subscription under them
// The page of the artist is asked when the menu opens, shuffle, mix and the subscription wait for it
@Composable
internal fun ArtistMenu(item: HomeItem) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val playList = rememberListPlayer()
    val onItemClick = rememberItemPlayer()

    var page by remember(item.id) { mutableStateOf<ArtistPage?>(null) }
    var isSubscribed by remember(item.id) { mutableStateOf(false) }
    LaunchedEffect(item.id) {
        page = ArtistRepository(context).load(item.id).getOrNull()
        isSubscribed = page?.subscription?.isSubscribed == true
    }

    val songs = page?.topSongs.orEmpty()
    val subscription = page?.subscription

    WavvySheet(onDismiss = ItemMenu::dismiss) {
        val closeSheet = LocalSheetClose.current

        Column(modifier = Modifier.padding(bottom = ItemMenuDimens.Bottom)) {
            // The artist the options are for
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = ItemMenuDimens.Side, end = ItemMenuDimens.CloseEnd, top = ItemMenuDimens.HeaderTop, bottom = ItemMenuDimens.HeaderBottom)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1
                    )
                    item.countText?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                }
                IconButton(onClick = closeSheet) {
                    Icon(
                        imageVector = WavvyIcons.Close,
                        contentDescription = stringResource(R.string.cd_close),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // The three buttons side by side
            Row(
                horizontalArrangement = Arrangement.spacedBy(ItemMenuDimens.TileGap),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = ItemMenuDimens.Side, vertical = ItemMenuDimens.TilesVertical)
            ) {
                MenuTile(WavvyIcons.Shuffle, stringResource(R.string.collection_shuffle), enabled = songs.isNotEmpty(), modifier = Modifier.weight(1f)) {
                    playList(songs, songs.indices.random(), true)
                    closeSheet()
                }
                MenuTile(WavvyIcons.Mix, stringResource(R.string.artist_mix), enabled = songs.isNotEmpty(), modifier = Modifier.weight(1f)) {
                    songs.firstOrNull()?.let(onItemClick)
                    closeSheet()
                }
                MenuTile(WavvyIcons.Share, stringResource(R.string.player_share), modifier = Modifier.weight(1f)) {
                    shareLink(context, "$MusicOrigin/channel/${item.id}")
                    closeSheet()
                }
            }

            // The line of the subscription keeps its room while the page of the artist loads, so the menu does not grow when it arrives
            if (page == null) {
                SkeletonHost {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(ItemMenuDimens.RowHeight)
                            .padding(horizontal = ItemMenuDimens.Side)
                    ) {
                        Box(modifier = Modifier.size(ItemMenuDimens.RowIcon).skeleton(RoundedCornerShape(ItemMenuDimens.TileCorner)))
                        Box(
                            modifier = Modifier
                                .padding(start = ItemMenuDimens.RowTextStart)
                                .width(ItemMenuDimens.PlaceholderWidth)
                                .height(ItemMenuDimens.PlaceholderHeight)
                                .skeleton(RoundedCornerShape(ItemMenuDimens.PlaceholderCorner))
                        )
                    }
                }
            }

            // The words of the button are the ones YouTube Music sends, in the language of the request
            if (subscription != null) {
                val label = (if (isSubscribed) subscription.subscribedLabel else subscription.unsubscribedLabel).orEmpty()
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(ItemMenuDimens.RowHeight)
                        .clickable {
                            scope.launch {
                                val wanted = !isSubscribed
                                ArtistRepository(context).setSubscribed(subscription, wanted).onSuccess { isSubscribed = wanted }
                            }
                        }
                        .padding(horizontal = ItemMenuDimens.Side)
                ) {
                    Icon(
                        imageVector = WavvyIcons.Subscriptions,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(ItemMenuDimens.RowIcon)
                    )
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(start = ItemMenuDimens.RowTextStart)
                    )
                }
            }
        }
    }
}

// A rounded button with an icon and its name under it
@Composable
private fun MenuTile(
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.alpha(if (enabled) 1f else ItemMenuDimens.DisabledAlpha)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxWidth()
                .height(ItemMenuDimens.TileHeight)
                .clip(RoundedCornerShape(ItemMenuDimens.TileCorner))
                .background(WavvyTheme.colors.chip)
                .clickable(enabled = enabled, onClick = onClick)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(ItemMenuDimens.TileIcon)
            )
        }

        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            maxLines = 1,
            modifier = Modifier
                .padding(top = ItemMenuDimens.TileLabelTop)
                .width(ItemMenuDimens.TileLabelWidth)
        )
    }
}

// Opens the sharing sheet of the system with the link
private fun shareLink(context: Context, url: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, url)
    }
    context.startActivity(Intent.createChooser(intent, context.getString(R.string.player_share_via)))
}

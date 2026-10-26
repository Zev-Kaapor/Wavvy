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
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.runtime.collectAsState
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
// Project resources
import com.wavvy.app.R
import com.wavvy.app.core.designsystem.components.SkeletonHost
import com.wavvy.app.core.designsystem.components.LocalSheetClose
import com.wavvy.app.core.designsystem.components.WavvySheet
import com.wavvy.app.core.designsystem.components.skeleton
import com.wavvy.app.core.designsystem.icons.WavvyIcons
import com.wavvy.app.core.designsystem.theme.WavvyTheme
import com.wavvy.app.core.history.PlayHistory
import com.wavvy.app.core.innertube.MusicOrigin
import com.wavvy.app.features.artist.data.ArtistPage
import com.wavvy.app.features.artist.data.ArtistRepository
import com.wavvy.app.features.home.data.HomeItem
import com.wavvy.app.features.home.data.HomeItemKind
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
    val pinnedIds by remember { PlayHistory.pinned(context).map { pinned -> pinned.map { it.id }.toSet() } }.collectAsState(initial = emptySet())
    val isPinned = item.id in pinnedIds

    WavvySheet(onDismiss = ItemMenu::dismiss) {
        val closeSheet = LocalSheetClose.current

        CardMenuLayout(
            head = {
                // The artist the options are for, round as the artists are
                CardMenuHead(title = item.title, subtitle = item.countText, coverUrl = item.thumbnailUrl, coverShape = CircleShape, isProfile = item.isProfile)
                CardMenuTiles {
                    MenuCardTile(WavvyIcons.Shuffle, stringResource(R.string.collection_shuffle), Modifier.weight(1f), enabled = songs.isNotEmpty()) {
                        playList(songs, songs.indices.random(), true)
                        closeSheet()
                    }
                    MenuCardTile(WavvyIcons.Mix, stringResource(R.string.artist_mix), Modifier.weight(1f), enabled = songs.isNotEmpty()) {
                        songs.firstOrNull()?.let(onItemClick)
                        closeSheet()
                    }
                    MenuCardTile(WavvyIcons.Share, stringResource(R.string.player_share), Modifier.weight(1f)) {
                        shareLink(context, "$MusicOrigin/channel/${item.id}")
                        closeSheet()
                    }
                }
            },
            options = {
                MenuCard(WavvyIcons.Pin, stringResource(if (isPinned) R.string.menu_unpin else R.string.menu_pin)) {
                    scope.launch(Dispatchers.IO) {
                        if (isPinned) {
                            PlayHistory.unpin(context, item.id)
                        } else {
                            PlayHistory.pin(context, item.id, item.title, null, item.thumbnailUrl, durationMs = 0L, kind = HomeItemKind.Artist.name)
                        }
                    }
                    closeSheet()
                }

                // The subscription needs the page of the artist, so its card comes in when the page does
                // The words of the button are the ones YouTube Music sends, in the language of the request
                if (subscription != null) {
                    MenuReveal {
                        val label = (if (isSubscribed) subscription.subscribedLabel else subscription.unsubscribedLabel).orEmpty()
                        MenuCard(WavvyIcons.Subscriptions, label) {
                            scope.launch {
                                val wanted = !isSubscribed
                                ArtistRepository(context).setSubscribed(subscription, wanted).onSuccess { isSubscribed = wanted }
                            }
                        }
                    }
                }
            }
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

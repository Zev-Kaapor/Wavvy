package com.wavvy.app.features.collection.ui

// Compose layouts and foundations
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
// Material 3 components
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
// Coroutines and reactive flows
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
// Image loading
import coil3.compose.AsyncImage
// Project resources
import com.wavvy.app.R
import com.wavvy.app.core.designsystem.components.WavvySheet
import com.wavvy.app.core.designsystem.icons.WavvyIcons
import com.wavvy.app.features.player.ui.components.MenuAction
import com.wavvy.app.features.player.ui.components.PlayerDimens

// What the menu of a page shows and does, the actions are made by the page that opens it
data class CollectionMenuData(
    val title: String,
    val subtitle: String?,
    val coverUrl: String?,
    val onShuffle: () -> Unit,
    val onPlayNext: () -> Unit,
    val onAddToQueue: () -> Unit,
    val onShare: () -> Unit
)

// The menu that is open, held outside the screens so the sheet can cover the whole window
object CollectionMenu {
    private val mutableData = MutableStateFlow<CollectionMenuData?>(null)
    val data: StateFlow<CollectionMenuData?> = mutableData.asStateFlow()

    fun show(data: CollectionMenuData) {
        mutableData.value = data
    }

    fun dismiss() {
        mutableData.value = null
    }
}

// Menu of the page of an album or of a playlist, drawn over the whole screen
@Composable
fun CollectionMenuHost() {
    val data by CollectionMenu.data.collectAsState()
    val current = data ?: return

    // Every action closes the menu after it runs
    val run: (() -> Unit) -> () -> Unit = { action -> { action(); CollectionMenu.dismiss() } }

    WavvySheet(onDismiss = CollectionMenu::dismiss) {
        Column(modifier = Modifier.padding(bottom = PlayerDimens.OptionsBottom)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = PlayerDimens.OptionsSide, vertical = PlayerDimens.OptionsRowGap)
            ) {
                AsyncImage(
                    model = current.coverUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(PlayerDimens.QueueMenuCover)
                        .clip(RoundedCornerShape(CollectionDimens.MenuCoverCorner))
                        .background(MaterialTheme.colorScheme.surfaceContainer)
                )
                Column(modifier = Modifier.weight(1f).padding(start = PlayerDimens.QueueTextSide)) {
                    Text(
                        text = current.title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        maxLines = 1,
                        modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE)
                    )
                    current.subtitle?.let { subtitle ->
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE)
                        )
                    }
                }
            }

            MenuAction(WavvyIcons.Shuffle, stringResource(R.string.collection_shuffle), run(current.onShuffle))
            MenuAction(WavvyIcons.PlaylistPlay, stringResource(R.string.queue_play_next), run(current.onPlayNext))
            MenuAction(WavvyIcons.QueueMusic, stringResource(R.string.menu_add_to_queue), run(current.onAddToQueue))
            MenuAction(WavvyIcons.Share, stringResource(R.string.player_share), run(current.onShare))
        }
    }
}

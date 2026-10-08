package com.wavvy.app.features.player.ui.components

// Compose layouts and foundations
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
// Material 3 components
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
// Compose state and runtime
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
// UI styling and utilities
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
// Images
import coil3.compose.AsyncImage
// Project resources
import com.wavvy.app.R
import com.wavvy.app.core.designsystem.components.WavvySheet
import com.wavvy.app.core.designsystem.icons.WavvyIcons
import com.wavvy.app.core.designsystem.theme.DarkColors
import com.wavvy.app.features.artist.data.ArtistRepository
import com.wavvy.app.features.artist.data.ArtistSummary

// The artists of a song with more than one, each with its photo and its subscribers, a tap on one opens its page
@Composable
fun ArtistPickerSheet(
    artists: List<Pair<String, String>>,
    onArtistClick: (id: String) -> Unit,
    onDismiss: () -> Unit
) {
    DarkColors {
        WavvySheet(onDismiss = onDismiss) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = PlayerDimens.OptionsBottom)
            ) {
                Text(
                    text = stringResource(R.string.player_artists),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    modifier = Modifier.padding(horizontal = PlayerDimens.OptionsSide, vertical = PlayerDimens.OptionsRowGap)
                )

                artists.forEach { (name, id) ->
                    ArtistRow(name = name, id = id, onClick = { onArtistClick(id) })
                }
            }
        }
    }
}

// An artist with a round photo and, under the name, how many subscribers it has, both appear as they arrive
@Composable
private fun ArtistRow(name: String, id: String, onClick: () -> Unit) {
    val context = LocalContext.current
    var summary by remember(id) { mutableStateOf<ArtistSummary?>(null) }
    LaunchedEffect(id) { if (id.isNotEmpty()) summary = ArtistRepository(context).summary(id).getOrNull() }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .height(PlayerDimens.OptionsRowHeight)
            .clickable(enabled = id.isNotEmpty(), onClick = onClick)
            .padding(horizontal = PlayerDimens.OptionsSide)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(PlayerDimens.ArtistPhoto)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
        ) {
            Icon(
                imageVector = WavvyIcons.Person,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(PlayerDimens.ToolbarIcon)
            )
            summary?.photoUrl?.let { photo ->
                AsyncImage(
                    model = photo,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        Column(modifier = Modifier.weight(1f).padding(start = PlayerDimens.QueueTextSide)) {
            Text(
                text = name,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                modifier = Modifier.basicMarquee()
            )
            summary?.subscribers?.let { subscribers ->
                Text(
                    text = subscribers,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
        }
    }
}

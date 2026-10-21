package com.wavvy.app.features.menu

// Android context
import android.content.Context
// Compose layouts and foundations
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
// Material 3 components
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
import androidx.compose.runtime.setValue
// UI styling and utilities
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
// JSON
import org.json.JSONObject
// Project resources
import com.wavvy.app.R
import com.wavvy.app.core.designsystem.components.LocalSheetClose
import com.wavvy.app.core.designsystem.components.SkeletonHost
import com.wavvy.app.core.designsystem.components.Toasts
import com.wavvy.app.core.designsystem.components.WavvySheet
import com.wavvy.app.core.designsystem.components.scrollIndicator
import com.wavvy.app.core.designsystem.components.skeleton
import com.wavvy.app.core.designsystem.icons.WavvyIcons
import com.wavvy.app.core.designsystem.theme.WavvyTheme
import com.wavvy.app.core.innertube.InnerTubeClient
import com.wavvy.app.core.innertube.arrayAt
import com.wavvy.app.core.innertube.currentSession
import com.wavvy.app.core.innertube.objectAt
import com.wavvy.app.core.innertube.objects
import com.wavvy.app.core.innertube.stringAt
import com.wavvy.app.core.playback.PlayableTrack

// Prefix of the page of the credits of a song, followed by the id of its video
private const val CreditsPrefix = "MPTC"

// What separates the names of a group of the credits
private const val NameSeparator = "\n"

// A group of the credits, such as who wrote the song, with the names in it
data class CreditGroup(val title: String, val names: List<String>)

// The credits of a song as YouTube Music writes them, in the language of the request
data class SongCredits(val title: String, val groups: List<CreditGroup>)

// Asks the page of the credits, which comes as a box to open with its groups
suspend fun loadSongCredits(context: Context, videoId: String): Result<SongCredits> =
    InnerTubeClient.browse(currentSession(context.applicationContext), browseId = CreditsPrefix + videoId)
        .mapCatching { parseSongCredits(it) ?: throw IllegalStateException("The answer has no box of credits") }

// A song YouTube Music knows nothing about comes with the box and no groups, which is not a failure
private fun parseSongCredits(response: JSONObject): SongCredits? {
    val dialog = response.arrayAt("onResponseReceivedActions").objects()
        .firstNotNullOfOrNull { it.objectAt("openPopupAction", "popup", "dismissableDialogRenderer") } ?: return null

    val groups = dialog.arrayAt("sections").objects().mapNotNull { section ->
        val content = section.objectAt("dismissableDialogContentSectionRenderer") ?: return@mapNotNull null
        val names = content.arrayAt("subtitle", "runs").objects().joinToString("") { it.optString("text") }
            .split(NameSeparator).map { it.trim() }.filter { it.isNotEmpty() }
        CreditGroup(title = content.stringAt("title", "runs", 0, "text") ?: return@mapNotNull null, names = names).takeIf { names.isNotEmpty() }
    }

    return SongCredits(title = dialog.optString("title"), groups = groups)
}

// The credits of a song in a sheet, each group a card as in the menu, placeholders keep the room while they load
@Composable
fun SongCreditsSheet(track: PlayableTrack, onDismiss: () -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current
    val resources = LocalResources.current
    var credits by remember(track.id) { mutableStateOf<SongCredits?>(null) }

    WavvySheet(onDismiss = onDismiss) {
        val closeSheet = LocalSheetClose.current

        // A request that fails closes the sheet and says so
        LaunchedEffect(track.id) {
            loadSongCredits(context, track.id)
                .onSuccess { credits = it }
                .onFailure {
                    Toasts.show(resources.getString(R.string.menu_credits_error), isError = true)
                    closeSheet()
                }
        }

        // The bar on the end tells there are more groups below
        val scroll = rememberScrollState()
        Column(
            verticalArrangement = Arrangement.spacedBy(ItemMenuDimens.CardGap),
            modifier = Modifier
                .scrollIndicator(scroll)
                .verticalScroll(scroll)
                .padding(start = ItemMenuDimens.CardsSide, end = ItemMenuDimens.CardsSide, bottom = ItemMenuDimens.CreditsBottom)
        ) {
            // The arrow goes back to the menu of the song, before the title of the box and the song it is about
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = ItemMenuDimens.HeaderVertical)) {
                IconButton(onClick = {
                    onBack()
                    closeSheet()
                }) {
                    Icon(imageVector = WavvyIcons.Back, contentDescription = stringResource(R.string.cd_back), tint = MaterialTheme.colorScheme.onSurface)
                }
                Column(modifier = Modifier.padding(start = ItemMenuDimens.CreditsTitleStart)) {
                    Text(
                        text = credits?.title?.ifEmpty { null } ?: stringResource(R.string.menu_credits),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1
                    )
                    Text(
                        text = track.title,
                        style = MaterialTheme.typography.bodyMedium,
                        color = WavvyTheme.colors.playerAccent,
                        maxLines = 1,
                        modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE)
                    )
                }
            }

            val current = credits
            if (current == null) {
                SkeletonHost {
                    Column(verticalArrangement = Arrangement.spacedBy(ItemMenuDimens.CardGap)) {
                        repeat(ItemMenuDimens.CreditsSkeletonCards) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(ItemMenuDimens.CreditsSkeletonHeight)
                                    .skeleton(RoundedCornerShape(ItemMenuDimens.CardCorner))
                            )
                        }
                    }
                }
            } else if (current.groups.isEmpty()) {
                // YouTube Music has no credits for some songs, such as many remixes
                Text(
                    text = stringResource(R.string.menu_credits_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(ItemMenuDimens.CardCorner))
                        .background(WavvyTheme.colors.menuCard)
                        .padding(horizontal = ItemMenuDimens.CardRowHorizontal, vertical = ItemMenuDimens.CardRowVertical)
                )
            } else {
                current.groups.forEach { group -> CreditCard(group) }
            }
        }
    }
}

// A group of the credits as a card, its title over the names, one on each line
@Composable
private fun CreditCard(group: CreditGroup) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(ItemMenuDimens.CardCorner))
            .background(WavvyTheme.colors.menuCard)
            .padding(horizontal = ItemMenuDimens.CardRowHorizontal, vertical = ItemMenuDimens.CardRowVertical)
    ) {
        Text(
            text = group.title,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        group.names.forEach { name ->
            Text(
                text = name,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = ItemMenuDimens.CreditsNameGap)
            )
        }
    }
}

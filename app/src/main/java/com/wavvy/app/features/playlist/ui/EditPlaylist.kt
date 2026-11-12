package com.wavvy.app.features.playlist.ui

// Compose layouts and foundations
import com.wavvy.app.features.menu.CardMenuButton
import com.wavvy.app.features.menu.CardMenuField
import com.wavvy.app.features.menu.CardMenuSection
import android.net.Uri
// Material 3 components
// Compose state and runtime
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
// UI styling and utilities
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
// Coroutines and reactive flows
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
// Project resources
import com.wavvy.app.R
import com.wavvy.app.core.designsystem.components.LocalSheetClose
import com.wavvy.app.core.designsystem.components.WavvySheet
import com.wavvy.app.features.collection.data.CollectionKind
import com.wavvy.app.features.collection.data.CollectionRepository
import com.wavvy.app.features.home.data.PlaylistPagePrefix
import com.wavvy.app.features.menu.CardMenuForm
import com.wavvy.app.features.playlist.data.PlaylistCover
import com.wavvy.app.features.playlist.data.PlaylistPrivacy
import com.wavvy.app.features.playlist.data.PlaylistRepository

// What the sheet starts with, the description and who can find the playlist are empty when the screen that opens it does not know them
data class EditPlaylistData(
    val id: String,
    val title: String,
    val description: String?,
    val privacy: PlaylistPrivacy?,
    // The cover it has now, shown until another one is chosen
    val coverUrl: String? = null
)

// The playlist being changed, held outside the screens so the sheet can cover the whole window
object EditPlaylist {
    private val mutableData = MutableStateFlow<EditPlaylistData?>(null)
    val data: StateFlow<EditPlaylistData?> = mutableData.asStateFlow()

    fun show(data: EditPlaylistData) {
        mutableData.value = data
    }

    fun dismiss() {
        mutableData.value = null
    }
}

// The sheet that changes the name, the description and who can find a playlist of the account, only what was changed is sent
@Composable
fun EditPlaylistHost() {
    val data by EditPlaylist.data.collectAsState()
    val current = data ?: return

    val context = LocalContext.current
    val resources = LocalResources.current
    val scope = rememberCoroutineScope()
    val repository = remember { PlaylistRepository(context) }
    var title by remember(current) { mutableStateOf(current.title) }
    var description by remember(current) { mutableStateOf(current.description.orEmpty()) }
    var privacy by remember(current) { mutableStateOf(current.privacy) }
    var isBusy by remember(current) { mutableStateOf(false) }
    var isDone by remember(current) { mutableStateOf(false) }
    var coverUri by remember(current) { mutableStateOf<Uri?>(null) }
    // What the playlist has in the account, the one that opens the sheet may not know the description and the privacy, and then they are asked
    var saved by remember(current) { mutableStateOf(current) }
    val canSave = title.isNotBlank() && !isBusy

    LaunchedEffect(current) {
        if (current.description != null && current.privacy != null) return@LaunchedEffect

        CollectionRepository(context).load(CollectionKind.Playlist, current.id.removePrefix(PlaylistPagePrefix)).getOrNull()?.let { page ->
            val loaded = PlaylistPrivacy.entries.firstOrNull { it.status == page.privacy }

            // What the user already typed or chose while it came is kept
            if (description == current.description.orEmpty()) description = page.description.orEmpty()
            if (privacy == current.privacy) privacy = loaded
            saved = saved.copy(description = page.description.orEmpty(), privacy = loaded)
        }
    }

    val onSave = {
        if (canSave) {
            isBusy = true
            scope.launch {
                repository.update(
                    playlistId = current.id,
                    title = title.trim().takeIf { it != saved.title },
                    description = description.trim().takeIf { it != saved.description.orEmpty() },
                    privacy = privacy.takeIf { it != saved.privacy }
                )
                    .onSuccess {
                        // The cover goes after the rest, a failure of it is told in the same way
                        coverUri?.let { picked ->
                            val sent = PlaylistCover.encode(context, picked)?.let { repository.setCover(current.id, it) }
                            if (sent == null || sent.isFailure) toast(context, resources.getString(R.string.playlist_action_error), isError = true)
                        }
                        toast(context, resources.getString(R.string.playlist_edited))
                        isDone = true
                    }
                    .onFailure {
                        toast(context, resources.getString(R.string.playlist_action_error), isError = true)
                        isBusy = false
                    }
            }
        }
    }

    WavvySheet(onDismiss = EditPlaylist::dismiss) {
        // Once the playlist is saved the sheet slides out
        val closeSheet = LocalSheetClose.current
        LaunchedEffect(isDone) { if (isDone) closeSheet() }

        CardMenuForm(
            title = stringResource(R.string.playlist_edit),
            action = { CardMenuButton(text = stringResource(R.string.playlist_save_changes), isEnabled = canSave, onClick = onSave) }
        ) {
            CoverPicker(pickedUri = coverUri, currentUrl = current.coverUrl, onPicked = { coverUri = it })

            CardMenuField(value = title, onValueChange = { title = it }, label = stringResource(R.string.playlist_title_hint), singleLine = true)
            CardMenuField(
                value = description,
                onValueChange = { description = it },
                label = stringResource(R.string.playlist_description_hint),
                maxLines = PlaylistDimens.DescriptionLines
            )

            CardMenuSection(stringResource(R.string.playlist_privacy))
            PlaylistPrivacy.entries.forEach { option ->
                PrivacyRow(
                    icon = iconOf(option),
                    title = stringResource(titleOf(option)),
                    isSelected = option == privacy,
                    onClick = { privacy = option }
                )
            }
        }
    }
}

package com.wavvy.app.features.playlist.ui

// Compose layouts and foundations
import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
// Material 3 components
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
// Compose state and runtime
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
// UI styling and utilities
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
// Coroutines and reactive flows
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
// Project resources
import com.wavvy.app.R
import com.wavvy.app.core.designsystem.components.WavvySheet
import com.wavvy.app.features.menu.ItemMenuDimens
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
    var coverUri by remember(current) { mutableStateOf<Uri?>(null) }
    val canSave = title.isNotBlank() && !isBusy

    val onSave = {
        if (canSave) {
            isBusy = true
            scope.launch {
                repository.update(
                    playlistId = current.id,
                    title = title.trim().takeIf { it != current.title },
                    description = description.trim().takeIf { it != current.description.orEmpty() },
                    privacy = privacy.takeIf { it != current.privacy }
                )
                    .onSuccess {
                        // The cover goes after the rest, a failure of it is told in the same way
                        coverUri?.let { picked ->
                            val sent = PlaylistCover.encode(context, picked)?.let { repository.setCover(current.id, it) }
                            if (sent == null || sent.isFailure) toast(context, resources.getString(R.string.playlist_action_error))
                        }
                        toast(context, resources.getString(R.string.playlist_edited))
                        EditPlaylist.dismiss()
                    }
                    .onFailure {
                        toast(context, resources.getString(R.string.playlist_action_error))
                        isBusy = false
                    }
            }
        }
    }

    val fieldColors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent)

    WavvySheet(onDismiss = EditPlaylist::dismiss) {
        Column(modifier = Modifier.imePadding().verticalScroll(rememberScrollState()).padding(bottom = ItemMenuDimens.Bottom)) {
            SheetHeader(title = stringResource(R.string.playlist_edit), onClose = EditPlaylist::dismiss)

            CoverPicker(pickedUri = coverUri, currentUrl = current.coverUrl, onPicked = { coverUri = it })

            TextField(
                value = title,
                onValueChange = { title = it },
                label = { Text(text = stringResource(R.string.playlist_title_hint)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                colors = fieldColors,
                modifier = Modifier.fillMaxWidth().padding(horizontal = ItemMenuDimens.Side, vertical = PlaylistDimens.FieldVertical)
            )
            TextField(
                value = description,
                onValueChange = { description = it },
                label = { Text(text = stringResource(R.string.playlist_description_hint)) },
                maxLines = PlaylistDimens.DescriptionLines,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                colors = fieldColors,
                modifier = Modifier.fillMaxWidth().padding(horizontal = ItemMenuDimens.Side, vertical = PlaylistDimens.FieldVertical)
            )

            Text(
                text = stringResource(R.string.playlist_privacy),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = ItemMenuDimens.Side, vertical = PlaylistDimens.FieldVertical)
            )
            PlaylistPrivacy.entries.forEach { option ->
                PrivacyRow(
                    icon = iconOf(option),
                    title = stringResource(titleOf(option)),
                    isSelected = option == privacy,
                    onClick = { privacy = option }
                )
            }

            SheetButton(text = stringResource(R.string.playlist_save_changes), isEnabled = canSave, onClick = onSave)
        }
    }
}

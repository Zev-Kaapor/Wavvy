package com.wavvy.app.features.playlist.ui

// Compose layouts and foundations
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
// Material 3 components
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
// Coroutines and reactive flows
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
// Project resources
import com.wavvy.app.R
import com.wavvy.app.core.designsystem.components.WavvySheet
import com.wavvy.app.core.designsystem.icons.WavvyIcons
import com.wavvy.app.features.menu.ItemMenuDimens
import com.wavvy.app.features.playlist.data.PlaylistCover
import com.wavvy.app.features.playlist.data.PlaylistPrivacy
import com.wavvy.app.features.playlist.data.PlaylistRepository

// The songs a new playlist starts with, none when it starts empty, held outside the screens so the sheet can cover the whole window
object NewPlaylist {
    private val mutableVideoIds = MutableStateFlow<List<String>?>(null)
    val videoIds: StateFlow<List<String>?> = mutableVideoIds.asStateFlow()

    fun show(videoIds: List<String> = emptyList()) {
        mutableVideoIds.value = videoIds
    }

    fun dismiss() {
        mutableVideoIds.value = null
    }
}

// The sheet that makes a playlist, its name, who can find it and the button that makes it with the songs it was opened with
@Composable
fun NewPlaylistHost() {
    val videoIds by NewPlaylist.videoIds.collectAsState()
    val current = videoIds ?: return

    val context = LocalContext.current
    val resources = LocalResources.current
    val scope = rememberCoroutineScope()
    val repository = remember { PlaylistRepository(context) }
    val focus = remember { FocusRequester() }
    var isSignedIn by remember(current) { mutableStateOf(true) }
    var title by remember(current) { mutableStateOf("") }
    var privacy by remember(current) { mutableStateOf(PlaylistPrivacy.Private) }
    var isBusy by remember(current) { mutableStateOf(false) }
    var coverUri by remember(current) { mutableStateOf<Uri?>(null) }
    val canCreate = title.isNotBlank() && !isBusy

    LaunchedEffect(current) {
        isSignedIn = repository.isSignedIn()
        if (isSignedIn) focus.requestFocus()
    }

    val onCreate = {
        if (canCreate) {
            isBusy = true
            val name = title.trim()
            scope.launch {
                repository.create(name, privacy, current)
                    .onSuccess { playlistId ->
                        // The cover goes after the playlist exists, a failure of it does not undo the playlist
                        val cover = coverUri
                        if (cover != null && playlistId != null) {
                            PlaylistCover.encode(context, cover)?.let { repository.setCover(playlistId, it) }
                        }
                        toast(context, resources.getString(R.string.playlist_created, name))
                        NewPlaylist.dismiss()
                    }
                    .onFailure {
                        toast(context, resources.getString(R.string.playlist_action_error))
                        isBusy = false
                    }
            }
        }
    }

    WavvySheet(onDismiss = NewPlaylist::dismiss) {
        Column(modifier = Modifier.imePadding().verticalScroll(rememberScrollState()).padding(bottom = ItemMenuDimens.Bottom)) {
            SheetHeader(title = stringResource(R.string.library_new_playlist), onClose = NewPlaylist::dismiss)

            if (!isSignedIn) {
                SheetMessage(stringResource(R.string.playlist_sign_in))
                return@Column
            }

            TextField(
                value = title,
                onValueChange = { title = it },
                placeholder = { Text(text = stringResource(R.string.playlist_title_hint)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = ItemMenuDimens.Side, vertical = PlaylistDimens.FieldVertical)
                    .focusRequester(focus)
            )

            CoverPicker(pickedUri = coverUri, currentUrl = null, onPicked = { coverUri = it })

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

            SheetButton(text = stringResource(R.string.playlist_create), isEnabled = canCreate, onClick = onCreate)
        }
    }
}

// The round button at the end of a sheet that makes or keeps the playlist, faded while it cannot
@Composable
internal fun SheetButton(text: String, isEnabled: Boolean, onClick: () -> Unit) {
    Box(modifier = Modifier.fillMaxWidth().padding(horizontal = ItemMenuDimens.Side), contentAlignment = Alignment.CenterEnd) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .padding(top = PlaylistDimens.CreateTop)
                .alpha(if (isEnabled) 1f else ItemMenuDimens.DisabledAlpha)
                .height(PlaylistDimens.CreateHeight)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.onSurface)
                .clickable(enabled = isEnabled, onClick = onClick)
                .padding(horizontal = PlaylistDimens.CreatePaddingX)
        ) {
            Text(text = text, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.surface)
        }
    }
}

// One choice of who can find the playlist, with the check when it is the one on
@Composable
internal fun PrivacyRow(icon: ImageVector, title: String, isSelected: Boolean, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .height(PlaylistDimens.PrivacyRowHeight)
            .clickable(onClick = onClick)
            .padding(horizontal = ItemMenuDimens.Side)
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface)
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f).padding(start = ItemMenuDimens.RowTextStart)
        )
        Box(modifier = Modifier.width(ItemMenuDimens.RowIcon)) {
            if (isSelected) Icon(imageVector = WavvyIcons.Check, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface)
        }
    }
}

internal fun iconOf(privacy: PlaylistPrivacy): ImageVector = when (privacy) {
    PlaylistPrivacy.Public -> WavvyIcons.Public
    PlaylistPrivacy.Unlisted -> WavvyIcons.Link
    PlaylistPrivacy.Private -> WavvyIcons.Lock
}

internal fun titleOf(privacy: PlaylistPrivacy): Int = when (privacy) {
    PlaylistPrivacy.Public -> R.string.playlist_privacy_public
    PlaylistPrivacy.Unlisted -> R.string.playlist_privacy_unlisted
    PlaylistPrivacy.Private -> R.string.playlist_privacy_private
}

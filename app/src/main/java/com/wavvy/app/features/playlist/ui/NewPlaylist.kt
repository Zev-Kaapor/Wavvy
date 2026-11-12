package com.wavvy.app.features.playlist.ui

// Compose layouts and foundations
import com.wavvy.app.features.menu.CardMenuButton
import com.wavvy.app.features.menu.CardMenuField
import com.wavvy.app.features.menu.CardMenuSection
import android.net.Uri
// Material 3 components
import androidx.compose.material3.Icon
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
// Coroutines and reactive flows
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
// Project resources
import com.wavvy.app.R
import com.wavvy.app.core.designsystem.components.Toasts
import com.wavvy.app.core.designsystem.components.LocalSheetClose
import com.wavvy.app.core.designsystem.components.WavvySheet
import com.wavvy.app.core.designsystem.icons.WavvyIcons
import com.wavvy.app.core.designsystem.theme.WavvyTheme
import com.wavvy.app.features.menu.CardMenuForm
import com.wavvy.app.features.menu.MenuCard
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
    var isDone by remember(current) { mutableStateOf(false) }
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
                        // The cover goes after the playlist exists, a failure of it does not undo the playlist but it is told with its reason
                        val cover = coverUri
                        if (cover != null) {
                            val sent = playlistId?.let { id -> PlaylistCover.encode(context, cover)?.let { repository.setCover(id, it) } }
                            if (sent == null || sent.isFailure) {
                                Toasts.show(resources.getString(R.string.playlist_cover_failed), subtitle = sent?.exceptionOrNull()?.message, isError = true)
                            }
                        }
                        toast(context, resources.getString(R.string.playlist_created, name))
                        isDone = true
                    }
                    .onFailure {
                        toast(context, resources.getString(R.string.playlist_action_error), isError = true)
                        isBusy = false
                    }
            }
        }
    }

    WavvySheet(onDismiss = NewPlaylist::dismiss) {
        // Once the playlist is made the sheet slides out
        val closeSheet = LocalSheetClose.current
        LaunchedEffect(isDone) { if (isDone) closeSheet() }

        CardMenuForm(
            title = stringResource(R.string.library_new_playlist),
            action = { if (isSignedIn) CardMenuButton(text = stringResource(R.string.playlist_create), isEnabled = canCreate, onClick = onCreate) }
        ) {
            if (!isSignedIn) {
                SheetMessage(stringResource(R.string.playlist_sign_in))
                return@CardMenuForm
            }

            CardMenuField(
                value = title,
                onValueChange = { title = it },
                placeholder = stringResource(R.string.playlist_title_hint),
                singleLine = true,
                imeAction = ImeAction.Done,
                modifier = Modifier.focusRequester(focus)
            )

            CoverPicker(pickedUri = coverUri, currentUrl = null, onPicked = { coverUri = it })

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

// One choice of who can find the playlist as a card, with the check when it is the one on
@Composable
internal fun PrivacyRow(icon: ImageVector, title: String, isSelected: Boolean, onClick: () -> Unit) {
    MenuCard(
        icon = icon,
        title = title,
        trailing = { if (isSelected) Icon(imageVector = WavvyIcons.Check, contentDescription = null, tint = WavvyTheme.colors.playerAccent) },
        onClick = onClick
    )
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

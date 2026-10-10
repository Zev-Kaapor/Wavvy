package com.wavvy.app.features.podcast.ui

// Compose layouts and foundations
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
// Material 3 components
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
// Compose state and runtime
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
// UI styling and utilities
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
// Project resources
import com.wavvy.app.R
import com.wavvy.app.core.designsystem.icons.WavvyIcons
import com.wavvy.app.core.designsystem.theme.WavvyTheme

// The field that looks for episodes of the podcast or for items of the library, it takes the place of the filters while it is open and the arrow closes it
@Composable
internal fun PodcastSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    onClose: () -> Unit,
    hint: String = stringResource(R.string.podcast_search_hint),
    // How much of its width the field has, it grows from its end so the icon at the start travels to its place
    progress: Float = 1f
) {
    val focus = remember { FocusRequester() }
    val container = WavvyTheme.colors.chip

    // The keyboard comes up as the field opens
    LaunchedEffect(Unit) { focus.requestFocus() }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background.copy(alpha = progress))
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
            .padding(PaddingValues(horizontal = PodcastDimens.Side, vertical = PodcastDimens.ChipVertical))
    ) {
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
        TextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier
                .fillMaxWidth(progress)
                .height(PodcastDimens.SearchHeight)
                .clipToBounds()
                .alpha((progress / PodcastDimens.SearchFadeFraction).coerceIn(0f, 1f))
                .focusRequester(focus),
            placeholder = { Text(text = hint, style = MaterialTheme.typography.bodyMedium, maxLines = 1, softWrap = false) },
            textStyle = MaterialTheme.typography.bodyMedium,
            singleLine = true,
            shape = CircleShape,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { }),
            leadingIcon = {
                Icon(imageVector = WavvyIcons.Search, contentDescription = null)
            },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = container,
                unfocusedContainerColor = container,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            )
        )
        }

        IconButton(onClick = onClose, modifier = Modifier.alpha(progress)) {
            Icon(
                imageVector = WavvyIcons.Close,
                contentDescription = stringResource(R.string.cd_close),
                tint = MaterialTheme.colorScheme.onBackground
            )
        }
    }
}

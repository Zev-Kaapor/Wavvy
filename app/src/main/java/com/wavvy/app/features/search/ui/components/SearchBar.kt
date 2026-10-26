package com.wavvy.app.features.search.ui.components

// Compose animation
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.animation.togetherWith
// Compose layouts and foundations
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
// Material 3 components
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
// Compose state and runtime
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
// UI styling and utilities
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.focus.FocusState
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
// Project resources
import com.wavvy.app.R
import com.wavvy.app.core.designsystem.icons.WavvyIcons
import com.wavvy.app.core.designsystem.theme.WavvyTheme
import com.wavvy.app.features.search.ui.SearchDimens

// Pill field of the search with the arrow to leave it on its side when there is one, and at the end the search or the button that clears what was typed
@Composable
fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    // The arrow on the start that leaves the search, none on a tab, which is left by the bar and the back gesture
    onBack: (() -> Unit)?,
    onFocusChange: (FocusState) -> Unit,
    modifier: Modifier = Modifier,
    // What the empty field says, the one of the search of songs when none is given
    hint: String? = null,
    // The round buttons after the field that search by voice and by the sound of a song, none when the search has no such buttons
    onVoiceSearch: (() -> Unit)? = null,
    onSoundSearch: (() -> Unit)? = null
) {
    val dimens = WavvyTheme.dimens
    val container = WavvyTheme.colors.chip

    Row(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))
            .padding(horizontal = dimens.screenPadding, vertical = dimens.spaceMedium),
        verticalAlignment = Alignment.CenterVertically
    ) {
        onBack?.let { onClick ->
            IconButton(onClick = onClick) {
                Icon(
                    imageVector = WavvyIcons.Back,
                    contentDescription = stringResource(R.string.cd_back),
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
        }

        // The field of Material is never shorter than its own minimum, so the text box is drawn with its look and the height of the bar
        val interactionSource = remember { MutableInteractionSource() }
        val colors = TextFieldDefaults.colors(
            focusedContainerColor = container,
            unfocusedContainerColor = container,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent
        )
        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier
                .weight(1f)
                .height(SearchDimens.FieldHeight)
                .onFocusChanged(onFocusChange),
            textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onSearch() }),
            interactionSource = interactionSource,
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary)
        ) { innerTextField ->
            TextFieldDefaults.DecorationBox(
                value = query,
                innerTextField = innerTextField,
                enabled = true,
                singleLine = true,
                visualTransformation = VisualTransformation.None,
                interactionSource = interactionSource,
                placeholder = {
                    Text(text = hint ?: stringResource(R.string.search_hint), style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                },
                shape = CircleShape,
                colors = colors,
                contentPadding = TextFieldDefaults.contentPaddingWithoutLabel(top = SearchDimens.FieldTextPadding, bottom = SearchDimens.FieldTextPadding),
                trailingIcon = {
                    AnimatedContent(
                        targetState = query.isNotEmpty(),
                        transitionSpec = { fadeIn(tween(SearchDimens.FieldIconMillis)) togetherWith fadeOut(tween(SearchDimens.FieldIconMillis)) },
                        label = "searchFieldIcon"
                    ) { hasText ->
                        if (hasText) {
                            IconButton(onClick = { onQueryChange("") }) {
                                Icon(
                                    imageVector = WavvyIcons.Close,
                                    contentDescription = stringResource(R.string.search_clear),
                                    modifier = Modifier.size(SearchDimens.ClearIcon)
                                )
                            }
                        } else {
                            Icon(
                                imageVector = WavvyIcons.Search,
                                contentDescription = null,
                                modifier = Modifier
                                    .padding(end = SearchDimens.FieldIconEnd)
                                    .size(SearchDimens.FieldIcon)
                            )
                        }
                    }
                }
            )
        }

        onVoiceSearch?.let { RoundButton(icon = WavvyIcons.Mic, description = stringResource(R.string.search_voice_cd), container = container, onClick = it) }
        onSoundSearch?.let { RoundButton(icon = WavvyIcons.Sound, description = stringResource(R.string.search_sound_cd), container = container, onClick = it) }
    }
}

// A round button after the field, as tall as it and with the same color
@Composable
private fun RoundButton(icon: ImageVector, description: String, container: Color, onClick: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .padding(start = SearchDimens.SoundButtonGap)
            .size(SearchDimens.SoundButton)
            .clip(CircleShape)
            .background(container)
            .clickable(onClick = onClick)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.size(SearchDimens.SoundIcon)
        )
    }
}

package com.wavvy.app.features.search.ui.components

// Compose animation
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.animation.togetherWith
// Compose layouts and foundations
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
// UI styling and utilities
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.focus.FocusState
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
// Project resources
import com.wavvy.app.R
import com.wavvy.app.core.designsystem.icons.WavvyIcons
import com.wavvy.app.core.designsystem.theme.WavvyTheme
import com.wavvy.app.features.search.ui.SearchDimens

// Pill field of the search, the arrow to leave it shows while it is in use and the icon at the end is the search or the button that clears what was typed
@Composable
fun SearchBar(
    query: String,
    isActive: Boolean,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onBack: () -> Unit,
    onFocusChange: (FocusState) -> Unit,
    modifier: Modifier = Modifier
) {
    val dimens = WavvyTheme.dimens
    val container = MaterialTheme.colorScheme.surfaceContainerHigh

    Row(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))
            .padding(horizontal = dimens.screenPadding, vertical = dimens.spaceMedium),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isActive) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = WavvyIcons.Back,
                    contentDescription = stringResource(R.string.cd_back),
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
        }

        TextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier
                .weight(1f)
                .height(SearchDimens.FieldHeight)
                .onFocusChanged(onFocusChange),
            placeholder = {
                Text(text = stringResource(R.string.search_hint), style = MaterialTheme.typography.bodyMedium)
            },
            textStyle = MaterialTheme.typography.bodyMedium,
            singleLine = true,
            shape = CircleShape,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onSearch() }),
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
            },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = container,
                unfocusedContainerColor = container,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            )
        )
    }
}

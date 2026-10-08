package com.wavvy.app.features.search.ui.components

// Compose layouts and foundations
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
// Material 3 components
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
// Compose state and runtime
import androidx.compose.runtime.Composable
// UI styling and utilities
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
// Project resources
import com.wavvy.app.R
import com.wavvy.app.core.designsystem.icons.WavvyIcons
import com.wavvy.app.core.designsystem.theme.WavvyTheme
import com.wavvy.app.features.home.data.HomeItem
import com.wavvy.app.features.home.ui.components.HomeListItem
import com.wavvy.app.features.home.ui.components.HomeType
import com.wavvy.app.features.player.ui.LocalMiniPlayerInset
import com.wavvy.app.features.search.data.SearchSuggestion
import com.wavvy.app.features.search.ui.SearchDimens

// Searches made before, with the button that clears them all, a tap searches again and the arrow puts the words back in the field
@Composable
fun SearchHistory(
    history: List<String>,
    suggested: List<String>,
    onSearch: (String) -> Unit,
    onInsert: (String) -> Unit,
    onRemove: (String) -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dimens = WavvyTheme.dimens

    if (history.isEmpty() && suggested.isEmpty()) {
        Box(modifier = modifier.fillMaxSize())
        return
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = dimens.screenPadding, vertical = SearchDimens.HeaderPaddingVertical)
    ) {
        if (history.isNotEmpty()) item(key = "header") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(R.string.search_history),
                    style = HomeType.SectionTitle,
                    color = MaterialTheme.colorScheme.onBackground
                )
                TextButton(onClick = onClear) {
                    Text(text = stringResource(R.string.search_clear_all), style = MaterialTheme.typography.labelLarge)
                }
            }
        }

        items(history, key = { it }) { query ->
            SuggestionRow(
                text = query,
                icon = WavvyIcons.History,
                onClick = { onSearch(query) },
                onInsert = { onInsert(query) },
                onRemove = { onRemove(query) }
            )
        }

        // Searches that may please the user, a tap searches and the arrow puts the words in the field
        if (suggested.isNotEmpty()) {
            item(key = "suggested") {
                Text(
                    text = stringResource(R.string.search_suggested),
                    style = HomeType.SectionTitle,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(top = SearchDimens.SuggestedTop, bottom = SearchDimens.HeaderPaddingVertical)
                )
            }

            items(suggested, key = { "suggested-$it" }) { query ->
                SuggestionRow(
                    text = query,
                    icon = WavvyIcons.Search,
                    onClick = { onSearch(query) },
                    onInsert = { onInsert(query) }
                )
            }
        }

        item(key = "inset") { Spacer(modifier = Modifier.height(LocalMiniPlayerInset.current)) }
    }
}

// Words and songs that complete what is typed, the words search and the cards act like the ones of the results
@Composable
fun SearchSuggestionList(
    suggestions: List<SearchSuggestion>,
    isLoading: Boolean,
    onSearch: (String) -> Unit,
    onInsert: (String) -> Unit,
    onItemClick: (HomeItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val dimens = WavvyTheme.dimens

    // Nothing came back and nothing is coming
    if (suggestions.isEmpty() && !isLoading) {
        Box(modifier = modifier.fillMaxSize().padding(SearchDimens.EmptyPadding), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(SearchDimens.EmptyGap)) {
                Icon(
                    imageVector = WavvyIcons.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier
                        .size(SearchDimens.EmptyIcon)
                        .alpha(SearchDimens.EmptyIconAlpha)
                )
                Text(
                    text = stringResource(R.string.search_no_suggestions),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondary,
                    textAlign = TextAlign.Center
                )
            }
        }
        return
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = dimens.screenPadding, vertical = SearchDimens.HeaderPaddingVertical)
    ) {
        items(suggestions) { suggestion ->
            when (suggestion) {
                is SearchSuggestion.Words -> SuggestionRow(
                    text = suggestion.text,
                    icon = if (suggestion.isHistory) WavvyIcons.History else WavvyIcons.Search,
                    onClick = { onSearch(suggestion.text) },
                    onInsert = { onInsert(suggestion.text) }
                )

                is SearchSuggestion.Match -> HomeListItem(
                    item = suggestion.item,
                    onClick = { onItemClick(suggestion.item) }
                )
            }
        }

        item(key = "inset") { Spacer(modifier = Modifier.height(LocalMiniPlayerInset.current)) }
    }
}

// One line of words with its icon, the arrow that puts them in the field and, for the history, the button that removes them
@Composable
private fun SuggestionRow(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    onInsert: () -> Unit,
    modifier: Modifier = Modifier,
    onRemove: (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(SearchDimens.RowCorner))
            .clickable(onClick = onClick)
            .padding(SearchDimens.RowPadding),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier
                .padding(horizontal = SearchDimens.RowPadding)
                .size(SearchDimens.RowIcon)
                .alpha(SearchDimens.RowIconAlpha)
        )

        Spacer(modifier = Modifier.width(SearchDimens.RowGap))

        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )

        if (onRemove != null) {
            RowAction(icon = WavvyIcons.Close, description = stringResource(R.string.search_remove), onClick = onRemove)
        }

        RowAction(icon = WavvyIcons.NorthWest, description = stringResource(R.string.search_insert), onClick = onInsert)
    }
}

// Small button at the end of a line of words
@Composable
private fun RowAction(
    icon: ImageVector,
    description: String,
    onClick: () -> Unit
) {
    IconButton(onClick = onClick, modifier = Modifier.size(SearchDimens.RowAction)) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier
                .size(SearchDimens.RowActionIcon)
                .alpha(SearchDimens.RowActionAlpha)
        )
    }
}

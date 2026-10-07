package com.wavvy.app.features.discography.ui

// Compose layouts and foundations
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
// Material 3 components
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
// Compose state and runtime
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
// UI styling and utilities
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
// Lifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
// Project resources
import com.wavvy.app.R
import com.wavvy.app.core.designsystem.icons.WavvyIcons
import com.wavvy.app.core.designsystem.theme.WavvyTheme
import com.wavvy.app.features.home.data.HomeFilter
import com.wavvy.app.features.home.data.HomeItem
import com.wavvy.app.features.home.ui.components.HomeDimens
import com.wavvy.app.features.home.ui.components.HomeFilterRow
import com.wavvy.app.features.home.ui.components.HomeMessage
import com.wavvy.app.features.home.ui.components.HomeType
import com.wavvy.app.features.home.ui.components.ItemBadges
import com.wavvy.app.features.home.ui.components.ItemThumbnail
import com.wavvy.app.features.home.ui.components.coverShape
import com.wavvy.app.features.home.ui.components.itemSubtitle
import com.wavvy.app.features.home.ui.rememberItemPlayer
import com.wavvy.app.features.menu.ItemMenu
import com.wavvy.app.features.player.ui.LocalMiniPlayerInset

// The full list behind a shelf of an artist, such as all the singles, with the name of the artist on top, the shelf as the filter under it and the covers in two columns
@Composable
fun DiscographyScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DiscographyViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()
    val onItemClick = rememberItemPlayer()
    val dimens = WavvyTheme.dimens

    // Solid, so the screen under it does not show through while the page slides
    Column(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))
                .padding(horizontal = dimens.spaceSmall)
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = WavvyIcons.Back,
                    contentDescription = stringResource(R.string.cd_back),
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }

            Text(
                text = state.title,
                style = MaterialTheme.typography.titleLarge.merge(HomeType.SectionTitle),
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = dimens.spaceSmall)
            )
        }

        // The filters of the list, such as albums and singles, until they arrive the shelf the list came from stands as the only one
        val filters = if (state.chips.isNotEmpty()) {
            state.chips.mapIndexed { index, chip -> HomeFilter(title = chip.title, params = index.toString()) }
        } else {
            listOfNotNull(state.filter.takeIf { it.isNotEmpty() }?.let { HomeFilter(title = it, params = "") })
        }
        if (filters.isNotEmpty()) {
            val selected = filters.getOrNull(state.chips.indexOfFirst { it.isSelected }) ?: filters.first()
            HomeFilterRow(
                filters = filters,
                selected = selected,
                onSelect = { filter -> filter.params.toIntOrNull()?.let { state.chips.getOrNull(it) }?.let(viewModel::select) },
                modifier = Modifier.padding(vertical = dimens.spaceSmall)
            )
        }

        when {
            state.status == DiscographyStatus.Loading -> Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }

            state.status == DiscographyStatus.Error || state.items.isEmpty() -> HomeMessage(
                text = stringResource(if (state.status == DiscographyStatus.Error) R.string.discography_error else R.string.collection_empty),
                modifier = Modifier.weight(1f),
                actionLabel = if (state.status == DiscographyStatus.Error) stringResource(R.string.home_retry) else null,
                onAction = viewModel::load
            )

            else -> LazyVerticalGrid(
                columns = GridCells.Fixed(DiscographyColumns),
                modifier = Modifier
                    .weight(1f)
                    .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)),
                contentPadding = PaddingValues(
                    start = dimens.screenPadding,
                    top = dimens.spaceSmall,
                    end = dimens.screenPadding,
                    bottom = LocalMiniPlayerInset.current + dimens.spaceSmall
                ),
                horizontalArrangement = Arrangement.spacedBy(dimens.spaceMedium),
                verticalArrangement = Arrangement.spacedBy(dimens.spaceMedium)
            ) {
                items(state.items, key = { it.id }) { item ->
                    DiscographyCard(item = item, onClick = { onItemClick(item) })
                }
            }
        }
    }
}

// How many covers sit side by side
private const val DiscographyColumns = 2

// Card with a cover as wide as its column, the title and the line under it
@Composable
private fun DiscographyCard(
    item: HomeItem,
    onClick: () -> Unit
) {
    val base = MaterialTheme.typography.bodyMedium

    Column(modifier = Modifier.combinedClickable(onClick = onClick, onLongClick = { ItemMenu.show(item) })) {
        ItemThumbnail(url = item.thumbnailUrl, shape = coverShape(item), modifier = Modifier.fillMaxWidth())

        Spacer(modifier = Modifier.height(HomeDimens.GridTextGap))

        Text(
            text = item.title,
            style = base.merge(HomeType.GridTitle),
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            ItemBadges(item = item)

            itemSubtitle(item)?.let { subtitle ->
                Text(
                    text = subtitle,
                    style = base.merge(HomeType.GridSubtitle),
                    color = MaterialTheme.colorScheme.secondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

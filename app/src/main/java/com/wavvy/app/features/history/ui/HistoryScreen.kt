package com.wavvy.app.features.history.ui

// Android time formats
import android.text.format.DateFormat
// Compose layouts and foundations
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
// Material 3 components
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
// Compose state and runtime
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
// UI styling and utilities
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.viewmodel.compose.viewModel
// Time and coroutines
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.temporal.WeekFields
import java.util.Locale
import java.util.Date
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
// Image loading
import coil3.compose.AsyncImage
// Project resources
import com.wavvy.app.R
import com.wavvy.app.core.designsystem.components.ConfirmDialog
import com.wavvy.app.core.designsystem.icons.WavvyIcons
import com.wavvy.app.core.designsystem.theme.WavvyTheme
import com.wavvy.app.core.history.PlayHistory
import com.wavvy.app.core.history.SongEntity
import com.wavvy.app.features.home.data.toHomeItem
import com.wavvy.app.features.home.ui.components.HomeMessage
import com.wavvy.app.features.home.ui.components.HomeType
import com.wavvy.app.features.home.ui.rememberListPlayer
import com.wavvy.app.features.menu.ItemMenu
import com.wavvy.app.features.player.ui.LocalMiniPlayerInset

// What was listened to, the most recent first, in groups by the day, a tap plays it, a long press opens its menu and the cross takes it out
// The button on top clears the whole history, after a question
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HistoryScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val dimens = WavvyTheme.dimens
    val songs by remember { PlayHistory.all(context) }.collectAsState(initial = null)
    val playList = rememberListPlayer()
    var isClearing by remember { mutableStateOf(false) }
    var filter by rememberSaveable { mutableStateOf<HistoryFilter?>(null) }
    var expanded by remember(filter) { mutableStateOf<Set<LocalDate>>(emptySet()) }

    val list = songs
    val zone = remember { ZoneId.systemDefault() }
    val today = remember { LocalDate.now(zone) }
    val dateFormat = remember { DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM) }

    if (isClearing) {
        ConfirmDialog(
            title = stringResource(R.string.history_clear_title),
            text = stringResource(R.string.history_clear_text),
            confirmLabel = stringResource(R.string.history_clear_confirm),
            cancelLabel = stringResource(R.string.playlist_cancel),
            onConfirm = {
                isClearing = false
                scope.launch(Dispatchers.IO) { PlayHistory.clearHistory(context) }
            },
            onDismiss = { isClearing = false }
        )
    }

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
                Icon(imageVector = WavvyIcons.Back, contentDescription = stringResource(R.string.cd_back), tint = MaterialTheme.colorScheme.onBackground)
            }
            Text(
                text = stringResource(R.string.history_title),
                style = MaterialTheme.typography.titleLarge.merge(HomeType.SectionTitle),
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f).padding(start = dimens.spaceSmall)
            )
            if (!list.isNullOrEmpty()) {
                IconButton(onClick = { isClearing = true }) {
                    Icon(imageVector = WavvyIcons.Delete, contentDescription = stringResource(R.string.history_clear_title), tint = MaterialTheme.colorScheme.onBackground)
                }
            }
        }

        // The periods, a tap on the one that is on turns it off
        if (!list.isNullOrEmpty()) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(HistoryDimens.ChipGap),
                contentPadding = PaddingValues(horizontal = HistoryDimens.Side, vertical = HistoryDimens.ChipsVertical),
                modifier = Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
            ) {
                items(HistoryFilter.entries, key = { it.name }) { option ->
                    HistoryChip(text = stringResource(option.titleRes), isOn = filter == option, onClick = { filter = if (filter == option) null else option })
                }
            }
        }

        val firstDay = remember { WeekFields.of(Locale.getDefault()).firstDayOfWeek }
        val narrowed = list?.filter { song ->
            filter?.matches(Instant.ofEpochMilli(song.lastPlayedAt).atZone(zone).toLocalDate(), today, firstDay) ?: true
        }

        when {
            // Nothing is drawn until the database answers
            list == null -> Box(modifier = Modifier.weight(1f).fillMaxWidth())

            list.isEmpty() -> HomeMessage(text = stringResource(R.string.history_empty), modifier = Modifier.weight(1f))

            narrowed.isNullOrEmpty() -> HomeMessage(text = stringResource(R.string.history_empty_period), modifier = Modifier.weight(1f))

            else -> {
                // Each day shows its first songs, the arrow under them shows all the ones of that day
                val days = narrowed.groupBy { Instant.ofEpochMilli(it.lastPlayedAt).atZone(zone).toLocalDate() }
                val items = remember(narrowed) { narrowed.map(SongEntity::toHomeItem) }

                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)),
                    contentPadding = PaddingValues(bottom = LocalMiniPlayerInset.current + dimens.spaceSmall)
                ) {
                    days.forEach { (day, songsOfDay) ->
                        item(key = "day-$day") {
                            Text(
                                text = when (day) {
                                    today -> stringResource(R.string.history_today)
                                    today.minusDays(1) -> stringResource(R.string.history_yesterday)
                                    else -> dateFormat.format(day)
                                },
                                style = MaterialTheme.typography.titleMedium.merge(HomeType.SectionTitle),
                                color = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier.padding(horizontal = HistoryDimens.Side, vertical = HistoryDimens.DayVertical)
                            )
                        }
                        val isOpen = day in expanded
                        items(if (isOpen) songsOfDay else songsOfDay.take(HistoryDimens.PageSize), key = { it.id }) { song ->
                            val time = DateFormat.getTimeFormat(context).format(Date(song.lastPlayedAt))
                            HistoryRow(
                                song = song,
                                line = listOfNotNull(song.artist, time).joinToString(" • "),
                                onClick = { playList(items, items.indexOfFirst { it.id == song.id }.coerceAtLeast(0), null) },
                                onLongClick = { ItemMenu.show(song.toHomeItem()) },
                                onRemove = { scope.launch(Dispatchers.IO) { PlayHistory.removeFromHistory(context, song.id) } }
                            )
                        }

                        if (!isOpen && songsOfDay.size > HistoryDimens.PageSize) {
                            item(key = "more-$day") {
                                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth().height(HistoryDimens.MoreHeight)) {
                                    IconButton(onClick = { expanded = expanded + day }) {
                                        Icon(imageVector = WavvyIcons.ChevronDown, contentDescription = stringResource(R.string.history_more), tint = MaterialTheme.colorScheme.onBackground)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// A period of the filters, light on the dark theme when it is on, as the buttons of the library
@Composable
private fun HistoryChip(text: String, isOn: Boolean, onClick: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .height(HistoryDimens.ChipHeight)
            .clip(RoundedCornerShape(HistoryDimens.ChipCorner))
            .background(if (isOn) MaterialTheme.colorScheme.onBackground else WavvyTheme.colors.chip)
            .clickable(onClick = onClick)
            .padding(horizontal = HistoryDimens.ChipPaddingX)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
            color = if (isOn) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.onBackground,
            maxLines = 1
        )
    }
}

// A song of the history, its cover, its name, the artist and the time, and the cross that takes it out
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HistoryRow(song: SongEntity, line: String, onClick: () -> Unit, onLongClick: () -> Unit, onRemove: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .height(HistoryDimens.RowHeight)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(start = HistoryDimens.Side)
    ) {
        AsyncImage(
            model = song.artworkUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(HistoryDimens.Cover)
                .clip(RoundedCornerShape(HistoryDimens.CoverCorner))
                .background(MaterialTheme.colorScheme.surfaceContainer)
        )
        Column(modifier = Modifier.weight(1f).padding(horizontal = HistoryDimens.Gap)) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = line,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        IconButton(onClick = onRemove) {
            Icon(imageVector = WavvyIcons.Close, contentDescription = stringResource(R.string.history_remove), tint = MaterialTheme.colorScheme.onBackground)
        }
    }
}

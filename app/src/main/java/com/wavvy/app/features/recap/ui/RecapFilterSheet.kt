package com.wavvy.app.features.recap.ui

// Time
import com.wavvy.app.features.menu.CardMenuForm
import com.wavvy.app.features.menu.CardMenuSection
import com.wavvy.app.features.menu.CardSwitchRow
import com.wavvy.app.features.menu.ItemMenuDimens
import com.wavvy.app.features.menu.MenuCardDivider
import com.wavvy.app.features.menu.MenuCardGroup
import java.time.LocalDate
import java.time.ZoneId
// Compose layouts and foundations
import androidx.compose.foundation.layout.padding
// Material 3 components
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
// Compose state and runtime
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
// UI styling and utilities
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
// Coroutines
import kotlinx.coroutines.launch
// Project resources
import com.wavvy.app.R
import com.wavvy.app.core.designsystem.components.WavvySheet
import com.wavvy.app.core.history.PlayHistory
import com.wavvy.app.core.history.TopSong
import com.wavvy.app.features.recap.data.RecapFilter

// How many songs the sheet lists to be taken out or put back
private const val ListedSongs = 40

// What the retrospective counts: tracks that are too long to be music, and the songs the user takes out one by one, such as a rain sound or a song of a child
@Composable
fun RecapFilterSheet(year: Int, onChanged: () -> Unit, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var skipsLong by remember { mutableStateOf(RecapFilter.skipsLong(context)) }
    var skipsLoops by remember { mutableStateOf(RecapFilter.skipsLoops(context)) }
    val songs by produceState(initialValue = emptyList<TopSong>(), year) {
        val zone = ZoneId.systemDefault()
        val since = LocalDate.of(year, 1, 1).atStartOfDay(zone).toInstant().toEpochMilli()
        val until = LocalDate.of(year + 1, 1, 1).atStartOfDay(zone).toInstant().toEpochMilli()
        value = PlayHistory.topSongs(context, since, until, ListedSongs)
    }
    // What was changed here shows at once, before the history is read again
    var changes by remember { mutableStateOf(emptyMap<String, Boolean>()) }

    WavvySheet(onDismiss = onDismiss) {
        CardMenuForm(title = stringResource(R.string.recap_filter_title)) {
            Text(
                text = stringResource(R.string.recap_filter_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = ItemMenuDimens.CardRowHorizontal)
            )

            MenuCardGroup {
                CardSwitchRow(
                    title = stringResource(R.string.recap_filter_long),
                    hint = stringResource(R.string.recap_filter_long_hint),
                    checked = skipsLong,
                    onChange = {
                        skipsLong = it
                        RecapFilter.setSkipsLong(context, it)
                        onChanged()
                    }
                )
                MenuCardDivider()
                CardSwitchRow(
                    title = stringResource(R.string.recap_filter_loops),
                    hint = stringResource(R.string.recap_filter_loops_hint),
                    checked = skipsLoops,
                    onChange = {
                        skipsLoops = it
                        RecapFilter.setSkipsLoops(context, it)
                        onChanged()
                    }
                )
            }

            if (songs.isNotEmpty()) {
                CardMenuSection(stringResource(R.string.recap_filter_songs))
                MenuCardGroup {
                    songs.forEachIndexed { index, top ->
                        val isCounted = !(changes[top.song.id] ?: top.song.isExcluded)

                        if (index > 0) MenuCardDivider()
                        CardSwitchRow(
                            title = top.song.title,
                            hint = top.song.artist,
                            checked = isCounted,
                            onChange = { counts ->
                                changes = changes + (top.song.id to !counts)
                                scope.launch {
                                    PlayHistory.setExcluded(context, top.song.id, isExcluded = !counts)
                                    onChanged()
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

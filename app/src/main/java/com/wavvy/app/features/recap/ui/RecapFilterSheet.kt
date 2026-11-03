package com.wavvy.app.features.recap.ui

// Time
import java.time.LocalDate
import java.time.ZoneId
// Compose layouts and foundations
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
// Coroutines
import kotlinx.coroutines.launch
// Project resources
import com.wavvy.app.R
import com.wavvy.app.core.designsystem.components.WavvySheet
import com.wavvy.app.core.designsystem.components.WavvySwitch
import com.wavvy.app.core.history.PlayHistory
import com.wavvy.app.core.history.TopSong
import com.wavvy.app.features.player.ui.components.PlayerDimens
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
        Column(
            verticalArrangement = Arrangement.spacedBy(PlayerDimens.OptionsRowGap),
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = PlayerDimens.OptionsSide)
                .padding(bottom = PlayerDimens.OptionsBottom)
        ) {
            Text(text = stringResource(R.string.recap_filter_title), style = MaterialTheme.typography.titleLarge)
            Text(text = stringResource(R.string.recap_filter_hint), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(top = PlayerDimens.OptionsRowGap)) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = stringResource(R.string.recap_filter_long), style = MaterialTheme.typography.bodyLarge)
                    Text(text = stringResource(R.string.recap_filter_long_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                WavvySwitch(checked = skipsLong, onCheckedChange = {
                    skipsLong = it
                    RecapFilter.setSkipsLong(context, it)
                    onChanged()
                })
            }

            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = stringResource(R.string.recap_filter_loops), style = MaterialTheme.typography.bodyLarge)
                    Text(text = stringResource(R.string.recap_filter_loops_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                WavvySwitch(checked = skipsLoops, onCheckedChange = {
                    skipsLoops = it
                    RecapFilter.setSkipsLoops(context, it)
                    onChanged()
                })
            }

            Text(
                text = stringResource(R.string.recap_filter_songs),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                modifier = Modifier.padding(top = PlayerDimens.OptionsRowGap)
            )
            songs.forEach { top ->
                val isCounted = !(changes[top.song.id] ?: top.song.isExcluded)

                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = top.song.title, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(text = top.song.artist.orEmpty(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    WavvySwitch(checked = isCounted, onCheckedChange = { counts ->
                        changes = changes + (top.song.id to !counts)
                        scope.launch {
                            PlayHistory.setExcluded(context, top.song.id, isExcluded = !counts)
                            onChanged()
                        }
                    })
                }
            }
        }
    }
}

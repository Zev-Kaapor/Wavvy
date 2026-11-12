package com.wavvy.app.features.player.ui.components

// Compose animation
import com.wavvy.app.features.menu.CardMenuButton
import com.wavvy.app.features.menu.CardMenuField
import com.wavvy.app.features.menu.CardMenuForm
import com.wavvy.app.features.menu.ItemMenuDimens
import com.wavvy.app.core.designsystem.theme.WavvyTheme
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
// Compose layouts and foundations
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
// Material 3 components
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
// Compose state and runtime
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
// UI styling and utilities
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
// Coroutines
import kotlinx.coroutines.launch
// Math
import kotlin.math.abs
import kotlin.math.floor
// Project resources
import com.wavvy.app.R
import com.wavvy.app.core.designsystem.components.WavvySheet
import com.wavvy.app.core.designsystem.theme.DarkColors
import com.wavvy.app.core.lyrics.LyricsMatch
import com.wavvy.app.core.lyrics.SongLyrics
import com.wavvy.app.core.playback.PlayerConnection
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow

// Manual search of the lyrics, the song and artist can be fixed and every source is asked again, the one tapped is used for the song
@Composable
fun LyricsSearchSheet(
    initialTitle: String,
    initialArtist: String,
    onSearch: (title: String, artist: String) -> Flow<LyricsMatch>,
    onChoose: (LyricsMatch) -> Unit,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var title by remember { mutableStateOf(initialTitle) }
    var artist by remember { mutableStateOf(initialArtist) }
    var results by remember { mutableStateOf<List<LyricsMatch>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }
    var hasSearched by remember { mutableStateOf(false) }

    // Position of the song, followed while the sheet is open so each timed result shows the line it says is sung now
    var position by remember { mutableLongStateOf(PlayerConnection.positionMs) }
    LaunchedEffect(Unit) {
        while (true) {
            position = PlayerConnection.positionMs
            delay(PlayerDimens.LyricsRefreshMillis)
        }
    }

    // Asks every source again with what is typed, each one shows as soon as it answers, word by word first, then timed, then plain, and the order of the sources keeps the ones of the same kind in place
    // A search that is running is not started twice
    fun search() {
        if (isSearching || title.isBlank()) return
        isSearching = true
        hasSearched = true
        results = emptyList()
        scope.launch {
            try {
                onSearch(title.trim(), artist.trim()).collect { match ->
                    results = (results + match).sortedWith(compareByDescending<LyricsMatch> { it.quality }.thenBy { it.order })
                }
            } finally {
                isSearching = false
            }
        }
    }

    DarkColors {
        WavvySheet(onDismiss = onDismiss) {
            CardMenuForm(
                title = stringResource(R.string.lyrics_search_title),
                action = { CardMenuButton(text = stringResource(R.string.lyrics_search_go), isEnabled = !isSearching && title.isNotBlank(), onClick = ::search) }
            ) {
                CardMenuField(
                    value = title,
                    onValueChange = { title = it },
                    label = stringResource(R.string.lyrics_search_song),
                    singleLine = true,
                    capitalization = KeyboardCapitalization.None,
                    imeAction = ImeAction.Next
                )
                CardMenuField(
                    value = artist,
                    onValueChange = { artist = it },
                    label = stringResource(R.string.lyrics_search_artist),
                    singleLine = true,
                    capitalization = KeyboardCapitalization.None,
                    imeAction = ImeAction.Search,
                    keyboardActions = KeyboardActions(onSearch = { search() })
                )

                results.forEach { match -> MatchCard(match, positionMs = { position }, onClick = { onChoose(match) }) }

                // The circle stays under what already came until the last source answers, and nothing found is only said then
                when {
                    isSearching -> Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(modifier = Modifier.padding(PlayerDimens.OptionsSectionGap).size(PlayerDimens.SearchSpinner))
                    }
                    hasSearched && results.isEmpty() -> Text(
                        text = stringResource(R.string.lyrics_search_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = ItemMenuDimens.CardRowHorizontal)
                    )
                }
            }
        }
    }
}

// What one source found, the song it is for, how the lyrics are timed and how long they are, and the lyrics going up to see them
@Composable
private fun MatchCard(match: LyricsMatch, positionMs: () -> Long, onClick: () -> Unit) {
    val lyrics = match.lyrics
    val kind = when {
        lyrics.lines.any { it.words.isNotEmpty() } -> stringResource(R.string.lyrics_result_word)
        lyrics.isSynced -> stringResource(R.string.lyrics_result_timed)
        else -> stringResource(R.string.lyrics_result_plain)
    }

    Surface(
        shape = RoundedCornerShape(ItemMenuDimens.CardCorner),
        color = WavvyTheme.colors.menuCard,
        contentColor = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(PlayerDimens.OptionsTitleGap),
            modifier = Modifier.padding(PlayerDimens.SearchResultPadding)
        ) {
            Text(
                text = match.title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = match.artist,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = stringResource(
                    R.string.lyrics_result_summary,
                    stringResource(sourceName(match.source)),
                    kind,
                    pluralStringResource(R.plurals.lyrics_result_lines, lyrics.lines.size, lyrics.lines.size)
                ),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
            // Timed lyrics follow the song to be compared with what is sung, the others only go up to be read
            if (lyrics.isSynced) {
                SyncedPreview(lyrics = lyrics, positionMs = positionMs)
            } else {
                LyricsPreview(lines = remember(lyrics) { lyrics.lines.take(PlayerDimens.SearchPreviewMaxLines).map { it.text } })
            }
        }
    }
}

// The line sung now by the timing of the result, between the one before and the one after, only the line that changes builds again
@Composable
private fun SyncedPreview(lyrics: SongLyrics, positionMs: () -> Long) {
    val lines = lyrics.lines
    val index by remember(lyrics) { derivedStateOf { lines.indexOfLast { it.timeMs <= positionMs() } } }

    Column(
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxWidth()
            .height(PlayerDimens.SearchPreviewHeight)
    ) {
        for (offset in -1..1) {
            val line = lines.getOrNull(index + offset)
            Text(
                text = line?.text.orEmpty(),
                style = if (offset == 0) {
                    MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                } else {
                    MaterialTheme.typography.bodySmall
                },
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (offset == 0) 1f else PlayerDimens.SearchSyncedDimAlpha),
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(PlayerDimens.SearchPreviewLine)
            )
        }
    }
}

// Lines of the lyrics going slowly up, the one in the middle bright and the ones at the ends fading away
// The position is read only while drawing, so the movement never builds the card again, only the lines in view change when one passes
@Composable
private fun LyricsPreview(lines: List<String>) {
    val transition = rememberInfiniteTransition(label = "Preview")
    val position = transition.animateFloat(
        initialValue = 0f,
        targetValue = lines.size.toFloat(),
        animationSpec = infiniteRepeatable(tween(lines.size * PlayerDimens.SearchPreviewMillisPerLine, easing = LinearEasing)),
        label = "PreviewPosition"
    )
    val base by remember { derivedStateOf { floor(position.value).toInt() } }
    val lineHeight = with(LocalDensity.current) { PlayerDimens.SearchPreviewLine.toPx() }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxWidth()
            .height(PlayerDimens.SearchPreviewHeight)
    ) {
        for (index in base - PlayerDimens.SearchPreviewReach..base + PlayerDimens.SearchPreviewReach + 1) {
            key(index) {
                Text(
                    text = lines[Math.floorMod(index, lines.size)],
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            // A line sits at its distance in lines from the middle, and fades the further it is
                            val distance = index - position.value
                            translationY = distance * lineHeight
                            alpha = (1f - abs(distance) / (PlayerDimens.SearchPreviewReach + 1)).coerceIn(0f, 1f)
                        }
                )
            }
        }
    }
}

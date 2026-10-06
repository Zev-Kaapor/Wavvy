package com.wavvy.app.features.player.ui.components

// Compose animation
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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
// Material 3 components
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
// Compose state and runtime
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
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

// Manual search of the lyrics, the song and artist can be fixed and every source is asked again, the one tapped is used for the song
@Composable
fun LyricsSearchSheet(
    initialTitle: String,
    initialArtist: String,
    onSearch: suspend (title: String, artist: String) -> List<LyricsMatch>,
    onChoose: (LyricsMatch) -> Unit,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var title by remember { mutableStateOf(initialTitle) }
    var artist by remember { mutableStateOf(initialArtist) }
    var results by remember { mutableStateOf<List<LyricsMatch>?>(null) }
    var isSearching by remember { mutableStateOf(false) }

    // Asks every source again with what is typed, a search that is running is not started twice
    fun search() {
        if (isSearching || title.isBlank()) return
        isSearching = true
        scope.launch {
            results = onSearch(title.trim(), artist.trim())
            isSearching = false
        }
    }

    DarkColors {
        WavvySheet(onDismiss = onDismiss) {
            Column(
                verticalArrangement = Arrangement.spacedBy(PlayerDimens.OptionsRowGap),
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = PlayerDimens.OptionsSide)
                    .padding(bottom = PlayerDimens.OptionsBottom)
            ) {
                Text(text = stringResource(R.string.lyrics_search_title), style = MaterialTheme.typography.titleLarge)

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(stringResource(R.string.lyrics_search_song)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = artist,
                    onValueChange = { artist = it },
                    label = { Text(stringResource(R.string.lyrics_search_artist)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { search() }),
                    modifier = Modifier.fillMaxWidth()
                )

                Button(onClick = ::search, enabled = !isSearching && title.isNotBlank(), modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.lyrics_search_go))
                }

                when {
                    isSearching -> CircularProgressIndicator(
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .padding(PlayerDimens.OptionsSectionGap)
                            .size(PlayerDimens.SearchSpinner)
                    )
                    results?.isEmpty() == true -> Text(
                        text = stringResource(R.string.lyrics_search_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    else -> results?.forEach { match -> MatchCard(match, onClick = { onChoose(match) }) }
                }
            }
        }
    }
}

// What one source found, the song it is for, how the lyrics are timed and how long they are, and the lyrics going up to see them
@Composable
private fun MatchCard(match: LyricsMatch, onClick: () -> Unit) {
    val lyrics = match.lyrics
    val kind = when {
        lyrics.lines.any { it.words.isNotEmpty() } -> stringResource(R.string.lyrics_result_word)
        lyrics.isSynced -> stringResource(R.string.lyrics_result_timed)
        else -> stringResource(R.string.lyrics_result_plain)
    }

    Surface(
        shape = RoundedCornerShape(PlayerDimens.SearchResultCorner),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
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
            LyricsPreview(lines = remember(lyrics) { lyrics.lines.take(PlayerDimens.SearchPreviewMaxLines).map { it.text } })
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

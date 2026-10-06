package com.wavvy.app.features.player.ui.components

// Compose animation
import androidx.compose.animation.AnimatedVisibility
// Compose layouts and foundations
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
// Material 3 components
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
// Compose state and runtime
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
// UI styling and utilities
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
// Java utilities
import java.util.Locale
// Project resources
import com.wavvy.app.R
import com.wavvy.app.core.designsystem.components.WavvySheet
import com.wavvy.app.core.designsystem.icons.WavvyIcons
import com.wavvy.app.core.designsystem.theme.DarkColors
import com.wavvy.app.core.lyrics.LyricsAlignment
import com.wavvy.app.core.lyrics.LyricsDisplay
import com.wavvy.app.core.lyrics.LyricsSettings
import com.wavvy.app.core.lyrics.LyricsSource
import com.wavvy.app.core.lyrics.LyricsTextSize

// Languages the lyrics can be translated into, besides the one of the device, as language tags
private val TranslationLanguages = listOf(
    "pt", "en", "es", "fr", "de", "it", "ja", "ko", "zh-CN", "zh-TW", "ru", "ar", "hi", "tr", "nl", "pl", "id", "uk", "he"
)

// Panel of the lyrics options, opened from the menu of the lyrics, every change is saved at once
@Composable
fun LyricsSettingsSheet(
    settings: LyricsSettings,
    onChange: (LyricsSettings) -> Unit,
    onSearchAgain: () -> Unit,
    onSearchManually: () -> Unit,
    onDismiss: () -> Unit
) {
    DarkColors {
        WavvySheet(onDismiss = onDismiss) {
            Column(
                verticalArrangement = Arrangement.spacedBy(PlayerDimens.OptionsSectionGap),
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = PlayerDimens.OptionsSide)
                    .padding(bottom = PlayerDimens.OptionsBottom)
            ) {
                Text(text = stringResource(R.string.lyrics_options), style = MaterialTheme.typography.titleLarge)

                TranslationSection(settings, onChange)
                DisplaySection(settings, onChange)
                SourcesSection(settings, onChange, onSearchAgain, onSearchManually)
            }
        }
    }
}

// Translation on or off and the language it goes into
@Composable
private fun TranslationSection(settings: LyricsSettings, onChange: (LyricsSettings) -> Unit) {
    var showLanguages by remember { mutableStateOf(false) }
    val deviceLocale = LocalConfiguration.current.locales[0]
    val deviceLanguage = languageName(deviceLocale.language, deviceLocale)
    val chosen = if (settings.targetLanguage.isEmpty()) {
        stringResource(R.string.lyrics_language_device, deviceLanguage)
    } else {
        languageName(settings.targetLanguage, deviceLocale)
    }

    Section(title = stringResource(R.string.lyrics_section_translation)) {
        SwitchRow(
            title = stringResource(R.string.lyrics_translate),
            checked = settings.translate,
            onCheckedChange = { onChange(settings.copy(translate = it)) }
        )

        OptionRow(
            title = stringResource(R.string.lyrics_translate_language),
            hint = chosen,
            onClick = { showLanguages = !showLanguages }
        ) {
            Icon(imageVector = if (showLanguages) WavvyIcons.ArrowUp else WavvyIcons.ArrowDown, contentDescription = null)
        }

        AnimatedVisibility(visible = showLanguages) {
            Column {
                LanguageRow(
                    name = stringResource(R.string.lyrics_language_device, deviceLanguage),
                    selected = settings.targetLanguage.isEmpty(),
                    onClick = { onChange(settings.copy(targetLanguage = "")) }
                )
                TranslationLanguages.forEach { tag ->
                    LanguageRow(
                        name = languageName(tag, deviceLocale),
                        selected = settings.targetLanguage == tag,
                        onClick = { onChange(settings.copy(targetLanguage = tag)) }
                    )
                }
            }
        }
    }
}

// Mode, word by word, alignment and size of the lines
@Composable
private fun DisplaySection(settings: LyricsSettings, onChange: (LyricsSettings) -> Unit) {
    Section(title = stringResource(R.string.lyrics_section_display)) {
        ChoiceRow(
            title = stringResource(R.string.lyrics_display_mode),
            options = LyricsDisplay.entries,
            selected = settings.display,
            label = { display ->
                stringResource(if (display == LyricsDisplay.Synced) R.string.lyrics_display_synced else R.string.lyrics_display_static)
            },
            onSelect = { onChange(settings.copy(display = it)) }
        )

        SwitchRow(
            title = stringResource(R.string.lyrics_word_by_word),
            hint = stringResource(R.string.lyrics_word_by_word_hint),
            checked = settings.wordByWord,
            enabled = settings.display == LyricsDisplay.Synced,
            onCheckedChange = { onChange(settings.copy(wordByWord = it)) }
        )

        ChoiceRow(
            title = stringResource(R.string.lyrics_alignment),
            options = LyricsAlignment.entries,
            selected = settings.alignment,
            label = { alignment ->
                stringResource(
                    when (alignment) {
                        LyricsAlignment.Start -> R.string.lyrics_align_start
                        LyricsAlignment.Center -> R.string.lyrics_align_center
                        LyricsAlignment.End -> R.string.lyrics_align_end
                    }
                )
            },
            onSelect = { onChange(settings.copy(alignment = it)) }
        )

        ChoiceRow(
            title = stringResource(R.string.lyrics_text_size),
            options = LyricsTextSize.entries,
            selected = settings.textSize,
            label = { size ->
                stringResource(
                    when (size) {
                        LyricsTextSize.Small -> R.string.lyrics_size_small
                        LyricsTextSize.Medium -> R.string.lyrics_size_medium
                        LyricsTextSize.Large -> R.string.lyrics_size_large
                    }
                )
            },
            onSelect = { onChange(settings.copy(textSize = it)) }
        )
    }
}

// The sources in the order they are tried, each one can be turned off or moved
@Composable
private fun SourcesSection(
    settings: LyricsSettings,
    onChange: (LyricsSettings) -> Unit,
    onSearchAgain: () -> Unit,
    onSearchManually: () -> Unit
) {
    Section(title = stringResource(R.string.lyrics_section_sources), hint = stringResource(R.string.lyrics_sources_hint)) {
        settings.sources.forEachIndexed { index, source ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = PlayerDimens.OptionsRowHeight)
            ) {
                Switch(
                    checked = source !in settings.disabledSources,
                    onCheckedChange = { on ->
                        val disabled = if (on) settings.disabledSources - source else settings.disabledSources + source
                        onChange(settings.copy(disabledSources = disabled))
                    }
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = PlayerDimens.OptionsRowGap)
                ) {
                    Text(text = stringResource(sourceName(source)), style = MaterialTheme.typography.bodyLarge)
                    Text(
                        text = stringResource(sourceHint(source)),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(
                    onClick = { onChange(settings.copy(sources = settings.sources.moved(index, index - 1))) },
                    enabled = index > 0,
                    modifier = Modifier.size(PlayerDimens.OptionsMoveButton)
                ) {
                    Icon(imageVector = WavvyIcons.ArrowUp, contentDescription = stringResource(R.string.lyrics_move_up))
                }
                IconButton(
                    onClick = { onChange(settings.copy(sources = settings.sources.moved(index, index + 1))) },
                    enabled = index < settings.sources.lastIndex,
                    modifier = Modifier.size(PlayerDimens.OptionsMoveButton)
                ) {
                    Icon(imageVector = WavvyIcons.ArrowDown, contentDescription = stringResource(R.string.lyrics_move_down))
                }
            }
        }

        Spacer(Modifier.height(PlayerDimens.OptionsRowGap))
        OutlinedButton(onClick = onSearchAgain, modifier = Modifier.fillMaxWidth()) {
            Text(text = stringResource(R.string.lyrics_search_again))
        }
        OutlinedButton(onClick = onSearchManually, modifier = Modifier.fillMaxWidth()) {
            Text(text = stringResource(R.string.lyrics_search_manual))
        }
    }
}

// Title of a group of options, with a short explanation when it needs one
@Composable
private fun Section(title: String, hint: String? = null, content: @Composable () -> Unit) {
    Column {
        Text(text = title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        hint?.let {
            Spacer(Modifier.height(PlayerDimens.OptionsTitleGap))
            Text(text = it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(PlayerDimens.OptionsRowGap))
        content()
    }
}

// Option with a switch at the end
@Composable
private fun SwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    hint: String? = null,
    enabled: Boolean = true
) {
    OptionRow(title = title, hint = hint, onClick = { if (enabled) onCheckedChange(!checked) }) {
        Switch(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled)
    }
}

// Row with a title, an optional explanation and something at the end
@Composable
private fun OptionRow(
    title: String,
    hint: String?,
    onClick: () -> Unit,
    end: @Composable () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = PlayerDimens.OptionsRowHeight)
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = PlayerDimens.OptionsRowGap)
        ) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            hint?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        end()
    }
}

// One of a few choices, as a row of joined buttons under its title
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> ChoiceRow(
    title: String,
    options: List<T>,
    selected: T,
    label: @Composable (T) -> String,
    onSelect: (T) -> Unit
) {
    Column(modifier = Modifier.padding(vertical = PlayerDimens.OptionsRowGap)) {
        Text(text = title, style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(PlayerDimens.OptionsRowGap))
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            options.forEachIndexed { index, option ->
                SegmentedButton(
                    selected = option == selected,
                    onClick = { onSelect(option) },
                    shape = SegmentedButtonDefaults.itemShape(index, options.size)
                ) {
                    Text(text = label(option), maxLines = 1)
                }
            }
        }
    }
}

// Language with a round mark when it is the chosen one
@Composable
private fun LanguageRow(name: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Text(text = name, style = MaterialTheme.typography.bodyLarge)
    }
}

// Name of a language in the language of the device, starting with a capital letter
private fun languageName(tag: String, deviceLocale: Locale): String =
    Locale.forLanguageTag(tag).getDisplayName(deviceLocale).replaceFirstChar { it.titlecase(deviceLocale) }

// Name of a source
internal fun sourceName(source: LyricsSource): Int = when (source) {
    LyricsSource.BetterLyrics -> R.string.lyrics_source_betterlyrics
    LyricsSource.LrcLib -> R.string.lyrics_source_lrclib
    LyricsSource.KuGou -> R.string.lyrics_source_kugou
    LyricsSource.YouTubeMusic -> R.string.lyrics_source_youtube_music
    LyricsSource.Paxsenix -> R.string.lyrics_source_paxsenix
    LyricsSource.LyricsPlus -> R.string.lyrics_source_lyricsplus
    LyricsSource.Zemer -> R.string.lyrics_source_zemer
    LyricsSource.YouTubeCaptions -> R.string.lyrics_source_youtube_captions
}

// What a source is good at
private fun sourceHint(source: LyricsSource): Int = when (source) {
    LyricsSource.BetterLyrics -> R.string.lyrics_source_betterlyrics_hint
    LyricsSource.LrcLib -> R.string.lyrics_source_lrclib_hint
    LyricsSource.KuGou -> R.string.lyrics_source_kugou_hint
    LyricsSource.YouTubeMusic -> R.string.lyrics_source_youtube_music_hint
    LyricsSource.Paxsenix -> R.string.lyrics_source_paxsenix_hint
    LyricsSource.LyricsPlus -> R.string.lyrics_source_lyricsplus_hint
    LyricsSource.Zemer -> R.string.lyrics_source_zemer_hint
    LyricsSource.YouTubeCaptions -> R.string.lyrics_source_youtube_captions_hint
}

// The list with one item moved to another place
private fun <T> List<T>.moved(from: Int, to: Int): List<T> {
    if (to !in indices) return this
    return toMutableList().apply { add(to, removeAt(from)) }
}

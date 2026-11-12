package com.wavvy.app.features.player.ui.components

// Compose animation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.Color
import com.wavvy.app.features.menu.CardMenuForm
import com.wavvy.app.features.menu.CardMenuSection
import com.wavvy.app.features.menu.ItemMenuDimens
import com.wavvy.app.features.menu.MenuCardDivider
import com.wavvy.app.features.menu.MenuCardGroup
import androidx.compose.ui.graphics.vector.ImageVector
import com.wavvy.app.core.designsystem.theme.WavvyTheme
import androidx.compose.animation.AnimatedVisibility
// Compose layouts and foundations
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
// Material 3 components
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
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
import java.text.Collator
import java.util.Locale
// Project resources
import com.wavvy.app.core.designsystem.components.WavvySwitch
import com.wavvy.app.R
import com.wavvy.app.core.designsystem.components.WavvySheet
import com.wavvy.app.core.designsystem.icons.WavvyIcons
import com.wavvy.app.core.designsystem.theme.DarkColors
import com.wavvy.app.core.lyrics.LyricsAlignment
import com.wavvy.app.core.lyrics.LyricsDisplay
import com.wavvy.app.core.lyrics.LyricsSettings
import com.wavvy.app.core.lyrics.LyricsSource
import com.wavvy.app.core.lyrics.LyricsTextSize

// Languages the lyrics can be translated into, besides the one of the device, which are all the ones of the app, as the codes the translator knows
// Brazilian Portuguese is pt, Norwegian is no for both of its writings, and Serbian is one, which the translator writes in Cyrillic
private val TranslationLanguages = listOf(
    "af", "sq", "ar", "az", "eu", "be", "bn", "bs", "bg", "ca", "zh-CN", "zh-TW", "hr", "cs", "nl", "en", "et", "tl", "fi", "fr", "gl", "de", "el", "gu", "haw",
    "he", "hi", "hu", "id", "it", "ja", "kn", "km", "ko", "lo", "lt", "ms", "ml", "mr", "my", "ne", "no", "or", "fa", "pl", "pt", "pt-PT", "pa", "ro", "ru", "sr",
    "sk", "sl", "es", "sw", "sv", "ta", "te", "th", "tr", "uk", "ur", "vi"
)

// The tag the name of a language is read from, where the code of the translator has no region of its own to tell the writings apart
private fun nameTag(code: String): String = when (code) {
    "pt" -> "pt-BR"
    "tl" -> "fil"
    else -> code
}

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
            CardMenuForm(title = stringResource(R.string.lyrics_options)) {
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

    // The languages in alphabetical order by the name the user reads, with the accents in their place
    val sortedLanguages = remember(deviceLocale) {
        val collator = Collator.getInstance(deviceLocale)
        TranslationLanguages.sortedWith(compareBy(collator) { languageName(it, deviceLocale) })
    }
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
        MenuCardDivider()

        OptionRow(
            title = stringResource(R.string.lyrics_translate_language),
            hint = chosen,
            onClick = { showLanguages = !showLanguages }
        ) {
            Icon(imageVector = if (showLanguages) WavvyIcons.ArrowUp else WavvyIcons.ArrowDown, contentDescription = null)
        }

        AnimatedVisibility(visible = showLanguages) {
            Column {
                MenuCardDivider()
                LanguageRow(
                    name = stringResource(R.string.lyrics_language_device, deviceLanguage),
                    selected = settings.targetLanguage.isEmpty(),
                    onClick = { onChange(settings.copy(targetLanguage = "")) }
                )
                sortedLanguages.forEach { tag ->
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
        MenuCardDivider()

        SwitchRow(
            title = stringResource(R.string.lyrics_word_by_word),
            hint = stringResource(R.string.lyrics_word_by_word_hint),
            checked = settings.wordByWord,
            enabled = settings.display == LyricsDisplay.Synced,
            onCheckedChange = { onChange(settings.copy(wordByWord = it)) }
        )
        MenuCardDivider()

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
        MenuCardDivider()

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
            if (index > 0) MenuCardDivider()
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = ItemMenuDimens.CardRowHorizontal, vertical = ItemMenuDimens.CardRowVertical)
            ) {
                WavvySwitch(
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
                    Text(text = stringResource(sourceName(source)), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium), color = MaterialTheme.colorScheme.onSurface)
                    Text(
                        text = stringResource(sourceHint(source)),
                        style = MaterialTheme.typography.bodySmall,
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

        MenuCardDivider()
        ActionRow(icon = WavvyIcons.Refresh, title = stringResource(R.string.lyrics_search_again), onClick = onSearchAgain)
        MenuCardDivider()
        ActionRow(icon = WavvyIcons.Search, title = stringResource(R.string.lyrics_search_manual), onClick = onSearchManually)
    }
}

// A row of a card that does something, such as asking for the lyrics again
@Composable
private fun ActionRow(icon: ImageVector, title: String, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = ItemMenuDimens.CardRowHorizontal, vertical = ItemMenuDimens.CardRowVertical)
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = WavvyTheme.colors.playerAccent, modifier = Modifier.size(ItemMenuDimens.CardRowIcon))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(start = ItemMenuDimens.CardRowTextStart)
        )
    }
}

// Title of a group of options, with a short explanation when it needs one, and the options in one card
@Composable
private fun Section(title: String, hint: String? = null, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(ItemMenuDimens.CardGap)) {
        CardMenuSection(title, color = WavvyTheme.colors.playerAccent)
        hint?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = ItemMenuDimens.CardRowHorizontal)
            )
        }
        MenuCardGroup { content() }
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
        WavvySwitch(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled)
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
            .clickable(onClick = onClick)
            .padding(horizontal = ItemMenuDimens.CardRowHorizontal, vertical = ItemMenuDimens.CardRowVertical)
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = PlayerDimens.OptionsRowGap)
        ) {
            Text(text = title, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium), color = MaterialTheme.colorScheme.onSurface)
            hint?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
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
    val accent = WavvyTheme.colors.playerAccent

    Column(modifier = Modifier.padding(horizontal = ItemMenuDimens.CardRowHorizontal, vertical = PlayerDimens.OptionsRowGap)) {
        Text(text = title, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium), color = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.height(PlayerDimens.OptionsRowGap))
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            options.forEachIndexed { index, option ->
                SegmentedButton(
                    selected = option == selected,
                    onClick = { onSelect(option) },
                    shape = SegmentedButtonDefaults.itemShape(index, options.size),
                    colors = SegmentedButtonDefaults.colors(
                        activeContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = PlayerDimens.OptionsSelectedAlpha),
                        activeContentColor = MaterialTheme.colorScheme.onSurface,
                        activeBorderColor = MaterialTheme.colorScheme.outlineVariant,
                        inactiveContainerColor = Color.Transparent,
                        inactiveContentColor = MaterialTheme.colorScheme.onSurface,
                        inactiveBorderColor = MaterialTheme.colorScheme.outlineVariant
                    )
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
            .padding(horizontal = ItemMenuDimens.CardRowHorizontal, vertical = ItemMenuDimens.CardRowVertical)
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Text(text = name, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
    }
}

// Name of a language in the language of the device, starting with a capital letter
private fun languageName(tag: String, deviceLocale: Locale): String =
    Locale.forLanguageTag(nameTag(tag)).getDisplayName(deviceLocale).replaceFirstChar { it.titlecase(deviceLocale) }

// Name of a source
internal fun sourceName(source: LyricsSource): Int = when (source) {
    LyricsSource.BetterLyrics -> R.string.lyrics_source_betterlyrics
    LyricsSource.LrcLib -> R.string.lyrics_source_lrclib
    LyricsSource.KuGou -> R.string.lyrics_source_kugou
    LyricsSource.YouTubeMusic -> R.string.lyrics_source_youtube_music
    LyricsSource.Paxsenix -> R.string.lyrics_source_paxsenix
    LyricsSource.LyricsPlus -> R.string.lyrics_source_lyricsplus
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
    LyricsSource.YouTubeCaptions -> R.string.lyrics_source_youtube_captions_hint
}

// The list with one item moved to another place
private fun <T> List<T>.moved(from: Int, to: Int): List<T> {
    if (to !in indices) return this
    return toMutableList().apply { add(to, removeAt(from)) }
}

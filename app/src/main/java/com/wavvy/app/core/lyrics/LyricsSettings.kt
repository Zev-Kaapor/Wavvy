package com.wavvy.app.core.lyrics

// Android context and DataStore
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
// Coroutines and reactive flows
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Places the lyrics can come from, in their default order, the ones that time word by word first, then the timed ones, then the plain ones
enum class LyricsSource { BetterLyrics, Paxsenix, LyricsPlus, LrcLib, KuGou, YouTubeMusic, Zemer, YouTubeCaptions }

// Timed lyrics follow the song, static ones are shown as plain text
enum class LyricsDisplay { Synced, Static }

// Where the lines sit across the screen
enum class LyricsAlignment { Start, Center, End }

// Size of the text of the lines
enum class LyricsTextSize { Small, Medium, Large }

// Everything the user can choose about the lyrics, an empty language means the language of the device
data class LyricsSettings(
    val translate: Boolean = false,
    val targetLanguage: String = "",
    val sources: List<LyricsSource> = LyricsSource.entries,
    val disabledSources: Set<LyricsSource> = emptySet(),
    val display: LyricsDisplay = LyricsDisplay.Synced,
    val wordByWord: Boolean = true,
    val alignment: LyricsAlignment = LyricsAlignment.Center,
    val textSize: LyricsTextSize = LyricsTextSize.Medium
) {
    // Sources that are on, in the order they are tried
    val activeSources: List<LyricsSource> get() = sources.filter { it !in disabledSources }
}

// DataStore file and keys of the lyrics settings, plain values so they can become a JSON file later
private val Context.lyricsDataStore: DataStore<Preferences> by preferencesDataStore(name = "lyrics_settings")
private val TranslateKey = booleanPreferencesKey("translate")
private val TargetLanguageKey = stringPreferencesKey("target_language")
private val SourcesKey = stringPreferencesKey("sources")
private val DisabledSourcesKey = stringPreferencesKey("disabled_sources")
private val DisplayKey = stringPreferencesKey("display")
private val WordByWordKey = booleanPreferencesKey("word_by_word")
private val AlignmentKey = stringPreferencesKey("alignment")
private val TextSizeKey = stringPreferencesKey("text_size")
private const val ListSeparator = ","

// Keeps the lyrics settings on the device
class LyricsSettingsStore(context: Context) {
    private val dataStore = context.applicationContext.lyricsDataStore

    val settings: Flow<LyricsSettings> = dataStore.data.map { preferences ->
        val defaults = LyricsSettings()
        // Sources added in a later version join at the end of a saved order
        val saved = preferences[SourcesKey]?.split(ListSeparator)?.mapNotNull(::sourceOf).orEmpty()
        LyricsSettings(
            translate = preferences[TranslateKey] ?: defaults.translate,
            targetLanguage = preferences[TargetLanguageKey] ?: defaults.targetLanguage,
            sources = saved + LyricsSource.entries.filter { it !in saved },
            disabledSources = preferences[DisabledSourcesKey]?.split(ListSeparator)?.mapNotNull(::sourceOf)?.toSet().orEmpty(),
            display = LyricsDisplay.entries.firstOrNull { it.name == preferences[DisplayKey] } ?: defaults.display,
            wordByWord = preferences[WordByWordKey] ?: defaults.wordByWord,
            alignment = LyricsAlignment.entries.firstOrNull { it.name == preferences[AlignmentKey] } ?: defaults.alignment,
            textSize = LyricsTextSize.entries.firstOrNull { it.name == preferences[TextSizeKey] } ?: defaults.textSize
        )
    }

    // Saves the whole settings at once
    suspend fun save(settings: LyricsSettings) {
        dataStore.edit { preferences ->
            preferences[TranslateKey] = settings.translate
            preferences[TargetLanguageKey] = settings.targetLanguage
            preferences[SourcesKey] = settings.sources.joinToString(ListSeparator) { it.name }
            preferences[DisabledSourcesKey] = settings.disabledSources.joinToString(ListSeparator) { it.name }
            preferences[DisplayKey] = settings.display.name
            preferences[WordByWordKey] = settings.wordByWord
            preferences[AlignmentKey] = settings.alignment.name
            preferences[TextSizeKey] = settings.textSize.name
        }
    }

    // Source of a saved name, none when the name is unknown
    private fun sourceOf(name: String): LyricsSource? = LyricsSource.entries.firstOrNull { it.name == name }
}

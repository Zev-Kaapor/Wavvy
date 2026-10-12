package com.wavvy.app.core.backup

// Android context and resources
import android.content.Context
import androidx.annotation.StringRes
// JSON
import org.json.JSONArray
import org.json.JSONObject
// Project resources
import com.wavvy.app.R
import com.wavvy.app.core.history.ChosenLyricsEntity
import com.wavvy.app.core.history.EventEntity
import com.wavvy.app.core.history.PinnedEntity
import com.wavvy.app.core.history.PlayHistory
import com.wavvy.app.core.history.SearchEntity
import com.wavvy.app.core.history.SongEntity
import com.wavvy.app.core.lyrics.LyricsSettingsStore
import com.wavvy.app.features.library.data.LibraryStore
import com.wavvy.app.features.settings.data.SettingsStore

// The parts of the app that the user can put in a backup, with the words that name each one and say what is in it
enum class BackupSection(@StringRes val titleRes: Int, @StringRes val subtitleRes: Int) {
    Settings(R.string.backup_section_settings, R.string.backup_section_settings_subtitle),
    Library(R.string.backup_section_library, R.string.backup_section_library_subtitle),
    History(R.string.backup_section_history, R.string.backup_section_history_subtitle),
    Pinned(R.string.backup_section_pinned, R.string.backup_section_pinned_subtitle),
    Lyrics(R.string.backup_section_lyrics, R.string.backup_section_lyrics_subtitle),
    Searches(R.string.backup_section_searches, R.string.backup_section_searches_subtitle)
}

// The names in the file, of the parts of the backup and of what is in the file besides them
private const val SectionsKey = "sections"
private const val AppKey = "app"
private const val AppName = "wavvy"
private const val CreatedKey = "createdAt"
private const val SettingsKey = "settings"
private const val LyricsKey = "lyrics"
private const val SongsKey = "songs"
private const val EventsKey = "events"

// Reads what each part of the backup keeps, and puts it back in the app, the parts that come in replace the ones that are there
object BackupData {
    // The parts that were asked for, as the JSON that goes into the file
    suspend fun collect(context: Context, sections: Set<BackupSection>): JSONObject {
        val parts = JSONObject()
        val history = PlayHistory.dao(context)

        sections.forEach { section ->
            parts.put(
                section.name,
                when (section) {
                    BackupSection.Settings -> JSONObject()
                        .put(SettingsKey, SettingsStore(context).backup())
                        .put(LyricsKey, LyricsSettingsStore(context).backup())
                    BackupSection.Library -> LibraryStore(context).backup()
                    BackupSection.History -> JSONObject()
                        .put(SongsKey, history.songsOnce().toJson { song ->
                            JSONObject().put("id", song.id).put("title", song.title).putOpt("artist", song.artist).putOpt("artworkUrl", song.artworkUrl)
                                .put("durationMs", song.durationMs).put("totalPlayTimeMs", song.totalPlayTimeMs).put("lastPlayedAt", song.lastPlayedAt)
                        })
                        .put(EventsKey, history.eventsOnce().toJson { event ->
                            JSONObject().put("id", event.id).put("songId", event.songId).put("timestamp", event.timestamp).put("playTimeMs", event.playTimeMs)
                        })
                    BackupSection.Pinned -> history.pinnedOnce().toJson { pinned ->
                        JSONObject().put("id", pinned.id).put("title", pinned.title).putOpt("artist", pinned.artist).putOpt("artworkUrl", pinned.artworkUrl)
                            .put("durationMs", pinned.durationMs).put("pinnedAt", pinned.pinnedAt).put("kind", pinned.kind)
                    }
                    BackupSection.Lyrics -> history.lyricsOnce().toJson { lyrics ->
                        JSONObject().put("videoId", lyrics.videoId).put("json", lyrics.json).put("chosenAt", lyrics.chosenAt)
                    }
                    BackupSection.Searches -> history.searchesOnce().toJson { search ->
                        JSONObject().put("query", search.query).put("searchedAt", search.searchedAt)
                    }
                }
            )
        }

        return JSONObject().put(AppKey, AppName).put(CreatedKey, System.currentTimeMillis()).put(SectionsKey, parts)
    }

    // The parts a file has and how many things each one has in it, none for a file that is not a backup of this app
    fun available(payload: JSONObject): Map<BackupSection, Int> {
        if (payload.optString(AppKey) != AppName) return emptyMap()
        val parts = payload.optJSONObject(SectionsKey) ?: return emptyMap()

        return BackupSection.entries.mapNotNull { section ->
            val part = parts.opt(section.name) ?: return@mapNotNull null
            section to when (section) {
                BackupSection.Settings -> (part as JSONObject).let { it.optJSONObject(SettingsKey).size() + it.optJSONObject(LyricsKey).size() }
                BackupSection.Library -> (part as JSONObject).length()
                BackupSection.History -> (part as JSONObject).optJSONArray(SongsKey)?.length() ?: 0
                else -> (part as JSONArray).length()
            }
        }.toMap()
    }

    // Puts the chosen parts of the file in the app, in place of what is there
    suspend fun restore(context: Context, payload: JSONObject, sections: Set<BackupSection>) {
        val parts = payload.optJSONObject(SectionsKey) ?: return
        val history = PlayHistory.dao(context)

        sections.forEach { section ->
            val part = parts.opt(section.name) ?: return@forEach
            when (section) {
                BackupSection.Settings -> (part as JSONObject).let {
                    SettingsStore(context).restore(it.optJSONObject(SettingsKey) ?: JSONObject())
                    LyricsSettingsStore(context).restore(it.optJSONObject(LyricsKey) ?: JSONObject())
                }
                BackupSection.Library -> LibraryStore(context).restore(part as JSONObject)
                BackupSection.History -> (part as JSONObject).let { json ->
                    history.replaceHistory(
                        json.optJSONArray(SongsKey).toList { item ->
                            SongEntity(
                                id = item.getString("id"), title = item.getString("title"), artist = item.optString("artist").ifEmpty { null },
                                artworkUrl = item.optString("artworkUrl").ifEmpty { null }, durationMs = item.optLong("durationMs"),
                                totalPlayTimeMs = item.optLong("totalPlayTimeMs"), lastPlayedAt = item.optLong("lastPlayedAt")
                            )
                        },
                        json.optJSONArray(EventsKey).toList { item ->
                            EventEntity(id = item.getLong("id"), songId = item.getString("songId"), timestamp = item.getLong("timestamp"), playTimeMs = item.optLong("playTimeMs"))
                        }
                    )
                }
                BackupSection.Pinned -> history.replacePinned(
                    (part as JSONArray).toList { item ->
                        PinnedEntity(
                            id = item.getString("id"), title = item.getString("title"), artist = item.optString("artist").ifEmpty { null },
                            artworkUrl = item.optString("artworkUrl").ifEmpty { null }, durationMs = item.optLong("durationMs"),
                            pinnedAt = item.optLong("pinnedAt"), kind = item.optString("kind", "Song")
                        )
                    }
                )
                BackupSection.Lyrics -> history.replaceChosenLyrics(
                    (part as JSONArray).toList { item -> ChosenLyricsEntity(item.getString("videoId"), item.getString("json"), item.optLong("chosenAt")) }
                )
                BackupSection.Searches -> history.replaceSearches(
                    (part as JSONArray).toList { item -> SearchEntity(item.getString("query"), item.optLong("searchedAt")) }
                )
            }
        }
    }

    // A list written as an array of objects, and an array read as a list
    private fun <T> List<T>.toJson(item: (T) -> JSONObject): JSONArray = JSONArray().also { array -> forEach { array.put(item(it)) } }

    private fun <T> JSONArray?.toList(item: (JSONObject) -> T): List<T> =
        if (this == null) emptyList() else (0 until length()).mapNotNull { optJSONObject(it)?.let(item) }

    private fun JSONObject?.size(): Int = this?.length() ?: 0
}

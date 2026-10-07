package com.wavvy.app.core.history

// Android context
import android.content.Context
// Room
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
// Media3 item
import androidx.media3.common.MediaItem
// Reactive flows
import kotlinx.coroutines.flow.Flow

// File of the history on the device
private const val DatabaseName = "history.db"

// Milliseconds in a day, for the period of the most listened songs
private const val MillisPerDay = 86_400_000L

// A song is forgotten when what it is listened to now is under this share of what it was listened to before
private const val ForgottenShare = 0.2

// A song counts as listened to after it plays this long with sound, so a song skipped at once stays out of the history
const val MinimumListenMillis = 3_000L

// What the user listened to, written by the playback service and read by the screens
object PlayHistory {
    @Volatile
    private var database: HistoryDatabase? = null

    // Version 2 added the pinned songs, what was listened to stays as it was
    private val addPinnedSongs = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS pinned_song (id TEXT NOT NULL, title TEXT NOT NULL, artist TEXT, artworkUrl TEXT, " +
                    "durationMs INTEGER NOT NULL, pinnedAt INTEGER NOT NULL, PRIMARY KEY(id))"
            )
        }
    }

    // Version 3 added the lyrics picked by hand
    private val addChosenLyrics = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("CREATE TABLE IF NOT EXISTS chosen_lyrics (videoId TEXT NOT NULL, json TEXT NOT NULL, chosenAt INTEGER NOT NULL, PRIMARY KEY(videoId))")
        }
    }

    // The database, opened the first time it is needed
    private fun dao(context: Context): HistoryDao =
        (database ?: synchronized(this) {
            database ?: Room.databaseBuilder(context.applicationContext, HistoryDatabase::class.java, DatabaseName)
                .addMigrations(addPinnedSongs, addChosenLyrics)
                .build()
                .also { database = it }
        }).dao()

    // Writes a listening of a song, empty details are never written, a song without a title cannot be shown
    suspend fun record(context: Context, item: MediaItem, playTimeMs: Long) {
        val metadata = item.mediaMetadata
        val title = metadata.title?.toString()?.takeIf { it.isNotBlank() } ?: return

        dao(context).record(
            song = SongEntity(
                id = item.mediaId,
                title = title,
                artist = metadata.artist?.toString(),
                artworkUrl = metadata.artworkUri?.toString(),
                durationMs = metadata.durationMs ?: 0L,
                totalPlayTimeMs = 0L,
                lastPlayedAt = 0L
            ),
            playTimeMs = playTimeMs,
            at = System.currentTimeMillis()
        )
    }

    // Adds the time a song kept playing after it was counted, to its total and to its last listening
    suspend fun addTime(context: Context, songId: String, playTimeMs: Long) {
        dao(context).addTime(songId, playTimeMs)
    }

    // The lyrics the user picked by hand for a video, as the text that was saved, empty when none was picked
    suspend fun chosenLyrics(context: Context, videoId: String): String? = dao(context).chosenLyrics(videoId)

    suspend fun saveChosenLyrics(context: Context, videoId: String, json: String) {
        dao(context).saveChosenLyrics(ChosenLyricsEntity(videoId, json, System.currentTimeMillis()))
    }

    suspend fun deleteChosenLyrics(context: Context, videoId: String) {
        dao(context).deleteChosenLyrics(videoId)
    }

    // Pins a song to the speed dial of the Home
    suspend fun pin(context: Context, id: String, title: String, artist: String?, artworkUrl: String?, durationMs: Long) {
        dao(context).pin(PinnedEntity(id, title, artist, artworkUrl, durationMs, System.currentTimeMillis()))
    }

    suspend fun unpin(context: Context, id: String) {
        dao(context).unpin(id)
    }

    // The pinned songs, the last one pinned first, updated whenever they change
    fun pinned(context: Context): Flow<List<PinnedEntity>> = dao(context).pinnedSongs()

    // The songs listened to, the most recent first, updated whenever the history changes
    fun recent(context: Context, limit: Int): Flow<List<SongEntity>> = dao(context).recentSongs(limit)

    // The songs that were listened to a lot before the last days and much less since, updated whenever the history changes
    fun forgotten(context: Context, days: Int, limit: Int): Flow<List<SongEntity>> =
        dao(context).forgottenFavorites(System.currentTimeMillis() - days * MillisPerDay, ForgottenShare, limit)

    // The songs listened to the longest in the last days, the most listened first, updated whenever the history changes
    fun mostPlayed(context: Context, days: Int, limit: Int): Flow<List<SongEntity>> =
        dao(context).mostPlayedSongs(System.currentTimeMillis() - days * MillisPerDay, limit)
}

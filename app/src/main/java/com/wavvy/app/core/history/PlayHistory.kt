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

    // Version 4 added the searches
    private val addSearches = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("CREATE TABLE IF NOT EXISTS search_history (`query` TEXT NOT NULL, searchedAt INTEGER NOT NULL, PRIMARY KEY(`query`))")
        }
    }

    // Version 5 added the releases of the artists the user follows
    private val addReleases = object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `release` (id TEXT NOT NULL, artistId TEXT NOT NULL, artistName TEXT NOT NULL, artistPhoto TEXT, " +
                    "title TEXT NOT NULL, coverUrl TEXT, kind TEXT, seenAt INTEGER NOT NULL, isAnnounced INTEGER NOT NULL, isRead INTEGER NOT NULL, PRIMARY KEY(id))"
            )
            db.execSQL("CREATE TABLE IF NOT EXISTS scanned_artist (id TEXT NOT NULL, scannedAt INTEGER NOT NULL, PRIMARY KEY(id))")
        }
    }

    // The database, opened the first time it is needed
    private fun dao(context: Context): HistoryDao =
        (database ?: synchronized(this) {
            database ?: Room.databaseBuilder(context.applicationContext, HistoryDatabase::class.java, DatabaseName)
                .addMigrations(addPinnedSongs, addChosenLyrics, addSearches, addReleases)
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

    // Keeps a search to be offered again
    suspend fun saveSearch(context: Context, query: String) {
        dao(context).saveSearch(SearchEntity(query, System.currentTimeMillis()))
    }

    suspend fun removeSearch(context: Context, query: String) {
        dao(context).removeSearch(query)
    }

    suspend fun clearSearches(context: Context) {
        dao(context).clearSearches()
    }

    // Keeps what the page of an artist lists, the new ones among them become news
    suspend fun saveReleases(context: Context, artistId: String, releases: List<ReleaseEntity>, scannedAt: Long): List<ReleaseEntity> =
        dao(context).addReleases(artistId, releases, scannedAt)

    // The artists listened to the most and the artists followed, as names, for the suggestions of the search
    suspend fun topArtistTexts(context: Context, limit: Int): List<String> = dao(context).topArtistTexts(limit)

    suspend fun followedArtistNames(context: Context): List<String> = dao(context).followedArtistNames()

    // The news about the artists the user follows, the newest first, and how many were not read
    fun announcedReleases(context: Context): Flow<List<ReleaseEntity>> = dao(context).announcedReleases()

    fun unreadReleases(context: Context): Flow<Int> = dao(context).unreadReleases()

    suspend fun markReleasesRead(context: Context) {
        dao(context).markReleasesRead()
    }

    suspend fun dismissRelease(context: Context, id: String) {
        dao(context).dismissRelease(id)
    }

    // The last searches, the most recent first, updated whenever they change
    fun searches(context: Context, limit: Int): Flow<List<String>> = dao(context).searches(limit)

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

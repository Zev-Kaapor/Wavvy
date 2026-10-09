package com.wavvy.app.core.history

// Room database
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Transaction
// Reactive flows
import kotlinx.coroutines.flow.Flow

// A song the user listened to, with when it was last played and for how long it was played in all
@Entity(tableName = "song")
data class SongEntity(
    @PrimaryKey val id: String,
    val title: String,
    val artist: String?,
    val artworkUrl: String?,
    val durationMs: Long,
    val totalPlayTimeMs: Long,
    val lastPlayedAt: Long
)

// One time a song was listened to, kept so the sections of the Home can look at a period
@Entity(
    tableName = "event",
    foreignKeys = [ForeignKey(entity = SongEntity::class, parentColumns = ["id"], childColumns = ["songId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("songId"), Index("timestamp")]
)
data class EventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val songId: String,
    val timestamp: Long,
    val playTimeMs: Long
)

// A song the user pinned to the speed dial of the Home, kept with its details because it may never have been listened to
@Entity(tableName = "pinned_song")
data class PinnedEntity(
    @PrimaryKey val id: String,
    val title: String,
    val artist: String?,
    val artworkUrl: String?,
    val durationMs: Long,
    val pinnedAt: Long,
    // What was pinned, a song or a podcast, by the name of its kind
    @ColumnInfo(defaultValue = "Song") val kind: String = "Song"
)

// The lyrics the user picked by hand for a song, kept as the text the lyrics code writes so the history knows nothing of its format
@Entity(tableName = "chosen_lyrics")
data class ChosenLyricsEntity(
    @PrimaryKey val videoId: String,
    val json: String,
    val chosenAt: Long
)

// A search the user made, kept to be offered again, a search made again only moves to the front
@Entity(tableName = "search_history")
data class SearchEntity(
    @PrimaryKey val query: String,
    val searchedAt: Long
)

// A release of an artist the user follows, seen in the page of the artist, the ones found after the first look at the artist are the notifications
@Entity(tableName = "release")
data class ReleaseEntity(
    // The page of the album, which is also what opens it
    @PrimaryKey val id: String,
    val artistId: String,
    val artistName: String,
    val artistPhoto: String?,
    val title: String,
    val coverUrl: String?,
    // The word YouTube Music uses for the kind, such as Single or EP
    val kind: String?,
    val seenAt: Long,
    // Listed in the Activity, the ones that were already out when the artist was first looked at are not
    val isAnnounced: Boolean,
    val isRead: Boolean
)

// An artist whose releases were already looked at once, so its next releases are news
@Entity(tableName = "scanned_artist")
data class ScannedArtistEntity(
    @PrimaryKey val id: String,
    val scannedAt: Long
)

// Everything the app asks of the history
@Dao
abstract class HistoryDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    protected abstract suspend fun insertSong(song: SongEntity)

    // Keeps the details of the song up to date, since YouTube may change them, and adds the time listened
    @Query(
        "UPDATE song SET title = :title, artist = :artist, artworkUrl = :artworkUrl, durationMs = :durationMs, " +
            "totalPlayTimeMs = totalPlayTimeMs + :playTimeMs, lastPlayedAt = :at WHERE id = :id"
    )
    protected abstract suspend fun updateSong(
        id: String,
        title: String,
        artist: String?,
        artworkUrl: String?,
        durationMs: Long,
        playTimeMs: Long,
        at: Long
    )

    @Insert
    protected abstract suspend fun insertEvent(event: EventEntity)

    // Writes the song and the time it was listened to as one change, so a failure never leaves one without the other
    @Transaction
    open suspend fun record(song: SongEntity, playTimeMs: Long, at: Long) {
        insertSong(song.copy(totalPlayTimeMs = 0L, lastPlayedAt = at))
        updateSong(song.id, song.title, song.artist, song.artworkUrl, song.durationMs, playTimeMs, at)
        insertEvent(EventEntity(songId = song.id, timestamp = at, playTimeMs = playTimeMs))
    }

    @Query("UPDATE song SET totalPlayTimeMs = totalPlayTimeMs + :playTimeMs WHERE id = :id")
    protected abstract suspend fun addToSong(id: String, playTimeMs: Long)

    @Query("UPDATE event SET playTimeMs = playTimeMs + :playTimeMs WHERE id = (SELECT MAX(id) FROM event WHERE songId = :id)")
    protected abstract suspend fun addToLastEvent(id: String, playTimeMs: Long)

    // Adds the time a song kept playing after it was written, to the song and to its last listening
    @Transaction
    open suspend fun addTime(id: String, playTimeMs: Long) {
        addToSong(id, playTimeMs)
        addToLastEvent(id, playTimeMs)
    }

    // Keeps a search, making it again only changes when it was made
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun saveSearch(search: SearchEntity)

    @Query("DELETE FROM search_history WHERE `query` = :query")
    abstract suspend fun removeSearch(query: String)

    @Query("DELETE FROM search_history")
    abstract suspend fun clearSearches()

    // The last searches, the most recent first
    @Query("SELECT `query` FROM search_history ORDER BY searchedAt DESC LIMIT :limit")
    abstract fun searches(limit: Int): Flow<List<String>>

    // Keeps the lyrics picked for a song, picking again replaces them
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun saveChosenLyrics(lyrics: ChosenLyricsEntity)

    @Query("SELECT json FROM chosen_lyrics WHERE videoId = :videoId")
    abstract suspend fun chosenLyrics(videoId: String): String?

    @Query("DELETE FROM chosen_lyrics WHERE videoId = :videoId")
    abstract suspend fun deleteChosenLyrics(videoId: String)

    // Pins a song, pinning it again only moves it to the front
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun pin(pinned: PinnedEntity)

    @Query("DELETE FROM pinned_song WHERE id = :id")
    abstract suspend fun unpin(id: String)

    // The pinned songs, the last one pinned first
    @Query("SELECT * FROM pinned_song ORDER BY pinnedAt DESC")
    abstract fun pinnedSongs(): Flow<List<PinnedEntity>>

    // The songs listened to, the most recent first
    @Query("SELECT * FROM song ORDER BY lastPlayedAt DESC LIMIT :limit")
    abstract fun recentSongs(limit: Int): Flow<List<SongEntity>>

    // Songs listened to a lot before a moment and much less since then, the ones listened to the longest first, adapted from Metrolist (GPL-3.0)
    // A song not listened to at all since the moment counts as listened to for no time, so it is the most forgotten
    @Query(
        "SELECT song.* FROM song JOIN (" +
            "SELECT songId, SUM(playTimeMs) AS oldTime FROM event WHERE timestamp < :before GROUP BY songId" +
            ") AS past ON song.id = past.songId LEFT JOIN (" +
            "SELECT songId, SUM(playTimeMs) AS newTime FROM event WHERE timestamp >= :before GROUP BY songId" +
            ") AS since ON song.id = since.songId " +
            "WHERE :share * past.oldTime > COALESCE(since.newTime, 0) ORDER BY past.oldTime DESC LIMIT :limit"
    )
    abstract fun forgottenFavorites(before: Long, share: Double, limit: Int): Flow<List<SongEntity>>

    // The songs listened to the longest since a moment, the most listened first
    @Query(
        "SELECT song.* FROM song JOIN (" +
            "SELECT songId, SUM(playTimeMs) AS total FROM event WHERE timestamp > :since GROUP BY songId ORDER BY total DESC LIMIT :limit" +
            ") AS top ON song.id = top.songId ORDER BY top.total DESC"
    )
    abstract fun mostPlayedSongs(since: Long, limit: Int): Flow<List<SongEntity>>

    // The artists of the songs listened to the longest, as the texts of the songs write them
    @Query("SELECT artist FROM song WHERE artist IS NOT NULL ORDER BY totalPlayTimeMs DESC LIMIT :limit")
    abstract suspend fun topArtistTexts(limit: Int): List<String>

    // The names of the artists whose releases are watched
    @Query("SELECT DISTINCT artistName FROM release")
    abstract suspend fun followedArtistNames(): List<String>

    @Query("SELECT id FROM release WHERE artistId = :artistId")
    protected abstract suspend fun knownReleaseIds(artistId: String): List<String>

    @Query("SELECT title FROM release WHERE artistId = :artistId")
    protected abstract suspend fun knownReleaseTitles(artistId: String): List<String>

    @Query("SELECT COUNT(*) FROM scanned_artist WHERE id = :artistId")
    protected abstract suspend fun scannedCount(artistId: String): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    protected abstract suspend fun insertReleases(releases: List<ReleaseEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun markScanned(scanned: ScannedArtistEntity)

    // Keeps the releases of an artist that are not known yet, they are news unless it is the first look at the artist or the release is old, and gives back the news
    @Transaction
    open suspend fun addReleases(artistId: String, releases: List<ReleaseEntity>, scannedAt: Long): List<ReleaseEntity> {
        val isFirstLook = scannedCount(artistId) == 0
        val known = knownReleaseIds(artistId).toSet()
        // YouTube gives an old release a new id now and then, so one with the title of a known release is the same release
        val knownTitles = knownReleaseTitles(artistId).map { it.trim().lowercase() }.toSet()

        val fresh = releases.filter { it.id !in known && it.title.trim().lowercase() !in knownTitles }.map { it.copy(isAnnounced = it.isAnnounced && !isFirstLook, isRead = isFirstLook || !it.isAnnounced) }
        insertReleases(fresh)
        if (isFirstLook) markScanned(ScannedArtistEntity(artistId, scannedAt))
        return fresh.filter { it.isAnnounced }
    }

    // The news about artists, the newest first
    @Query("SELECT * FROM release WHERE isAnnounced = 1 ORDER BY seenAt DESC")
    abstract fun announcedReleases(): Flow<List<ReleaseEntity>>

    @Query("SELECT COUNT(*) FROM release WHERE isAnnounced = 1 AND isRead = 0")
    abstract fun unreadReleases(): Flow<Int>

    @Query("UPDATE release SET isRead = 1 WHERE isAnnounced = 1 AND isRead = 0")
    abstract suspend fun markReleasesRead()

    // Takes a release out of the news, it stays known so it is never found as new again
    @Query("UPDATE release SET isAnnounced = 0 WHERE id = :id")
    abstract suspend fun dismissRelease(id: String)
}

// The local history of what was listened to, kept on the device
@Database(
    entities = [
        SongEntity::class, EventEntity::class, PinnedEntity::class, ChosenLyricsEntity::class, SearchEntity::class,
        ReleaseEntity::class, ScannedArtistEntity::class
    ],
    version = 6,
    exportSchema = false
)
abstract class HistoryDatabase : RoomDatabase() {
    abstract fun dao(): HistoryDao
}

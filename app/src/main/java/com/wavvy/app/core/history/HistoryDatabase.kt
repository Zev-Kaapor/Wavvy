package com.wavvy.app.core.history

// Room database
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
    val pinnedAt: Long
)

// The lyrics the user picked by hand for a song, kept as the text the lyrics code writes so the history knows nothing of its format
@Entity(tableName = "chosen_lyrics")
data class ChosenLyricsEntity(
    @PrimaryKey val videoId: String,
    val json: String,
    val chosenAt: Long
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
}

// The local history of what was listened to, kept on the device
@Database(
    entities = [SongEntity::class, EventEntity::class, PinnedEntity::class, ChosenLyricsEntity::class],
    version = 3,
    exportSchema = false
)
abstract class HistoryDatabase : RoomDatabase() {
    abstract fun dao(): HistoryDao
}

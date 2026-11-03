package com.wavvy.app.core.history

// Room database
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Embedded
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

// A song the user listened to, with when it was last played and for how long it was played in all, the time is a guess for a song that came from the history of the account
@Entity(tableName = "song")
data class SongEntity(
    @PrimaryKey val id: String,
    val title: String,
    val artist: String?,
    val artworkUrl: String?,
    val durationMs: Long,
    val totalPlayTimeMs: Long,
    val lastPlayedAt: Long,
    @ColumnInfo(defaultValue = "0") val isEstimated: Boolean = false,
    // The album and the genres of the artist, genres written one after the other with a bar between them, filled in after the song was listened to
    val albumId: String? = null,
    val albumName: String? = null,
    val genres: String? = null,
    // When the album and the genres were looked for, zero while they were not
    @ColumnInfo(defaultValue = "0") val detailsAt: Long = 0L,
    // The year the album came out, zero when the album does not say it and nothing while it was not looked for
    val releaseYear: Int? = null,
    // A song the user took out of the retrospective, such as a sound to sleep to
    @ColumnInfo(defaultValue = "0") val isExcluded: Boolean = false
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
    val playTimeMs: Long,
    // A play of a song that came right after many plays of the same song, which is a loop left on and not something that was chosen
    @ColumnInfo(defaultValue = "0") val isLoop: Boolean = false
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
    val isRead: Boolean,
    // The year the page of the artist says it came out, nothing when it does not say
    val releaseYear: Int? = null
)

// An artist whose releases were already looked at once, so its next releases are news
@Entity(tableName = "scanned_artist")
data class ScannedArtistEntity(
    @PrimaryKey val id: String,
    val scannedAt: Long
)

// What was listened to in a period, for how long, how many times and how many different songs
data class ListenStats(val totalMs: Long, val listens: Int, val songs: Int)

// A listening with the artist text of its song, which the retrospective joins by the first artist
data class ArtistListen(val artist: String, val timestamp: Long, val playTimeMs: Long)

// A listening with the song it was of, which the retrospective joins by the song
data class SongListen(val songId: String, val timestamp: Long, val playTimeMs: Long)

// When a song was last played, to place the songs of the history of the account among the ones of the device
data class PlayedTime(val id: String, val lastPlayedAt: Long)

// When a listening started and how long it lasted
data class ListenTime(val timestamp: Long, val playTimeMs: Long)

// A song with the time it was listened to in a period and how many times
data class TopSong(@Embedded val song: SongEntity, val total: Long, val plays: Int)

// What was listened to of an artist, as the songs write the name, in a period, with the cover of one of its songs
data class ArtistTotal(val artist: String, val total: Long, val plays: Int, val cover: String?)

// How many plays of the same song in a row come before the next ones are taken for a loop
private const val LoopRun = 6

// A listening counts as a play only when the song was heard for at least a fifth of its length, which a seek or a skip does not add to
private const val CountedPlay = "CASE WHEN song.durationMs > 0 AND event.playTimeMs * 100 >= song.durationMs * 20 THEN 1 ELSE 0 END"

// Everything the app asks of the history
@Dao
abstract class HistoryDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    protected abstract suspend fun insertSong(song: SongEntity)

    // Keeps the details of the song up to date, since YouTube may change them, and adds the time listened, its time is a real one from now on
    @Query(
        "UPDATE song SET title = :title, artist = :artist, artworkUrl = :artworkUrl, durationMs = :durationMs, " +
            "totalPlayTimeMs = totalPlayTimeMs + :playTimeMs, lastPlayedAt = :at, isEstimated = 0 WHERE id = :id"
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
        // The plays right before this one, when they are all of this same song the new one is part of a loop
        val before = recentSongIds(LoopRun)
        val isLoop = before.size == LoopRun && before.all { it == song.id }
        insertEvent(EventEntity(songId = song.id, timestamp = at, playTimeMs = playTimeMs, isLoop = isLoop))
    }

    // The songs of the latest plays, the newest first
    @Query("SELECT songId FROM event ORDER BY id DESC LIMIT :limit")
    protected abstract suspend fun recentSongIds(limit: Int): List<String>

    @Query("SELECT * FROM song WHERE id = :id")
    abstract suspend fun song(id: String): SongEntity?

    // The songs listened to most recently whose album and genres were not looked for yet
    @Query("SELECT id FROM song WHERE detailsAt = 0 ORDER BY lastPlayedAt DESC LIMIT :limit")
    abstract suspend fun songsWithoutDetails(limit: Int): List<String>

    // The genres already known for an artist, from another song of the same artist
    @Query("SELECT genres FROM song WHERE artist = :artist AND detailsAt != 0 AND genres IS NOT NULL LIMIT 1")
    abstract suspend fun genresOfArtist(artist: String): String?

    // The songs of an album that is known whose year was not looked for yet, the most recent first
    @Query("SELECT DISTINCT albumId FROM song WHERE albumId IS NOT NULL AND albumId != '' AND releaseYear IS NULL ORDER BY lastPlayedAt DESC LIMIT :limit")
    abstract suspend fun albumsWithoutYear(limit: Int): List<String>

    // Every song of an album gets its year, zero says the album does not tell it
    @Query("UPDATE song SET releaseYear = :year WHERE albumId = :albumId AND releaseYear IS NULL")
    abstract suspend fun saveYear(albumId: String, year: Int)

    // Takes a song out of the retrospective, or puts it back
    @Query("UPDATE song SET isExcluded = :isExcluded WHERE id = :id")
    abstract suspend fun setExcluded(id: String, isExcluded: Boolean)

    @Query("UPDATE song SET albumId = :albumId, albumName = :albumName, genres = :genres, detailsAt = :at WHERE id = :id")
    abstract suspend fun saveDetails(id: String, albumId: String?, albumName: String?, genres: String?, at: Long)

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
    // Everything that was listened to, the most recent first
    @Query("SELECT * FROM song ORDER BY lastPlayedAt DESC")
    abstract fun allSongs(): Flow<List<SongEntity>>

    // When each song was last played
    @Query("SELECT id, lastPlayedAt FROM song")
    abstract suspend fun playedTimes(): List<PlayedTime>

    // Keeps the songs found in the history of the account that are not here yet
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun addFound(songs: List<SongEntity>)

    // Takes a song out of the history, with the times it was listened to
    @Query("DELETE FROM song WHERE id = :id")
    abstract suspend fun deleteSong(id: String)

    // Takes everything out of the history
    @Query("DELETE FROM song")
    abstract suspend fun clearSongs()

    // The time, the listenings and the songs of a period, from the moment it starts until now
    @Query(
        "SELECT COALESCE(SUM(event.playTimeMs), 0) AS totalMs, COALESCE(SUM($CountedPlay), 0) AS listens, " +
            "COUNT(DISTINCT CASE WHEN $CountedPlay = 1 THEN event.songId END) AS songs FROM event JOIN song ON song.id = event.songId WHERE event.timestamp >= :since"
    )
    abstract suspend fun listenStats(since: Long): ListenStats

    // The latest listenings, the newest first, with the time each one lasted
    @Query("SELECT timestamp, playTimeMs FROM event ORDER BY timestamp DESC LIMIT :limit")
    abstract suspend fun recentListens(limit: Int): List<ListenTime>

    // What the retrospective counts: not the songs taken out of it, and not the ones longer than the limit, which is how mixes and sounds to sleep to are left out
    // Every listening of a period with the time each one lasted, which the retrospective sorts by month and by day
    @Query(
        "SELECT event.timestamp AS timestamp, event.playTimeMs AS playTimeMs FROM event JOIN song ON song.id = event.songId " +
            "WHERE event.timestamp >= :since AND event.timestamp < :until AND song.isExcluded = 0 AND song.durationMs <= :maxDurationMs AND (event.isLoop = 0 OR :keepLoops = 1)"
    )
    abstract suspend fun listensBetween(since: Long, until: Long, maxDurationMs: Long, keepLoops: Boolean): List<ListenTime>

    // The listenings of a period with the artist of each song, for the months of each artist
    @Query(
        "SELECT song.artist AS artist, event.timestamp AS timestamp, event.playTimeMs AS playTimeMs FROM event JOIN song ON song.id = event.songId " +
            "WHERE event.timestamp >= :since AND event.timestamp < :until AND song.artist IS NOT NULL AND song.artist != '' AND song.isExcluded = 0 AND song.durationMs <= :maxDurationMs AND (event.isLoop = 0 OR :keepLoops = 1)"
    )
    abstract suspend fun artistListensBetween(since: Long, until: Long, maxDurationMs: Long, keepLoops: Boolean): List<ArtistListen>

    // The listenings of a period with the song of each one, for the months of each song
    @Query(
        "SELECT event.songId AS songId, event.timestamp AS timestamp, event.playTimeMs AS playTimeMs FROM event JOIN song ON song.id = event.songId " +
            "WHERE event.timestamp >= :since AND event.timestamp < :until AND song.isExcluded = 0 AND song.durationMs <= :maxDurationMs AND (event.isLoop = 0 OR :keepLoops = 1)"
    )
    abstract suspend fun songListensBetween(since: Long, until: Long, maxDurationMs: Long, keepLoops: Boolean): List<SongListen>

    // The songs of the retrospective by the times listened, the ones counted only
    @Query(
        "SELECT song.*, SUM(event.playTimeMs) AS total, SUM($CountedPlay) AS plays FROM event JOIN song ON song.id = event.songId " +
            "WHERE event.timestamp >= :since AND event.timestamp < :until AND song.isExcluded = 0 AND song.durationMs <= :maxDurationMs AND (event.isLoop = 0 OR :keepLoops = 1) " +
            "GROUP BY song.id HAVING plays > 0 ORDER BY plays DESC, total DESC LIMIT :limit"
    )
    abstract suspend fun recapSongs(since: Long, until: Long, maxDurationMs: Long, keepLoops: Boolean, limit: Int): List<TopSong>

    // The time listened to each artist text for the retrospective, the ones counted only
    @Query(
        "SELECT song.artist AS artist, SUM(event.playTimeMs) AS total, SUM($CountedPlay) AS plays, MAX(song.artworkUrl) AS cover FROM event " +
            "JOIN song ON song.id = event.songId WHERE event.timestamp >= :since AND event.timestamp < :until AND song.artist IS NOT NULL AND song.artist != '' " +
            "AND song.isExcluded = 0 AND song.durationMs <= :maxDurationMs AND (event.isLoop = 0 OR :keepLoops = 1) GROUP BY song.artist HAVING plays > 0"
    )
    abstract suspend fun recapArtists(since: Long, until: Long, maxDurationMs: Long, keepLoops: Boolean): List<ArtistTotal>

    // When the first listening was written, nothing before it can be told about
    @Query("SELECT MIN(timestamp) FROM event")
    abstract suspend fun firstListenAt(): Long?

    // The songs listened to the most times in a period, the time listened breaks a tie, from the moment it starts until the moment it ends
    @Query(
        "SELECT song.*, SUM(event.playTimeMs) AS total, SUM($CountedPlay) AS plays FROM event JOIN song ON song.id = event.songId " +
            "WHERE event.timestamp >= :since AND event.timestamp < :until GROUP BY song.id HAVING plays > 0 ORDER BY plays DESC, total DESC LIMIT :limit"
    )
    abstract suspend fun topSongs(since: Long, until: Long, limit: Int): List<TopSong>

    // The time listened to each artist text in a period, which the app joins by the first artist
    @Query(
        "SELECT song.artist AS artist, SUM(event.playTimeMs) AS total, SUM($CountedPlay) AS plays, MAX(song.artworkUrl) AS cover FROM event " +
            "JOIN song ON song.id = event.songId WHERE event.timestamp >= :since AND event.timestamp < :until AND song.artist IS NOT NULL AND song.artist != '' GROUP BY song.artist HAVING plays > 0"
    )
    abstract suspend fun artistTotals(since: Long, until: Long): List<ArtistTotal>

    // What the backup reads, everything of one kind at once
    @Query("SELECT * FROM song")
    abstract suspend fun songsOnce(): List<SongEntity>

    @Query("SELECT * FROM event")
    abstract suspend fun eventsOnce(): List<EventEntity>

    @Query("SELECT * FROM pinned_song")
    abstract suspend fun pinnedOnce(): List<PinnedEntity>

    @Query("SELECT * FROM chosen_lyrics")
    abstract suspend fun lyricsOnce(): List<ChosenLyricsEntity>

    @Query("SELECT * FROM search_history")
    abstract suspend fun searchesOnce(): List<SearchEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun putSongs(songs: List<SongEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun putEvents(events: List<EventEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun putPinned(pinned: List<PinnedEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun putLyrics(lyrics: List<ChosenLyricsEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun putSearches(searches: List<SearchEntity>)

    @Query("DELETE FROM pinned_song")
    protected abstract suspend fun clearPinned()

    @Query("DELETE FROM chosen_lyrics")
    protected abstract suspend fun clearLyrics()

    // What the backup puts back, each as one change, the songs first because the times listened to belong to them
    @Transaction
    open suspend fun replaceHistory(songs: List<SongEntity>, events: List<EventEntity>) {
        clearSongs()
        putSongs(songs)
        putEvents(events)
    }

    @Transaction
    open suspend fun replacePinned(pinned: List<PinnedEntity>) {
        clearPinned()
        putPinned(pinned)
    }

    @Transaction
    open suspend fun replaceChosenLyrics(lyrics: List<ChosenLyricsEntity>) {
        clearLyrics()
        putLyrics(lyrics)
    }

    @Transaction
    open suspend fun replaceSearches(searches: List<SearchEntity>) {
        clearSearches()
        putSearches(searches)
    }

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
        // The releases already known get their year, which they were kept without
        releases.forEach { release -> release.releaseYear?.let { setReleaseYear(release.id, it) } }
        if (isFirstLook) markScanned(ScannedArtistEntity(artistId, scannedAt))
        return fresh.filter { it.isAnnounced }
    }

    @Query("UPDATE release SET releaseYear = :year WHERE id = :id AND releaseYear IS NULL")
    protected abstract suspend fun setReleaseYear(id: String, year: Int)

    // The releases of a year by artists the user follows that were never listened to, by the page of the album or by its name, the newest seen first
    @Query(
        "SELECT * FROM release WHERE releaseYear = :year AND id NOT IN (SELECT albumId FROM song WHERE albumId IS NOT NULL AND albumId != '') " +
            "AND lower(trim(title)) NOT IN (SELECT lower(trim(albumName)) FROM song WHERE albumName IS NOT NULL AND albumName != '') ORDER BY seenAt DESC LIMIT :limit"
    )
    abstract suspend fun missedReleases(year: Int, limit: Int): List<ReleaseEntity>

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
    version = 10,
    exportSchema = false
)
abstract class HistoryDatabase : RoomDatabase() {
    abstract fun dao(): HistoryDao
}

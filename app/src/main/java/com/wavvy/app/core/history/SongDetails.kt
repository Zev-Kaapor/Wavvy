package com.wavvy.app.core.history

// Android context
import android.content.Context
// Project resources
import com.wavvy.app.core.playback.RadioQueue
import com.wavvy.app.features.artist.data.MusicBrainz
import com.wavvy.app.features.collection.data.CollectionKind
import com.wavvy.app.features.collection.data.CollectionRepository
// Coroutines
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex

// How many genres of an artist are kept for its songs
private const val MaxGenres = 3

// How many older songs without details are looked for each time a song is listened to, so the history fills in little by little
private const val BackfillBatch = 3

// Separates the genres written in one column
private const val GenreSeparator = "|"

// How many albums get their year looked for each time, and what a year looks like in the line of an album
private const val YearBatch = 2
private val YearPattern = Regex("""\b(19|20)\d{2}\b""")

// Looks for the album and the genres of the songs that were listened to, which the retrospective of the year is made from
object SongDetails {
    private val musicBrainz = MusicBrainz()
    private val running = Mutex()

    // The search goes on by its own, so changing or pausing the song does not cancel it halfway
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Fills in the song that was just listened to and a few older ones, a failure leaves the song to be tried again next time
    fun fill(context: Context, songId: String) {
        scope.launch {
            if (!running.tryLock()) return@launch

            try {
                val dao = PlayHistory.dao(context)
                val ids = (listOf(songId) + dao.songsWithoutDetails(BackfillBatch)).distinct()
                ids.forEach { fillOne(context, it) }
                dao.albumsWithoutYear(YearBatch).forEach { fillYear(context, it) }
            } finally {
                running.unlock()
            }
        }
    }

    // The year of an album, written in the line under its name on its page, every song of the album takes it, an album that does not say it is marked with zero
    private suspend fun fillYear(context: Context, albumId: String) {
        val page = CollectionRepository(context).load(CollectionKind.Album, albumId).getOrNull() ?: return
        val year = page.subtitle?.let { YearPattern.find(it)?.value?.toIntOrNull() } ?: 0

        PlayHistory.dao(context).saveYear(albumId, year)
    }

    // The album comes from the entry of the song, the genres from the first artist, which another song of the same artist may have already
    private suspend fun fillOne(context: Context, songId: String) {
        val dao = PlayHistory.dao(context)
        val song = dao.song(songId)?.takeIf { it.detailsAt == 0L } ?: return
        val links = RadioQueue.links(context, songId).getOrNull() ?: return

        // A video someone uploaded has the channel as its artist, which has no genres nor album
        val artist = links.artists.firstOrNull()?.takeUnless { links.isUpload }
        val known = song.artist?.let { dao.genresOfArtist(it) }
        val genres = known ?: artist?.let { (name, id) ->
            val profile = musicBrainz.profile(listOf(id).filter { it.isNotEmpty() }, name)
            if (profile.isFailure) return
            profile.getOrNull()?.genres.orEmpty().take(MaxGenres).joinToString(GenreSeparator)
        }.orEmpty()

        dao.saveDetails(songId, links.albumId, links.albumName, genres, System.currentTimeMillis())
    }
}

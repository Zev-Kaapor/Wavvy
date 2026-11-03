package com.wavvy.app.features.recap.data

// Android context
import android.content.Context
import androidx.annotation.StringRes
// Time
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
// Coroutines
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
// Project resources
import com.wavvy.app.R
import com.wavvy.app.core.history.PlayHistory
import com.wavvy.app.core.history.TopSong
import com.wavvy.app.features.profile.ui.ProfileRanking
import com.wavvy.app.features.profile.ui.TopArtist

// Separates the genres written in one column of a song
private const val GenreSeparator = "|"

// How many months a year has, and the hours of a day
private const val MonthsInYear = 12
private const val HoursInDay = 24

// How many artists get their months worked out, how many releases that were missed are told, and how many times a hundred makes a whole
private const val ArtistsWithMonths = 5
private const val SongsWithMonths = 5
private const val AlbumsWithMonths = 5
private const val MissedReleases = 6
private const val PercentOf = 100L

// How many songs must tell their year before the age of the music is told, and the age the taste of a person is said to form
private const val MinimumDatedSongs = 8
private const val FormativeAge = 14

// Where what the retrospective counts is chosen, and how long a track can be before it is taken for a mix or a sound to sleep to
private const val FilterPreferences = "recap_filter"
private const val SkipLongKey = "skip_long"
private const val LongTrackMs = 20L * 60 * 1000

private const val SkipLoopsKey = "skip_loops"

// What the retrospective leaves out, by length and by loops, which the user can turn off
object RecapFilter {
    fun skipsLong(context: Context): Boolean = context.applicationContext.getSharedPreferences(FilterPreferences, Context.MODE_PRIVATE).getBoolean(SkipLongKey, true)

    fun setSkipsLong(context: Context, isOn: Boolean) {
        context.applicationContext.getSharedPreferences(FilterPreferences, Context.MODE_PRIVATE).edit().putBoolean(SkipLongKey, isOn).apply()
    }

    // The plays of a song that came right after many of the same song, a loop left on, are left out
    fun skipsLoops(context: Context): Boolean = context.applicationContext.getSharedPreferences(FilterPreferences, Context.MODE_PRIVATE).getBoolean(SkipLoopsKey, true)

    fun setSkipsLoops(context: Context, isOn: Boolean) {
        context.applicationContext.getSharedPreferences(FilterPreferences, Context.MODE_PRIVATE).edit().putBoolean(SkipLoopsKey, isOn).apply()
    }

    // The longest a track can be to count, no limit at all when the filter is off
    fun maxDurationMs(context: Context): Long = if (skipsLong(context)) LongTrackMs else Long.MAX_VALUE
}

// A genre and the time the user listened to songs of it, a song with more than one genre counts for each of them
data class RecapGenre(val name: String, val totalMs: Long)

// An album and what was listened to of it, with the cover of its most listened song
data class RecapAlbum(val id: String, val name: String, val cover: String?, val totalMs: Long, val plays: Int, val artist: String? = null)

// The part of the day the user listened the most in
enum class DayPart { Night, Morning, Afternoon, Evening }

// A stretch of the year the retrospective can be about, from its first month to its last, counting from one
enum class RecapPart(val startMonth: Int, val endMonth: Int, @StringRes val labelRes: Int) {
    Year(1, 12, R.string.recap_part_year),
    FirstHalf(1, 6, R.string.recap_part_first_half),
    SecondHalf(7, 12, R.string.recap_part_second_half),
    FirstQuarter(1, 3, R.string.recap_part_q1),
    SecondQuarter(4, 6, R.string.recap_part_q2),
    ThirdQuarter(7, 9, R.string.recap_part_q3),
    FourthQuarter(10, 12, R.string.recap_part_q4)
}

// A release of an artist the user follows that came out in the year and was never listened to
data class RecapMissed(val id: String, val title: String, val artist: String, val cover: String?)

// What a year of listening comes to
class Recap(
    val year: Int,
    val totalMs: Long,
    val listens: Int,
    val days: Int,
    // The most days in a row with something listened to
    val longestStreak: Int,
    // The day the most was listened to and how long, none when nothing was
    val biggestDay: LocalDate?,
    val biggestDayMs: Long,
    // The time listened in each month, the first is January
    val months: List<Long>,
    // The time listened in each hour of the day, the first is midnight
    val hours: List<Long>,
    // The time listened in each month to each of the first artists, by their names
    val artistMonths: Map<String, List<Long>>,
    val songs: List<TopSong>,
    val artists: List<TopArtist>,
    val genres: List<RecapGenre>,
    val albums: List<RecapAlbum>,
    // What the same stretch of the year before had, nothing when there was nothing listened to then
    val previousTotalMs: Long? = null,
    val missed: List<RecapMissed> = emptyList(),
    val part: RecapPart = RecapPart.Year,
    // How many artists were listened to
    val artistCount: Int = artists.size,
    // The time listened in each month to each of the first songs, by their ids
    val songMonths: Map<String, List<Long>> = emptyMap(),
    // The time listened in each month to each of the first albums, by their ids
    val albumMonths: Map<String, List<Long>> = emptyMap()
) {
    val isEmpty: Boolean get() = totalMs == 0L

    // How much more or less was listened to than in the year before, in percent, nothing when there is no year before to compare with
    val changeFromPrevious: Int?
        get() = previousTotalMs?.takeIf { it > 0L }?.let { before -> ((totalMs - before) * PercentOf / before).toInt() }

    // The year that weighs the most in what was listened to, by the albums that tell their year, nothing when too few songs say it
    val musicYear: Int?
        get() {
            val dated = songs.filter { (it.song.releaseYear ?: 0) > 0 }
            if (dated.size < MinimumDatedSongs) return null
            return dated.groupBy { it.song.releaseYear ?: 0 }.maxByOrNull { (_, list) -> list.sumOf { it.total } }?.key
        }

    // The age of the era that year belongs to, taking it as the one the user was growing up in at the age the taste forms
    val musicAge: Int? get() = musicYear?.let { year -> year.let { LocalDate.now().year - it + FormativeAge } }

    // The part of the day with the most time listened: night from midnight to six, then morning, afternoon and evening
    val dayPart: DayPart
        get() {
            val parts = DayPart.entries
            val sums = parts.indices.map { index -> hours.subList(index * HoursInDay / parts.size, (index + 1) * HoursInDay / parts.size).sum() }
            return parts[sums.indexOf(sums.max())]
        }

    // The share of the time listened that went to the first artist, from zero to one
    val topArtistShare: Float
        get() = if (totalMs == 0L) 0f else (artists.firstOrNull()?.totalMs ?: 0L).toFloat() / totalMs

    // How many times each song was played on average
    val playsPerSong: Float
        get() = if (songs.isEmpty()) 0f else listens.toFloat() / songs.size
}

// Works out the retrospective of a year from the history kept on the device, from the first of January to the last of December
object RecapRepository {
    // The years the history has something of, the newest first, the current year is always there
    suspend fun years(context: Context, zone: ZoneId = ZoneId.systemDefault()): List<Int> = withContext(Dispatchers.IO) {
        val current = LocalDate.now(zone).year
        val first = PlayHistory.firstListenAt(context)?.let { Instant.ofEpochMilli(it).atZone(zone).year } ?: current
        (current downTo minOf(first, current)).toList()
    }

    // The numbers of a year
    suspend fun load(context: Context, year: Int, part: RecapPart = RecapPart.Year, zone: ZoneId = ZoneId.systemDefault()): Recap = withContext(Dispatchers.IO) {
        val since = LocalDate.of(year, part.startMonth, 1).atStartOfDay(zone).toInstant().toEpochMilli()
        val until = LocalDate.of(year, part.endMonth, 1).plusMonths(1).atStartOfDay(zone).toInstant().toEpochMilli()

        // The time of each listening goes to its month, its day and its hour
        val maxDuration = RecapFilter.maxDurationMs(context)
        val keepLoops = !RecapFilter.skipsLoops(context)
        val listens = PlayHistory.listensBetween(context, since, until, maxDuration, keepLoops)
        val months = LongArray(MonthsInYear)
        val hours = LongArray(HoursInDay)
        val perDay = HashMap<LocalDate, Long>()
        listens.forEach { listen ->
            val moment = Instant.ofEpochMilli(listen.timestamp).atZone(zone)
            val day = moment.toLocalDate()
            months[day.monthValue - 1] += listen.playTimeMs
            hours[moment.hour] += listen.playTimeMs
            perDay[day] = (perDay[day] ?: 0L) + listen.playTimeMs
        }
        val biggest = perDay.maxByOrNull { it.value }

        val songs = PlayHistory.recapSongs(context, since, until, maxDuration, keepLoops, Int.MAX_VALUE)
        // The artists by the time listened, which is what the pages of the story tell
        val artists = ProfileRanking.groupArtists(PlayHistory.recapArtists(context, since, until, maxDuration, keepLoops)).sortedByDescending { it.totalMs }

        // The same stretch of the year before, to say how much more or less was listened to
        val previousSince = LocalDate.of(year - 1, part.startMonth, 1).atStartOfDay(zone).toInstant().toEpochMilli()
        val previousUntil = LocalDate.of(year - 1, part.endMonth, 1).plusMonths(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val previousTotal = PlayHistory.listensBetween(context, previousSince, previousUntil, maxDuration, keepLoops).sumOf { it.playTimeMs }

        val albums = albumsOf(songs)

        Recap(
            year = year,
            totalMs = listens.sumOf { it.playTimeMs },
            listens = songs.sumOf { it.plays },
            days = perDay.size,
            longestStreak = streakOf(perDay.keys),
            biggestDay = biggest?.key,
            biggestDayMs = biggest?.value ?: 0L,
            months = months.toList(),
            hours = hours.toList(),
            artistMonths = artistMonthsOf(context, artists, since, until, maxDuration, keepLoops, zone),
            songs = songs,
            artists = artists,
            genres = genresOf(songs),
            albums = albums,
            previousTotalMs = previousTotal.takeIf { it > 0L },
            missed = PlayHistory.missedReleases(context, year, MissedReleases).map { RecapMissed(it.id, it.title, it.artistName, it.coverUrl) },
            part = part,
            songMonths = songMonthsOf(context, songs, since, until, maxDuration, keepLoops, zone),
            albumMonths = albumMonthsOf(context, songs, albums, since, until, maxDuration, keepLoops, zone)
        )
    }

    // The time listened in each month to each of the first artists
    private suspend fun artistMonthsOf(context: Context, artists: List<TopArtist>, since: Long, until: Long, maxDuration: Long, keepLoops: Boolean, zone: ZoneId): Map<String, List<Long>> {
        val wanted = artists.take(ArtistsWithMonths).map { it.name }.toSet()
        val months = wanted.associateWith { LongArray(MonthsInYear) }

        PlayHistory.artistListensBetween(context, since, until, maxDuration, keepLoops).forEach { listen ->
            val name = ProfileRanking.firstArtist(listen.artist)
            val month = months[name] ?: return@forEach
            month[Instant.ofEpochMilli(listen.timestamp).atZone(zone).monthValue - 1] += listen.playTimeMs
        }
        return months.mapValues { it.value.toList() }
    }

    // The time listened in each month to each of the first songs
    private suspend fun songMonthsOf(context: Context, songs: List<TopSong>, since: Long, until: Long, maxDuration: Long, keepLoops: Boolean, zone: ZoneId): Map<String, List<Long>> {
        val months = songs.take(SongsWithMonths).associate { it.song.id to LongArray(MonthsInYear) }

        PlayHistory.songListensBetween(context, since, until, maxDuration, keepLoops).forEach { listen ->
            val month = months[listen.songId] ?: return@forEach
            month[Instant.ofEpochMilli(listen.timestamp).atZone(zone).monthValue - 1] += listen.playTimeMs
        }
        return months.mapValues { it.value.toList() }
    }

    // The time listened in each month to each of the first albums, which is the time of the songs that belong to them
    private suspend fun albumMonthsOf(context: Context, songs: List<TopSong>, albums: List<RecapAlbum>, since: Long, until: Long, maxDuration: Long, keepLoops: Boolean, zone: ZoneId): Map<String, List<Long>> {
        val wanted = albums.take(AlbumsWithMonths).map { it.id }.toSet()
        val albumOfSong = songs.filter { it.song.albumId in wanted }.associate { it.song.id to it.song.albumId.orEmpty() }
        val months = wanted.associateWith { LongArray(MonthsInYear) }

        PlayHistory.songListensBetween(context, since, until, maxDuration, keepLoops).forEach { listen ->
            val album = albumOfSong[listen.songId] ?: return@forEach
            months[album]?.let { it[Instant.ofEpochMilli(listen.timestamp).atZone(zone).monthValue - 1] += listen.playTimeMs }
        }
        return months.mapValues { it.value.toList() }
    }

    // The longest run of days one right after the other
    private fun streakOf(days: Set<LocalDate>): Int {
        var longest = 0
        var run = 0
        var previous: LocalDate? = null
        days.sorted().forEach { day ->
            run = if (previous != null && previous!!.plusDays(1) == day) run + 1 else 1
            longest = maxOf(longest, run)
            previous = day
        }
        return longest
    }

    // The genres by the time listened, the songs whose genres were not looked for yet are left out
    private fun genresOf(songs: List<TopSong>): List<RecapGenre> {
        val totals = HashMap<String, Long>()
        songs.forEach { top ->
            top.song.genres?.split(GenreSeparator)?.filter { it.isNotBlank() }?.forEach { genre ->
                totals[genre] = (totals[genre] ?: 0L) + top.total
            }
        }
        return totals.map { (name, total) -> RecapGenre(name, total) }.sortedByDescending { it.totalMs }
    }

    // The albums by the time listened, the songs without an album are left out
    private fun albumsOf(songs: List<TopSong>): List<RecapAlbum> =
        songs
            .filter { !it.song.albumId.isNullOrBlank() && !it.song.albumName.isNullOrBlank() }
            .groupBy { it.song.albumId.orEmpty() }
            .map { (id, list) ->
                val lead = list.maxBy { it.total }
                RecapAlbum(id, lead.song.albumName.orEmpty(), lead.song.artworkUrl, list.sumOf { it.total }, list.sumOf { it.plays }, lead.song.artist)
            }
            .sortedWith(compareByDescending<RecapAlbum> { it.totalMs })
}

package com.wavvy.app.features.recap.data

// Time
import java.time.LocalDate

// Temporary: gives a year the numbers it lacks, only so every page can be seen while they are checked, to be deleted with the review switch
object RecapReview {
    private const val SampleMonth = 6
    private const val SampleDay = 14
    private const val SampleDayMs = 5 * 60 * 60 * 1000L
    private const val SampleYear = 2012
    private const val PreviousShare = 4L
    private const val PreviousOf = 5L
    private const val MillisPerHour = 60 * 60 * 1000L

    // Genres with the hours listened to each one, and the releases that were missed
    private val genres = listOf("Pop" to 41L, "Rock" to 33L, "Hip-hop" to 26L, "Funk" to 19L, "Electronic" to 14L, "Jazz" to 9L, "Lo-fi" to 6L, "Classical" to 3L)
    private val missed = listOf("First Light" to "Nova Ray", "Paper Moons" to "Lia Duarte", "Night Shift" to "The Parallels", "Slow Burn" to "Mika Sol", "Open Roads" to "Bruno Vega", "Glass Rooms" to "Aurora Lane")

    fun fill(recap: Recap): Recap {
        if (recap.isEmpty) return recap

        // The songs get a year of release only when too few of them have one, so the age of the music can show
        val songs = if (recap.musicYear != null) recap.songs else recap.songs.map { it.copy(song = it.song.copy(releaseYear = SampleYear)) }

        return Recap(
            year = recap.year,
            totalMs = recap.totalMs,
            listens = recap.listens,
            days = recap.days,
            longestStreak = recap.longestStreak,
            biggestDay = recap.biggestDay ?: LocalDate.of(recap.year, SampleMonth, SampleDay),
            biggestDayMs = recap.biggestDayMs.takeIf { it > 0L } ?: SampleDayMs,
            months = recap.months,
            hours = recap.hours,
            artistMonths = recap.artistMonths,
            songs = songs,
            artists = recap.artists,
            genres = recap.genres.ifEmpty { genres.map { (name, hours) -> RecapGenre(name, hours * MillisPerHour) } },
            albums = recap.albums,
            previousTotalMs = recap.previousTotalMs ?: (recap.totalMs * PreviousShare / PreviousOf),
            missed = recap.missed.ifEmpty { missed.mapIndexed { index, (title, artist) -> RecapMissed("review$index", title, artist, null) } },
            part = recap.part,
            artistCount = recap.artistCount,
            songMonths = recap.songMonths,
            albumMonths = recap.albumMonths
        )
    }
}

package com.wavvy.app.features.podcast.data

// Time
import java.util.Calendar

// When a premiere is set for, as YouTube Music writes it, and the moment that makes
class ScheduledTime(val text: String, val at: Long?)

// Reads the moment of a premiere from the words of the subtitle, such as Programado para 29 de out., 13:45, which are written in the time zone of the device
object ScheduleTime {
    // The words that say the episode is not out yet, in the two languages of the app
    private val Label = Regex("^(programado para|scheduled for|premieres on|premieres|estreia em|estreia)\\s*", RegexOption.IGNORE_CASE)

    // The day, the month, an optional year and the time, in Portuguese and in English
    private val Portuguese = Regex("(\\d{1,2})\\s+de\\s+(\\p{L}+)\\.?,?\\s+(?:de\\s+(\\d{4}),?\\s+)?(\\d{1,2}):(\\d{2})")
    private val English = Regex("(\\p{L}+)\\.?\\s+(\\d{1,2}),?\\s+(?:(\\d{4}),?\\s+)?(?:at\\s+)?(\\d{1,2}):(\\d{2})\\s*(AM|PM)?", RegexOption.IGNORE_CASE)

    // The first three letters of each month in the two languages
    private val Months = mapOf(
        "jan" to Calendar.JANUARY, "fev" to Calendar.FEBRUARY, "feb" to Calendar.FEBRUARY, "mar" to Calendar.MARCH,
        "abr" to Calendar.APRIL, "apr" to Calendar.APRIL, "mai" to Calendar.MAY, "may" to Calendar.MAY,
        "jun" to Calendar.JUNE, "jul" to Calendar.JULY, "ago" to Calendar.AUGUST, "aug" to Calendar.AUGUST,
        "set" to Calendar.SEPTEMBER, "sep" to Calendar.SEPTEMBER, "out" to Calendar.OCTOBER, "oct" to Calendar.OCTOBER,
        "nov" to Calendar.NOVEMBER, "dez" to Calendar.DECEMBER, "dec" to Calendar.DECEMBER
    )

    // A date that came out a long time ago is of the next year, YouTube Music leaves the year out of the ones in the near future
    private const val DaysBeforeNextYear = 183
    private const val MillisPerDay = 24L * 60 * 60 * 1000
    private const val Noon = 12

    // The time of the premiere when the subtitle says one, none when the episode is out or the words are not the ones of a premiere
    fun read(subtitle: String, now: Long = System.currentTimeMillis()): ScheduledTime? {
        if (!Label.containsMatchIn(subtitle.trim())) return null

        val rest = subtitle.trim().replace(Label, "")
        Portuguese.find(rest)?.let { match ->
            val (day, month, year, hour, minute) = match.destructured
            return ScheduledTime(match.value, moment(month, day.toInt(), year.toIntOrNull(), hour.toInt(), minute.toInt(), now))
        }
        English.find(rest)?.let { match ->
            val (month, day, year, hour, minute, period) = match.destructured
            val hours = when {
                period.equals("PM", ignoreCase = true) && hour.toInt() < Noon -> hour.toInt() + Noon
                period.equals("AM", ignoreCase = true) && hour.toInt() == Noon -> 0
                else -> hour.toInt()
            }
            return ScheduledTime(match.value, moment(month, day.toInt(), year.toIntOrNull(), hours, minute.toInt(), now))
        }
        return ScheduledTime(rest.trim(), null)
    }

    private fun moment(month: String, day: Int, year: Int?, hour: Int, minute: Int, now: Long): Long? {
        val index = Months[month.take(3).lowercase()] ?: return null
        val thisYear = Calendar.getInstance().apply { timeInMillis = now }.get(Calendar.YEAR)

        fun at(inYear: Int): Long = Calendar.getInstance().apply {
            clear()
            set(inYear, index, day, hour, minute, 0)
        }.timeInMillis

        val found = at(year ?: thisYear)
        return if (year == null && found < now - DaysBeforeNextYear * MillisPerDay) at(thisYear + 1) else found
    }
}

package com.wavvy.app.features.history.ui

// Android resources
import androidx.annotation.StringRes
// Time
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
// Project resources
import com.wavvy.app.R

// The periods that the history can be narrowed to, from today to this year
enum class HistoryFilter(@StringRes val titleRes: Int) {
    Today(R.string.history_today),
    Yesterday(R.string.history_yesterday),
    Week(R.string.history_filter_week),
    Month(R.string.history_filter_month),
    Semester(R.string.history_filter_semester),
    Year(R.string.history_filter_year);

    // True when the day is inside the period, the week starts on the first day of the week of the place of the user
    fun matches(day: LocalDate, today: LocalDate, firstDayOfWeek: DayOfWeek): Boolean = when (this) {
        Today -> day == today
        Yesterday -> day == today.minusDays(1)
        Week -> !day.isBefore(today.with(TemporalAdjusters.previousOrSame(firstDayOfWeek))) && !day.isAfter(today)
        Month -> day.year == today.year && day.month == today.month
        Semester -> day.year == today.year && (day.monthValue <= SemesterLastMonth) == (today.monthValue <= SemesterLastMonth)
        Year -> day.year == today.year
    }
}

// The last month of the first semester of the year
private const val SemesterLastMonth = 6

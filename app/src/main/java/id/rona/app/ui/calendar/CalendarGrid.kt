package id.rona.app.ui.calendar

import java.time.LocalDate
import java.time.YearMonth

/**
 * Pure, side-effect-free calendar-grid helpers shared by [CalendarScreen]
 * and [CalendarViewModel]. Extracted so weekday ordering and the Monday-based
 * grid start can be asserted in plain JVM unit tests (no DAOs / Compose).
 *
 * The grid pads back to Monday (ISO `dayOfWeek.value - 1`), so the header
 * labels below are ordered Monday-first to stay aligned with column 0.
 */
internal val calendarWeekdayLabelsMonFirst: List<String> =
    listOf("SEN", "SEL", "RAB", "KAM", "JUM", "SAB", "MIN")

/** First day shown in a month grid: the Monday on/before the 1st. */
internal fun monthStartDay(yearMonth: YearMonth): LocalDate {
    val firstOfMonth = yearMonth.atDay(1)
    return firstOfMonth.minusDays((firstOfMonth.dayOfWeek.value - 1).toLong())
}

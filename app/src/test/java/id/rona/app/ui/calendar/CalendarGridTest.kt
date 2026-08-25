package id.rona.app.ui.calendar

import com.google.common.truth.Truth.assertThat
import java.time.LocalDate
import java.time.YearMonth
import org.junit.Test

/**
 * Verifies the calendar grid stays Monday-first and that the weekday header
 * labels align with the grid's column-0 (Monday). This is the regression guard
 * for the phase-A fix where the header was accidentally Sunday-first while the
 * grid padded back to Monday.
 */
class CalendarGridTest {

    private fun assertMondayFirstLabelOrder() {
        // ISO dayOfWeek Monday == 1, and monthStartDay pads back to that Monday.
        assertThat(calendarWeekdayLabelsMonFirst)
            .containsExactly("SEN", "SEL", "RAB", "KAM", "JUM", "SAB", "MIN")
            .inOrder()
    }

    @Test
    fun weekdayLabelsAreMondayFirst() {
        assertMondayFirstLabelOrder()
    }

    @Test
    fun monthStartIsMondayBeforeOrOnFirstDay() {
        // August 1 2026 is a Saturday; grid must start on the preceding Monday (July 27).
        val aug2026 = YearMonth.of(2026, 8)
        assertThat(monthStartDay(aug2026)).isEqualTo(LocalDate.of(2026, 7, 27))
        assertThat(monthStartDay(aug2026).dayOfWeek.value).isEqualTo(1) // ISO Monday

        // March 1 2026 is a Sunday; start on Monday Feb 23.
        val mar2026 = YearMonth.of(2026, 3)
        assertThat(monthStartDay(mar2026)).isEqualTo(LocalDate.of(2026, 2, 23))

        // June 1 2026 is a Monday; start on June 1 itself.
        val jun2026 = YearMonth.of(2026, 6)
        assertThat(monthStartDay(jun2026)).isEqualTo(LocalDate.of(2026, 6, 1))
    }

    @Test
    fun gridCellsAlignWithMondayFirstHeaders() {
        val yearMonth = YearMonth.of(2026, 8)
        val start = monthStartDay(yearMonth)
        // 42 cells (6 weeks). The first 7 cells must map to Monday..Sunday in order.
        val firstWeek = (0L until 7L).map { start.plusDays(it) }
        assertThat(firstWeek.map { it.dayOfWeek.value })
            .containsExactly(1, 2, 3, 4, 5, 6, 7)
            .inOrder()

        // Every Monday column across the grid must be at index % 7 == 0.
        (0L until 42L).forEach { index ->
            val dow = start.plusDays(index).dayOfWeek.value
            val expectedColumn = (dow - 1) % 7
            assertThat(expectedColumn).isEqualTo(index % 7)
        }
    }

    @Test
    fun labelCountMatchesGridWeekLength() {
        assertThat(calendarWeekdayLabelsMonFirst).hasSize(7)
        assertThat(calendarWeekdayLabelsMonFirst.size).isEqualTo(7)
    }
}

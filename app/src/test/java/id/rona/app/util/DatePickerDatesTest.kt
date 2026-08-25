package id.rona.app.util

import com.google.common.truth.Truth.assertThat
import java.time.LocalDate
import java.time.ZoneId
import java.util.TimeZone
import org.junit.After
import org.junit.Before
import org.junit.Test

/**
 * Regression guard for the DatePicker timezone bug: Material3's DatePicker
 * passes UTC-midnight millis. The conversion must stay fixed to UTC so a user
 * in any timezone sees exactly the day they tapped — never an off-by-one.
 *
 * The tests pin the default timezone to two extreme offsets to prove the
 * conversion is zone-independent.
 */
class DatePickerDatesTest {

    private lateinit var previousTimeZone: TimeZone

    @Before
    fun saveTimeZone() {
        previousTimeZone = TimeZone.getDefault()
    }

    @After
    fun restoreTimeZone() {
        TimeZone.setDefault(previousTimeZone)
    }

    private fun withTimeZone(id: String, block: () -> Unit) {
        TimeZone.setDefault(TimeZone.getTimeZone(ZoneId.of(id)))
        try {
            block()
        } finally {
            TimeZone.setDefault(previousTimeZone)
        }
    }

    @Test
    fun roundTripIsStableInUtcPlus7() {
        withTimeZone("Asia/Jakarta") {
            val date = LocalDate.of(2026, 8, 23)
            assertThat(DatePickerDates.fromMillis(DatePickerDates.toMillis(date))).isEqualTo(date)
        }
    }

    @Test
    fun roundTripIsStableInUtcMinus5() {
        withTimeZone("America/New_York") {
            val date = LocalDate.of(2026, 8, 23)
            assertThat(DatePickerDates.fromMillis(DatePickerDates.toMillis(date))).isEqualTo(date)
        }
    }

    @Test
    fun utcMidnightMillisResolveToThePickedDayRegardlessOfZone() {
        // 2026-08-23T00:00:00Z is 2026-08-23T07:00 in Jakarta.
        val utcMillis = LocalDate.of(2026, 8, 23)
            .atStartOfDay(ZoneId.of("UTC"))
            .toInstant()
            .toEpochMilli()

        withTimeZone("Asia/Jakarta") {
            assertThat(DatePickerDates.fromMillis(utcMillis)).isEqualTo(LocalDate.of(2026, 8, 23))
        }
        withTimeZone("America/New_York") {
            assertThat(DatePickerDates.fromMillis(utcMillis)).isEqualTo(LocalDate.of(2026, 8, 23))
        }
    }

    @Test
    fun millisAreUtcMidnightNotLocalMidnight() {
        // In Jakarta (+7), local midnight of Aug 23 is 2026-08-22T17:00Z.
        withTimeZone("Asia/Jakarta") {
            val millis = DatePickerDates.toMillis(LocalDate.of(2026, 8, 23))
            val asInstant = java.time.Instant.ofEpochMilli(millis)
            assertThat(asInstant.atZone(ZoneId.of("UTC")).toLocalDate())
                .isEqualTo(LocalDate.of(2026, 8, 23))
            // And it is *not* the local-midnight instant.
            val localMidnight = LocalDate.of(2026, 8, 23)
                .atStartOfDay(ZoneId.of("Asia/Jakarta"))
                .toInstant()
                .toEpochMilli()
            assertThat(millis).isNotEqualTo(localMidnight)
        }
    }
}

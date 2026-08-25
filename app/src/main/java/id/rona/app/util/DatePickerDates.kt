package id.rona.app.util

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * Conversions to/from the long milliseconds that `Material3` DatePicker passes
 * back via `selectedDateMillis`.
 *
 * Material3's DatePicker is UTC-based: a picked day D is encoded as the UTC
 * epoch-millis of D's midnight. We therefore interpret the value strictly in
 * [ZoneOffset.UTC] — never the device's default zone — so that a user in e.g.
 * UTC+7 or UTC-5 always sees *their* selected day, never an off-by-one shift.
 */
object DatePickerDates {

    /** `selectedDateMillis` -> `LocalDate` (UTC interpretation, never system zone). */
    fun fromMillis(millis: Long): LocalDate =
        Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()

    /** `LocalDate` -> UTC epoch-millis for [androidx.compose.material3.rememberDatePickerState]. */
    fun toMillis(date: LocalDate): Long =
        date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
}

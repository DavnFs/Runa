package id.rona.app.domain.model

import java.time.LocalDate

/**
 * Domain model representing a verified menstruation period entry.
 */
data class PeriodRecord(
    val id: Long = 0,
    val startDate: LocalDate,
    val endDate: LocalDate?,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
) {
    val isOngoing: Boolean get() = endDate == null

    val durationDays: Int? get() = endDate?.let {
        (it.toEpochDay() - startDate.toEpochDay()).toInt() + 1
    }
}

/**
 * Validation result for period record editing.
 */
sealed interface PeriodValidationResult {
    data object Valid : PeriodValidationResult

    data class InvalidDateOrder(
        val message: String = "Tanggal selesai tidak boleh sebelum tanggal mulai.",
    ) : PeriodValidationResult

    data class OverlapConflict(
        val message: String,
        val conflictingRecords: List<PeriodRecord>,
    ) : PeriodValidationResult

    /**
     * Warning only — does NOT block saving.
     */
    data class UnusualDurationWarning(
        val durationDays: Int,
        val message: String = "Periode ini tercatat $durationDays hari (lebih dari 15 hari). Pastikan tanggalnya sudah benar.",
    ) : PeriodValidationResult
}

/**
 * Strategy chosen by the user when overlapping records are detected.
 */
enum class OverlapResolutionStrategy {
    /**
     * Merge all overlapping records into a single bounding interval [min(start), max(end)].
     */
    MERGE,

    /**
     * Delete overlapping records and replace with the new record.
     */
    REPLACE,
}

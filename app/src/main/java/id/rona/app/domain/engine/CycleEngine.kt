package id.rona.app.domain.engine

import id.rona.app.domain.model.Confidence
import java.time.LocalDate

/**
 * Result of a deterministic cycle prediction.
 * Always a range; never a single absolute date.
 */
data class CyclePrediction(
    val predictedStart: LocalDate,
    val rangeLow: LocalDate,
    val rangeHigh: LocalDate,
    val medianCycleLengthDays: Int,
    val meanCycleLengthDays: Double,
    val madDays: Double,
    val cycleCountUsed: Int,
    val confidence: Confidence,
)

data class CycleLength(
    val startOfCurrent: LocalDate,
    val startOfPrevious: LocalDate,
    val lengthDays: Int,
)

/**
 * Deterministic cycle engine. Pure Kotlin, no Android imports.
 *
 * Rules (docs/PLAN.md "Prediction rules"):
 * - Cycle length = start(current period) - start(previous period).
 * - Valid cycle window: [MIN_CYCLE_DAYS, MAX_CYCLE_DAYS]; out-of-window lengths are ignored.
 * - Use the median of the last [MAX_CYCLES_USED] valid cycles; display mean alongside.
 * - Range = predictedStart ± max(1, 1.5 × MAD), capped at [MAX_RANGE_HALF_DAYS].
 * - Confidence: >=5 cycles with MAD<=2 -> HIGH; 3-4 cycles -> MEDIUM; <3 -> LOW.
 * - Never claims medical or contraceptive reliability.
 */
object CycleEngine {

    const val MIN_CYCLE_DAYS = 15
    const val MAX_CYCLE_DAYS = 90
    const val MAX_CYCLES_USED = 6
    const val MAX_RANGE_HALF_DAYS = 7
    const val ENGINE_VERSION = 1

    fun cycleLengths(periodStarts: List<LocalDate>): List<CycleLength> {
        val sorted = periodStarts.distinct().sorted()
        return sorted.zipWithNext().map { (previous, current) ->
            CycleLength(current, previous, (current.toEpochDay() - previous.toEpochDay()).toInt())
        }
    }

    /**
     * Returns valid cycle lengths (within window) from the most recent up to [maxCycles] cycles.
     */
    fun recentValidCycleLengths(
        periodStarts: List<LocalDate>,
        maxCycles: Int = MAX_CYCLES_USED,
    ): List<Int> {
        val all = cycleLengths(periodStarts)
            .filter { it.lengthDays in MIN_CYCLE_DAYS..MAX_CYCLE_DAYS }
            .map { it.lengthDays }
        return all.takeLast(maxCycles)
    }

    fun predict(periodStarts: List<LocalDate>): CyclePrediction? {
        val lengths = recentValidCycleLengths(periodStarts)
        if (lengths.size < MIN_CYCLES_FOR_PREDICTION) return null

        val lastStart = periodStarts.distinct().maxOrNull() ?: return null
        val median = median(lengths)
        val mean = lengths.average()
        val mad = medianAbsoluteDeviation(lengths, median)
        val confidence = confidenceFor(lengths, mad)

        val predictedStart = lastStart.plusDays(median.toLong())
        val halfRange = (mad * RANGE_FACTOR).toInt()
            .coerceAtLeast(MIN_RANGE_HALF_DAYS)
            .coerceAtMost(MAX_RANGE_HALF_DAYS)

        return CyclePrediction(
            predictedStart = predictedStart,
            rangeLow = predictedStart.minusDays(halfRange.toLong()),
            rangeHigh = predictedStart.plusDays(halfRange.toLong()),
            medianCycleLengthDays = median,
            meanCycleLengthDays = mean,
            madDays = mad,
            cycleCountUsed = lengths.size,
            confidence = confidence,
        )
    }

    /**
     * Day of the current cycle for [today]: days since the latest period start + 1.
     * Returns null when there is no recorded period.
     */
    fun cycleDayFor(today: LocalDate, periodStarts: List<LocalDate>): Int? {
        val lastStart = periodStarts.distinct().maxOrNull() ?: return null
        if (today < lastStart) return null
        return (today.toEpochDay() - lastStart.toEpochDay()).toInt() + 1
    }

    fun confidenceFor(cycleLengths: List<Int>, mad: Double): Confidence = when {
        cycleLengths.size < MEDIUM_CONFIDENCE_MIN_CYCLES -> Confidence.LOW
        mad >= HIGH_VARIABILITY_MAD_THRESHOLD -> Confidence.LOW
        cycleLengths.size >= HIGH_CONFIDENCE_MIN_CYCLES && mad <= HIGH_CONFIDENCE_MAX_MAD -> Confidence.HIGH
        else -> Confidence.MEDIUM
    }

    fun median(values: List<Int>): Int {
        require(values.isNotEmpty()) { "median of empty list is undefined" }
        val sorted = values.sorted()
        val middle = sorted.size / 2
        return if (sorted.size % 2 == 1) {
            sorted[middle]
        } else {
            (sorted[middle - 1] + sorted[middle]) / 2
        }
    }

    fun medianAbsoluteDeviation(values: List<Int>, medianValue: Int): Double {
        if (values.isEmpty()) return 0.0
        val deviations = values.map { kotlin.math.abs(it - medianValue) }.sorted()
        val middle = deviations.size / 2
        return if (deviations.size % 2 == 1) {
            deviations[middle].toDouble()
        } else {
            (deviations[middle - 1] + deviations[middle]) / 2.0
        }
    }

    const val MIN_CYCLES_FOR_PREDICTION = 2
    private const val MEDIUM_CONFIDENCE_MIN_CYCLES = 3
    private const val HIGH_CONFIDENCE_MIN_CYCLES = 5
    private const val HIGH_CONFIDENCE_MAX_MAD = 2.0
    private const val HIGH_VARIABILITY_MAD_THRESHOLD = 5.0
    private const val RANGE_FACTOR = 1.5
    private const val MIN_RANGE_HALF_DAYS = 1
}

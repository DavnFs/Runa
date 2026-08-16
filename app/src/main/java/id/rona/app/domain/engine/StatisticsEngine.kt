package id.rona.app.domain.engine

import id.rona.app.domain.model.SymptomType

data class SymptomFrequency(
    val symptomType: SymptomType,
    val frequency: Int,
)

enum class CyclePhase(val displayKey: String) {
    EARLY("phase_early"),
    MIDDLE("phase_middle"),
    LATE("phase_late"),
}

/**
 * Personal statistics derived from locally recorded data. Pure Kotlin.
 * Produces descriptive summaries only — never diagnoses.
 */
object StatisticsEngine {

    fun averagePeriodDurationDays(
        periods: List<PeriodSpan>,
    ): Double? {
        val durations = periods.mapNotNull { span ->
            span.endEpochDay?.minus(span.startEpochDay)?.plus(1)?.toDouble()
        }
        if (durations.isEmpty()) return null
        return durations.average()
    }

    fun medianPeriodDurationDays(periods: List<PeriodSpan>): Int? {
        val durations = periods.mapNotNull { span ->
            span.endEpochDay?.minus(span.startEpochDay)?.plus(1)?.toInt()
        }
        if (durations.isEmpty()) return null
        return CycleEngine.median(durations)
    }

    fun mostFrequentSymptoms(
        counts: Map<SymptomType, Int>,
        limit: Int = 5,
    ): List<SymptomFrequency> =
        counts.entries
            .sortedByDescending { it.value }
            .take(limit)
            .map { SymptomFrequency(it.key, it.value) }

    /**
     * Assigns each day of a cycle to EARLY / MIDDLE / LATE by normalizing the cycle
     * length into three equal thirds (boundaries truncated, not rounded, so all
     * three phases always exist).
     */
    fun phaseForCycleDay(cycleDay: Int, cycleLengthDays: Int): CyclePhase {
        require(cycleLengthDays >= CycleEngine.MIN_CYCLE_DAYS) { "cycle too short to phase" }
        require(cycleDay >= 1) { "cycle day starts at 1" }
        val earlyEnd = cycleLengthDays / 3
        val middleEnd = cycleLengthDays * 2 / 3
        return when {
            cycleDay <= earlyEnd -> CyclePhase.EARLY
            cycleDay <= middleEnd -> CyclePhase.MIDDLE
            else -> CyclePhase.LATE
        }
    }

    /**
     * How many recorded days fall into each phase, keyed by their cycle day.
     */
    fun phaseDistribution(
        cycleDays: List<Int>,
        cycleLengthDays: Int,
    ): Map<CyclePhase, Int> =
        cycleDays
            .groupingBy { phaseForCycleDay(it, cycleLengthDays) }
            .eachCount()
}

data class PeriodSpan(
    val startEpochDay: Long,
    val endEpochDay: Long?,
)

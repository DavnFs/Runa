package id.rona.app.domain.engine

import java.time.LocalDate

/**
 * Estimated fertile window by the calendar method, derived from a cycle
 * prediction. Educational estimate only — never presented as contraception or
 * as confirmation of ovulation (mirrors the disclaimers in the education
 * content).
 */
data class FertilityWindow(
    val estimatedOvulation: LocalDate,
    val low: LocalDate,
    val high: LocalDate,
)

object FertilityEstimator {

    /** Ovulation is estimated 14 days before the predicted next period. */
    const val LUTEAL_PHASE_DAYS = 14L

    /** Fertile window: 5 days before ovulation through 1 day after. */
    const val FERTILE_DAYS_BEFORE = 5L
    const val FERTILE_DAYS_AFTER = 1L

    fun estimate(prediction: CyclePrediction): FertilityWindow {
        val ovulation = prediction.predictedStart.minusDays(LUTEAL_PHASE_DAYS)
        return FertilityWindow(
            estimatedOvulation = ovulation,
            low = ovulation.minusDays(FERTILE_DAYS_BEFORE),
            high = ovulation.plusDays(FERTILE_DAYS_AFTER),
        )
    }

    fun isInWindow(today: LocalDate, window: FertilityWindow): Boolean =
        !today.isBefore(window.low) && !today.isAfter(window.high)
}

/**
 * Human phase label for the current state, shared by the ViewModel and the
 * Home composable fallback so the mapping lives in one place.
 */
fun phaseNameFor(isPeriodActive: Boolean, cycleDay: Int?): String = when {
    isPeriodActive -> "Periode aktif"
    cycleDay == null -> "Fase siklus"
    cycleDay <= 5 -> "Fase menstruasi"
    cycleDay <= 13 -> "Fase folikuler"
    cycleDay <= 16 -> "Fase ovulasi"
    else -> "Fase luteal"
}

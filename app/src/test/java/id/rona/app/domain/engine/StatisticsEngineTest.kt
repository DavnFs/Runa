package id.rona.app.domain.engine

import com.google.common.truth.Truth.assertThat
import id.rona.app.domain.model.SymptomType
import org.junit.Test

class StatisticsEngineTest {

    @Test
    fun averagePeriodDurationIgnoresOngoingPeriods() {
        val spans = listOf(
            PeriodSpan(0, 4),      // 5 days
            PeriodSpan(28, 32),    // 5 days
            PeriodSpan(60, null),  // ongoing -> ignored
        )
        assertThat(StatisticsEngine.averagePeriodDurationDays(spans)).isEqualTo(5.0)
        assertThat(StatisticsEngine.medianPeriodDurationDays(spans)).isEqualTo(5)
    }

    @Test
    fun averageReturnsNullWhenNoCompletedPeriod() {
        val spans = listOf(PeriodSpan(0, null))
        assertThat(StatisticsEngine.averagePeriodDurationDays(spans)).isNull()
        assertThat(StatisticsEngine.medianPeriodDurationDays(spans)).isNull()
    }

    @Test
    fun mostFrequentSymptomsAreSortedDescending() {
        val counts = mapOf(
            SymptomType.KRAM to 8,
            SymptomType.HEADACHE to 2,
            SymptomType.FATIGUE to 5,
            SymptomType.ACNE to 1,
        )
        val result = StatisticsEngine.mostFrequentSymptoms(counts, limit = 3)
        assertThat(result.map { it.symptomType })
            .containsExactly(SymptomType.KRAM, SymptomType.FATIGUE, SymptomType.HEADACHE)
            .inOrder()
    }

    @Test
    fun phaseBoundariesSplitCycleIntoThirds() {
        // 30-day cycle: early 1-10, middle 11-20, late 21+
        assertThat(StatisticsEngine.phaseForCycleDay(1, 30)).isEqualTo(CyclePhase.EARLY)
        assertThat(StatisticsEngine.phaseForCycleDay(10, 30)).isEqualTo(CyclePhase.EARLY)
        assertThat(StatisticsEngine.phaseForCycleDay(11, 30)).isEqualTo(CyclePhase.MIDDLE)
        assertThat(StatisticsEngine.phaseForCycleDay(20, 30)).isEqualTo(CyclePhase.MIDDLE)
        assertThat(StatisticsEngine.phaseForCycleDay(21, 30)).isEqualTo(CyclePhase.LATE)
        assertThat(StatisticsEngine.phaseForCycleDay(30, 30)).isEqualTo(CyclePhase.LATE)
    }

    @Test
    fun phaseDistributionCountsAllPhases() {
        val cycleDays = listOf(1, 3, 8, 11, 15, 21, 25, 29)
        val distribution = StatisticsEngine.phaseDistribution(cycleDays, 30)
        assertThat(distribution[CyclePhase.EARLY]).isEqualTo(3)
        assertThat(distribution[CyclePhase.MIDDLE]).isEqualTo(2)
        assertThat(distribution[CyclePhase.LATE]).isEqualTo(3)
    }

    @Test(expected = IllegalArgumentException::class)
    fun phaseRejectsTooShortCycle() {
        StatisticsEngine.phaseForCycleDay(1, 10)
    }

    @Test(expected = IllegalArgumentException::class)
    fun phaseRejectsInvalidCycleDay() {
        StatisticsEngine.phaseForCycleDay(0, 30)
    }
}

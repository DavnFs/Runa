package id.rona.app.domain.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class FertilityEstimatorTest {

    private fun prediction(predictedStart: LocalDate) = CyclePrediction(
        predictedStart = predictedStart,
        rangeLow = predictedStart.minusDays(2),
        rangeHigh = predictedStart.plusDays(2),
        medianCycleLengthDays = 28,
        meanCycleLengthDays = 28.0,
        madDays = 1.0,
        cycleCountUsed = 6,
        confidence = id.rona.app.domain.model.Confidence.HIGH,
    )

    @Test
    fun `ovulation estimated 14 days before predicted start`() {
        val start = LocalDate.of(2026, 8, 1)
        val window = FertilityEstimator.estimate(prediction(start))
        assertEquals(LocalDate.of(2026, 7, 18), window.estimatedOvulation)
    }

    @Test
    fun `fertile window spans 5 days before through 1 day after ovulation`() {
        val start = LocalDate.of(2026, 8, 1)
        val window = FertilityEstimator.estimate(prediction(start))
        assertEquals(LocalDate.of(2026, 7, 13), window.low)
        assertEquals(LocalDate.of(2026, 7, 19), window.high)
    }

    @Test
    fun `isInWindow true on edges and inside, false outside`() {
        val window = FertilityEstimator.estimate(prediction(LocalDate.of(2026, 8, 1)))
        assertTrue(FertilityEstimator.isInWindow(LocalDate.of(2026, 7, 13), window))
        assertTrue(FertilityEstimator.isInWindow(LocalDate.of(2026, 7, 19), window))
        assertTrue(FertilityEstimator.isInWindow(LocalDate.of(2026, 7, 16), window))
        assertFalse(FertilityEstimator.isInWindow(LocalDate.of(2026, 7, 12), window))
        assertFalse(FertilityEstimator.isInWindow(LocalDate.of(2026, 7, 20), window))
    }

    @Test
    fun `phase labels map cycle days`() {
        assertEquals("Periode aktif", phaseNameFor(true, 3))
        assertEquals("Fase siklus", phaseNameFor(false, null))
        assertEquals("Fase menstruasi", phaseNameFor(false, 5))
        assertEquals("Fase folikuler", phaseNameFor(false, 13))
        assertEquals("Fase ovulasi", phaseNameFor(false, 16))
        assertEquals("Fase luteal", phaseNameFor(false, 17))
    }
}

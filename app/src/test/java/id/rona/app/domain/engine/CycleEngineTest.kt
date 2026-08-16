package id.rona.app.domain.engine

import com.google.common.truth.Truth.assertThat
import id.rona.app.domain.model.Confidence
import java.time.LocalDate
import org.junit.Test

class CycleEngineTest {

    private fun d(s: String): LocalDate = LocalDate.parse(s)

    @Test
    fun cycleLengthIsDifferenceOfStarts() {
        val lengths = CycleEngine.cycleLengths(
            listOf(d("2026-01-01"), d("2026-01-29"), d("2026-02-26"))
        )
        assertThat(lengths).hasSize(2)
        assertThat(lengths[0].lengthDays).isEqualTo(28)
        assertThat(lengths[1].lengthDays).isEqualTo(28)
    }

    @Test
    fun lengthsAreSortedChronologicallyRegardlessOfInputOrder() {
        val lengths = CycleEngine.cycleLengths(
            listOf(d("2026-02-26"), d("2026-01-01"), d("2026-01-29"))
        )
        assertThat(lengths.map { it.lengthDays }).containsExactly(28, 28).inOrder()
    }

    @Test
    fun duplicateStartsAreIgnored() {
        val lengths = CycleEngine.cycleLengths(
            listOf(d("2026-01-01"), d("2026-01-01"), d("2026-01-29"))
        )
        assertThat(lengths).hasSize(1)
        assertThat(lengths[0].lengthDays).isEqualTo(28)
    }

    @Test
    fun outOfWindowCycleLengthsAreIgnored() {
        val starts = listOf(
            d("2026-01-01"),
            d("2026-01-10"), // 9 days after previous: too short -> ignored
            d("2026-02-10"), // 31 days after 01-10: valid
        )
        val lengths = CycleEngine.recentValidCycleLengths(starts)
        assertThat(lengths).containsExactly(31)
    }

    @Test
    fun onlyLastSixCyclesAreUsed() {
        val base = LocalDate.of(2025, 1, 1)
        val starts = (0..10).map { base.plusDays(it * 28L) }
        val lengths = CycleEngine.recentValidCycleLengths(starts)
        assertThat(lengths).hasSize(6)
    }

    @Test
    fun predictReturnsNullWithTooFewCycles() {
        assertThat(CycleEngine.predict(listOf(d("2026-01-01")))).isNull()
        assertThat(CycleEngine.predict(emptyList())).isNull()
    }

    @Test
    fun predictUsesMedianNotMeanWithOutlier() {
        // 28, 28, 28, 40 -> median 28, mean 31
        val starts = listOf(
            d("2026-01-01"),
            d("2026-01-29"),
            d("2026-02-26"),
            d("2026-03-26"),
            d("2026-05-05"),
        )
        val prediction = CycleEngine.predict(starts)!!
        assertThat(prediction.medianCycleLengthDays).isEqualTo(28)
        assertThat(prediction.meanCycleLengthDays).isGreaterThan(28.0)
        assertThat(prediction.predictedStart).isEqualTo(d("2026-06-02"))
    }

    @Test
    fun predictionIsAlwaysARange() {
        val starts = listOf(
            d("2026-01-01"), d("2026-01-29"), d("2026-02-26"), d("2026-03-26"),
        )
        val prediction = CycleEngine.predict(starts)!!
        assertThat(prediction.rangeLow.isBefore(prediction.predictedStart) ||
            prediction.rangeLow == prediction.predictedStart).isTrue()
        assertThat(prediction.rangeHigh.isAfter(prediction.predictedStart) ||
            prediction.rangeHigh == prediction.predictedStart).isTrue()
        assertThat(prediction.rangeHigh.toEpochDay() - prediction.predictedStart.toEpochDay())
            .isAtMost(CycleEngine.MAX_RANGE_HALF_DAYS.toLong())
    }

    @Test
    fun confidenceHighWhenFiveCyclesAndLowMAD() {
        val confidence = CycleEngine.confidenceFor(listOf(28, 28, 29, 28, 28), mad = 0.0)
        assertThat(confidence).isEqualTo(Confidence.HIGH)
    }

    @Test
    fun confidenceMediumForThreeToFourCycles() {
        assertThat(CycleEngine.confidenceFor(listOf(28, 29, 28), mad = 1.0))
            .isEqualTo(Confidence.MEDIUM)
        assertThat(CycleEngine.confidenceFor(listOf(28, 28, 28, 28), mad = 0.0))
            .isEqualTo(Confidence.MEDIUM)
    }

    @Test
    fun confidenceLowWhenHighVariabilityEvenWithManyCycles() {
        assertThat(CycleEngine.confidenceFor(listOf(22, 35, 30, 28, 40), mad = 5.0))
            .isEqualTo(Confidence.LOW)
    }

    @Test
    fun cycleDayCountsFromLastStart() {
        val starts = listOf(d("2026-01-01"), d("2026-01-29"))
        assertThat(CycleEngine.cycleDayFor(d("2026-01-29"), starts)).isEqualTo(1)
        assertThat(CycleEngine.cycleDayFor(d("2026-02-10"), starts)).isEqualTo(13)
        assertThat(CycleEngine.cycleDayFor(d("2026-01-01"), starts)).isNull()
    }

    @Test
    fun cycleDayNullWithoutHistory() {
        assertThat(CycleEngine.cycleDayFor(d("2026-01-01"), emptyList())).isNull()
    }

    @Test
    fun cycleDayNullBeforeFirstStart() {
        val starts = listOf(d("2026-01-10"))
        assertThat(CycleEngine.cycleDayFor(d("2026-01-01"), starts)).isNull()
    }

    @Test
    fun medianOfEvenListIsAverageOfMiddleTwo() {
        assertThat(CycleEngine.median(listOf(28, 30))).isEqualTo(29)
        assertThat(CycleEngine.median(listOf(40, 28, 28, 40))).isEqualTo(34)
    }

    @Test
    fun madHandlesOddAndEvenCounts() {
        assertThat(CycleEngine.medianAbsoluteDeviation(listOf(28, 28, 28), 28)).isEqualTo(0.0)
        assertThat(CycleEngine.medianAbsoluteDeviation(listOf(28, 30), 29)).isEqualTo(1.0)
    }

    @Test
    fun leapYearCrossingWorks() {
        val starts = listOf(d("2024-02-10"), d("2024-03-09")) // 28 days across Feb 29
        assertThat(CycleEngine.cycleLengths(starts)[0].lengthDays).isEqualTo(28)
    }

    @Test
    fun predictionRangeGrowsWithVariability() {
        val stable = CycleEngine.predict(
            listOf(d("2026-01-01"), d("2026-01-29"), d("2026-02-26"), d("2026-03-26"), d("2026-04-23"))
        )!!
        val varied = CycleEngine.predict(
            listOf(d("2026-01-01"), d("2026-02-05"), d("2026-02-25"), d("2026-04-03"), d("2026-04-26"))
        )!!
        val stableSpan = stable.rangeHigh.toEpochDay() - stable.rangeLow.toEpochDay()
        val variedSpan = varied.rangeHigh.toEpochDay() - varied.rangeLow.toEpochDay()
        assertThat(variedSpan).isGreaterThan(stableSpan)
    }
}

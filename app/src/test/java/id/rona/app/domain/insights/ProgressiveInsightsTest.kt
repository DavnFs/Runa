package id.rona.app.domain.insights

import com.google.common.truth.Truth.assertThat
import id.rona.app.domain.engine.CycleEngine
import id.rona.app.domain.model.SymptomType
import java.time.LocalDate
import org.junit.Test

class ProgressiveInsightsTest {

    private fun starts(vararg dates: String) = dates.map(LocalDate::parse)

    @Test
    fun `maturity follows confirmed starts and valid intervals`() {
        assertThat(ProgressiveInsightsGenerator.maturity(0, 0, 0, 0))
            .isEqualTo(InsightMaturityLevel.LEVEL_0_EMPTY)
        assertThat(ProgressiveInsightsGenerator.maturity(1, 0, 0, 0))
            .isEqualTo(InsightMaturityLevel.LEVEL_1_SINGLE_START)
        assertThat(ProgressiveInsightsGenerator.maturity(2, 1, 0, 0))
            .isEqualTo(InsightMaturityLevel.LEVEL_2_INITIAL_HISTORY)
        assertThat(ProgressiveInsightsGenerator.maturity(3, 2, 0, 0))
            .isEqualTo(InsightMaturityLevel.LEVEL_3_ESTABLISHING)
        assertThat(ProgressiveInsightsGenerator.maturity(5, 4, 0, 0))
            .isEqualTo(InsightMaturityLevel.LEVEL_3_ESTABLISHING)
        assertThat(ProgressiveInsightsGenerator.maturity(6, 5, 0, 0))
            .isEqualTo(InsightMaturityLevel.LEVEL_4_MATURE)
    }

    @Test
    fun `cycle engine prediction starts at three valid period starts`() {
        val twoStarts = starts("2026-01-01", "2026-01-29")
        val threeStarts = starts("2026-01-01", "2026-01-29", "2026-02-26")

        assertThat(CycleEngine.recentValidCycleLengths(twoStarts)).containsExactly(28)
        assertThat(CycleEngine.predict(twoStarts)).isNull()
        assertThat(CycleEngine.recentValidCycleLengths(threeStarts)).containsExactly(28, 28)
        assertThat(CycleEngine.predict(threeStarts)).isNotNull()
    }

    @Test
    fun `level zero contains education only`() {
        val insights = ProgressiveInsightsGenerator.generate(emptyList(), null, 0)

        assertThat(insights.maturity).isEqualTo(InsightMaturityLevel.LEVEL_0_EMPTY)
        assertThat(insights.cards).hasSize(2)
        assertThat(insights.cards).isInstanceOf(List::class.java)
        assertThat(insights.cards.all { it.sourceLabel == InsightSourceLabel.GENERAL_EDUCATION }).isTrue()
    }

    @Test
    fun `level two exposes recorded interval but no formal prediction`() {
        val insights = ProgressiveInsightsGenerator.generate(
            periodStarts = starts("2026-01-01", "2026-01-29"),
            cycleDay = 10,
            dailyLogCount = 0,
        )

        assertThat(insights.maturity).isEqualTo(InsightMaturityLevel.LEVEL_2_INITIAL_HISTORY)
        assertThat(insights.cards.filterIsInstance<LastObservedIntervalCard>().single().days).isEqualTo(28)
        assertThat(insights.cards.filterIsInstance<PredictionRangeCard>()).isEmpty()
    }

    @Test
    fun `level three prediction is estimated and pattern needs three observations`() {
        val insights = ProgressiveInsightsGenerator.generate(
            periodStarts = starts("2026-01-01", "2026-01-29", "2026-02-26"),
            cycleDay = 10,
            dailyLogCount = 4,
            symptomCounts = mapOf(SymptomType.KRAM to 3),
        )

        assertThat(insights.maturity).isEqualTo(InsightMaturityLevel.LEVEL_3_ESTABLISHING)
        assertThat(insights.cards.filterIsInstance<PredictionRangeCard>()).hasSize(1)
        assertThat(insights.cards.filterIsInstance<PatternCard>()).hasSize(1)
        assertThat(insights.cards.filterIsInstance<PatternCard>().single().explanation)
            .contains("Pola pada catatanmu, bukan diagnosis.")
    }

    @Test
    fun `mature level does not expose trend below five valid intervals`() {
        val insights = ProgressiveInsightsGenerator.generate(
            periodStarts = starts(
                "2026-01-01", "2026-01-29", "2026-02-08", "2026-03-08", "2026-04-05", "2026-05-03",
            ),
            cycleDay = 10,
            dailyLogCount = 1,
        )

        assertThat(insights.maturity).isEqualTo(InsightMaturityLevel.LEVEL_4_MATURE)
        assertThat(insights.cards.filterIsInstance<TrendCard>()).isEmpty()
    }

    @Test
    fun `every generated card has an explicit source label`() {
        val insights = ProgressiveInsightsGenerator.generate(
            periodStarts = starts("2026-01-01", "2026-01-29", "2026-02-26"),
            cycleDay = 10,
            dailyLogCount = 0,
        )

        assertThat(insights.cards).isNotEmpty()
        assertThat(insights.cards.all { it.sourceLabel in InsightSourceLabel.entries }).isTrue()
    }
}

package id.rona.app.domain.insights

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class CycleEducationProviderTest {

    @Test
    fun `level 0 gets all general topics without prediction change`() {
        val topics = CycleEducationProvider.topicsForMaturity(InsightMaturityLevel.LEVEL_0_EMPTY)
        assertThat(topics).hasSize(6)
        assertThat(topics.map { it.id }).doesNotContain("edu_prediction_change")
    }

    @Test
    fun `level 2 gets prediction change topic`() {
        val topics = CycleEducationProvider.topicsForMaturity(InsightMaturityLevel.LEVEL_2_INITIAL_HISTORY)
        assertThat(topics.map { it.id }).contains("edu_prediction_change")
    }

    @Test
    fun `all topics have non-empty title and summary`() {
        val topics = CycleEducationProvider.topicsForMaturity(InsightMaturityLevel.LEVEL_4_MATURE)
        topics.forEach { topic ->
            assertThat(topic.title).isNotEmpty()
            assertThat(topic.summary).isNotEmpty()
            assertThat(topic.details).isNotEmpty()
        }
    }

    @Test
    fun `ovulation topic has mandatory disclaimer`() {
        val topics = CycleEducationProvider.topicsForMaturity(InsightMaturityLevel.LEVEL_0_EMPTY)
        val ovulation = topics.first { it.id == "edu_ovulation_est" }
        assertThat(ovulation.disclaimer).contains("kontrasepsi")
        assertThat(ovulation.disclaimer).contains("kehamilan")
    }

    @Test
    fun `fertility limits topic has mandatory disclaimer`() {
        val topics = CycleEducationProvider.topicsForMaturity(InsightMaturityLevel.LEVEL_0_EMPTY)
        val fertility = topics.first { it.id == "edu_fertility_limits" }
        assertThat(fertility.disclaimer).contains("kontrasepsi")
    }

    @Test
    fun `prediction change topic has mandatory disclaimer`() {
        val topics = CycleEducationProvider.topicsForMaturity(InsightMaturityLevel.LEVEL_2_INITIAL_HISTORY)
        val prediction = topics.first { it.id == "edu_prediction_change" }
        assertThat(prediction.disclaimer).contains("kontrasepsi")
    }

    @Test
    fun `phase topic returns null without cycle day`() {
        assertThat(CycleEducationProvider.phaseTopicForToday(null, 28, InsightMaturityLevel.LEVEL_1_SINGLE_START)).isNull()
    }

    @Test
    fun `phase topic returns null without cycle length`() {
        assertThat(CycleEducationProvider.phaseTopicForToday(5, null, InsightMaturityLevel.LEVEL_1_SINGLE_START)).isNull()
    }

    @Test
    fun `phase topic returns null at level 0`() {
        assertThat(CycleEducationProvider.phaseTopicForToday(5, 28, InsightMaturityLevel.LEVEL_0_EMPTY)).isNull()
    }

    @Test
    fun `phase topic returns menstruation for early cycle day`() {
        val topic = CycleEducationProvider.phaseTopicForToday(3, 28, InsightMaturityLevel.LEVEL_1_SINGLE_START)
        assertThat(topic).isNotNull()
        assertThat(topic!!.id).isEqualTo("edu_menstruation")
    }

    @Test
    fun `phase topic returns ovulation for middle cycle day`() {
        val topic = CycleEducationProvider.phaseTopicForToday(14, 28, InsightMaturityLevel.LEVEL_1_SINGLE_START)
        assertThat(topic).isNotNull()
        assertThat(topic!!.id).isEqualTo("edu_ovulation_est")
    }

    @Test
    fun `phase topic returns luteal for late cycle day`() {
        val topic = CycleEducationProvider.phaseTopicForToday(22, 28, InsightMaturityLevel.LEVEL_1_SINGLE_START)
        assertThat(topic).isNotNull()
        assertThat(topic!!.id).isEqualTo("edu_luteal")
    }

    @Test
    fun `all phase-specific topics are marked as such`() {
        val topics = CycleEducationProvider.topicsForMaturity(InsightMaturityLevel.LEVEL_0_EMPTY)
        val phaseSpecific = topics.filter { it.isPhaseSpecific }
        assertThat(phaseSpecific.map { it.id }).containsExactly(
            "edu_menstruation", "edu_follicular", "edu_ovulation_est", "edu_luteal",
        )
    }

    @Test
    fun `prediction change topic requires level 2`() {
        val level1 = CycleEducationProvider.topicsForMaturity(InsightMaturityLevel.LEVEL_1_SINGLE_START)
        assertThat(level1.map { it.id }).doesNotContain("edu_prediction_change")

        val level2 = CycleEducationProvider.topicsForMaturity(InsightMaturityLevel.LEVEL_2_INITIAL_HISTORY)
        assertThat(level2.map { it.id }).contains("edu_prediction_change")
    }
}
